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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.json.JsonObject;
import javax.json.JsonValue;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class DotDataProvider {

  public static final String ESCAPE_CHAR = "?";

  public abstract String label();

  @CheckForNull
  public abstract Highlighting highlighting();

  @CheckForNull
  public abstract JsonObject details();


  public static String escape(@Nullable JsonValue jsonValue) {
    if (jsonValue == null) {
      return null;
    }
    return jsonValue.toString().replaceAll("\"", ESCAPE_CHAR);
  }

  public String dotProperties() {
    Map<String, String> properties = new HashMap<>();
    properties.put("label", label());
    properties.put("highlighting", Highlighting.name(highlighting()));
    properties.put("details", DotDataProvider.escape(details()));

    return properties.entrySet().stream()
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
    public static String name(@Nullable Highlighting highlighting) {
      if (highlighting == null) {
        return null;
      }
      return highlighting.name;
    }
  }

  public abstract static class Node extends DotDataProvider {

    private final int id;

    public Node(int id) {
      this.id = id;
    }

    public final String node() {
      return MessageFormat.format("{0}[{1}];\\n", id, dotProperties());
    }
  }

  public abstract static class Edge extends DotDataProvider {

    private final int from;
    private final int to;

    public Edge(int from, int to) {
      this.from = from;
      this.to = to;
    }

    @CheckForNull
    @Override
    public abstract String label();

    public final String edge() {
      return MessageFormat.format("{0}->{1}[{2}];\\n", from, to, dotProperties());
    }
  }
}
