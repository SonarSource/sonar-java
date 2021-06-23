/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.collections.SetUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.Tree;

public abstract class AbstractPrintfChecker extends AbstractMethodDetection {

  private static final Set<String> TIME_CONVERSIONS = SetUtils.immutableSetOf(
    "H", "I", "k", "l", "M", "S", "L", "N", "p", "z", "Z", "s", "Q",
    "B", "b", "h", "A", "a", "C", "Y", "y", "j", "m", "d", "e",
    "R", "T", "r", "D", "F", "c"
  );

  protected static final String JAVA_LANG_STRING = "java.lang.String";
  protected static final String JAVA_LANG_THROWABLE = "java.lang.Throwable";
  protected static final String ORG_APACHE_LOGGING_LOG4J_LOGGER = "org.apache.logging.log4j.Logger";

  private static final Pattern PRINTF_PARAM_PATTERN = Pattern.compile("%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])");

  protected static final String PRINTF_METHOD_NAME = "printf";
  private static final String FORMAT_METHOD_NAME = "format";
  protected static final Set<String> LEVELS = SetUtils.immutableSetOf("debug", "error", "info", "trace", "warn", "fatal");

  protected static final MethodMatchers MESSAGE_FORMAT = MethodMatchers.create()
    .ofTypes("java.text.MessageFormat")
    .names(FORMAT_METHOD_NAME)
    .withAnyParameters()
    .build();

  protected static final MethodMatchers STRING_FORMATTED = MethodMatchers.create()
    .ofTypes(JAVA_LANG_STRING)
    .names("formatted")
    .withAnyParameters()
    .build();

  protected static final Pattern MESSAGE_FORMAT_PATTERN = Pattern.compile("\\{(?<index>\\d+)(?<type>,\\w+)?(?<style>,[^}]*)?\\}");

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(
      MethodMatchers.create()
        .ofTypes(JAVA_LANG_STRING).names(FORMAT_METHOD_NAME).withAnyParameters().build(),
      STRING_FORMATTED,
      MethodMatchers.create()
        .ofTypes("java.util.Formatter").names(FORMAT_METHOD_NAME).withAnyParameters().build(),
      MethodMatchers.create()
        .ofTypes("java.io.PrintStream").names(FORMAT_METHOD_NAME, PRINTF_METHOD_NAME).withAnyParameters().build(),
      MethodMatchers.create()
        .ofTypes("java.io.PrintWriter").names(FORMAT_METHOD_NAME, PRINTF_METHOD_NAME).withAnyParameters().build(),
      MESSAGE_FORMAT);
  }

  protected final void checkFormatting(MethodInvocationTree mit, boolean isMessageFormat) {
    Arguments arguments = mit.arguments();
    if (arguments.stream().map(ExpressionTree::symbolType).anyMatch(Type::isUnknown)) {
      // method resolved but not all the parameters are
      return;
    }
    if (STRING_FORMATTED.matches(mit)) {
      if (!(mit.methodSelect().is(Tree.Kind.MEMBER_SELECT))) {
        return;
      }
      ExpressionTree formatTree = ((MemberSelectExpressionTree) mit.methodSelect()).expression();
      checkFormatting(mit, isMessageFormat, formatTree, arguments);
    } else if (arguments.get(0).symbolType().is(JAVA_LANG_STRING)) {
      checkFormatting(mit, isMessageFormat, arguments.get(0), arguments.subList(1, arguments.size()));
    } else {
      if (arguments.size() < 2) {
        // probably use a lambda or any other supplier form to get a message
        return;
      }
      checkFormatting(mit, isMessageFormat, arguments.get(1), arguments.subList(2, arguments.size()));
    }
  }

  private void checkFormatting(MethodInvocationTree mit, boolean isMessageFormat, ExpressionTree formatTree, List<ExpressionTree> args) {
    if (formatTree.is(Tree.Kind.STRING_LITERAL)) {
      String formatString = LiteralUtils.trimQuotes(((LiteralTree) formatTree).value());
      if (isMessageFormat && isProbablyLog4jFormatterLogger(mit, formatString)) {
        handlePrintfFormatCatchingErrors(mit, formatString, args);
        return;
      }
      if (isMessageFormat) {
        handleMessageFormat(mit, formatString, args);
      } else {
        transposeArgumentArray(args).ifPresent(transposedArgs ->
          handlePrintfFormat(mit, formatString, transposedArgs)
        );
      }
    } else {
      handleOtherFormatTree(mit, formatTree, args);
    }
  }

  protected static Optional<List<ExpressionTree>> transposeArgumentArray(List<ExpressionTree> args) {
    if (args.size() == 1) {
      ExpressionTree firstArg = args.get(0);
      if (firstArg.symbolType().isArray()) {
        if (isNewArrayWithInitializers(firstArg)) {
          args = ((NewArrayTree) firstArg).initializers();
        } else {
          // array content is unknown, declared somewhere else, we can not know the size
          return Optional.empty();
        }
      }
    }
    return Optional.of(args);
  }

  private static boolean isProbablyLog4jFormatterLogger(MethodInvocationTree mit, String formatString) {
    return mit.symbol().owner().type().is(ORG_APACHE_LOGGING_LOG4J_LOGGER) &&
      !formatString.contains("{}") &&
      formatString.contains("%");
  }

  protected abstract void handlePrintfFormat(MethodInvocationTree mit, String formatString, List<ExpressionTree> args);

  protected abstract void handlePrintfFormatCatchingErrors(MethodInvocationTree mit, String formatString, List<ExpressionTree> args);

  protected abstract void handleMessageFormat(MethodInvocationTree mit, String formatString, List<ExpressionTree> args);

  protected abstract void handleOtherFormatTree(MethodInvocationTree mit, ExpressionTree formatTree, List<ExpressionTree> args);

  protected static boolean isNewArrayWithInitializers(ExpressionTree expression) {
    return expression.is(Tree.Kind.NEW_ARRAY) && ((NewArrayTree) expression).openBraceToken() != null;
  }

  protected static String cleanupDoubleQuote(String formatString) {
    return formatString.replace("''", "");
  }

  protected static Set<Integer> getMessageFormatIndexes(String formatString, MethodInvocationTree mit) {
    if (LEVELS.contains(mit.symbol().name()) || formatString.contains("{}")) {
      return IntStream.range(0, StringUtils.countMatches(formatString, "{}")).boxed().collect(Collectors.toSet());
    }
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
    return start == 0 ||
      formatString.charAt(start - 1) != '\'' || StringUtils.countMatches(formatString.substring(0, start), "\'")%2 == 0;
  }

  protected List<String> getParameters(String formatString, MethodInvocationTree mit) {
    List<String> params = new ArrayList<>();
    Matcher matcher = PRINTF_PARAM_PATTERN.matcher(formatString);
    while (matcher.find()) {
      if (firstArgumentIsLT(params, matcher.group(2))) {
        reportMissingPrevious(mit);
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

  protected void reportMissingPrevious(MethodInvocationTree mit) {
    // no-op in default case.
  }

  protected static Integer getIndex(String param) {
    return Integer.valueOf(param.substring(0, param.indexOf('$')));
  }

  protected static void cleanupLineSeparator(List<String> params) {
    // Cleanup %n values
    Iterator<String> iter = params.iterator();
    while (iter.hasNext()) {
      String param = iter.next();
      if ("n".equals(param)) {
        iter.remove();
      }
    }
  }

  protected static Set<Integer> argIndexes(List<String> params) {
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

  private static boolean firstArgumentIsLT(List<String> params, @Nullable String group) {
    return params.isEmpty() && group != null && group.length() > 0 && group.charAt(0) == '<';
  }

  protected boolean checkArgumentNumber(MethodInvocationTree mit, int nbReadParams, int nbArgs) {
    if (nbReadParams > nbArgs) {
      reportIssue(mit, "Not enough arguments.");
      return true;
    }
    return false;
  }

  protected void verifyParametersForErrors(MethodInvocationTree mit, List<ExpressionTree> args, List<String> params) {
    int index = 0;
    for (String rawParam : params) {
      String param = rawParam;
      int argIndex = index;
      if (param.contains("$")) {
        argIndex = getIndex(param) - 1;
        if (argIndex == -1) {
          return;
        }
        param = param.substring(param.indexOf('$') + 1);
      } else if (param.charAt(0) == '<') {
        //refers to previous argument
        argIndex = Math.max(0, argIndex - 1);
      }else {
        index++;
      }
      if (argIndex >= args.size()) {
        int formatIndex = argIndex + 1;
        reportIssue(mit, "Not enough arguments to feed formater at index " + formatIndex + ": '%" + formatIndex + "$'.");
        return;
      }
      ExpressionTree argExpressionTree = args.get(argIndex);
      Type argType = argExpressionTree.symbolType();
      checkNumerical(mit, param, argType);
      checkTimeConversion(mit, param, argType);
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
      || argType.is("java.lang.Long")
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

}
