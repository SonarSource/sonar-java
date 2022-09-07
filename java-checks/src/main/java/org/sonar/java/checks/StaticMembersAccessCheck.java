/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.ast.visitors.ExtendedIssueBuilderSubscriptionVisitor;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2209")
public class StaticMembersAccessCheck extends ExtendedIssueBuilderSubscriptionVisitor {

  private QuickFixHelper.ImportSupplier importSupplier;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.MEMBER_SELECT);
  }

  @Override
  public void setContext(JavaFileScannerContext context) {
    super.setContext(context);
    importSupplier = null;
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    importSupplier = null;
  }

  @Override
  public void visitNode(Tree tree) {
    MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) tree;
    IdentifierTree memberSelectIdentifier = memberSelect.identifier();
    Symbol memberSelectSymbol = memberSelectIdentifier.symbol();
    if (memberSelectSymbol.isStatic()) {
      ExpressionTree leftOperand = memberSelect.expression();
      ExpressionTree selectExpression = leftOperand.is(Tree.Kind.MEMBER_SELECT)
        ? ((MemberSelectExpressionTree) leftOperand).identifier()
        : leftOperand;
      if (!selectExpression.is(Tree.Kind.IDENTIFIER) || ((IdentifierTree) selectExpression).symbol().isVariableSymbol()) {
        newIssue()
          .onTree(leftOperand)
          .withMessage("Change this instance-reference to a static reference.")
          .withQuickFix(() -> createQuickFixes(leftOperand, memberSelectSymbol.owner().type()))
          .report();
      }
    }
  }

  private JavaQuickFix createQuickFixes(ExpressionTree leftOperand, Type type) {
    String leftOperandAsText = leftOperand.is(Tree.Kind.IDENTIFIER)
      ? ("\"" + ((IdentifierTree) leftOperand).name() + "\"")
      : "the expression";
    JavaQuickFix.Builder builder = JavaQuickFix.newQuickFix(String.format("Replace %s by \"%s\"", leftOperandAsText, type.name()))
      .addTextEdit(JavaTextEdit.replaceTree(leftOperand, type.name()));

    if (importSupplier == null) {
      importSupplier = QuickFixHelper.newImportSupplier(context);
    }
    importSupplier.newImportEdit(type.fullyQualifiedName())
      .ifPresent(builder::addTextEdit);

    return builder.build();
  }

}
