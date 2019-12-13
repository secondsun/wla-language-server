package net.saga.snes.dev.wlalanguageserver.completion;

import dev.secondsun.lsp.*;
import java.io.StringReader;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.stream.JsonLocation;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParsingException;

/** Calculates completionLists for retro.json */
public final class RetroCompletionCalculator {

  private enum State {
    NEEDS_OBJ,
    NEEDS_TOP_KEY,
    NEEDS_TOP_VALUE_MAIN,
    NEEDS_TOP_VALUE_MAIN_ARCH,
    NEEDS_ARCHROOT_KEY,
    NEEDS_ARCHROOT_VALUE,
    NEEDS_ARCHROOT_OBJECT,
    NEEDS_ARCHROOT_ARRAY,
    DONE
  }

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

  public static Optional<CompletionList> calculate(
      String retrojsonFileContents, Position position) {

    if (retrojsonFileContents.isBlank()) {
      return Optional.of(CREATE_ROOT_OBJECT);
    } else {
      try {

        var parser = Json.createParser(new StringReader(retrojsonFileContents));
        var normalizedPosition = normalize(position);

        final Stack<Map<String, Object>> objectStack = new Stack<Map<String, Object>>();

        try {

          State result = tokenize(parser, objectStack, normalizedPosition);

          switch (result) {
            case DONE:
              // Likely there was an error and there are no logical responses
              return Optional.empty();
            case NEEDS_OBJ:
            case NEEDS_ARCHROOT_OBJECT:
              return Optional.of(RetroCompletionCalculator.CREATE_ROOT_OBJECT);
            case NEEDS_TOP_KEY:
              return completeKeys(position);
            case NEEDS_ARCHROOT_ARRAY:
              return createArray(position);
            case NEEDS_TOP_VALUE_MAIN:
              return createMain(position);
            case NEEDS_TOP_VALUE_MAIN_ARCH:
              return createArchList(position);
            case NEEDS_ARCHROOT_KEY:
              return completeArchRootKeys(position);
            case NEEDS_ARCHROOT_VALUE:
              return createArchList(position);
          }

        } catch (JsonParsingException ex) {
          // calculate?
          throw ex;
        }

      } catch (Exception ex) {
        Logger.getAnonymousLogger().log(Level.SEVERE, "error completing retro.json", ex);
      }
    }
    return Optional.empty();
  }

  /**
   * //This sets the states of the parser and objectStack to our best guesses up //until the cursor
   * position is set. After this we will calculate our completions
   *
   * @param parser jsonParser
   * @param objectStack a stack of the object state up until the cursor
   * @param normalizedPosition
   */
  private static State tokenize(
      JsonParser parser, Stack<Map<String, Object>> objectStack, Position normalizedPosition) {
    State state = State.NEEDS_OBJ;

    String archRootKey = "";
    String keyName = "";

    JsonLocation start = null;
    JsonParser.Event event = null;
    JsonLocation end = null;
    Map<String, Object> currentObject = new HashMap<>();
    List<Map<String, Object>> archArray = new ArrayList<>();

    while (parser.hasNext()) {
      start = parser.getLocation();
      try {
        event = parser.next();
      } catch (JsonParsingException ex) {
        Logger.getAnonymousLogger().log(Level.SEVERE, ex.getMessage(), ex);
        return state; // We WERE expecting something, let's go with that
      }
      end = parser.getLocation();

      if (normalizedPosition.line <= start.getLineNumber()
          && normalizedPosition.character <= start.getColumnNumber()) {
        return state;
      }

      switch (state) {
        case NEEDS_OBJ:
          if (event == JsonParser.Event.START_OBJECT) {
            state = State.NEEDS_TOP_KEY;
            break;
          } else {
            return State.NEEDS_OBJ;
          }

        case NEEDS_TOP_KEY:
          if (event == JsonParser.Event.KEY_NAME) {
            keyName = parser.getString();
            switch (keyName) {
              case "main":
                state = State.NEEDS_TOP_VALUE_MAIN;
                break;
              case "arch-roots":
                state = State.NEEDS_ARCHROOT_ARRAY;
                break;
              case "main-arch":
                state = State.NEEDS_TOP_VALUE_MAIN_ARCH;
                break;
            }
            break;
          } else if (event == JsonParser.Event.END_OBJECT) {
            return State.DONE; // we reached the end without being triggered
          } else {
            return State.NEEDS_TOP_KEY;
          }
        case NEEDS_TOP_VALUE_MAIN:
          if (event == JsonParser.Event.VALUE_STRING) {
            state = State.NEEDS_TOP_KEY;
            currentObject.put(keyName, parser.getString());
            keyName = "";
          } else {
            return State.NEEDS_TOP_VALUE_MAIN;
          }
          break;
        case NEEDS_TOP_VALUE_MAIN_ARCH:
          if (event == JsonParser.Event.VALUE_STRING) {
            state = State.NEEDS_TOP_KEY;
            currentObject.put(keyName, parser.getString());
            keyName = "";
          } else {
            return State.NEEDS_TOP_VALUE_MAIN_ARCH;
          }
          break;
        case NEEDS_ARCHROOT_ARRAY:
          if (event == JsonParser.Event.START_ARRAY) {
            state = State.NEEDS_ARCHROOT_OBJECT;
            currentObject.put(keyName, archArray);
            keyName = "";
            archArray.add(new HashMap<>());
          } else {
            return State.NEEDS_ARCHROOT_ARRAY;
          }
          break;
        case NEEDS_ARCHROOT_OBJECT:
          if (event == JsonParser.Event.START_OBJECT) {
            state = State.NEEDS_ARCHROOT_KEY;
          } else if (event == JsonParser.Event.END_ARRAY) {
            currentObject.put("arch-roots", archArray);
            archArray = new ArrayList<>();
            state = State.NEEDS_TOP_KEY;
          } else {
            // TODO: Throw
            return State.NEEDS_ARCHROOT_OBJECT;
          }
          break;
        case NEEDS_ARCHROOT_KEY:
          if (event == JsonParser.Event.END_OBJECT) {
            archArray.add(new HashMap<>());
            state = State.NEEDS_ARCHROOT_OBJECT;
          } else if (event == JsonParser.Event.KEY_NAME) {
            archRootKey = parser.getString();
            switch (archRootKey) {
              case "path":
              case "arch":
                archRootKey = parser.getString();
                state = State.NEEDS_ARCHROOT_VALUE;
                break;
              default:
                // TODO: Exception
                return State.DONE;
            }
          } else {
            return State.NEEDS_ARCHROOT_KEY;
          }
          break;
        case NEEDS_ARCHROOT_VALUE:
          if (event == JsonParser.Event.VALUE_STRING) {
            switch (archRootKey) {
              case "path":
                archArray.get(archArray.size() - 1).put("path", parser.getString());
                archRootKey = "";
                state = State.NEEDS_ARCHROOT_KEY;
                break;
              case "arch":
                archArray.get(archArray.size() - 1).put("arch", parser.getString());
                archRootKey = "";
                state = State.NEEDS_ARCHROOT_KEY;
                break;
              default:
                // TODO: Exception
                return State.DONE;
            }
          } else {
            // TODO: Exception
            return State.DONE;
          }

          break;
        default:
          return State.DONE;
      }
    }
    return State.DONE;
  }

  private static boolean within(Position normalizedPosition, JsonLocation start, JsonLocation end) {
    return start.getLineNumber() <= normalizedPosition.line
        && (start.getLineNumber() < normalizedPosition.line
            || (start.getLineNumber() == normalizedPosition.line
                && start.getColumnNumber() <= normalizedPosition.character))
        && end.getLineNumber() >= normalizedPosition.line
        && (end.getLineNumber() > normalizedPosition.line
            || (end.getLineNumber() == normalizedPosition.line
                && end.getColumnNumber() >= normalizedPosition.character));
  }

  private static Position normalize(Position position) {
    return new Position(
        position.line + 1, position.character + 1); // jsonParser uses one indexing, lsp uses 0.
  }

  private static Optional<CompletionList> completeKey(String string, Position start, Position end) {
    var list = new CompletionList();
    list.items = new ArrayList<>();

    for (RetroRootKeys key : RetroRootKeys.values()) {
      if (key.key.startsWith(string)) {
        addQuotedString(list, start, end, key.key);
      }
    }

    return Optional.of(list);
  }

  private static Optional<CompletionList> createArchList(Position position) {
    var list = new CompletionList();
    list.items = new ArrayList<>();

    for (RetroArches key : RetroArches.values()) {
      addQuotedString(list, position, key.arch);
    }

    return Optional.of(list);
  }

  private static Optional<CompletionList> completeArchRootKeys(Position position) {
    var list = new CompletionList();
    list.items = new ArrayList<>();

    addQuotedString(list, position, "arch");
    addQuotedString(list, position, "path");

    return Optional.of(list);
  }

  private static Optional<CompletionList> completeKeys(Position position) {
    var list = new CompletionList();
    list.items = new ArrayList<>();

    for (RetroRootKeys key : RetroRootKeys.values()) {
      addQuotedString(list, position, key.key);
    }

    return Optional.of(list);
  }

  private static Optional<CompletionList> createArray(Position position) {
    var list = new CompletionList();
    list.items = new ArrayList<>();

    var item = new CompletionItem();
    var edit = new TextEdit();
    var range = new Range();

    item.label = "New Array";
    item.kind = CompletionItemKind.Text;
    item.textEdit = edit;
    item.preselect = true;
    edit.newText = "[]";
    edit.range = range;
    range.end = position;
    range.start = position;

    list.items.add(item);

    return Optional.of(list);
  }

  private static Optional<CompletionList> createMain(Position position) {
    var list = new CompletionList();
    list.items = new ArrayList<>();

    var item = new CompletionItem();
    var edit = new TextEdit();
    var range = new Range();

    item.label = "main.s";
    item.kind = CompletionItemKind.Text;
    item.textEdit = edit;
    item.preselect = true;
    edit.newText = "main.s";
    edit.range = range;
    range.end = position;
    range.start = position;

    list.items.add(item);

    return Optional.of(list);
  }

  private static void addQuotedString(
      CompletionList list, Position start, Position end, String key) {
    var item = new CompletionItem();
    var edit = new TextEdit();
    var range = new Range();

    item.label = key;
    item.kind = CompletionItemKind.Text;
    item.textEdit = edit;
    item.preselect = true;
    edit.newText = String.format("\"%s\"", key);
    edit.range = range;
    range.end = end;
    range.start = start;

    list.items.add(item);
  }

  private static void addQuotedString(CompletionList list, Position start, String key) {
    addQuotedString(list, start, start, key);
  }
}
