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
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.LoggingMatchers;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
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

  private static final MethodMatchers LOG_METHODS = MethodMatchers.or(LoggingMatchers.SLF4J_LOG_METHODS, LoggingMatchers.LOG4J_LOG_METHODS);

  private static final String PRINTF_MESSAGE = "Use \"printf\" instead of \"String.format\".";
  private static final String LOGGING_MESSAGE = "Use the logger's built-in \"{}\" formatting instead of \"String.format\".";

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
    Tree argument = outermostParentheses(formatCall);
    Tree arguments = argument.parent();
    if (!arguments.is(Tree.Kind.ARGUMENTS)) {
      return;
    }
    if (!(arguments.parent() instanceof MethodInvocationTree call) || !isMessageArgument(call.methodSymbol(), call.arguments().indexOf(argument))) {
      return;
    }
    if (PRINT_METHODS.matches(call)) {
      QuickFixHelper.newIssue(context)
        .forRule(this)
        .onTree(formatCall.methodSelect())
        .withMessage(PRINTF_MESSAGE)
        .withQuickFixes(() -> printfQuickFix(call, argument, formatCall))
        .report();
    } else if (LOG_METHODS.matches(call) && hasSimpleLiteralFormat(formatCall) && hasNoFormattableArgument(formatCall)) {
      reportIssue(formatCall.methodSelect(), LOGGING_MESSAGE);
    }
  }

  private static Tree outermostParentheses(Tree tree) {
    Tree result = tree;
    while (result.parent().is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
      result = result.parent();
    }
    return result;
  }

  /**
   * Builds a quick fix turning {@code print(String.format(args))} into {@code printf(args)}.
   * For {@code println}, a {@code %n} is appended to the format, which is only possible when the format is a string literal.
   */
  private static List<JavaQuickFix> printfQuickFix(MethodInvocationTree printCall, Tree argument, MethodInvocationTree formatCall) {
    List<ExpressionTree> formatArguments = formatCall.arguments();
    boolean isPrintln = "println".equals(printCall.methodSymbol().name());
    JavaQuickFix.Builder builder = JavaQuickFix.newQuickFix("Replace with \"printf\"");
    if (isPrintln) {
      ExpressionTree format = formatArguments.get(formatArguments.get(0).symbolType().is("java.util.Locale") ? 1 : 0);
      if (!format.is(Tree.Kind.STRING_LITERAL)) {
        return List.of();
      }
      AnalyzerMessage.TextSpan formatSpan = AnalyzerMessage.textSpanFor(format);
      builder.addTextEdit(JavaTextEdit.insertAtPosition(formatSpan.endLine, formatSpan.endCharacter - 1, "%n"));
    }
    return List.of(builder
      .addTextEdit(JavaTextEdit.replaceTree(ExpressionUtils.methodName(printCall), "printf"))
      .addTextEdit(JavaTextEdit.removeTextSpan(AnalyzerMessage.textSpanBetween(argument, true, formatArguments.get(0), false)))
      .addTextEdit(JavaTextEdit.removeTextSpan(AnalyzerMessage.textSpanBetween(formatArguments.get(formatArguments.size() - 1), false, argument, true)))
      .build());
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
   * {@code %s} calls {@code formatTo} on {@link java.util.Formattable} values instead of {@code toString}, which "{}" placeholders would not preserve.
   */
  private static boolean hasNoFormattableArgument(MethodInvocationTree formatCall) {
    return formatCall.arguments().stream().noneMatch(arg -> arg.symbolType().isSubtypeOf("java.util.Formattable"));
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
