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
import org.sonar.java.model.JavaTree;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
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
    TypeTree type = variableTree.type();
    SyntaxToken identifierToken = variableTree.simpleName().identifierToken();
    while (type.is(Tree.Kind.ARRAY_TYPE)) {
      ArrayTypeTree arrayTypeTree = (ArrayTypeTree) type;
      SyntaxToken openBracketToken = arrayTypeTree.openBracketToken();
      if (openBracketToken != null && isInvalidPosition(openBracketToken, identifierToken)) {
        QuickFixHelper.newIssue(context)
          .forRule(this)
          .onTree(openBracketToken)
          .withMessage("Move the array designator from the variable to the type.")
          .withQuickFixes(() -> createQuickFixes(variableTree, arrayTypeTree))
          .report();
        break;
      }
      type = arrayTypeTree.type();
    }
  }

  private static List<JavaQuickFix> createQuickFixes(VariableTree variableTree, ArrayTypeTree arrayTypeTree) {
    if (isDeclarationTypeUsedBySeveralVariable(variableTree)) {
      return Collections.emptyList();
    }
    return Collections.singletonList(
      JavaQuickFix.newQuickFix("Move [] to the variable type")
        .addTextEdit(JavaTextEdit.replaceBetweenTree(
          arrayTypeTree.openBracketToken(),
          arrayTypeTree.closeBracketToken(),
          ""))
        .addTextEdit(JavaTextEdit.insertAfterTree(
          arrayTypeTree.type(),
          "[]"))
        .build());
  }

  private static boolean isDeclarationTypeUsedBySeveralVariable(VariableTree current) {
    Tree parent = current.parent();
    List<? extends Tree> children;
    if (parent instanceof ClassTree) {
      children = ((ClassTree) parent).members();
    } else if (parent.is(Tree.Kind.METHOD)) {
      children = ((MethodTree) parent).parameters();
    } else {
      children = ((JavaTree) parent).getChildren();
    }
    int index = children.indexOf(current);
    return ((index - 1 >= 0 && isVariableDeclarationOfTheSameType(current, children.get(index - 1))) ||
      (index + 1 < children.size() && isVariableDeclarationOfTheSameType(current, children.get(index + 1))));
  }

  private static boolean isVariableDeclarationOfTheSameType(VariableTree variable, Tree otherTree) {
    return otherTree.is(Tree.Kind.VARIABLE) && variable.firstToken().equals(otherTree.firstToken());
  }

  private static boolean isInvalidPosition(SyntaxToken arrayDesignatorToken, SyntaxToken identifierToken) {
    return identifierToken.line() < arrayDesignatorToken.line()
      || (identifierToken.line() == arrayDesignatorToken.line() && identifierToken.column() < arrayDesignatorToken.column());
  }

}
