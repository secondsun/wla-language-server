package net.saga.snes.dev.wlalanguageserver;

import com.google.gson.JsonObject;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.sagaoftherealms.tools.snes.assembler.definition.opcodes.OpCodeZ80;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.MultiFileParser;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.directive.definition.EnumNode;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.directive.macro.MacroNode;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.directive.section.SectionNode;
import net.sagaoftherealms.tools.snes.assembler.pass.scan.token.Token;
import org.javacs.lsp.DocumentSymbolParams;
import org.javacs.lsp.InitializeParams;
import org.javacs.lsp.InitializeResult;
import org.javacs.lsp.LanguageClient;
import org.javacs.lsp.LanguageServer;
import org.javacs.lsp.Location;
import org.javacs.lsp.Position;
import org.javacs.lsp.Range;
import org.javacs.lsp.SymbolInformation;

public class WLALanguageServer extends LanguageServer {

  private final LanguageClient client;
  private Path workspaceRoot;

  private static final Logger LOG = Logger.getLogger(WLALanguageServer.class.getName());

  private MultiFileParser parser;

  public WLALanguageServer(LanguageClient client) {

    this.client = client;
  }

  @Override
  public void initialized() {
    this.parser = createParser();
  }

  private MultiFileParser createParser() {
    return new MultiFileParser(OpCodeZ80.opcodes());
  }

  @Override
  public void shutdown() {}

  @Override
  public InitializeResult initialize(InitializeParams params) {
    this.workspaceRoot = Paths.get(params.rootUri);

    LOG.info(String.valueOf(workspaceRoot));
    LOG.info(String.valueOf(params));
    var c = new JsonObject();
    c.addProperty("documentSymbolProvider", true);

    return new InitializeResult(c);
  }

  @Override
  public List<SymbolInformation> documentSymbol(DocumentSymbolParams params) {
    var uri = params.textDocument.uri.toString().replace("file://", "");
    parser.parse(this.workspaceRoot.toString().replace("file://", ""), "main.s");
    LOG.info(String.valueOf(uri));
    LOG.info(parser.getParsedFiles().stream().collect(Collectors.joining("\n")));
    var result =
        parser
            .getNodes(String.valueOf(uri))
            .stream()
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
                      break;
                    case SECTION:
                      si.name = ((SectionNode) node).getName();
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
                      si.name = ((MacroNode) node).getName();
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
            .collect(Collectors.toList());

    return result;
  }

  private Range toRange(Token sourceToken) {
    var position = sourceToken.getPosition();
    var start = new Position(position.beginLine, position.beginOffset);
    var end = new Position(position.endLine, position.endOffset);
    Range r = new Range(start, end);
    return r;
  }
}
