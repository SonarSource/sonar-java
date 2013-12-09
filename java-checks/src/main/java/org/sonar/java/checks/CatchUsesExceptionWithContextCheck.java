/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import org.apache.commons.lang.ArrayUtils;
import org.sonar.api.rule.RuleKey;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;
import java.util.Stack;

@Rule(
  key = CatchUsesExceptionWithContextCheck.RULE_KEY,
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class CatchUsesExceptionWithContextCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String RULE_KEY = "S1166";
  private final RuleKey ruleKey = RuleKey.of(CheckList.REPOSITORY_KEY, RULE_KEY);
  private static final String[] EXCLUDED_EXCEPTION_TYPE = {
    "NumberFormatException",
    "InterruptedExcetpion",
    "ParseException",
    "MalformedURLException"};

  private Stack<CatchState> catchStack = new Stack<CatchState>();
  private boolean inThrow;
  private JavaFileScannerContext context;

  private static class CatchState {
    public String caughtVariable;
    public boolean isExceptionCorrectlyUsed;

    public CatchState(String caughtVariable) {
      this.caughtVariable = caughtVariable;
    }

  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    this.catchStack.clear();
    this.inThrow = false;
    scan(context.getTree());
  }

  @Override
  public void visitCatch(CatchTree tree) {
    if (tree.is(Tree.Kind.CATCH)) {

      if (isExcludedExceptionType(tree)) {
        super.visitCatch(tree);
      } else {
        catchStack.push(new CatchState(tree.parameter().simpleName()));
        super.visitCatch(tree);

        if (!catchStack.pop().isExceptionCorrectlyUsed) {
          context.addIssue(tree, ruleKey, "Either log or rethrow this exception along with some contextual information.");
        }
      }
    }
  }

  private boolean isExcludedExceptionType(CatchTree tree) {
    Tree exceptionType = tree.parameter().type();
    return exceptionType.is(Tree.Kind.IDENTIFIER)
      && ArrayUtils.contains(EXCLUDED_EXCEPTION_TYPE, ((IdentifierTree) exceptionType).name());
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    if (!catchStack.empty() && tree.is(Tree.Kind.METHOD_INVOCATION) && (inThrow || tree.arguments().size() > 1)) {
      checkArguments(tree.arguments());
    }
    super.visitMethodInvocation(tree);
  }


  @Override
  public void visitNewClass(NewClassTree tree) {
    if (!catchStack.empty() && tree.is(Tree.Kind.NEW_CLASS) && inThrow) {
      checkArguments(tree.arguments());
    }
    super.visitNewClass(tree);
  }

  private void checkArguments(List<ExpressionTree> arguments) {
    for (ExpressionTree expression : arguments) {
      if (expression.is(Tree.Kind.IDENTIFIER) && ((IdentifierTree) expression).name().equals(catchStack.peek().caughtVariable)) {
        catchStack.peek().isExceptionCorrectlyUsed = true;
      }
    }
  }

  @Override
  public void visitThrowStatement(ThrowStatementTree tree) {
    if (!catchStack.empty() && tree.is(Tree.Kind.THROW_STATEMENT)) {
      ExpressionTree expr = tree.expression();

      if (expr.is(Tree.Kind.IDENTIFIER) && ((IdentifierTree) expr).name().equals(catchStack.peek().caughtVariable)) {
        catchStack.peek().isExceptionCorrectlyUsed = true;
      }

      inThrow = true;
      super.visitThrowStatement(tree);
      inThrow = false;

    }
  }


}
