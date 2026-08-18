/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S9351")
public class BigDecimalEqualsCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "\"BigDecimal.equals()\" compares scale as well as value; use \"compareTo() == 0\" for numerical comparison.";
  private static final String BIG_DECIMAL = "java.math.BigDecimal";
  private static final String JAVA_LANG_OBJECT = "java.lang.Object";

  private static final MethodMatchers INSTANCE_EQUALS = MethodMatchers.create()
    .ofAnyType()
    .names("equals")
    .addParametersMatcher(JAVA_LANG_OBJECT)
    .build();

  private static final MethodMatchers STATIC_EQUALS = MethodMatchers.create()
    .ofTypes("java.util.Objects", "com.google.common.base.Objects")
    .names("equals", "equal")
    .addParametersMatcher(JAVA_LANG_OBJECT, JAVA_LANG_OBJECT)
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree mit = (MethodInvocationTree) tree;
    if (isInsideEqualsMethod(mit)) {
      return;
    }
    if (INSTANCE_EQUALS.matches(mit)) {
      Type ownerType = getMethodOwnerType(mit);
      Arguments arguments = mit.arguments();
      Type argumentType = arguments.get(0).symbolType();
      if (isBigDecimal(ownerType) || isBigDecimal(argumentType)) {
        reportIssue(ExpressionUtils.methodName(mit), MESSAGE);
      }
    } else if (STATIC_EQUALS.matches(mit)) {
      Arguments arguments = mit.arguments();
      Type firstType = arguments.get(0).symbolType();
      Type secondType = arguments.get(1).symbolType();
      if (isBigDecimal(firstType) || isBigDecimal(secondType)) {
        reportIssue(ExpressionUtils.methodName(mit), MESSAGE);
      }
    }
  }

  private static boolean isBigDecimal(Type type) {
    return !type.isUnknown() && type.isSubtypeOf(BIG_DECIMAL);
  }

  private static Type getMethodOwnerType(MethodInvocationTree mit) {
    if (mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      return ((MemberSelectExpressionTree) mit.methodSelect()).expression().symbolType();
    }
    return mit.methodSymbol().owner().type();
  }

  private static boolean isInsideEqualsMethod(Tree tree) {
    Tree parent = tree.parent();
    while (parent != null && !parent.is(Tree.Kind.CLASS, Tree.Kind.RECORD, Tree.Kind.INTERFACE, Tree.Kind.ENUM)) {
      if (parent.is(Tree.Kind.METHOD)) {
        return MethodTreeUtils.isEqualsMethod((MethodTree) parent);
      }
      parent = parent.parent();
    }
    return false;
  }
}
