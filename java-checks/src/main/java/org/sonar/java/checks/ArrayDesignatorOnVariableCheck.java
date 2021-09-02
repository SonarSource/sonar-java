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
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S1197")
public class ArrayDesignatorOnVariableCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.VARIABLE);
  }

  @Override
  public void visitNode(Tree tree) {
    VariableTree variableTree = (VariableTree) tree;
    MisplacedArray.find(variableTree.type(), variableTree.simpleName().identifierToken())
      .ifPresent(misplaced -> QuickFixHelper.newIssue(context)
        .forRule(this)
        .onRange(misplaced.firstArray.openBracketToken(), misplaced.lastArray.closeBracketToken())
        .withMessage("Move the array designators " + misplaced.replacement + " to the type.")
        .withQuickFixes(() -> isDeclarationTypeUsedBySeveralVariable(variableTree)
          ? Collections.emptyList()
          : Collections.singletonList(createQuickFix(misplaced, "variable type")))
        .report());
  }

  static JavaQuickFix createQuickFix(MisplacedArray misplaced, String type) {
    return JavaQuickFix.newQuickFix("Move " + misplaced.replacement + " to the " + type)
      .addTextEdit(JavaTextEdit.removeBetweenTree(
        misplaced.firstArray.openBracketToken(),
        misplaced.lastArray.closeBracketToken()))
      .addTextEdit(JavaTextEdit.insertAfterTree(
        misplaced.firstArray.type(),
        misplaced.replacement))
      .build();
  }

  private static boolean isDeclarationTypeUsedBySeveralVariable(VariableTree current) {
    return QuickFixHelper.previousVariable(current).isPresent() || QuickFixHelper.nextVariable(current).isPresent();
  }

  static class MisplacedArray {
    ArrayTypeTree firstArray;
    ArrayTypeTree lastArray;
    String replacement;

    private MisplacedArray(ArrayTypeTree lastArrayType, SyntaxToken identifierToken) {
      firstArray = lastArrayType;
      lastArray = lastArrayType;
      StringBuilder replacementBuilder = new StringBuilder("[]");
      while (firstArray.type().is(Tree.Kind.ARRAY_TYPE)) {
        ArrayTypeTree previous = (ArrayTypeTree) firstArray.type();
        if (!isInvalidPosition(previous, identifierToken)) {
          break;
        }
        replacementBuilder.append("[]");
        firstArray = previous;
      }
      replacement = replacementBuilder.toString();
    }

    static Optional<MisplacedArray> find(@Nullable TypeTree type, SyntaxToken identifierToken) {
      return Optional.ofNullable(type)
        .filter(t -> t.is(Tree.Kind.ARRAY_TYPE))
        .map(ArrayTypeTree.class::cast)
        .filter(arrayType -> isInvalidPosition(arrayType, identifierToken))
        .map(arrayType -> new MisplacedArray(arrayType, identifierToken));
    }

    private static boolean isInvalidPosition(ArrayTypeTree arrayTypeTree, SyntaxToken identifierToken) {
      SyntaxToken openBracketToken = arrayTypeTree.openBracketToken();
      return openBracketToken != null && identifierToken.range().start().isBefore(openBracketToken.range().start());
    }

  }

}
