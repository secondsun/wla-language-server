package net.saga.snes.dev.wlalanguageserver.features;

import static net.saga.snes.dev.wlalanguageserver.Utils.getNodeStream;
import static net.saga.snes.dev.wlalanguageserver.Utils.toRange;

import com.google.gson.JsonObject;
import dev.secondsun.lsp.DocumentSymbolParams;
import dev.secondsun.lsp.Location;
import dev.secondsun.lsp.SymbolInformation;
import dev.secondsun.wla4j.assembler.definition.directives.AllDirectives;
import dev.secondsun.wla4j.assembler.main.Project;
import dev.secondsun.wla4j.assembler.pass.parse.Node;
import dev.secondsun.wla4j.assembler.pass.parse.NodeTypes;
import dev.secondsun.wla4j.assembler.pass.parse.directive.DirectiveNode;
import dev.secondsun.wla4j.assembler.pass.parse.directive.definition.EnumNode;
import dev.secondsun.wla4j.assembler.pass.parse.directive.macro.MacroNode;
import dev.secondsun.wla4j.assembler.pass.parse.directive.section.SectionNode;
import dev.secondsun.wla4j.assembler.pass.parse.visitor.Visitor;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DocumentSymbolFeature implements Feature<DocumentSymbolParams, List<SymbolInformation>> {

    private final HashMap<String, List<Node>> includesNodes = new HashMap<>(100);

    private URI workspaceRoot;

    private static final Set<AllDirectives> namingDirectives = new HashSet<>();

    static {
        Collections.addAll(namingDirectives, AllDirectives.STRUCT, AllDirectives.SECTION, AllDirectives.DEFINE,
                AllDirectives.DEF, AllDirectives.DSTRUCT, AllDirectives.MACRO);
    }

    @Override
    public void initializeFeature(URI workspaceRoot, JsonObject initializeData) {
        initializeData.addProperty("documentSymbolProvider", true);
        this.workspaceRoot = workspaceRoot;
    }

    @Override
    public Visitor getFeatureVisitor() {
        return node -> {
        };
    }

    @Override
    public List<SymbolInformation> handle(Project project, DocumentSymbolParams params) {
        // We want to to filter out node types that don't provide symbols that are worth navigating to
        // IE opcodes, ifs etc
        final Set<NodeTypes> allowedSymbolNodeTypes = new HashSet<>();
        Collections.addAll(allowedSymbolNodeTypes, NodeTypes.DIRECTIVE, NodeTypes.SECTION, NodeTypes.LABEL_DEFINITION,
                NodeTypes.MACRO, NodeTypes.SLOT);

        var uri = params.textDocument.uri.toString().split(this.workspaceRoot.toString() + "/")[1];

        Stream<Node> targetStream = getNodeStream(uri, project);

        return new ArrayList<>(
                targetStream.filter(node -> allowedSymbolNodeTypes.contains(node.getType())).filter(node -> {
                    if (node.getType().equals(NodeTypes.DIRECTIVE)) {
                        return namingDirectives.contains(((DirectiveNode) node).getDirectiveType());
                    } else {
                        return true;
                    }
                }).filter(node -> !(null == node.getSourceToken().getString()
                        || node.getSourceToken().getString().isEmpty())).map((node) -> {
                            SymbolInformation si = new SymbolInformation();
                            si.location = new Location(URI.create(uri), toRange(node.getSourceToken()));
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
                        }).collect(Collectors.toSet()));
    }
}
