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
package org.sonar.java.se.xproc;

import com.google.common.collect.ImmutableList;

import org.sonar.java.collections.PCollections;
import org.sonar.java.collections.PMap;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.se.ExplodedGraph;
import org.sonar.java.se.checks.SECheck;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class MethodBehavior {
  private final Symbol.MethodSymbol methodSymbol;
  private final boolean varArgs;
  private final int arity;

  private final Set<MethodYield> yields;
  private final List<SymbolicValue> parameters;
  private boolean complete = false;

  public MethodBehavior(Symbol.MethodSymbol methodSymbol) {
    this.methodSymbol = methodSymbol;
    this.yields = new LinkedHashSet<>();
    this.parameters = new ArrayList<>();
    this.varArgs = ((JavaSymbol.MethodJavaSymbol) methodSymbol).isVarArgs();
    this.arity = methodSymbol.parameterTypes().size();
  }

  public void createYield(ExplodedGraph.Node node) {
    MethodYield yield;
    boolean expectReturnValue = !(isConstructor() || isVoidMethod());
    SymbolicValue resultSV = node.programState.exitValue();

    if (!node.onHappyPath() || (resultSV == null && expectReturnValue) || resultSV instanceof SymbolicValue.ExceptionalSymbolicValue) {
      ExceptionalYield exceptionalYield = new ExceptionalYield(node, this);
      if (resultSV != null) {
        exceptionalYield.setExceptionType(((SymbolicValue.ExceptionalSymbolicValue) resultSV).exceptionType());
      }
      yield = exceptionalYield;
    } else {
      HappyPathYield happyPathYield = new HappyPathYield(node, this);
      if (expectReturnValue) {
        happyPathYield.setResult(parameters.indexOf(resultSV), node.programState.getConstraints(resultSV));
      }
      yield = happyPathYield;
    }
    addParameterConstraints(node, yield);
    yields.add(yield);
  }

  private void addParameterConstraints(ExplodedGraph.Node node, MethodYield yield) {
    // add the constraints on all the parameters
    for (int i = 0; i < parameters.size(); i++) {
      PMap<Class<? extends Constraint>, Constraint> constraints = node.programState.getConstraints(parameters.get(i));
      if (constraints == null) {
        constraints = PCollections.emptyMap();
      }
      yield.parametersConstraints.add(constraints);
    }
  }

  public ExceptionalYield createExceptionalCheckBasedYield(SymbolicValue target, ExplodedGraph.Node node, Type exceptionType, SECheck check) {
    ExceptionalYield yield = new ExceptionalCheckBasedYield(target, exceptionType, check.getClass(), node, this);
    addParameterConstraints(node, yield);
    yields.add(yield);
    return yield;
  }

  public boolean isMethodVarArgs() {
    return varArgs;
  }

  public int methodArity() {
    return arity;
  }

  private boolean isVoidMethod() {
    return methodSymbol.returnType().type().isVoid();
  }

  private boolean isConstructor() {
    return ((JavaSymbol.MethodJavaSymbol) methodSymbol).isConstructor();
  }

  public List<MethodYield> yields() {
    return ImmutableList.<MethodYield>builder().addAll(yields).build();
  }

  public Stream<ExceptionalYield> exceptionalPathYields() {
    return yields.stream()
      .filter(y -> y instanceof ExceptionalYield)
      .map(ExceptionalYield.class::cast);
  }

  public Stream<HappyPathYield> happyPathYields() {
    return yields.stream()
      .filter(y -> y instanceof HappyPathYield)
      .map(HappyPathYield.class::cast);
  }

  public void addParameter(SymbolicValue sv) {
    parameters.add(sv);
  }

  public List<SymbolicValue> parameters() {
    return parameters;
  }

  public boolean isComplete() {
    return complete;
  }

  public void completed() {
    this.complete = true;
  }

  public void addYield(MethodYield yield) {
    yields.add(yield);
  }
}
