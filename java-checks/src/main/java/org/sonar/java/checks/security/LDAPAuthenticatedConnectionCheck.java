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

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.JavaPropertiesHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S4433")
public class LDAPAuthenticatedConnectionCheck  extends AbstractMethodDetection {

  private static final String CONTEXT_CLASS_NAME = "Context";

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Collections.singletonList(
      MethodMatcher.create()
        .typeDefinition(TypeCriteria.is("java.util.Hashtable"))
        .name("put")
        .withAnyParameters());
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree methodTree) {
    if (methodTree.arguments().size() != 2) {
      return;
    }
    ExpressionTree putKey = methodTree.arguments().get(0);
    ExpressionTree putValue = methodTree.arguments().get(1);
    ExpressionTree defaultPropertyValue = JavaPropertiesHelper.retrievedPropertyDefaultValue(putValue);
    ExpressionTree mechanismTree = defaultPropertyValue == null ? putValue : defaultPropertyValue;
    if (isSecurityAuthentication(putKey) && isNone(mechanismTree)) {
      reportIssue(putValue, "Change authentication to \"simple\" or stronger.");
    }
  }

  private static boolean isSecurityAuthentication(ExpressionTree tree) {
    if (tree.is(Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree javaxConstant = (MemberSelectExpressionTree) tree;
      return isContextClass(javaxConstant.expression())
        && "SECURITY_AUTHENTICATION".equals(javaxConstant.identifier().name());
    }
    return false;
  }

  private static boolean isNone(ExpressionTree authenticationMechanism) {
    if (authenticationMechanism.is(Kind.STRING_LITERAL)) {
      String mechanismName = LiteralUtils.trimQuotes(((LiteralTree) authenticationMechanism).value());
      return "none".equals(mechanismName);
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
