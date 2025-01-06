/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S1182")
public class CloneMethodCallsSuperCloneCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return List.of(Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;

    if (isCloneMethod(methodTree)) {
      var visitor = new CloneSuperCallVisitor();
      tree.accept(visitor);

      if (!visitor.foundSuperCall) {
        reportIssue(((MethodTree) tree).simpleName(), "Use super.clone() to create and seed the cloned instance to be returned.");
      }
    }
  }

  private static boolean isCloneMethod(MethodTree methodTree) {
    return "clone".equals(methodTree.simpleName().name())
      && methodTree.parameters().isEmpty()
      && methodTree.block() != null;
  }

  private static class CloneSuperCallVisitor extends BaseTreeVisitor {

    private boolean foundSuperCall;

    @Override
    public void visitClass(ClassTree tree) {
      // Skip class definitions because super.clone() found in an inner-class is not the one we're looking for
    }

    @Override
    public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
      // Skip lambda expressions because super.clone() found in a lambda is not the one we're looking for
    }

    @Override
    public void visitNewClass(NewClassTree tree) {
      // Skip new class expressions because super.clone() found in an anonymous class is not the one we're looking for
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (isSuperCloneCall(tree)) {
        foundSuperCall = true;
      }
    }

    private static boolean isSuperCloneCall(MethodInvocationTree mit) {
      return mit.arguments().isEmpty()
        && mit.methodSelect().is(Kind.MEMBER_SELECT)
        && isSuperClone((MemberSelectExpressionTree) mit.methodSelect());
    }

    private static boolean isSuperClone(MemberSelectExpressionTree tree) {
      return "clone".equals(tree.identifier().name())
        && tree.expression().is(Kind.IDENTIFIER)
        && "super".equals(((IdentifierTree) tree.expression()).name());
    }
  }
}
