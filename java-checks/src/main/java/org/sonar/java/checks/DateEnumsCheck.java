/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import java.util.ArrayList;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S8694")
public class DateEnumsCheck extends AbstractMethodDetection implements JavaVersionAwareVisitor {

  private static final MethodMatchers LOCAL_DATE_OF_MATCHER = MethodMatchers.create()
    .ofTypes("java.time.LocalDate")
    .names("of")
    .addParametersMatcher("int", "int", "int")
    .build();

  private static final MethodMatchers LOCAL_DATE_TIME_OF_MATCHER = MethodMatchers.create()
    .ofTypes("java.time.LocalDateTime")
    .names("of")
    .addParametersMatcher("int", "int", "int", "int", "int")
    .addParametersMatcher("int", "int", "int", "int", "int", "int")
    .addParametersMatcher("int", "int", "int", "int", "int", "int", "int")
    .build();

  private static final MethodMatchers OFFSET_DATE_TIME_OF_MATCHER = MethodMatchers.create()
    .ofTypes("java.time.OffsetDateTime")
    .names("of")
    .addParametersMatcher("int", "int", "int", "int", "int", "int", "int", "java.time.ZoneOffset")
    .build();

  private static final MethodMatchers ZONED_DATE_TIME_OF_MATCHER = MethodMatchers.create()
    .ofTypes("java.time.ZonedDateTime")
    .names("of")
    .addParametersMatcher("int", "int", "int", "int", "int", "int", "int", "java.time.ZoneId")
    .build();

  private static final MethodMatchers YEAR_MONTH_OF_MATCHER = MethodMatchers.create()
    .ofTypes("java.time.YearMonth")
    .names("of")
    .addParametersMatcher("int", "int")
    .build();

  private static final MethodMatchers MONTH_DAY_OF_MATCHER = MethodMatchers.create()
    .ofTypes("java.time.MonthDay")
    .names("of")
    .addParametersMatcher("int", "int")
    .build();

  private static final MethodMatchers DAY_OF_WEEK_OF_MATCHER = MethodMatchers.create()
    .ofTypes("java.time.DayOfWeek")
    .names("of")
    .addParametersMatcher("int")
    .build();

  private static final MethodMatchers MONTH_OF_MATCHER = MethodMatchers.create()
    .ofTypes("java.time.Month")
    .names("of")
    .addParametersMatcher("int")
    .build();

  private static final MethodMatchers DATE_OF_MATCHERS = MethodMatchers.or(LOCAL_DATE_OF_MATCHER, LOCAL_DATE_TIME_OF_MATCHER, OFFSET_DATE_TIME_OF_MATCHER,
    ZONED_DATE_TIME_OF_MATCHER, YEAR_MONTH_OF_MATCHER);

  private static final MethodMatchers GET_MONTH_VALUE_MATCHER = MethodMatchers.create()
    .ofTypes("java.time.LocalDate", "java.time.LocalDateTime", "java.time.OffsetDateTime", "java.time.ZonedDateTime",
      "java.time.YearMonth", "java.time.MonthDay")
    .names("getMonthValue")
    .addWithoutParametersMatcher()
    .build();

  private static final MethodMatchers MONTH_GET_VALUE_MATCHER = MethodMatchers.create()
    .ofTypes("java.time.Month")
    .names("getValue")
    .addWithoutParametersMatcher()
    .build();

  private static final MethodMatchers DAY_OF_WEEK_GET_VALUE_MATCHER = MethodMatchers.create()
    .ofTypes("java.time.DayOfWeek")
    .names("getValue")
    .addWithoutParametersMatcher()
    .build();

  private static final String MONTH_ISSUE_MESSAGE = "Use a \"java.time.Month\" enum constant instead of this int literal.";
  private static final String DAY_ISSUE_MESSAGE = "Use a \"java.time.DayOfWeek\" enum constant instead of this int literal.";

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava8Compatible();
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(DATE_OF_MATCHERS, DAY_OF_WEEK_OF_MATCHER, MONTH_OF_MATCHER, MONTH_DAY_OF_MATCHER);
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    List<Tree.Kind> kinds = new ArrayList<>(super.nodesToVisit());
    kinds.add(Tree.Kind.EQUAL_TO);
    kinds.add(Tree.Kind.NOT_EQUAL_TO);
    return kinds;
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (DATE_OF_MATCHERS.matches(mit)) {
      ExpressionTree secondArgument = mit.arguments().get(1);
      if (isIntLiteral(secondArgument)) {
        reportIssue(secondArgument, MONTH_ISSUE_MESSAGE);
        return;
      }
    }
    ExpressionTree firstArgument = mit.arguments().get(0);
    if (DAY_OF_WEEK_OF_MATCHER.matches(mit)
      && isIntLiteral(firstArgument)) {
      reportIssue(firstArgument, DAY_ISSUE_MESSAGE);
      return;
    }
    if (MONTH_OF_MATCHER.matches(mit)
      || MONTH_DAY_OF_MATCHER.matches(mit)
      && isIntLiteral(firstArgument)) {
      reportIssue(firstArgument, MONTH_ISSUE_MESSAGE);
    }
  }

  @Override
  public void visitNode(Tree tree) {
    super.visitNode(tree);
    if (tree.is(Tree.Kind.EQUAL_TO, Tree.Kind.NOT_EQUAL_TO)) {
      BinaryExpressionTree binaryExpressionTree = (BinaryExpressionTree) tree;
      ExpressionTree leftOperand = binaryExpressionTree.leftOperand();
      ExpressionTree rightOperand = binaryExpressionTree.rightOperand();
      // Check if left is method call and right is literal
      checkComparison(binaryExpressionTree, leftOperand, rightOperand);
      // Check if right is method call and left is literal
      checkComparison(binaryExpressionTree, rightOperand, leftOperand);
    }
  }

  private void checkComparison(BinaryExpressionTree binaryExpressionTree,  ExpressionTree methodCallSide, ExpressionTree literalSide) {
    if (methodCallSide instanceof MethodInvocationTree mit && isIntLiteral(literalSide)) {
      if (MethodMatchers.or(GET_MONTH_VALUE_MATCHER, MONTH_GET_VALUE_MATCHER).matches(mit)) {
        reportIssue(binaryExpressionTree, MONTH_ISSUE_MESSAGE);
        return;
      }
      if (DAY_OF_WEEK_GET_VALUE_MATCHER.matches(mit)) {
        reportIssue(binaryExpressionTree, DAY_ISSUE_MESSAGE);
      }
    }
  }

  private static boolean isIntLiteral(ExpressionTree arg) {
    if (arg.is(Tree.Kind.INT_LITERAL)) {
      return true;
    }
    if (arg.is(Tree.Kind.UNARY_MINUS, Tree.Kind.UNARY_PLUS)) {
      return ((UnaryExpressionTree) arg).expression().is(Tree.Kind.INT_LITERAL);
    }
    return false;
  }
}
