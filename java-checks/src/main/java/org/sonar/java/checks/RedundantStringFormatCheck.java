/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import java.util.List;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.LoggingMatchers;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S9413")
public class RedundantStringFormatCheck extends IssuableSubscriptionVisitor {

  private static final String STRING = "java.lang.String";

  private static final MethodMatchers STRING_FORMAT = MethodMatchers.create()
    .ofTypes(STRING)
    .names("format")
    .withAnyParameters()
    .build();

  private static final MethodMatchers PRINT_METHODS = MethodMatchers.create()
    .ofSubTypes("java.io.PrintStream", "java.io.PrintWriter")
    .names("print", "println")
    .addParametersMatcher(STRING)
    .build();

  private static final MethodMatchers APPEND_METHODS = MethodMatchers.create()
    .ofTypes("java.lang.StringBuilder", "java.lang.StringBuffer")
    .names("append")
    .addParametersMatcher(STRING)
    .build();

  private static final MethodMatchers LOG_METHODS = MethodMatchers.or(LoggingMatchers.SLF4J_LOG_METHODS, LoggingMatchers.LOG4J_LOG_METHODS);

  private static final String PRINTF_MESSAGE = "Use \"printf\" instead of \"String.format\".";
  private static final String LOGGING_MESSAGE = "Use the logger's built-in \"{}\" formatting instead of \"String.format\".";
  private static final String APPEND_MESSAGE = "Use chained \"append\" calls instead of \"String.format\".";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree formatCall = (MethodInvocationTree) tree;
    if (!STRING_FORMAT.matches(formatCall)) {
      return;
    }
    Tree argument = formatCall;
    while (argument.parent().is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
      argument = argument.parent();
    }
    Tree arguments = argument.parent();
    if (!arguments.is(Tree.Kind.ARGUMENTS)) {
      return;
    }
    String message = targetMessage(arguments.parent(), argument);
    if (message != null && (PRINTF_MESSAGE.equals(message) || hasSimpleLiteralFormat(formatCall))) {
      reportIssue(formatCall.methodSelect(), message);
    }
  }

  @Nullable
  private static String targetMessage(Tree call, Tree argument) {
    if (!(call instanceof MethodInvocationTree mit) || !isMessageArgument(mit.methodSymbol(), mit.arguments().indexOf(argument))) {
      return null;
    }
    if (PRINT_METHODS.matches(mit)) {
      return PRINTF_MESSAGE;
    }
    if (LOG_METHODS.matches(mit)) {
      return LOGGING_MESSAGE;
    }
    if (APPEND_METHODS.matches(mit)) {
      return APPEND_MESSAGE;
    }
    return null;
  }

  /**
   * An argument is the message argument when its declared parameter is the first parameter of type {@code String}.
   */
  private static boolean isMessageArgument(Symbol.MethodSymbol symbol, int index) {
    if (symbol.isUnknown()) {
      return false;
    }
    List<Type> parameterTypes = symbol.parameterTypes();
    if (index < 0 || index >= parameterTypes.size() || !parameterTypes.get(index).is(STRING)) {
      return false;
    }
    return parameterTypes.subList(0, index).stream().noneMatch(type -> type.is(STRING));
  }

  private static boolean hasSimpleLiteralFormat(MethodInvocationTree formatCall) {
    List<ExpressionTree> arguments = formatCall.arguments();
    if (arguments.isEmpty() || !arguments.get(0).is(Tree.Kind.STRING_LITERAL)) {
      return false;
    }
    return hasOnlySimpleConversions(LiteralUtils.trimQuotes(((LiteralTree) arguments.get(0)).value()));
  }

  /**
   * Returns true when every {@code %} sequence of the format string is {@code %s}, {@code %d} or {@code %%}.
   */
  private static boolean hasOnlySimpleConversions(String format) {
    int index = format.indexOf('%');
    while (index >= 0) {
      if (index + 1 >= format.length() || "sd%".indexOf(format.charAt(index + 1)) < 0) {
        return false;
      }
      index = format.indexOf('%', index + 2);
    }
    return true;
  }
}
