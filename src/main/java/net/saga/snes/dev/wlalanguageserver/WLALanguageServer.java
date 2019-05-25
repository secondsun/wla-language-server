package net.saga.snes.dev.wlalanguageserver;

import com.google.gson.Gson;
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
import net.sagaoftherealms.tools.snes.assembler.pass.parse.*;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.directive.DirectiveNode;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.directive.definition.DefinitionNode;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.directive.definition.EnumNode;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.directive.definition.StructNode;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.directive.macro.MacroNode;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.directive.section.SectionNode;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.expression.IdentifierNode;
import net.sagaoftherealms.tools.snes.assembler.pass.scan.token.Token;
import net.sagaoftherealms.tools.snes.assembler.pass.scan.token.TokenTypes;
import org.javacs.lsp.*;

public class WLALanguageServer extends LanguageServer {

  private static final Set<AllDirectives> namingDirectives = new HashSet<>();

  static {
    Collections.addAll(
        namingDirectives,
        AllDirectives.STRUCT,
        AllDirectives.SECTION,
        AllDirectives.DEFINE,
        AllDirectives.DEF,
        AllDirectives.DSTRUCT,
        AllDirectives.MACRO);
  }

  private final LanguageClient client;
  private Path workspaceRoot;

  private Map<String, List<Token>> directiveBasedDefinitions =
      new HashMap<>(); // Struct, macro, etc name:node, may have collisions across types (ie a
  // macoro and struct may share a name)
  private Map<String, Token> labelDefinitions = new HashMap<>(); // Labels name:node
  private Set<String> openDocs = new HashSet<>();

  private static final Logger LOG = Logger.getLogger(WLALanguageServer.class.getName());
  private Project project;

  public WLALanguageServer(LanguageClient client) {

    this.client = client;
  }

  @Override
  public void initialized() {

    this.project =
        new Project.Builder(this.workspaceRoot.toString())
            // Add definitions to directiveBasedDefinitions and labelDefinitions
            .addVisitor(
                (node -> {
                  String name = "";
                  if (node.getType().equals(NodeTypes.LABEL_DEFINITION)) {
                    LabelDefinitionNode labelDefNode = (LabelDefinitionNode) node;
                    labelDefinitions.put(
                        labelDefNode.getLabelName(), labelDefNode.getSourceToken());
                  } else if ((node.getType().equals(NodeTypes.DIRECTIVE))) {
                    DirectiveNode directiveNode = (DirectiveNode) node;

                    if (directiveNode instanceof EnumNode) {
                      var enumDirective = (EnumNode) directiveNode;

                      var enumBody = enumDirective.getBody();
                      enumBody
                          .getChildren()
                          .forEach(
                              child -> {
                                if (child instanceof DefinitionNode) {
                                  var label = ((DefinitionNode) child).getLabel();
                                  directiveBasedDefinitions.putIfAbsent(label, new ArrayList<>());
                                  directiveBasedDefinitions.get(label).add(child.getSourceToken());
                                }
                              });
                    } else {
                      switch (directiveNode.getDirectiveType()) {
                        case MACRO:
                          name = ((MacroNode) node).getName();
                          break;
                        case STRUCT:
                          name = ((StructNode) node).getName();
                          break;
                        case DEFINE:
                        case DEF:
                          name = ((DirectiveNode) node).getArguments().getString(0);
                          break;
                        case SECTION:
                          name = ((SectionNode) node).getName();
                          break;
                      }
                      if (!name.isEmpty()) {

                        directiveBasedDefinitions.putIfAbsent(name, new ArrayList<>());
                        directiveBasedDefinitions.get(name).add(directiveNode.getSourceToken());
                      }
                    }
                  }
                }))
            .build();

    LOG.info("initialized");
  }

  @Override
  public Optional<List<Location>> gotoDefinition(TextDocumentPositionParams params) {

    var uri = params.textDocument.uri.toString().replace("file://", "");
    var line = params.position.line + 1;
    var column = params.position.character;
    Stream<Node> nodeStream = getNodeStream(uri);

    var element =
        nodeStream
            .filter(
                node -> {
                  var token = node.getSourceToken();
                  if (token != null && token.getPosition() != null) {
                    var position = token.getPosition();
                    if (line == position.beginLine
                        && line == position.getEndLine()
                        && column >= position.beginOffset
                        && column <= position.getEndOffset()) {

                      return Set.of(
                              NodeTypes.MACRO_CALL,
                              NodeTypes.STRING_EXPRESSION,
                              NodeTypes
                                  .MACRO_BODY, // Right now, macro bodies are not parsed but only
                              // tokenized.
                              NodeTypes.IDENTIFIER_EXPRESSION,
                              NodeTypes.OPCODE_ARGUMENT)
                          .contains(node.getType());
                    }
                  }
                  return false;
                })
            .findFirst()
            .orElseGet(() -> null);

    if (element == null) {
      return Optional.empty();
    }

    String name;
    Token definition = null;

    switch (element.getType()) {
      case MACRO_CALL:
        name = ((MacroCallNode) element).getMacroNode();
        definition = directiveBasedDefinitions.get(name).get(0);
        break;
      case IDENTIFIER_EXPRESSION:
        name = ((IdentifierNode) element).getLabelName();
        definition = labelDefinitions.get(name);
        if (definition == null && directiveBasedDefinitions.containsKey(name)) {
          definition = directiveBasedDefinitions.get(name).get(0);
        }
        break;
      case MACRO_BODY:
      case OPCODE_ARGUMENT:
      case STRING_EXPRESSION:
        name = (element).getSourceToken().getString();
        definition = labelDefinitions.get(name);
        if (definition == null && directiveBasedDefinitions.containsKey(name)) {
          definition = directiveBasedDefinitions.get(name).get(0);
        }
        break;
    }

    if (definition != null) {
      Location loc =
          new Location(
              URI.create("file://" + this.workspaceRoot + "/" + definition.getFileName()),
              toRange(definition));
      return Optional.of(List.of(loc));
    } else {
      return Optional.empty();
    }
  }

  @Override
  public void shutdown() {}

  @Override
  public InitializeResult initialize(InitializeParams params) {
    this.workspaceRoot = Paths.get(params.rootUri);

    var c = new JsonObject();

    var documentLinkOptions = new JsonObject();
    documentLinkOptions.addProperty("resolveProvider", false);
    c.addProperty("documentSymbolProvider", true);
    c.addProperty("definitionProvider", true);
    c.add("documentLinkProvider", documentLinkOptions);

    return new InitializeResult(c);
  }

  @Override
  public void didChangeConfiguration(DidChangeConfigurationParams params) {
    // LOG.info(String.valueOf(params.settings));
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
                          .replace(this.workspaceRoot.toString(), "")
                          .replaceFirst("/", "");
                  var root = this.workspaceRoot.toString().replace("file://", "");

                  project.parseFile(root, fileName);
                  updateDiagnostics(fileName);
              }
            });
  }

  private void updateDiagnostics(String fileName) {
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
        NodeTypes.SECTION,
        NodeTypes.LABEL_DEFINITION,
        NodeTypes.MACRO,
        NodeTypes.SLOT);

    var uri = params.textDocument.uri.toString().replace("file://", "");

    Stream<Node> targetStream = getNodeStream(uri);

    return new ArrayList<>(
        targetStream
            .filter(node -> allowedSymbolNodeTypes.contains(node.getType()))
            .filter(
                node -> {
                  if (node.getType().equals(NodeTypes.DIRECTIVE)) {
                    return namingDirectives.contains(((DirectiveNode) node).getDirectiveType());
                  } else {
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
                        case SECTION:
                          si.name = ((SectionNode) node).getName() + "";
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
    Gson gson = new Gson();

    if (nodes == null) {
      return StreamSupport.stream(new ArrayList<Node>().spliterator(), false);
    }

    var parentNode =
        new Node(
            NodeTypes.ERROR,
            new Token("", TokenTypes.ERROR, uri, new Token.Position(0, 0, 0, 0))) {};
    nodes.forEach(parentNode::addChild);

    Iterator<Node> sourceIterator = parentNode.iterator();

    Iterable<Node> iterable = () -> sourceIterator;
    return StreamSupport.stream(iterable.spliterator(), false);
  }

  private Range toRange(Token sourceToken) {
    var position = sourceToken.getPosition();
    var start = new Position(position.beginLine - 1, position.beginOffset);
    var end = new Position(position.getEndLine() - 1, position.getEndOffset());
    Range r = new Range(start, end);
    return r;
  }
}
