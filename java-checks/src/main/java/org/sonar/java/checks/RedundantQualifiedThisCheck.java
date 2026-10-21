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

import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S9434")
public class RedundantQualifiedThisCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Remove this redundant qualifier.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.MEMBER_SELECT);
  }

  @Override
  public void visitNode(Tree tree) {
    MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) tree;
    if (!ExpressionUtils.isThis(memberSelect.identifier())) {
      return;
    }
    Symbol qualifier = memberSelect.expression().symbolType().symbol();
    if (!qualifier.isUnknown() && qualifier.equals(innermostEnclosingClass(memberSelect))) {
      QuickFixHelper.newIssue(context)
        .forRule(this)
        .onTree(memberSelect)
        .withMessage(MESSAGE)
        .withQuickFix(() -> removeQualifier(memberSelect))
        .report();
    }
  }

  private static JavaQuickFix removeQualifier(MemberSelectExpressionTree memberSelect) {
    return JavaQuickFix.newQuickFix("Remove the redundant qualifier")
      .addTextEdit(JavaTextEdit.removeBetweenTree(memberSelect.expression(), memberSelect.operatorToken()))
      .build();
  }

  /**
   * Returns the symbol of the class whose instance an unqualified {@code this} would denote at the given position.
   * Lambda bodies are transparent because a lambda does not introduce its own {@code this}, while anonymous and
   * local classes do and are therefore returned like any other class.
   */
  @CheckForNull
  private static Symbol innermostEnclosingClass(Tree tree) {
    Tree parent = tree.parent();
    while (parent != null) {
      if (parent instanceof ClassTree classTree) {
        return classTree.symbol();
      }
      parent = parent.parent();
    }
    return null;
  }
}
