/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

@Rule(key = "S1195")
public class ArrayDesignatorAfterTypeCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    TypeTree returnType = methodTree.returnType();
    SyntaxToken identifierToken = methodTree.simpleName().identifierToken();
    while (returnType.is(Tree.Kind.ARRAY_TYPE)) {
      ArrayTypeTree arrayTypeTree = (ArrayTypeTree) returnType;
      SyntaxToken openBracketToken = arrayTypeTree.openBracketToken();
      if (isInvalidPosition(openBracketToken, identifierToken)) {
        QuickFixHelper.newIssue(context)
          .forRule(this)
          .onTree(openBracketToken)
          .withMessage("Move the array designators \"[]\" to the end of the return type.")
          .withQuickFix(() -> createQuickFixes(arrayTypeTree))
          .report();
        break;
      }
      returnType = arrayTypeTree.type();
    }
  }

  private static JavaQuickFix createQuickFixes(ArrayTypeTree type) {
    return JavaQuickFix.newQuickFix("Move [] to the return type")
      .addTextEdit(JavaTextEdit.removeBetweenTree(type.openBracketToken(), type.closeBracketToken()))
      .addTextEdit(JavaTextEdit.insertAfterTree(type.type(), "[]"))
      .build();
  }

  private static boolean isInvalidPosition(SyntaxToken openBracketToken, SyntaxToken identifierToken) {
    return identifierToken.range().start().isBefore(openBracketToken.range().start());
  }

}
