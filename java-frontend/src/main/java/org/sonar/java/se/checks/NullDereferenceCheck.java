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
package org.sonar.java.se.checks;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.sonar.check.Rule;
import org.sonar.java.cfg.CFG;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.ExplodedGraph;
import org.sonar.java.se.FlowComputation;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Rule(key = "S2259")
public class NullDereferenceCheck extends SECheck {

  private static final ExceptionalYieldChecker EXCEPTIONAL_YIELD_CHECKER = new ExceptionalYieldChecker(
    "NullPointerException will be thrown when invoking method %s().");

  private static final String JAVA_LANG_NPE = "java.lang.NullPointerException";

  private static class NullDereferenceIssue {
    final ExplodedGraph.Node node;
    final SymbolicValue symbolicValue;
    final Tree tree;

    private NullDereferenceIssue(ExplodedGraph.Node node, SymbolicValue symbolicValue, Tree tree) {
      this.node = node;
      this.symbolicValue = symbolicValue;
      this.tree = tree;
    }
  }

  private Deque<Set<NullDereferenceIssue>> detectedIssues = new ArrayDeque<>();

  @Override
  public void init(MethodTree methodTree, CFG cfg) {
    detectedIssues.push(new HashSet<>());
  }

  @Override
  public ProgramState checkPreStatement(CheckerContext context, Tree syntaxNode) {
    if (context.getState().peekValue() == null) {
      // stack is empty, nothing to do.
      return context.getState();
    }
    if (syntaxNode.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree methodInvocation = (MethodInvocationTree) syntaxNode;
      Tree methodSelect = methodInvocation.methodSelect();
      if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
        SymbolicValue dereferencedSV = context.getState().peekValue(methodInvocation.arguments().size());
        return checkConstraint(context, methodSelect, dereferencedSV);
      }
    }
    if(syntaxNode.is(Tree.Kind.ARRAY_ACCESS_EXPRESSION)) {
      Tree toCheck = ((ArrayAccessExpressionTree) syntaxNode).expression();
      SymbolicValue currentVal = context.getState().peekValue(1);
      return checkConstraint(context, toCheck, currentVal);
    }
    if (syntaxNode.is(Tree.Kind.MEMBER_SELECT)) {
      return checkMemberSelect(context, (MemberSelectExpressionTree) syntaxNode, context.getState().peekValue());
    }
    return context.getState();
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
    ObjectConstraint constraint = programState.getConstraint(currentVal, ObjectConstraint.class);
    if (constraint != null && constraint.isNull()) {
      NullDereferenceIssue issue = new NullDereferenceIssue(context.getNode(), currentVal, syntaxNode);
      detectedIssues.peek().add(issue);

      // we reported the issue and stopped the exploration, but we still need to create a yield for x-procedural calls
      context.addExceptionalYield(currentVal, programState, JAVA_LANG_NPE, this);
      return null;
    }
    constraint = programState.getConstraint(currentVal, ObjectConstraint.class);
    if (constraint == null) {
      // a NPE will be triggered if the current value would have been null
      context.addExceptionalYield(currentVal, programState.addConstraint(currentVal, ObjectConstraint.NULL), JAVA_LANG_NPE, this);

      // We dereferenced the target value for the member select, so we can assume it is not null when not already known
      return programState.addConstraint(currentVal, ObjectConstraint.NOT_NULL);
    }
    return programState;
  }

  private void reportIssue(SymbolicValue currentVal, Tree syntaxNode, ExplodedGraph.Node node) {
    String message = "NullPointerException might be thrown as '" + SyntaxTreeNameFinder.getName(syntaxNode) + "' is nullable here";
    SymbolicValue val = null;
    if (!SymbolicValue.NULL_LITERAL.equals(currentVal)) {
      val = currentVal;
    }
    Symbol dereferencedSymbol = dereferencedSymbol(syntaxNode);
    Set<List<JavaFileScannerContext.Location>> flows = FlowComputation.flow(node, val, Lists.newArrayList(ObjectConstraint.class), dereferencedSymbol).stream()
      .map(f -> addDereferenceMessage(f, syntaxNode))
      .collect(Collectors.toSet());
    reportIssue(syntaxNode, message, flows);
  }

  @Nullable
  private static Symbol dereferencedSymbol(Tree syntaxNode) {
    if (syntaxNode.is(Tree.Kind.MEMBER_SELECT)) {
      ExpressionTree memberSelectExpr = ((MemberSelectExpressionTree) syntaxNode).expression();
      if (memberSelectExpr.is(Tree.Kind.IDENTIFIER)) {
        return ((IdentifierTree) memberSelectExpr).symbol();
      }
    }
    return null;
  }

  private static List<JavaFileScannerContext.Location> addDereferenceMessage(List<JavaFileScannerContext.Location> flow, Tree syntaxNode) {
    String symbolName = SyntaxTreeNameFinder.getName(syntaxNode);
    String msg;
    if (syntaxNode.is(Tree.Kind.MEMBER_SELECT) && ((MemberSelectExpressionTree) syntaxNode).expression().is(Tree.Kind.METHOD_INVOCATION)) {
      msg = String.format("Result of '%s()' is dereferenced.", symbolName);
    } else {
      msg = String.format("'%s' is dereferenced.", symbolName);
    }
    return ImmutableList.<JavaFileScannerContext.Location>builder()
      .add(new JavaFileScannerContext.Location(msg, syntaxNode))
      .addAll(flow)
      .build();
  }

  @Override
  public ProgramState checkPostStatement(CheckerContext context, Tree syntaxNode) {
    if (syntaxNode.is(Tree.Kind.SWITCH_STATEMENT, Tree.Kind.THROW_STATEMENT) && context.getConstraintManager().isNull(context.getState(), context.getState().peekValue())) {
      NullDereferenceIssue issue = new NullDereferenceIssue(context.getNode(), context.getState().peekValue(), syntaxNode);
      detectedIssues.peek().add(issue);
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
      Preconditions.checkNotNull(val);
      List<ProgramState> states = new ArrayList<>();
      states.addAll(val.setConstraint(context.getState(), ObjectConstraint.NULL));
      states.addAll(val.setConstraint(context.getState(), ObjectConstraint.NOT_NULL));
      return states;
    }
    return Lists.newArrayList(context.getState());
  }

  private static boolean isAnnotatedCheckForNull(MethodInvocationTree syntaxNode) {
    return syntaxNode.symbol().metadata().isAnnotatedWith("javax.annotation.CheckForNull");
  }

  @Override
  public void checkEndOfExecutionPath(CheckerContext context, ConstraintManager constraintManager) {
    EXCEPTIONAL_YIELD_CHECKER.reportOnExceptionalYield(context.getNode(), this);
  }

  @Override
  public void checkEndOfExecution(CheckerContext context) {
    reportIssues();
  }

  @Override
  public void interruptedExecution(CheckerContext context) {
    reportIssues();
  }

  private void reportIssues() {
    Set<NullDereferenceIssue> issues = detectedIssues.pop();
    issues.forEach(issue -> reportIssue(issue.symbolicValue, issue.tree, issue.node));
  }
}
