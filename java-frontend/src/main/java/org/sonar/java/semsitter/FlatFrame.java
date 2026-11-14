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
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Decoder of the flat {@code get-file} frame, which yields the same JSON structure as {@link BridgeFrames#document}.
 *
 * <p>Every string of the frame is a position in one string table, and the node tree, the declarations and references,
 * the symbols and the graph facts are runs of varints over that table. The tree carries no text and no line or column:
 * both are derived from the source bytes of the file.
 */
final class FlatFrame {

  private static final int FIELD_EXTENDS = 5;
  private static final String[] EDGE_NAMES = {"extends", "implements", "permits", "returns", "type", "params", "overrides", "annotations"};
  private static final Set<String> LITERAL_KINDS = Set.of(
    "string_literal", "character_literal", "text_block", "decimal_integer_literal", "hex_integer_literal",
    "octal_integer_literal", "binary_integer_literal", "decimal_floating_point_literal", "hex_floating_point_literal");

  private static final int FLAG_NAMED = 1;
  private static final int FLAG_MISSING = 2;
  private static final int FLAG_FIELD = 4;

  private FlatFrame() {
  }

  /** The flat {@code Document} of a file, given the bytes of that file's source. */
  static JsonObject document(byte[] payload, byte[] source) {
    BridgeFrames.Cursor cursor = new BridgeFrames.Cursor(payload);
    List<String> strings = new ArrayList<>();
    byte[] nodes = new byte[0];
    byte[] semantics = new byte[0];
    byte[] symbols = null;
    byte[] items = new byte[0];
    JsonObject object = new JsonObject();
    object.addProperty("hasError", false);
    object.addProperty("semantic", false);
    object.add("symbols", new JsonArray());
    object.add("facts", new JsonObject());
    while (cursor.remaining()) {
      long key = cursor.varint();
      switch ((int) (key >> 3)) {
        case 1 -> object.addProperty("hasError", cursor.varint() != 0);
        case 3 -> object.addProperty("path", cursor.text());
        case 4 -> object.addProperty("semantic", cursor.varint() != 0);
        case 5 -> object.add("symbols", JsonParser.parseString(cursor.text()));
        case 7 -> strings.add(cursor.text());
        case 8 -> nodes = cursor.bytes();
        case 10 -> semantics = cursor.bytes();
        case 11 -> symbols = cursor.bytes();
        case 12 -> items = cursor.bytes();
        default -> cursor.skip((int) (key & 7));
      }
    }
    Table table = new Table(strings);
    if (symbols != null) {
      object.add("symbols", symbols(new BridgeFrames.Cursor(symbols), table));
    }
    object.add("facts", items(new BridgeFrames.Cursor(items), table));
    if (nodes.length > 0) {
      object.add("tree", tree(new BridgeFrames.Cursor(nodes), semantics(new BridgeFrames.Cursor(semantics), table), table, source));
    }
    return object;
  }

  private record NodeSemantics(List<String> decls, JsonArray refs) {
  }

  private static Map<Long, NodeSemantics> semantics(BridgeFrames.Cursor run, Table table) {
    Map<Long, NodeSemantics> result = new HashMap<>();
    long node = 0;
    while (run.remaining()) {
      node += run.varint();
      List<String> decls = new ArrayList<>();
      for (long i = run.varint(); i > 0; i--) {
        decls.add(table.string(run));
      }
      JsonArray refs = new JsonArray();
      for (long i = run.varint(); i > 0; i--) {
        JsonObject ref = new JsonObject();
        for (String name : new String[] {"access", "outcome", "target", "reason"}) {
          String text = table.optional(run);
          if (text != null) {
            ref.addProperty(name, text);
          }
        }
        if (run.varint() == 1) {
          ref.addProperty("bounded", true);
        }
        JsonElement facts = table.value(run);
        if (facts != null) {
          ref.add("facts", facts);
        }
        refs.add(ref);
      }
      result.put(node, new NodeSemantics(decls, refs));
    }
    return result;
  }

  private static JsonObject tree(BridgeFrames.Cursor run, Map<Long, NodeSemantics> semantics, Table table, byte[] source) {
    Lines lines = new Lines(source);
    JsonObject root = null;
    Deque<Pending> open = new ArrayDeque<>();
    long start = 0;
    for (long index = 0; run.remaining(); index++) {
      long header = run.varint();
      int flags = (int) (header & 7);
      String kind = table.get(header >> 3);
      String field = (flags & FLAG_FIELD) != 0 ? table.get(run.varint()) : null;
      start += run.varint();
      long end = start + run.varint();
      long children = run.varint();
      if (end > source.length) {
        throw new IllegalArgumentException("node beyond the end of the source");
      }

      JsonObject node = new JsonObject();
      node.addProperty("k", kind);
      if ((flags & FLAG_NAMED) != 0) {
        node.addProperty("n", true);
      }
      if (field != null) {
        node.addProperty("f", field);
      }
      if ((flags & FLAG_MISSING) != 0) {
        node.addProperty("m", true);
      }
      JsonArray position = new JsonArray();
      int[] from = lines.position(source, (int) start);
      int[] to = lines.position(source, (int) end);
      position.add(from[0]);
      position.add(from[1]);
      position.add(to[0]);
      position.add(to[1]);
      node.add("p", position);
      JsonArray span = new JsonArray();
      span.add(start);
      span.add(end);
      node.add("b", span);
      if (children == 0 || LITERAL_KINDS.contains(kind)) {
        node.addProperty("t", new String(source, (int) start, (int) (end - start), StandardCharsets.UTF_8));
      }
      NodeSemantics own = semantics.get(index);
      if (own != null) {
        if (!own.decls().isEmpty()) {
          JsonArray decls = new JsonArray();
          own.decls().forEach(decls::add);
          node.add("decls", decls);
        }
        if (!own.refs().isEmpty()) {
          node.add("refs", own.refs());
        }
      }

      Pending parent = open.peek();
      if (parent == null) {
        root = node;
      } else {
        if (!parent.node.has("c")) {
          parent.node.add("c", new JsonArray());
        }
        parent.node.getAsJsonArray("c").add(node);
        parent.left--;
      }
      if (children > 0) {
        open.push(new Pending(node, children));
      }
      while (!open.isEmpty() && open.peek().left == 0) {
        open.pop();
      }
    }
    if (root == null || !open.isEmpty()) {
      throw new IllegalArgumentException("truncated node stream");
    }
    return root;
  }

  private static final class Pending {
    private final JsonObject node;
    private long left;

    private Pending(JsonObject node, long left) {
      this.node = node;
      this.left = left;
    }
  }

  private static JsonArray symbols(BridgeFrames.Cursor run, Table table) {
    JsonArray symbols = new JsonArray();
    while (run.remaining()) {
      JsonObject symbol = new JsonObject();
      symbol.addProperty("id", table.string(run));
      symbol.addProperty("kind", table.string(run));
      symbol.addProperty("name", table.string(run));
      long start = run.varint();
      if (start == 0) {
        for (String name : new String[] {"start", "end", "line", "col"}) {
          symbol.add(name, JsonNull.INSTANCE);
        }
      } else {
        symbol.addProperty("start", start - 1);
        symbol.addProperty("end", run.varint());
        symbol.addProperty("line", run.varint());
        symbol.addProperty("col", run.varint());
      }
      String owner = table.optional(run);
      symbol.add("owner", owner == null ? JsonNull.INSTANCE : new JsonPrimitive(owner));
      JsonElement facts = table.value(run);
      symbol.add("facts", facts == null ? JsonNull.INSTANCE : facts);
      symbols.add(symbol);
    }
    return symbols;
  }

  private static JsonObject items(BridgeFrames.Cursor run, Table table) {
    JsonObject facts = new JsonObject();
    while (run.remaining()) {
      String id = table.string(run);
      JsonObject fact = new JsonObject();
      for (String name : new String[] {"kind", "tier", "parent"}) {
        String text = table.optional(run);
        if (text != null) {
          fact.addProperty(name, text);
        }
      }
      for (long i = run.varint(); i > 0; i--) {
        String name = table.string(run);
        JsonArray values = new JsonArray();
        for (long j = run.varint(); j > 0; j--) {
          values.add(table.string(run));
        }
        if (!fact.has("props")) {
          fact.add("props", new JsonObject());
        }
        fact.getAsJsonObject("props").add(name, values);
      }
      for (long i = run.varint(); i > 0; i--) {
        int number = (int) run.varint() - FIELD_EXTENDS;
        if (number < 0 || number >= EDGE_NAMES.length) {
          throw new IllegalArgumentException("unknown edge group " + (number + FIELD_EXTENDS));
        }
        for (long j = run.varint(); j > 0; j--) {
          if (!fact.has(EDGE_NAMES[number])) {
            fact.add(EDGE_NAMES[number], new JsonArray());
          }
          fact.getAsJsonArray(EDGE_NAMES[number]).add(table.string(run));
        }
      }
      facts.add(id, fact);
    }
    return facts;
  }

  /** The string table, and the readers of what the runs say over it. */
  private record Table(List<String> strings) {

    String get(long position) {
      if (position < 0 || position >= strings.size()) {
        throw new IllegalArgumentException("string " + position + " is not in the table");
      }
      return strings.get((int) position);
    }

    String string(BridgeFrames.Cursor run) {
      return get(run.varint());
    }

    /** A string position plus one, {@code 0} meaning none. */
    String optional(BridgeFrames.Cursor run) {
      long position = run.varint();
      return position == 0 ? null : get(position - 1);
    }

    /** One value, or null for the none tag. */
    JsonElement value(BridgeFrames.Cursor run) {
      long header = run.varint();
      long n = header >>> 3;
      switch ((int) (header & 7)) {
        case 0:
          return null;
        case 1:
          return n == 0 ? JsonNull.INSTANCE : new JsonPrimitive(n == 2);
        case 2:
          return new JsonPrimitive(run.varint());
        case 3:
          return new JsonPrimitive(-(run.varint() + 1));
        case 4:
          return number(get(n));
        case 5:
          return new JsonPrimitive(get(n));
        case 6:
          JsonArray array = new JsonArray();
          for (long i = n; i > 0; i--) {
            array.add(orNull(value(run)));
          }
          return array;
        case 7:
          JsonObject object = new JsonObject();
          for (long i = n; i > 0; i--) {
            String key = string(run);
            object.add(key, orNull(value(run)));
          }
          return object;
        default:
          throw new IllegalArgumentException("unknown value tag " + (header & 7));
      }
    }

    private static JsonElement orNull(JsonElement element) {
      return element == null ? JsonNull.INSTANCE : element;
    }

    /** A number no integer tag holds; a text JSON cannot read as a number stays a string. */
    private static JsonElement number(String text) {
      try {
        JsonElement parsed = JsonParser.parseString(text);
        return parsed.isJsonPrimitive() ? parsed : new JsonPrimitive(text);
      } catch (RuntimeException e) {
        return new JsonPrimitive(text);
      }
    }
  }

  /** Line starts of a source: a line ends at {@code \n}, {@code \r\n} or a lone {@code \r}. */
  private static final class Lines {
    private final int[] starts;

    private Lines(byte[] source) {
      List<Integer> found = new ArrayList<>();
      found.add(0);
      for (int i = 0; i < source.length; i++) {
        if (source[i] == '\n' || (source[i] == '\r' && (i + 1 >= source.length || source[i + 1] != '\n'))) {
          found.add(i + 1);
        }
      }
      starts = found.stream().mapToInt(Integer::intValue).toArray();
    }

    /** The 1-based line and the column, in UTF-16 units from the line start, of a byte offset. */
    int[] position(byte[] source, int offset) {
      int low = 0;
      int high = starts.length - 1;
      while (low < high) {
        int middle = (low + high + 1) >>> 1;
        if (starts[middle] <= offset) {
          low = middle;
        } else {
          high = middle - 1;
        }
      }
      int lineStart = starts[low];
      boolean ascii = true;
      for (int i = lineStart; i < offset && ascii; i++) {
        ascii = source[i] >= 0;
      }
      int column;
      if (ascii) {
        column = offset - lineStart;
      } else {
        String prefix = new String(source, lineStart, offset - lineStart, StandardCharsets.UTF_8);
        column = (prefix.startsWith("﻿") ? prefix.substring(1) : prefix).length();
      }
      return new int[] {low + 1, column};
    }
  }
}
