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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.MethodInvocationMatcher;
import org.sonar.java.checks.methods.MethodInvocationMatcherCollection;
import org.sonar.java.checks.methods.TypeCriteria;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.CheckForNull;
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
public class LocksNotUnlockedCheck extends SubscriptionBaseVisitor {

  private enum State {

    // * | U | L | I | N |
    // --+---+---+---+---|
    // U | U | L | I | U | <- UNLOCKED
    // --+---+---+---+---|
    // L | L | L | I | L | <- LOCKED
    // --+---+---+---+---|
    // I | I | I | I | I | <- IGNORED
    // --+---+---+---+---|
    // N | U | L | I | N | <- NULL
    // ------------------+

    NULL {
      @Override
      public State merge(State s) {
        return s;
      }
    },
    UNLOCKED {
      @Override
      public State merge(State s) {
        if (s == NULL) {
          return this;
        }
        return s;
      }
    },
    LOCKED {
      @Override
      public State merge(State s) {
        if (s == IGNORED) {
          return s;
        }
        return this;
      }
    },
    IGNORED {
      @Override
      public State merge(State s) {
        return this;
      }
    };

    public abstract State merge(State s);

    public boolean isIgnored() {
      return this.equals(IGNORED);
    }

    public boolean isLocked() {
      return this.equals(LOCKED);
    }
  }

  private static final String JAVA_LOCK = "java.util.concurrent.locks.Lock";

  private static final MethodInvocationMatcherCollection LOCK_INVOCATIONS = lockMethodInvocationMatcher();
  private static final MethodInvocationMatcher UNLOCK_INVOCATION = MethodInvocationMatcher.create().typeDefinition(JAVA_LOCK).name("unlock");

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
      LockedVisitor visitor = new LockedVisitor(methodTree.parameters(), this);
      block.accept(visitor);
      visitor.executionState.insertIssues();
    }
  }

  private static MethodInvocationMatcherCollection lockMethodInvocationMatcher() {
    return MethodInvocationMatcherCollection.create(
      MethodInvocationMatcher.create()
        .typeDefinition(TypeCriteria.subtypeOf(JAVA_LOCK))
        .name("lock"),
      MethodInvocationMatcher.create()
        .typeDefinition(TypeCriteria.subtypeOf(JAVA_LOCK))
        .name("lockInterruptibly"),
      MethodInvocationMatcher.create()
        .typeDefinition(TypeCriteria.subtypeOf(JAVA_LOCK))
        .name("tryLock")
        .withNoParameterConstraint());
  }

  private static class LockedVisitor extends BaseTreeVisitor {

    private ExecutionState executionState;

    public LockedVisitor(List<VariableTree> methodParameters, SubscriptionBaseVisitor check) {
      executionState = new ExecutionState(check);
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      ExpressionTree variable = tree.variable();
      if (variable.is(Tree.Kind.IDENTIFIER, Tree.Kind.MEMBER_SELECT)) {

        IdentifierTree identifier;
        if (variable.is(Tree.Kind.IDENTIFIER)) {
          identifier = (IdentifierTree) variable;
        } else {
          identifier = ((MemberSelectExpressionTree) variable).identifier();
        }
        Symbol symbol = identifier.symbol();
        if (symbol.type().isSubtypeOf(JAVA_LOCK)) {
          executionState.newValueForSymbol(symbol, tree);
        }
      }
    }

    @Override
    public void visitNewClass(NewClassTree tree) {
      // do nothing, inner methods will be visited later
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (LOCK_INVOCATIONS.anyMatch(tree)) {
        Symbol symbol = extractInvokedOnSymbol(tree.methodSelect());
        if (symbol != null) {
          executionState.addLockable(symbol, tree);
        }
      } else if (UNLOCK_INVOCATION.matches(tree)) {
        ExpressionTree methodSelect = tree.methodSelect();
        if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
          ExpressionTree expression = ((MemberSelectExpressionTree) methodSelect).expression();
          if (expression.is(Tree.Kind.IDENTIFIER)) {
            executionState.markAsUnlocked(((IdentifierTree) expression).symbol());
          }
        }
      }
    }

    @CheckForNull
    private Symbol extractInvokedOnSymbol(ExpressionTree expressionTree) {
      if (expressionTree.is(Tree.Kind.MEMBER_SELECT)) {
        return extractSymbol(((MemberSelectExpressionTree) expressionTree).expression());
      } else if (expressionTree.is(Tree.Kind.IDENTIFIER)) {
        return ((IdentifierTree) expressionTree).symbol().owner();
      }
      return null;
    }

    @CheckForNull
    private Symbol extractSymbol(ExpressionTree expressionTree) {
      if (expressionTree.is(Tree.Kind.MEMBER_SELECT)) {
        return ((MemberSelectExpressionTree) expressionTree).identifier().symbol();
      } else if (expressionTree.is(Tree.Kind.IDENTIFIER)) {
        return ((IdentifierTree) expressionTree).symbol();
      }
      return null;
    }

    @Override
    public void visitClass(ClassTree tree) {
      // do nothing, inner methods will be visited later
    }

    @Override
    public void visitTryStatement(TryStatementTree tree) {
      ExecutionState blockES = new ExecutionState(executionState);
      executionState = blockES;
      scan(tree.block());

      for (CatchTree catchTree : tree.catches()) {
        executionState = new ExecutionState(blockES.parent);
        scan(catchTree.block());
        blockES.merge(executionState);
      }

      if (tree.finallyBlock() != null) {
        executionState = new ExecutionState(blockES.parent);
        scan(tree.finallyBlock());
        executionState = blockES.parent.overrideBy(blockES.overrideBy(executionState));
      } else {
        executionState = blockES.parent.merge(blockES);
      }
    }

    @Override
    public void visitIfStatement(IfStatementTree tree) {
      scan(tree.condition());
      ExecutionState thenES = new ExecutionState(executionState);
      executionState = thenES;
      scan(tree.thenStatement());

      if (tree.elseStatement() == null) {
        executionState = thenES.parent.merge(thenES);
      } else {
        ExecutionState elseES = new ExecutionState(thenES.parent);
        executionState = elseES;
        scan(tree.elseStatement());
        executionState = thenES.parent.overrideBy(thenES.merge(elseES));
      }
    }

    @Override
    public void visitSwitchStatement(SwitchStatementTree tree) {
      scan(tree.expression());
      ExecutionState resultingES = new ExecutionState(executionState);
      executionState = new ExecutionState(executionState);
      for (CaseGroupTree caseGroupTree : tree.cases()) {
        for (StatementTree statement : caseGroupTree.body()) {
          if (isBreakOrReturnStatement(statement)) {
            resultingES = executionState.merge(resultingES);
            executionState = new ExecutionState(resultingES.parent);
          } else {
            scan(statement);
          }
        }
      }
      if (!lastStatementIsBreakOrReturn(tree)) {
        // merge the last execution state
        resultingES = executionState.merge(resultingES);
      }

      if (switchContainsDefaultLabel(tree)) {
        // the default block guarantees that we will cover all the paths
        executionState = resultingES.parent.overrideBy(resultingES);
      } else {
        executionState = resultingES.parent.merge(resultingES);
      }
    }

    private boolean isBreakOrReturnStatement(StatementTree statement) {
      return statement.is(Tree.Kind.BREAK_STATEMENT, Tree.Kind.RETURN_STATEMENT);
    }

    private boolean switchContainsDefaultLabel(SwitchStatementTree tree) {
      for (CaseGroupTree caseGroupTree : tree.cases()) {
        for (CaseLabelTree label : caseGroupTree.labels()) {
          if ("default".equals(label.caseOrDefaultKeyword().text())) {
            return true;
          }
        }
      }
      return false;
    }

    private boolean lastStatementIsBreakOrReturn(SwitchStatementTree tree) {
      List<CaseGroupTree> cases = tree.cases();
      if (!cases.isEmpty()) {
        List<StatementTree> lastStatements = cases.get(cases.size() - 1).body();
        return !lastStatements.isEmpty() && isBreakOrReturnStatement(lastStatements.get(lastStatements.size() - 1));
      }
      return false;
    }

    @Override
    public void visitWhileStatement(WhileStatementTree tree) {
      scan(tree.condition());
      visitStatement(tree.statement());
    }

    @Override
    public void visitDoWhileStatement(DoWhileStatementTree tree) {
      visitStatement(tree.statement());
      scan(tree.condition());
    }

    @Override
    public void visitForStatement(ForStatementTree tree) {
      scan(tree.condition());
      scan(tree.initializer());
      scan(tree.update());
      visitStatement(tree.statement());
    }

    @Override
    public void visitForEachStatement(ForEachStatement tree) {
      scan(tree.variable());
      scan(tree.expression());
      visitStatement(tree.statement());
    }

    private void visitStatement(StatementTree tree) {
      executionState = new ExecutionState(executionState);
      scan(tree);
      executionState = executionState.restoreParent();
    }
  }

  private static class LockedOccurence {

    @Nullable
    private Tree lastAssignment;
    private State state;

    public LockedOccurence(@Nullable Tree lastAssignment, State state) {
      this.lastAssignment = lastAssignment;
      this.state = state;
    }

    @Override
    public String toString() {
      return ((JavaTree) lastAssignment).getLine()+" : "+state.name();
    }
  }

  private static class ExecutionState {
    @Nullable
    private ExecutionState parent;
    private Map<Symbol, LockedOccurence> lockedOccurencesBySymbol = Maps.newHashMap();
    private IssuableSubscriptionVisitor check;

    ExecutionState(IssuableSubscriptionVisitor check) {
      this.check = check;
    }

    public ExecutionState(ExecutionState parent) {
      this.parent = parent;
      this.check = parent.check;
    }

    @Override
    public String toString() {
      String s = "";
      for (Entry<Symbol, LockedOccurence> symbolLockedOccurenceEntry : lockedOccurencesBySymbol.entrySet()) {
        LockedOccurence value = symbolLockedOccurenceEntry.getValue();
        s += "  " + symbolLockedOccurenceEntry.getKey().name() + "  " + ((JavaTree) value.lastAssignment).getLine() + "  " + value.state.name() + "  ";
      }
      if(s.isEmpty()) {
        s += " no locked occurences ";
      }
      return s;
    }

    public ExecutionState merge(ExecutionState executionState) {
      for (Entry<Symbol, LockedOccurence> entry : executionState.lockedOccurencesBySymbol.entrySet()) {
        Symbol symbol = entry.getKey();
        LockedOccurence currentOccurence = getLockedOccurence(symbol);
        LockedOccurence occurenceToMerge = entry.getValue();
        if (currentOccurence != null) {
          currentOccurence.state = currentOccurence.state.merge(occurenceToMerge.state);
          lockedOccurencesBySymbol.put(symbol, currentOccurence);
        } else {
          // possible way to solve the problem of variable defined in outer Execution state. lockedOccurencesBySymbol.put(symbol,
          // occurenceToMerge);
          if (occurenceToMerge.state.isLocked()) {
            insertIssue(occurenceToMerge.lastAssignment);
          }
        }
      }
      return this;
    }

    public ExecutionState overrideBy(ExecutionState currentES) {
      for (Entry<Symbol, LockedOccurence> entry : currentES.lockedOccurencesBySymbol.entrySet()) {
        Symbol symbol = entry.getKey();
        LockedOccurence occurence = entry.getValue();
        if (getLockedOccurence(symbol) != null) {
          markAs(symbol, occurence.state);
        } else {
          lockedOccurencesBySymbol.put(symbol, occurence);
        }
      }
      return this;
    }

    public ExecutionState restoreParent() {
      if (parent != null) {
        // insertIssues();
        return parent.merge(this);
      }
      return this;
    }

    private void insertIssues() {
      for (Tree tree : getLeftLocked()) {
        insertIssue(tree);
      }
    }

    private void insertIssue(Tree tree) {
      check.addIssue(tree, "Unlock this lock.");
    }

    private void  addLockable(Symbol symbol, Tree lockInvocationTree) {
      LockedOccurence knownOccurence = getLockedOccurence(symbol);
      if (knownOccurence != null) {
        if(knownOccurence.state == State.LOCKED) {
          insertIssue(knownOccurence.lastAssignment);
        }
      } else {
        // no known occurence, means its a field or a method param.
        createValueInTopExecutionState(symbol, lockInvocationTree, State.NULL);
      }
      lockedOccurencesBySymbol.put(symbol, new LockedOccurence(lockInvocationTree, State.LOCKED));
    }

    private void createValueInTopExecutionState(Symbol symbol, Tree tree, State state) {
      ExecutionState top = this;
      while (top.parent != null) {
        top = top.parent;
      }
      top.lockedOccurencesBySymbol.put(symbol, new LockedOccurence(tree, state));
    }

    public void newValueForSymbol(Symbol symbol, Tree definition) {
      checkCreationOfIssue(symbol);
      lockedOccurencesBySymbol.put(symbol, new LockedOccurence(definition, State.NULL));
    }

    @CheckForNull
    private LockedOccurence checkCreationOfIssue(Symbol symbol) {
      LockedOccurence knownOccurence = getLockedOccurence(symbol);
      if (knownOccurence != null) {
        LockedOccurence currentOccurence = lockedOccurencesBySymbol.get(symbol);
        if (currentOccurence != null && currentOccurence.state.isLocked()) {
          insertIssue(knownOccurence.lastAssignment);
        }
        lockedOccurencesBySymbol.remove(symbol);
      }
      return knownOccurence;
    }

    private void markAsUnlocked(Symbol symbol) {
      markAs(symbol, State.UNLOCKED);
    }

    private void markAs(Symbol symbol, State state) {
      LockedOccurence occurence = getLockedOccurence(symbol);
      if (occurence != null) {
        lockedOccurencesBySymbol.put(symbol, new LockedOccurence(occurence.lastAssignment, state));
      }
    }

    private Set<Tree> getLeftLocked() {
      Set<Tree> results = Sets.newHashSet();
      for (LockedOccurence occurence : lockedOccurencesBySymbol.values()) {
        if (occurence.state.isLocked()) {
          results.add(occurence.lastAssignment);
        }
      }
      return results;
    }

    @CheckForNull
    private LockedOccurence getLockedOccurence(Symbol symbol) {
      LockedOccurence occurence = lockedOccurencesBySymbol.get(symbol);
      if (occurence != null) {
        return new LockedOccurence(occurence.lastAssignment, occurence.state);
      } else if (parent != null) {
        return parent.getLockedOccurence(symbol);
      }
      return null;
    }

  }
}
