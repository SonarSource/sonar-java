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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.sonar.java.reporting.AnalyzerMessage;

/**
 * JSON encoding of cache entries, for checks that persist per-file data through {@link FileCachingCheck}.
 *
 * <p>An entry is a JSON object carrying a {@code version} property. The version is owned by the check:
 * bumping it invalidates that check's previously cached entries, which are then recomputed on the next
 * analysis. Use {@link #newDocument(int)} to start an entry and {@link #parseDocument(byte[], int)} to
 * read one back.
 *
 * <p>Every accessor below is strict: anything unexpected — a missing property, a wrong JSON type, a
 * fractional number where an integer is required — throws {@link IllegalArgumentException}.
 * {@link FileCachingCheck#readFromCache} turns that into a cache miss, so a corrupt or stale entry
 * degrades to re-parsing the file rather than corrupting the analysis.
 *
 * <p>Only the Gson tree API is used here, never its reflective object binding: the plugin is shaded with
 * {@code minimizeJar}, which strips classes reached only by reflection.
 */
public final class JsonCacheFormat {

  private static final String VERSION = "version";
  private static final String START_LINE = "startLine";
  private static final String START_CHARACTER = "startCharacter";
  private static final String END_LINE = "endLine";
  private static final String END_CHARACTER = "endCharacter";

  private JsonCacheFormat() {
  }

  /**
   * Starts a new cache entry stamped with {@code version}.
   */
  public static JsonObject newDocument(int version) {
    var document = new JsonObject();
    document.addProperty(VERSION, version);
    return document;
  }

  /**
   * Parses a cache entry and checks that it was written in the expected format version.
   *
   * @throws IllegalArgumentException if the content is not a JSON object, carries no usable
   *                                  {@code version}, or was written in a different version
   */
  public static JsonObject parseDocument(byte[] data, int expectedVersion) {
    JsonObject document = requiredObject(JsonParser.parseString(new String(data, StandardCharsets.UTF_8)));
    int version = requiredInt(document, VERSION);
    if (version != expectedVersion) {
      throw new IllegalArgumentException("Unsupported cache format version: " + version);
    }
    return document;
  }

  public static byte[] toBytes(JsonObject document) {
    return document.toString().getBytes(StandardCharsets.UTF_8);
  }

  public static JsonArray strings(Collection<String> values) {
    var serializedValues = new JsonArray();
    values.forEach(serializedValues::add);
    return serializedValues;
  }

  public static List<String> stringList(JsonArray values) {
    List<String> result = new ArrayList<>();
    for (JsonElement value : values) {
      result.add(asString(value));
    }
    return result;
  }

  /**
   * Reads a JSON array of strings, preserving its order and dropping duplicates.
   */
  public static Set<String> stringSet(JsonArray values) {
    return new LinkedHashSet<>(stringList(values));
  }

  public static JsonObject spanToJson(AnalyzerMessage.TextSpan span) {
    var serializedSpan = new JsonObject();
    serializedSpan.addProperty(START_LINE, span.startLine);
    serializedSpan.addProperty(START_CHARACTER, span.startCharacter);
    serializedSpan.addProperty(END_LINE, span.endLine);
    serializedSpan.addProperty(END_CHARACTER, span.endCharacter);
    return serializedSpan;
  }

  /**
   * @throws IllegalArgumentException if the span does not describe a possible source range
   */
  public static AnalyzerMessage.TextSpan spanFromJson(JsonObject serializedSpan) {
    int startLine = requiredInt(serializedSpan, START_LINE);
    int startCharacter = requiredInt(serializedSpan, START_CHARACTER);
    int endLine = requiredInt(serializedSpan, END_LINE);
    int endCharacter = requiredInt(serializedSpan, END_CHARACTER);
    if (startLine < 1 || startCharacter < 0 || endLine < startLine || endCharacter < 0 || (startLine == endLine && endCharacter < startCharacter)) {
      throw new IllegalArgumentException("Invalid source span: " + serializedSpan);
    }
    return new AnalyzerMessage.TextSpan(startLine, startCharacter, endLine, endCharacter);
  }

  public static JsonObject requiredObject(JsonElement element) {
    if (!element.isJsonObject()) {
      throw new IllegalArgumentException("Expected a JSON object, got: " + element);
    }
    return element.getAsJsonObject();
  }

  public static JsonObject requiredObject(JsonObject object, String property) {
    return requiredObject(requiredElement(object, property));
  }

  public static JsonArray requiredArray(JsonObject object, String property) {
    JsonElement element = requiredElement(object, property);
    if (!element.isJsonArray()) {
      throw new IllegalArgumentException("Expected a JSON array for property '" + property + "', got: " + element);
    }
    return element.getAsJsonArray();
  }

  public static String requiredString(JsonObject object, String property) {
    JsonElement element = requiredElement(object, property);
    if (!isString(element)) {
      throw new IllegalArgumentException("Expected a JSON string for property '" + property + "', got: " + element);
    }
    return element.getAsString();
  }

  @CheckForNull
  public static String nullableString(JsonObject object, String property) {
    JsonElement element = requiredElement(object, property);
    if (element.isJsonNull()) {
      return null;
    }
    if (!isString(element)) {
      throw new IllegalArgumentException("Expected a JSON string or null for property '" + property + "', got: " + element);
    }
    return element.getAsString();
  }

  public static boolean requiredBoolean(JsonObject object, String property) {
    JsonPrimitive primitive = requiredPrimitive(object, property);
    if (!primitive.isBoolean()) {
      throw new IllegalArgumentException("Expected a JSON boolean for property '" + property + "', got: " + primitive);
    }
    return primitive.getAsBoolean();
  }

  /**
   * @throws IllegalArgumentException if the property is not a number, or holds a value that is not an exact {@code int}
   */
  public static int requiredInt(JsonObject object, String property) {
    JsonPrimitive primitive = requiredPrimitive(object, property);
    if (!primitive.isNumber()) {
      throw new IllegalArgumentException("Expected a JSON integer for property '" + property + "', got: " + primitive);
    }
    try {
      return primitive.getAsBigDecimal().intValueExact();
    } catch (ArithmeticException e) {
      throw new IllegalArgumentException("Expected an exact JSON integer for property '" + property + "', got: " + primitive, e);
    }
  }

  private static String asString(JsonElement element) {
    if (!isString(element)) {
      throw new IllegalArgumentException("Expected a JSON string, got: " + element);
    }
    return element.getAsString();
  }

  private static boolean isString(JsonElement element) {
    return element.isJsonPrimitive() && element.getAsJsonPrimitive().isString();
  }

  private static JsonPrimitive requiredPrimitive(JsonObject object, String property) {
    JsonElement element = requiredElement(object, property);
    if (!element.isJsonPrimitive()) {
      throw new IllegalArgumentException("Expected a JSON primitive for property '" + property + "', got: " + element);
    }
    return element.getAsJsonPrimitive();
  }

  private static JsonElement requiredElement(JsonObject object, String property) {
    JsonElement element = object.get(property);
    if (element == null) {
      throw new IllegalArgumentException("Missing JSON property '" + property + "'");
    }
    return element;
  }
}
