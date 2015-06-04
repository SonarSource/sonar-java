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

import org.sonar.java.checks.methods.MethodMatcher;
import org.sonar.java.checks.methods.MethodInvocationMatcherCollection;
import org.sonar.java.checks.methods.TypeCriteria;
import org.sonar.java.symexecengine.ExecutionState;
import org.sonar.java.symexecengine.State;
import org.sonar.java.symexecengine.SymbolicExecutionCheck;
import org.sonar.java.symexecengine.SymbolicValue;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;

import javax.annotation.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CloseableVisitor extends SymbolicExecutionCheck {

  private static final String CLOSE_METHOD_NAME = "close";
  private static final String JAVA_IO_CLOSEABLE = "java.io.Closeable";
  private static final String JAVA_LANG_AUTOCLOSEABLE = "java.lang.AutoCloseable";

  private static final MethodInvocationMatcherCollection CLOSE_INVOCATIONS = MethodInvocationMatcherCollection.create(
    MethodMatcher.create()
      .typeDefinition(TypeCriteria.subtypeOf(JAVA_IO_CLOSEABLE))
      .name(CLOSE_METHOD_NAME)
      .withNoParameterConstraint(),
    MethodMatcher.create()
      .typeDefinition(TypeCriteria.subtypeOf(JAVA_LANG_AUTOCLOSEABLE))
      .name(CLOSE_METHOD_NAME)
      .withNoParameterConstraint(),
    MethodMatcher.create()
      .typeDefinition(TypeCriteria.subtypeOf("org.springframework.context.support.AbstractApplicationContext"))
      .name("registerShutdownHook")
      .withNoParameterConstraint());

  private static final String[] IGNORED_CLOSEABLE_SUBTYPES = {
    "java.io.ByteArrayOutputStream",
    "java.io.ByteArrayInputStream",
    "java.io.StringReader",
    "java.io.StringWriter",
    "java.io.CharArrayReader",
    "java.io.CharArrayWriter"
  };

  @Override
  protected void initialize(ExecutionState executionState, MethodTree tree, List<SymbolicValue> arguments) {
    for (SymbolicValue argument : arguments) {
      ignoreValue(executionState, argument);
    }
  }

  private void ignoreValue(ExecutionState executionState, SymbolicValue value) {
    executionState.markValueAs(value, new CloseableState.Ignored(value.getTree()));
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
    Symbol.TypeSymbol typeSymbol = type.symbol();
    Type superClass = typeSymbol.superClass();
    if (superClass != null && (superClass.is("java.io.OutputStream") || superClass.is("java.io.InputStream"))) {
      return typeSymbol.lookupSymbols(CLOSE_METHOD_NAME).isEmpty();
    }
    return false;
  }

  @Override
  protected void onAssignment(ExecutionState executionState, Tree tree, Symbol variable, ExpressionTree expression) {
    // check first usage of closeables in order to manage use of same symbol
    ignoreClosableSymbols(executionState, expression);
    if ((tree.is(Tree.Kind.VARIABLE) || isVariableIdentifierOrMemberSelect((AssignmentExpressionTree) tree)) && isCloseableOrAutoCloseableSubtype(variable.type())) {
      executionState.markValueAs(variable, getCloseableStateFromExpression(executionState, variable, expression));
    }
  }

  private boolean isVariableIdentifierOrMemberSelect(AssignmentExpressionTree tree) {
    return tree.variable().is(Tree.Kind.IDENTIFIER, Tree.Kind.MEMBER_SELECT);
  }

  private State getCloseableStateFromExpression(ExecutionState executionState, Symbol symbol, @Nullable ExpressionTree expression) {
    if (shouldBeIgnored(executionState, symbol, expression)) {
      return new CloseableState.Ignored(expression);
    } else if (isNull(expression)) {
      return State.UNSET;
    } else if (expression.is(Tree.Kind.NEW_CLASS)) {
      if (usesIgnoredCloseableAsArgument(executionState, ((NewClassTree) expression).arguments())) {
        return new CloseableState.Ignored(expression);
      }
      return new CloseableState.Open(expression);
    }
    // TODO SONARJAVA-1029 : Engine currently ignore closeables which are retrieved from method calls. Handle them as OPEN.
    return new CloseableState.Ignored(expression);
  }

  private static boolean isNull(ExpressionTree expression) {
    return expression == null || expression.is(Tree.Kind.NULL_LITERAL);
  }

  private boolean shouldBeIgnored(ExecutionState executionState, Symbol symbol, @Nullable ExpressionTree expression) {
    return shouldBeIgnored(executionState, symbol) || shouldBeIgnored(expression);
  }

  private boolean shouldBeIgnored(ExecutionState executionState, Symbol symbol) {
    return isSymbolIgnored(executionState, symbol) || symbol.isFinal()
      || isIgnoredCloseableSubtype(symbol.type())
      || isSubclassOfInputStreamOrOutputStreamWithoutClose(symbol.type());
  }

  private static boolean shouldBeIgnored(@Nullable ExpressionTree expression) {
    return expression != null && (isSubclassOfInputStreamOrOutputStreamWithoutClose(expression.symbolType()) || isIgnoredCloseableSubtype(expression.symbolType()));
  }

  private boolean usesIgnoredCloseableAsArgument(ExecutionState executionState, List<ExpressionTree> arguments) {
    for (ExpressionTree argument : arguments) {
      if (isNewClassWithIgnoredArguments(executionState, argument)) {
        return true;
      } else if (isMethodInvocationWithIgnoredArguments(executionState, argument)) {
        return true;
      } else if (useIgnoredCloseable(executionState, argument) || isSubclassOfInputStreamOrOutputStreamWithoutClose(argument.symbolType())) {
        return true;
      }
    }
    return false;
  }

  private boolean isNewClassWithIgnoredArguments(ExecutionState executionState, ExpressionTree argument) {
    return argument.is(Tree.Kind.NEW_CLASS) && usesIgnoredCloseableAsArgument(executionState, ((NewClassTree) argument).arguments());
  }

  private boolean isMethodInvocationWithIgnoredArguments(ExecutionState executionState, ExpressionTree argument) {
    return argument.is(Tree.Kind.METHOD_INVOCATION) && usesIgnoredCloseableAsArgument(executionState, ((MethodInvocationTree) argument).arguments());
  }

  private boolean useIgnoredCloseable(ExecutionState executionState, ExpressionTree expression) {
    if (expression.is(Tree.Kind.IDENTIFIER, Tree.Kind.MEMBER_SELECT)) {
      IdentifierTree identifier;
      if (expression.is(Tree.Kind.MEMBER_SELECT)) {
        identifier = ((MemberSelectExpressionTree) expression).identifier();
      } else {
        identifier = (IdentifierTree) expression;
      }
      if (isIgnoredCloseable(executionState, identifier.symbol())) {
        return true;
      }
    }
    return false;
  }

  private boolean isIgnoredCloseable(ExecutionState executionState, Symbol symbol) {
    if (CloseableVisitor.isCloseableOrAutoCloseableSubtype(symbol.type()) && !symbol.owner().isMethodSymbol()) {
      return true;
    } else {
      return isSymbolIgnored(executionState, symbol);
    }
  }

  private boolean isSymbolIgnored(ExecutionState executionState, Symbol symbol) {
    List<State> statesOf = executionState.getStatesOf(symbol);
    for (State state : statesOf) {
      if ((state instanceof CloseableState) && ((CloseableState) state).isIgnored()) {
        return true;
      }
    }
    return false;

  }

  @Override
  protected void onTryResourceClosed(ExecutionState executionState, SymbolicValue resource) {
    ignoreValue(executionState, resource);
  }

  @Override
  protected void onValueReturned(ExecutionState executionState, ReturnStatementTree tree, ExpressionTree expression) {
    ignoreClosableSymbols(executionState, expression);
  }

  @Override
  protected void onExecutableElementInvocation(ExecutionState executionState, Tree tree, List<ExpressionTree> arguments) {
    if (tree.is(Tree.Kind.NEW_CLASS)) {
      ignoreClosableSymbols(executionState, ((NewClassTree) tree).arguments());
    } else {
      MethodInvocationTree methodInvocation = (MethodInvocationTree) tree;
      if (CLOSE_INVOCATIONS.anyMatch(methodInvocation)) {
        ExpressionTree methodSelect = methodInvocation.methodSelect();
        if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
          ExpressionTree expression = ((MemberSelectExpressionTree) methodSelect).expression();
          if (expression.is(Tree.Kind.IDENTIFIER)) {
            executionState.markValueAs(((IdentifierTree) expression).symbol(), new CloseableState.Closed(expression));
          }
        }
      } else {
        ignoreClosableSymbols(executionState, methodInvocation.arguments());
      }
    }
  }

  private void ignoreClosableSymbols(ExecutionState executionState, List<ExpressionTree> expressions) {
    for (ExpressionTree expression : expressions) {
      ignoreClosableSymbols(executionState, expression);
    }
  }

  private void ignoreClosableSymbols(ExecutionState executionState, @Nullable ExpressionTree expression) {
    if (expression != null) {
      if (expression.is(Tree.Kind.IDENTIFIER) && CloseableVisitor.isCloseableOrAutoCloseableSubtype(expression.symbolType())) {
        executionState.markValueAs(((IdentifierTree) expression).symbol(), new CloseableState.Ignored(expression));
      } else if (expression.is(Tree.Kind.MEMBER_SELECT)) {
        ignoreClosableSymbols(executionState, ((MemberSelectExpressionTree) expression).identifier());
      } else if (expression.is(Tree.Kind.TYPE_CAST)) {
        ignoreClosableSymbols(executionState, ((TypeCastTree) expression).expression());
      } else if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
        ignoreClosableSymbols(executionState, ((MethodInvocationTree) expression).arguments());
      } else if (expression.is(Tree.Kind.NEW_CLASS)) {
        ignoreClosableSymbols(executionState, ((NewClassTree) expression).arguments());
      }
    }
  }

  private final Set<Tree> issueTree = new HashSet<>();

  @Override
  protected void onValueUnreachable(ExecutionState executionState, State state) {
    if (state instanceof CloseableState.Open) {
      issueTree.addAll(state.reportingTrees());
    }
  }

  public Set<Tree> getIssueTrees() {
    return issueTree;
  }

}
