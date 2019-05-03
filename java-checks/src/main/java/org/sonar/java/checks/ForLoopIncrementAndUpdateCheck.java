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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Rule(key = "S1994")
public class ForLoopIncrementAndUpdateCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.FOR_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    ForStatementTree forStatementTree = (ForStatementTree) tree;
    if (forStatementTree.update().isEmpty() || forStatementTree.condition() == null) {
      return;
    }
    Collection<Symbol> symbolsFromConditionsNotUpdated = symbolsFromConditionsNotUpdated(forStatementTree);
    if (!symbolsFromConditionsNotUpdated.isEmpty()) {
      Map<Symbol, Tree> updatesInBody = singleUpdatesInBody(forStatementTree.statement(), symbolsFromConditionsNotUpdated);
      if (!updatesInBody.isEmpty()) {
        reportIssue(forStatementTree.forKeyword(), getMessage(updatesInBody.keySet()), getSecondaries(updatesInBody.values()), null);
      }
    }
  }

  private static String getMessage(Set<Symbol> updates) {
    return String.format("Move the update of %s into this loop's update clause.", symbolNames(updates));
  }

  private static Collection<Symbol> symbolsFromConditionsNotUpdated(ForStatementTree forStatementTree) {
    Collection<Symbol> symbols = getConditionSymbols(forStatementTree.condition());
    symbols.removeAll(getUpdatedSymbols(forStatementTree.update()));
    return symbols;
  }

  private static Collection<Symbol> getConditionSymbols(ExpressionTree condition) {
    ConditionVisitor conditionVisitor = new ConditionVisitor();
    condition.accept(conditionVisitor);
    return conditionVisitor.symbols;
  }

  private static Collection<Symbol> getUpdatedSymbols(ListTree<StatementTree> updates) {
    UpdateVisitor updateVisitor = new UpdateVisitor();
    updates.accept(updateVisitor);
    return updateVisitor.symbols;
  }

  private static Map<Symbol, Tree> singleUpdatesInBody(StatementTree statement, Collection<Symbol> conditionsNotUpdated) {
    UpdatesInBodyVisitor updatedInBodyVisitor = new UpdatesInBodyVisitor(conditionsNotUpdated);
    statement.accept(updatedInBodyVisitor);
    // only report if there is symbols which are updated only once, as multiple update cannot easily be moved to updates
    return updatedOnlyOnceWithUnaryExpression(updatedInBodyVisitor.updates, statement);
  }

  private static String symbolNames(Set<Symbol> symbols) {
    return symbols.stream().map(s -> "\"" + s.name() + "\"").sorted().collect(Collectors.joining(","));
  }

  private static List<JavaFileScannerContext.Location> getSecondaries(Collection<Tree> updateTrees) {
    return updateTrees.stream().map(t -> new JavaFileScannerContext.Location("", t)).collect(Collectors.toList());
  }

  private static Map<Symbol, Tree> updatedOnlyOnceWithUnaryExpression(Multimap<Symbol, Tree> updatesInBody, StatementTree forStatementBody) {
    Map<Symbol, Tree> result = new HashMap<>();
    for (Symbol updatedSymbol : updatesInBody.keySet()) {
      Collection<Tree> updateTrees = updatesInBody.get(updatedSymbol);
      if (updateTrees.size() == 1) {
        Tree updateTree = updateTrees.iterator().next();
        if (updateTree.is(Tree.Kind.POSTFIX_INCREMENT, Tree.Kind.POSTFIX_DECREMENT, Tree.Kind.PREFIX_INCREMENT, Tree.Kind.PREFIX_DECREMENT)
          // only consider unary expression as single statement, and not nested
          && updateTree.parent().is(Tree.Kind.EXPRESSION_STATEMENT) && updateTree.parent().parent() == forStatementBody) {
          result.put(updatedSymbol, updateTree);
        }
      }
    }
    return result;
  }

  private static class ConditionVisitor extends BaseTreeVisitor {
    Collection<Symbol> symbols = new HashSet<>();

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      Symbol symbol = tree.symbol();
      if (symbol.isVariableSymbol()) {
        symbols.add(symbol);
      }
      super.visitIdentifier(tree);
    }
  }

  private static class UpdateVisitor extends BaseTreeVisitor {
    Collection<Symbol> symbols = new ArrayList<>();

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

    @Override
    public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
      checkIdentifier(tree.identifier());
      // skip expression
    }

    private void checkIdentifier(ExpressionTree expression) {
      ExpressionTree expr = ExpressionUtils.skipParentheses(expression);
      if (expr.is(Tree.Kind.IDENTIFIER)) {
        addSymbol((IdentifierTree) expr);
      } else if (expr.is(Tree.Kind.MEMBER_SELECT)) {
        addSymbol(((MemberSelectExpressionTree) expr).identifier());
      }
    }

    private void addSymbol(IdentifierTree identifierTree) {
      Symbol symbol = identifierTree.symbol();
      if (!symbol.isUnknown()) {
        symbols.add(symbol);
      }
    }
  }

  private static class UpdatesInBodyVisitor extends BaseTreeVisitor {
    private final Collection<Symbol> targets;
    private final Multimap<Symbol, Tree> updates = ArrayListMultimap.create();

    private UpdatesInBodyVisitor(Collection<Symbol> targets) {
      this.targets = targets;
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      checkIdentifier(tree.variable(), tree);
      super.visitAssignmentExpression(tree);
    }

    @Override
    public void visitUnaryExpression(UnaryExpressionTree tree) {
      checkIdentifier(tree.expression(), tree);
      super.visitUnaryExpression(tree);
    }

    private void checkIdentifier(ExpressionTree expression, Tree root) {
      ExpressionTree expr = ExpressionUtils.skipParentheses(expression);
      if (expr.is(Tree.Kind.IDENTIFIER)) {
        addSymbol((IdentifierTree) expr, root);
      } else if (expr.is(Tree.Kind.MEMBER_SELECT)) {
        addSymbol(((MemberSelectExpressionTree) expr).identifier(), root);
      }
    }

    private void addSymbol(IdentifierTree identifierTree, Tree root) {
      Symbol symbol = identifierTree.symbol();
      if (targets.contains(symbol)) {
        updates.put(symbol, root);
      }
    }
  }

}
