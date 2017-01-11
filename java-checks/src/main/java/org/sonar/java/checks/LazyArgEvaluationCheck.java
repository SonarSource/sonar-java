/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.MethodMatcherCollection;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.sonar.java.matcher.TypeCriteria.anyType;

@Rule(key = "S2629")
public class LazyArgEvaluationCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final String SLF4J_LOGGER = "org.slf4j.Logger";
  public static final String JUL_LOGGER = "java.util.logging.Logger";
  private static final TypeCriteria STRING = TypeCriteria.is("java.lang.String");
  public static final TypeCriteria OBJECT_ARR = TypeCriteria.is("java.lang.Object[]");

  interface LogLevels {
    List<MethodMatcher> log();

    MethodMatcher test();

    static Stream<LogLevels> logLevels() {
      return Stream.concat(Arrays.stream(SLF4J_LEVELS.values()), Arrays.stream(JUL_LEVELS.values()));
    }

    enum SLF4J_LEVELS implements LogLevels {
      TRACE,
      DEBUG,
      INFO,
      WARN,
      ERROR
      ;

      public static final TypeCriteria MARKER = TypeCriteria.is("org.slf4j.Marker");

      @Override
      public List<MethodMatcher> log() {
        return slf4jVariants(() -> MethodMatcher.create().typeDefinition(SLF4J_LOGGER).name(toString().toLowerCase(Locale.ROOT)));
      }

      @Override
      public MethodMatcher test() {
        return levelTestMatcher(SLF4J_LOGGER, toString());
      }

      private static List<MethodMatcher> slf4jVariants(Supplier<MethodMatcher> prototype) {
        return ImmutableList.of(
          prototype.get().parameters(STRING),
          prototype.get().parameters(STRING, anyType()),
          prototype.get().parameters(STRING, anyType(), anyType()),
          prototype.get().parameters(STRING, OBJECT_ARR),
          prototype.get().parameters(MARKER, STRING),
          prototype.get().parameters(MARKER, STRING, anyType()),
          prototype.get().parameters(MARKER, STRING, anyType(), anyType()),
          prototype.get().parameters(MARKER, STRING, OBJECT_ARR)
        );
      }

      private static MethodMatcher levelTestMatcher(String typeDefinition, String level) {
        return MethodMatcher.create()
          .typeDefinition(typeDefinition)
          .name(String.format("is%c%sEnabled", level.charAt(0), level.toLowerCase(Locale.ROOT).substring(1)))
          .withoutParameter();
      }
    }

    enum JUL_LEVELS implements LogLevels {
      SEVERE,
      WARNING,
      INFO,
      CONFIG,
      FINE,
      FINER,
      FINEST;

      @Override
      public List<MethodMatcher> log() {
        MethodMatcher matcher = MethodMatcher.create()
          .typeDefinition(JUL_LOGGER)
          .name(toString().toLowerCase(Locale.ROOT))
          .addParameter(STRING);
        return ImmutableList.of(matcher);
      }

      @Override
      public MethodMatcher test() {
        return MethodMatcher.create().typeDefinition(JUL_LOGGER).name("isLoggable").addParameter("java.util.logging.Level");
      }
    }
  }

  private static final MethodMatcher PRECONDITIONS = MethodMatcher.create()
    .typeDefinition("com.google.common.base.Preconditions")
    .name("checkState")
    .withAnyParameters();

  private static final MethodMatcher JUL_LOG = MethodMatcher.create()
    .typeDefinition(JUL_LOGGER)
    .name("log")
    .addParameter("java.util.logging.Level")
    .addParameter(STRING);

  private static final MethodMatcherCollection LAZY_ARG_METHODS = MethodMatcherCollection.create(PRECONDITIONS, JUL_LOG);

  static {
    LogLevels.logLevels().forEach(l -> LAZY_ARG_METHODS.addAll(l.log()));
  }

  private static final MethodMatcherCollection LOG_LEVEL_TESTS = MethodMatcherCollection.create();

  static {
    LogLevels.logLevels().forEach(l -> LOG_LEVEL_TESTS.add(l.test()));
  }

  private JavaFileScannerContext context;
  private Deque<Tree> treeStack = new ArrayDeque<>();

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
    if (LAZY_ARG_METHODS.anyMatch(tree) && !insideCatchStatement() && !insideLevelTest()) {
      onMethodInvocationFound(tree);
    }
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
  public void visitCatch(CatchTree tree) {
    stackAndContinue(tree, super::visitCatch);
  }

  @Override
  public void visitMethod(MethodTree tree) {
    // we put method trees on stack to be able to detect log statements in anonymous classes
    stackAndContinue(tree, super::visitMethod);
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
      .collect(Collectors.toList());
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
      .filter(arg -> arg.symbolType().is("java.lang.String"));
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
      String methodName = tree.symbol().name();
      return methodName != null && (methodName.startsWith("get") || methodName.startsWith("is"));
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
      super.visitBinaryExpression(tree);
    }
  }

  private static class LevelTestVisitor extends BaseTreeVisitor {
    boolean match = false;

    @Override
    public void visitMethodInvocation(MethodInvocationTree mit) {
      if (LOG_LEVEL_TESTS.anyMatch(mit)) {
        match = true;
      }
    }
  }

}
