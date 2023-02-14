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

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

@Rule(key = "S1695")
public class NPEThrowCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.THROW_STATEMENT, Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.THROW_STATEMENT)) {
      ExpressionTree expressionTree = ((ThrowStatementTree) tree).expression();
      raiseIssueOnNpe(expressionTree, expressionTree.symbolType());
    } else {
      for (TypeTree throwClause : ((MethodTree) tree).throwsClauses()) {
        raiseIssueOnNpe(throwClause, throwClause.symbolType());
      }
    }
  }

  private void raiseIssueOnNpe(Tree tree, Type type) {
    if (type.is("java.lang.NullPointerException")) {
      reportIssue(treeAtFault(tree), "Throw some other exception here, such as \"IllegalArgumentException\".");
    }
  }

  private static Tree treeAtFault(Tree tree) {
    return tree.is(Tree.Kind.NEW_CLASS) ? ((NewClassTree) tree).identifier() : tree;
  }

}
