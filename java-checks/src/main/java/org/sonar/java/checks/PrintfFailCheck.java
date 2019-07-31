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

import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;

@Rule(key = "S2275")
public class PrintfFailCheck extends AbstractPrintfChecker {

  private static final Set<String> TIME_CONVERSIONS = Sets.newHashSet(
    "H", "I", "k", "l", "M", "S", "L", "N", "p", "z", "Z", "s", "Q",
    "B", "b", "h", "A", "a", "C", "Y", "y", "j", "m", "d", "e",
    "R", "T", "r", "D", "F", "c"
  );

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    boolean isMessageFormat = MESSAGE_FORMAT.matches(mit);
    if (isMessageFormat && !mit.symbol().isStatic()) {
      // only consider the static method
      return;
    }
    if (!isMessageFormat) {
      isMessageFormat = JAVA_UTIL_LOGGER.matches(mit);
      if (isMessageFormat && mit.arguments().get(2).symbolType().isSubtypeOf("java.lang.Throwable")) {
        // ignore formatting issues when last argument is a throwable
        return;
      }
    }
    if(!isMessageFormat) {
      isMessageFormat = LEVELS.contains(mit.symbol().name());
      if (isMessageFormat && mit.arguments().get(mit.arguments().size() - 1).symbolType().isSubtypeOf("java.lang.Throwable")) {
        // ignore formatting issues when last argument is a throwable
        return;
      }
    }
    super.checkFormatting(mit, isMessageFormat);
  }

  @Override
  protected void handlePrintfFormat(MethodInvocationTree mit, String formatString, List<ExpressionTree> args) {
    List<String> params = getParameters(formatString, mit);
    cleanupLineSeparator(params);
    if (!params.isEmpty()) {
      if (checkArgumentNumber(mit, argIndexes(params).size(), args.size())) {
        return;
      }
      verifyParameters(mit, args, params);
    }
  }

  @Override
  protected void handleMessageFormat(MethodInvocationTree mit, String formatString, List<ExpressionTree> args) {
    String newFormatString = cleanupDoubleQuote(formatString);
    Set<Integer> indexes = getMessageFormatIndexes(newFormatString, mit);
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
    if (checkArgumentNumber(mit, indexes.size(), newArgs.size())
      || checkUnbalancedQuotes(mit, newFormatString)
      || checkUnbalancedBraces(mit, newFormatString)) {
      return;
    }
    verifyParameters(mit, newArgs, indexes);
  }

  @Override
  protected void handleOtherFormatTree(MethodInvocationTree mit, ExpressionTree formatTree) {
    // do nothing
  }

  private boolean checkArgumentNumber(MethodInvocationTree mit, int nbReadParams, int nbArgs) {
    if (nbReadParams > nbArgs) {
      reportIssue(mit, "Not enough arguments.");
      return true;
    }
    return false;
  }

  private boolean checkUnbalancedQuotes(MethodInvocationTree mit, String formatString) {
    if(LEVELS.contains(mit.symbol().name())) {
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


  private void verifyParameters(MethodInvocationTree mit, List<ExpressionTree> args, Set<Integer> indexes) {
    for (int index : indexes) {
      if (index >= args.size()) {
        reportIssue(mit, "Not enough arguments.");
        return;
      }
    }
  }

  private void verifyParameters(MethodInvocationTree mit, List<ExpressionTree> args, List<String> params) {
    int index = 0;
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

  @Override
  protected void reportMissingPrevious(MethodInvocationTree mit) {
    reportIssue(mit, "The argument index '<' refers to the previous format specifier but there isn't one.");
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
