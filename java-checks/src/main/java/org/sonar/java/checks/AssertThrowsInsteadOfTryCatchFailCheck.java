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
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.Arguments;

import javax.annotation.Nullable;
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
    private final boolean hasJunitJupiterTestAnnotation;

    public TryStatementsVisitor(boolean hasJunitJupiterTestAnnotation) {
      this.hasJunitJupiterTestAnnotation = hasJunitJupiterTestAnnotation;
    }

    @Override
    public void visitTryStatement(TryStatementTree tree) {
      checkBlock(tree.block(), tree, true);
      tree.catches().forEach(c ->
        checkBlock(c.block(), tree, false)
      );
      super.visitTryStatement(tree);
    }

    private void checkBlock(
      BlockTree block,
      TryStatementTree tryStatement,
      boolean isTryBlock
    ) {
      UnitTestUtils.findFail(block).ifPresent(failMethodInvocation -> {

          var isAssertJ = failMethodInvocation.methodSymbol().signature().contains("org.assertj");
          if (hasJunitJupiterTestAnnotation || isAssertJ) {
            String issueMessage = isTryBlock ?
              "Use assertThrows() instead of try/catch and fail() in the try block." :
              "Use assertDoesNotThrow() instead of try/catch and fail() in the catch block.";
            InternalJavaIssueBuilder issueBuilder = QuickFixHelper
              .newIssue(context)
              .forRule(AssertThrowsInsteadOfTryCatchFailCheck.this)
              .onTree(failMethodInvocation)
              .withMessage(issueMessage)
              .withQuickFix(() ->
                provideQuickFix(
                  tryStatement,
                  failMethodInvocation,
                  issueMessage,
                  isAssertJ,
                  isTryBlock
                )
              );
            issueBuilder.report();
          }
        }
      );
    }

    private JavaQuickFix provideQuickFix(
      TryStatementTree tryStatement,
      MethodInvocationTree failMethodInvocation,
      String issueMessage,
      boolean isAssertJ,
      boolean isTryBlock
    ) {
      Arguments failArguments = failMethodInvocation.arguments();

      Replacements replacements = isAssertJ ?
        assertJReplacement(failArguments, tryStatement, isTryBlock) :
        junitReplacement(failArguments, tryStatement, isTryBlock);

      JavaTextEdit lastEdit;
      if (!tryStatement.catches().isEmpty()){
        var start = tryStatement.catches().get(0).firstToken();
        var end = tryStatement.finallyBlock() != null ?
          tryStatement.finallyBlock().lastToken() :
          tryStatement.catches().get(tryStatement.catches().size() - 1).block().lastToken();
        lastEdit = JavaTextEdit.replaceBetweenTree(start, end, replacements.replaceCatchesWith);
      } else if (tryStatement.finallyBlock() != null) {
        var start = tryStatement.finallyBlock().firstToken();
        var end = tryStatement.finallyBlock().lastToken();
        lastEdit = JavaTextEdit.replaceBetweenTree(start, end, replacements.replaceCatchesWith);
      } else {
        lastEdit = JavaTextEdit.insertAfterTree(tryStatement.block(), replacements.replaceCatchesWith);
      }

      return JavaQuickFix.newQuickFix(issueMessage).addTextEdit(
        JavaTextEdit.replaceTree(tryStatement.tryKeyword(), replacements.replaceTryWith),
        JavaTextEdit.replaceTree(failMethodInvocation.parent(), ""),
        lastEdit
      ).build();
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
      var failureMessagePart = failArguments.stream().findFirst().map(this::contentFor).map(".withFailMessage(%s)"::formatted).orElse("");
      return isTryBlock ?
        new Replacements(
          "assertThatCode(() -> ",
          ")%s.isInstanceOf(%s);".formatted(failureMessagePart, typeClass(firstCaughtTypeInTry(tryStatement)))
        ) :
        new Replacements(
          "assertThatCode(() -> ",
          ")%s.doesNotThrowAnyException();".formatted(failureMessagePart)
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
    if (!tryStatement.catches().isEmpty()) {
      return getCaughtTypes(tryStatement.catches().get(0)).get(0);
    }
    return null;
  }

}
