/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.checks;

import java.util.HashSet;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S1153")
public class ConcatenationWithStringValueOfCheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitBinaryExpression(BinaryExpressionTree tree) {
    if (!tree.is(Kind.PLUS)) {
      super.visitBinaryExpression(tree);
      return;
    }

    Set<MethodInvocationTree> valueOfTrees = new HashSet<>();
    boolean flagIssue = false;
    ExpressionTree current = tree;
    while (current.is(Kind.PLUS)) {
      BinaryExpressionTree binOp = (BinaryExpressionTree) current;
      scan(binOp.rightOperand());
      if (isStringValueOf(binOp.rightOperand())) {
        valueOfTrees.add((MethodInvocationTree) binOp.rightOperand());
      }
      flagIssue |= binOp.leftOperand().is(Kind.STRING_LITERAL);
      if (!valueOfTrees.isEmpty()) {
        flagIssue |= binOp.rightOperand().is(Kind.STRING_LITERAL);
      }
      current = ((BinaryExpressionTree) current).leftOperand();
    }

    if (flagIssue) {
      for (MethodInvocationTree valueOfTree : valueOfTrees) {
        QuickFixHelper.newIssue(context)
          .forRule(this)
          .onTree(valueOfTree)
          .withMessage("Directly append the argument of String.valueOf().")
          .withQuickFix(() -> createQuickFix(valueOfTree))
          .report();
      }
    }
    scan(current);
  }

  private JavaQuickFix createQuickFix(MethodInvocationTree invocationTree) {
    ExpressionTree argumentTree = invocationTree.arguments().get(0);
    String replacement = QuickFixHelper.contentForTree(argumentTree, context);

    if (argumentTree instanceof BinaryExpressionTree && !argumentTree.symbolType().is("java.lang.String")) {
      replacement = "(" + replacement + ")";
    }

    return JavaQuickFix.newQuickFix("Replace String.valueOf() with its argument")
      .addTextEdit(JavaTextEdit.replaceTree(invocationTree, replacement))
      .build();
  }

  private static boolean isStringValueOf(ExpressionTree tree) {
    return tree.is(Kind.METHOD_INVOCATION) && isStringValueOf((MethodInvocationTree) tree);
  }

  private static boolean isStringValueOf(MethodInvocationTree tree) {
    return tree.methodSelect().is(Kind.MEMBER_SELECT) && isStringValueOf((MemberSelectExpressionTree) tree.methodSelect()) && matchArgument(tree.arguments());
  }

  private static boolean matchArgument(Arguments args) {
    return args.size() == 1 && !args.get(0).symbolType().isUnknown() && !args.get(0).symbolType().is("char[]");
  }

  private static boolean isStringValueOf(MemberSelectExpressionTree tree) {
    return tree.expression().is(Kind.IDENTIFIER) && "valueOf".equals(tree.identifier().name()) && "String".equals(((IdentifierTree) tree.expression()).name());
  }

}
