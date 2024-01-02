/*
 * Copyright (C) 2012-2024 SonarSource SA - mailto:info AT sonarsource DOT com
 * This code is released under [MIT No Attribution](https://opensource.org/licenses/MIT-0) license.
 */
package org.sonar.samples.java.checks;

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "NoIfStatementInTests")
/**
 * To use subscription visitor, just extend the IssuableSubscriptionVisitor.
 */
public class NoIfStatementInTestsRule extends IssuableSubscriptionVisitor {

  private final BaseTreeVisitor ifStatementVisitor = new IfStatementVisitor();

  /**
   * Unit tests are special methods, so we are just going to visit all of them.
   */
  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree method = (MethodTree) tree;
    if (!isJunit4TestMethod(method)) {
      // any other method which is not a test
      return;
    }
    BlockTree block = method.block();
    if (block == null) {
      // an abstract test method maybe?
      return;
    }
    block.accept(ifStatementVisitor);
  }

  /**
   * Checks that a give method is annotated with JUnit4 'org.junit.Test' annotation
   */
  private static boolean isJunit4TestMethod(MethodTree method) {
    return method.symbol()
      .metadata()
      .isAnnotatedWith("org.junit.Test");
  }

  private class IfStatementVisitor extends BaseTreeVisitor {

    @Override
    public void visitIfStatement(IfStatementTree tree) {
      // report an issue on any "if" it finds
      reportIssue(tree.ifKeyword(), "Remove this 'if' statement from this test.");
      super.visitIfStatement(tree);
    }

    @Override
    public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
      // skip lambdas
    }

    @Override
    public void visitClass(ClassTree tree) {
      // skip inner or anonymous classes
    }
  }

}
