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
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Rule(key = "S2196")
public class SwitchInsteadOfIfSequenceCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.IF_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    if(tree.parent().is(Tree.Kind.IF_STATEMENT)) {
      IfStatementTree parentIf = (IfStatementTree) tree.parent();
      if(tree.equals(parentIf.elseStatement())) {
        // don't double count nested ifs
        return;
      }
    }
    IfStatementTree ifStatementTree = (IfStatementTree) tree;
    int level = 1;
    while (ifStatementTree.elseKeyword() != null && ifStatementTree.elseStatement().is(Tree.Kind.IF_STATEMENT)) {
      level++;
      if(!sameEqualCondition(ifStatementTree.condition(), ((IfStatementTree) ifStatementTree.elseStatement()).condition())) {
        level = 0;
        break;
      }
      ifStatementTree = (IfStatementTree) ifStatementTree.elseStatement();
    }
    if(level > 2) {
      reportIssue(((IfStatementTree) tree).condition(), "Convert this \"if/else if\" structure into a \"switch\"." + context.getJavaVersion().java7CompatibilityMessage());
    }

  }

  private static boolean sameEqualCondition(ExpressionTree firstIfCondition, ExpressionTree secondIfCondition) {
    Optional<EqualsOperands> equalsOperandsFirst = getEqualMethodInvocationOperands(firstIfCondition);
    Optional<EqualsOperands> equalsOperandsSecond = getEqualMethodInvocationOperands(secondIfCondition);
    return equalsOperandsFirst.isPresent()
    && equalsOperandsSecond.isPresent()
    && equalsOperandsFirst.get().identifier.symbol().equals(equalsOperandsSecond.get().identifier.symbol());
  }

  private static Optional<EqualsOperands> getEqualMethodInvocationOperands(ExpressionTree expressionTree) {
    ExpressionTree arg = null;
    ExpressionTree expression = null;
    if (expressionTree.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) expressionTree;
      Symbol symbol = mit.symbol();
      ExpressionTree methodSelect = mit.methodSelect();
      if (mit.arguments().size() == 1) {
        arg = mit.arguments().get(0);
        if ("equals".equals(symbol.name()) && arg.symbolType().is("java.lang.String") && methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
          expression = ((MemberSelectExpressionTree) methodSelect).expression();
        }
      }
    } else if (expressionTree.is(Tree.Kind.EQUAL_TO)) {
      BinaryExpressionTree equalTo = (BinaryExpressionTree) expressionTree;
      arg = equalTo.leftOperand();
      expression = equalTo.rightOperand();
    }
    if (arg != null && expression != null) {
      if (arg.is(Tree.Kind.STRING_LITERAL) && expression.is(Tree.Kind.IDENTIFIER)) {
        return Optional.of(new EqualsOperands((LiteralTree) arg, (IdentifierTree) expression));
      } else if (arg.is(Tree.Kind.IDENTIFIER) && expression.is(Tree.Kind.STRING_LITERAL)) {
        return Optional.of(new EqualsOperands((LiteralTree) expression, (IdentifierTree) arg));
      }
    }
    return Optional.empty();
  }

  private static class EqualsOperands {
    LiteralTree literal;
    IdentifierTree identifier;

    EqualsOperands(LiteralTree literal, IdentifierTree identifier) {
      this.literal = literal;
      this.identifier = identifier;
    }
  }

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava7Compatible();
  }
}
