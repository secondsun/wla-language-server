package net.saga.snes.dev.wlalanguageserver;

import dev.secondsun.lsp.*;
import java.util.ArrayList;
import java.util.Optional;

/** Calculates completionLists for retro.json */
public class RetroCompletionCalculator {
  private static final CompletionList CREATE_ROOT_OBJECT = createObjectList();

  private static CompletionList createObjectList() {
    var list = new CompletionList();
    var item = new CompletionItem();
    var edit = new TextEdit();
    var range = new Range();
    var position = new Position();

    item.label = "Create Body";
    item.kind = CompletionItemKind.Text;
    item.textEdit = edit;
    item.preselect = true;
    edit.newText = "{\n}";
    edit.range = range;
    range.end = position;
    range.start = position;
    position.character = 0;
    position.line = 0;

    list.items = new ArrayList<>();

    list.items.add(item);

    return list;
  }

  public Optional<CompletionList> calculate(String retrojsonFile, Position position) {
    if (retrojsonFile.isBlank()) {
      return Optional.of(CREATE_ROOT_OBJECT);
    }
    return Optional.empty();
  }
}
