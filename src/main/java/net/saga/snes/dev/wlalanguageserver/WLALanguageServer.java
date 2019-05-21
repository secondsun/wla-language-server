package net.saga.snes.dev.wlalanguageserver;

import com.google.gson.JsonObject;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.sagaoftherealms.tools.snes.assembler.definition.directives.AllDirectives;
import net.sagaoftherealms.tools.snes.assembler.main.Project;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.ErrorNode;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.Node;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.NodeTypes;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.directive.DirectiveNode;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.directive.definition.EnumNode;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.directive.macro.MacroNode;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.directive.section.SectionNode;
import net.sagaoftherealms.tools.snes.assembler.pass.scan.token.Token;
import net.sagaoftherealms.tools.snes.assembler.pass.scan.token.TokenTypes;
import org.javacs.lsp.*;

public class WLALanguageServer extends LanguageServer {

  private final LanguageClient client;
  private Path workspaceRoot;

  private static final Logger LOG = Logger.getLogger(WLALanguageServer.class.getName());
  private Project project;

  public WLALanguageServer(LanguageClient client) {

    this.client = client;
  }

  @Override
  public void initialized() {

    this.project = new Project.Builder(this.workspaceRoot.toString()).build();
    LOG.info("initialized");
    LOG.info(project.getParsedFiles().toString());
    project
        .getParsedFiles()
        .forEach(
            (file) -> {
              LOG.info(file);
              LOG.info("Nodes for file");

              LOG.info(
                  project
                      .getNodes(file)
                      .stream()
                      .map(Node::toString)
                      .collect(Collectors.joining("\n")));
            });
  }

  @Override
  public void shutdown() {}

  @Override
  public InitializeResult initialize(InitializeParams params) {
    this.workspaceRoot = Paths.get(params.rootUri);

    LOG.info(String.valueOf(workspaceRoot));
    LOG.info(String.valueOf(params));
    var c = new JsonObject();

    var documentLinkOptions = new JsonObject();
    documentLinkOptions.addProperty("resolveProvider", false);
    c.addProperty("documentSymbolProvider", true);
    c.add("documentLinkProvider", documentLinkOptions);

    return new InitializeResult(c);
  }

  @Override
  public void didChangeConfiguration(DidChangeConfigurationParams params) {
    LOG.info(String.valueOf(params.settings));
  }

  @Override
  public List<DocumentLink> documentLink(DocumentLinkParams params) {
    var uri = params.textDocument.uri.toString().replace("file://", "");

    Stream<Node> nodeStream = getNodeStream(uri);

    return nodeStream
        .filter(
            node ->
                node.getType() == NodeTypes.DIRECTIVE
                    && ((DirectiveNode) node).getDirectiveType().equals(AllDirectives.INCLUDE))
        .map(
            node -> {
              var link = new DocumentLink();
              var arguments = ((DirectiveNode) node).getArguments();
              var argsToken = arguments.getChildren().get(0).getSourceToken();
              var range = toRange(argsToken);
              LOG.info(
                  "document Link file://"
                      + this.workspaceRoot.toString()
                      + "/"
                      + arguments.getString(0).replace("\"", ""));
              LOG.info(
                  String.format(
                      " Document Link Range {%d:%d,%d:%d}",
                      range.start.line,
                      range.start.character,
                      range.end.line,
                      range.end.character));
              link.range = range;
              link.target =
                  "file://"
                      + this.workspaceRoot.toString()
                      + "/"
                      + arguments.getString(0).replace("\"", "");
              return link;
            })
        .collect(Collectors.toList());
  }

  @Override
  public void didSaveTextDocument(DidSaveTextDocumentParams params) {
    var uri =
        params
            .textDocument
            .uri
            .toString()
            .replace("file://", "")
            .replace(this.workspaceRoot.toString() + "/", "");
    var root = this.workspaceRoot.toString().replace("file://", "");

    project.parseFile(root, uri);
    publicDiagnostics(uri);
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
                          .replace(this.workspaceRoot.toString(), "")
                          .replaceFirst("/", "");
                  var root = this.workspaceRoot.toString().replace("file://", "");

                  project.parseFile(root, fileName);
                  publicDiagnostics(fileName);
              }
            });
  }

  private void publicDiagnostics(String fileName) {
    List<ErrorNode> errors = project.getErrors(workspaceRoot.toString() + "/" + fileName);

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

    var file = URI.create("file://" + workspaceRoot.toString() + "/" + fileName);
    LOG.info(String.format("{diagnosticsFile: %s}", file));

    client.publishDiagnostics(new PublishDiagnosticsParams(file, diagnostics));
  }

  @Override
  public List<SymbolInformation> documentSymbol(DocumentSymbolParams params) {

    // We want to to filter out node types that don't provide symbols that are worth navigating to
    // IE opcodes, ifs etc
    final Set<NodeTypes> allowedSymbolNodeTypes = new HashSet<>();
    Collections.addAll(
        allowedSymbolNodeTypes,
        NodeTypes.DIRECTIVE,
        NodeTypes.ENUM,
        NodeTypes.SECTION,
        NodeTypes.LABEL_DEFINITION,
        NodeTypes.MACRO,
        NodeTypes.SLOT);

    final Set<AllDirectives> allowedDirectiveTypes = new HashSet<>();
    Collections.addAll(
        allowedDirectiveTypes, AllDirectives.STRUCT, AllDirectives.DEFINE, AllDirectives.MACRO);

    var uri = params.textDocument.uri.toString().replace("file://", "");

    Stream<Node> targetStream = getNodeStream(uri);

    return new ArrayList<>(
        targetStream
            .filter(node -> allowedSymbolNodeTypes.contains(node.getType()))
            .filter(
                node -> {
                  if (node.getType().equals(NodeTypes.DIRECTIVE)) {
                    return allowedDirectiveTypes.contains(
                        ((DirectiveNode) node).getDirectiveType());
                  } else {
                    if (node.getSourceToken().getString().equals("ldh")) {
                      LOG.info("Found a wrong typed symbol, should be opcode");
                      LOG.info(node.toString());
                      LOG.info(node.getSourceToken().toString());
                    }
                    return true;
                  }
                })
            .filter(
                node ->
                    !(null == node.getSourceToken().getString()
                        || node.getSourceToken().getString().isEmpty()))
            .map(
                (node) -> {
                  SymbolInformation si = new SymbolInformation();
                  si.location =
                      new Location(URI.create("file://" + uri), toRange(node.getSourceToken()));
                  si.name = node.getSourceToken().getString();
                  si.kind = 11;
                  switch (node.getType()) {
                    case DIRECTIVE_ARGUMENTS:
                      break;
                    case DIRECTIVE_BODY:
                      break;
                    case DIRECTIVE:
                      si.kind = 6;
                      switch (((DirectiveNode) node).getDirectiveType()) {
                        case STRUCT:
                        case DSTRUCT:
                        case DEFINE:
                          si.name = ((DirectiveNode) node).getArguments().getString(0);
                          break;
                        case MACRO:
                          if (!(null == node.getSourceToken().getString()
                              || node.getSourceToken().getString().isEmpty())) {
                            si.name = ((MacroNode) node).getName();
                          }
                          si.kind = 6;
                          break;
                        default:
                          break;
                      }
                      break;
                    case SECTION:
                      if (!(null == node.getSourceToken().getString()
                          || node.getSourceToken().getString().isEmpty())) {
                        si.name = ((SectionNode) node).getName();
                      }
                      break;
                    case LABEL:
                      si.kind = 13;
                      break;
                    case OPCODE:
                      si.kind = 14;
                      break;
                    case OPCODE_ARGUMENT:
                      break;
                    case NUMERIC_EXPRESION:
                      break;
                    case NUMERIC_CONSTANT:
                      si.kind = 16;
                      break;
                    case MACRO:
                      if (!(null == node.getSourceToken().getString()
                          || node.getSourceToken().getString().isEmpty())) {
                        si.name = ((MacroNode) node).getName();
                      }
                      si.kind = 6;
                      break;
                    case STRING_EXPRESSION:
                      si.kind = 15;
                      break;
                    case IDENTIFIER_EXPRESSION:
                      si.kind = 13;
                      break;
                    case LABEL_DEFINITION:
                      break;
                    case MACRO_CALL:
                      si.kind = 12;
                      break;
                    case SLOT:
                      break;
                    case MACRO_BODY:
                      break;
                    case ENUM:
                      si.name = ((EnumNode) node).getAddress();
                      si.kind = 10;
                      break;
                    default:
                      si.name = node.getSourceToken().getString();
                      break;
                  }
                  return si;
                })
            .collect(Collectors.toSet()));
  }

  private Stream<Node> getNodeStream(String uri) {
    var nodes = project.getNodes(String.valueOf(uri));
    if (nodes == null) {
      return StreamSupport.stream(new ArrayList<Node>().spliterator(), true);
    }

    var parentNode =
        new Node(
            NodeTypes.ERROR,
            new Token("", TokenTypes.ERROR, uri, new Token.Position(0, 0, 0, 0))) {};
    nodes.forEach(parentNode::addChild);

    Iterator<Node> sourceIterator = parentNode.iterator();

    Iterable<Node> iterable = () -> sourceIterator;
    return StreamSupport.stream(iterable.spliterator(), true);
  }

  private Range toRange(Token sourceToken) {
    var position = sourceToken.getPosition();
    var start = new Position(position.beginLine - 1, position.beginOffset);
    var end = new Position(position.getEndLine() - 1, position.getEndOffset());
    Range r = new Range(start, end);
    return r;
  }
}
