/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.SyntacticEquivalence;
import org.sonar.java.syntaxtoken.FirstSyntaxTokenFinder;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Rule(
  key = "S3047",
  name = "Multiple loops over the same set should be combined",
  tags = {"performance"},
  priority = Priority.MAJOR)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.CPU_EFFICIENCY)
@SqaleConstantRemediation("20min")
public class LoopsOnSameSetCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    // raise issues only when they appear in the same block
    return ImmutableList.of(Tree.Kind.BLOCK);
  }

  @Override
  public void visitNode(Tree tree) {
    Map<Symbol, Integer> forEachSymbols = new HashMap<>();
    Set<Tree> forEachIterables = new HashSet<>();
    for (Tree item : ((BlockTree) tree).body()) {
      if (item.is(Tree.Kind.FOR_EACH_STATEMENT)) {
        checkForEach(forEachSymbols, forEachIterables, (ForEachStatement) item);
      } else {
        item.accept(new InvalidatorVisitor(forEachSymbols));
      }
    }
  }

  private void checkForEach(Map<Symbol, Integer> forEachSymbols, Set<Tree> forEachIterables, ForEachStatement item) {
    ExpressionTree expressionTree = ExpressionsHelper.skipParentheses(item.expression());
    if (expressionTree.is(Tree.Kind.IDENTIFIER)) {
      checkForEachIdentifier(forEachSymbols, (IdentifierTree) expressionTree);
    } else {
      checkForEachExpression(forEachIterables, expressionTree);
    }
  }

  private void checkForEachExpression(Set<Tree> forEachIterables, ExpressionTree expressionTree) {
    for (Tree subTree : forEachIterables) {
      if (SyntacticEquivalence.areEquivalent(expressionTree, subTree)) {
        addIssue(expressionTree, FirstSyntaxTokenFinder.firstSyntaxToken(subTree).line());
        return;
      }
    }
    forEachIterables.add(expressionTree);
  }

  private void checkForEachIdentifier(Map<Symbol, Integer> forEachSymbols, IdentifierTree node) {
    Symbol symbol = node.symbol();
    if (forEachSymbols.containsKey(symbol)) {
      addIssue(node, forEachSymbols.get(symbol));
    } else {
      forEachSymbols.put(symbol, ((JavaTree) node).getLine());
    }
  }

  private void addIssue(Tree tree, int line) {
    addIssue(tree, "Combine this loop with the one that starts on line " + line + ".");
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
