package net.saga.dev.wlalanguageserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import dev.secondsun.lsp.CompletionItemKind;
import dev.secondsun.lsp.Position;
import dev.secondsun.lsp.TextEdit;
import net.saga.snes.dev.wlalanguageserver.RetroCompletionCalculator;
import org.junit.jupiter.api.Test;

public class TestRetroCompletion {

  public static final String EMPTY = "";

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
}
