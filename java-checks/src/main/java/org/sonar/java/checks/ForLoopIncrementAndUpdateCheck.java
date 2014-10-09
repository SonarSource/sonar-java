/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.resolve.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

import java.util.Collection;
import java.util.List;

@Rule(
    key = "S1994",
    priority = Priority.CRITICAL,
    tags = {"bug"})
@BelongsToProfile(title = "Sonar way", priority = Priority.CRITICAL)
public class ForLoopIncrementAndUpdateCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.FOR_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      ForStatementTree forStatementTree = (ForStatementTree) tree;
      if (!forStatementTree.update().isEmpty()) {
        Collection<Symbol> updateSymbols = getUpdatedSymbols(forStatementTree);
        ConditionVisitor conditionVisitor = new ConditionVisitor(updateSymbols);
        forStatementTree.accept(conditionVisitor);
        if (conditionVisitor.shouldRaiseIssue) {
          addIssue(tree, "This loop's stop condition tests \"" + Joiner.on(", ").join(conditionVisitor.conditionNames)
              + "\" but the incrementer updates \"" + getSymbols(updateSymbols) + "\".");
        }
      }
    }
  }

  private Collection<Symbol> getUpdatedSymbols(ForStatementTree forStatementTree) {
    UpdateVisitor updateVisitor = new UpdateVisitor();
    forStatementTree.accept(updateVisitor);
    return updateVisitor.symbols;
  }

  private String getSymbols(Collection<Symbol> updateSymbols) {
    List<String> names = Lists.newArrayList();
    for (Symbol updateSymbol : updateSymbols) {
      names.add(updateSymbol.getName());
    }
    return Joiner.on(", ").join(names);
  }

  private class UpdateVisitor extends BaseTreeVisitor {
    Collection<Symbol> symbols = Lists.newArrayList();

    @Override
    public void visitForStatement(ForStatementTree tree) {
      scan(tree.update());
    }

    @Override
    public void visitUnaryExpression(UnaryExpressionTree tree) {
      if (tree.expression().is(Tree.Kind.IDENTIFIER)) {
        addSymbol((IdentifierTree) tree.expression());
      }
      super.visitUnaryExpression(tree);
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      if (tree.variable().is(Tree.Kind.IDENTIFIER)) {
        addSymbol((IdentifierTree) tree.variable());
      }
      super.visitAssignmentExpression(tree);
    }

    private void addSymbol(IdentifierTree identifierTree) {
      Symbol symbol = getSemanticModel().getReference(identifierTree);
      if (symbol != null) {
        symbols.add(symbol);
      }
    }
  }

  private class ConditionVisitor extends BaseTreeVisitor {
    private final Collection<Symbol> updateSymbols;
    private final Collection<String> conditionNames;
    private boolean shouldRaiseIssue;


    ConditionVisitor(Collection<Symbol> updateSymbols) {
      this.updateSymbols = updateSymbols;
      conditionNames = Lists.newArrayList();
      shouldRaiseIssue = !updateSymbols.isEmpty();
    }

    @Override
    public void visitForStatement(ForStatementTree tree) {
      scan(tree.condition());
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      conditionNames.add(tree.name());
      if (updateSymbols.contains(getSemanticModel().getReference(tree))) {
        shouldRaiseIssue = false;
      }
    }
  }

}
