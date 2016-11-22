/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Rule(key = "S2629")
public class LazyArgEvaluationCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final MethodMatcher PRECONDITIONS = MethodMatcher.create()
    .typeDefinition("com.google.common.base.Preconditions")
    .name("checkState")
    .withAnyParameters();

  private static final MethodMatcher LOG = MethodMatcher.create()
    .typeDefinition("java.util.logging.Logger")
    .name("log")
    .addParameter(TypeCriteria.anyType())
    .addParameter(String.class.getCanonicalName());

  private static final MethodMatcher TRACE = slf4jPrototype().name("trace");
  private static final MethodMatcher DEBUG = slf4jPrototype().name("debug");
  private static final MethodMatcher INFO = slf4jPrototype().name("info");
  private static final MethodMatcher WARN = slf4jPrototype().name("warn");
  private static final MethodMatcher ERROR = slf4jPrototype().name("error");

  private static final MethodMatcherCollection LAZY_ARG_METHODS = MethodMatcherCollection.create(PRECONDITIONS, LOG, TRACE, DEBUG, INFO, WARN, ERROR);

  private JavaFileScannerContext context;

  private static MethodMatcher slf4jPrototype() {
    return MethodMatcher.create()
      .typeDefinition("org.slf4j.Logger")
      .addParameter(String.class.getCanonicalName())
      .addParameter(TypeCriteria.anyType());
  }

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
    if (LAZY_ARG_METHODS.anyMatch(tree)) {
      onMethodInvocationFound(tree);
    }
  }

  @Override
  public void visitCatch(CatchTree tree) {
    // cut the visit on catch blocks, because we don't mind some performance loss on exceptional paths
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
      .filter(arg -> arg.symbolType().is(String.class.getCanonicalName()));
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
      return methodName.startsWith("get") || methodName.startsWith("is");
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

}
