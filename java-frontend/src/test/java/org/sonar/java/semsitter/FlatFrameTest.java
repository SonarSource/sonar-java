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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlatFrameTest {

  // a CRLF, then a two-byte character: a, \r\n, e-acute (2 bytes), b
  private static final byte[] SOURCE = "a\r\néb".getBytes(StandardCharsets.UTF_8);

  @Test
  void flat_frame_decodes_to_the_structure_of_the_tree_frame() {
    Frame frame = new Frame();
    // strings are interned in order of first use
    int unit = frame.string("unit");
    int identifier = frame.string("identifier");
    int name = frame.string("name");

    Out nodes = new Out();
    // root: named, spans the whole source, two children
    nodes.varint((unit << 3) | 1).varint(0).varint(SOURCE.length).varint(2);
    // first child: named, fills the "name" field, at offset 0
    nodes.varint((identifier << 3) | 1 | 4).varint(name).varint(0).varint(1).varint(0);
    // second child: missing, 3 bytes after the previous start
    nodes.varint((identifier << 3) | 2).varint(3).varint(3).varint(0);

    Out semantics = new Out();
    // node 1 declares an id
    semantics.varint(1).varint(1).varint(frame.string("p.A")).varint(0);
    // node 2 (one entry later) references with every optional text but the reason, bounded, and mixed facts
    semantics.varint(1).varint(0).varint(1)
      .varint(frame.string("r") + 1).varint(frame.string("bound") + 1).varint(frame.string("p.A") + 1).varint(0)
      .varint(1)
      .varint(7 | (6 << 3))
      .varint(frame.string("neg")).varint(3).varint(4)
      .varint(frame.string("big")).varint(2).varint(300)
      .varint(frame.string("exp")).varint(4 | (frame.string("1.5e3") << 3))
      .varint(frame.string("flag")).varint(1 | (2 << 3))
      .varint(frame.string("nothing")).varint(1)
      .varint(frame.string("list")).varint(6 | (2 << 3)).varint(5 | (frame.string("x") << 3)).varint(1 | (1 << 3));

    Out symbols = new Out();
    symbols.varint(frame.string("p.A")).varint(frame.string("class")).varint(frame.string("A"))
      .varint(1).varint(4).varint(1).varint(0).varint(0).varint(0);
    symbols.varint(frame.string("p.A#<init>()")).varint(frame.string("ctor")).varint(frame.string("<init>"))
      .varint(0).varint(frame.string("p.A") + 1).varint(7);

    Out items = new Out();
    items.varint(frame.string("p.A")).varint(frame.string("type") + 1).varint(0).varint(frame.string("p") + 1)
      .varint(1).varint(frame.string("mods")).varint(2).varint(frame.string("public")).varint(frame.string("final"))
      .varint(2)
      .varint(5).varint(1).varint(frame.string("p.Base"))
      .varint(12).varint(2).varint(frame.string("p.Ann")).varint(frame.string("p.Other"));

    byte[] payload = frame.document(nodes, semantics, symbols, items);

    JsonObject expected = JsonParser.parseString("""
      {
        "hasError": true,
        "semantic": true,
        "path": "a/A.java",
        "tree": {
          "k": "unit", "n": true, "p": [1, 0, 2, 2], "b": [0, 6],
          "c": [
            {"k": "identifier", "n": true, "f": "name", "p": [1, 0, 1, 1], "b": [0, 1], "t": "a", "decls": ["p.A"]},
            {"k": "identifier", "m": true, "p": [2, 0, 2, 2], "b": [3, 6], "t": "\\u00e9b",
             "refs": [{"access": "r", "outcome": "bound", "target": "p.A", "bounded": true,
                       "facts": {"neg": -5, "big": 300, "exp": 1.5e3, "flag": true, "nothing": null, "list": ["x", false]}}]}
          ]
        },
        "symbols": [
          {"id": "p.A", "kind": "class", "name": "A", "start": 0, "end": 4, "line": 1, "col": 0, "owner": null, "facts": null},
          {"id": "p.A#<init>()", "kind": "ctor", "name": "<init>", "start": null, "end": null, "line": null, "col": null,
           "owner": "p.A", "facts": {}}
        ],
        "facts": {
          "p.A": {"kind": "type", "parent": "p", "props": {"mods": ["public", "final"]},
                  "extends": ["p.Base"], "annotations": ["p.Ann", "p.Other"]}
        }
      }
      """).getAsJsonObject();

    assertThat(FlatFrame.document(payload, SOURCE)).isEqualTo(expected);
  }

  @Test
  void symbols_json_is_read_when_the_model_was_not_built_and_an_empty_frame_has_no_tree() {
    Frame frame = new Frame();
    byte[] payload = frame.raw(new Out().field(5, "[{\"id\":\"x\"}]").field(3, "B.java"));

    JsonObject document = FlatFrame.document(payload, SOURCE);

    assertThat(document.get("symbols").toString()).isEqualTo("[{\"id\":\"x\"}]");
    assertThat(document.has("tree")).isFalse();
    assertThat(document.get("hasError").getAsBoolean()).isFalse();
    assertThat(document.get("semantic").getAsBoolean()).isFalse();
  }

  @Test
  void a_node_stream_that_ends_early_or_leaves_the_source_is_refused() {
    Frame frame = new Frame();
    int unit = frame.string("unit");

    Out truncated = new Out().varint(unit << 3).varint(0).varint(1).varint(2);
    assertThatThrownBy(() -> FlatFrame.document(frame.document(truncated, new Out(), new Out(), new Out()), SOURCE))
      .isInstanceOf(IllegalArgumentException.class);

    Out beyond = new Out().varint(unit << 3).varint(0).varint(SOURCE.length + 1).varint(0);
    assertThatThrownBy(() -> FlatFrame.document(frame.document(beyond, new Out(), new Out(), new Out()), SOURCE))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void the_flat_frame_is_asked_for_from_protocol_1_8_on() {
    assertThat(AnalyzerSession.atLeastMinor("1.8.0", 8)).isTrue();
    assertThat(AnalyzerSession.atLeastMinor("1.10.2", 8)).isTrue();
    assertThat(AnalyzerSession.atLeastMinor("1.7.9", 8)).isFalse();
    assertThat(AnalyzerSession.atLeastMinor("2.0.0", 8)).isFalse();
    assertThat(AnalyzerSession.atLeastMinor("1", 8)).isFalse();
    assertThat(AnalyzerSession.atLeastMinor("1.x.0", 8)).isFalse();
  }

  private static final class Frame {
    private final List<String> strings = new ArrayList<>();

    int string(String text) {
      int at = strings.indexOf(text);
      if (at < 0) {
        strings.add(text);
        at = strings.size() - 1;
      }
      return at;
    }

    byte[] document(Out nodes, Out semantics, Out symbols, Out items) {
      Out out = new Out().varintField(1, 1).field(3, "a/A.java").varintField(4, 1);
      out.bytesField(8, nodes.bytes());
      out.bytesField(10, semantics.bytes());
      out.bytesField(11, symbols.bytes());
      out.bytesField(12, items.bytes());
      return raw(out);
    }

    byte[] raw(Out body) {
      Out out = new Out();
      for (String text : strings) {
        out.field(7, text);
      }
      out.raw(body.bytes());
      return out.bytes();
    }
  }

  private static final class Out {
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

    Out varint(long value) {
      long rest = value;
      while (rest >= 0x80) {
        bytes.write((int) ((rest & 0x7f) | 0x80));
        rest >>>= 7;
      }
      bytes.write((int) rest);
      return this;
    }

    Out varintField(int number, long value) {
      return varint((long) number << 3).varint(value);
    }

    Out bytesField(int number, byte[] value) {
      varint(((long) number << 3) | 2).varint(value.length);
      bytes.writeBytes(value);
      return this;
    }

    Out field(int number, String value) {
      return bytesField(number, value.getBytes(StandardCharsets.UTF_8));
    }

    Out raw(byte[] value) {
      bytes.writeBytes(value);
      return this;
    }

    byte[] bytes() {
      return bytes.toByteArray();
    }
  }
}
