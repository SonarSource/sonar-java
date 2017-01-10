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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

import java.util.Collection;
import java.util.List;

@Rule(key = "S1994")
public class ForLoopIncrementAndUpdateCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.FOR_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      ForStatementTree forStatementTree = (ForStatementTree) tree;
      if (!forStatementTree.update().isEmpty() && forStatementTree.condition() != null) {
        Collection<Symbol> updateSymbols = getUpdatedSymbols(forStatementTree);
        ConditionVisitor conditionVisitor = new ConditionVisitor(updateSymbols);
        forStatementTree.accept(conditionVisitor);
        if (conditionVisitor.shouldRaiseIssue) {
          reportIssue(forStatementTree.forKeyword(), "This loop's stop condition tests \"" + Joiner.on(", ").join(conditionVisitor.conditionNames)
            + "\" but the incrementer updates \"" + getSymbols(updateSymbols) + "\".");
        }
      }
    }
  }

  private static Collection<Symbol> getUpdatedSymbols(ForStatementTree forStatementTree) {
    UpdateVisitor updateVisitor = new UpdateVisitor();
    forStatementTree.accept(updateVisitor);
    return updateVisitor.symbols;
  }

  private static String getSymbols(Collection<Symbol> updateSymbols) {
    List<String> names = Lists.newArrayList();
    for (Symbol updateSymbol : updateSymbols) {
      names.add(updateSymbol.name());
    }
    return Joiner.on(", ").join(names);
  }

  private static class UpdateVisitor extends BaseTreeVisitor {
    Collection<Symbol> symbols = Lists.newArrayList();

    @Override
    public void visitForStatement(ForStatementTree tree) {
      scan(tree.update());
    }

    @Override
    public void visitUnaryExpression(UnaryExpressionTree tree) {
      checkIdentifier(tree.expression());
      super.visitUnaryExpression(tree);
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      checkIdentifier(tree.variable());
      super.visitAssignmentExpression(tree);
    }

    private void checkIdentifier(ExpressionTree expression) {
      if (expression.is(Tree.Kind.IDENTIFIER)) {
        addSymbol((IdentifierTree) expression);
      } else if (expression.is(Tree.Kind.MEMBER_SELECT)) {
        addSymbol(((MemberSelectExpressionTree) expression).identifier());
      }
    }

    private void addSymbol(IdentifierTree identifierTree) {
      Symbol symbol = identifierTree.symbol();
      if (!symbol.isUnknown()) {
        symbols.add(symbol);
      }
    }
  }

  private static class ConditionVisitor extends BaseTreeVisitor {
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
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (tree.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree mset = (MemberSelectExpressionTree) tree.methodSelect();
        ExpressionTree expression = mset.expression();
        if (expression.is(Tree.Kind.IDENTIFIER)) {
          checkIdentifier((IdentifierTree) expression);
        } else {
          checkIdentifier(mset.identifier());
        }
      } else {
        scan(tree.methodSelect());
      }
      scan(tree.typeArguments());
      scan(tree.arguments());
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      checkIdentifier(tree);
    }

    private void checkIdentifier(IdentifierTree tree) {
      Symbol reference = tree.symbol();
      String name = tree.name();
      if (reference.isMethodSymbol()) {
        name += "()";
      }
      conditionNames.add(name);
      if (updateSymbols.contains(reference)) {
        shouldRaiseIssue = false;
      }
    }
  }

}
