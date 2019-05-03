/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Collections;
import java.util.List;

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
      SyntaxToken arrayDesignatorToken = arrayTypeTree.ellipsisToken();
      if (arrayDesignatorToken == null) {
        arrayDesignatorToken = arrayTypeTree.openBracketToken();
      }
      if (isInvalidPosition(arrayDesignatorToken, identifierToken)) {
        reportIssue(arrayDesignatorToken, "Move the array designator from the variable to the type.");
        break;
      }
      type = arrayTypeTree.type();
    }
  }

  private static boolean isInvalidPosition(SyntaxToken arrayDesignatorToken, SyntaxToken identifierToken) {
    return identifierToken.line() < arrayDesignatorToken.line()
      || (identifierToken.line() == arrayDesignatorToken.line() && identifierToken.column() < arrayDesignatorToken.column());
  }

}
