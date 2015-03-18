/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.MethodInvocationMatcher;
import org.sonar.java.resolve.Symbol;
import org.sonar.java.resolve.Symbol.VariableSymbol;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

@Rule(
  key = "S2696",
  name = "Instance methods should not write to \"static\" fields",
  tags = {"bug", "multi-threading"},
  priority = Priority.CRITICAL)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.SYNCHRONIZATION_RELIABILITY)
@SqaleConstantRemediation("20min")
public class StaticFieldUpateCheck extends AbstractInSynchronizeChecker {

  private static final Kind[] ASSIGNMENT_EXPRESSIONS = new Kind[] {
    Kind.AND_ASSIGNMENT,
    Kind.ASSIGNMENT,
    Kind.DIVIDE_ASSIGNMENT,
    Kind.LEFT_SHIFT_ASSIGNMENT,
    Kind.MINUS_ASSIGNMENT,
    Kind.MULTIPLY_ASSIGNMENT,
    Kind.OR_ASSIGNMENT,
    Kind.PLUS_ASSIGNMENT,
    Kind.REMAINDER_ASSIGNMENT,
    Kind.UNSIGNED_RIGHT_SHIFT_ASSIGNMENT,
    Kind.XOR_ASSIGNMENT};

  private static final Kind[] UNARY_EXPRESSIONS = new Kind[] {
    Kind.POSTFIX_DECREMENT,
    Kind.POSTFIX_INCREMENT,
    Kind.PREFIX_DECREMENT,
    Kind.PREFIX_INCREMENT};

  private Deque<Boolean> withinStaticMethod = new LinkedList<>();

  @Override
  public List<Kind> nodesToVisit() {
    List<Kind> results = Lists.newArrayList(super.nodesToVisit());
    results.addAll(Lists.newArrayList(ASSIGNMENT_EXPRESSIONS));
    results.addAll(Lists.newArrayList(UNARY_EXPRESSIONS));
    return results;
  }

  @Override
  public void visitNode(Tree tree) {
    // use AbstractInSynchronizeChecker logic to check synchronized blocks
    super.visitNode(tree);

    if (tree.is(Kind.METHOD)) {
      withinStaticMethod.push(isMethodStatic((MethodTree) tree));
    } else if (hasSemantic() && !isInSyncBlock() && isInInstancecMethod()) {
      if (tree.is(ASSIGNMENT_EXPRESSIONS)) {
        checkAssignement(((AssignmentExpressionTree) tree).variable());
      } else if (tree.is(UNARY_EXPRESSIONS)) {
        checkAssignement(((UnaryExpressionTree) tree).expression());
      }
    }
  }

  private boolean isInInstancecMethod() {
    return !withinStaticMethod.isEmpty() && !withinStaticMethod.peek();
  }

  private boolean isMethodStatic(MethodTree tree) {
    return tree.modifiers().modifiers().contains(Modifier.STATIC);
  }

  @Override
  public void leaveNode(Tree tree) {
    super.leaveNode(tree);

    if (tree.is(Tree.Kind.METHOD)) {
      withinStaticMethod.pop();
    }
  }

  private void checkAssignement(ExpressionTree expression) {
    if (expression.is(Kind.IDENTIFIER)) {
      Symbol variable = getSemanticModel().getReference((IdentifierTree) expression);
      if (variable != null && variable.isKind(Symbol.VAR)) {
        checkVariable((VariableSymbol) variable, expression);
      }
    } else if (expression.is(Kind.MEMBER_SELECT)) {
      checkAssignement(((MemberSelectExpressionTree) expression).identifier());
    } else if (expression.is(Kind.ARRAY_ACCESS_EXPRESSION)) {
      checkAssignement(((ArrayAccessExpressionTree) expression).expression());
    }
  }

  private void checkVariable(VariableSymbol variable, ExpressionTree expression) {
    if (isStaticField(variable)) {
      addIssue(expression, "Make the enclosing method \"static\" or remove this set.");
    }
  }

  private boolean isStaticField(VariableSymbol variable) {
    return variable.owner().isKind(Symbol.TYP) && variable.isStatic();
  }

  @Override
  protected List<MethodInvocationMatcher> getMethodInvocationMatchers() {
    return ImmutableList.of();
  }
}
