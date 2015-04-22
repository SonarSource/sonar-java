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
import org.sonar.plugins.java.api.semantic.Symbol.TypeSymbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ClassTree;
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
    NULL, CLOSED, OPEN, IGNORED;

    public static State mergeStates(State state1, State state2) {

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

      if (state1.equals(state2)) {
        return state1;
      } else if ((State.CLOSED.equals(state1) && State.OPEN.equals(state2)) || (State.OPEN.equals(state1) && State.CLOSED.equals(state2))) {
        return State.OPEN;
      } else if (State.NULL.equals(state1)) {
        return state2;
      } else if (State.NULL.equals(state2)) {
        return state1;
      }
      return State.IGNORED;
    }

    public boolean isIgnored() {
      return this.equals(IGNORED);
    }

    public boolean isOpen() {
      return this.equals(OPEN);
    }
  }

  private static final String IGNORED_CLOSEABLE_SUBTYPES[] = {
    "java.io.ByteArrayOutputStream",
    "java.io.ByteArrayInputStream",
    "java.io.StringReader",
    "java.io.StringWriter",
    "java.io.CharArraReader",
    "java.io.CharArrayWriter"
  };

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
        .name("close")
        .withNoParameterConstraint(),
      MethodInvocationMatcher.create()
        .typeDefinition(TypeCriteria.subtypeOf(JAVA_LANG_AUTOCLOSEABLE))
        .name("close")
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
      return typeSymbol.lookupSymbols("close").isEmpty();
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
    private Tree lastAssignment;
    private State state;

    public CloseableOccurence(Tree lastAssignment, State state) {
      this.lastAssignment = lastAssignment;
      this.state = state;
    }

    @Override
    public String toString() {
      JavaTree tree = (JavaTree) lastAssignment;
      return "CloseableOccurence [lastAssignment=" + tree.getName() + " (L." + tree.getLine() + "), state=" + state + "]";
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
          currentOccurence.state = State.mergeStates(currentOccurence.state, occurenceToMerge.state);
          closeableOccurenceBySymbol.put(symbol, currentOccurence);
        } else if (occurenceToMerge.state.isOpen()) {
          insertIssue(occurenceToMerge.lastAssignment);
        }
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

    private void addCloseable(Symbol symbol, Tree lastAssignmentTree, @Nullable ExpressionTree assignmentExpression) {
      CloseableOccurence newOccurence = new CloseableOccurence(lastAssignmentTree, getCloseableStateFromExpression(symbol, assignmentExpression));
      CloseableOccurence currentOccurence = getCloseableOccurence(symbol);
      if (currentOccurence != null) {
        if (currentOccurence.state.isOpen()) {
          insertIssue(currentOccurence.lastAssignment);
        }
        if (!currentOccurence.state.isIgnored()) {
          closeableOccurenceBySymbol.put(symbol, newOccurence);
        }
      } else {
        closeableOccurenceBySymbol.put(symbol, newOccurence);
      }
    }

    private State getCloseableStateFromExpression(Symbol symbol, @Nullable ExpressionTree expression) {
      if (isIgnoredCloseableSubtype(symbol.type())
        || isSubclassOfInputStreamOrOutputStreamWithoutClose(symbol.type())
        || (expression != null && isSubclassOfInputStreamOrOutputStreamWithoutClose(expression.symbolType()))) {
        return State.IGNORED;
      } else if (expression == null
        || expression.is(Tree.Kind.NULL_LITERAL)) {
        return State.NULL;
      } else if (expression.is(Tree.Kind.NEW_CLASS)) {
        if (usesIgnoredCloseable(((NewClassTree) expression).arguments())) {
          return State.IGNORED;
        }
        return State.OPEN;
      }
      // FIXME Engine ignore closeable which are retrieved from method calls. Handle them as OPEN ?
      return State.IGNORED;
    }

    private boolean usesIgnoredCloseable(List<ExpressionTree> arguments) {
      for (ExpressionTree argument : arguments) {
        if (argument.is(Tree.Kind.IDENTIFIER, Tree.Kind.MEMBER_SELECT)) {
          IdentifierTree identifier;
          if (argument.is(Tree.Kind.MEMBER_SELECT)) {
            identifier = ((MemberSelectExpressionTree) argument).identifier();
          } else {
            identifier = (IdentifierTree) argument;
          }
          if (isIgnoredCloseable(identifier.symbol())) {
            return true;
          }
        } else if (argument.is(Tree.Kind.NEW_CLASS) && usesIgnoredCloseable(((NewClassTree) argument).arguments())) {
          return true;
        } else if (argument.is(Tree.Kind.METHOD_INVOCATION) && usesIgnoredCloseable(((MethodInvocationTree) argument).arguments())) {
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
