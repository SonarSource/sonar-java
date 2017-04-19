/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
import org.sonar.java.RspecKey;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.Collections;
import java.util.List;

@Rule(key = "UselessParenthesesCheck")
@RspecKey("S1110")
public class UselessParenthesesCheck extends IssuableSubscriptionVisitor {

  private static final Kind[] PARENT_EXPRESSION =  {
      Kind.ANNOTATION,
      Kind.LIST,
      Kind.ARRAY_ACCESS_EXPRESSION,
      Kind.ARRAY_DIMENSION,
      Kind.ASSERT_STATEMENT,
      Kind.ASSIGNMENT,
      Kind.CASE_LABEL,
      Kind.CONDITIONAL_EXPRESSION,
      Kind.DO_STATEMENT,
      Kind.EXPRESSION_STATEMENT,
      Kind.FOR_EACH_STATEMENT,
      Kind.FOR_STATEMENT,
      Kind.IF_STATEMENT,
      Kind.LAMBDA_EXPRESSION,
      Kind.ARGUMENTS,
      Kind.METHOD,
      Kind.NEW_ARRAY,
      Kind.NEW_CLASS,
      Kind.PARENTHESIZED_EXPRESSION,
      Kind.RETURN_STATEMENT,
      Kind.SWITCH_STATEMENT,
      Kind.SYNCHRONIZED_STATEMENT,
      Kind.THROW_STATEMENT,
      Kind.VARIABLE,
      Kind.WHILE_STATEMENT
  };


  @Override
  public void visitNode(Tree tree) {
    ParenthesizedTree parenthesizedTree = (ParenthesizedTree) tree;
    if (uselessParentheses(parenthesizedTree)) {
      reportIssue(parenthesizedTree.openParenToken(),
          "Remove these useless parentheses.",
          Collections.singletonList(new JavaFileScannerContext.Location("", parenthesizedTree.closeParenToken())), null);
    }
  }

  private static boolean uselessParentheses(ParenthesizedTree tree) {
    Tree parentTree = tree.parent();
    if (!parentTree.is(Kind.PARENTHESIZED_EXPRESSION)) {
      return false;
    }
    Tree grandParentTree = parentTree.parent();
    if (grandParentTree == null) {
      return false;
    }
    return parentTree.is(PARENT_EXPRESSION);
  }

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Kind.PARENTHESIZED_EXPRESSION);
  }
}
