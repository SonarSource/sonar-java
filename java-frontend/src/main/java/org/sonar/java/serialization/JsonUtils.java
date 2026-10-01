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
package org.sonar.java.serialization;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * Low-level JSON reading and writing mechanics, together with the names of the properties making up the JSON
 * representation of cached elements.
 * Renaming any of those names changes the serialized format and makes previously written cache entries unreadable.
 */
public final class JsonUtils {

  private static final String VERSION = "version";

  // Spring properties:
  public static final String BEANS = "beans";
  public static final String PACKAGES = "packages";

  // Spring beans properties:
  public static final String NAME = "name";
  public static final String TYPE = "type";
  public static final String PACKAGE = "package";
  public static final String SPAN = "span";
  public static final String PRIMARY = "primary";
  public static final String PROFILES = "profiles";
  public static final String QUALIFIER = "qualifier";
  public static final String DEPENDENCIES = "dependencies";
  public static final String INJECTION_POINTS = "injectionPoints";
  public static final String MULTIPLE = "multiple";
  public static final String TYPE_HIERARCHY = "typeHierarchy";

  // Text span properties:
  public static final String START_LINE = "startLine";
  public static final String START_CHARACTER = "startCharacter";
  public static final String END_LINE = "endLine";
  public static final String END_CHARACTER = "endCharacter";

  private JsonUtils() {
  }

  /**
   * Serializes a document holding its format version followed by the properties written by {@code body}.
   *
   * @param version Version of the format of the document, checked when reading it back with
   *                {@link #parseDocument(String, int)}.
   * @param body    Writes the properties of the document, into the object the version was written to.
   */
  public static String writeDocument(int version, DocumentBody body) {
    var out = new StringWriter();
    try (var writer = new JsonWriter(out)) {
      writer.beginObject();
      writer.name(VERSION).value(version);
      body.write(writer);
      writer.endObject();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to serialize a JSON document", e);
    }
    return out.toString();
  }

  /**
   * Writes the properties a {@link #writeDocument(int, DocumentBody)} document holds besides its version.
   */
  @FunctionalInterface
  public interface DocumentBody {
    void write(JsonWriter out) throws IOException;
  }

  /**
   * Parses a document written by {@link #writeDocument(int, DocumentBody)}, rejecting content that was written in
   * another format version and can therefore no longer be read.
   */
  public static JsonObject parseDocument(String content, int expectedVersion) {
    JsonObject document = requiredObject(JsonParser.parseString(content));
    int version = requiredInt(document, VERSION);
    if (version != expectedVersion) {
      throw new IllegalArgumentException("Unsupported document version: " + version);
    }
    return document;
  }

  /**
   * Deserializes a JSON array into a set of strings.
   *
   * @param values The JSON array to deserialize.
   * @return The set of strings read from the array.
   */
  public static Set<String> deserializeStrings(JsonArray values) {
    Set<String> result = new LinkedHashSet<>();
    for (JsonElement value : values) {
      if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
        throw new IllegalArgumentException("Expected a JSON string, got: " + value);
      }
      result.add(value.getAsString());
    }
    return result;
  }

  /**
   * Reads a string from a JSON reader.
   *
   * @param in The JSON reader to consume the string from.
   * @return The string consumed from the reader.
   */
  public static String readString(JsonReader in) throws IOException {
    expect(in, JsonToken.STRING);
    return in.nextString();
  }

  /**
   * Reads a property whose value may be an explicit JSON {@code null}. The property itself is still expected to be
   * present, but since {@code null} is a valid value, absence cannot be detected with
   * {@link #required(Object, String)}: the caller must track it and report it with {@link #missingProperty(String)}.
   */
  @CheckForNull
  public static String readNullableString(JsonReader in) throws IOException {
    if (in.peek() == JsonToken.NULL) {
      in.nextNull();
      return null;
    }
    return readString(in);
  }

  /**
   * Writes a collection of strings to a JSON writer.
   *
   * @param out    The JSON writer to write the strings to.
   * @param values The strings to serialize.
   */
  public static void writeStrings(JsonWriter out, Collection<String> values) throws IOException {
    out.beginArray();
    for (String value : values) {
      out.value(value);
    }
    out.endArray();
  }

  /**
   * Reads a set of strings from a JSON reader.
   *
   * @param in The JSON reader to consume the strings from.
   * @return The set of strings consumed from the reader.
   */
  public static Set<String> readStrings(JsonReader in) throws IOException {
    Set<String> values = new LinkedHashSet<>();
    in.beginArray();
    while (in.hasNext()) {
      values.add(readString(in));
    }
    in.endArray();
    return values;
  }

  /**
   * Read an integer value from a JSON reader.
   *
   * @param in The JSON reader to consume the integer from.
   * @return The integer value consumed from the reader.
   */
  public static int readInt(JsonReader in) throws IOException {
    expect(in, JsonToken.NUMBER);
    return new BigDecimal(in.nextString()).intValueExact();
  }

  /**
   * Checks that the given value is not null, or throws an exception reporting a missing property otherwise.
   *
   * @param value    The value to check for nullability.
   * @param property The property's name.
   * @return The value if it is not null.
   */
  public static <T> T required(@Nullable T value, String property) {
    if (value == null) {
      throw missingProperty(property);
    }
    return value;
  }

  /**
   * Throws a new {@link IllegalArgumentException} reporting a missing property in a JSON object.
   *
   * @param property The missing property's name.
   */
  public static IllegalArgumentException missingProperty(String property) {
    return new IllegalArgumentException("Missing JSON property '" + property + "'");
  }

  /**
   * Checks that the given JSON element is an object.
   *
   * @param element The element to check.
   * @return The element as a {@link JsonObject}.
   * @throws IllegalArgumentException If the element is not a JSON object.
   */
  public static JsonObject requiredObject(JsonElement element) {
    if (!element.isJsonObject()) {
      throw new IllegalArgumentException("Expected a JSON object, got: " + element);
    }
    return element.getAsJsonObject();
  }

  /**
   * Reads a property expected to hold a JSON array from a JSON object.
   *
   * @param object   The object to read the property from.
   * @param property The property's name.
   * @return The value of the property, as a {@link JsonArray}.
   * @throws IllegalArgumentException If the property is missing or does not hold a JSON array.
   */
  public static JsonArray requiredArray(JsonObject object, String property) {
    JsonElement element = requiredElement(object, property);
    if (!element.isJsonArray()) {
      throw new IllegalArgumentException("Expected a JSON array for property '" + property + "', got: " + element);
    }
    return element.getAsJsonArray();
  }

  /**
   * Reads a property expected to hold an integer from a JSON object.
   *
   * @param object   The object to read the property from.
   * @param property The property's name.
   * @return The value of the property, as an {@code int}.
   * @throws IllegalArgumentException If the property is missing or does not hold a JSON number.
   * @throws ArithmeticException      If the property holds a number with a non-zero fractional part, or a number that
   *                                  does not fit in an {@code int}.
   */
  public static int requiredInt(JsonObject object, String property) {
    JsonElement element = requiredElement(object, property);
    if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
      throw new IllegalArgumentException("Expected a JSON integer for property '" + property + "', got: " + element);
    }
    return element.getAsBigDecimal().intValueExact();
  }

  private static void expect(JsonReader in, JsonToken expected) throws IOException {
    JsonToken actual = in.peek();
    if (actual != expected) {
      throw new IllegalArgumentException("Expected a JSON " + expected + " at " + in.getPath() + ", got: " + actual);
    }
  }

  private static JsonElement requiredElement(JsonObject object, String property) {
    JsonElement element = object.get(property);
    if (element == null) {
      throw missingProperty(property);
    }
    return element;
  }
}
