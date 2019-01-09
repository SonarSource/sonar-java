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
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S4433")
public class LDAPAuthenticatedConnectionCheck extends AbstractMethodDetection {

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Collections.singletonList(
      MethodMatcher.create()
        .typeDefinition(TypeCriteria.subtypeOf("java.util.Map"))
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
    if (isSecurityAuthenticationConstant(putKey) && LiteralUtils.hasValue(mechanismTree, "none")) {
      reportIssue(putValue, "Change authentication to \"simple\" or stronger.");
    }
  }

  private static boolean isSecurityAuthenticationConstant(ExpressionTree tree) {
    if (tree.is(Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree constantExpression = (MemberSelectExpressionTree) tree;
      return "javax.naming.Context".equals(constantExpression.expression().symbolType().fullyQualifiedName())
        && "SECURITY_AUTHENTICATION".equals(constantExpression.identifier().name());
    }
    return LiteralUtils.hasValue(tree, "java.naming.security.authentication");
  }
}
