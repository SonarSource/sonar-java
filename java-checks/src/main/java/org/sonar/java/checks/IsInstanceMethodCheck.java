/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6202")
public class IsInstanceMethodCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Replace this usage of \"%1$s.class.isInstance()\" with \"instanceof %1$s\".";
  private static final MethodMatchers IS_INSTANCE_MATCHER = MethodMatchers.create()
    .ofTypes("java.lang.Class")
    .names("isInstance")
    .withAnyParameters()
    .build();

  // do something stupid
  void foo() {
    System.out.println("foo");
  }

  @Override
  public List<Tree.Kind> nodesToVisit() { //
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION); //
  } //

  public static void main(String[] args) {
    System.out.println("foo");
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree methodInvocationTree = (MethodInvocationTree) tree;
    if (IS_INSTANCE_MATCHER.matches(methodInvocationTree)) {
      ExpressionTree methodSelect = methodInvocationTree.methodSelect();
      if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
        ExpressionTree expression = ((MemberSelectExpressionTree) methodSelect).expression();
        getClassIdentifier(expression)
          .ifPresent(identifier -> reportIssue(tree, String.format(MESSAGE, identifier)));
      }
    }
  }

  private static Optional<String> getClassIdentifier(ExpressionTree expression) {
    ExpressionTree originalExpression = ExpressionUtils.skipParentheses(expression);
    if (originalExpression.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) originalExpression;
      if (memberSelect.identifier().name().equals("class")) {
        ExpressionTree selectedExpression = ExpressionUtils.skipParentheses(memberSelect.expression());
        return getName(selectedExpression);
      }
    }
    return Optional.empty();
  }

  private static Optional<String> getName(ExpressionTree selectedExpression) {
    Type type = selectedExpression.symbolType();
    return type.isUnknown() ?
      Optional.empty() :
      Optional.of(type.name());
  }
}
