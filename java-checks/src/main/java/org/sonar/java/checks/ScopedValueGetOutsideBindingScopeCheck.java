/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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

import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.checks.helpers.TreeHelper.findClosestParentOfKind;

@Rule(key = "S8459")
public class ScopedValueGetOutsideBindingScopeCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  private static final String MESSAGE = "Move this \"get()\" call inside a \"run()\" or \"call()\" block where the ScopedValue is bound.";

  private static final MethodMatchers SCOPED_VALUE_GET = MethodMatchers.create()
    .ofTypes("java.lang.ScopedValue")
    .names("get")
    .addWithoutParametersMatcher()
    .build();

  private static final MethodMatchers CARRIER_RUN_OR_CALL = MethodMatchers.create()
    .ofTypes("java.lang.ScopedValue$Carrier")
    .names("run", "call")
    .withAnyParameters()
    .build();

  private static final Set<Tree.Kind> LAMBDA_KIND = Set.of(Tree.Kind.LAMBDA_EXPRESSION);

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava25Compatible();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    var mit = (MethodInvocationTree) tree;
    if (SCOPED_VALUE_GET.matches(mit) && !isInsideCarrierRunOrCall(mit)) {
      reportIssue(ExpressionUtils.methodName(mit), MESSAGE);
    }
  }

  private static boolean isInsideCarrierRunOrCall(Tree tree) {
    Tree current = tree;
    while (current != null) {
      if (findClosestParentOfKind(current, LAMBDA_KIND) instanceof LambdaExpressionTree lambda
        && isLambdaArgumentOfRunOrCall(lambda)) {
        return true;
      }
      current = findClosestParentOfKind(current, LAMBDA_KIND);
      if (current != null) {
        current = current.parent();
      }
    }
    return false;
  }

  private static boolean isLambdaArgumentOfRunOrCall(LambdaExpressionTree lambda) {
    Tree parent = lambda.parent();
    if (parent != null && parent.is(Tree.Kind.ARGUMENTS)) {
      Tree grandParent = parent.parent();
      return grandParent instanceof MethodInvocationTree enclosingMit && CARRIER_RUN_OR_CALL.matches(enclosingMit);
    }
    return false;
  }

}
