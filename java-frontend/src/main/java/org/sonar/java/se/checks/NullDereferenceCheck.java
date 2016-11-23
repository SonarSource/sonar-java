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
package org.sonar.java.se.checks;

import com.google.common.collect.Lists;
import org.sonar.check.Rule;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.ExplodedGraph;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Rule(key = "S2259")
public class NullDereferenceCheck extends SECheck {

  @Override
  public ProgramState checkPreStatement(CheckerContext context, Tree syntaxNode) {
    SymbolicValue currentVal = context.getState().peekValue();
    if (currentVal == null) {
      // stack is empty, nothing to do.
      return context.getState();
    }
    Tree toCheck = syntaxNode;
    if (syntaxNode.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree methodInvocation = (MethodInvocationTree) syntaxNode;
      toCheck = methodInvocation.methodSelect();
      int numberArguments = methodInvocation.arguments().size();
      List<SymbolicValue> values = context.getState().peekValues(numberArguments + 1);
      currentVal = values.get(numberArguments);
      if (isObjectsRequireNonNullMethod(methodInvocation.symbol())) {
        SymbolicValue firstArg = values.get(numberArguments - 1);
        return context.getState().addConstraint(firstArg, ObjectConstraint.NOT_NULL);
      }
    }
    if(toCheck.is(Tree.Kind.ARRAY_ACCESS_EXPRESSION)) {
      toCheck = ((ArrayAccessExpressionTree) toCheck).expression();
      currentVal = context.getState().peekValues(2).get(1);
      return checkConstraint(context, toCheck, currentVal);
    }
    if (toCheck.is(Tree.Kind.MEMBER_SELECT)) {
      return checkMemberSelect(context, (MemberSelectExpressionTree) toCheck, currentVal);
    }
    return context.getState();
  }

  private static boolean isObjectsRequireNonNullMethod(Symbol symbol) {
    return symbol.isMethodSymbol() && symbol.owner().type().is("java.util.Objects") && "requireNonNull".equals(symbol.name());
  }

  private ProgramState checkMemberSelect(CheckerContext context, MemberSelectExpressionTree mse, SymbolicValue currentVal) {
    if ("class".equals(mse.identifier().name())) {
      // expression ClassName.class won't raise NPE.
      return context.getState();
    }
    return checkConstraint(context, mse, currentVal);
  }

  private ProgramState checkConstraint(CheckerContext context, Tree syntaxNode, SymbolicValue currentVal) {
    ProgramState programState = context.getState();
    Constraint constraint = programState.getConstraint(currentVal);
    if (constraint != null && constraint.isNull()) {
      String message = "NullPointerException might be thrown as '" + SyntaxTreeNameFinder.getName(syntaxNode) + "' is nullable here";
      Set<List<JavaFileScannerContext.Location>> flows = new HashSet<>();
      flows.add(flow(context.getNode(), currentVal));
      context.reportIssue(syntaxNode, this, message, flows);
      return null;
    }
    constraint = programState.getConstraint(currentVal);
    if (constraint == null) {
      // We dereferenced the target value for the member select, so we can assume it is not null when not already known
      return programState.addConstraint(currentVal, ObjectConstraint.NOT_NULL);
    }
    return programState;
  }

  private static List<JavaFileScannerContext.Location> flow(ExplodedGraph.Node currentNode, SymbolicValue currentVal) {
    List<JavaFileScannerContext.Location> flow = new ArrayList<>();
    ExplodedGraph.Node node = currentNode;
    Symbol lastEvaluated = currentNode.programState.getLastEvaluated();
    while (node != null) {
      ExplodedGraph.Node finalNode = node;
      if(finalNode.programPoint.syntaxTree() != null) {
        node.learnedConstraints.stream()
          .map(lc->lc.sv)
          .filter(sv -> sv.equals(currentVal))
          .findFirst()
          .ifPresent(sv -> flow.add(new JavaFileScannerContext.Location("", finalNode.parent.programPoint.syntaxTree())));
        if (lastEvaluated != null) {
          Symbol finalLastEvaluated = lastEvaluated;
          Optional<Symbol> learnedSymbol = node.getLearnedSymbols().stream()
            .map(ls -> ls.symbol)
            .filter(sv -> sv.equals(finalLastEvaluated))
            .findFirst();
          if (learnedSymbol.isPresent()) {
            lastEvaluated = finalNode.parent.programState.getLastEvaluated();
            flow.add(new JavaFileScannerContext.Location("", finalNode.parent.programPoint.syntaxTree()));
          }
        }

      }
      node = node.parent;
    }
    return flow;
  }

  @Override
  public ProgramState checkPostStatement(CheckerContext context, Tree syntaxNode) {
    if (syntaxNode.is(Tree.Kind.SWITCH_STATEMENT, Tree.Kind.THROW_STATEMENT) && context.getConstraintManager().isNull(context.getState(), context.getState().peekValue())) {
      context.reportIssue(syntaxNode, this, "NullPointerException might be thrown as '" + SyntaxTreeNameFinder.getName(syntaxNode) + "' is nullable here");
      context.createSink();
      return context.getState();
    }
    List<ProgramState> programStates = setNullConstraint(context, syntaxNode);
    for (ProgramState programState : programStates) {
      context.addTransition(programState);
    }
    return context.getState();
  }

  private static List<ProgramState> setNullConstraint(CheckerContext context, Tree syntaxNode) {
    SymbolicValue val = context.getState().peekValue();
    if (syntaxNode.is(Tree.Kind.METHOD_INVOCATION) && isAnnotatedCheckForNull((MethodInvocationTree) syntaxNode)) {
      List<ProgramState> states = new ArrayList<>();
      states.addAll(val.setConstraint(context.getState(), ObjectConstraint.nullConstraint(syntaxNode)));
      states.addAll(val.setConstraint(context.getState(), ObjectConstraint.NOT_NULL));
      return states;
    }
    return Lists.newArrayList(context.getState());
  }

  private static boolean isAnnotatedCheckForNull(MethodInvocationTree syntaxNode) {
    return syntaxNode.symbol().metadata().isAnnotatedWith("javax.annotation.CheckForNull");
  }

}
