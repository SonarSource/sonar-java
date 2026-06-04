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
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.statement.BlockTreeImpl;
import org.sonar.java.reporting.InternalJavaIssueBuilder;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.*;

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

          // Compute try block without tne fail method invocation
          var filteredTryBlock = new BlockTreeImpl(
            (InternalSyntaxToken) tryStatement.block().openBraceToken(),
            tryStatement.block().body().stream()
              .filter(statement ->
                statement instanceof ExpressionStatementTree expression && expression.expression() != failMethodInvocation
              ).toList(),
            (InternalSyntaxToken) tryStatement.block().closeBraceToken());
          var filteredTryBlockString = contentFor(filteredTryBlock);

          if (isJunit56) {
            issueBuilder.withQuickFix(() ->
              /** Replace single text edit by 3 :
               * 1 :  Try token -> assertThrows(
               * 2 :  fail method invocation .-> ""
               * 3 : catch -> );
               */

              JavaQuickFix.newQuickFix(issueMessage).addTextEdit(
                JavaTextEdit.replaceTree(
                  tryStatement,
                  junitReplacement(failArguments, tryStatement, filteredTryBlockString, isTryBlock)
                )
              ).build()
            );
          } else if (failMethodInvocation.methodSymbol().signature().contains("org.assertj")) {
            issueBuilder.withQuickFix(() ->
              JavaQuickFix.newQuickFix(issueMessage).addTextEdit(
                JavaTextEdit.replaceTree(
                  tryStatement,
                  assertJReplacement(failArguments, tryStatement, filteredTryBlockString, isTryBlock)
                )
              ).build()
            );
          }

          issueBuilder.report();
        }
      );
    }

    private String junitReplacement(
      Arguments failArguments,
      TryStatementTree tryStatement,
      String filteredTryBlockString,
      boolean isTryBlock
    ) {
      String argumentsSuffix = failArguments.stream().findFirst().filter(argument ->
        argument.symbolType().is("java.lang.String") // || argument.symbolType().is("java.util.function.Supplier<java.lang.String>")
      ).map(argument ->
        ", %s".formatted(contentFor(argument))
      ).orElse("");

      if (isTryBlock) {
        return "assertThrows(%s, () -> %s%s);".formatted(
          typeClass(firstCaughtTypeInTry(tryStatement)),
          filteredTryBlockString,
          argumentsSuffix
        );
      } else {
        return "assertDoesNotThrow(() -> %s%s);".formatted(
          filteredTryBlockString,
          argumentsSuffix
        );
      }
    }

    private String assertJReplacement(
      Arguments failArguments,
      TryStatementTree tryStatement,
      String filteredTryBlockString,
      boolean isTryBlock
    ) {
      // in assertJ the failure message is mandatory
      var failureMessage = contentFor(failArguments.get(0));

      if (isTryBlock) {
        return "assertThatCode(() -> %s).withFailMessage(%s).isInstanceOf(%s);".formatted(
          filteredTryBlockString,
          failureMessage,
          typeClass(firstCaughtTypeInTry(tryStatement))
        );
      } else {
        return "assertThatCode(() -> %s).withFailMessage(%s).doesNotThrowAnyException();".formatted(
          filteredTryBlockString,
          failureMessage
        );
      }
    }

    private String contentFor(Tree tree) {
      return QuickFixHelper.contentForTree(tree, context);
    }

    private static String typeClass(Type caughtType) {
      return caughtType.name() + ".class";
    }

    private static Type firstCaughtTypeInTry(TryStatementTree tryStatement) {
      return getCaughtTypes(tryStatement.catches().get(0)).get(0);
    }

  }
}
