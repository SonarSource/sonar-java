/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.UnresolvedIdentifiersVisitor;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.java.model.JUtils.isLocalVariable;
import static org.sonar.java.model.JUtils.isParameter;

@Rule(key = "S1481")
public class UnusedLocalVariableCheck extends IssuableSubscriptionVisitor {

  private static final Tree.Kind[] INCREMENT_KINDS = {
    Tree.Kind.POSTFIX_DECREMENT,
    Tree.Kind.POSTFIX_INCREMENT,
    Tree.Kind.PREFIX_DECREMENT,
    Tree.Kind.PREFIX_INCREMENT
  };

  private static final String MESSAGE = "Remove this unused \"%s\" local variable.";

  private static final UnresolvedIdentifiersVisitor UNRESOLVED_IDENTIFIERS_VISITOR = new UnresolvedIdentifiersVisitor();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.COMPILATION_UNIT, Tree.Kind.VARIABLE);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.COMPILATION_UNIT)) {
      UNRESOLVED_IDENTIFIERS_VISITOR.check(tree);
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Tree.Kind.VARIABLE)) {
      VariableTree variable = (VariableTree) tree;
      String name = variable.simpleName().name();
      boolean unresolved = UNRESOLVED_IDENTIFIERS_VISITOR.isUnresolved(name);
      if (!unresolved && isProperLocalVariable(variable) && isUnused(variable.symbol())) {
        reportIssue(variable.simpleName(), String.format(MESSAGE, name));
      }
    }
  }

  private static boolean isUnused(Symbol symbol) {
    return symbol.usages().stream().noneMatch(UnusedLocalVariableCheck::isRValue);
  }

  /**
   * An identifier is being used as an r-value if it is not used as the left operand of an assignment nor as the
   * operand of a stand-alone increment
   */
  private static boolean isRValue(IdentifierTree tree) {
    Tree parent = skipParenthesesUpwards(tree.parent());
    if (parent instanceof AssignmentExpressionTree) {
      AssignmentExpressionTree assignment = (AssignmentExpressionTree) parent;
      return assignment.variable() != tree;
    }
    // Note that an expression statement can't be a parenthesized expression, so we don't need to skip parentheses here
    return !(parent.is(INCREMENT_KINDS) && parent.parent().is(Tree.Kind.EXPRESSION_STATEMENT));
  }

  private static Tree skipParenthesesUpwards(Tree tree) {
    while (tree.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
      tree = tree.parent();
    }
    return tree;
  }

  private static boolean isProperLocalVariable(VariableTree variable) {
    Symbol symbol = variable.symbol();
    return isLocalVariable(symbol)
      && !isParameter(symbol)
      && !isDefinedInCatchClause(variable)
      && !isTryResource(variable);
  }

  private static boolean isDefinedInCatchClause(VariableTree variable) {
    return variable.parent().is(Tree.Kind.CATCH);
  }

  private static boolean isTryResource(VariableTree variable) {
    return variable.parent().is(Tree.Kind.LIST) && variable.parent().parent().is(Tree.Kind.TRY_STATEMENT);
  }

}
