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
import com.google.common.collect.Sets;

import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Rule(key = "S2275")
public class PrintfCheck extends AbstractMethodDetection {

  private static final Pattern PRINTF_PARAM_PATTERN = Pattern.compile("%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])");
  private static final Set<String> TIME_CONVERSIONS = Sets.newHashSet(
    "H", "I", "k", "l", "M", "S", "L", "N", "p", "z", "Z", "s", "Q",
    "B", "b", "h", "A", "a", "C", "Y", "y", "j", "m", "d", "e",
    "R", "T", "r", "D", "F", "c"
    );
  private static final String FORMAT_METHOD_NAME = "format";

  private static final MethodMatcher MESSAGE_FORMAT = MethodMatcher.create().typeDefinition("java.text.MessageFormat").name(FORMAT_METHOD_NAME).withAnyParameters();
  private static final MethodMatcher TO_STRING = MethodMatcher.create().typeDefinition(TypeCriteria.anyType()).name("toString").withoutParameter();
  private static final Pattern MESSAGE_FORMAT_PATTERN = Pattern.compile("\\{(?<index>\\d+)(?<type>,\\w+)?(?<style>,[^}]*)?\\}");

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return ImmutableList.of(
      MethodMatcher.create().typeDefinition("java.lang.String").name(FORMAT_METHOD_NAME).withAnyParameters(),
      MethodMatcher.create().typeDefinition("java.util.Formatter").name(FORMAT_METHOD_NAME).withAnyParameters(),
      MethodMatcher.create().typeDefinition("java.io.PrintStream").name(FORMAT_METHOD_NAME).withAnyParameters(),
      MethodMatcher.create().typeDefinition("java.io.PrintStream").name("printf").withAnyParameters(),
      MethodMatcher.create().typeDefinition("java.io.PrintWriter").name(FORMAT_METHOD_NAME).withAnyParameters(),
      MethodMatcher.create().typeDefinition("java.io.PrintWriter").name("printf").withAnyParameters(),
      MESSAGE_FORMAT
      );
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    ExpressionTree formatStringTree;
    List<ExpressionTree> args;
    boolean isMessageFormat = MESSAGE_FORMAT.matches(mit);
    if (isMessageFormat && !mit.symbol().isStatic()) {
      // only consider the static method
      return;
    }
    // Check type of first argument:
    if (mit.arguments().get(0).symbolType().is("java.lang.String")) {
      formatStringTree = mit.arguments().get(0);
      args = mit.arguments().subList(1, mit.arguments().size());
    } else {
      // format method with "Locale" first argument, skip that one.
      formatStringTree = mit.arguments().get(1);
      args = mit.arguments().subList(2, mit.arguments().size());
    }
    if (formatStringTree.is(Tree.Kind.STRING_LITERAL)) {
      String formatString = LiteralUtils.trimQuotes(((LiteralTree) formatStringTree).value());
      if (isMessageFormat) {
        handleMessageFormat(mit, formatString, args);
      } else {
        handlePrintfFormat(mit, formatString, args);
      }
    } else if (isConcatenationOnSameLine(formatStringTree)) {
      reportIssue(mit, "Format specifiers should be used instead of string concatenation.");
    }
  }

  private void handlePrintfFormat(MethodInvocationTree mit, String formatString, List<ExpressionTree> args) {
    List<String> params = getParameters(formatString, mit);
    if (usesMessageFormat(formatString, params)) {
      reportIssue(mit, "Looks like there is a confusion with the use of java.text.MessageFormat, parameters will be simply ignored here");
      return;
    }
    checkLineFeed(formatString, mit);
    if (checkEmptyParams(mit, params)) {
      return;
    }
    cleanupLineSeparator(params);
    if (!params.isEmpty()) {
      if (checkArgumentNumber(mit, argIndexes(params).size(), args.size())) {
        return;
      }
      verifyParameters(mit, args, params);
    }
  }

  private void handleMessageFormat(MethodInvocationTree mit, String formatString, List<ExpressionTree> args) {
    String newFormatString = cleanupDoubleQuote(formatString);
    Set<Integer> indexes = getMessageFormatIndexes(newFormatString);
    List<ExpressionTree> newArgs = args;
    if (newArgs.size() == 1) {
      ExpressionTree firstArg = newArgs.get(0);
      if (firstArg.symbolType().isArray()) {
        if (isNewArrayWithInitializers(firstArg)) {
          newArgs = ((NewArrayTree) firstArg).initializers();
        } else {
          // size is unknown
          return;
        }
      }
    }
    if (checkEmptyParams(mit, indexes)
      || checkArgumentNumber(mit, indexes.size(), newArgs.size())
      || checkUnbalancedQuotes(mit, newFormatString)
      || checkUnbalancedBraces(mit, newFormatString)) {
      return;
    }
    checkToStringInvocation(newArgs);
    verifyParameters(mit, newArgs, indexes);
  }

  private static boolean isNewArrayWithInitializers(ExpressionTree expression) {
    return expression.is(Tree.Kind.NEW_ARRAY) && ((NewArrayTree) expression).openBraceToken() != null;
  }

  private static String cleanupDoubleQuote(String formatString) {
    return formatString.replaceAll("\'\'", "");
  }

  private boolean checkEmptyParams(MethodInvocationTree mit, Collection<?> params) {
    return checkAndReport(mit, params.isEmpty(), "String contains no format specifiers.");
  }

  private boolean checkArgumentNumber(MethodInvocationTree mit, int nbReadParams, int nbArgs) {
    return checkAndReport(mit, nbReadParams > nbArgs, "Not enough arguments.");
  }

  private boolean checkAndReport(MethodInvocationTree mit, boolean shouldReport, String message) {
    if (shouldReport) {
      reportIssue(mit, message);
    }
    return shouldReport;
  }

  private boolean checkUnbalancedQuotes(MethodInvocationTree mit, String formatString) {
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

  private boolean checkUnbalancedBraces(MethodInvocationTree mit, String formatString) {
    String withoutParam = MESSAGE_FORMAT_PATTERN.matcher(formatString).replaceAll("");
    int numberOpenBrace = 0;
    for (int i = 0; i < withoutParam.length(); ++i) {
      char ch = withoutParam.charAt(i);
      switch (ch) {
        case '{':
          numberOpenBrace++;
          break;
        case '}':
          numberOpenBrace--;
          break;
        default:
          break;
      }
    }
    boolean unbalancedBraces = numberOpenBrace > 0;
    if (unbalancedBraces) {
      reportIssue(mit.arguments().get(0), "Single left curly braces \"{\" must be escaped.");
    }
    return unbalancedBraces;
  }

  private void checkToStringInvocation(List<ExpressionTree> args) {
    args.stream()
      .filter(arg -> arg.is(Tree.Kind.METHOD_INVOCATION) && TO_STRING.matches((MethodInvocationTree) arg))
      .forEach(arg -> reportIssue(arg, "No need to call toString \"method()\" as formatting and string conversion is done by the Formatter."));
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

  private static Set<Integer> getMessageFormatIndexes(String formatString) {
    Matcher matcher = MESSAGE_FORMAT_PATTERN.matcher(formatString);
    Set<Integer> result = new HashSet<>();
    while (matcher.find()) {
      if (isMessageFormatPattern(formatString, matcher.start())) {
        result.add(Integer.parseInt(matcher.group("index")));
      }
    }
    return result;
  }

  private static boolean isMessageFormatPattern(String formatString, int start) {
    return start == 0 || formatString.charAt(start - 1) != '\'';
  }

  private static Set<Integer> argIndexes(List<String> params) {
    int index = 0;
    Set<Integer> result = new HashSet<>();
    for (String rawParam : params) {
      if (rawParam.contains("$")) {
        result.add(getIndex(rawParam));
      } else if (rawParam.charAt(0) != '<') {
        index++;
        result.add(index);
      }
    }
    return result;
  }

  private static boolean isConcatenationOnSameLine(ExpressionTree formatStringTree) {
    return formatStringTree.is(Tree.Kind.PLUS) && operandsAreOnSameLine((BinaryExpressionTree) formatStringTree);
  }

  private static boolean operandsAreOnSameLine(BinaryExpressionTree formatStringTree) {
    return formatStringTree.leftOperand().firstToken().line() == formatStringTree.rightOperand().firstToken().line();
  }

  private static void cleanupLineSeparator(List<String> params) {
    // Cleanup %n values
    Iterator<String> iter = params.iterator();
    while (iter.hasNext()) {
      String param = iter.next();
      if ("n".equals(param)) {
        iter.remove();
      }
    }
  }

  private void checkLineFeed(String formatString, MethodInvocationTree mit) {
    if (formatString.contains("\\n")) {
      reportIssue(mit, "%n should be used in place of \\n to produce the platform-specific line separator.");
    }
  }

  private void verifyParameters(MethodInvocationTree mit, List<ExpressionTree> args, List<String> params) {
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
      }else {
        index++;
      }
      ExpressionTree argExpressionTree = args.get(argIndex);
      unusedArgs.remove(argExpressionTree);
      Type argType = argExpressionTree.symbolType();
      checkNumerical(mit, param, argType);
      checkBoolean(mit, param, argType);
      checkTimeConversion(mit, param, argType);

    }
    reportUnusedArgs(mit, args, unusedArgs);
  }

  private static Integer getIndex(String param) {
    return Integer.valueOf(param.substring(0, param.indexOf('$')));
  }

  private void checkBoolean(MethodInvocationTree mit, String param, Type argType) {
    if (param.charAt(0) == 'b' && !(argType.is("boolean") || argType.is("java.lang.Boolean"))) {
      reportIssue(mit, "Directly inject the boolean value.");
    }
  }

  private void checkNumerical(MethodInvocationTree mit, String param, Type argType) {
    if (param.charAt(0) == 'd' && !isNumerical(argType)) {
      reportIssue(mit, "An 'int' is expected rather than a " + argType + ".");
    }
  }

  private void checkTimeConversion(MethodInvocationTree mit, String param, Type argType) {
    if (param.charAt(0) == 't' || param.charAt(0) == 'T') {
      String timeConversion = param.substring(1);
      if (timeConversion.isEmpty()) {
        reportIssue(mit, "Time conversion requires a second character.");
        checkTimeTypeArgument(mit, argType);
        return;
      }
      if (!TIME_CONVERSIONS.contains(timeConversion)) {
        reportIssue(mit, timeConversion + " is not a supported time conversion character");
      }
      checkTimeTypeArgument(mit, argType);
    }
  }

  private void checkTimeTypeArgument(MethodInvocationTree mit, Type argType) {
    if (!(argType.isNumerical()
      || isTypeOfAny(argType, "java.lang.Long")
      || isSubtypeOfAny(argType, "java.util.Date", "java.util.Calendar", "java.time.temporal.TemporalAccessor"))) {
      reportIssue(mit, "Time argument is expected (long, Long, Calendar, Date and TemporalAccessor).");
    }
  }

  private static boolean isNumerical(Type argType) {
    return argType.isNumerical()
      || isTypeOfAny(argType,
        "java.math.BigInteger",
        "java.math.BigDecimal",
        "java.lang.Byte",
        "java.lang.Short",
        "java.lang.Integer",
        "java.lang.Long",
        "java.lang.Float",
        "java.lang.Double");
  }

  private static boolean isTypeOfAny(Type argType, String... fullyQualifiedNames) {
    return Arrays.stream(fullyQualifiedNames).anyMatch(argType::is);
  }

  private static boolean isSubtypeOfAny(Type argType, String... fullyQualifiedNames) {
    return Arrays.stream(fullyQualifiedNames).anyMatch(argType::isSubtypeOf);
  }

  private static boolean usesMessageFormat(String formatString, List<String> params) {
    return params.isEmpty() && (formatString.contains("{0") || formatString.contains("{1"));
  }

  private void reportUnusedArgs(MethodInvocationTree mit, List<ExpressionTree> args, List<ExpressionTree> unusedArgs) {
    for (ExpressionTree unusedArg : unusedArgs) {
      int i = args.indexOf(unusedArg);
      String stringArgIndex = "first";
      if (i == 1) {
        stringArgIndex = "2nd";
      } else if (i == 2) {
        stringArgIndex = "3rd";
      } else if (i >= 3) {
        stringArgIndex = (i + 1) + "th";
      }
      reportIssue(mit, stringArgIndex + " argument is not used.");
    }
  }

  private List<String> getParameters(String formatString, MethodInvocationTree mit) {
    List<String> params = new ArrayList<>();
    Matcher matcher = PRINTF_PARAM_PATTERN.matcher(formatString);
    while (matcher.find()) {
      if (firstArgumentIsLT(params, matcher.group(2))) {
        reportIssue(mit, "The argument index '<' refers to the previous format specifier but there isn't one.");
        continue;
      }
      StringBuilder param = new StringBuilder();
      for (int groupIndex : new int[] {1, 2, 5, 6}) {
        if (matcher.group(groupIndex) != null) {
          param.append(matcher.group(groupIndex));
        }
      }
      String specifier = param.toString();
      if(!"%".equals(specifier)) {
        params.add(specifier);
      }
    }
    return params;
  }

  private static boolean firstArgumentIsLT(List<String> params, @Nullable String group) {
    return params.isEmpty() && group != null && group.length() > 0 && group.charAt(0) == '<';
  }

}
