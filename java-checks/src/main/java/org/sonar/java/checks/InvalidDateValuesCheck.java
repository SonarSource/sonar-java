/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
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

  private static final MethodMatchers DATE_METHODS_COMPARISON = MethodMatchers.or(
    MethodMatchers.create().ofTypes(JAVA_UTIL_CALENDAR).names("get").addParametersMatcher("int").build(),
    // date get matchers
    MethodMatchers.create().ofTypes(JAVA_UTIL_DATE).names(DATE_GET_METHODS).addWithoutParametersMatcher().build(),
    MethodMatchers.create().ofTypes(JAVA_SQL_DATE).names(DATE_GET_METHODS).addWithoutParametersMatcher().build()
  );

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
    if (tree.is(Tree.Kind.EQUAL_TO, Tree.Kind.NOT_EQUAL_TO)) {
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
      if (DATE_METHODS_COMPARISON.matches(mit)) {
        String name = getMethodName(mit);
        return getName(mit, name);
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
      reference.owner().type().is(JAVA_UTIL_CALENDAR) && DateField.containsField(reference.name())) {
      return reference.name();
    }
    return null;
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(
      MethodMatchers.create().ofTypes(JAVA_UTIL_DATE, JAVA_SQL_DATE).names(DATE_SET_METHODS).addParametersMatcher("int").build(),
      MethodMatchers.create().ofTypes(JAVA_UTIL_CALENDAR).names("set").addParametersMatcher("int", "int").build(),
      MethodMatchers.create().ofTypes("java.util.GregorianCalendar").constructor().withAnyParameters().build()
    );
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
    } else if ("<init>".equals(mit.methodSymbol().name())) {
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
      Range range = DateField.getFieldRange(name);
      if (argValue > range.maxValue || argValue < range.minValue) {
        reportIssue(arg, MessageFormat.format(message, argValue, name));
      }
    }
  }

  private static String getMethodName(MethodInvocationTree mit) {
    return ExpressionUtils.methodName(mit).name();
  }

  private enum DateField {
    MONTH(new Range(0, 11), "setMonth", "getMonth", "MONTH", "month"),
    DATE(new Range(1, 31), "setDate", "getDate", "DAY_OF_MONTH", "dayOfMonth"),
    HOURS(new Range(0, 23), "setHours", "getHours", "HOUR_OF_DAY", "hourOfDay"),
    MINUTE(new Range(0, 60), "setMinutes", "getMinutes", "MINUTE", "minute"),
    SECOND(new Range(0, 61), "setSeconds", "getSeconds", "SECOND", "second");

    private static final Map<String, Range> rangeByName = new HashMap<>();

    static {
      for (DateField field : DateField.values()) {
        rangeByName.put(field.javaDateSetter, field.range);
        rangeByName.put(field.javaDateGetter, field.range);
        rangeByName.put(field.calendarConstant, field.range);
        rangeByName.put(field.gregorianParam, field.range);
      }
    }

    private final Range range;
    private final String javaDateSetter;
    private final String javaDateGetter;
    private final String calendarConstant;
    private final String gregorianParam;

    DateField(Range range, String javaDateSetter, String javaDateGetter, String calendarConstant, String gregorianParam) {
      this.range = range;
      this.javaDateSetter = javaDateSetter;
      this.javaDateGetter = javaDateGetter;
      this.calendarConstant = calendarConstant;
      this.gregorianParam = gregorianParam;
    }

    public static Range getFieldRange(String name) {
      return rangeByName.get(name);
    }

    public static boolean containsField(String name) {
      return rangeByName.containsKey(name);
    }
  }

  private static class Range {
    public final int minValue;
    public final int maxValue;

    private Range(int minValue, int maxValue) {
      this.minValue = minValue;
      this.maxValue = maxValue;
    }
  }
}
