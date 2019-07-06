package net.saga.dev.wlalanguageserver;

import dev.secondsun.lsp.CompletionItemKind;
import dev.secondsun.lsp.Position;
import dev.secondsun.lsp.TextEdit;
import net.saga.snes.dev.wlalanguageserver.RetroCompletionCalculator;
import org.junit.jupiter.api.Test;

import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

public class TestRetroCompletion {

    public static final String EMPTY = "";
    public static final String BLANK = "{\n}";
    public static final String BLANK_MAIN = "{\n" +
            "\"main\":\"\"\n" +
            "}";

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

        var main = list.items.get(1);
        assertEquals("main", main.label);
        assertEquals(CompletionItemKind.Text, main.kind);

        edit = main.textEdit;
        assertNotNull(edit);
        assertEquals("\"main\" : \"\"", edit.newText);
        assertEquals(1, edit.range.start.line);
        assertEquals(0, edit.range.start.character);
        assertEquals(1, edit.range.end.line);
        assertEquals(0, edit.range.end.character);

        var mainArch = list.items.get(2);
        assertEquals("main-arch", mainArch.label);
        assertEquals(CompletionItemKind.Text, mainArch.kind);

        assertEquals(true, mainArch.preselect);
        edit = mainArch.textEdit;
        assertNotNull(edit);
        assertEquals("\"main-arch\" : \"\"", edit.newText);
        assertEquals(1, edit.range.start.line);
        assertEquals(0, edit.range.start.character);
        assertEquals(1, edit.range.end.line);
        assertEquals(0, edit.range.end.character);

    }

    @Test
    public void positionInsideValidKeyReturnsEmpty() {
        fail("when I try to complete typing on the ma of main I should get empty");
    }

    @Test
    public void positionInsideInvalidKeyReturnsSomethingAppropriate() {
        fail("when I try to complete typing on the ma of ma I should get main");
    }

}
