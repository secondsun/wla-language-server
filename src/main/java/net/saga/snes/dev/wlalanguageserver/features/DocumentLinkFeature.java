package net.saga.snes.dev.wlalanguageserver.features;

import com.google.gson.JsonObject;
import net.sagaoftherealms.tools.snes.assembler.definition.directives.AllDirectives;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.Node;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.NodeTypes;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.directive.DirectiveNode;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.visitor.Visitor;
import org.javacs.lsp.DocumentLink;
import org.javacs.lsp.DocumentLinkParams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.saga.snes.dev.wlalanguageserver.WLALanguageServer.toRange;

public class DocumentLinkFeature implements Feature<DocumentLinkParams, List<DocumentLink>> {

    private final HashMap<String, List<Node>> includesNodes = new HashMap<>(100);
    private String workspaceRoot;

    @Override
    public void initializeFeature(String workspaceRoot, JsonObject initializeData) {
        var documentLinkOptions = new JsonObject();
        documentLinkOptions.addProperty("resolveProvider", false);
        initializeData.add("documentLinkProvider", documentLinkOptions);
        this.workspaceRoot = workspaceRoot;
    }

    @Override
    public Visitor getFeatureVisitor() {
        return node -> {
            var fileName = node.getSourceToken().getFileName();
            var list = includesNodes.getOrDefault(fileName, new ArrayList<>(50));
            if (node.getType().equals(NodeTypes.DIRECTIVE)
                    && ((DirectiveNode)node).getDirectiveType().equals(AllDirectives.INCLUDE)) {
                list.add(node);
                includesNodes.put(fileName, list);
            }
        };
    }

    @Override
    public void handle(DocumentLinkParams documentLinkParams, List<DocumentLink> documentLinks) {
        var uri = documentLinkParams.textDocument.uri.toString().replace("file://", "");

        List nodes = includesNodes.get(uri);

        nodes.forEach((node) -> {
            var link = new DocumentLink();
            var arguments = ((DirectiveNode) node).getArguments();
            var argsToken = arguments.getChildren().get(0).getSourceToken();
            var range = toRange(argsToken);
            link.range = range;
            link.target =
                    "file://"
                            + this.workspaceRoot
                            + "/"
                            + arguments.getString(0).replace("\"", "");

            documentLinks.add(link);
        });

    }

}
