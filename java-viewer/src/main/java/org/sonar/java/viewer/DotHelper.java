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

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class DotHelper {

  private static final char ESCAPE_CHAR = '?';
  private static final String ESCAPED_COUPLE = escape("{0}") + ":" + escape("{1}");

  private DotHelper() {
  }

  public static String escape(String value) {
    return ESCAPE_CHAR + value + ESCAPE_CHAR;
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
    return node(id, label, highlighting, Collections.emptyMap());
  }

  public static String node(int id, String label, @Nullable Highlighting highlighting, Map<String, String> extraFields) {
    Map<String, String> newMap = new HashMap<>(extraFields);
    newMap.put("label", label);
    if (highlighting != null) {
      newMap.put("highlighting", highlighting.name);
    }
    return MessageFormat.format("{0}[{1}];", id, extraFields(newMap));
  }

  public static String edge(int from, int to, @Nullable String label) {
    return edge(from, to, label, null, Collections.emptyMap());
  }

  public static String edge(int from, int to, @Nullable String label, Highlighting highlighting) {
    return edge(from, to, label, highlighting, Collections.emptyMap());
  }

  public static String edge(int from, int to, @Nullable String label, @Nullable Highlighting highlighting, Map<String, String> extraFields) {
    Map<String, String> newMap = new HashMap<>(extraFields);
    if (label != null) {
      newMap.put("label", label);
    }
    if (highlighting != null) {
      newMap.put("highlighting", highlighting.name);
    }
    return MessageFormat.format("{0}->{1}[{2}];", from, to, extraFields(newMap));
  }

  private static String extraFields(Map<String, String> extraFields) {
    if (extraFields.isEmpty()) {
      return "";
    }
    return extraFields.entrySet().stream()
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
