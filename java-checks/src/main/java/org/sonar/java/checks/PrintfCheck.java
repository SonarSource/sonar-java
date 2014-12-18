/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.checks.methods.MethodInvocationMatcher;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Rule(
    key = "S2275",
    priority = Priority.CRITICAL,
    tags = {"bug", "pitfall"})
@BelongsToProfile(title = "Sonar way", priority = Priority.CRITICAL)
public class PrintfCheck extends AbstractMethodDetection {

  private static final Pattern pattern = Pattern.compile("%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])");
  private static final Set<String> TIME_CONVERSIONS = Sets.newHashSet(
      "H", "I", "k", "l", "M", "S", "L", "N", "p", "z", "Z", "s", "Q",
      "B", "b", "h", "A", "a", "C", "Y", "y", "j", "m", "d", "e",
      "R", "T", "r", "D", "F", "c"
  );

  @Override
  protected List<MethodInvocationMatcher> getMethodInvocationMatchers() {
    return ImmutableList.of(
        MethodInvocationMatcher.create().typeDefinition("java.lang.String").name("format").withNoParameterConstraint(),
        MethodInvocationMatcher.create().typeDefinition("java.util.Formatter").name("format").withNoParameterConstraint(),
        MethodInvocationMatcher.create().typeDefinition("java.io.PrintStream").name("format").withNoParameterConstraint(),
        MethodInvocationMatcher.create().typeDefinition("java.io.PrintStream").name("printf").withNoParameterConstraint(),
        MethodInvocationMatcher.create().typeDefinition("java.io.PrintWriter").name("format").withNoParameterConstraint(),
        MethodInvocationMatcher.create().typeDefinition("java.io.PrintWriter").name("printf").withNoParameterConstraint()
    );
  }

  @Override
  protected void onMethodFound(MethodInvocationTree mit) {
    ExpressionTree formatStringTree;
    List<ExpressionTree> args;
    //Check type of first argument:
    if (((AbstractTypedTree) mit.arguments().get(0)).getSymbolType().is("java.lang.String")) {
      formatStringTree = mit.arguments().get(0);
      args = mit.arguments().subList(1, mit.arguments().size());
    } else {
      //format method with "Locale" first argument, skip that one.
      formatStringTree = mit.arguments().get(1);
      args = mit.arguments().subList(2, mit.arguments().size());
    }
    if (formatStringTree.is(Tree.Kind.STRING_LITERAL)) {
      String formatString = trimQuotes(((LiteralTree) formatStringTree).value());
      List<String> params = getParameters(formatString);

      if (formatString.contains("\\n")) {
        addIssue(mit, "%n should be used in place of \\n to produce the platform-specific line separator.");
      }

      if (firstArgumentIsLT(params)) {
        addIssue(mit, "The argument index '<' refers to the previous format specifier but there isn't one.");
        return;
      }

      if (usesMessageFormat(formatString, params)) {
        addIssue(mit, "Looks like there is a confusion with the use of java.text.MessageFormat, parameters will be simply ignored here");
        return;
      }

      //Cleanup %n values
      while(params.contains("n")) {
        params.remove("n");
      }
      if (params.size() > args.size()) {
        addIssue(mit, "Not enough arguments.");
        return;
      }

      int index = 0;
      List<ExpressionTree> unusedArgs = Lists.newArrayList(args);
      for (String rawParam : params) {
        String param = rawParam;
        int argIndex = index;
        if (param.contains("$")) {
          argIndex = Integer.valueOf(param.substring(0, param.indexOf("$"))) - 1;
          param = param.substring(param.indexOf("$") + 1);
        } else {
          index++;
        }
        ExpressionTree argExpressionTree = args.get(argIndex);
        unusedArgs.remove(argExpressionTree);
        Type argType = ((AbstractTypedTree) argExpressionTree).getSymbolType();
        if (param.startsWith("d") && !isNumerical(argType)) {
          addIssue(mit, "An 'int' is expected rather than a " + argType + ".");
        }
        if (param.startsWith("b") && !(argType.is("boolean") || argType.is("java.lang.Boolean"))) {
          addIssue(mit, "Directly inject the boolean value.");
        }
        if((param.startsWith("t") || param.startsWith("T")) && !TIME_CONVERSIONS.contains(param.substring(1))) {
          addIssue(mit, param.substring(1)+" is not a supported time conversion character");
        }

      }
      reportUnusedArgs(mit, args, unusedArgs);
    }
  }

  private boolean isNumerical(Type argType) {
    return argType.isNumerical()
        || argType.is("java.math.BigInteger")
        || argType.is("java.math.BigDecimal")
        || argType.is("java.lang.Byte")
        || argType.is("java.lang.Short")
        || argType.is("java.lang.Integer")
        || argType.is("java.lang.Long")
        || argType.is("java.lang.Float")
        || argType.is("java.lang.Double")
        ;
  }

  private boolean usesMessageFormat(String formatString, List<String> params) {
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
        stringArgIndex = i + "th";
      }
      addIssue(mit, stringArgIndex + " argument is not used.");
    }
  }

  private List<String> getParameters(String formatString) {
    List<String> params = Lists.newArrayList();
    Matcher matcher = pattern.matcher(formatString);
    while (matcher.find()) {
      //remove starting %
      params.add(matcher.group().substring(1));
    }
    return params;
  }

  private boolean firstArgumentIsLT(List<String> params) {
    return !params.isEmpty() && params.get(0).startsWith("<");
  }

  private String trimQuotes(String value) {
    return value.substring(1, value.length() - 1);
  }
}
