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

import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class DotHelper {

  private static final String ESCAPE_CHAR = "?";
  private static final String ESCAPED_COUPLE = escape("{0}") + ":" + escape("{1}");

  private DotHelper() {
  }

  public static String escape(String value) {
    return ESCAPE_CHAR + value + ESCAPE_CHAR;
  }

  public static String escape(JsonValue jsonValue) {
    return jsonValue.toString().replaceAll("\"", ESCAPE_CHAR);
  }

  public static JsonObjectBuilder addIfNotNull(JsonObjectBuilder builder, String key, @Nullable JsonValue value) {
    if (value != null) {
      builder.add(key, value);
    }
    return builder;
  }

  public static JsonObjectBuilder addIfNotNull(JsonObjectBuilder builder, String key, @Nullable String value) {
    if (value != null) {
      builder.add(key, value);
    }
    return builder;
  }

  public static String escapeCouple(Object key, Object value) {
    return MessageFormat.format(ESCAPED_COUPLE, key, value);
  }

  public static String asObject(@Nullable String value) {
    return "{" + valueOrEmpty(value) + "}";
  }

  public static String asList(@Nullable String value) {
    return "[" + valueOrEmpty(value) + "]";
  }

  private static String valueOrEmpty(@Nullable String value) {
    return value != null ? value : "";
  }

  public static String node(int id, String label, @Nullable Highlighting highlighting) {
    return node(id, label, highlighting, null);
  }

  public static String node(int id, String label, @Nullable Highlighting highlighting, @Nullable JsonObject details) {
    Map<String, String> dotFields = new HashMap<>();
    dotFields.put("label", label);
    if (highlighting != null) {
      dotFields.put("highlighting", highlighting.name);
    }
    if (details != null) {
      dotFields.put("details", escape(details));
    }
    return MessageFormat.format("{0}[{1}];\\n", id, dotFields(dotFields));
  }

  public static String edge(int from, int to, @Nullable String label) {
    return edge(from, to, label, null, null);
  }

  public static String edge(int from, int to, @Nullable String label, Highlighting highlighting) {
    return edge(from, to, label, highlighting, null);
  }

  public static String edge(int from, int to, @Nullable String label, @Nullable Highlighting highlighting, @Nullable JsonObject details) {
    Map<String, String> dotFields = new HashMap<>();
    if (label != null) {
      dotFields.put("label", label);
    }
    if (highlighting != null) {
      dotFields.put("highlighting", highlighting.name);
    }
    if (details != null) {
      dotFields.put("details", escape(details));
    }
    return MessageFormat.format("{0}->{1}[{2}];\\n", from, to, dotFields(dotFields));
  }

  private static String dotFields(Map<String, String> fields) {
    if (fields.isEmpty()) {
      return "";
    }
    return fields.entrySet().stream()
      .filter(entry -> entry.getValue() != null)
      .map(entry -> MessageFormat.format("{0}=\"{1}\"", entry.getKey(), entry.getValue()))
      .collect(Collectors.joining(","));
  }

  public enum Highlighting {
    FIRST_NODE("firstNode"),
    LOST_NODE("lostNode"),
    EXIT_NODE("exitNode"),

    TOKEN_KIND("tokenKind"),
    CLASS_KIND("classKind"),
    METHOD_KIND("methodKind"),

    EXCEPTION_EDGE("exceptionEdge"),
    YIELD_EDGE("yieldEdge");

    private final String name;

    Highlighting(String name) {
      this.name = name;
    }

    @CheckForNull
    public static Highlighting fromTreeKind(Tree.Kind kind) {
      switch (kind) {
        case COMPILATION_UNIT:
          return FIRST_NODE;
        case CLASS:
        case INTERFACE:
        case ANNOTATION_TYPE:
        case ENUM:
          return CLASS_KIND;
        case CONSTRUCTOR:
        case METHOD:
          return METHOD_KIND;
        case TOKEN:
          // token are explicitly selected
        default:
          return null;
      }
    }

  }
}
