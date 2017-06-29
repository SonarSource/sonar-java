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
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProgramStateDataProvider {

  private final ProgramState ps;
  private final ProgramPoint pp;

  public ProgramStateDataProvider(ExplodedGraph.Node node) {
    this.ps = node.programState;
    this.pp = node.programPoint;
  }

  public String values() {
    return ps.values.toString();
  }

  public String constraints() {
    List<String> result = new ArrayList<>();
    ps.constraints.forEach((sv, constraints) -> result.add(sv.toString() + "=" + constraints.stream().map(Constraint::toString).collect(Collectors.toList()).toString()));
    return result.stream().sorted().collect(Collectors.toList()).toString();
  }

  public String stack() {
    /// Ugly hack to get the stack and not expose programState API. The stack should remain private to avoid uncontrolled usage in engine
    try {
      Field stackField = ps.getClass().getDeclaredField("stack");
      stackField.setAccessible(true);
      PStack<SymbolicValue> stack = (PStack<SymbolicValue>) stackField.get(ps);
      return stack.toString();
    } catch (Exception e) {
      return "[]";
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
