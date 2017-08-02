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
package org.sonar.java.se;

import org.sonar.java.collections.PStack;
import org.sonar.java.se.ProgramState.SymbolicValueSymbol;
import org.sonar.java.se.constraint.Constraint;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProgramStateDataProvider {

  private static final char ESCAPE_CHAR = '?';
  private static final String ESCAPED_COUPLE = escape("{0}") + ":" + escape("{1}");
  private final ProgramState ps;
  private final ProgramPoint pp;

  public ProgramStateDataProvider(ExplodedGraph.Node node) {
    this.ps = node.programState;
    this.pp = node.programPoint;
  }

  public String values() {
    List<String> values = new ArrayList<>();
    ps.values.forEach((symbol, sv) -> values.add(MessageFormat.format(ESCAPED_COUPLE, symbol, sv)));
    String result = values.stream().collect(Collectors.joining(","));
    return "{" + result + "}";
  }

  private static String escape(String value) {
    return ESCAPE_CHAR + value + ESCAPE_CHAR;
  }

  public String constraints() {
    List<String> constraints = new ArrayList<>();
    ps.constraints.forEach((sv, svConstraints) -> constraints.add(escape(sv.toString()) + ":" + svConstraints.stream()
      .map(Constraint::toString)
      .map(ProgramStateDataProvider::escape)
      .collect(Collectors.toList()).toString()));
    String result = constraints.stream().sorted().collect(Collectors.joining(","));
    return "{" + result + "}";
  }

  public String stack() {
    // Ugly hack to get the stack and not expose programState API. The stack should remain private to avoid uncontrolled usage in engine
    try {
      Field stackField = ps.getClass().getDeclaredField("stack");
      stackField.setAccessible(true);
      PStack<SymbolicValueSymbol> stack = (PStack<SymbolicValueSymbol>) stackField.get(ps);
      List<String> stackItems = new ArrayList<>();
      stack.forEach(svs -> stackItems.add(MessageFormat.format(ESCAPED_COUPLE, svs.sv, svs.symbol)));
      String result = stackItems.stream().sorted().collect(Collectors.joining(","));
      return "{" + result + "}";
    } catch (Exception e) {
      return "{}";
    }
  }

  public String programPoint() {
    String tree = "";
    if (pp.i < pp.block.elements().size()) {
      tree = "" + pp.block.elements().get(pp.i).kind() + " L#" + pp.block.elements().get(pp.i).firstToken().line();
    }
    return "B" + pp.block.id() + "." + pp.i + "  " + tree;
  }
}
