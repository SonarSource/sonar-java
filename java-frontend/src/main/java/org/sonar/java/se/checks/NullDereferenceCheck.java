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
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.ArrayList;
import java.util.List;

@Rule(
  key = "S2259",
  name = "Null pointers should not be dereferenced",
  priority = Priority.BLOCKER)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.LOGIC_RELIABILITY)
@SqaleConstantRemediation("10min")
@ActivatedByDefault
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
      if (isObjectsRequireNonNullMethod(methodInvocation.symbol())) {
        int numberArguments = methodInvocation.arguments().size();
        List<SymbolicValue> values = context.getState().peekValues(numberArguments + 1);
        return context.getState().addConstraint(values.get(numberArguments), ObjectConstraint.NOT_NULL);
      }
    }
    if (toCheck.is(Tree.Kind.MEMBER_SELECT)) {
      return checkMemberSelect(context, (MemberSelectExpressionTree) toCheck, currentVal);
    }
    return context.getState();
  }

  private static boolean isObjectsRequireNonNullMethod(Symbol symbol) {
    return symbol.owner().type().is("java.util.Objects") && "requireNonNull".equals(symbol.name());
  }

  private ProgramState checkMemberSelect(CheckerContext context, MemberSelectExpressionTree syntaxNode, SymbolicValue currentVal) {
    final ProgramState programState = context.getState();
    if ("class".equals(syntaxNode.identifier().name())) {
      // expression ClassName.class won't raise NPE.
      return programState;
    }

    Constraint constraint = programState.getConstraint(currentVal);
    if (constraint != null && constraint.isNull()) {
      List<JavaFileScannerContext.Location> secondary = new ArrayList<>();
      if(((ObjectConstraint) constraint).syntaxNode() != null ) {
        secondary.add(new JavaFileScannerContext.Location("", ((ObjectConstraint) constraint).syntaxNode()));
      }
      context.reportIssue(syntaxNode, this, "NullPointerException might be thrown as '" + SyntaxTreeNameFinder.getName(syntaxNode) + "' is nullable here", secondary);
      return null;
    }
    SymbolicValue targetValue = programState.peekValue();
    constraint = programState.getConstraint(targetValue);
    if (constraint == null) {
      // We dereferenced the target value for the member select, so we can assume it is not null when not already known
      return programState.addConstraint(targetValue, ObjectConstraint.NOT_NULL);
    }
    return programState;
  }

  @Override
  public ProgramState checkPostStatement(CheckerContext context, Tree syntaxNode) {
    if (syntaxNode.is(Tree.Kind.SWITCH_STATEMENT) && context.getConstraintManager().isNull(context.getState(), context.getState().peekValue())) {
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
