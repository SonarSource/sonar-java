/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
package org.sonar.java.checks.security;

import org.sonar.check.Rule;
import org.sonar.java.checks.AbstractInjectionChecker;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S4433")
public class LDAPAuthenticatedConnectionCheck extends AbstractInjectionChecker {

  private final static String CONTEXT_CLASS_NAME = "Context";

  @Override
  public void visitNode(Tree tree) {
    if(!hasSemantic()) {
      return;
    }
    MethodInvocationTree methodTree = (MethodInvocationTree) tree;
    if (isPutMethod(methodTree) && methodTree.arguments().size() == 2) {
      Tree putKey = methodTree.arguments().get(0);
      Tree putValue = methodTree.arguments().get(1);
      if (isSoughtKey(putKey) && isSoughtValue(putValue)) {
        reportIssue(putValue, "Change authentication to \"simple\" or stronger.");
      }
    }
  }

  private static boolean isPutMethod(MethodInvocationTree methodTree) {
    if (methodTree.methodSelect().is(Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree methodSelect = (MemberSelectExpressionTree) methodTree.methodSelect();
      return "put".equals(methodSelect.identifier().name());
    }
    return false;
  }

  private static boolean isSoughtKey(Tree tree) {
    if (tree.is(Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree javaxConstant = (MemberSelectExpressionTree) tree;
      return isContextClass(javaxConstant.expression())
        && "SECURITY_AUTHENTICATION".equals(javaxConstant.identifier().name());
    }
    return false;
  }

  private static boolean isSoughtValue(Tree tree) {
    if (tree.is(Kind.STRING_LITERAL)) {
      LiteralTree lt = (LiteralTree) tree;
      return "\"none\"".equals(lt.value());
    }
    return false;
  }

  private static boolean isContextClass(ExpressionTree expressionTree) {
    if (expressionTree.is(Kind.IDENTIFIER)) {
      return CONTEXT_CLASS_NAME.equals(((IdentifierTree) expressionTree).name());
    }
    return isFullyQualifiedContextClass(expressionTree);
  }

  private static boolean isFullyQualifiedContextClass(ExpressionTree expressionTree) {
    if (expressionTree.is(Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree fullyQualifiedName = (MemberSelectExpressionTree) expressionTree;
      if (fullyQualifiedName.expression().is(Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree packageName = (MemberSelectExpressionTree) fullyQualifiedName.expression();
        if (packageName.expression().is(Kind.IDENTIFIER)) {
          return "javax".equals(((IdentifierTree) packageName.expression()).name())
              && "naming".equals(packageName.identifier().name())
              && CONTEXT_CLASS_NAME.equals(fullyQualifiedName.identifier().name());
        }
      }
    }
    return false;
  }
}
