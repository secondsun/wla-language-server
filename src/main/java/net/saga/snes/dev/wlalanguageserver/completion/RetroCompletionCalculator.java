package net.saga.snes.dev.wlalanguageserver.completion;

import dev.secondsun.lsp.*;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;

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

  public Optional<CompletionList> calculate(String retrojsonFileContents, Position position) {
    if (retrojsonFileContents.isBlank()) {
      return Optional.of(CREATE_ROOT_OBJECT);
    } else {
      try {

        var retroFile = Json.createReader(new StringReader(retrojsonFileContents)).readObject();
        var parser = Json.createParser(new StringReader(retrojsonFileContents));
        var normalizedPosition =
            new Position(
                position.line + 1,
                position.character + 1); // jsonParser uses one indexing, lsp uses 0.

        while (parser.hasNext()) {
          var start = parser.getLocation();
          var event = parser.next();
          var end = parser.getLocation();
          if (start.getLineNumber() <= normalizedPosition.line
              && (start.getLineNumber() < normalizedPosition.line
                  || (start.getLineNumber() == normalizedPosition.line
                      && start.getColumnNumber() <= normalizedPosition.character))
              && end.getLineNumber() >= normalizedPosition.line
              && (end.getLineNumber() > normalizedPosition.line
                  || (end.getLineNumber() == normalizedPosition.line
                      && end.getColumnNumber() >= normalizedPosition.character))) {
            switch (event) {
              
              case KEY_NAME:
                return completeKey(
                    parser.getString(),
                    new Position(
                        (int) start.getLineNumber() - 1, (int) start.getColumnNumber() - 1),
                    new Position((int) end.getLineNumber() - 1, (int) end.getColumnNumber() - 1));
              case START_OBJECT:
              case END_OBJECT:
              default:
                return completeKeys(retroFile, position);
            }
          }
        }

      } catch (Exception ex) {
        Logger.getAnonymousLogger().log(Level.SEVERE, "error completing retro.json", ex);
      }
    }
    return Optional.empty();
  }

  private Optional<CompletionList> completeKey(String string, Position start, Position end) {
    var list = new CompletionList();
    list.items = new ArrayList<>();

    for (RetroRootKeys key : RetroRootKeys.values()) {
      if (key.key.startsWith(string)) {
        addKey(list, start, end, key);
      }
    }

    return Optional.of(list);
  }

  private Optional<CompletionList> completeKeys(JsonObject retroFile, Position position) {
    var list = new CompletionList();
    list.items = new ArrayList<>();

    for (RetroRootKeys key : RetroRootKeys.values()) {
      if (!retroFile.containsKey(key.key)) {
        addEntry(list, position, key);
      }
    }

    return Optional.of(list);
  }

  private void addKey(CompletionList list, Position start, Position end, RetroRootKeys key) {
    var item = new CompletionItem();
    var edit = new TextEdit();
    var range = new Range();

    item.label = key.key;
    item.kind = CompletionItemKind.Text;
    item.textEdit = edit;
    item.preselect = true;
    edit.newText = String.format("\"%s\"", key.key);
    edit.range = range;
    range.end = end;
    range.start = start;

    list.items.add(item);
  }

  private void addEntry(CompletionList list, Position position, RetroRootKeys key) {
    var item = new CompletionItem();
    var edit = new TextEdit();
    var range = new Range();

    item.label = key.key;
    item.kind = CompletionItemKind.Text;
    item.textEdit = edit;
    item.preselect = true;
    edit.newText = String.format("\"%s\" : %s", key.key, key.calculateValue(""));
    edit.range = range;
    range.end = position;
    range.start = position;

    list.items.add(item);
  }
}
