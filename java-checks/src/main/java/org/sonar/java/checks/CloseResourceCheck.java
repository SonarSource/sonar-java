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
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
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
import java.util.Map.Entry;
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

  private static enum State {
    NULL, CLOSED, OPEN, IGNORED
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
      for (Tree ref : closeableVisitor.executionState.getUnclosedClosables()) {
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

    private ExecutionState executionState;

    public CloseableVisitor(List<VariableTree> methodParameters) {
      executionState = new ExecutionState(extractCloseableSymbols(methodParameters));
    }

    @Override
    public void visitVariable(VariableTree tree) {
      ExpressionTree initializer = tree.initializer();
      if (tree.symbol().type().isSubtypeOf(JAVA_IO_CLOSEABLE)) {
        executionState.addCloseable(tree.symbol(), tree, initializer);
      }
      executionState.checkUsageOfClosables(initializer);
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      ExpressionTree variable = tree.variable();
      if (variable.is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree identifier = (IdentifierTree) variable;
        Symbol symbol = identifier.symbol();
        if (identifier.symbolType().isSubtypeOf(JAVA_IO_CLOSEABLE) && symbol.owner().isMethodSymbol()) {
          executionState.addCloseable(symbol, variable, tree.expression());
        }
        executionState.checkUsageOfClosables(tree.expression());
      }
    }

    @Override
    public void visitNewClass(NewClassTree tree) {
      super.visitNewClass(tree);
      executionState.checkUsageOfClosables(tree);
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (CLOSE_INVOCATION.matches(tree)) {
        ExpressionTree methodSelect = tree.methodSelect();
        if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
          ExpressionTree expression = ((MemberSelectExpressionTree) methodSelect).expression();
          if (expression.is(Tree.Kind.IDENTIFIER)) {
            executionState.markAsClosed(((IdentifierTree) expression).symbol());
          }
        }
      } else {
        executionState.checkUsageOfClosables(tree);
      }
    }

    @Override
    public void visitReturnStatement(ReturnStatementTree tree) {
      super.visitReturnStatement(tree);
      executionState.checkUsageOfClosables(tree.expression());
    }

    @Override
    public void visitTryStatement(TryStatementTree tree) {
      executionState.exclude(extractCloseableSymbols(tree.resources()));

      ExecutionState currentES = new ExecutionState(executionState);
      ExecutionState endES = new ExecutionState(executionState);

      visitBlock(tree.block(), currentES, endES);

      for (CatchTree catchTree : tree.catches()) {
        visitBlock(catchTree.block(), new ExecutionState(currentES), endES);
      }

      if (tree.finallyBlock() != null) {
        visitBlock(tree.finallyBlock(), new ExecutionState(currentES), endES, true);
      }

      executionState = endES;
    }

    private void visitBlock(BlockTree block, ExecutionState startES, ExecutionState endEs) {
      visitBlock(block, startES, endEs, false);
    }

    private void visitBlock(BlockTree block, ExecutionState startES, ExecutionState endEs, boolean forceState) {
      ExecutionState currentES = new ExecutionState(startES);
      executionState = currentES;
      block.accept(this);
      endEs.merge(currentES, forceState);
    }

    @Override
    public void visitIfStatement(IfStatementTree tree) {
      tree.condition().accept(this);

      ExecutionState currentES = executionState;
      ExecutionState thenES = new ExecutionState(currentES);
      ExecutionState elseES = new ExecutionState(currentES);

      executionState = thenES;
      tree.thenStatement().accept(this);

      if (tree.elseStatement() != null) {
        executionState = elseES;
        tree.elseStatement().accept(this);
      }
      executionState = currentES.merge(thenES, elseES);
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

    private static class ExecutionState {
      private Set<Symbol> excludedCloseables;
      private Map<Symbol, CloseableOccurence> closeableOccurenceBySymbol = Maps.newHashMap();
      private Set<Tree> unclosedCloseableReferences = Sets.newHashSet();

      ExecutionState(Set<Symbol> excludedCloseables) {
        this.excludedCloseables = excludedCloseables;
      }

      public ExecutionState(ExecutionState baseES) {
        this.excludedCloseables = Sets.newHashSet(baseES.excludedCloseables);
        for (Entry<Symbol, CloseableOccurence> entry : baseES.closeableOccurenceBySymbol.entrySet()) {
          this.closeableOccurenceBySymbol.put(entry.getKey(), new CloseableOccurence(entry.getValue()));
        }
        // the unclosed closeable set is let empty
      }

      public ExecutionState merge(ExecutionState es, boolean forceNewState) {
        return this.merge(es, null, forceNewState);
      }

      public ExecutionState merge(ExecutionState es1, @Nullable ExecutionState es2) {
        return this.merge(es1, es2, false);
      }

      public ExecutionState merge(ExecutionState es1, @Nullable ExecutionState es2, boolean forceFirstState) {
        // only look for symbols which are known from the current execution state
        for (Symbol symbol : closeableOccurenceBySymbol.keySet()) {
          CloseableOccurence currentOccurence = closeableOccurenceBySymbol.get(symbol);
          State state1 = es1.closeableOccurenceBySymbol.get(symbol).state;
          State state2 = currentOccurence.state;
          if (es2 != null) {
            state2 = es2.closeableOccurenceBySymbol.get(symbol).state;
          }

          // * | C | O | I | N |
          // --+---+---+---+---|
          // C | C | O | I | I | <- CLOSED
          // --+---+---+---+---|
          // O | O | O | I | I | <- OPEN
          // --+---+---+---+---|
          // I | I | I | I | I | <- IGNORED
          // --+---+---+---+---|
          // N | I | I | I | N | <- NULL
          // ------------------+

          // same state after the if statement
          if (forceFirstState || state1.equals(state2)) {
            currentOccurence.state = state1;
          } else if ((State.CLOSED.equals(state1) && State.OPEN.equals(state2)) || (State.OPEN.equals(state1) && State.CLOSED.equals(state2))) {
            currentOccurence.state = State.OPEN;
          } else {
            currentOccurence.state = State.IGNORED;
          }

          // clear the closeable occurences from the child execution states
          es1.closeableOccurenceBySymbol.remove(symbol);
          if (es2 != null) {
            es2.closeableOccurenceBySymbol.remove(symbol);
          }
        }

        // add the closeables which could have been created but not properly closed in the context of the child ESs
        unclosedCloseableReferences.addAll(es1.getUnclosedClosables());
        if (es2 != null) {
          unclosedCloseableReferences.addAll(es2.getUnclosedClosables());
        }
        return this;
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
            for (ExpressionTree argument : arguments) {
              checkUsageOfClosables(argument);
            }
          } else if (expression.is(Tree.Kind.IDENTIFIER) && expression.symbolType().isSubtypeOf(JAVA_IO_CLOSEABLE)) {
            markAsIgnored(((IdentifierTree) expression).symbol());
          } else if (expression.is(Tree.Kind.MEMBER_SELECT)) {
            checkUsageOfClosables(((MemberSelectExpressionTree) expression).identifier());
          } else if (expression.is(Tree.Kind.TYPE_CAST)) {
            checkUsageOfClosables(((TypeCastTree) expression).expression());
          }
        }
      }

      private State getCloseableState(ExpressionTree expression) {
        if (expression == null || expression.is(Tree.Kind.NULL_LITERAL)) {
          return State.NULL;
        } else if (expression.is(Tree.Kind.NEW_CLASS)) {
          return State.OPEN;
        }
        return State.IGNORED;
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

      public void exclude(Set<Symbol> symbols) {
        excludedCloseables.addAll(symbols);
      }

      private boolean isCloseableNotClosed(Symbol symbol) {
        CloseableOccurence occurence = closeableOccurenceBySymbol.get(symbol);
        return occurence != null && State.OPEN.equals(occurence.state);
      }

      private Set<Tree> getUnclosedClosables() {
        Set<Tree> results = Sets.newHashSet(unclosedCloseableReferences);
        for (Symbol symbol : closeableOccurenceBySymbol.keySet()) {
          if (isCloseableNotClosed(symbol)) {
            results.add(closeableOccurenceBySymbol.get(symbol).lastAssignment);
          }
        }
        return results;
      }

      private static class CloseableOccurence {

        private Tree lastAssignment;
        private State state;

        public CloseableOccurence(Tree lastAssignment, State state) {
          this.lastAssignment = lastAssignment;
          this.state = state;
        }

        public CloseableOccurence(CloseableOccurence value) {
          this.lastAssignment = value.lastAssignment;
          this.state = value.state;
        }

        public void close() {
          this.state = State.CLOSED;
        }

        public void ignore() {
          this.state = State.IGNORED;
        }
      }
    }
  }
}
