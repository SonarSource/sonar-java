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

import org.sonar.java.checks.methods.MethodInvocationMatcher;
import org.sonar.java.checks.methods.MethodInvocationMatcherCollection;
import org.sonar.java.checks.methods.TypeCriteria;
import org.sonar.java.symexecengine.DataFlowVisitor;
import org.sonar.java.symexecengine.State;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.Nullable;
import java.util.List;

public class CloseableVisitor extends DataFlowVisitor {

  private static final MethodInvocationMatcherCollection CLOSE_INVOCATIONS = closeMethodInvocationMatcher();

  private static final String[] IGNORED_CLOSEABLE_SUBTYPES = {
    "java.io.ByteArrayOutputStream",
    "java.io.ByteArrayInputStream",
    "java.io.StringReader",
    "java.io.StringWriter",
    "java.io.CharArrayReader",
    "java.io.CharArrayWriter"
  };

  private static final String CLOSE_METHOD_NAME = "close";
  private static final String JAVA_IO_CLOSEABLE = "java.io.Closeable";
  private static final String JAVA_LANG_AUTOCLOSEABLE = "java.lang.AutoCloseable";

  public CloseableVisitor(List<VariableTree> methodParameters) {
    ignoreVariables(methodParameters);
  }

  private void ignoreVariables(List<VariableTree> variables) {
    for (VariableTree methodParameter : variables) {
      super.visitVariable(methodParameter);
      ignoreVariable(methodParameter);
    }
  }

  private void ignoreVariable(VariableTree variableTree) {
    executionState.markValueAs(variableTree.symbol(), new CloseableState.Ignored(variableTree));
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
        .withNoParameterConstraint(),
      MethodInvocationMatcher.create()
        .typeDefinition(TypeCriteria.subtypeOf("org.springframework.context.support.AbstractApplicationContext"))
        .name("registerShutdownHook")
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
    Symbol.TypeSymbol typeSymbol = type.symbol();
    Type superClass = typeSymbol.superClass();
    if (superClass != null && (superClass.is("java.io.OutputStream") || superClass.is("java.io.InputStream"))) {
      return typeSymbol.lookupSymbols(CLOSE_METHOD_NAME).isEmpty();
    }
    return false;
  }

  @Override
  protected boolean isSymbolRelevant(Symbol symbol) {
    return isCloseableOrAutoCloseableSubtype(symbol.type());
  }

  @Override
  public void visitVariable(VariableTree tree) {
    super.visitVariable(tree);
    ExpressionTree initializer = tree.initializer();

    // check first usage of closeables in order to manage use of same symbol
    ignoreClosableSymbols(initializer);

    Symbol symbol = tree.symbol();
    if (isCloseableOrAutoCloseableSubtype(symbol.type())) {
      executionState.markValueAs(symbol, getCloseableStateFromExpression(symbol, initializer));
    }
  }

  private State getCloseableStateFromExpression(Symbol symbol, @Nullable ExpressionTree expression) {
    if (shouldBeIgnored(symbol, expression)) {
      return new CloseableState.Ignored(expression);
    } else if (isNull(expression)) {
      return State.UNSET;
    } else if (expression.is(Tree.Kind.NEW_CLASS)) {
      if (usesIgnoredCloseableAsArgument(((NewClassTree) expression).arguments())) {
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

  private boolean shouldBeIgnored(Symbol symbol, @Nullable ExpressionTree expression) {
    return shouldBeIgnored(symbol) || shouldBeIgnored(expression);
  }

  private boolean shouldBeIgnored(Symbol symbol) {
    return isSymbolIgnored(symbol) || symbol.isFinal()
      || isIgnoredCloseableSubtype(symbol.type())
      || isSubclassOfInputStreamOrOutputStreamWithoutClose(symbol.type());
  }

  private static boolean shouldBeIgnored(@Nullable ExpressionTree expression) {
    return expression != null && (isSubclassOfInputStreamOrOutputStreamWithoutClose(expression.symbolType()) || isIgnoredCloseableSubtype(expression.symbolType()));
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
    if (CloseableVisitor.isCloseableOrAutoCloseableSubtype(symbol.type()) && !symbol.owner().isMethodSymbol()) {
      return true;
    } else {
      return isSymbolIgnored(symbol);
    }
  }

  private boolean isSymbolIgnored(Symbol symbol) {
    List<State> statesOf = executionState.getStatesOf(symbol);
    for (State state : statesOf) {
      if ((state instanceof CloseableState) && ((CloseableState) state).isIgnored()) {
        return true;
      }
    }
    return false;

  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    super.visitAssignmentExpression(tree);
    ExpressionTree variable = tree.variable();
    if (variable.is(Tree.Kind.IDENTIFIER, Tree.Kind.MEMBER_SELECT)) {
      ExpressionTree expression = tree.expression();

      // check first usage of closeables in order to manage use of same symbol
      ignoreClosableSymbols(expression);

      IdentifierTree identifier;
      if (variable.is(Tree.Kind.IDENTIFIER)) {
        identifier = (IdentifierTree) variable;
      } else {
        identifier = ((MemberSelectExpressionTree) variable).identifier();
      }
      Symbol symbol = identifier.symbol();
      if (isCloseableOrAutoCloseableSubtype(identifier.symbolType()) && symbol.owner().isMethodSymbol()) {
        executionState.markValueAs(symbol, getCloseableStateFromExpression(symbol, expression));
      }
    }
  }

  @Override
  protected void handleResources(List<VariableTree> resources) {
    for (VariableTree resource : resources) {
      ignoreVariable(resource);
    }
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    ignoreClosableSymbols(tree.arguments());
  }

  @Override
  public void visitReturnStatement(ReturnStatementTree tree) {
    ignoreClosableSymbols(tree.expression());
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    if (CLOSE_INVOCATIONS.anyMatch(tree)) {
      ExpressionTree methodSelect = tree.methodSelect();
      if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
        ExpressionTree expression = ((MemberSelectExpressionTree) methodSelect).expression();
        if (expression.is(Tree.Kind.IDENTIFIER)) {
          executionState.markValueAs(((IdentifierTree) expression).symbol(), new CloseableState.Closed(expression));
        }
      }
    } else {
      ignoreClosableSymbols(tree.arguments());
    }
  }

  private void ignoreClosableSymbols(List<ExpressionTree> expressions) {
    for (ExpressionTree expression : expressions) {
      ignoreClosableSymbols(expression);
    }
  }

  private void ignoreClosableSymbols(@Nullable ExpressionTree expression) {
    if (expression != null) {
      if (expression.is(Tree.Kind.IDENTIFIER) && CloseableVisitor.isCloseableOrAutoCloseableSubtype(expression.symbolType())) {
        executionState.markValueAs(((IdentifierTree) expression).symbol(), new CloseableState.Ignored(expression));
      } else if (expression.is(Tree.Kind.MEMBER_SELECT)) {
        ignoreClosableSymbols(((MemberSelectExpressionTree) expression).identifier());
      } else if (expression.is(Tree.Kind.TYPE_CAST)) {
        ignoreClosableSymbols(((TypeCastTree) expression).expression());
      } else if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
        ignoreClosableSymbols(((MethodInvocationTree) expression).arguments());
      } else if (expression.is(Tree.Kind.NEW_CLASS)) {
        ignoreClosableSymbols(((NewClassTree) expression).arguments());
      }
    }
  }

}
