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

import com.google.common.collect.ImmutableList;

import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MethodBehavior {
  private final Symbol.MethodSymbol methodSymbol;
  private final List<MethodYield> yields;
  private final Map<Symbol, SymbolicValue> parameters;

  public MethodBehavior(Symbol.MethodSymbol methodSymbol) {
    this.methodSymbol = methodSymbol;
    this.yields = new ArrayList<>();
    this.parameters = new LinkedHashMap<>();
  }

  public void createYield(ProgramState programState) {
    List<MethodYield.ConstrainedSymbolicValue> parametersCSV = methodSymbol.declaration().parameters().stream()
      .map(VariableTree::symbol)
      .map(parameters::get)
      .map(sv -> new MethodYield.ConstrainedSymbolicValue(sv, programState.getConstraint(sv)))
      .collect(Collectors.toList());

    MethodYield.ConstrainedSymbolicValue returnCSV = null;
    if (!isConstructor() && !isVoidMethod()) {
      SymbolicValue returnSV = programState.peekValue();
      if (returnSV != null) {
        returnCSV = new MethodYield.ConstrainedSymbolicValue(returnSV, programState.getConstraint(returnSV));
      } else {
        // FIXME Handle exception path
      }
    }

    yields.add(new MethodYield(parametersCSV, returnCSV));
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

  public void addParameter(Symbol symbol, SymbolicValue sv) {
    parameters.put(symbol, sv);
  }

  public Collection<SymbolicValue> parameters() {
    return parameters.values();
  }

}
