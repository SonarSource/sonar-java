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

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Map;

public class DotHelper {

  private DotHelper() {
  }

  public static String node(int id, String label, @Nullable Highlighting highlighting) {
    return node(id, label, highlighting, Collections.emptyMap());
  }

  public static String node(int id, String label, @Nullable Highlighting highlighting, Map<String, String> extraFields) {
    return MessageFormat.format("{0}[{1}{2}{3}];", id, label(label), highlight(highlighting), extraFields(extraFields));
  }

  public static String edge(int from, int to, @Nullable String label) {
    return edge(from, to, label, null, Collections.emptyMap());
  }

  public static String edge(int from, int to, @Nullable String label, Highlighting highlighting) {
    return edge(from, to, label, highlighting, Collections.emptyMap());
  }

  public static String edge(int from, int to, @Nullable String label, @Nullable Highlighting highlighting, Map<String, String> extraFields) {
    return MessageFormat.format("{0}->{1}[{2}{3}{4}];", from, to, label(label), highlight(highlighting), extraFields(extraFields));
  }

  private static String extraFields(Map<String, String> extraFields) {
    if (extraFields.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, String> extraField : extraFields.entrySet()) {
      sb.append(MessageFormat.format(",{0}=\"{1}\"", extraField.getKey(), extraField.getValue()));
    }
    return sb.toString();
  }

  private static String highlight(@Nullable Highlighting highlighting) {
    return highlighting == null ? "" : ",highlighting=\"" + highlighting.name + "\"";
  }

  private static String label(@Nullable String label) {
    return label == null ? "" : MessageFormat.format("label=\"{0}\"", label);
  }

  public enum Highlighting {
    FIRST_NODE("firstNode"),
    LOST_NODE("lostNode"),
    EXIT_NODE("exitNode"),
    EXCEPTION_EDGE("exceptionEdge"),
    YIELD_EDGE("yieldEdge");

    private final String name;

    private Highlighting(String name) {
      this.name = name;
    }

  }
}
