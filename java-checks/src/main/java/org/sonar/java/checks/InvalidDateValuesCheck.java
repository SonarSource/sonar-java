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

import com.google.common.collect.ImmutableList;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

@Rule(key = "S2110")
public class InvalidDateValuesCheck extends AbstractMethodDetection {

  public static final String JAVA_UTIL_CALENDAR = "java.util.Calendar";
  public static final String JAVA_UTIL_DATE = "java.util.Date";
  public static final String JAVA_SQL_DATE = "java.sql.Date";

  private static final String[] GREGORIAN_PARAMETERS = {"year", "month", "dayOfMonth", "hourOfDay", "minute", "second"};
  private static final String[] DATE_GET_METHODS = {"getDate", "getMonth", "getHours", "getMinutes", "getSeconds"};
  private static final String[] DATE_SET_METHODS = {"setDate", "setMonth", "setHours", "setMinutes", "setSeconds"};

  private static final List<MethodMatcher> DATE_METHODS_COMPARISON = ImmutableList.<MethodMatcher>builder()
    .add(MethodMatcher.create().typeDefinition(JAVA_UTIL_CALENDAR).name("get").addParameter("int"))
    .addAll(dateGetMatchers())
    .build();

  private static List<MethodMatcher> dateGetMatchers() {
    ImmutableList.Builder<MethodMatcher> builder = ImmutableList.builder();
    for (String dateGetMethod : DATE_GET_METHODS) {
      builder.add(dateMethodInvocationMatcherGetter(JAVA_UTIL_DATE, dateGetMethod));
      builder.add(dateMethodInvocationMatcherGetter(JAVA_SQL_DATE, dateGetMethod));
    }
    return builder.build();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    ArrayList<Tree.Kind> kinds = new ArrayList<>(super.nodesToVisit());
    kinds.add(Tree.Kind.EQUAL_TO);
    kinds.add(Tree.Kind.NOT_EQUAL_TO);
    return kinds;
  }

  @Override
  public void visitNode(Tree tree) {
    super.visitNode(tree);
    if (hasSemantic() && tree.is(Tree.Kind.EQUAL_TO, Tree.Kind.NOT_EQUAL_TO)) {
      BinaryExpressionTree binaryExpressionTree = (BinaryExpressionTree) tree;
      String name = getThresholdToCheck(binaryExpressionTree.leftOperand());
      ExpressionTree argToCheck = null;
      if (name == null) {
        name = getThresholdToCheck(binaryExpressionTree.rightOperand());
        if (name != null) {
          argToCheck = binaryExpressionTree.leftOperand();
        }
      } else {
        argToCheck = binaryExpressionTree.rightOperand();
      }
      if (argToCheck != null) {
        checkArgument(argToCheck, name, "\"{0}\" is not a valid value for \"{1}\".");
      }
    }
  }

  @CheckForNull
  private static String getThresholdToCheck(ExpressionTree tree) {
    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      String name = getMethodName(mit);
      for (MethodMatcher methodInvocationMatcher : DATE_METHODS_COMPARISON) {
        if (methodInvocationMatcher.matches(mit)) {
          return getName(mit, name);
        }
      }
    }
    return null;
  }

  @CheckForNull
  private static String getName(MethodInvocationTree mit, String name) {
    if ("get".equals(name)) {
      return getReferencedCalendarName(mit.arguments().get(0));
    }
    return name;
  }

  @CheckForNull
  private static String getReferencedCalendarName(ExpressionTree argument) {
    Symbol reference = null;
    if (argument.is(Tree.Kind.MEMBER_SELECT)) {
      reference = ((MemberSelectExpressionTree) argument).identifier().symbol();
    } else if (argument.is(Tree.Kind.IDENTIFIER)) {
      reference = ((IdentifierTree) argument).symbol();
    }
    if (reference != null &&
        reference.owner().type().is(JAVA_UTIL_CALENDAR) &&
        Threshold.getThreshold(reference.name()) != null) {
      return reference.name();
    }
    return null;
  }

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    ArrayList<MethodMatcher> matchers = new ArrayList<>();
    for (String dateSetMethod : DATE_SET_METHODS) {
      matchers.add(dateMethodInvocationMatcherSetter(JAVA_UTIL_DATE, dateSetMethod));
      matchers.add(dateMethodInvocationMatcherSetter(JAVA_SQL_DATE, dateSetMethod));
    }
    matchers.add(MethodMatcher.create().typeDefinition(JAVA_UTIL_CALENDAR).name("set").addParameter("int").addParameter("int"));
    matchers.add(MethodMatcher.create().typeDefinition("java.util.GregorianCalendar").name("<init>").withAnyParameters());
    return matchers;
  }

  private static MethodMatcher dateMethodInvocationMatcherGetter(String type, String methodName) {
    return MethodMatcher.create().typeDefinition(type).name(methodName).withoutParameter();
  }

  private static MethodMatcher dateMethodInvocationMatcherSetter(String type, String methodName) {
    return MethodMatcher.create().typeDefinition(type).name(methodName).addParameter("int");
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    String name = getMethodName(mit);
    Arguments arguments = mit.arguments();
    if ("set".equals(name)) {
      // Calendar method
      ExpressionTree arg0 = arguments.get(0);
      ExpressionTree arg1 = arguments.get(1);
      String referenceName = getReferencedCalendarName(arg0);
      if (referenceName != null) {
        checkArgument(arg1, referenceName, "\"{0}\" is not a valid value for setting \"{1}\".");
      }
    } else if ("<init>".equals(mit.symbol().name())) {
      // call to this() or super()
      checkConstructorArguments(mit.arguments());
    } else {
      checkArgument(arguments.get(0), name, "\"{0}\" is not a valid value for \"{1}\" method.");
    }
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    checkConstructorArguments(newClassTree.arguments());
  }

  private void checkConstructorArguments(Arguments arguments) {
    // Gregorian Calendar : simply ignore miliseconds, which could be a 7th parameter of the constructor
    int numberArgsToCheck = Math.min(arguments.size(), GREGORIAN_PARAMETERS.length);
    // Gregorian Calendar : ignore first argument: year.
    for (int i = 1; i < numberArgsToCheck; i++) {
      checkArgument(arguments.get(i), GREGORIAN_PARAMETERS[i], "\"{0}\" is not a valid value for setting \"{1}\".");
    }
  }

  private void checkArgument(ExpressionTree arg, String name, String message) {
    LiteralTree literal = null;
    int sign = 1;
    if (arg.is(Tree.Kind.INT_LITERAL)) {
      literal = (LiteralTree) arg;
    } else if (arg.is(Tree.Kind.UNARY_MINUS, Tree.Kind.UNARY_PLUS) && ((UnaryExpressionTree) arg).expression().is(Tree.Kind.INT_LITERAL)) {
      if (arg.is(Tree.Kind.UNARY_MINUS)) {
        sign = -1;
      }
      literal = (LiteralTree) ((UnaryExpressionTree) arg).expression();
    }
    if (literal != null) {
      int argValue = Integer.parseInt(literal.value()) * sign;
      if (argValue > Threshold.getThreshold(name) || argValue < 0) {
        reportIssue(arg, MessageFormat.format(message, argValue, name));
      }
    }
  }

  private static String getMethodName(MethodInvocationTree mit) {
    return ExpressionUtils.methodName(mit).name();
  }

  private enum Threshold {
    MONTH(11, "setMonth", "getMonth", "MONTH", "month"),
    DATE(31, "setDate", "getDate", "DAY_OF_MONTH", "dayOfMonth"),
    HOURS(23, "setHours", "getHours", "HOUR_OF_DAY", "hourOfDay"),
    MINUTE(60, "setMinutes", "getMinutes", "MINUTE", "minute"),
    SECOND(61, "setSeconds", "getSeconds", "SECOND", "second");

    private static Map<String, Integer> thresholdByName = new HashMap<>();

    static {
      for (Threshold value : Threshold.values()) {
        thresholdByName.put(value.javaDateSetter, value.edgeValue);
        thresholdByName.put(value.javaDateGetter, value.edgeValue);
        thresholdByName.put(value.calendarConstant, value.edgeValue);
        thresholdByName.put(value.gregorianParam, value.edgeValue);
      }
    }

    private final int edgeValue;
    private final String javaDateSetter;
    private final String javaDateGetter;
    private final String calendarConstant;
    private final String gregorianParam;

    Threshold(int edgeValue, String javaDateSetter, String javaDateGetter, String calendarConstant, String gregorianParam) {
      this.edgeValue = edgeValue;
      this.javaDateSetter = javaDateSetter;
      this.javaDateGetter = javaDateGetter;
      this.calendarConstant = calendarConstant;
      this.gregorianParam = gregorianParam;
    }

    public static Integer getThreshold(String name) {
      return thresholdByName.get(name);
    }
  }

}
