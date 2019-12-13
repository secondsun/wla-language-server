package net.saga.dev.wlalanguageserver;

import static net.saga.dev.wlalanguageserver.RetroJsonExamples.*;
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
    assertEquals("\"arch-roots\"", edit.newText);
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
    assertEquals("\"main-arch\"", edit.newText);
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
    assertEquals("\"main\"", edit.newText);
    assertEquals(line, edit.range.start.line);
    assertEquals(start, edit.range.start.character);
    assertEquals(line, edit.range.end.line);
    assertEquals(start, edit.range.end.character);
  }

  @Test
  public void suggestMainArch() {
    var parser = Json.createParser(new StringReader(SUGGEST_ARCH_ROOT));

    Position position = new Position(2, 1);
    RetroCompletionCalculator calculator = new RetroCompletionCalculator();
    var optionalList = calculator.calculate(SUGGEST_ARCH_ROOT, position);
    assertNotNull(optionalList);
    assertNotNull(optionalList.get());
    var list = optionalList.get();
    assertNotNull(list);
    assertEquals(3, list.items.size()); // main-arch and arch-roots, and main.

    var mainItem = list.items.get(1);
    assertEquals("\"main-arch\"", mainItem.textEdit.newText);
  }

  @Test
  public void suggestMainArchWhenDocumentIsWrong() {
    var parser = Json.createParser(new StringReader(SUGGEST_ARCH_ROOT_WITH_INCOMPLETE));

    Position position = new Position(2, 1);
    RetroCompletionCalculator calculator = new RetroCompletionCalculator();
    var optionalList = calculator.calculate(SUGGEST_ARCH_ROOT_WITH_INCOMPLETE, position);
    assertNotNull(optionalList);
    assertNotNull(optionalList.get());
    var list = optionalList.get();
    assertNotNull(list);
    assertEquals(3, list.items.size()); // main-arch and arch-roots, and main.

    var mainItem = list.items.get(1);
    assertEquals("\"main-arch\"", mainItem.textEdit.newText);
  }

  @Test
  public void suggestArches() {

    Position position = new Position(2, 13);
    RetroCompletionCalculator calculator = new RetroCompletionCalculator();
    var optionalList = calculator.calculate(SUGGEST_ARCHES, position);
    assertNotNull(optionalList);
    assertNotNull(optionalList.get());
    var list = optionalList.get();
    assertNotNull(list);
    assertEquals(4, list.items.size()); // main-arch and arch-roots, and main.

    var mainItem = list.items.get(1);
    assertEquals("\"spc700\"", mainItem.textEdit.newText);
  }

  @Test
  public void suggestArchRootObjectKeys() {

    Position position = new Position(3, 20);
    RetroCompletionCalculator calculator = new RetroCompletionCalculator();
    var optionalList = calculator.calculate(SUGGEST_ARCH_ROOT_OBJECT, position);
    assertNotNull(optionalList);
    assertNotNull(optionalList.get());
    var list = optionalList.get();
    assertNotNull(list);
    assertEquals(2, list.items.size()); // main-arch and arch-roots, and main.

    var mainItem = list.items.get(0);
    assertEquals("\"arch\"", mainItem.textEdit.newText);
    mainItem = list.items.get(1);
    assertEquals("\"path\"", mainItem.textEdit.newText);
  }

  @Test
  public void suggestArchRootObject() {

    Position position = new Position(3, 20);
    RetroCompletionCalculator calculator = new RetroCompletionCalculator();
    var optionalList = calculator.calculate(SUGGEST_ARCH_ROOTS, position);
    assertNotNull(optionalList);
    assertNotNull(optionalList.get());
    var list = optionalList.get();
    assertNotNull(list);
    assertEquals(1, list.items.size()); // main-arch and arch-roots, and main.

    var mainItem = list.items.get(0);
    assertEquals("{\n}", mainItem.textEdit.newText);
  }

  @Test
  public void suggestArchRootObjectKeys2() {

    Position position = new Position(4, 0);
    RetroCompletionCalculator calculator = new RetroCompletionCalculator();
    var optionalList = calculator.calculate(SUGGEST_ARCH_ROOT_OBJECT_KEYS, position);
    assertNotNull(optionalList);
    assertNotNull(optionalList.get());
    var list = optionalList.get();
    assertNotNull(list);
    assertEquals(2, list.items.size()); // main-arch and arch-roots, and main.

    var mainItem = list.items.get(0);
    assertEquals("\"arch\"", mainItem.textEdit.newText);
  }
}
