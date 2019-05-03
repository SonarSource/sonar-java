/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.checks.unused;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Rule(key = "S1481")
public class UnusedLocalVariableCheck extends IssuableSubscriptionVisitor {

  private static final Tree.Kind[] ASSIGNMENT_KINDS = {
    Tree.Kind.ASSIGNMENT,
    Tree.Kind.MULTIPLY_ASSIGNMENT,
    Tree.Kind.DIVIDE_ASSIGNMENT,
    Tree.Kind.REMAINDER_ASSIGNMENT,
    Tree.Kind.PLUS_ASSIGNMENT,
    Tree.Kind.MINUS_ASSIGNMENT,
    Tree.Kind.LEFT_SHIFT_ASSIGNMENT,
    Tree.Kind.RIGHT_SHIFT_ASSIGNMENT,
    Tree.Kind.UNSIGNED_RIGHT_SHIFT_ASSIGNMENT,
    Tree.Kind.AND_ASSIGNMENT,
    Tree.Kind.XOR_ASSIGNMENT,
    Tree.Kind.OR_ASSIGNMENT
  };

  private static final Tree.Kind[] INCREMENT_KINDS = {
    Tree.Kind.POSTFIX_DECREMENT,
    Tree.Kind.POSTFIX_INCREMENT,
    Tree.Kind.PREFIX_DECREMENT,
    Tree.Kind.PREFIX_INCREMENT
  };

  private List<VariableTree> variables = new ArrayList<>();
  private ListMultimap<Symbol, IdentifierTree> assignments = ArrayListMultimap.create();

  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(
      Tree.Kind.BLOCK, Tree.Kind.STATIC_INITIALIZER,
      Tree.Kind.FOR_STATEMENT, Tree.Kind.FOR_EACH_STATEMENT,
      Tree.Kind.EXPRESSION_STATEMENT, Tree.Kind.COMPILATION_UNIT);
  }

  @Override
  public void leaveNode(Tree tree) {
    if (hasSemantic()) {
      if (tree.is(Tree.Kind.BLOCK, Tree.Kind.STATIC_INITIALIZER)) {
        BlockTree blockTree = (BlockTree) tree;
        addVariables(blockTree.body());
      } else if (tree.is(Tree.Kind.FOR_STATEMENT)) {
        ForStatementTree forStatementTree = (ForStatementTree) tree;
        addVariables(forStatementTree.initializer());
      } else if (tree.is(Tree.Kind.FOR_EACH_STATEMENT)) {
        ForEachStatement forEachStatement = (ForEachStatement) tree;
        addVariable(forEachStatement.variable());
      } else if (tree.is(Tree.Kind.EXPRESSION_STATEMENT)) {
        leaveExpressionStatement((ExpressionStatementTree) tree);
      } else {
        checkVariableUsages();
        variables.clear();
        assignments.clear();
      }
    }
  }

  private void leaveExpressionStatement(ExpressionStatementTree expressionStatement) {
    ExpressionTree expression = expressionStatement.expression();
    if (expression.is(ASSIGNMENT_KINDS)) {
      addAssignment(((AssignmentExpressionTree) expression).variable());
    } else if (expression.is(INCREMENT_KINDS)) {
      addAssignment(((UnaryExpressionTree) expression).expression());
    }
  }

  private void checkVariableUsages() {
    for (VariableTree variableTree : variables) {
      Symbol symbol = variableTree.symbol();
      if (symbol.usages().size() == assignments.get(symbol).size()) {
        IdentifierTree simpleName = variableTree.simpleName();
        reportIssue(simpleName, "Remove this unused \"" + simpleName + "\" local variable.");
      }
    }
  }

  public void addVariables(List<StatementTree> statementTrees) {
    for (StatementTree statementTree : statementTrees) {
      if (statementTree.is(Tree.Kind.VARIABLE)) {
        addVariable((VariableTree) statementTree);
      }
    }
  }

  private void addVariable(VariableTree variableTree) {
    variables.add(variableTree);
  }

  private void addAssignment(ExpressionTree variable) {
    if (variable.is(Tree.Kind.IDENTIFIER)) {
      addAssignment((IdentifierTree) variable);
    }
  }

  private void addAssignment(IdentifierTree identifier) {
    Symbol reference = identifier.symbol();
    if (!reference.isUnknown()) {
      assignments.put(reference, identifier);
    }
  }

}
