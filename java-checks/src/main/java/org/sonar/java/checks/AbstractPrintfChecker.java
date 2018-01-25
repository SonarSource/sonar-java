/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.Tree;

public abstract class AbstractPrintfChecker extends AbstractMethodDetection {

  private static final Pattern PRINTF_PARAM_PATTERN = Pattern.compile("%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])");
  private static final String FORMAT_METHOD_NAME = "format";
  protected static final List<String> LEVELS = Arrays.asList("debug", "error", "info", "trace", "warn");

  protected static final MethodMatcher MESSAGE_FORMAT = MethodMatcher.create().typeDefinition("java.text.MessageFormat").name(FORMAT_METHOD_NAME).withAnyParameters();
  protected static final MethodMatcher JAVA_UTIL_LOGGER = MethodMatcher.create().typeDefinition("java.util.logging.Logger").name("log")
    .addParameter("java.util.logging.Level")
    .addParameter("java.lang.String")
    .addParameter(TypeCriteria.anyType());
  protected static final Pattern MESSAGE_FORMAT_PATTERN = Pattern.compile("\\{(?<index>\\d+)(?<type>,\\w+)?(?<style>,[^}]*)?\\}");

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return ImmutableList.<MethodMatcher>builder().add(
      MethodMatcher.create().typeDefinition("java.lang.String").name(FORMAT_METHOD_NAME).withAnyParameters(),
      MethodMatcher.create().typeDefinition("java.util.Formatter").name(FORMAT_METHOD_NAME).withAnyParameters(),
      MethodMatcher.create().typeDefinition("java.io.PrintStream").name(FORMAT_METHOD_NAME).withAnyParameters(),
      MethodMatcher.create().typeDefinition("java.io.PrintStream").name("printf").withAnyParameters(),
      MethodMatcher.create().typeDefinition("java.io.PrintWriter").name(FORMAT_METHOD_NAME).withAnyParameters(),
      MethodMatcher.create().typeDefinition("java.io.PrintWriter").name("printf").withAnyParameters(),
      MESSAGE_FORMAT,
      JAVA_UTIL_LOGGER
      )
      .addAll(slf4jMethods())
      .build();
  }

  private static Collection<MethodMatcher> slf4jMethods() {
    return LEVELS.stream()
      .map(l -> MethodMatcher.create().typeDefinition("org.slf4j.Logger").name(l).withAnyParameters())
      .collect(Collectors.toList());
  }



  protected abstract void handlePrintfFormat(MethodInvocationTree mit, String formatString, List<ExpressionTree> args);

  protected abstract void handleMessageFormat(MethodInvocationTree mit, String formatString, List<ExpressionTree> args);

  protected static boolean isNewArrayWithInitializers(ExpressionTree expression) {
    return expression.is(Tree.Kind.NEW_ARRAY) && ((NewArrayTree) expression).openBraceToken() != null;
  }

  protected static String cleanupDoubleQuote(String formatString) {
    return formatString.replaceAll("\'\'", "");
  }


  protected static Set<Integer> getMessageFormatIndexes(String formatString, MethodInvocationTree mit) {
    if(LEVELS.contains(mit.symbol().name())) {
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

}
