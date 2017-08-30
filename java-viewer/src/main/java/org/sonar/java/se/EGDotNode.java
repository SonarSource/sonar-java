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

import org.sonar.java.cfg.CFG;
import org.sonar.java.collections.PStack;
import org.sonar.java.se.ProgramState.SymbolicValueSymbol;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintsByDomain;
import org.sonar.java.se.xproc.BehaviorCache;
import org.sonar.java.se.xproc.ExceptionalYield;
import org.sonar.java.se.xproc.HappyPathYield;
import org.sonar.java.se.xproc.MethodBehavior;
import org.sonar.java.se.xproc.MethodYield;
import org.sonar.java.viewer.DotGraph;
import org.sonar.java.viewer.JsonHelper;
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class EGDotNode extends DotGraph.Node {

  private final ProgramState ps;
  private final ProgramPoint pp;
  @Nullable
  private final MethodBehavior methodBehavior;
  private final boolean hasParents;
  private final boolean isFirstBlock;

  public EGDotNode(int id, ExplodedGraph.Node node, BehaviorCache behaviorCache, boolean hasParents, int firstBlockId) {
    super(id);
    this.ps = node.programState;
    this.pp = node.programPoint;
    this.hasParents = hasParents;
    this.isFirstBlock = isFirstBlock(node, firstBlockId);
    this.methodBehavior = getMethodBehavior(behaviorCache, pp.syntaxTree());
  }

  private static boolean isFirstBlock(ExplodedGraph.Node node, int firstBlockId) {
    return node.programPoint.toString().startsWith("B" + firstBlockId + "." + "0");
  }

  @Override
  public String label() {
    return programPoint();
  }

  @Override
  @CheckForNull
  public DotGraph.Highlighting highlighting() {
    if (hasParents) {
      if (isFirstBlock) {
        return DotGraph.Highlighting.FIRST_NODE;
      }
      // lost nodes - should never happen - worth investigation if appears in viewer
      return DotGraph.Highlighting.LOST_NODE;
    } else if (programPoint().startsWith("B0.0")) {
      return DotGraph.Highlighting.EXIT_NODE;
    }
    return null;
  }

  @Override
  public JsonObject details() {
    JsonObjectBuilder builder = Json.createObjectBuilder();
    builder.add("ppKey", programPointKey());
    builder.add("psStack", stack());
    builder.add("psConstraints", constraints());
    builder.add("psValues", values());
    JsonHelper.addIfNotNull(builder, "methodYields", yields());
    JsonHelper.addIfNotNull(builder, "methodName", methodName());
    return builder.build();
  }

  private JsonArray values() {
    Stream.Builder<JsonObject> values = Stream.builder();
    ps.values.forEach((symbol, sv) -> {
      JsonObject jsonConstraint = Json.createObjectBuilder()
        .add("sv", sv.toString())
        .add("symbol", symbol.toString())
        .build();
      values.add(jsonConstraint);
    });
    return JsonHelper.toArraySortedByField(values.build(), "sv");
  }

  private JsonArray constraints() {
    Stream.Builder<JsonObject> constraints = Stream.builder();
    ps.constraints.forEach((sv, constraint) -> {
      JsonObject jsonConstraint = Json.createObjectBuilder()
        .add("sv", sv.toString())
        .add("constraints", constraintsAsJsonArray(constraint))
        .build();
      constraints.add(jsonConstraint);
    });
    return JsonHelper.toArraySortedByField(constraints.build(), "sv");
  }

  private static JsonArray constraintsAsJsonArray(List<ConstraintsByDomain> constraintsByDomain) {
    return JsonHelper.toArray(constraintsByDomain.stream().map(EGDotNode::constraintsAsJsonArray));
  }

  private static JsonArray constraintsAsJsonArray(@Nullable ConstraintsByDomain constraintsByDomain) {
    JsonArrayBuilder builder = Json.createArrayBuilder();
    if (constraintsByDomain == null || constraintsByDomain.isEmpty()) {
      builder.add("no constraint");
    } else {
      constraintsByDomain.stream().map(Constraint::toString).forEach(builder::add);
    }
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

  private String programPoint() {
    String tree = "";
    if (pp.i < pp.block.elements().size()) {
      Tree syntaxNode = ((CFG.Block) pp.block).elements().get(pp.i);
      tree = "" + syntaxNode.kind() + " L#" + syntaxNode.firstToken().line();
    }
    return programPointKey() + "  " + tree;
  }

  @CheckForNull
  private JsonArray yields() {
    if (methodBehavior == null) {
      return null;
    }
    return JsonHelper.toArray(methodBehavior.yields().stream().map(EGDotNode::yield));
  }

  @CheckForNull
  private static MethodBehavior getMethodBehavior(BehaviorCache bc, @Nullable Tree syntaxTree) {
    if (syntaxTree == null || !syntaxTree.is(Tree.Kind.METHOD_INVOCATION)) {
      return null;
    }
    Symbol symbol = ((MethodInvocationTree) syntaxTree).symbol();
    if (!symbol.isMethodSymbol()) {
      return null;
    }
    return bc.get((Symbol.MethodSymbol) symbol);
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
    if (methodBehavior == null) {
      return null;
    }
    return methodBehavior.methodSymbol().name();
  }

}
