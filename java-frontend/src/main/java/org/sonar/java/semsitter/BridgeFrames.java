/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.semsitter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Protobuf codec of {@code semsitter-bridge/proto/bridge.proto}. Field numbers live here and in
 * the Rust encoder so the CST framing can move into SemSitter without a second schema.
 */
final class BridgeFrames {

  private BridgeFrames() {
  }

  static byte[] analyze(int id, String root, Iterable<String> files, Iterable<String> classpath) {
    ByteSink analyze = new ByteSink();
    analyze.string(1, root);
    for (String file : files) {
      analyze.string(2, file);
    }
    for (String entry : classpath) {
      analyze.string(3, entry);
    }
    ByteSink out = id(id);
    out.message(2, analyze.bytes());
    return out.bytes();
  }

  static byte[] cst(int id, String path) {
    ByteSink body = new ByteSink();
    body.string(1, path);
    ByteSink out = id(id);
    out.message(3, body.bytes());
    return out.bytes();
  }

  static byte[] types(int id, Iterable<String> names) {
    ByteSink body = new ByteSink();
    for (String name : names) {
      body.string(1, name);
    }
    ByteSink out = id(id);
    out.message(4, body.bytes());
    return out.bytes();
  }

  static byte[] parse(int id, String source) {
    ByteSink body = new ByteSink();
    body.string(1, source);
    ByteSink out = id(id);
    out.message(5, body.bytes());
    return out.bytes();
  }

  static byte[] quit(int id) {
    ByteSink out = id(id);
    out.varint(6, 1);
    return out.bytes();
  }

  static JsonObject response(byte[] bytes) {
    Cursor cursor = new Cursor(bytes);
    JsonObject object = new JsonObject();
    object.addProperty("ok", false);
    while (cursor.remaining()) {
      long key = cursor.varint();
      int field = (int) (key >> 3);
      int wire = (int) (key & 7);
      switch (field) {
        case 1 -> object.addProperty("id", (int) cursor.varint());
        case 2 -> object.addProperty("ok", cursor.varint() != 0);
        case 3 -> object.addProperty("error", cursor.text());
        case 4 -> analyzeResult(object, cursor.bytes());
        case 5 -> object.add("cst", document(cursor.bytes()));
        case 6 -> object.add("types", JsonParser.parseString(cursor.text()));
        default -> cursor.skip(wire);
      }
    }
    return object;
  }

  private static void analyzeResult(JsonObject object, byte[] bytes) {
    Cursor cursor = new Cursor(bytes);
    JsonArray files = new JsonArray();
    while (cursor.remaining()) {
      long key = cursor.varint();
      int field = (int) (key >> 3);
      switch (field) {
        case 1 -> object.addProperty("root", cursor.text());
        case 2 -> files.add(cursor.text());
        default -> cursor.skip((int) (key & 7));
      }
    }
    object.add("files", files);
  }

  private static JsonObject document(byte[] bytes) {
    Cursor cursor = new Cursor(bytes);
    JsonObject object = new JsonObject();
    object.addProperty("hasError", false);
    object.addProperty("semantic", false);
    object.add("symbols", new JsonArray());
    object.add("facts", new JsonObject());
    while (cursor.remaining()) {
      long key = cursor.varint();
      int field = (int) (key >> 3);
      switch (field) {
        case 1 -> object.addProperty("hasError", cursor.varint() != 0);
        case 2 -> object.add("tree", node(cursor.bytes()));
        case 3 -> object.addProperty("path", cursor.text());
        case 4 -> object.addProperty("semantic", cursor.varint() != 0);
        case 5 -> object.add("symbols", JsonParser.parseString(cursor.text()));
        case 6 -> factEntry(object.getAsJsonObject("facts"), cursor.bytes());
        default -> cursor.skip((int) (key & 7));
      }
    }
    return object;
  }

  private static void factEntry(JsonObject facts, byte[] bytes) {
    Cursor cursor = new Cursor(bytes);
    String key = "";
    JsonObject fact = new JsonObject();
    while (cursor.remaining()) {
      long header = cursor.varint();
      int field = (int) (header >> 3);
      switch (field) {
        case 1 -> key = cursor.text();
        case 2 -> fact = fact(cursor.bytes());
        default -> cursor.skip((int) (header & 7));
      }
    }
    facts.add(key, fact);
  }

  private static JsonObject fact(byte[] bytes) {
    Cursor cursor = new Cursor(bytes);
    JsonObject object = new JsonObject();
    while (cursor.remaining()) {
      long header = cursor.varint();
      int field = (int) (header >> 3);
      switch (field) {
        case 1 -> object.addProperty("kind", cursor.text());
        case 2 -> object.addProperty("tier", cursor.text());
        case 3 -> object.addProperty("parent", cursor.text());
        case 4 -> prop(object, cursor.bytes());
        case 5 -> addString(object, "extends", cursor.text());
        case 6 -> addString(object, "implements", cursor.text());
        case 7 -> addString(object, "permits", cursor.text());
        case 8 -> addString(object, "returns", cursor.text());
        case 9 -> addString(object, "type", cursor.text());
        case 10 -> addString(object, "params", cursor.text());
        case 11 -> addString(object, "overrides", cursor.text());
        case 12 -> addString(object, "annotations", cursor.text());
        default -> cursor.skip((int) (header & 7));
      }
    }
    return object;
  }

  private static void prop(JsonObject fact, byte[] bytes) {
    Cursor cursor = new Cursor(bytes);
    String name = "";
    JsonArray values = new JsonArray();
    while (cursor.remaining()) {
      long header = cursor.varint();
      int field = (int) (header >> 3);
      switch (field) {
        case 1 -> name = cursor.text();
        case 2 -> values.add(cursor.text());
        default -> cursor.skip((int) (header & 7));
      }
    }
    if (!fact.has("props")) {
      fact.add("props", new JsonObject());
    }
    fact.getAsJsonObject("props").add(name, values);
  }

  private static JsonObject node(byte[] bytes) {
    Cursor cursor = new Cursor(bytes);
    JsonObject object = new JsonObject();
    long[] position = new long[4];
    long[] span = new long[2];
    boolean sawPosition = false;
    boolean sawSpan = false;
    while (cursor.remaining()) {
      long header = cursor.varint();
      int field = (int) (header >> 3);
      switch (field) {
        case 1 -> object.addProperty("k", cursor.text());
        case 2 -> {
          if (cursor.varint() != 0) {
            object.addProperty("n", true);
          }
        }
        case 3 -> object.addProperty("f", cursor.text());
        case 4 -> {
          if (cursor.varint() != 0) {
            object.addProperty("m", true);
          }
        }
        case 5, 6, 7, 8 -> {
          position[field - 5] = cursor.varint();
          sawPosition = true;
        }
        case 9, 10 -> {
          span[field - 9] = cursor.varint();
          sawSpan = true;
        }
        case 11 -> object.addProperty("t", cursor.text());
        case 12 -> add(object, "c", node(cursor.bytes()));
        case 13 -> addString(object, "decls", cursor.text());
        case 14 -> add(object, "refs", ref(cursor.bytes()));
        default -> cursor.skip((int) (header & 7));
      }
    }
    if (sawPosition) {
      JsonArray array = new JsonArray();
      for (long value : position) {
        array.add(value);
      }
      object.add("p", array);
    }
    if (sawSpan) {
      JsonArray array = new JsonArray();
      for (long value : span) {
        array.add(value);
      }
      object.add("b", array);
    }
    return object;
  }

  private static JsonObject ref(byte[] bytes) {
    Cursor cursor = new Cursor(bytes);
    JsonObject object = new JsonObject();
    while (cursor.remaining()) {
      long header = cursor.varint();
      int field = (int) (header >> 3);
      switch (field) {
        case 1 -> object.addProperty("access", cursor.text());
        case 2 -> object.addProperty("outcome", cursor.text());
        case 3 -> object.addProperty("target", cursor.text());
        case 4 -> object.addProperty("reason", cursor.text());
        case 5 -> {
          if (cursor.varint() != 0) {
            object.addProperty("bounded", true);
          }
        }
        case 6 -> object.add("facts", JsonParser.parseString(cursor.text()));
        default -> cursor.skip((int) (header & 7));
      }
    }
    return object;
  }

  private static void add(JsonObject object, String key, JsonElement element) {
    if (!object.has(key)) {
      object.add(key, new JsonArray());
    }
    object.getAsJsonArray(key).add(element);
  }

  private static void addString(JsonObject object, String key, String value) {
    if (!object.has(key)) {
      object.add(key, new JsonArray());
    }
    object.getAsJsonArray(key).add(value);
  }

  private static ByteSink id(int id) {
    ByteSink out = new ByteSink();
    out.varint(1, id);
    return out;
  }

  private static final class ByteSink {
    private final List<Byte> bytes = new ArrayList<>();

    void varint(int field, long value) {
      rawVarint(((long) field) << 3);
      rawVarint(value);
    }

    void string(int field, String value) {
      message(field, value.getBytes(StandardCharsets.UTF_8));
    }

    void message(int field, byte[] value) {
      rawVarint((((long) field) << 3) | 2);
      rawVarint(value.length);
      for (byte b : value) {
        bytes.add(b);
      }
    }

    byte[] bytes() {
      byte[] out = new byte[bytes.size()];
      for (int i = 0; i < bytes.size(); i++) {
        out[i] = bytes.get(i);
      }
      return out;
    }

    private void rawVarint(long value) {
      long rest = value;
      while (true) {
        int bite = (int) (rest & 0x7f);
        rest >>>= 7;
        if (rest != 0) {
          bite |= 0x80;
        }
        bytes.add((byte) bite);
        if (rest == 0) {
          return;
        }
      }
    }
  }

  private static final class Cursor {
    private final byte[] bytes;
    private int at;

    private Cursor(byte[] bytes) {
      this.bytes = bytes;
    }

    private boolean remaining() {
      return at < bytes.length;
    }

    private long varint() {
      long value = 0;
      int shift = 0;
      while (at < bytes.length && shift < 64) {
        int bite = bytes[at++] & 0xff;
        value |= (long) (bite & 0x7f) << shift;
        if ((bite & 0x80) == 0) {
          return value;
        }
        shift += 7;
      }
      throw new IllegalArgumentException("truncated varint");
    }

    private byte[] bytes() {
      int len = (int) varint();
      byte[] out = new byte[len];
      System.arraycopy(bytes, at, out, 0, len);
      at += len;
      return out;
    }

    private String text() {
      return new String(bytes(), StandardCharsets.UTF_8);
    }

    private void skip(int wire) {
      switch (wire) {
        case 0 -> varint();
        case 1 -> at += 8;
        case 2 -> at += (int) varint();
        case 5 -> at += 4;
        default -> throw new IllegalArgumentException("unsupported protobuf wire type " + wire);
      }
    }
  }
}
