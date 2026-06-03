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
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;

import java.util.List;

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
      new TryStatementsVisitor(UnitTestUtils.hasJUnitJupiterAnnotation(methodTree))
    );
  }

  private final class TryStatementsVisitor extends BaseTreeVisitor {
    private final boolean isJunit56;

    public TryStatementsVisitor(boolean isJunit56) {
      this.isJunit56 = isJunit56;
    }

    @Override
    public void visitTryStatement(TryStatementTree tree) {
      checkBlock(tree.block(), tree, true, "Use assertThrows() instead of try/catch and fail() in the try block.");
      tree.catches().forEach(c ->
        checkBlock(c.block(), tree, false, "Use assertDoesNotThrow() instead of try/catch and fail() in the catch block.")
      );
      super.visitTryStatement(tree);
    }

    private void checkBlock(
      BlockTree block,
      TryStatementTree tryStatement,
      boolean isTryBlock,
      String issueMessage
    ) {
      UnitTestUtils.findFail(block).ifPresent(failMethodInvocation -> {

          var context = AssertThrowsInsteadOfTryCatchFailCheck.this.context;
          List<String> failArguments = failMethodInvocation.arguments().stream()
            .map(argument -> QuickFixHelper.contentForTree(argument, context))
            .toList();

          InternalJavaIssueBuilder result = QuickFixHelper
            .newIssue(AssertThrowsInsteadOfTryCatchFailCheck.this.context)
            .forRule(AssertThrowsInsteadOfTryCatchFailCheck.this)
            .withMessage(issueMessage)
            .onTree(failMethodInvocation);

          if (isJunit56) {
            result = result.withQuickFix(() ->
              JavaQuickFix.newQuickFix(issueMessage).addTextEdit(
                JavaTextEdit.replaceTree(
                  tryStatement,
                  junitReplacement(failArguments, tryStatement, isTryBlock)
                )
              ).build()
            );
          } else if (failMethodInvocation.methodSymbol().signature().contains("org.assertj")) {
            result = result.withQuickFix(() ->
              JavaQuickFix.newQuickFix(issueMessage).addTextEdit(
                JavaTextEdit.replaceTree(
                  tryStatement,
                  assertJReplacement(failArguments, tryStatement, isTryBlock)
                )
              ).build()
            );
          }

          result.report();
        }
      );
    }

    private String junitReplacement(
      List<String> failArguments,
      TryStatementTree tryStatement,
      boolean isTryBlock
    ) {
      String tryBlockString = QuickFixHelper.contentForTree(tryStatement.block(), AssertThrowsInsteadOfTryCatchFailCheck.this.context);
      if (isTryBlock) {
        return "assertThrows(%s, () -> %s);".formatted(
          typeClass(firstCaughtTypeInTry(tryStatement)),
          tryBlockString
        );
      } else {
        return "assertDoesNotThrow(%s, %s);".formatted(
          tryBlockString
        );
      }
    }

    private String assertJReplacement(
      List<String> failArguments,
      TryStatementTree tryStatement,
      boolean isTryBlock
    ) {
      String tryBlockString = QuickFixHelper.contentForTree(tryStatement.block(), AssertThrowsInsteadOfTryCatchFailCheck.this.context);
      if (isTryBlock) {
        return "assertThatThrownBy(() -> %s).isInstanceOf(%s);".formatted(
          tryBlockString,
          typeClass(firstCaughtTypeInTry(tryStatement))
        );
      } else {
        return "assertThatThrownBy(() -> %s).doesNotThrowAnyException();".formatted(
          tryBlockString
        );
      }
    }

    private static String typeClass(Type caughtType) {
      return caughtType.name() + ".class";
    }

    private static Type firstCaughtTypeInTry(TryStatementTree tryStatement) {
      return getCaughtTypes(tryStatement.catches().getFirst()).getFirst();
    }
  }
}
