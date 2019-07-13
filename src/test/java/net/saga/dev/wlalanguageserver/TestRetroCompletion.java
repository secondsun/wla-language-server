package net.saga.dev.wlalanguageserver;

import static org.junit.jupiter.api.Assertions.*;

import dev.secondsun.lsp.CompletionItemKind;
import dev.secondsun.lsp.CompletionList;
import dev.secondsun.lsp.Position;
import dev.secondsun.lsp.TextEdit;
import java.io.StringReader;
import java.util.Comparator;
import javax.json.Json;
import net.saga.snes.dev.wlalanguageserver.completion.RetroCompletionCalculator;
import org.junit.jupiter.api.Test;

public class TestRetroCompletion {

  public static final String EMPTY = "";
  public static final String BLANK = "{\n}";
  public static final String BLANK_MAIN = "{\n" + "\"main\" :\"\"\n" + "}";
  public static final String BLANK_MA = "{\n" + "\"ma\" :\"\"\n" + "}";

  @Test
  public void testEmptyFileCompletesWithBraces() {
    Position position = new Position(0, 0);
    RetroCompletionCalculator calculator = new RetroCompletionCalculator();
    var optionalList = calculator.calculate(EMPTY, position);
    assertNotNull(optionalList);
    assertNotNull(optionalList.get());
    var list = optionalList.get();
    assertNotNull(list);
    assertEquals(1, list.items.size());
    var item = list.items.get(0);
    assertEquals("Create Body", item.label);
    assertEquals(CompletionItemKind.Text, item.kind);

    assertEquals(true, item.preselect);

    TextEdit edit = item.textEdit;
    assertNotNull(edit);
    assertEquals("{\n}", edit.newText);
    assertEquals(0, edit.range.start.line);
    assertEquals(0, edit.range.start.character);
    assertEquals(0, edit.range.end.line);
    assertEquals(0, edit.range.end.character);
  }

  @Test
  public void suggestTopLevelComponentsWhenBlank() {
    Position position = new Position(1, 0);
    RetroCompletionCalculator calculator = new RetroCompletionCalculator();
    var optionalList = calculator.calculate(BLANK, position);
    assertNotNull(optionalList);
    assertNotNull(optionalList.get());
    var list = optionalList.get();
    assertNotNull(list);
    assertEquals(3, list.items.size());
    list.items.sort(Comparator.comparing(item -> item.label));

    var archRoots = list.items.get(0);
    assertEquals("arch-roots", archRoots.label);
    assertEquals(CompletionItemKind.Text, archRoots.kind);

    TextEdit edit = archRoots.textEdit;
    assertNotNull(edit);
    assertEquals("\"arch-roots\" : []", edit.newText);
    assertEquals(1, edit.range.start.line);
    assertEquals(0, edit.range.start.character);
    assertEquals(1, edit.range.end.line);
    assertEquals(0, edit.range.end.character);

    checkMain(list, 1, 1, 0);
    checkMainArch(list, 2, 1, 0);
  }

  private void checkMainArch(CompletionList list, int index, int line, int start) {
    var mainArch = list.items.get(index);
    assertEquals("main-arch", mainArch.label);
    assertEquals(CompletionItemKind.Text, mainArch.kind);

    assertEquals(true, mainArch.preselect);
    var edit = mainArch.textEdit;
    assertNotNull(edit);
    assertEquals("\"main-arch\" : \"\"", edit.newText);
    assertEquals(line, edit.range.start.line);
    assertEquals(start, edit.range.start.character);
    assertEquals(line, edit.range.end.line);
    assertEquals(start, edit.range.end.character);
  }

  private void checkMain(CompletionList list, int index, int line, int start) {
    var main = list.items.get(index);
    assertEquals("main", main.label);
    assertEquals(CompletionItemKind.Text, main.kind);

    var edit = main.textEdit;
    assertNotNull(edit);
    assertEquals("\"main\" : \"\"", edit.newText);
    assertEquals(line, edit.range.start.line);
    assertEquals(start, edit.range.start.character);
    assertEquals(line, edit.range.end.line);
    assertEquals(start, edit.range.end.character);
  }

  @Test
  public void positionInsideInvalidKeyReturnsSomethingAppropriate() {
    var parser = Json.createParser(new StringReader(BLANK_MA));
    while (parser.hasNext()) {
      var location1 = parser.getLocation();
      var event = parser.next();
      var location2 = parser.getLocation();

      System.out.println(
          "Start : " + location1.getLineNumber() + " @ " + location1.getColumnNumber());
      System.out.println("Event : " + event.name());
      System.out.println(
          "End : " + location2.getLineNumber() + " @ " + location2.getColumnNumber());
    }

    Position position = new Position(1, 2);
    RetroCompletionCalculator calculator = new RetroCompletionCalculator();
    var optionalList = calculator.calculate(BLANK_MA, position);
    assertNotNull(optionalList);
    assertNotNull(optionalList.get());
    var list = optionalList.get();
    assertNotNull(list);
    assertEquals(2, list.items.size());

    var mainItem = list.items.get(0);
    assertEquals("\"main\"", mainItem.textEdit.newText);
    assertEquals(1, mainItem.textEdit.range.start.line);
    assertEquals(0, mainItem.textEdit.range.start.character);
    assertEquals(4, mainItem.textEdit.range.end.character);
  }
}
