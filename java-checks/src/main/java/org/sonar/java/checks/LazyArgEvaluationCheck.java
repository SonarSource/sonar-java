/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
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

import static org.sonar.plugins.java.api.semantic.MethodMatchers.ANY;

@Rule(key = "S2629")
public class LazyArgEvaluationCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final String STRING = "java.lang.String";
  private static final String OBJECT_ARR = "java.lang.Object[]";

  private static class SLF4J {

    private static final String[] METHOD_NAMES = {
      "trace",
      "debug",
      "info",
      "warn",
      "error"
    };

    private static final String LOGGER = "org.slf4j.Logger";
    private static final String MARKER = "org.slf4j.Marker";

    private static final MethodMatchers LOG = MethodMatchers.create()
      .ofSubTypes(LOGGER)
      .names(METHOD_NAMES)
      .addParametersMatcher(STRING)
      .addParametersMatcher(STRING, ANY)
      .addParametersMatcher(STRING, ANY, ANY)
      .addParametersMatcher(STRING, OBJECT_ARR)
      .addParametersMatcher(MARKER, STRING)
      .addParametersMatcher(MARKER, STRING, ANY)
      .addParametersMatcher(MARKER, STRING, ANY, ANY)
      .addParametersMatcher(MARKER, STRING, OBJECT_ARR)
      .build();

    private static final MethodMatchers TEST = MethodMatchers.create()
      .ofSubTypes(LOGGER)
      .names(testMethodNames(METHOD_NAMES))
      .addWithoutParametersMatcher()
      .addParametersMatcher(MARKER)
      .build();
  }

  private static class JUL {

    private static final String[] METHOD_NAMES = {
      "severe",
      "warning",
      "info",
      "config",
      "fine",
      "finer",
      "finest"
    };

    private static final String LOGGER = "java.util.logging.Logger";

    private static final MethodMatchers LOG = MethodMatchers.or(
      MethodMatchers.create()
        .ofTypes(LOGGER)
        .names(METHOD_NAMES)
        .addParametersMatcher(STRING)
        .build(),
      MethodMatchers.create()
        .ofTypes(LOGGER)
        .names("log")
        .addParametersMatcher("java.util.logging.Level", STRING)
        .build());

    private static final MethodMatchers TEST = MethodMatchers.create()
      .ofTypes(LOGGER)
      .names("isLoggable")
      .addParametersMatcher("java.util.logging.Level")
      .build();
  }

  private static class LOG4J {

    private static final String[] METHOD_NAMES = {
      "debug",
      "error",
      "fatal",
      "info",
      "trace",
      "warn"
    };

    private static final String LEVEL = "org.apache.logging.log4j.Level";
    private static final String LOGGER = "org.apache.logging.log4j.Logger";
    private static final String MARKER = "org.apache.logging.log4j.Marker";
    private static final Predicate<Type> SUPPLIER = type -> type.isSubtypeOf("org.apache.logging.log4j.util.Supplier") ||
      type.isSubtypeOf("org.apache.logging.log4j.util.MessageSupplier");

    private static final MethodMatchers LOG = MethodMatchers.or(
      MethodMatchers.create()
        .ofSubTypes(LOGGER)
        .names(METHOD_NAMES)
        .withAnyParameters()
        .build(),
      MethodMatchers.create()
        .ofSubTypes(LOGGER)
        .names("log")
        .withAnyParameters()
        .build());

    private static final MethodMatchers TEST = MethodMatchers.or(
      MethodMatchers.create()
        .ofSubTypes(LOGGER)
        .names(testMethodNames(METHOD_NAMES))
        .withAnyParameters()
        .build(),
      MethodMatchers.create()
        .ofSubTypes(LOGGER)
        .names("isEnabled")
        .addParametersMatcher(LEVEL)
        .addParametersMatcher(LEVEL, MARKER)
        .build());
  }

  private static final MethodMatchers PRECONDITIONS = MethodMatchers.create()
    .ofTypes("com.google.common.base.Preconditions")
    .names("checkState")
    .withAnyParameters()
    .build();

  private static final MethodMatchers LAZY_ARG_METHODS = MethodMatchers.or(
    PRECONDITIONS,
    SLF4J.LOG,
    JUL.LOG,
    LOG4J.LOG);

  private static final MethodMatchers LOG_LEVEL_TESTS = MethodMatchers.or(
    SLF4J.TEST,
    JUL.TEST,
    LOG4J.TEST);

  private static String[] testMethodNames(String[] lowerCaseNames) {
    return Stream.of(lowerCaseNames)
      .map(name -> "is" + Character.toUpperCase(name.charAt(0)) + name.substring(1) + "Enabled")
      .toArray(String[]::new);
  }

  private JavaFileScannerContext context;
  private Deque<Tree> treeStack = new ArrayDeque<>();
  private boolean foundReturnStatementInsideLevelTest = false;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    if (context.getSemanticModel() == null) {
      return;
    }
    scan(context.getTree());
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    if (LAZY_ARG_METHODS.matches(tree) &&
      !insideCatchStatement() &&
      !insideLevelTest() &&
      !foundReturnStatementInsideLevelTest &&
      !argsUsingSuppliers(tree)) {
      onMethodInvocationFound(tree);
    }
  }

  private static boolean argsUsingSuppliers(MethodInvocationTree tree) {
    return tree.arguments().stream().map(ExpressionTree::symbolType).anyMatch(LOG4J.SUPPLIER);
  }

  @Override
  public void visitIfStatement(IfStatementTree ifTree) {
    LevelTestVisitor levelTestVisitor = new LevelTestVisitor();
    ifTree.condition().accept(levelTestVisitor);
    if (levelTestVisitor.match) {
      stackAndContinue(ifTree, super::visitIfStatement);
    } else {
      super.visitIfStatement(ifTree);
    }
  }

  @Override
  public void visitReturnStatement(ReturnStatementTree tree) {
    if (insideLevelTest()) {
      // record when a return is detected within a log test
      foundReturnStatementInsideLevelTest = true;
    }
  }

  @Override
  public void visitCatch(CatchTree tree) {
    stackAndContinue(tree, super::visitCatch);
  }

  @Override
  public void visitMethod(MethodTree tree) {
    // we put method trees on stack to be able to detect log statements in anonymous classes
    stackAndContinue(tree, super::visitMethod);
    // once a method is exited, then this field cannot be true (if it was set to true)
    foundReturnStatementInsideLevelTest = false;
  }

  private boolean insideLevelTest() {
    return treeStack.stream().anyMatch(t -> t.is(Tree.Kind.IF_STATEMENT));
  }

  private boolean insideCatchStatement() {
    return treeStack.peek() != null && treeStack.peek().is(Tree.Kind.CATCH);
  }

  private <T extends Tree> void stackAndContinue(T tree, Consumer<T> visit) {
    treeStack.push(tree);
    visit.accept(tree);
    treeStack.pop();
  }

  private void onMethodInvocationFound(MethodInvocationTree mit) {
    List<JavaFileScannerContext.Location> flow = findStringArg(mit)
      .flatMap(LazyArgEvaluationCheck::checkArgument)
      .toList();
    if (!flow.isEmpty()) {
      context.reportIssue(this, flow.get(0).syntaxNode, flow.get(0).msg, flow.subList(1, flow.size()), null);
    }
  }

  private static Stream<JavaFileScannerContext.Location> checkArgument(ExpressionTree stringArgument) {
    StringExpressionVisitor visitor = new StringExpressionVisitor();
    stringArgument.accept(visitor);
    if (visitor.shouldReport) {
      return Stream.of(locationFromArg(stringArgument, visitor));
    } else {
      return Stream.empty();
    }
  }

  private static JavaFileScannerContext.Location locationFromArg(ExpressionTree stringArgument, StringExpressionVisitor visitor) {
    StringBuilder msg = new StringBuilder();
    if (visitor.hasMethodInvocation) {
      msg.append("Invoke method(s) only conditionally. ");
    }
    if (visitor.hasBinaryExpression) {
      msg.append("Use the built-in formatting to construct this argument.");
    }
    return new JavaFileScannerContext.Location(msg.toString(), stringArgument);
  }

  private static Stream<ExpressionTree> findStringArg(MethodInvocationTree mit) {
    return mit.arguments().stream()
      .filter(arg -> arg.symbolType().is(STRING));
  }

  private static class StringExpressionVisitor extends BaseTreeVisitor {

    private boolean hasBinaryExpression;
    private boolean shouldReport;
    private boolean hasMethodInvocation;

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (!isGetter(tree)) {
        shouldReport = true;
        hasMethodInvocation = true;
      }
    }

    private static boolean isGetter(MethodInvocationTree tree) {
      if (isAnnotationMethod(tree)) {
        return true;
      }

      String methodName = tree.methodSymbol().name();
      return tree.methodSymbol().parameterTypes().isEmpty()
        && (methodName.startsWith("get") || methodName.startsWith("is") || isGetterMatchingFieldNameAndType(tree.methodSymbol()));
    }

    private static boolean isGetterMatchingFieldNameAndType(Symbol.MethodSymbol methodSymbol) {
      Symbol owner = methodSymbol.owner();
      Type getterReturnType = methodSymbol.returnType().type();
      // methodSymbol's owner is always not null and TypeSymbol
      return ((Symbol.TypeSymbol) owner).memberSymbols()
        .stream()
        .filter(symbol -> symbol.isVariableSymbol() && symbol.type().equals(getterReturnType))
        .anyMatch(symbol -> symbol.name().equals(methodSymbol.name()));
    }

    private static boolean isAnnotationMethod(MethodInvocationTree tree) {
      Symbol owner = tree.methodSymbol().owner();
      return owner.isTypeSymbol() && ((Symbol.TypeSymbol) owner).isAnnotation();
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      if (hasBinaryExpression) {
        shouldReport = true;
      }
    }

    @Override
    public void visitNewClass(NewClassTree tree) {
      hasMethodInvocation = true;
      shouldReport = true;
    }

    @Override
    public void visitBinaryExpression(BinaryExpressionTree tree) {
      hasBinaryExpression = true;
      if (!isConstant(tree.rightOperand())) {
        tree.rightOperand().accept(this);
      }
      if (!isConstant(tree.leftOperand())) {
        tree.leftOperand().accept(this);
      }
    }

    private static boolean isConstant(ExpressionTree operand) {
      switch (operand.kind()) {
        case BOOLEAN_LITERAL,
          CHAR_LITERAL,
          DOUBLE_LITERAL,
          FLOAT_LITERAL,
          INT_LITERAL,
          LONG_LITERAL,
          STRING_LITERAL,
          NULL_LITERAL:
          return true;
        case IDENTIFIER:
          return isConstant(((IdentifierTree) operand).symbol());
        case MEMBER_SELECT:
          MemberSelectExpressionTree mset = (MemberSelectExpressionTree) operand;
          return isConstant(mset.identifier().symbol());
        default:
          return false;
      }
    }

    private static boolean isConstant(Symbol symbol) {
      return symbol.isStatic() && symbol.isFinal();
    }
  }

  private static class LevelTestVisitor extends BaseTreeVisitor {
    boolean match = false;

    @Override
    public void visitMethodInvocation(MethodInvocationTree mit) {
      if (LOG_LEVEL_TESTS.matches(mit)) {
        match = true;
      }
    }
  }

}
