/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.plugins.java.api.semantic.MethodMatchers.ANY;

public abstract class AbstractPrintfChecker extends AbstractMethodDetection {

  protected static final String JAVA_LANG_STRING = "java.lang.String";
  protected static final String JAVA_UTIL_LOGGING_LOGGER = "java.util.logging.Logger";
  protected static final String ORG_APACHE_LOGGING_LOG4J_LOGGER = "org.apache.logging.log4j.Logger";
  protected static final String ORG_SLF4J_LOGGER = "org.slf4j.Logger";
  protected static final String JAVA_LANG_THROWABLE = "java.lang.Throwable";

  private static final Pattern PRINTF_PARAM_PATTERN = Pattern.compile("%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])");

  private static final String PRINTF_METHOD_NAME = "printf";
  private static final String FORMAT_METHOD_NAME = "format";
  protected static final List<String> LEVELS = Arrays.asList("debug", "error", "info", "trace", "warn", "fatal");

  protected static final MethodMatchers MESSAGE_FORMAT = MethodMatchers.create()
    .ofTypes("java.text.MessageFormat")
    .names(FORMAT_METHOD_NAME)
    .withAnyParameters()
    .build();
  protected static final MethodMatchers JAVA_UTIL_LOGGER_LOG_LEVEL_STRING = MethodMatchers.create()
    .ofTypes(JAVA_UTIL_LOGGING_LOGGER)
    .names("log")
    .addParametersMatcher("java.util.logging.Level", JAVA_LANG_STRING)
    .build();
  protected static final MethodMatchers JAVA_UTIL_LOGGER_LOG_LEVEL_STRING_ANY = MethodMatchers.create()
    .ofTypes(JAVA_UTIL_LOGGING_LOGGER)
    .names("log")
    .addParametersMatcher("java.util.logging.Level", JAVA_LANG_STRING, ANY)
    .build();
  protected static final MethodMatchers JAVA_UTIL_LOGGER_LOG_MATCHER = MethodMatchers.or(
    JAVA_UTIL_LOGGER_LOG_LEVEL_STRING,
    JAVA_UTIL_LOGGER_LOG_LEVEL_STRING_ANY);
    
  protected static final Pattern MESSAGE_FORMAT_PATTERN = Pattern.compile("\\{(?<index>\\d+)(?<type>,\\w+)?(?<style>,[^}]*)?\\}");


  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    ArrayList<MethodMatchers> matchers = new ArrayList<>(slf4jMethods());
    matchers.add(log4jMethods());
    matchers.addAll(Arrays.asList(
      MethodMatchers.create().ofTypes(JAVA_LANG_STRING).names(FORMAT_METHOD_NAME).withAnyParameters().build(),
      MethodMatchers.create()
        .ofTypes("java.util.Formatter").names(FORMAT_METHOD_NAME).withAnyParameters().build(),
      MethodMatchers.create()
        .ofTypes("java.io.PrintStream").names(FORMAT_METHOD_NAME, PRINTF_METHOD_NAME).withAnyParameters().build(),
      MethodMatchers.create()
        .ofTypes("java.io.PrintWriter").names(FORMAT_METHOD_NAME, PRINTF_METHOD_NAME).withAnyParameters().build(),
      MESSAGE_FORMAT,
      JAVA_UTIL_LOGGER_LOG_LEVEL_STRING,
      JAVA_UTIL_LOGGER_LOG_LEVEL_STRING_ANY
      ));
    return MethodMatchers.or(matchers);
  }

  private static Collection<MethodMatchers> slf4jMethods() {
    return LEVELS.stream()
      .map(l -> MethodMatchers.create().ofTypes(ORG_SLF4J_LOGGER).names(l).withAnyParameters().build())
      .collect(Collectors.toList());
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

  protected final void checkFormatting(MethodInvocationTree mit, boolean isMessageFormat) {
    Arguments arguments = mit.arguments();
    if (arguments.stream().map(ExpressionTree::symbolType).anyMatch(Type::isUnknown)) {
      // method resolved but not all the parameters are
      return;
    }
    ExpressionTree formatTree;
    List<ExpressionTree> args;
    // Check type of first argument:
    if (arguments.get(0).symbolType().is(JAVA_LANG_STRING)) {
      formatTree = arguments.get(0);
      args = arguments.subList(1, arguments.size());
    } else {
      if (arguments.size() < 2) {
        // probably use a lambda or any other supplier form to get a message
        return;
      }
      // format method with "Locale" or "Level" as first argument, skip that one.
      formatTree = arguments.get(1);
      args = arguments.subList(2, arguments.size());
    }
    if (formatTree.is(Tree.Kind.STRING_LITERAL)) {
      String formatString = LiteralUtils.trimQuotes(((LiteralTree) formatTree).value());
      if (isMessageFormat && isProbablyLog4jFormatterLogger(mit, formatString)) {
        isMessageFormat = false;
      }
      if (isMessageFormat) {
        handleMessageFormat(mit, formatString, args);
      } else {
        handlePrintfFormat(mit, formatString, args);
      }
    } else {
      handleOtherFormatTree(mit, formatTree, args);
    }
  }

  private static boolean isProbablyLog4jFormatterLogger(MethodInvocationTree mit, String formatString) {
    return mit.symbol().owner().type().is(ORG_APACHE_LOGGING_LOG4J_LOGGER) &&
      !formatString.contains("{}") &&
      formatString.contains("%");
  }

  protected abstract void handlePrintfFormat(MethodInvocationTree mit, String formatString, List<ExpressionTree> args);

  protected abstract void handleMessageFormat(MethodInvocationTree mit, String formatString, List<ExpressionTree> args);

  protected abstract void handleOtherFormatTree(MethodInvocationTree mit, ExpressionTree formatTree, List<ExpressionTree> args);

  protected static boolean isNewArrayWithInitializers(ExpressionTree expression) {
    return expression.is(Tree.Kind.NEW_ARRAY) && ((NewArrayTree) expression).openBraceToken() != null;
  }

  protected static String cleanupDoubleQuote(String formatString) {
    return formatString.replaceAll("\'\'", "");
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

  @Nullable
  protected static List<ExpressionTree> transposeArgumentArrayAndRemoveThrowable(MethodInvocationTree mit, List<ExpressionTree> args) {
    List<ExpressionTree> transposedArgs = args;
    if (args.size() == 1) {
      ExpressionTree firstArg = args.get(0);
      if (firstArg.symbolType().isArray()) {
        if (isNewArrayWithInitializers(firstArg)) {
          transposedArgs = ((NewArrayTree) firstArg).initializers();
        } else {
          // size is unknown
          return null;
        }
      }
    }
    if (lastArgumentShouldBeIgnored(mit, args, transposedArgs)) {
      transposedArgs = transposedArgs.subList(0, transposedArgs.size() - 1);
    }
    return transposedArgs;
  }

  private static boolean lastArgumentShouldBeIgnored(MethodInvocationTree mit, List<ExpressionTree> args, List<ExpressionTree> transposedArgs) {
    if (mit.symbol().owner().type().is(JAVA_UTIL_LOGGING_LOGGER)) {
      return args.size() == 1 && isLastArgumentThrowable(args);
    }
    // org.apache.logging.log4j.Logger and org.slf4j.Logger
    return isLastArgumentThrowable(transposedArgs);
  }

  protected static boolean isLastArgumentThrowable(List<ExpressionTree> arguments) {
    if (!arguments.isEmpty()) {
      ExpressionTree lastArgument = arguments.get(arguments.size() - 1);
      return lastArgument.symbolType().isSubtypeOf(JAVA_LANG_THROWABLE);
    }
    return false;
  }

  protected static boolean isLoggingMethod(MethodInvocationTree mit) {
    String methodName = mit.symbol().name();
    return "log".equals(methodName) || LEVELS.contains(methodName);
  }

}
