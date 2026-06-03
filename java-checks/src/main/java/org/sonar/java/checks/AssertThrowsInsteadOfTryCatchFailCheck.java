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

import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.checks.helpers.UnitTestUtils;
import org.sonar.java.reporting.InternalJavaIssueBuilder;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

import static org.sonar.java.checks.helpers.TryCatchUtils.getCaughtTypes;

@Rule(key = "S8714")
public class AssertThrowsInsteadOfTryCatchFailCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    tree.accept(
      new TryStatementsVisitor(UnitTestUtils.hasJUnit56TestAnnotation(methodTree))
    );
  }

  private final class TryStatementsVisitor extends BaseTreeVisitor {
    private final boolean isJunit56;

    public TryStatementsVisitor(boolean isJunit56) {
      this.isJunit56 = isJunit56;
    }

    @Override
    public void visitTryStatement(TryStatementTree tree) {
      checkBlock(tree.block(), tree, List.of(), "Use assertThrows() instead of try/catch and fail() in the try block.");
      tree.catches().forEach(c ->
        checkBlock(c.block(), tree, getCaughtTypes(c), "Use assertDoesNotThrow() instead of try/catch and fail() in the catch block.")
      );
      super.visitTryStatement(tree);
    }

    private void checkBlock(BlockTree block, TryStatementTree tryStatement, List<Type> caughtTypes, String message) {
      UnitTestUtils.findFail(block).ifPresent(failMethodInvocation -> {

          var context = AssertThrowsInsteadOfTryCatchFailCheck.this.context;
          List<String> arguments = failMethodInvocation.arguments().stream()
            .map(argument -> QuickFixHelper.contentForTree(argument, context))
            .toList();

          InternalJavaIssueBuilder result = QuickFixHelper
            .newIssue(AssertThrowsInsteadOfTryCatchFailCheck.this.context)
            .forRule(AssertThrowsInsteadOfTryCatchFailCheck.this)
            .withMessage(message)
            .onTree(failMethodInvocation);

          if (isJunit56) {
            result = result.withQuickFix(() ->
              JavaQuickFix.newQuickFix(message).addTextEdit(JavaTextEdit.replaceTree(tryStatement, junitReplacement(arguments, caughtTypes, message))).build()
            );
          } else if (failMethodInvocation.methodSymbol().signature().contains("org.assertj")) {
            result = result.withQuickFix(() ->
              JavaQuickFix.newQuickFix(message).addTextEdit(JavaTextEdit.replaceTree(tryStatement, assertJReplacement(arguments, caughtTypes, message))).build()
            );
          }

          result.report();
        }
      );
    }

    private static String junitReplacement(List<String> arguments, List<Type> caughtTypes, String message) {
      boolean isTryBlock = caughtTypes.isEmpty();
      if (isTryBlock) {

      } else {

      }
      return "";
    }

    private static String assertJReplacement(List<String> arguments, List<Type> caughtTypes, String message) {
      boolean isTryBlock = caughtTypes.isEmpty();
      if (isTryBlock) {

      } else {

      }
      return "";
    }
  }
}
