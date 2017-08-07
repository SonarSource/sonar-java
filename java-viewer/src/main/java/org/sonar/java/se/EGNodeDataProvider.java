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
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class EGNodeDataProvider {

  private static final Comparator<JsonObject> BY_SV = (o1, o2) -> o1.getString("sv").compareTo(o2.getString("sv"));

  private final ProgramState ps;
  private final ProgramPoint pp;
  private final BehaviorCache bc;

  public EGNodeDataProvider(ExplodedGraph.Node node, BehaviorCache behaviorCache) {
    this.ps = node.programState;
    this.pp = node.programPoint;
    this.bc = behaviorCache;
  }

  private JsonArray values() {
    List<JsonObject> values = new ArrayList<>();
    ps.values.forEach((symbol, sv) -> {
      JsonObject jsonConstraint = Json.createObjectBuilder()
        .add("symbol", symbol.toString())
        .add("sv", sv.toString())
        .build();
      values.add(jsonConstraint);
    });
    JsonArrayBuilder builder = Json.createArrayBuilder();
    values.stream().sorted(EGNodeDataProvider.BY_SV).forEach(builder::add);
    return builder.build();
  }

  private JsonArray constraints() {
    List<JsonObject> constraints = new ArrayList<>();
    ps.constraints.forEach((sv, svConstraints) -> {
      JsonObject jsonConstraint = Json.createObjectBuilder()
        .add("sv", sv.toString())
        .add("constraints", constraintsAsJsonArray(svConstraints))
        .build();
      constraints.add(jsonConstraint);
    });
    JsonArrayBuilder builder = Json.createArrayBuilder();
    constraints.stream().sorted(EGNodeDataProvider.BY_SV).forEach(builder::add);
    return builder.build();
  }

  private static JsonArray constraintsAsJsonArray(List<ConstraintsByDomain> constraintsByDomain) {
    JsonArrayBuilder builder = Json.createArrayBuilder();
    constraintsByDomain.stream().map(EGNodeDataProvider::constraintsAsJsonArray).forEach(builder::add);
    return builder.build();
  }

  private static JsonArray constraintsAsJsonArray(@Nullable ConstraintsByDomain constraintsByDomain) {
    JsonArrayBuilder builder = Json.createArrayBuilder();
    if (constraintsByDomain == null || constraintsByDomain.isEmpty()) {
      return builder.add("no constraint").build();
    }
    constraintsByDomain.stream().forEach(constraint -> builder.add(constraint.toString()));
    return builder.build();
  }

  @SuppressWarnings("unchecked")
  private JsonArray stack() {
    // Ugly hack to get the stack and not expose programState API.
    // The stack should remain private to avoid uncontrolled usage in engine
    JsonArrayBuilder builder = Json.createArrayBuilder();
    try {
      Field stackField = ps.getClass().getDeclaredField("stack");
      stackField.setAccessible(true);
      PStack<SymbolicValueSymbol> stack = (PStack<SymbolicValueSymbol>) stackField.get(ps);
      stack.forEach(svs -> {
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        objectBuilder.add("sv", svs.sv.toString());
        Symbol symbol = svs.symbol;
        if (symbol != null) {
          objectBuilder.add("symbol", symbol.toString());
        }
        builder.add(objectBuilder.build());
      });
    } catch (Exception e) {
      // do nothing
    }
    return builder.build();
  }

  private String programPointKey() {
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
  private JsonArray yields() {
    Tree syntaxTree = pp.syntaxTree();
    if (syntaxTree == null || !syntaxTree.is(Tree.Kind.METHOD_INVOCATION)) {
      return null;
    }
    MethodBehavior knownBehavior = bc.behaviors.get(((MethodInvocationTree) syntaxTree).symbol());
    if (knownBehavior == null) {
      return null;
    }
    JsonArrayBuilder builder = Json.createArrayBuilder();
    knownBehavior.yields().stream().map(EGNodeDataProvider::yield).forEach(builder::add);
    return builder.build();
  }

  public static JsonObject yield(MethodYield methodYield) {
    JsonObjectBuilder builder = Json.createObjectBuilder();
    List<ConstraintsByDomain> parametersConstraints = getParametersConstraints(methodYield);
    builder.add("params", constraintsAsJsonArray(parametersConstraints));
    if (methodYield instanceof HappyPathYield) {
      HappyPathYield hpy = (HappyPathYield) methodYield;
      builder.add("result", constraintsAsJsonArray(hpy.resultConstraint()));
      builder.add("resultIndex", hpy.resultIndex());
    } else if (methodYield instanceof ExceptionalYield) {
      Type exceptionType = ((ExceptionalYield) methodYield).exceptionType();
      String exceptionFQN = exceptionType == null ? "runtime Exception" : exceptionType.fullyQualifiedName();
      builder.add("exception", exceptionFQN);
    }
    return builder.build();
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
  private String methodName() {
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

  public JsonObject details() {
    JsonObjectBuilder builder = Json.createObjectBuilder();
    builder.add("ppKey", programPointKey());
    builder.add("psStack", stack());
    builder.add("psConstraints", constraints());
    builder.add("psValues", values());
    DotHelper.addIfNotNull(builder, "methodYields", yields());
    DotHelper.addIfNotNull(builder, "methodName", methodName());
    return builder.build();
  }
}
