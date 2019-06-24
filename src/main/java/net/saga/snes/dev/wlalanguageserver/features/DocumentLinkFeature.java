package net.saga.snes.dev.wlalanguageserver.features;

import static net.saga.snes.dev.wlalanguageserver.Utils.getNodeStream;
import static net.saga.snes.dev.wlalanguageserver.Utils.toRange;

import com.google.gson.JsonObject;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;
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

  private URI workspaceRoot;

  @Override
  public void initializeFeature(URI workspaceRoot, JsonObject initializeData) {
    var documentLinkOptions = new JsonObject();
    documentLinkOptions.addProperty("resolveProvider", false);
    initializeData.add("documentLinkProvider", documentLinkOptions);
    this.workspaceRoot = workspaceRoot;
  }

  @Override
  public Visitor getFeatureVisitor() {
    return node -> {};
  }

  @Override
  public List<DocumentLink> handle(Project project, DocumentLinkParams documentLinkParams) {
    LOG.info("DocumentLInkFeature.handle workspaceRoot is " + this.workspaceRoot);
    LOG.info(
        "DocumentLInkFeature.handle uri passed in is "
            + documentLinkParams.textDocument.uri.toString());

    var uri = documentLinkParams.textDocument.uri.toString().split(this.workspaceRoot + "/")[1];
    var documentLinks = new ArrayList<DocumentLink>();

    Stream<Node> targetStream = getNodeStream(uri, project);

    LOG.info("Looking up " + uri);
    targetStream
        .filter(
            node ->
                node.getType().equals(NodeTypes.DIRECTIVE)
                    && ((DirectiveNode) node).getDirectiveType().equals(AllDirectives.INCLUDE))
        .forEach(
            (node) -> {
              var link = new DocumentLink();
              var arguments = ((DirectiveNode) node).getArguments();
              var argsToken = arguments.getChildren().get(0).getSourceToken();
              var range = toRange(argsToken);
              link.range = range;
              link.target =
                  this.workspaceRoot.toString() + "/" + arguments.getString(0).replace("\"", "");

              documentLinks.add(link);
            });

    return documentLinks;
  }
}
