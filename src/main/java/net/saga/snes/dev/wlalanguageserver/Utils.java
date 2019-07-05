package net.saga.snes.dev.wlalanguageserver;

import dev.secondsun.lsp.Position;
import dev.secondsun.lsp.Range;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.sagaoftherealms.tools.snes.assembler.main.Project;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.Node;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.NodeTypes;
import net.sagaoftherealms.tools.snes.assembler.pass.scan.token.Token;
import net.sagaoftherealms.tools.snes.assembler.pass.scan.token.TokenTypes;

public class Utils {

  public static Stream<Node> getNodeStream(String uri, Project project) {
    var nodes = project.getNodes(String.valueOf(uri));

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

  public static Range toRange(Token sourceToken) {
    var position = sourceToken.getPosition();
    var start = new Position(position.beginLine - 1, position.beginOffset);
    var end = new Position(position.getEndLine() - 1, position.getEndOffset());
    Range r = new Range(start, end);
    return r;
  }
}
