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
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.SyntacticEquivalence;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Rule(key = "S3047")
public class LoopsOnSameSetCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    // raise issues only when they appear in the same block
    return Collections.singletonList(Tree.Kind.BLOCK);
  }

  @Override
  public void visitNode(Tree tree) {
    Map<Symbol, Integer> forEachSymbols = new HashMap<>();
    Tree previousForeachIterable = null;
    for (Tree item : ((BlockTree) tree).body()) {
      if (item.is(Tree.Kind.FOR_EACH_STATEMENT)) {
        ForEachStatement forEachStatement = (ForEachStatement) item;
        checkForEach(forEachSymbols, previousForeachIterable, forEachStatement);
        previousForeachIterable = forEachStatement.expression();
      } else {
        previousForeachIterable = null;
        item.accept(new InvalidatorVisitor(forEachSymbols));
      }
    }
  }

  private void checkForEach(Map<Symbol, Integer> forEachSymbols, @Nullable Tree previousIterable, ForEachStatement item) {
    ExpressionTree expressionTree = ExpressionUtils.skipParentheses(item.expression());
    if (expressionTree.is(Tree.Kind.IDENTIFIER)) {
      checkForEachIdentifier(forEachSymbols, (IdentifierTree) expressionTree);
    } else if (previousIterable != null) {
      checkForEachExpression(previousIterable, expressionTree);
    }
  }

  private void checkForEachExpression(Tree forEachIterable, ExpressionTree expressionTree) {
    if (SyntacticEquivalence.areEquivalent(expressionTree, forEachIterable)) {
      addIssue(expressionTree, forEachIterable.firstToken().line());
    }
  }

  private void checkForEachIdentifier(Map<Symbol, Integer> forEachSymbols, IdentifierTree node) {
    Symbol symbol = node.symbol();
    if (symbol.owner().isMethodSymbol()) {
      if (forEachSymbols.containsKey(symbol)) {
        addIssue(node, forEachSymbols.get(symbol));
      } else {
        forEachSymbols.put(symbol, ((JavaTree) node).getLine());
      }
    }
  }

  private void addIssue(Tree tree, int line) {
    reportIssue(tree, "Combine this loop with the one that starts on line " + line + ".");
  }

  private static class InvalidatorVisitor extends BaseTreeVisitor {
    private final Map<Symbol, Integer> forEachSymbols;

    InvalidatorVisitor(Map<Symbol, Integer> forEachSymbols) {
      this.forEachSymbols = forEachSymbols;
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      Symbol symbol = tree.symbol();
      if (forEachSymbols.containsKey(symbol)) {
        forEachSymbols.remove(symbol);
      }
      super.visitIdentifier(tree);
    }
  }
}
