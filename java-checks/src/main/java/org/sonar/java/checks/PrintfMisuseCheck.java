/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.UnionTypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.plugins.java.api.semantic.MethodMatchers.ANY;

@Rule(key = "S3457")
public class PrintfMisuseCheck extends AbstractPrintfChecker {

  private static final String ORG_SLF4J_LOGGER = "org.slf4j.Logger";
  private static final String JAVA_UTIL_LOGGING_LOGGER = "java.util.logging.Logger";

  private static final MethodMatchers TO_STRING = MethodMatchers.create()
    .ofAnyType().names("toString").addWithoutParametersMatcher().build();
  private static final MethodMatchers GET_LOGGER = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes(JAVA_UTIL_LOGGING_LOGGER).names("getLogger").addParametersMatcher(JAVA_LANG_STRING, JAVA_LANG_STRING).build(),
    MethodMatchers.create()
      .ofTypes(JAVA_UTIL_LOGGING_LOGGER).names("getAnonymousLogger").addParametersMatcher(JAVA_LANG_STRING).build());

  private static final MethodMatchers JAVA_UTIL_LOGGER_LOG_LEVEL_STRING = MethodMatchers.create()
    .ofTypes(JAVA_UTIL_LOGGING_LOGGER)
    .names("log")
    .addParametersMatcher("java.util.logging.Level", JAVA_LANG_STRING)
    .build();
  private static final MethodMatchers JAVA_UTIL_LOGGER_LOG_LEVEL_STRING_ANY = MethodMatchers.create()
    .ofTypes(JAVA_UTIL_LOGGING_LOGGER)
    .names("log")
    .addParametersMatcher("java.util.logging.Level", JAVA_LANG_STRING, ANY)
    .build();
  private static final MethodMatchers JAVA_UTIL_LOGGER_LOG_MATCHER = MethodMatchers.or(
    JAVA_UTIL_LOGGER_LOG_LEVEL_STRING,
    JAVA_UTIL_LOGGER_LOG_LEVEL_STRING_ANY);

  private static final MethodMatchers SLF4J_METHOD_MATCHERS = MethodMatchers.or(LEVELS.stream()
    .map(l -> MethodMatchers.create().ofTypes(ORG_SLF4J_LOGGER).names(l).withAnyParameters().build())
    .collect(Collectors.toList()));

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    ArrayList<MethodMatchers> matchers = new ArrayList<>();
    matchers.add(SLF4J_METHOD_MATCHERS);
    matchers.add(super.getMethodInvocationMatchers());
    // Add log methods as they only apply to misuse and not error.
    matchers.add(log4jMethods());
    matchers.add(JAVA_UTIL_LOGGER_LOG_LEVEL_STRING);
    matchers.add(JAVA_UTIL_LOGGER_LOG_LEVEL_STRING_ANY);
    return MethodMatchers.or(matchers);
  }

  private static MethodMatchers log4jMethods() {
    List<String> methodNames = new ArrayList<>();
    methodNames.add(PRINTF_METHOD_NAME);
    methodNames.add("log");
    methodNames.addAll(LEVELS);
    return MethodMatchers.create()
      .ofTypes(ORG_APACHE_LOGGING_LOG4J_LOGGER)
      .names(methodNames.toArray(new String[0]))
      .withAnyParameters()
      .build();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    boolean isMessageFormat = MESSAGE_FORMAT.matches(mit);
    if (isMessageFormat && !mit.methodSymbol().isStatic()) {
      // only consider the static method
      return;
    }
    if (!isMessageFormat && JAVA_UTIL_LOGGER_LOG_MATCHER.matches(mit) && hasResourceBundle(mit)) {
      return;
    }
    if (!isMessageFormat) {
      isMessageFormat = JAVA_UTIL_LOGGER_LOG_LEVEL_STRING_ANY.matches(mit);
    }
    if (!isMessageFormat) {
      isMessageFormat = isLoggingMethod(mit);
    }
    super.checkFormatting(mit, isMessageFormat);
  }

  private static boolean hasResourceBundle(MethodInvocationTree mit) {
    Tree id;
    if (mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      id = ((MemberSelectExpressionTree) mit.methodSelect()).expression();
    } else {
      // defensive programming : cannot be reached : log methods are not static
      return false;
    }
    if (id.is(Tree.Kind.MEMBER_SELECT)) {
      id = ((MemberSelectExpressionTree) id).identifier();
    }
    if (id.is(Tree.Kind.IDENTIFIER)) {
      Tree decl = ((IdentifierTree) id).symbol().declaration();
      if (decl != null && decl.is(Tree.Kind.VARIABLE)) {
        VariableTree variable = ((VariableTree) decl);
        ExpressionTree initializer = variable.initializer();
        if (initializer != null && initializer.is(Tree.Kind.METHOD_INVOCATION)) {
          return GET_LOGGER.matches((MethodInvocationTree) initializer);
        }
      }
    }
    return false;
  }

  @Override
  protected void handlePrintfFormat(MethodInvocationTree mit, String formatString, List<ExpressionTree> args) {
    handlePrintfFormat(mit, formatString, args, false);
  }

  @Override
  protected void handlePrintfFormatCatchingErrors(MethodInvocationTree mit, String formatString, List<ExpressionTree> args) {
    handlePrintfFormat(mit, formatString, args, true);
  }

  private void handlePrintfFormat(MethodInvocationTree mit, String formatString, List<ExpressionTree> args, boolean catchErrors) {
    List<String> params = getParameters(formatString, mit);
    if (usesMessageFormat(formatString, params)) {
      reportIssue(mit, "Looks like there is a confusion with the use of java.text.MessageFormat, parameters will be simply ignored here");
      return;
    }
    checkLineFeed(formatString, mit);
    if (params.isEmpty() && (!args.isEmpty() || !isLoggingMethod(mit))) {
      reportIssue(mit, "String contains no format specifiers.");
      return;
    }
    cleanupLineSeparator(params);
    if (!params.isEmpty()) {
      if (argIndexes(params).size() <= args.size()) {
        verifyParametersForMisuse(mit, args, params);
      }
      if (catchErrors) {
        // Errors are caught, we can report them in this rule
        if (checkArgumentNumber(mit, argIndexes(params).size(), args.size())) {
          return;
        }
        verifyParametersForErrors(mit, args, params);
      }
    }
  }

  private void verifyParametersForMisuse(MethodInvocationTree mit, List<ExpressionTree> args, List<String> params) {
    int index = 0;
    List<ExpressionTree> unusedArgs = new ArrayList<>(args);
    for (String rawParam : params) {
      String param = rawParam;
      int argIndex = index;
      if (param.contains("$")) {
        argIndex = getIndex(param) - 1;
        if (argIndex == -1) {
          reportIssue(mit, "Arguments are numbered starting from 1.");
          return;
        }
        param = param.substring(param.indexOf('$') + 1);
      } else if (param.charAt(0) == '<') {
        //refers to previous argument
        argIndex = Math.max(0, argIndex - 1);
      } else {
        index++;
      }
      if (argIndex >= args.size()) {
        // indexes are obviously wrong - will be caught by S2275 (PrintfFailCheck)
        return;
      }
      ExpressionTree argExpressionTree = args.get(argIndex);
      unusedArgs.remove(argExpressionTree);
      Type argType = argExpressionTree.symbolType();
      checkBoolean(mit, param, argType);
    }
    reportUnusedArgs(mit, args, unusedArgs);
  }

  @Override
  protected void handleMessageFormat(MethodInvocationTree mit, String formatString, List<ExpressionTree> args) {
    String newFormatString = cleanupDoubleQuote(formatString);
    Set<Integer> indexes = getMessageFormatIndexes(newFormatString, mit);
    List<ExpressionTree> transposedArgs = transposeArgumentArrayAndRemoveThrowable(mit, args, indexes);
    if (transposedArgs == null) {
      return;
    }
    if (indexes.isEmpty() && !transposedArgs.isEmpty()) {
      reportIssue(mit, "String contains no format specifiers.");
      return;
    }
    if (checkArgumentNumber(mit, indexes.size(), transposedArgs.size())
      || checkUnbalancedQuotes(mit, newFormatString)) {
      return;
    }
    checkToStringInvocation(transposedArgs);
    verifyParameters(mit, transposedArgs, indexes);
  }

  private boolean checkUnbalancedQuotes(MethodInvocationTree mit, String formatString) {
    if (LEVELS.contains(mit.methodSymbol().name())) {
      return false;
    }
    String withoutParam = MESSAGE_FORMAT_PATTERN.matcher(formatString).replaceAll("");
    int numberQuote = 0;
    for (int i = 0; i < withoutParam.length(); ++i) {
      if (withoutParam.charAt(i) == '\'') {
        numberQuote++;
      }
    }
    boolean unbalancedQuotes = (numberQuote % 2) != 0;
    if (unbalancedQuotes) {
      reportIssue(mit.arguments().get(0), "Single quote \"'\" must be escaped.");
    }
    return unbalancedQuotes;
  }

  @Nullable
  private static List<ExpressionTree> transposeArgumentArrayAndRemoveThrowable(MethodInvocationTree mit, List<ExpressionTree> args, Set<Integer> indexes) {
    return transposeArgumentArray(args).map(transposedArgs -> {
      if (lastArgumentShouldBeIgnored(mit, args, transposedArgs, indexes)) {
        return transposedArgs.subList(0, transposedArgs.size() - 1);
      } else {
        return transposedArgs;
      }
    }).orElse(null);
  }

  private static boolean lastArgumentShouldBeIgnored(MethodInvocationTree mit, List<ExpressionTree> args, List<ExpressionTree> transposedArgs, Set<Integer> indexes) {
    if (!isLoggingMethod(mit)) {
      return false;
    }
    if (mit.methodSymbol().owner().type().is(JAVA_UTIL_LOGGING_LOGGER)) {
      // Remove the last argument from the count if it's a throwable, since log(Level level, String msg, Throwable thrown) will be called.
      // If the argument is an array, any exception in the array will be considered as Object, behaving as any others.
      return args.size() == 1 && isLastArgumentThrowable(args);
    }
    // org.apache.logging.log4j.Logger and org.slf4j.Logger
    if (transposedArgs.size() == 1) {
      // Logging methods with only one throwable argument will treat it differently (and should be removed from the count).
      return isLastArgumentThrowable(transposedArgs);
    } else {
      // One extra throwable argument can be consumed by logging methods, it should be removed from the count if it exists.
      return (transposedArgs.size() > indexes.size()) && isLastArgumentThrowable(transposedArgs);
    }
  }

  private static boolean isLastArgumentThrowable(List<ExpressionTree> arguments) {
    if (arguments.isEmpty()) {
      return false;
    }
    ExpressionTree lastArgument = arguments.get(arguments.size() - 1);
    if (lastArgument.symbolType().isSubtypeOf(JAVA_LANG_THROWABLE)) {
      return true;
    }
    return hasUnknownExceptionInUnionType(ExpressionUtils.skipParentheses(lastArgument));
  }

  /**
   * Limitation of ECJ, which will approximate type of a variable to 'Object' if some types
   * are unknown in its defining union type, for instance: in the following catch tree:
   *
   * catch (UnknownException | IllegalArgumentException e) { ... }
   *
   * leads to have 'e' being of type 'Object'
   */
  private static boolean hasUnknownExceptionInUnionType(ExpressionTree lastArgument) {
    if (!lastArgument.is(Tree.Kind.IDENTIFIER)) {
      return false;
    }
    Symbol symbol = ((IdentifierTree) lastArgument).symbol();
    VariableTree declaration = symbol.isVariableSymbol() ? ((Symbol.VariableSymbol) symbol).declaration() : null;
    if (declaration == null) {
      return false;
    }
    TypeTree declarationType = declaration.type();
    return declarationType.is(Tree.Kind.UNION_TYPE)
      && ((UnionTypeTree) declarationType)
        .typeAlternatives()
        .stream()
        .map(TypeTree::symbolType)
        .anyMatch(Type::isUnknown);
  }

  private void checkToStringInvocation(List<ExpressionTree> args) {
    args.stream()
      .filter(arg -> arg.is(Tree.Kind.METHOD_INVOCATION))
      .map(MethodInvocationTree.class::cast)
      .filter(TO_STRING::matches)
      .filter(arg -> arg != args.get(args.size() - 1) || !isMethodOfThrowable(arg))
      .forEach(arg -> reportIssue(arg, getToStringMessage(arg)));
  }

  private static boolean isMethodOfThrowable(MethodInvocationTree argument) {
    Symbol owner = argument.methodSymbol().owner();
    return owner != null && owner.type().isSubtypeOf(JAVA_LANG_THROWABLE);
  }

  private static String getToStringMessage(ExpressionTree arg) {
    if (isInStringArrayInitializer(arg)) {
      return "No need to call \"toString()\" method since an array of Objects can be used here.";
    }
    return "No need to call \"toString()\" method as formatting and string conversion is done by the Formatter.";
  }

  private static boolean isInStringArrayInitializer(ExpressionTree arg) {
    return Optional.of(arg)
      .map(Tree::parent)
      .filter(tree -> tree.is(Tree.Kind.LIST))
      .map(Tree::parent)
      .filter(tree -> tree.is(Tree.Kind.NEW_ARRAY))
      .map(NewArrayTree.class::cast)
      .map(ExpressionTree::symbolType)
      .filter(Type::isArray)
      .map(Type.ArrayType.class::cast)
      .map(Type.ArrayType::elementType)
      .filter(type -> type.is(JAVA_LANG_STRING))
      .isPresent();
  }

  private void verifyParameters(MethodInvocationTree mit, List<ExpressionTree> args, Set<Integer> indexes) {
    List<ExpressionTree> unusedArgs = new ArrayList<>(args);
    for (int index : indexes) {
      if (index >= args.size()) {
        reportIssue(mit, "Not enough arguments.");
        return;
      }
      unusedArgs.remove(args.get(index));
    }
    reportUnusedArgs(mit, args, unusedArgs);
  }

  private void reportUnusedArgs(MethodInvocationTree mit, List<ExpressionTree> args, List<ExpressionTree> unusedArgs) {
    for (ExpressionTree unusedArg : unusedArgs) {
      int i = args.indexOf(unusedArg);
      reportIssue(mit, postFixedIndex(i) + " argument is not used.");
    }
  }

  private static String postFixedIndex(int i) {
    if (i < 1) {
      return "first";
    } else if (i < 2) {
      return "2nd";
    } else if (i < 3) {
      return "3rd";
    } else {
      return (i + 1) + "th";
    }
  }

  private void checkBoolean(MethodInvocationTree mit, String param, Type argType) {
    if (param.charAt(0) == 'b' && !(argType.is("boolean") || argType.is("java.lang.Boolean"))) {
      reportIssue(mit, "Directly inject the boolean value.");
    }
  }

  private void checkLineFeed(String formatString, MethodInvocationTree mit) {
    if (formatString.contains("\\n")) {
      reportIssue(mit, "%n should be used in place of \\n to produce the platform-specific line separator.");
    }
  }

  private static boolean usesMessageFormat(String formatString, List<String> params) {
    return params.isEmpty() && (formatString.contains("{0") || formatString.contains("{1"));
  }

  @Override
  protected void handleOtherFormatTree(MethodInvocationTree mit, ExpressionTree formatTree, List<ExpressionTree> args) {
    if (isIncorrectConcatenation(formatTree)) {
      boolean lastArgumentThrowable = isLastArgumentThrowable(args);
      if (JAVA_UTIL_LOGGER_LOG_MATCHER.matches(mit)) {
        if (lastArgumentThrowable) {
          reportIssue(mit, "Lambda should be used to defer string concatenation.");
        } else {
          reportIssue(mit, "Format specifiers or lambda should be used instead of string concatenation.");
        }
      } else if (!(lastArgumentThrowable && SLF4J_METHOD_MATCHERS.matches(mit))) {
        reportIssue(mit, "Format specifiers should be used instead of string concatenation.");
      }
    }
  }

  private static boolean isIncorrectConcatenation(ExpressionTree formatStringTree) {
    return formatStringTree.is(Tree.Kind.PLUS) && !formatStringTree.asConstant().isPresent();
  }

  private static boolean isLoggingMethod(MethodInvocationTree mit) {
    String methodName = mit.methodSymbol().name();
    return "log".equals(methodName) || LEVELS.contains(methodName);
  }
}
