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
package org.sonar.java.caching;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.java.reporting.AnalyzerMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonCacheFormatTest {

  private static final int VERSION = 3;

  @Test
  void newDocument_stamps_the_version() {
    assertThat(JsonCacheFormat.newDocument(VERSION)).hasToString("{\"version\":3}");
  }

  @Test
  void document_round_trip() {
    var document = JsonCacheFormat.newDocument(VERSION);
    document.addProperty("greeting", "hello");

    var parsed = JsonCacheFormat.parseDocument(JsonCacheFormat.toBytes(document), VERSION);

    assertThat(JsonCacheFormat.requiredString(parsed, "greeting")).isEqualTo("hello");
  }

  @Test
  void toBytes_encodes_as_utf8() {
    var document = JsonCacheFormat.newDocument(VERSION);
    document.addProperty("name", "café");

    assertThat(new String(JsonCacheFormat.toBytes(document), StandardCharsets.UTF_8))
      .contains("café");
  }

  @Test
  void parseDocument_rejects_another_version() {
    var bytes = JsonCacheFormat.toBytes(JsonCacheFormat.newDocument(VERSION + 1));

    assertThatThrownBy(() -> JsonCacheFormat.parseDocument(bytes, VERSION))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Unsupported cache format version: 4");
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "",
    "not json",
    "[]",
    "\"a string\"",
    "{}",
    "{\"version\":null}",
    "{\"version\":\"3\"}",
    "{\"version\":{}}",
    "{\"version\":3.5}"
  })
  void parseDocument_rejects_malformed_entries(String content) {
    var bytes = content.getBytes(StandardCharsets.UTF_8);

    assertThatThrownBy(() -> JsonCacheFormat.parseDocument(bytes, VERSION))
      .isInstanceOf(RuntimeException.class);
  }

  @Test
  void strings_round_trip_preserving_order() {
    var values = List.of("b", "a", "c");

    JsonArray serialized = JsonCacheFormat.strings(values);

    assertThat(serialized).hasToString("[\"b\",\"a\",\"c\"]");
    assertThat(JsonCacheFormat.stringList(serialized)).containsExactly("b", "a", "c");
  }

  @Test
  void strings_handles_empty_collections() {
    JsonArray serialized = JsonCacheFormat.strings(Set.of());

    assertThat(serialized).hasToString("[]");
    assertThat(JsonCacheFormat.stringList(serialized)).isEmpty();
    assertThat(JsonCacheFormat.stringSet(serialized)).isEmpty();
  }

  @Test
  void stringSet_drops_duplicates_and_keeps_order() {
    JsonArray serialized = JsonCacheFormat.strings(List.of("b", "a", "b"));

    assertThat(JsonCacheFormat.stringSet(serialized)).containsExactly("b", "a");
  }

  @ParameterizedTest
  @ValueSource(strings = {"[1]", "[null]", "[[]]", "[{}]", "[true]"})
  void stringList_rejects_non_strings(String content) {
    JsonArray values = JsonParser.parseString(content).getAsJsonArray();

    assertThatThrownBy(() -> JsonCacheFormat.stringList(values))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageStartingWith("Expected a JSON string, got:");
  }

  @Test
  void span_round_trip() {
    var span = new AnalyzerMessage.TextSpan(12, 6, 12, 32);

    JsonObject serialized = JsonCacheFormat.spanToJson(span);

    assertThat(serialized).hasToString("{\"startLine\":12,\"startCharacter\":6,\"endLine\":12,\"endCharacter\":32}");
    assertThat(JsonCacheFormat.spanFromJson(serialized)).isEqualTo(span);
  }

  @Test
  void span_round_trip_across_lines() {
    var span = new AnalyzerMessage.TextSpan(4, 30, 9, 2);

    assertThat(JsonCacheFormat.spanFromJson(JsonCacheFormat.spanToJson(span))).isEqualTo(span);
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "{\"startLine\":0,\"startCharacter\":0,\"endLine\":1,\"endCharacter\":1}",
    "{\"startLine\":2,\"startCharacter\":-1,\"endLine\":2,\"endCharacter\":1}",
    "{\"startLine\":5,\"startCharacter\":0,\"endLine\":4,\"endCharacter\":1}",
    "{\"startLine\":2,\"startCharacter\":0,\"endLine\":2,\"endCharacter\":-1}",
    "{\"startLine\":2,\"startCharacter\":8,\"endLine\":2,\"endCharacter\":4}"
  })
  void spanFromJson_rejects_impossible_ranges(String content) {
    JsonObject serialized = JsonParser.parseString(content).getAsJsonObject();

    assertThatThrownBy(() -> JsonCacheFormat.spanFromJson(serialized))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageStartingWith("Invalid source span:");
  }

  @Test
  void spanFromJson_rejects_missing_and_fractional_bounds() {
    JsonObject missing = JsonParser.parseString("{\"startLine\":1,\"startCharacter\":0,\"endLine\":1}").getAsJsonObject();
    JsonObject fractional = JsonParser.parseString("{\"startLine\":1.5,\"startCharacter\":0,\"endLine\":1,\"endCharacter\":1}").getAsJsonObject();

    assertThatThrownBy(() -> JsonCacheFormat.spanFromJson(missing))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Missing JSON property 'endCharacter'");
    assertThatThrownBy(() -> JsonCacheFormat.spanFromJson(fractional))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void accessors_read_well_formed_properties() {
    JsonObject object = JsonParser.parseString("""
      {"text":"a","absent":null,"flag":true,"count":7,"nested":{"k":"v"},"list":["x"]}
      """).getAsJsonObject();

    assertThat(JsonCacheFormat.requiredString(object, "text")).isEqualTo("a");
    assertThat(JsonCacheFormat.nullableString(object, "text")).isEqualTo("a");
    assertThat(JsonCacheFormat.nullableString(object, "absent")).isNull();
    assertThat(JsonCacheFormat.requiredBoolean(object, "flag")).isTrue();
    assertThat(JsonCacheFormat.requiredInt(object, "count")).isEqualTo(7);
    assertThat(JsonCacheFormat.requiredObject(object, "nested")).hasToString("{\"k\":\"v\"}");
    assertThat(JsonCacheFormat.requiredArray(object, "list")).hasToString("[\"x\"]");
  }

  @Test
  void accessors_reject_missing_properties() {
    JsonObject object = new JsonObject();

    assertThatThrownBy(() -> JsonCacheFormat.requiredString(object, "nope"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Missing JSON property 'nope'");
    assertThatThrownBy(() -> JsonCacheFormat.nullableString(object, "nope"))
      .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> JsonCacheFormat.requiredBoolean(object, "nope"))
      .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> JsonCacheFormat.requiredInt(object, "nope"))
      .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> JsonCacheFormat.requiredObject(object, "nope"))
      .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> JsonCacheFormat.requiredArray(object, "nope"))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void accessors_reject_wrong_types() {
    JsonObject object = JsonParser.parseString("""
      {"text":42,"flag":"yes","count":"7","nested":[],"list":{},"nullValue":null}
      """).getAsJsonObject();

    assertThatThrownBy(() -> JsonCacheFormat.requiredString(object, "text"))
      .hasMessage("Expected a JSON string for property 'text', got: 42");
    assertThatThrownBy(() -> JsonCacheFormat.nullableString(object, "text"))
      .hasMessage("Expected a JSON string or null for property 'text', got: 42");
    assertThatThrownBy(() -> JsonCacheFormat.requiredBoolean(object, "flag"))
      .hasMessage("Expected a JSON boolean for property 'flag', got: \"yes\"");
    assertThatThrownBy(() -> JsonCacheFormat.requiredInt(object, "count"))
      .hasMessage("Expected a JSON integer for property 'count', got: \"7\"");
    assertThatThrownBy(() -> JsonCacheFormat.requiredObject(object, "nested"))
      .hasMessage("Expected a JSON object, got: []");
    assertThatThrownBy(() -> JsonCacheFormat.requiredArray(object, "list"))
      .hasMessage("Expected a JSON array for property 'list', got: {}");
    assertThatThrownBy(() -> JsonCacheFormat.requiredBoolean(object, "nullValue"))
      .hasMessage("Expected a JSON primitive for property 'nullValue', got: null");
  }

  @Test
  void requiredInt_rejects_values_outside_int_range() {
    JsonObject object = JsonParser.parseString("{\"big\":99999999999}").getAsJsonObject();

    assertThatThrownBy(() -> JsonCacheFormat.requiredInt(object, "big"))
      .isInstanceOf(IllegalArgumentException.class);
  }
}
