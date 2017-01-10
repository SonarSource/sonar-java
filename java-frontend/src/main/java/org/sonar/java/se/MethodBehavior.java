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

import com.google.common.collect.ImmutableList;

import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.semantic.Symbol;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class MethodBehavior {
  private final Symbol.MethodSymbol methodSymbol;
  private final Set<MethodYield> yields;
  private final Map<Symbol, SymbolicValue> parameters;
  private boolean complete = false;

  public MethodBehavior(Symbol.MethodSymbol methodSymbol) {
    this.methodSymbol = methodSymbol;
    this.yields = new LinkedHashSet<>();
    this.parameters = new LinkedHashMap<>();
  }

  public void createYield(ProgramState programState, boolean happyPathYield) {
    MethodYield yield = new MethodYield(parameters.size(), ((JavaSymbol.MethodJavaSymbol) methodSymbol).isVarArgs());
    yield.exception = !happyPathYield;
    List<SymbolicValue> parameterSymbolicValues = new ArrayList<>(parameters.values());

    for (int i = 0; i < yield.parametersConstraints.length; i++) {
      yield.parametersConstraints[i] = programState.getConstraint(parameterSymbolicValues.get(i));
    }

    SymbolicValue resultSV = programState.exitValue();
    if (resultSV instanceof SymbolicValue.ExceptionalSymbolicValue) {
      yield.exception = true;
      yield.exceptionType = ((SymbolicValue.ExceptionalSymbolicValue) resultSV).exceptionType();
    } else if (!isConstructor() && !isVoidMethod()) {
      if (resultSV == null) {
        // if there is no return value but we are not in a void method or constructor, we are not in a happy path
        yield.exception = true;
      } else {
        yield.resultIndex = parameterSymbolicValues.indexOf(resultSV);
        yield.resultConstraint = programState.getConstraint(resultSV);
      }
    }

    yields.add(yield);
  }

  private boolean isVoidMethod() {
    return methodSymbol.returnType().type().isVoid();
  }

  private boolean isConstructor() {
    return ((JavaSymbol.MethodJavaSymbol) methodSymbol).isConstructor();
  }

  List<MethodYield> yields() {
    return ImmutableList.<MethodYield>builder().addAll(yields).build();
  }

  Stream<MethodYield> exceptionalPathYields() {
    return yields.stream().filter(y -> y.exception);
  }

  Stream<MethodYield> happyPathYields() {
    return yields.stream().filter(y -> !y.exception);
  }

  public void addParameter(Symbol symbol, SymbolicValue sv) {
    parameters.put(symbol, sv);
  }

  public Collection<SymbolicValue> parameters() {
    return parameters.values();
  }

  public boolean isComplete() {
    return complete;
  }

  void completed() {
    this.complete = true;
  }
}
