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

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Table;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Set;

@Rule(
  key = "S864",
  name = "Limited dependence should be placed on operator precedence rules in expressions",
  tags = {"cert", "cwe", "misra"},
  priority = Priority.MAJOR)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("2min")
public class OperatorPrecedenceCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final Table<Tree.Kind, Tree.Kind, Boolean> TABLE;

  private static final Set<Tree.Kind> ARITHMETIC_OPERATORS = ImmutableSet.of(
    Tree.Kind.MINUS,
    Tree.Kind.REMAINDER,
    Tree.Kind.MULTIPLY,
    Tree.Kind.PLUS
    );

  private static final Set<Tree.Kind> SHIFT_OPERATORS = ImmutableSet.of(
    Tree.Kind.LEFT_SHIFT,
    Tree.Kind.RIGHT_SHIFT,
    Tree.Kind.UNSIGNED_RIGHT_SHIFT
    );

  private static void put(Iterable<Tree.Kind> firstSet, Iterable<Tree.Kind> secondSet) {
    for (Tree.Kind first : firstSet) {
      for (Tree.Kind second : secondSet) {
        TABLE.put(first, second, true);
      }
    }
  }

  static {
    TABLE = HashBasedTable.create();
    put(ARITHMETIC_OPERATORS, Iterables.concat(SHIFT_OPERATORS, ImmutableSet.of(Tree.Kind.AND, Tree.Kind.XOR, Tree.Kind.OR)));
    put(SHIFT_OPERATORS, Iterables.concat(ARITHMETIC_OPERATORS, ImmutableSet.of(Tree.Kind.AND, Tree.Kind.XOR, Tree.Kind.OR)));
    put(ImmutableSet.of(Tree.Kind.AND), Iterables.concat(ARITHMETIC_OPERATORS, SHIFT_OPERATORS, ImmutableSet.of(Tree.Kind.XOR, Tree.Kind.OR)));
    put(ImmutableSet.of(Tree.Kind.XOR), Iterables.concat(ARITHMETIC_OPERATORS, SHIFT_OPERATORS, ImmutableSet.of(Tree.Kind.AND, Tree.Kind.OR)));
    put(ImmutableSet.of(Tree.Kind.OR), Iterables.concat(ARITHMETIC_OPERATORS, SHIFT_OPERATORS, ImmutableSet.of(Tree.Kind.AND, Tree.Kind.XOR)));
    put(ImmutableSet.of(Tree.Kind.CONDITIONAL_AND), ImmutableSet.of(Tree.Kind.CONDITIONAL_OR));
    put(ImmutableSet.of(Tree.Kind.CONDITIONAL_OR), ImmutableSet.of(Tree.Kind.CONDITIONAL_AND));
  }

  private JavaFileScannerContext context;
  private Deque<Tree.Kind> stack = new LinkedList<>();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitAnnotation(AnnotationTree tree) {
    stack.push(null);
    for (ExpressionTree argument : tree.arguments()) {
      if (argument.is(Tree.Kind.ASSIGNMENT)) {
        scan(((AssignmentExpressionTree) argument).expression());
      } else {
        scan(argument);
      }
    }
    stack.pop();
  }

  @Override
  public void visitArrayAccessExpression(ArrayAccessExpressionTree tree) {
    scan(tree.expression());
    stack.push(null);
    scan(tree.index());
    stack.pop();
  }

  @Override
  public void visitBinaryExpression(BinaryExpressionTree tree) {
    Tree.Kind peek = stack.peek();
    Tree.Kind kind = getKind(tree);
    if (Boolean.TRUE.equals(TABLE.get(peek, kind))) {
      raiseIssue(tree);
    }
    stack.push(kind);
    super.visitBinaryExpression(tree);
    stack.pop();
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    scan(tree.methodSelect());
    scan(tree.typeArguments());
    for (ExpressionTree argument : tree.arguments()) {
      stack.push(null);
      scan(argument);
      stack.pop();
    }
  }

  @Override
  public void visitNewArray(NewArrayTree tree) {
    stack.push(null);
    super.visitNewArray(tree);
    stack.pop();
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    stack.push(null);
    super.visitNewClass(tree);
    stack.pop();
  }

  @Override
  public void visitParenthesized(ParenthesizedTree tree) {
    stack.push(null);
    super.visitParenthesized(tree);
    stack.pop();
  }

  private void raiseIssue(Tree tree) {
    context.addIssue(tree, this, "Add parentheses to make the operator precedence explicit.");
  }

  private Tree.Kind getKind(Tree tree) {
    return ((JavaTree) tree).getKind();
  }

}
