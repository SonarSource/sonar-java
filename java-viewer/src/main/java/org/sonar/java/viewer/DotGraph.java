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
import java.util.stream.Stream;

public abstract class DotGraph {

  private static final String ESCAPE_CHAR = "?";

  private final Stream.Builder<DotElement> elements = Stream.builder();

  /**
   * Provide the graph name
   * @return the graph name
   */
  public abstract String name();

  /**
   * Fill nodes and edges for the graph
   * @return the completed graph
   */
  public abstract void build();

  public final void addEdge(DotGraph.Edge edge) {
    elements.add(edge);
  }

  public final void addNode(DotGraph.Node node) {
    elements.add(node);
  }

  /**
   * Convert the graph to DOT format (graph description language).
   * See language specification: http://www.graphviz.org/content/dot-language
   */
  public final String toDot() {
    build();

    StringBuilder sb = new StringBuilder()
      .append("graph ")
      .append(name())
      .append(" {");
    elements.build().map(DotElement::toDot).forEach(sb::append);
    return sb.append("}")
      .toString();
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

  private abstract static class DotElement {


    public abstract String label();

    @CheckForNull
    public abstract Highlighting highlighting();

    @CheckForNull
    public abstract JsonObject details();

    public abstract String toDot();

    protected String dotProperties() {
      Map<String, String> properties = new HashMap<>();
      properties.put("label", label());
      properties.put("highlighting", Highlighting.name(highlighting()));
      properties.put("details", escape(details()));

      return properties.entrySet().stream()
        .filter(entry -> entry.getValue() != null)
        .map(entry -> MessageFormat.format("{0}=\"{1}\"", entry.getKey(), entry.getValue()))
        .collect(Collectors.joining(","));
    }

    private static String escape(@Nullable JsonValue jsonValue) {
      if (jsonValue == null) {
        return null;
      }
      return jsonValue.toString().replaceAll("\"", ESCAPE_CHAR);
    }
  }

  public abstract static class Node extends DotElement {

    private final int id;

    public Node(int id) {
      this.id = id;
    }

    @Override
    public final String toDot() {
      return MessageFormat.format("{0}[{1}];", id, dotProperties());
    }
  }

  public abstract static class Edge extends DotElement {

    private final int from;
    private final int to;

    public Edge(int from, int to) {
      this.from = from;
      this.to = to;
    }

    /**
     * Label can be null for Edges
     */
    @CheckForNull
    @Override
    public abstract String label();

    @Override
    public final String toDot() {
      return MessageFormat.format("{0}->{1}[{2}];", from, to, dotProperties());
    }
  }
}
