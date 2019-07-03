package net.saga.snes.dev.wlalanguageserver.features;

import static net.saga.snes.dev.wlalanguageserver.Utils.getNodeStream;
import static net.saga.snes.dev.wlalanguageserver.Utils.toRange;

import java.net.URI;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.json.JsonObjectBuilder;
import net.sagaoftherealms.tools.snes.assembler.main.Project;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.LabelDefinitionNode;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.MacroCallNode;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.Node;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.NodeTypes;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.directive.DirectiveNode;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.directive.definition.DefinitionNode;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.directive.definition.EnumNode;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.directive.definition.StructNode;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.directive.macro.MacroNode;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.directive.section.SectionNode;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.expression.IdentifierNode;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.visitor.Visitor;
import net.sagaoftherealms.tools.snes.assembler.pass.scan.token.Token;
import org.javacs.lsp.Location;
import org.javacs.lsp.TextDocumentPositionParams;

public class GotoDefinitionFeature
    implements Feature<TextDocumentPositionParams, Optional<List<Location>>> {
  private static final Logger LOG = Logger.getLogger(GotoDefinitionFeature.class.getName());

  private Map<String, List<Token>> directiveBasedDefinitions =
      new HashMap<>(); // Struct, macro, etc name:node, may have collisions across types (ie a
  // macoro and struct may share a name)
  private Map<String, Token> labelDefinitions = new HashMap<>(); // Labels name:node

  private URI workspaceRoot;

  @Override
  public void initializeFeature(URI workspaceRoot, JsonObjectBuilder initializeData) {
    this.workspaceRoot = workspaceRoot;
    initializeData.add("definitionProvider", true);
  }

  @Override
  public Visitor getFeatureVisitor() {
    return (node -> {
      String name = "";
      if (node.getType().equals(NodeTypes.LABEL_DEFINITION)) {
        LabelDefinitionNode labelDefNode = (LabelDefinitionNode) node;
        labelDefinitions.put(labelDefNode.getLabelName(), labelDefNode.getSourceToken());
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
    });
  }

  @Override
  public Optional<List<Location>> handle(Project project, TextDocumentPositionParams params) {
    var uri = params.textDocument.uri.toString().split(this.workspaceRoot.toString() + "/")[1];
    var line = params.position.line + 1;
    var column = params.position.character;

    Stream<Node> nodeStream = getNodeStream(uri, project);

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
              URI.create(this.workspaceRoot + "/" + definition.getFileName()), toRange(definition));
      return Optional.of(List.of(loc));
    } else {
      return Optional.empty();
    }
  }
}
