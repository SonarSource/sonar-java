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
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Symbol.TypeSymbol;
import org.sonar.plugins.java.api.semantic.Type;
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
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
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
public class CloseResourceCheck extends SubscriptionBaseVisitor {

  private enum State {

    // * | C | O | I | N |
    // --+---+---+---+---|
    // C | C | O | I | C | <- CLOSED
    // --+---+---+---+---|
    // O | O | O | I | O | <- OPEN
    // --+---+---+---+---|
    // I | I | I | I | I | <- IGNORED
    // --+---+---+---+---|
    // N | C | O | I | N | <- NULL
    // ------------------+

    NULL {
      @Override
      public State merge(State s) {
        return s;
      }
    },
    CLOSED {
      @Override
      public State merge(State s) {
        if (s == NULL) {
          return this;
        }
        return s;
      }
    },
    OPEN {
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

    public boolean isOpen() {
      return this.equals(OPEN);
    }
  }

  private static final String[] IGNORED_CLOSEABLE_SUBTYPES = {
    "java.io.ByteArrayOutputStream",
    "java.io.ByteArrayInputStream",
    "java.io.StringReader",
    "java.io.StringWriter",
    "java.io.CharArraReader",
    "java.io.CharArrayWriter"
  };

  private static final String CLOSE_METHOD_NAME = "close";
  private static final String JAVA_IO_CLOSEABLE = "java.io.Closeable";
  private static final String JAVA_LANG_AUTOCLOSEABLE = "java.lang.AutoCloseable";

  private static final MethodInvocationMatcherCollection CLOSE_INVOCATIONS = closeMethodInvocationMatcher();

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
      CloseableVisitor visitor = new CloseableVisitor(methodTree.parameters(), this);
      block.accept(visitor);
      visitor.executionState.insertIssues();
    }
  }

  private static MethodInvocationMatcherCollection closeMethodInvocationMatcher() {
    return MethodInvocationMatcherCollection.create(
      MethodInvocationMatcher.create()
        .typeDefinition(TypeCriteria.subtypeOf(JAVA_IO_CLOSEABLE))
        .name(CLOSE_METHOD_NAME)
        .withNoParameterConstraint(),
      MethodInvocationMatcher.create()
        .typeDefinition(TypeCriteria.subtypeOf(JAVA_LANG_AUTOCLOSEABLE))
        .name(CLOSE_METHOD_NAME)
        .withNoParameterConstraint());
  }

  private static boolean isCloseableOrAutoCloseableSubtype(Type type) {
    return type.isSubtypeOf(JAVA_IO_CLOSEABLE) || type.isSubtypeOf(JAVA_LANG_AUTOCLOSEABLE);
  }

  private static boolean isIgnoredCloseableSubtype(Type type) {
    for (String fullyQualifiedName : IGNORED_CLOSEABLE_SUBTYPES) {
      if (type.isSubtypeOf(fullyQualifiedName)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isSubclassOfInputStreamOrOutputStreamWithoutClose(Type type) {
    TypeSymbol typeSymbol = type.symbol();
    Type superClass = typeSymbol.superClass();
    if (superClass != null && (superClass.is("java.io.OutputStream") || superClass.is("java.io.InputStream"))) {
      return typeSymbol.lookupSymbols(CLOSE_METHOD_NAME).isEmpty();
    }
    return false;
  }

  private static class CloseableVisitor extends BaseTreeVisitor {

    private ExecutionState executionState;

    public CloseableVisitor(List<VariableTree> methodParameters, SubscriptionBaseVisitor check) {
      executionState = new ExecutionState(extractCloseableSymbols(methodParameters), check);
    }

    @Override
    public void visitVariable(VariableTree tree) {
      ExpressionTree initializer = tree.initializer();

      // check first usage of closeables in order to manage use of same symbol
      executionState.checkUsageOfClosables(initializer);

      Symbol symbol = tree.symbol();
      if (isCloseableOrAutoCloseableSubtype(symbol.type())) {
        executionState.addCloseable(symbol, tree, initializer);
      }
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      ExpressionTree variable = tree.variable();
      if (variable.is(Tree.Kind.IDENTIFIER, Tree.Kind.MEMBER_SELECT)) {
        ExpressionTree expression = tree.expression();

        // check first usage of closeables in order to manage use of same symbol
        executionState.checkUsageOfClosables(expression);

        IdentifierTree identifier;
        if (variable.is(Tree.Kind.IDENTIFIER)) {
          identifier = (IdentifierTree) variable;
        } else {
          identifier = ((MemberSelectExpressionTree) variable).identifier();
        }
        Symbol symbol = identifier.symbol();
        if (isCloseableOrAutoCloseableSubtype(identifier.symbolType()) && symbol.owner().isMethodSymbol()) {
          executionState.addCloseable(symbol, identifier, expression);
        }
      }
    }

    @Override
    public void visitNewClass(NewClassTree tree) {
      executionState.checkUsageOfClosables(tree.arguments());
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (CLOSE_INVOCATIONS.anyMatch(tree)) {
        ExpressionTree methodSelect = tree.methodSelect();
        if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
          ExpressionTree expression = ((MemberSelectExpressionTree) methodSelect).expression();
          if (expression.is(Tree.Kind.IDENTIFIER)) {
            executionState.markAsClosed(((IdentifierTree) expression).symbol());
          }
        }
      } else {
        executionState.checkUsageOfClosables(tree.arguments());
      }
    }

    @Override
    public void visitClass(ClassTree tree) {
      // do nothing, inner methods will be visited later
    }

    @Override
    public void visitReturnStatement(ReturnStatementTree tree) {
      executionState.checkUsageOfClosables(tree.expression());
    }

    @Override
    public void visitTryStatement(TryStatementTree tree) {
      for (VariableTree resource : tree.resources()) {
        executionState.markAsIgnored(resource.symbol());
      }

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
      ExecutionState caseGroupsES = new ExecutionState(executionState);
      executionState = new ExecutionState(caseGroupsES.parent);
      for (CaseGroupTree caseGroupTree : tree.cases()) {
        for (StatementTree statement : caseGroupTree.body()) {
          if (isBreakOrReturnStatement(statement)) {
            caseGroupsES = executionState.merge(caseGroupsES);
            executionState = new ExecutionState(caseGroupsES.parent);
          } else {
            scan(statement);
          }
        }
      }
      if (!lastStatementIsBreakOrReturn(tree)) {
        // merge the last execution state
        caseGroupsES = executionState.merge(caseGroupsES);
      }

      if (switchContainsDefaultLabel(tree)) {
        // the default block guarantees that we will cover all the paths
        executionState = caseGroupsES.parent.overrideBy(caseGroupsES);
      } else {
        executionState = caseGroupsES.parent.merge(caseGroupsES);
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

    private Set<Symbol> extractCloseableSymbols(List<VariableTree> variableTrees) {
      Set<Symbol> symbols = Sets.newHashSet();
      for (VariableTree variableTree : variableTrees) {
        Symbol symbol = variableTree.symbol();
        if (isCloseableOrAutoCloseableSubtype(symbol.type())) {
          symbols.add(symbol);
        }
      }
      return symbols;
    }
  }

  private static class CloseableOccurence {

    private static final CloseableOccurence IGNORED = new CloseableOccurence(null, State.IGNORED);
    @Nullable
    private Tree lastAssignment;
    private State state;

    public CloseableOccurence(@Nullable Tree lastAssignment, State state) {
      this.lastAssignment = lastAssignment;
      this.state = state;
    }
  }

  private static class ExecutionState {
    @Nullable
    private ExecutionState parent;
    private Map<Symbol, CloseableOccurence> closeableOccurenceBySymbol = Maps.newHashMap();
    private IssuableSubscriptionVisitor check;

    ExecutionState(Set<Symbol> excludedCloseables, IssuableSubscriptionVisitor check) {
      this.check = check;
      for (Symbol symbol : excludedCloseables) {
        closeableOccurenceBySymbol.put(symbol, CloseableOccurence.IGNORED);
      }
    }

    public ExecutionState(ExecutionState parent) {
      this.parent = parent;
      this.check = parent.check;
    }

    public ExecutionState merge(ExecutionState executionState) {
      for (Entry<Symbol, CloseableOccurence> entry : executionState.closeableOccurenceBySymbol.entrySet()) {
        Symbol symbol = entry.getKey();
        CloseableOccurence currentOccurence = getCloseableOccurence(symbol);
        CloseableOccurence occurenceToMerge = entry.getValue();
        if (currentOccurence != null) {
          currentOccurence.state = currentOccurence.state.merge(occurenceToMerge.state);
          closeableOccurenceBySymbol.put(symbol, currentOccurence);
        } else if (occurenceToMerge.state.isOpen()) {
          insertIssue(occurenceToMerge.lastAssignment);
        }
      }
      return this;
    }

    public ExecutionState overrideBy(ExecutionState currentES) {
      for (Entry<Symbol, CloseableOccurence> entry : currentES.closeableOccurenceBySymbol.entrySet()) {
        Symbol symbol = entry.getKey();
        CloseableOccurence occurence = entry.getValue();
        if (getCloseableOccurence(symbol) != null) {
          markAs(symbol, occurence.state);
        } else {
          closeableOccurenceBySymbol.put(symbol, occurence);
        }
      }
      return this;
    }

    public ExecutionState restoreParent() {
      if (parent != null) {
        insertIssues();
        return parent.merge(this);
      }
      return this;
    }

    private void insertIssues() {
      for (Tree tree : getUnclosedClosables()) {
        insertIssue(tree);
      }
    }

    private void insertIssue(Tree tree) {
      Type type;
      if (tree.is(Tree.Kind.VARIABLE)) {
        type = ((VariableTree) tree).symbol().type();
      } else {
        type = ((IdentifierTree) tree).symbol().type();
      }
      check.addIssue(tree, "Close this \"" + type.name() + "\"");
    }

    private void addCloseable(Symbol symbol, Tree lastAssignmentTree, @Nullable ExpressionTree assignmentExpression) {
      CloseableOccurence newOccurence = new CloseableOccurence(lastAssignmentTree, getCloseableStateFromExpression(symbol, assignmentExpression));
      CloseableOccurence knownOccurence = getCloseableOccurence(symbol);
      if (knownOccurence != null) {
        CloseableOccurence currentOccurence = closeableOccurenceBySymbol.get(symbol);
        if (currentOccurence != null && currentOccurence.state.isOpen()) {
          insertIssue(knownOccurence.lastAssignment);
        }
        if (!knownOccurence.state.isIgnored()) {
          closeableOccurenceBySymbol.put(symbol, newOccurence);
        }
      } else {
        closeableOccurenceBySymbol.put(symbol, newOccurence);
      }
    }

    private State getCloseableStateFromExpression(Symbol symbol, @Nullable ExpressionTree expression) {
      if (shouldBeIgnored(symbol, expression)) {
        return State.IGNORED;
      } else if (expression == null || expression.is(Tree.Kind.NULL_LITERAL)) {
        return State.NULL;
      } else if (expression.is(Tree.Kind.NEW_CLASS)) {
        if (usesIgnoredCloseableAsArgument(((NewClassTree) expression).arguments())) {
          return State.IGNORED;
        }
        return State.OPEN;
      }
      // FIXME Engine ignore closeable which are retrieved from method calls. Handle them as OPEN ?
      return State.IGNORED;
    }

    private static boolean shouldBeIgnored(Symbol symbol, @Nullable ExpressionTree expression) {
      return isIgnoredCloseableSubtype(symbol.type())
        || isSubclassOfInputStreamOrOutputStreamWithoutClose(symbol.type())
        || (expression != null && isSubclassOfInputStreamOrOutputStreamWithoutClose(expression.symbolType()));
    }

    private boolean usesIgnoredCloseableAsArgument(List<ExpressionTree> arguments) {
      for (ExpressionTree argument : arguments) {
        if (isNewClassWithIgnoredArguments(argument)) {
          return true;
        } else if (isMethodInvocationWithIgnoredArguments(argument)) {
          return true;
        } else if (useIgnoredCloseable(argument) || isSubclassOfInputStreamOrOutputStreamWithoutClose(argument.symbolType())) {
          return true;
        }
      }
      return false;
    }

    private boolean isNewClassWithIgnoredArguments(ExpressionTree argument) {
      return argument.is(Tree.Kind.NEW_CLASS) && usesIgnoredCloseableAsArgument(((NewClassTree) argument).arguments());
    }

    private boolean isMethodInvocationWithIgnoredArguments(ExpressionTree argument) {
      return argument.is(Tree.Kind.METHOD_INVOCATION) && usesIgnoredCloseableAsArgument(((MethodInvocationTree) argument).arguments());
    }

    private boolean useIgnoredCloseable(ExpressionTree expression) {
      if (expression.is(Tree.Kind.IDENTIFIER, Tree.Kind.MEMBER_SELECT)) {
        IdentifierTree identifier;
        if (expression.is(Tree.Kind.MEMBER_SELECT)) {
          identifier = ((MemberSelectExpressionTree) expression).identifier();
        } else {
          identifier = (IdentifierTree) expression;
        }
        if (isIgnoredCloseable(identifier.symbol())) {
          return true;
        }
      }
      return false;
    }

    private boolean isIgnoredCloseable(Symbol symbol) {
      if (isCloseableOrAutoCloseableSubtype(symbol.type()) && !symbol.owner().isMethodSymbol()) {
        return true;
      } else {
        CloseableOccurence currentOccurence = getCloseableOccurence(symbol);
        return currentOccurence != null && currentOccurence.state.isIgnored();
      }
    }

    private void checkUsageOfClosables(List<ExpressionTree> expressions) {
      for (ExpressionTree expression : expressions) {
        checkUsageOfClosables(expression);
      }
    }

    private void checkUsageOfClosables(@Nullable ExpressionTree expression) {
      if (expression != null) {
        if (expression.is(Tree.Kind.IDENTIFIER) && isCloseableOrAutoCloseableSubtype(expression.symbolType())) {
          markAsIgnored(((IdentifierTree) expression).symbol());
        } else if (expression.is(Tree.Kind.MEMBER_SELECT)) {
          checkUsageOfClosables(((MemberSelectExpressionTree) expression).identifier());
        } else if (expression.is(Tree.Kind.TYPE_CAST)) {
          checkUsageOfClosables(((TypeCastTree) expression).expression());
        } else if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
          checkUsageOfClosables(((MethodInvocationTree) expression).arguments());
        } else if (expression.is(Tree.Kind.NEW_CLASS)) {
          checkUsageOfClosables(((NewClassTree) expression).arguments());
        }
      }
    }

    private void markAsIgnored(Symbol symbol) {
      markAs(symbol, State.IGNORED);
    }

    private void markAsClosed(Symbol symbol) {
      markAs(symbol, State.CLOSED);
    }

    private void markAs(Symbol symbol, State state) {
      if (closeableOccurenceBySymbol.containsKey(symbol)) {
        closeableOccurenceBySymbol.get(symbol).state = state;
      } else if (parent != null) {
        CloseableOccurence occurence = getCloseableOccurence(symbol);
        if (occurence != null) {
          occurence.state = state;
          closeableOccurenceBySymbol.put(symbol, occurence);
        }
      }
    }

    private Set<Tree> getUnclosedClosables() {
      Set<Tree> results = Sets.newHashSet();
      for (CloseableOccurence occurence : closeableOccurenceBySymbol.values()) {
        if (occurence.state.isOpen()) {
          results.add(occurence.lastAssignment);
        }
      }
      return results;
    }

    @CheckForNull
    private CloseableOccurence getCloseableOccurence(Symbol symbol) {
      CloseableOccurence occurence = closeableOccurenceBySymbol.get(symbol);
      if (occurence != null) {
        return new CloseableOccurence(occurence.lastAssignment, occurence.state);
      } else if (parent != null) {
        return parent.getCloseableOccurence(symbol);
      }
      return null;
    }
  }
}
