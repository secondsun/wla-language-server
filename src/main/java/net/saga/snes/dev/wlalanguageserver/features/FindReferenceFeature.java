package net.saga.snes.dev.wlalanguageserver.features;

import static net.saga.snes.dev.wlalanguageserver.Utils.getNodeStream;
import static net.saga.snes.dev.wlalanguageserver.Utils.toRange;

import dev.secondsun.lsp.Location;
import dev.secondsun.lsp.ReferenceParams;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.json.JsonObjectBuilder;
import net.saga.snes.dev.wlalanguageserver.Utils;
import net.sagaoftherealms.tools.snes.assembler.main.Project;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.Node;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.visitor.Visitor;

public class FindReferenceFeature implements Feature<ReferenceParams, Optional<List<Location>>> {

  private URI workspaceRoot;
  private static final Logger LOG = Logger.getLogger(FindReferenceFeature.class.getName());

  @Override
  public void initializeFeature(URI workspaceRoot, JsonObjectBuilder initializeData) {
    initializeData.add("referencesProvider", true);
    this.workspaceRoot = workspaceRoot;
  }

  @Override
  public Visitor getFeatureVisitor() {
    return node -> {};
  }

  @Override
  public Optional<List<Location>> handle(Project project, ReferenceParams params) {
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

                      return true;
                    }
                  }
                  return false;
                })
            .findFirst()
            .orElseGet(() -> null);

    if (element == null) {
      return Optional.empty();
    }

    var allFileKeys = project.getParsedFiles();
    LOG.info("Found element, @" + element.getSourceToken());
    LOG.info("Loading files " + allFileKeys.stream().collect(Collectors.joining(", ")));

    var result =
        allFileKeys
            // Map keys to All nodes in codespace
            .stream()
            .flatMap(fileName -> Utils.getNodeStream(fileName, project))
            // Collect nodes with the same token as the object we are trying to reference
            .filter(
                node -> {
                  LOG.info(node.toString());
                  return element
                      .getSourceToken()
                      .getString()
                      .replace(":", "")
                      .trim()
                      .equals(node.getSourceToken().getString().trim());
                })
            .collect(Collectors.toList())
            .stream()
            // turn those nodes into locations
            .map(Node::getSourceToken)
            .map(
                token ->
                    new Location(
                        URI.create(this.workspaceRoot + "/" + token.getFileName()), toRange(token)))
            .collect(Collectors.toList());

    return Optional.of(result);
  }
}
