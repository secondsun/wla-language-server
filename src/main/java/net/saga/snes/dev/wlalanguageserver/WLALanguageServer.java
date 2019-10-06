package net.saga.snes.dev.wlalanguageserver;

import static net.saga.snes.dev.wlalanguageserver.Utils.toRange;

import dev.secondsun.lsp.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import net.saga.snes.dev.wlalanguageserver.features.*;
import net.sagaoftherealms.tools.snes.assembler.main.Project;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.ErrorNode;

public class WLALanguageServer extends LanguageServer {

  private final LanguageClient client;
  private URI workspaceRoot;

  private static final Logger LOG = Logger.getLogger(WLALanguageServer.class.getName());
  private Project project;

  private InitializeProject initializeProject;
  private DocumentLinkFeature documentLinkFeature = new DocumentLinkFeature();
  private GotoDefinitionFeature gotoDefinitionFeature = new GotoDefinitionFeature();
  private DocumentSymbolFeature documentSymbolFeature = new DocumentSymbolFeature();
  private FindReferenceFeature findReferenceFeature = new FindReferenceFeature();
  private RetroJsonCompletionFeature retroCompletionFeature = new RetroJsonCompletionFeature();
  private Map<Integer, Function<Object, Void>> requests = new HashMap<>();

  public WLALanguageServer(LanguageClient client) {

    this.client = client;
  }

  @Override
  public InitializeResult initialize(InitializeParams params) {
    this.workspaceRoot = params.rootUri;
    List<Feature> features = features();

    this.initializeProject = new InitializeProject(params.rootUri, features);

    var initializeData = Json.createObjectBuilder();

    features.forEach(
        feature -> {
          feature.initializeFeature(workspaceRoot, initializeData);
        });

    return new InitializeResult(initializeData.build());
  }

  private List<Feature> features() {
    return Arrays.asList(
        this.documentLinkFeature,
        this.gotoDefinitionFeature,
        this.documentSymbolFeature,
        this.retroCompletionFeature,
        this.findReferenceFeature);
  }

  @Override
  public Optional<CompletionList> completion(TextDocumentPositionParams params) {
    if (isRetro(params.textDocument.uri)) {
      return this.retroCompletionFeature.handle(project, params);
    }
    return Optional.empty();
  }

  @Override
  public void initialized() {
    if (!new File(this.workspaceRoot.toString() + File.separator + "retro.json").exists()) {
      requestCreateRetroJson();
    } else {
      this.project = initializeProject.getProject();
    }

    LOG.info("initialized");
  }

  @Override
  public Optional<List<Location>> gotoDefinition(TextDocumentPositionParams params) {
    return this.gotoDefinitionFeature.handle(project, params);
  }

  @Override
  public void shutdown() {}

  @Override
  public void didChangeConfiguration(DidChangeConfigurationParams params) {
    // LOG.info(String.valueOf(params.settings));
  }

  @Override
  public List<DocumentLink> documentLink(DocumentLinkParams params) {
    if (isRetro(params.textDocument.uri)) {
      return new ArrayList<>();
    } else {
      return this.documentLinkFeature.handle(project, params);
    }
  }

  @Override
  public void didSaveTextDocument(DidSaveTextDocumentParams params) {
    var uri = params.textDocument.uri.relativize(this.workspaceRoot).toString();

    project.parseFile(this.workspaceRoot, uri);
    updateDiagnostics(uri);
  }

  @Override
  public Optional<List<Location>> findReferences(ReferenceParams params) {
    if (isRetro(params.textDocument.uri)) {
      return Optional.empty();
    }
    return this.findReferenceFeature.handle(project, params);
  }

  private void requestCreateRetroJson() {
    var params = new ShowMessageRequestParams();
    params.message = "retro.json file is missing.  Please create it.";
    params.actions.add(item("Create retro.json"));
    requests.put(
        (Integer) client.showMessageRequest(params),
        (Object item) -> {
          if (item != null && ((MessageActionItem) item).title.equals("Create retro.json")) {
            var file = Paths.get(this.workspaceRoot).resolve("retro.json").toFile();
            try {
              file.createNewFile();
            } catch (IOException e) {
              LOG.log(Level.SEVERE, "Could not make retro.json", e);
            }
          }
          return null;
        });
  }

  private MessageActionItem item(String string) {
    var item = new MessageActionItem();
    item.title = string;
    return item;
  }

  @Override
  public void handleShowMessageRequestResponse(int id, MessageActionItem result) {
    var request = requests.get(id);
    request.apply(result);
  }

  @Override
  public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {

    params
        .changes
        .parallelStream()
        .forEach(
            change -> {
              switch (change.type) {
                case 1:
                case 2:
                  var fileName = this.workspaceRoot.relativize(change.uri).toString();
                  var root = this.workspaceRoot;

                  project.parseFile(root, fileName);
                  updateDiagnostics(fileName);
              }
            });
  }

  private void updateDiagnostics(String fileName) {
    LOG.info(String.format("updateDiagnostics(%s)", fileName));
    List<ErrorNode> errors = project.getErrors(fileName);
    LOG.info(String.format("updateDiagnostics errorsFound %d", errors.size()));
    List<Diagnostic> diagnostics = new ArrayList<>();

    errors
        .stream()
        .map(
            error -> {
              var d = new Diagnostic();
              d.source = error.getSourceToken().getString();
              d.range = toRange(error.getSourceToken());
              d.message = error.getException().getMessage();
              return d;
            })
        .forEach(diagnostics::add);

    var file = URI.create(workspaceRoot.toString() + "/" + (fileName));

    client.publishDiagnostics(new PublishDiagnosticsParams(file, diagnostics));
  }

  @Override
  public List<SymbolInformation> documentSymbol(DocumentSymbolParams params) {
    if (isRetro(params.textDocument.uri)) {
      return new ArrayList<>();
    }
    return documentSymbolFeature.handle(project, params);
  }

  private boolean isRetro(URI location) {
    return location.toString().endsWith("retro.json");
  }
}
