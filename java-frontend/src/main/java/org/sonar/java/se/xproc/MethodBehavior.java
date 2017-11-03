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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.sonar.java.se.ExplodedGraph;
import org.sonar.java.se.checks.SECheck;
import org.sonar.java.se.constraint.ConstraintsByDomain;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.semantic.Type;

public class MethodBehavior {
  private boolean varArgs;
  private final int arity;

  private final Set<MethodYield> yields;
  private final List<SymbolicValue> parameters;
  private final String signature;
  private boolean complete = false;
  private boolean visited = false;
  private List<String> declaredExceptions;

  public MethodBehavior(String signature, boolean varArgs) {
    this.signature = signature;
    this.yields = new LinkedHashSet<>();
    this.parameters = new ArrayList<>();
    this.varArgs = varArgs;
    this.arity = org.objectweb.asm.Type.getArgumentTypes(signature.substring(signature.indexOf('('))).length;
    this.declaredExceptions = Collections.emptyList();
  }

  public MethodBehavior(String signature) {
    this(signature, false);
  }

  public void createYield(ExplodedGraph.Node node) {
    createYield(node, true);
  }

  public void createYield(ExplodedGraph.Node node, boolean storeNodeForReporting) {
    ExplodedGraph.Node nodeForYield = null;
    if(storeNodeForReporting) {
      nodeForYield = node;
    }
    MethodYield yield;
    boolean expectReturnValue = !(isConstructor() || isVoidMethod());
    SymbolicValue resultSV = node.programState.exitValue();

    if ((resultSV == null && expectReturnValue) || resultSV instanceof SymbolicValue.ExceptionalSymbolicValue) {
      ExceptionalYield exceptionalYield = new ExceptionalYield(nodeForYield, this);
      if (resultSV != null) {
        Type type = ((SymbolicValue.ExceptionalSymbolicValue) resultSV).exceptionType();
        String typeName = null;
        if(type != null) {
          typeName = type.fullyQualifiedName();
        }
        exceptionalYield.setExceptionType(typeName);
      }
      yield = exceptionalYield;
    } else {
      HappyPathYield happyPathYield = new HappyPathYield(nodeForYield, this);
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
    for (SymbolicValue parameter : parameters) {
      ConstraintsByDomain constraints = node.programState.getConstraints(parameter);
      if (constraints == null) {
        constraints = ConstraintsByDomain.empty();
      }
      yield.parametersConstraints.add(constraints);
    }
  }

  public ExceptionalYield createExceptionalCheckBasedYield(SymbolicValue target, ExplodedGraph.Node node, String exceptionType, SECheck check) {
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
    return org.objectweb.asm.Type.getReturnType(signature.substring(signature.indexOf('('))) == org.objectweb.asm.Type.VOID_TYPE;
  }

  private boolean isConstructor() {
    return signature.contains("<init>");
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
    this.visited = true;
  }

  public boolean isVisited() {
    return visited;
  }
  public void visited() {
    visited = true;
  }

  public String signature() {
    return signature;
  }

  public void setVarArgs(boolean varArgs) {
    this.varArgs = varArgs;
  }

  public List<String> getDeclaredExceptions() {
    return declaredExceptions;
  }

  public void setDeclaredExceptions(List<String> declaredExceptions) {
    this.declaredExceptions = declaredExceptions;
  }
}
