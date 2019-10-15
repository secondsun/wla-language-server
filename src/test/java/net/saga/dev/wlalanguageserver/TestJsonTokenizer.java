package net.saga.dev.wlalanguageserver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Objects;
import org.junit.jupiter.api.Test;

public class TestJsonTokenizer {

    @Test
    public void testObjectStart() {
        String json = "{}";
        var tokenizer = new JsonTokenizer(json);
        assertEquals(new JsonToken("{",TokenTypes.OBJECT_START, 0,0), tokenizer.nextToken());
        assertEquals(new JsonToken("{",TokenTypes.OBJECT_START, 0,0), tokenizer.getToken());
        assertEquals(new JsonToken("}",TokenTypes.OBJECT_END, 0,1), tokenizer.nextToken());
    }

    @Test
    public void testObjectMultiline() {
        String json = "{\n}";
        var tokenizer = new JsonTokenizer(json);
        assertEquals(new JsonToken("{",TokenTypes.OBJECT_START, 0,0), tokenizer.nextToken());
        assertEquals(new JsonToken("}",TokenTypes.OBJECT_END, 1,0), tokenizer.nextToken());
    }

    public enum TokenTypes {
        ARRAY_START,ARRAY_END,OBJECT_END,OBJECT_START,COMMA, COMMA_END, COLON,EOF,NUMBER,STRING,TRUE,FALSE,NULL
    }

    public static class JsonToken {
        public JsonToken(String stringValue, TokenTypes type, int line, int column) {
            this.stringValue = stringValue;
            this.type = type;
            this.line = line;
            this.column = column;
        }

        public final TokenTypes type;
        public final String stringValue;
        public final int line, column;

        @Override
        public String toString() {
            return "JsonToken{" +
                "type=" + type +
                ", stringValue='" + stringValue + '\'' +
                ", line=" + line +
                ", column=" + column +
                '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            JsonToken jsonToken = (JsonToken) o;
            return line == jsonToken.line &&
                column == jsonToken.column &&
                type == jsonToken.type &&
                Objects.equals(stringValue, jsonToken.stringValue);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, stringValue, line, column);
        }
    }

    public static class JsonTokenizer {

        public static final String ARRAY_START ="[";
        public static final String ARRAY_END ="]";
        public static final String OBJECT_START ="{";
        public static final String OBJECT_END ="}";
        public static final String COMMA =",";
        public static final String COLON =":";
        public static final String QUOTE ="\"";
        
        

        public final String jsonDocument;
        public String jsonLine;
        public JsonToken token;
        public int line = 0;
        public int column = 0;
        public JsonTokenizer(String jsonDocument) {
            this.jsonDocument = jsonDocument;
            this.jsonLine = jsonDocument.split("\\n")[line];
        }

        public JsonToken nextToken() {
            // can I get the nex token?
            if (line >= jsonDocument.split("\\n").length) {
                this.token = new JsonToken("", TokenTypes.EOF, line,column);
            }
            if (this.token != null && this.token.type == TokenTypes.EOF) {
                return this.token;
            }

            //ok I can get a token
            switch (jsonLine.charAt(column) + "") {
                case OBJECT_START:
                    this.token =  new JsonToken("{", TokenTypes.OBJECT_START, line, column);
                    break;
                case OBJECT_END:
                    this.token =  new JsonToken("}", TokenTypes.OBJECT_END, line, column);
                    break;
            }

            column++;
            if (column >= this.jsonLine.length() || this.jsonLine.charAt(column) == '\n') {
                line++;
                column = 0;
                if (line >= jsonDocument.split("\\n").length) {
                } else {
                    this.jsonLine = jsonDocument.split("\\n")[line];
                }
            }
            return token;
        }


        public JsonToken getToken() {
            return token;
        }
    }

}