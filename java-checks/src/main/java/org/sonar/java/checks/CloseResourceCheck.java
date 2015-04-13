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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.MethodInvocationMatcher;
import org.sonar.java.checks.methods.TypeCriteria;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Rule(
  key = "S2095",
  name = "Resources should be closed",
  tags = {"bug", "cert", "cwe", "denial-of-service", "leak", "security"},
  priority = Priority.BLOCKER)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.LOGIC_RELIABILITY)
@SqaleConstantRemediation("5min")
public class CloseResourceCheck extends SubscriptionBaseVisitor {

  private static enum CloseableState {
    NULL, CLOSED, NOT_CLOSED, IGNORED
  }

  private static final String JAVA_IO_CLOSEABLE = "java.io.Closeable";
  private static final MethodInvocationMatcher CLOSE_INVOCATION = closeMethodInvocationMatcher();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }

    MethodTree methodTree = (MethodTree) tree;
    BlockTree block = methodTree.block();
    if (block != null) {
      CloseableVisitor closeableVisitor = new CloseableVisitor(methodTree.parameters());
      block.accept(closeableVisitor);
      for (Tree ref : closeableVisitor.closeableTracker.getUnclosedClosables()) {
        insertIssue(ref);
      }
    }
  }

  private void insertIssue(Tree tree) {
    Type type;
    if (tree.is(Tree.Kind.VARIABLE)) {
      type = ((VariableTree) tree).symbol().type();
    } else {
      type = ((IdentifierTree) tree).symbol().type();
    }
    addIssue(tree, "Close this \"" + type.name() + "\"");
  }

  protected static MethodInvocationMatcher closeMethodInvocationMatcher() {
    return MethodInvocationMatcher.create()
      .typeDefinition(TypeCriteria.subtypeOf(JAVA_IO_CLOSEABLE))
      .name("close")
      .withNoParameterConstraint();
  }

  private static class CloseableVisitor extends BaseTreeVisitor {

    private final CloseableTracker closeableTracker;

    public CloseableVisitor(List<VariableTree> methodParameters) {
      closeableTracker = new CloseableTracker(extractCloseableSymbols(methodParameters));
    }

    @Override
    public void visitVariable(VariableTree tree) {
      ExpressionTree initializer = tree.initializer();
      if (tree.symbol().type().isSubtypeOf(JAVA_IO_CLOSEABLE)) {
        closeableTracker.addCloseable(tree.symbol(), tree, initializer);
      }
      closeableTracker.checkUsageOfClosables(initializer);
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      ExpressionTree variable = tree.variable();
      if (variable.is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree identifier = (IdentifierTree) variable;
        Symbol symbol = identifier.symbol();
        if (identifier.symbolType().isSubtypeOf(JAVA_IO_CLOSEABLE) && symbol.owner().isMethodSymbol()) {
          closeableTracker.addCloseable(symbol, variable, tree.expression());
        }
        closeableTracker.checkUsageOfClosables(tree.expression());
      }
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (CLOSE_INVOCATION.matches(tree)) {
        ExpressionTree methodSelect = tree.methodSelect();
        if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
          ExpressionTree expression = ((MemberSelectExpressionTree) methodSelect).expression();
          if (expression.is(Tree.Kind.IDENTIFIER)) {
            closeableTracker.markAsClosed(((IdentifierTree) expression).symbol());
          }
        }
      } else {
        closeableTracker.checkUsageOfClosables(tree);
      }
    }

    @Override
    public void visitReturnStatement(ReturnStatementTree tree) {
      super.visitReturnStatement(tree);
      closeableTracker.checkUsageOfClosables(tree.expression());
    }

    @Override
    public void visitTryStatement(TryStatementTree tree) {
      closeableTracker.excludedCloseables.addAll(extractCloseableSymbols(tree.resources()));
      super.visitTryStatement(tree);
    }

    private Set<Symbol> extractCloseableSymbols(List<VariableTree> variableTrees) {
      Set<Symbol> symbols = Sets.newHashSet();
      for (VariableTree variableTree : variableTrees) {
        Symbol symbol = variableTree.symbol();
        if (symbol.type().isSubtypeOf(JAVA_IO_CLOSEABLE)) {
          symbols.add(symbol);
        }
      }
      return symbols;
    }

    private static class CloseableTracker {
      private final Set<Symbol> excludedCloseables;
      private Map<Symbol, CloseableOccurence> closeableOccurenceBySymbol = Maps.newHashMap();
      private List<Tree> unclosedCloseableReferences = Lists.newLinkedList();

      CloseableTracker(Set<Symbol> excludedCloseables) {
        this.excludedCloseables = excludedCloseables;
      }

      private void addCloseable(Symbol symbol, Tree lastAssignmentTree, @Nullable ExpressionTree assignmentExpression) {
        if (!excludedCloseables.contains(symbol)) {
          if (isCloseableNotClosed(symbol)) {
            Tree lastAssignment = closeableOccurenceBySymbol.get(symbol).lastAssignment;
            unclosedCloseableReferences.add(lastAssignment);
          }
          closeableOccurenceBySymbol.put(symbol, new CloseableOccurence(lastAssignmentTree, getCloseableState(assignmentExpression)));
        }
      }

      private void checkUsageOfClosables(@Nullable ExpressionTree expression) {
        if (expression != null) {
          if (expression.is(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS)) {
            List<ExpressionTree> arguments = Lists.newArrayList();
            if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
              arguments = ((MethodInvocationTree) expression).arguments();
            } else {
              arguments = ((NewClassTree) expression).arguments();
            }
            markCloseableArgumentsAsIgnored(arguments);
          } else if (expression.is(Tree.Kind.IDENTIFIER)) {
            markAsIgnored(((IdentifierTree) expression).symbol());
          } else if (expression.is(Tree.Kind.MEMBER_SELECT)) {
            checkUsageOfClosables(((MemberSelectExpressionTree) expression).identifier());
          } else if (expression.is(Tree.Kind.TYPE_CAST)) {
            checkUsageOfClosables(((TypeCastTree) expression).expression());
          }
        }
      }

      private void markCloseableArgumentsAsIgnored(List<ExpressionTree> arguments) {
        for (ExpressionTree argument : arguments) {
          if (argument.is(Tree.Kind.IDENTIFIER) && argument.symbolType().isSubtypeOf(JAVA_IO_CLOSEABLE)) {
            markAsIgnored(((IdentifierTree) argument).symbol());
          }
        }
      }

      private CloseableState getCloseableState(ExpressionTree expression) {
        if (expression == null || expression.is(Tree.Kind.NULL_LITERAL)) {
          return CloseableState.NULL;
        } else if (expression.is(Tree.Kind.NEW_CLASS)) {
          return CloseableState.NOT_CLOSED;
        }
        return CloseableState.IGNORED;
      }

      private void markAsIgnored(Symbol symbol) {
        if (closeableOccurenceBySymbol.containsKey(symbol)) {
          closeableOccurenceBySymbol.get(symbol).ignore();
        }
      }

      private void markAsClosed(Symbol symbol) {
        if (closeableOccurenceBySymbol.containsKey(symbol)) {
          closeableOccurenceBySymbol.get(symbol).close();
        }
      }

      private boolean isCloseableNotClosed(Symbol symbol) {
        CloseableOccurence occurence = closeableOccurenceBySymbol.get(symbol);
        return occurence != null && CloseableState.NOT_CLOSED.equals(occurence.state);
      }

      private List<Tree> getUnclosedClosables() {
        List<Tree> results = Lists.newArrayList(unclosedCloseableReferences);
        for (Symbol symbol : closeableOccurenceBySymbol.keySet()) {
          if (isCloseableNotClosed(symbol)) {
            results.add(closeableOccurenceBySymbol.get(symbol).lastAssignment);
          }
        }
        return results;
      }

      private static class CloseableOccurence {

        private Tree lastAssignment;
        private CloseableState state;

        public CloseableOccurence(Tree lastAssignment, CloseableState state) {
          this.lastAssignment = lastAssignment;
          this.state = state;
        }

        public void close() {
          this.state = CloseableState.CLOSED;
        }

        public void ignore() {
          this.state = CloseableState.IGNORED;
        }
      }
    }
  }
}
