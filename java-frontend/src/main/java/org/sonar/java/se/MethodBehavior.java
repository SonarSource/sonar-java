/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.Preconditions;
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

public class MethodBehavior {
  private final Symbol.MethodSymbol methodSymbol;
  private final Set<MethodYield> yields;
  private final Map<Symbol, SymbolicValue> parameters;
  private boolean complete = false;
  private List<MethodYield> reducedYields = null;

  public MethodBehavior(Symbol.MethodSymbol methodSymbol) {
    this.methodSymbol = methodSymbol;
    this.yields = new LinkedHashSet<>();
    this.parameters = new LinkedHashMap<>();
  }

  public void createYield(ProgramState programState, boolean happyPathYield) {
    MethodYield yield = new MethodYield(parameters.size());
    yield.exception = !happyPathYield;
    List<SymbolicValue> parameterSymbolicValues = new ArrayList<>(parameters.values());

    for (int i = 0; i < yield.parametersConstraints.length; i++) {
      yield.parametersConstraints[i] = programState.getConstraint(parameterSymbolicValues.get(i));
    }

    if (!isConstructor() && !isVoidMethod()) {
      SymbolicValue resultSV = programState.returnValue();
      if (resultSV != null) {
        yield.resultIndex = parameterSymbolicValues.indexOf(resultSV);
        yield.resultConstraint = programState.getConstraint(resultSV);
      } else {
        // if there is no return value but we are not in a void method or constructor, we are not in a happy path
        yield.exception = true;
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
    Preconditions.checkState(this.complete);
    return ImmutableList.<MethodYield>builder().addAll(yields).build();
  }

  List<MethodYield> reducedYields() {
    Preconditions.checkState(this.complete);
    if (reducedYields == null) {
      List<MethodYield> results = new ArrayList<>(yields.size());
      for (MethodYield yield : yields) {
        if (results.stream().noneMatch(yield::similarYield)) {
          results.add(yield);
        }
      }
      reducedYields = ImmutableList.<MethodYield>builder().addAll(results).build();
    }
    return reducedYields;
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
