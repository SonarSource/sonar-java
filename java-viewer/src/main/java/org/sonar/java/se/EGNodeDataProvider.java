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
import org.sonar.java.se.constraint.ConstraintsByDomain;
import org.sonar.java.se.xproc.BehaviorCache;
import org.sonar.java.se.xproc.ExceptionalYield;
import org.sonar.java.se.xproc.HappyPathYield;
import org.sonar.java.se.xproc.MethodBehavior;
import org.sonar.java.se.xproc.MethodYield;
import org.sonar.java.viewer.DotHelper;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class EGNodeDataProvider {

  private final ProgramState ps;
  private final ProgramPoint pp;
  private final BehaviorCache bc;

  public EGNodeDataProvider(ExplodedGraph.Node node, BehaviorCache behaviorCache) {
    this.ps = node.programState;
    this.pp = node.programPoint;
    this.bc = behaviorCache;
  }

  public String values() {
    List<String> values = new ArrayList<>();
    ps.values.forEach((symbol, sv) -> values.add(DotHelper.escapeCouple(symbol, sv)));
    String result = values.stream().collect(Collectors.joining(","));
    return DotHelper.asObject(result);
  }

  public String constraints() {
    List<String> constraints = new ArrayList<>();
    ps.constraints.forEach((sv, svConstraints) -> constraints.add(DotHelper.escape(sv.toString()) + ":" + constraintsAsString(svConstraints)));
    String result = constraints.stream().sorted().collect(Collectors.joining(","));
    return DotHelper.asObject(result);
  }

  private static String constraintsAsString(@Nullable ConstraintsByDomain constraintsByDomain) {
    if (constraintsByDomain == null || constraintsByDomain.isEmpty()) {
      return DotHelper.escape("no constraint");
    }
    return constraintsByDomain.stream()
      .map(Constraint::toString)
      .map(DotHelper::escape)
      .collect(Collectors.toList()).toString();
  }

  @SuppressWarnings("unchecked")
  public String stack() {
    // Ugly hack to get the stack and not expose programState API.
    // The stack should remain private to avoid uncontrolled usage in engine
    String result = null;
    try {
      Field stackField = ps.getClass().getDeclaredField("stack");
      stackField.setAccessible(true);
      PStack<SymbolicValueSymbol> stack = (PStack<SymbolicValueSymbol>) stackField.get(ps);
      List<String> stackItems = new ArrayList<>();
      stack.forEach(svs -> stackItems.add(DotHelper.escapeCouple(svs.sv, svs.symbol)));
      result = stackItems.stream().sorted().collect(Collectors.joining(","));
    } catch (Exception e) {
      // do nothing
    }
    return DotHelper.asObject(result);
  }

  public String programPointKey() {
    return "B" + pp.block.id() + "." + pp.i;
  }

  public String programPoint() {
    String tree = "";
    if (pp.i < pp.block.elements().size()) {
      tree = "" + pp.block.elements().get(pp.i).kind() + " L#" + pp.block.elements().get(pp.i).firstToken().line();
    }
    return programPointKey() + "  " + tree;
  }

  @CheckForNull
  public String yields() {
    Tree syntaxTree = pp.syntaxTree();
    if (syntaxTree == null || !syntaxTree.is(Tree.Kind.METHOD_INVOCATION)) {
      return null;
    }
    MethodBehavior knownBehavior = bc.behaviors.get(((MethodInvocationTree) syntaxTree).symbol());
    if (knownBehavior == null) {
      return null;
    }
    String result = knownBehavior.yields().stream().map(EGNodeDataProvider::yield).collect(Collectors.joining(","));
    return DotHelper.asList(result);
  }

  public static String yield(MethodYield methodYield) {
    List<ConstraintsByDomain> parametersConstraints = getParametersConstraints(methodYield);
    String result = DotHelper.escape("params") + ":" + parametersConstraints.stream().map(EGNodeDataProvider::constraintsAsString).collect(Collectors.toList()).toString();
    if (methodYield instanceof HappyPathYield) {
      HappyPathYield hpy = (HappyPathYield) methodYield;
      result += "," + DotHelper.escape("result") + ":" + constraintsAsString(hpy.resultConstraint());
      result += "," + DotHelper.escape("resultIndex") + ":" + hpy.resultIndex();
    } else if (methodYield instanceof ExceptionalYield) {
      Type exceptionType = ((ExceptionalYield) methodYield).exceptionType();
      String exceptionFQN = exceptionType == null ? "runtime Exception" : exceptionType.fullyQualifiedName();
      result += "," + DotHelper.escapeCouple("exception", exceptionFQN);
    }
    return DotHelper.asObject(result);
  }

  @SuppressWarnings("unchecked")
  private static List<ConstraintsByDomain> getParametersConstraints(MethodYield methodYield) {
    // Ugly hack to get method yield parameters without exposing MethodYield API
    try {
      Field parametersConstraintsField = MethodYield.class.getDeclaredField("parametersConstraints");
      parametersConstraintsField.setAccessible(true);
      return (List<ConstraintsByDomain>) parametersConstraintsField.get(methodYield);
    } catch (Exception e) {
      return Collections.emptyList();
    }
  }

  @CheckForNull
  public String methodName() {
    Tree syntaxTree = pp.syntaxTree();
    if (syntaxTree == null || !syntaxTree.is(Tree.Kind.METHOD_INVOCATION)) {
      return null;
    }
    Symbol symbol = ((MethodInvocationTree) syntaxTree).symbol();
    MethodBehavior knownBehavior = bc.behaviors.get(symbol);
    if (knownBehavior == null) {
      return null;
    }
    return symbol.name();
  }
}
