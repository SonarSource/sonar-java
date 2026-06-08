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
import org.sonar.java.checks.helpers.TryCatchUtils;
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
import org.sonar.plugins.java.api.tree.Arguments;

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
      new TryStatementsVisitor(UnitTestUtils.hasJUnitJupiterAnnotation(methodTree))
    );
  }

  private final class TryStatementsVisitor extends BaseTreeVisitor {
    private final boolean hasJunitJupiterTestAnnotation;

    public TryStatementsVisitor(boolean hasJunitJupiterTestAnnotation) {
      this.hasJunitJupiterTestAnnotation = hasJunitJupiterTestAnnotation;
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

          Arguments failArguments = failMethodInvocation.arguments();

          InternalJavaIssueBuilder issueBuilder = QuickFixHelper
            .newIssue(context)
            .forRule(AssertThrowsInsteadOfTryCatchFailCheck.this)
            .onTree(failMethodInvocation)
            .withMessage(issueMessage);

          var isAssertJ = failMethodInvocation.methodSymbol().signature().contains("org.assertj");
          if (hasJunitJupiterTestAnnotation || isAssertJ) {
            Replacements replacements = isAssertJ ?
              assertJReplacement(failArguments, tryStatement, isTryBlock) :
              junitReplacement(failArguments, tryStatement, isTryBlock);
            var firstCatchFinallyToken = tryStatement.catches().isEmpty() ?
              tryStatement.finallyKeyword().firstToken() :
              tryStatement.catches().get(0).firstToken();
            var lastCatchFinallyToken = tryStatement.finallyBlock() != null ?
              tryStatement.finallyBlock().lastToken() :
              tryStatement.catches().get(tryStatement.catches().size() - 1).block().lastToken();
            issueBuilder.withQuickFix(() ->
              JavaQuickFix.newQuickFix(issueMessage).addTextEdit(
                JavaTextEdit.replaceTree(tryStatement.tryKeyword(), replacements.replaceTryWith),
                JavaTextEdit.replaceTree(failMethodInvocation.parent(), ""),
                JavaTextEdit.replaceBetweenTree(
                  firstCatchFinallyToken,
                  lastCatchFinallyToken,
                  replacements.replaceCatchesWith
                )
              ).build()
            );
            issueBuilder.report();
          }
        }
      );
    }

    private Replacements junitReplacement(
      Arguments failArguments,
      TryStatementTree tryStatement,
      boolean isTryBlock
    ) {

      String argumentsSuffix = failArguments.stream().findFirst().filter(argument ->
        {
          Type symbolType = argument.symbolType();
          return !symbolType.isUnknown() && (symbolType.is("java.lang.String") || symbolType.isSubtypeOf("java.util.function.Supplier<java.lang.String>"));
        }
      ).map(argument ->
        ", %s".formatted(contentFor(argument))
      ).orElse("");

      return isTryBlock ?
        new Replacements(
          "assertThrows(%s, () -> ".formatted(typeClass(firstCaughtTypeInTry(tryStatement))),
          "%s);".formatted(argumentsSuffix)
        ) :
        new Replacements(
          "assertDoesNotThrow(() -> ",
          "%s);".formatted(argumentsSuffix)
        );
    }

    private Replacements assertJReplacement(
      Arguments failArguments,
      TryStatementTree tryStatement,
      boolean isTryBlock
    ) {
      // in assertJ the failure message is mandatory
      var failureMessage = contentFor(failArguments.get(0));
      return isTryBlock ?
        new Replacements(
          "assertThatCode(() -> ",
          ").withFailMessage(%s).isInstanceOf(%s);".formatted(failureMessage, typeClass(firstCaughtTypeInTry(tryStatement)))
        ) :
        new Replacements(
          "assertThatCode(() -> ",
          ").withFailMessage(%s).doesNotThrowAnyException();".formatted(failureMessage)
        );
    }

    private String contentFor(Tree tree) {
      return QuickFixHelper.contentForTree(tree, context);
    }

    private record Replacements(String replaceTryWith, String replaceCatchesWith) {
    }
  }


  private static String typeClass(@Nullable Type caughtType) {
    if (caughtType == null) return "Throwable";
    return caughtType.name() + ".class";
  }

  @Nullable
  private static Type firstCaughtTypeInTry(TryStatementTree tryStatement) {
    if (tryStatement.catches().size() > 0) {
      return getCaughtTypes(tryStatement.catches().get(0)).get(0);
    }
    return null;
  }

}
