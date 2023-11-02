/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;


@Rule(key = "S2233")
public class ResultSetTypeForwardUnsafeMethodsCheck extends IssuableSubscriptionVisitor {

  private final static MethodMatchers MATCHER = MethodMatchers.create()
    .ofTypes("java.sql.ResultSet")
    .names("isBeforeFirst", "isAfterLast", "isFirst", "getRow")
    .addWithoutParametersMatcher()
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree mit = (MethodInvocationTree) tree;
    if (MATCHER.matches(mit) && isForwardOnly(mit)) {
      reportIssue(mit, String.format("Remove this call to \"%s\".", mit.methodSymbol().name()));
    }
  }

  private boolean isForwardOnly(MethodInvocationTree mit) {
    MemberSelectExpressionTree methodSelect = (MemberSelectExpressionTree) ExpressionUtils.skipParentheses(mit.methodSelect());
    IdentifierTree resultSetIdentifier = (IdentifierTree) methodSelect.expression();
    return false;
  }

  private ExpressionTree getInitializer(IdentifierTree identifier) {
    return ((VariableTree) identifier.symbol().declaration()).initializer();
  }

}
