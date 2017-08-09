/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.viewer;

import javax.annotation.Nullable;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import java.util.Comparator;
import java.util.stream.Stream;

public interface JsonHelper {

  static JsonObjectBuilder addIfNotNull(JsonObjectBuilder builder, String key, @Nullable JsonValue value) {
    if (value != null) {
      builder.add(key, value);
    }
    return builder;
  }

  static JsonObjectBuilder addIfNotNull(JsonObjectBuilder builder, String key, @Nullable String value) {
    if (value != null) {
      builder.add(key, value);
    }
    return builder;
  }

  static JsonArray toArray(Stream<? extends JsonValue> stream) {
    JsonArrayBuilder builder = Json.createArrayBuilder();
    stream.forEach(builder::add);
    return builder.build();
  }

  static JsonArray toArraySortedByField(Stream<JsonObject> stream, String field) {
    return toArray(stream.sorted(Comparator.comparing(jObj -> jObj.getString(field))));
  }

}
