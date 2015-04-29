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
package org.sonar.java.closeresource;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExecutionState {
  @Nullable
  public ExecutionState parent;
  private Map<Symbol, CloseableOccurence> closeableOccurenceBySymbol = Maps.newHashMap();
  private IssuableSubscriptionVisitor check;

  public ExecutionState(Set<Symbol> excludedCloseables, IssuableSubscriptionVisitor check) {
    this.check = check;
    for (Symbol symbol : excludedCloseables) {
      closeableOccurenceBySymbol.put(symbol, CloseableOccurence.IGNORED);
    }
  }

  ExecutionState(ExecutionState parent) {
    this.parent = parent;
    this.check = parent.check;
  }

  public ExecutionState merge(ExecutionState executionState) {
    for (Map.Entry<Symbol, CloseableOccurence> entry : executionState.closeableOccurenceBySymbol.entrySet()) {
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
    for (Map.Entry<Symbol, CloseableOccurence> entry : currentES.closeableOccurenceBySymbol.entrySet()) {
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

  void insertIssues() {
    for (Tree tree : getUnclosedClosables()) {
      insertIssue(tree);
    }
  }

  void insertIssue(Tree tree) {
    Type type;
    if (tree.is(Tree.Kind.VARIABLE)) {
      type = ((VariableTree) tree).symbol().type();
    } else {
      type = ((IdentifierTree) tree).symbol().type();
    }
    check.addIssue(tree, "Close this \"" + type.name() + "\"");
  }

  void addCloseable(Symbol symbol, Tree lastAssignmentTree, @Nullable ExpressionTree assignmentExpression) {
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
    } else if (isNull(expression)) {
      return State.NULL;
    } else if (expression.is(Tree.Kind.NEW_CLASS)) {
      if (usesIgnoredCloseableAsArgument(((NewClassTree) expression).arguments())) {
        return State.IGNORED;
      }
      return State.OPEN;
    }
    // TODO SONARJAVA-1029 : Engine currently ignore closeables which are retrieved from method calls. Handle them as OPEN.
    return State.IGNORED;
  }

  private static boolean isNull(ExpressionTree expression) {
    return expression == null || expression.is(Tree.Kind.NULL_LITERAL);
  }

  private static boolean shouldBeIgnored(Symbol symbol, @Nullable ExpressionTree expression) {
    return shouldBeIgnored(symbol) || shouldBeIgnored(expression);
  }

  private static boolean shouldBeIgnored(Symbol symbol) {
    return symbol.isFinal()
      || CloseableVisitor.isIgnoredCloseableSubtype(symbol.type())
      || CloseableVisitor.isSubclassOfInputStreamOrOutputStreamWithoutClose(symbol.type());
  }

  private static boolean shouldBeIgnored(@Nullable ExpressionTree expression) {
    return expression != null && CloseableVisitor.isSubclassOfInputStreamOrOutputStreamWithoutClose(expression.symbolType());
  }

  private boolean usesIgnoredCloseableAsArgument(List<ExpressionTree> arguments) {
    for (ExpressionTree argument : arguments) {
      if (isNewClassWithIgnoredArguments(argument)) {
        return true;
      } else if (isMethodInvocationWithIgnoredArguments(argument)) {
        return true;
      } else if (useIgnoredCloseable(argument) || CloseableVisitor.isSubclassOfInputStreamOrOutputStreamWithoutClose(argument.symbolType())) {
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
    if (CloseableVisitor.isCloseableOrAutoCloseableSubtype(symbol.type()) && !symbol.owner().isMethodSymbol()) {
      return true;
    } else {
      CloseableOccurence currentOccurence = getCloseableOccurence(symbol);
      return currentOccurence != null && currentOccurence.state.isIgnored();
    }
  }

  void checkUsageOfClosables(List<ExpressionTree> expressions) {
    for (ExpressionTree expression : expressions) {
      checkUsageOfClosables(expression);
    }
  }

  void checkUsageOfClosables(@Nullable ExpressionTree expression) {
    if (expression != null) {
      if (expression.is(Tree.Kind.IDENTIFIER) && CloseableVisitor.isCloseableOrAutoCloseableSubtype(expression.symbolType())) {
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

  void markAsIgnored(Symbol symbol) {
    markAs(symbol, State.IGNORED);
  }

  void markAsClosed(Symbol symbol) {
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
