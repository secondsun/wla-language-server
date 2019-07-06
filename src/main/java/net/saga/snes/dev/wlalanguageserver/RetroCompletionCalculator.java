package net.saga.snes.dev.wlalanguageserver;

import dev.secondsun.lsp.*;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.FileInputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        var list = new CompletionList();
        list.items = new ArrayList<>();

        JsonObject value = Json.createReader(new StringReader(retrojsonFileContents)).readObject();

        if (!value.containsKey("main")) {
          addMain(list, position);
        }

        if (!value.containsKey("main-arch")) {
          addMainArch(list, position);
        }

        if (!value.containsKey("arch-roots")) {
          addArchRoots(list, position);
        }
      return Optional.of(list);
      } catch (Exception ex) {
        Logger.getAnonymousLogger().log(Level.SEVERE, "error completing retro.json", ex);
      }
    }
    return Optional.empty();
  }

  private void addArchRoots(CompletionList list, Position position) {
    var item = new CompletionItem();
    var edit = new TextEdit();
    var range = new Range();


    item.label = "arch-roots";
    item.kind = CompletionItemKind.Text;
    item.textEdit = edit;
    item.preselect = true;
    edit.newText = "\"arch-roots\" : []";
    edit.range = range;
    range.end = position;
    range.start = position;

    list.items.add(item);

  }

  private void addMainArch(CompletionList list, Position position) {
    var item = new CompletionItem();
    var edit = new TextEdit();
    var range = new Range();


    item.label = "main-arch";
    item.kind = CompletionItemKind.Text;
    item.textEdit = edit;
    item.preselect = true;
    edit.newText = "\"main-arch\" : \"\"";
    edit.range = range;
    range.end = position;
    range.start = position;

    list.items.add(item);
  }

  private void addMain(CompletionList list, Position position) {
    var item = new CompletionItem();
    var edit = new TextEdit();
    var range = new Range();


    item.label = "main";
    item.kind = CompletionItemKind.Text;
    item.textEdit = edit;
    item.preselect = true;
    edit.newText = "\"main\" : \"\"";
    edit.range = range;
    range.end = position;
    range.start = position;

    list.items.add(item);
  }
}
