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

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2912")
public class IndexOfStartPositionCheck extends IssuableSubscriptionVisitor {

  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final MethodMatcher INDEX_OF_METHOD = MethodMatcher.create()
    .typeDefinition(JAVA_LANG_STRING).name("indexOf").addParameter(JAVA_LANG_STRING);

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(
      Tree.Kind.GREATER_THAN, Tree.Kind.GREATER_THAN_OR_EQUAL_TO,
      Tree.Kind.LESS_THAN, Tree.Kind.LESS_THAN_OR_EQUAL_TO,
      Tree.Kind.EQUAL_TO, Tree.Kind.NOT_EQUAL_TO);
  }

  @Override
  public void visitNode(Tree tree) {
    ExpressionTree leftOperand = ExpressionUtils.skipParentheses(((BinaryExpressionTree) tree).leftOperand());
    ExpressionTree rightOperand = ExpressionUtils.skipParentheses(((BinaryExpressionTree) tree).rightOperand());
    if (leftOperand.is(Tree.Kind.METHOD_INVOCATION)) {
      checkIndexOfInvocation((MethodInvocationTree) leftOperand, rightOperand);
    } else if (rightOperand.is(Tree.Kind.METHOD_INVOCATION)) {
      checkIndexOfInvocation((MethodInvocationTree) rightOperand, leftOperand);
    }
  }

  private void checkIndexOfInvocation(MethodInvocationTree mit, ExpressionTree other) {
    if (INDEX_OF_METHOD.matches(mit)) {
      String replaceMessage;
      ExpressionTree firstPar = mit.arguments().get(0);
      if (firstPar.is(Tree.Kind.STRING_LITERAL)) {
        replaceMessage = ((LiteralTree) firstPar).value();
      } else if (firstPar.is(Tree.Kind.IDENTIFIER)) {
        replaceMessage = ((IdentifierTree) firstPar).name();
      } else {
        replaceMessage = "xxx";
      }
      Long otherValue = LiteralUtils.longLiteralValue(other);
      if (otherValue != null && otherValue != -1 && otherValue != 0) {
        reportIssue(ExpressionUtils.methodName(mit), "Use \".indexOf(" + replaceMessage + ",n) > -1\" instead.");
      }
    }
  }

}
