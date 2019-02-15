/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.MethodMatcherCollection;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.java.model.ExpressionUtils.skipParentheses;

@Rule(key = "S1166")
public class CatchUsesExceptionWithContextCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final MethodMatcherCollection GET_MESSAGE_METHODS = MethodMatcherCollection.create(
    MethodMatcher.create().typeDefinition("java.lang.Throwable").name("getMessage").withoutParameter(),
    MethodMatcher.create().typeDefinition("java.lang.Throwable").name("getLocalizedMessage").withoutParameter());

  private static final String JAVA_UTIL_LOGGING_LOGGER = "java.util.logging.Logger";
  private static final String SLF4J_LOGGER = "org.slf4j.Logger";

  private static final MethodMatcher JAVA_UTIL_LOG_METHOD = MethodMatcher.create().typeDefinition(JAVA_UTIL_LOGGING_LOGGER).name("log").withAnyParameters();
  private static final MethodMatcher JAVA_UTIL_LOGP_METHOD = MethodMatcher.create().typeDefinition(JAVA_UTIL_LOGGING_LOGGER).name("logp").withAnyParameters();
  private static final MethodMatcher JAVA_UTIL_LOGRB_METHOD = MethodMatcher.create().typeDefinition(JAVA_UTIL_LOGGING_LOGGER).name("logrb").withAnyParameters();

  private static final MethodMatcherCollection LOGGING_METHODS = MethodMatcherCollection.create(
    MethodMatcher.create().typeDefinition(JAVA_UTIL_LOGGING_LOGGER).name("config").withAnyParameters(),
    MethodMatcher.create().typeDefinition(JAVA_UTIL_LOGGING_LOGGER).name("fine").withAnyParameters(),
    MethodMatcher.create().typeDefinition(JAVA_UTIL_LOGGING_LOGGER).name("finer").withAnyParameters(),
    MethodMatcher.create().typeDefinition(JAVA_UTIL_LOGGING_LOGGER).name("finest").withAnyParameters(),
    MethodMatcher.create().typeDefinition(JAVA_UTIL_LOGGING_LOGGER).name("info").withAnyParameters(),
    MethodMatcher.create().typeDefinition(JAVA_UTIL_LOGGING_LOGGER).name("severe").withAnyParameters(),
    MethodMatcher.create().typeDefinition(JAVA_UTIL_LOGGING_LOGGER).name("warning").withAnyParameters(),
    JAVA_UTIL_LOG_METHOD,
    JAVA_UTIL_LOGP_METHOD,
    JAVA_UTIL_LOGRB_METHOD,
    MethodMatcher.create().typeDefinition(SLF4J_LOGGER).name("debug").withAnyParameters(),
    MethodMatcher.create().typeDefinition(SLF4J_LOGGER).name("error").withAnyParameters(),
    MethodMatcher.create().typeDefinition(SLF4J_LOGGER).name("info").withAnyParameters(),
    MethodMatcher.create().typeDefinition(SLF4J_LOGGER).name("trace").withAnyParameters(),
    MethodMatcher.create().typeDefinition(SLF4J_LOGGER).name("warn").withAnyParameters());

  private static final String EXCLUDED_EXCEPTION_TYPE = "java.lang.InterruptedException, " +
      "java.lang.NumberFormatException, " +
      "java.lang.NoSuchMethodException, " +
      "java.text.ParseException, " +
      "java.net.MalformedURLException, " +
      "java.time.format.DateTimeParseException";

  @RuleProperty(
      key = "exceptions",
      description = "List of exceptions which should not be checked",
      defaultValue = "" + EXCLUDED_EXCEPTION_TYPE)
  public String exceptionsCommaSeparated = EXCLUDED_EXCEPTION_TYPE;

  private JavaFileScannerContext context;
  private Deque<UsageStatus> usageStatusStack;
  private List<String> exceptions;
  private List<String> exceptionIdentifiers;
  private Set<CatchTree> excludedCatchTrees = new HashSet<>();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    usageStatusStack = new ArrayDeque<>();
    if (context.getSemanticModel() != null) {
      scan(context.getTree());
    }
    excludedCatchTrees.clear();
  }

  @Override
  public void visitTryStatement(TryStatementTree tree) {
    if (containsEnumValueOf(tree.block())) {
      tree.catches().stream()
        .filter(c -> c.parameter().symbol().type().is("java.lang.IllegalArgumentException"))
        .findAny()
        .ifPresent(excludedCatchTrees::add);
    }
    super.visitTryStatement(tree);
  }

  private static boolean containsEnumValueOf(Tree tree) {
    EnumValueOfVisitor visitor = new EnumValueOfVisitor();
    tree.accept(visitor);
    return visitor.hasEnumValueOf;
  }

  private static class EnumValueOfVisitor extends BaseTreeVisitor {
    private static final MethodMatcher ENUM_VALUE_OF = MethodMatcher.create()
      .typeDefinition(TypeCriteria.subtypeOf("java.lang.Enum"))
      .name("valueOf")
      .withAnyParameters();
    private boolean hasEnumValueOf = false;

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (ENUM_VALUE_OF.matches(tree)) {
        hasEnumValueOf = true;
      }
      super.visitMethodInvocation(tree);
    }

    @Override
    public void visitClass(ClassTree tree) {
      // skip anonymous classes
    }

    @Override
    public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
      // skip lambdas
    }
  }

  @Override
  public void visitCatch(CatchTree tree) {
    if (!isExcludedType(tree.parameter().type()) && !excludedCatchTrees.contains(tree)) {
      Symbol exception = tree.parameter().symbol();
      usageStatusStack.addFirst(new UsageStatus(exception.usages()));
      super.visitCatch(tree);
      if (usageStatusStack.pop().isInvalid()) {
        context.reportIssue(this, tree.parameter(), "Either log or rethrow this exception.");
      }
    }
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree mit) {
    super.visitMethodInvocation(mit);
    if (LOGGING_METHODS.anyMatch(mit)) {
      usageStatusStack.forEach(usageStatus -> usageStatus.addLoggingMethodInvocation(mit));
    }
  }

  @Override
  public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
    final IdentifierTree identifier;
    ExpressionTree expression = tree.expression();
    if (expression.is(Kind.IDENTIFIER)) {
      identifier = (IdentifierTree) expression;
    } else if (expression.is(Kind.PARENTHESIZED_EXPRESSION) && ((ParenthesizedTree) expression).expression().is(Kind.IDENTIFIER)) {
      identifier = (IdentifierTree) ((ParenthesizedTree) expression).expression();
    } else {
      identifier = null;
    }

    if (!usageStatusStack.isEmpty() && identifier != null) {
      usageStatusStack.forEach(usageStatus -> usageStatus.addInvalidUsage(identifier));
    }
    super.visitMemberSelectExpression(tree);

  }

  private boolean isExcludedType(Tree tree) {
    return isUnqualifiedExcludedType(tree) ||
      isQualifiedExcludedType(tree);
  }

  private boolean isUnqualifiedExcludedType(Tree tree) {
    return tree.is(Kind.IDENTIFIER) &&
      getExceptionIdentifiers().contains(((IdentifierTree) tree).name());
  }

  private boolean isQualifiedExcludedType(Tree tree) {
    if (!tree.is(Kind.MEMBER_SELECT)) {
      return false;
    }
    return getExceptions().contains(ExpressionsHelper.concatenate((MemberSelectExpressionTree) tree));
  }

  private List<String> getExceptions() {
    if (exceptions == null) {
      exceptions = Stream.of(exceptionsCommaSeparated.split(",")).map(String::trim).collect(Collectors.toList());
    }
    return exceptions;
  }

  private List<String> getExceptionIdentifiers() {
    if (exceptionIdentifiers == null) {
      exceptionIdentifiers = getExceptions().stream()
        .map(exception -> exception.substring(exception.lastIndexOf('.') + 1))
        .collect(Collectors.toList());
    }
    return exceptionIdentifiers;
  }

  private static class UsageStatus {
    private final Collection<IdentifierTree> validUsages;
    private final Set<MethodInvocationTree> loggingMethodInvocations;

    UsageStatus(Collection<IdentifierTree> usages) {
      validUsages = new ArrayList<>(usages);
      loggingMethodInvocations = new HashSet<>();
    }

    public void addInvalidUsage(IdentifierTree exceptionIdentifier) {
      validUsages.remove(exceptionIdentifier);
    }

    public void addLoggingMethodInvocation(MethodInvocationTree mit) {
      loggingMethodInvocations.add(mit);
    }

    public boolean isInvalid() {
      return validUsages.isEmpty() && !isMessageLoggedWithAdditionalContext();
    }

    private boolean isMessageLoggedWithAdditionalContext() {
      return loggingMethodInvocations.stream().anyMatch(mit -> hasGetMessageInvocation(mit) && hasDynamicExceptionMessageUsage(mit));
    }

    private static boolean hasGetMessageInvocation(MethodInvocationTree mit) {
      return hasGetMessageMethodInvocation(mit) || isGetMessageReferencedByIdentifiers(mit);
    }

    private static boolean isGetMessageReferencedByIdentifiers(MethodInvocationTree mit) {
      ChildrenIdentifierCollector visitor = new ChildrenIdentifierCollector();
      mit.accept(visitor);

      boolean invocationInInitializer = visitor.identifiersChildren.stream()
        .map(UsageStatus::getVariableInitializer)
        .distinct()
        .anyMatch(UsageStatus::hasGetMessageMethodInvocation);

      return invocationInInitializer || visitor.identifiersChildren.stream()
        .map(IdentifierTree::symbol)
        .map(Symbol::usages)
        .flatMap(usagesList -> getAssignments(usagesList).stream())
        .anyMatch(assignment -> hasGetMessageMethodInvocation(assignment.expression()));
    }

    private static boolean hasGetMessageMethodInvocation(@Nullable Tree tree) {
      if (tree == null) {
        return false;
      }
      GetExceptionMessageVisitor visitor = new GetExceptionMessageVisitor();
      tree.accept(visitor);
      return visitor.hasGetMessageCall;
    }

    private static boolean hasDynamicExceptionMessageUsage(MethodInvocationTree mit) {
      Arguments arguments = mit.arguments();
      int argumentsCount = arguments.size();
      ExpressionTree firstArg = arguments.get(0);
      ExpressionTree argumentToCheck;
      if (mit.symbol().owner().type().is(SLF4J_LOGGER)) {
        if (argumentsCount == 1) {
          argumentToCheck = firstArg;
        } else if (argumentsCount == 2 && firstArg.symbolType().is("org.slf4j.Marker")) {
          argumentToCheck = arguments.get(1);
        } else {
          argumentToCheck = null;
        }
      } else if (JAVA_UTIL_LOG_METHOD.matches(mit) && argumentsCount == 2) {
        argumentToCheck = arguments.get(1);
      } else if (JAVA_UTIL_LOGP_METHOD.matches(mit) && argumentsCount == 4) {
        argumentToCheck = arguments.get(3);
      } else if (JAVA_UTIL_LOGRB_METHOD.matches(mit) && argumentsCount == 5) {
        argumentToCheck = arguments.get(4);
      } else {
        argumentToCheck = firstArg;
      }

      return argumentToCheck == null || !isSimpleExceptionMessage(argumentToCheck);
    }

    private static boolean isSimpleExceptionMessage(ExpressionTree expressionTree) {
      ExpressionTree innerExpression = skipParentheses(expressionTree);
      if (innerExpression.is(Kind.IDENTIFIER)) {
        IdentifierTree variable = (IdentifierTree) innerExpression;
        List<AssignmentExpressionTree> assignments = getAssignments(variable.symbol().usages());
        ExpressionTree initializer = getVariableInitializer(variable);
        return assignments.isEmpty() && initializer != null && isSimpleExceptionMessage(initializer);
      } else if (innerExpression.is(Kind.METHOD_INVOCATION)) {
        return GET_MESSAGE_METHODS.anyMatch(((MethodInvocationTree) innerExpression));
      }
      return false;
    }

    private static List<AssignmentExpressionTree> getAssignments(List<IdentifierTree> usages) {
      return usages.stream().map(UsageStatus::getAssignmentToIdentifier).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @CheckForNull
    private static AssignmentExpressionTree getAssignmentToIdentifier(IdentifierTree usage) {
      Tree parent = usage.parent();
      while (parent != null) {
        if (parent.is(Kind.ASSIGNMENT, Kind.PLUS_ASSIGNMENT)) {
          AssignmentExpressionTree assignmentExpressionTree = (AssignmentExpressionTree) parent;
          if (assignmentExpressionTree.variable().equals(usage)) {
            return assignmentExpressionTree;
          } else {
            return null;
          }
        }
        parent = parent.parent();
      }

      return null;
    }

    @CheckForNull
    private static ExpressionTree getVariableInitializer(IdentifierTree variable) {
      Tree declaration = variable.symbol().declaration();
      if (declaration != null && declaration.is(Kind.VARIABLE)) {
        return ((VariableTree) declaration).initializer();
      }
      return null;
    }
  }

  private static class GetExceptionMessageVisitor extends BaseTreeVisitor {
    boolean hasGetMessageCall = false;

    @Override
    public void visitMethodInvocation(MethodInvocationTree mit) {
      if (!hasGetMessageCall && GET_MESSAGE_METHODS.anyMatch(mit)) {
        hasGetMessageCall = true;
      }
      super.visitMethodInvocation(mit);
    }

    @Override
    public void visitNewClass(NewClassTree newClassTree) {
      // skip method invocations found in a NewClassTree
    }
  }

  private static class ChildrenIdentifierCollector extends BaseTreeVisitor {
    Set<IdentifierTree> identifiersChildren = new HashSet<>();

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      identifiersChildren.add(tree);
    }
  }

}
