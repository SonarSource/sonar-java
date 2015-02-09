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
import com.google.common.collect.Maps;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.checks.methods.MethodInvocationMatcher;
import org.sonar.java.resolve.Symbol;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.Nullable;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

@Rule(
  key = "S2110",
  name = "Invalid \"Date\" values should not be used",
  tags = {"bug"},
  priority = Priority.CRITICAL)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.INSTRUCTION_RELIABILITY)
@SqaleConstantRemediation("5min")
@ActivatedByDefault
public class InvalidDateValuesCheck extends AbstractMethodDetection {

  private static final String[] GREGORIAN_PARAMETERS = {"year", "month", "dayOfMonth", "hourOfDay", "minute", "second"};

  private static final List<MethodInvocationMatcher> DATE_METHODS_COMPARISON = ImmutableList.<MethodInvocationMatcher>builder()
    .add(MethodInvocationMatcher.create().typeDefinition("java.util.Calendar").name("get").addParameter("int"))
    .add(dateMethodInvocationMatcherGetter("java.util.Date", "getDate"))
    .add(dateMethodInvocationMatcherGetter("java.util.Date", "getMonth"))
    .add(dateMethodInvocationMatcherGetter("java.util.Date", "getHours"))
    .add(dateMethodInvocationMatcherGetter("java.util.Date", "getMinutes"))
    .add(dateMethodInvocationMatcherGetter("java.util.Date", "getSeconds"))
    .add(dateMethodInvocationMatcherGetter("java.sql.Date", "getDate"))
    .add(dateMethodInvocationMatcherGetter("java.sql.Date", "getMonth"))
    .add(dateMethodInvocationMatcherGetter("java.sql.Date", "getHours"))
    .add(dateMethodInvocationMatcherGetter("java.sql.Date", "getMinutes"))
    .add(dateMethodInvocationMatcherGetter("java.sql.Date", "getSeconds"))
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.<Tree.Kind>builder().addAll(super.nodesToVisit())
      .add(Tree.Kind.EQUAL_TO)
      .add(Tree.Kind.NOT_EQUAL_TO)
      .build();
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

  @Nullable
  private String getThresholdToCheck(ExpressionTree tree) {
    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      String name = getMethodName(mit);
      for (MethodInvocationMatcher methodInvocationMatcher : DATE_METHODS_COMPARISON) {
        if (methodInvocationMatcher.matches(mit, getSemanticModel())) {
          if (name.equals("get")) {
            // Calendar
            ExpressionTree arg0 = mit.arguments().get(0);
            if (arg0.is(Tree.Kind.MEMBER_SELECT)) {
              MemberSelectExpressionTree mse = (MemberSelectExpressionTree) arg0;
              Symbol reference = getSemanticModel().getReference(mse.identifier());
              if (reference.owner().getType().is("java.util.Calendar") && Threshold.getThreshold(reference.getName()) != null) {
                return reference.getName();
              }
            }
          } else {
            return name;
          }
        }
      }
    }
    return null;
  }

  @Override
  protected List<MethodInvocationMatcher> getMethodInvocationMatchers() {
    return ImmutableList.<MethodInvocationMatcher>builder()
      .add(dateMethodInvocationMatcher("java.util.Date", "setDate"))
      .add(dateMethodInvocationMatcher("java.util.Date", "setMonth"))
      .add(dateMethodInvocationMatcher("java.util.Date", "setHours"))
      .add(dateMethodInvocationMatcher("java.util.Date", "setMinutes"))
      .add(dateMethodInvocationMatcher("java.util.Date", "setSeconds"))
      .add(dateMethodInvocationMatcher("java.sql.Date", "setDate"))
      .add(dateMethodInvocationMatcher("java.sql.Date", "setMonth"))
      .add(dateMethodInvocationMatcher("java.sql.Date", "setHours"))
      .add(dateMethodInvocationMatcher("java.sql.Date", "setMinutes"))
      .add(dateMethodInvocationMatcher("java.sql.Date", "setSeconds"))
      .add(MethodInvocationMatcher.create().typeDefinition("java.util.Calendar").name("set").addParameter("int").addParameter("int"))
      .add(MethodInvocationMatcher.create().typeDefinition("java.util.GregorianCalendar").name("<init>").withNoParameterConstraint())
      .build();
  }

  private static MethodInvocationMatcher dateMethodInvocationMatcherGetter(String type, String methodName) {
    return MethodInvocationMatcher.create().typeDefinition(type).name(methodName);
  }

  private static MethodInvocationMatcher dateMethodInvocationMatcher(String type, String methodName) {
    return MethodInvocationMatcher.create().typeDefinition(type).name(methodName).addParameter("int");
  }

  @Override
  protected void onMethodFound(MethodInvocationTree mit) {
    String name = getMethodName(mit);
    if (name.equals("set")) {
      // Calendar method
      ExpressionTree arg0 = mit.arguments().get(0);
      ExpressionTree arg1 = mit.arguments().get(1);
      if (arg0.is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree mse = (MemberSelectExpressionTree) arg0;
        Symbol reference = getSemanticModel().getReference(mse.identifier());
        if (reference.owner().getType().is("java.util.Calendar") && Threshold.getThreshold(reference.getName()) != null) {
          checkArgument(arg1, reference.getName(), "\"{0}\" is not a valid value for setting \"{1}\".");
        }
      }
    } else {
      ExpressionTree arg = mit.arguments().get(0);
      checkArgument(arg, name, "\"{0}\" is not a valid value for \"{1}\" method.");
    }
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    // Gregorian Calendar : ignore first argument: year.
    for (int i = 1; i < newClassTree.arguments().size(); i++) {
      checkArgument(newClassTree.arguments().get(i), GREGORIAN_PARAMETERS[i], "\"{0}\" is not a valid value for setting \"{1}\".");
    }
  }

  private void checkArgument(ExpressionTree arg, String name, String message) {
    LiteralTree literal = null;
    int sign = 1;
    if (arg.is(Tree.Kind.INT_LITERAL)) {
      literal = (LiteralTree) arg;
    } else if (arg.is(Tree.Kind.UNARY_MINUS) && ((UnaryExpressionTree) arg).expression().is(Tree.Kind.INT_LITERAL)) {
      sign = -1;
      literal = (LiteralTree) ((UnaryExpressionTree) arg).expression();
    }
    if (literal != null) {
      int argValue = Integer.parseInt(literal.value()) * sign;
      if (argValue > Threshold.getThreshold(name) || argValue < 0) {
        addIssue(arg, MessageFormat.format(message, argValue, name));
      }
    }
  }

  private String getMethodName(MethodInvocationTree mit) {
    if (mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      return ((MemberSelectExpressionTree) mit.methodSelect()).identifier().name();
    }
    return ((IdentifierTree) mit.methodSelect()).name();
  }

  private static enum Threshold {
    MONTH(11, "setMonth", "getMonth", "MONTH", "month"),
    DATE(31, "setDate", "getDate", "DAY_OF_MONTH", "dayOfMonth"),
    HOURS(23, "setHours", "getHours", "HOUR_OF_DAY", "hourOfDay"),
    MINUTE(60, "setMinutes", "getMinutes", "MINUTE", "minute"),
    SECOND(61, "setSeconds", "getSeconds", "SECOND", "second");

    private static Map<String, Integer> thresholdByName = null;

    private final int threshold;
    private final String javaDateSetter;
    private final String javaDateGetter;
    private final String calendarConstant;
    private final String gregorianParam;

    Threshold(int threshold, String javaDateSetter, String javaDateGetter, String calendarConstant, String gregorianParam) {
      this.threshold = threshold;
      this.javaDateSetter = javaDateSetter;
      this.javaDateGetter = javaDateGetter;
      this.calendarConstant = calendarConstant;
      this.gregorianParam = gregorianParam;
    }

    public static Integer getThreshold(String name) {
      if (thresholdByName == null) {
        thresholdByName = Maps.newHashMap();
        for (Threshold value : Threshold.values()) {
          thresholdByName.put(value.javaDateSetter, value.threshold);
          thresholdByName.put(value.javaDateGetter, value.threshold);
          thresholdByName.put(value.calendarConstant, value.threshold);
          thresholdByName.put(value.gregorianParam, value.threshold);
        }
      }
      return thresholdByName.get(name);
    }
  }

}
