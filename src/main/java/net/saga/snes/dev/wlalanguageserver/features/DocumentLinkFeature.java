package net.saga.snes.dev.wlalanguageserver.features;

import static net.saga.snes.dev.wlalanguageserver.Utils.toRange;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import net.sagaoftherealms.tools.snes.assembler.definition.directives.AllDirectives;
import net.sagaoftherealms.tools.snes.assembler.main.Project;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.Node;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.NodeTypes;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.directive.DirectiveNode;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.visitor.Visitor;
import org.javacs.lsp.DocumentLink;
import org.javacs.lsp.DocumentLinkParams;

public class DocumentLinkFeature implements Feature<DocumentLinkParams, List<DocumentLink>> {

  private static final Logger LOG = Logger.getLogger(DocumentLinkFeature.class.getName());

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
          && ((DirectiveNode) node).getDirectiveType().equals(AllDirectives.INCLUDE)) {
        list.add(node);
        includesNodes.put(fileName, list);
        LOG.info("Adding to documentlink file " + fileName);
      }
    };
  }

  @Override
  public List<DocumentLink> handle(Project project, DocumentLinkParams documentLinkParams) {
    var uri =
        documentLinkParams
            .textDocument
            .uri
            .toString()
            .replace("file://", "")
            .split(this.workspaceRoot + "/")[1];
    var documentLinks = new ArrayList<DocumentLink>();

    List nodes = includesNodes.getOrDefault(uri, new ArrayList<>());
    LOG.info("Looking up " + uri);
    nodes.forEach(
        (node) -> {
          var link = new DocumentLink();
          var arguments = ((DirectiveNode) node).getArguments();
          var argsToken = arguments.getChildren().get(0).getSourceToken();
          var range = toRange(argsToken);
          link.range = range;
          link.target =
              "file://" + this.workspaceRoot + "/" + arguments.getString(0).replace("\"", "");

          documentLinks.add(link);
        });

    return documentLinks;
  }
}
