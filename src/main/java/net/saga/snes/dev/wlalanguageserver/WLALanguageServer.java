package net.saga.snes.dev.wlalanguageserver;

import static net.saga.snes.dev.wlalanguageserver.Utils.toRange;

import com.google.gson.JsonObject;
import java.net.URI;
import java.util.*;
import java.util.logging.Logger;
import net.saga.snes.dev.wlalanguageserver.features.*;
import net.sagaoftherealms.tools.snes.assembler.main.Project;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.ErrorNode;
import org.javacs.lsp.*;

public class WLALanguageServer extends LanguageServer {

  private final LanguageClient client;
  private String workspaceRoot;

  private static final Logger LOG = Logger.getLogger(WLALanguageServer.class.getName());
  private Project project;

  private InitializeProject initializeProject;
  private DocumentLinkFeature documentLinkFeature = new DocumentLinkFeature();
  private GotoDefinitionFeature gotoDefinitionFeature = new GotoDefinitionFeature();
  private DocumentSymbolFeature documentSymbolFeature = new DocumentSymbolFeature();

  public WLALanguageServer(LanguageClient client) {

    this.client = client;
  }

  @Override
  public InitializeResult initialize(InitializeParams params) {
    this.workspaceRoot = params.rootUri.toString();
    List<Feature> features = features();

    this.initializeProject = new InitializeProject(workspaceRoot, features);

    var initializeData = new JsonObject();

    features.forEach(
        feature -> {
          feature.initializeFeature(workspaceRoot, initializeData);
        });

    return new InitializeResult(initializeData);
  }

  private List<Feature> features() {
    return Arrays.asList(
        this.documentLinkFeature, this.gotoDefinitionFeature, this.documentSymbolFeature);
  }

  @Override
  public void initialized() {

    this.project = initializeProject.getProject();

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
    return this.documentLinkFeature.handle(project, params);
  }

  @Override
  public void didSaveTextDocument(DidSaveTextDocumentParams params) {
    var uri =
        params
            .textDocument
            .uri
            .toString()
            .replace("file://", "")
            .replace(this.workspaceRoot + "/", "");
    var root = this.workspaceRoot.replace("file://", "");

    project.parseFile(root, uri);
    updateDiagnostics(uri);
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
                  var fileName =
                      change
                          .uri
                          .toString()
                          .replace("file://", "")
                          .replace(this.workspaceRoot, "")
                          .replaceFirst("/", "");
                  var root = this.workspaceRoot.replace("file://", "");

                  project.parseFile(root, fileName);
                  updateDiagnostics(fileName);
              }
            });
  }

  private void updateDiagnostics(String fileName) {
    List<ErrorNode> errors = project.getErrors(workspaceRoot + "/" + fileName);

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

    var file = URI.create("file://" + workspaceRoot + "/" + fileName);

    client.publishDiagnostics(new PublishDiagnosticsParams(file, diagnostics));
  }

  @Override
  public List<SymbolInformation> documentSymbol(DocumentSymbolParams params) {

    return documentSymbolFeature.handle(project, params);
  }
}
