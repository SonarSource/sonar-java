/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;

@DeprecatedRuleKey(ruleKey = "ObjectFinalizeCheck", repositoryKey = "squid")
@Rule(key = "S1111")
public class ObjectFinalizeCheck extends IssuableSubscriptionVisitor {

  private boolean isInFinalizeMethod = false;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD)) {
      isInFinalizeMethod = isFinalizeMethodMember((MethodTree) tree);
    } else {
      MethodInvocationTree methodInvocationTree = (MethodInvocationTree) tree;
      IdentifierTree methodName = ExpressionUtils.methodName(methodInvocationTree);
      if (!isInFinalizeMethod && "finalize".equals(methodName.name()) && methodInvocationTree.arguments().isEmpty()) {
        reportIssue(methodName, "Remove this call to finalize().");
      }
    }

  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD) && isFinalizeMethodMember((MethodTree) tree)) {
      isInFinalizeMethod = false;
    }
  }

  private static boolean isFinalizeMethodMember(MethodTree methodTree) {
    Tree returnType = methodTree.returnType();
    boolean returnVoid = returnType != null && returnType.is(Tree.Kind.PRIMITIVE_TYPE) && "void".equals(((PrimitiveTypeTree) returnType).keyword().text());
    return returnVoid && "finalize".equals(methodTree.simpleName().name());
  }

}
