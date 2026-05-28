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
import java.util.Objects;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.java.reporting.JavaTextEdit;

@Rule(key = "S8694")
public class DateEnumsCheck extends AbstractMethodDetection implements JavaVersionAwareVisitor {
  private static final String JAVA_TIME_MONTH = "java.time.Month";
  private static final String JAVA_TIME_DAY_OF_WEEK = "java.time.DayOfWeek";

  private static final MethodMatchers METHOD_WITH_MONTH_AS_SECOND_ARGUMENT = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes("java.time.LocalDate")
      .names("of")
      .addParametersMatcher("int", "int", "int")
      .build(),
    MethodMatchers.create()
      .ofTypes("java.time.LocalDateTime")
      .names("of")
      .addParametersMatcher("int", "int", "int", "int", "int")
      .addParametersMatcher("int", "int", "int", "int", "int", "int")
      .addParametersMatcher("int", "int", "int", "int", "int", "int", "int")
      .build(),
    MethodMatchers.create()
      .ofTypes("java.time.YearMonth")
      .names("of")
      .addParametersMatcher("int", "int")
      .build());

  private static final MethodMatchers MONTH_DAY_OF_MATCHER = MethodMatchers.create()
    .ofTypes("java.time.MonthDay")
    .names("of")
    .addParametersMatcher("int", "int")
    .build();

  private static final MethodMatchers MONTH_OF_MATCHER = MethodMatchers.create()
    .ofTypes(JAVA_TIME_MONTH)
    .names("of")
    .addParametersMatcher("int")
    .build();

  private static final MethodMatchers DAY_OF_WEEK_OF_MATCHER = MethodMatchers.create()
    .ofTypes(JAVA_TIME_DAY_OF_WEEK)
    .names("of")
    .addParametersMatcher("int")
    .build();

  private static final MethodMatchers GET_MONTH_VALUE_MATCHER = MethodMatchers.create()
    .ofTypes("java.time.LocalDate", "java.time.LocalDateTime", "java.time.OffsetDateTime", "java.time.ZonedDateTime",
      "java.time.YearMonth", "java.time.MonthDay")
    .names("getMonthValue")
    .addWithoutParametersMatcher()
    .build();

  private static final MethodMatchers MONTH_GET_VALUE_MATCHER = MethodMatchers.create()
    .ofTypes(JAVA_TIME_MONTH)
    .names("getValue")
    .addWithoutParametersMatcher()
    .build();

  private static final MethodMatchers DAY_OF_WEEK_GET_VALUE_MATCHER = MethodMatchers.create()
    .ofTypes(JAVA_TIME_DAY_OF_WEEK)
    .names("getValue")
    .addWithoutParametersMatcher()
    .build();

  private static final String MONTH_ISSUE_MESSAGE = "Use a \"java.time.Month\" enum constant instead of this int literal.";
  private static final String DAY_ISSUE_MESSAGE = "Use a \"java.time.DayOfWeek\" enum constant instead of this int literal.";
  private QuickFixHelper.ImportSupplier importSupplier;


  @Override
  public void setContext(JavaFileScannerContext context) {
    super.setContext(context);
    importSupplier = null;
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    importSupplier = null;
  }

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava8Compatible();
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(METHOD_WITH_MONTH_AS_SECOND_ARGUMENT, MONTH_OF_MATCHER, MONTH_DAY_OF_MATCHER, DAY_OF_WEEK_OF_MATCHER);
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
    if (METHOD_WITH_MONTH_AS_SECOND_ARGUMENT.matches(mit)) {
      ExpressionTree secondArgument = mit.arguments().get(1);
      int secondArgumentLiteral = getIntLiteral(secondArgument);
      if (isValidMonth(secondArgumentLiteral)) {
        reportAndCreateQuickfix(secondArgument, getMonthEnumName(secondArgumentLiteral), MONTH_ISSUE_MESSAGE, JAVA_TIME_MONTH);
        return;
      }
    }
    ExpressionTree firstArgument = mit.arguments().get(0);
    int firstArgumentLiteral = getIntLiteral(firstArgument);
    if (DAY_OF_WEEK_OF_MATCHER.matches(mit)
      && isValidDay(firstArgumentLiteral)) {
      reportAndCreateQuickfix(mit, getDayOfWeekEnumName(firstArgumentLiteral), DAY_ISSUE_MESSAGE, JAVA_TIME_DAY_OF_WEEK);
      return;
    }
    if (MONTH_OF_MATCHER.matches(mit)
      && isValidMonth(firstArgumentLiteral)) {
      reportAndCreateQuickfix(mit, getMonthEnumName(firstArgumentLiteral), MONTH_ISSUE_MESSAGE, JAVA_TIME_MONTH);
      return;
    }
    if (MONTH_DAY_OF_MATCHER.matches(mit)
      && isValidMonth(firstArgumentLiteral)) {
      reportAndCreateQuickfix(firstArgument, getMonthEnumName(firstArgumentLiteral), MONTH_ISSUE_MESSAGE, JAVA_TIME_MONTH);
    }
  }

  private void reportAndCreateQuickfix(ExpressionTree arg, String replacement, String issueMessage, String importName) {
    QuickFixHelper.newIssue(context)
      .forRule(this)
      .onTree(arg)
      .withMessage(issueMessage)
      .withQuickFix(() -> computeQuickfix(arg, replacement, importName))
      .report();
  }

  private JavaQuickFix computeQuickfix(Tree replacedTree, String replacement, String importName) {
    JavaQuickFix.Builder builder = JavaQuickFix.newQuickFix(String.format("Replace with %s.", replacement))
      .addTextEdit(JavaTextEdit.replaceTree(replacedTree, replacement));
    if (importSupplier == null) {
      importSupplier = QuickFixHelper.newImportSupplier(context);
    }
    importSupplier.newImportEdit(importName).ifPresent(builder::addTextEdit);
    return builder.build();
  }

  private static String getMonthEnumName(int month) {
    String[] monthNames = {"JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE",
      "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"};
    return "Month." + monthNames[month - 1];
  }

  private static String getDayOfWeekEnumName(int day) {
    String[] dayOfWeekNames = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY",
      "SUNDAY"};
    return "DayOfWeek." + dayOfWeekNames[day - 1];
  }

  @Override
  public void visitNode(Tree tree) {
    super.visitNode(tree);
    if (tree.is(Tree.Kind.EQUAL_TO, Tree.Kind.NOT_EQUAL_TO)) {
      BinaryExpressionTree binaryExpressionTree = (BinaryExpressionTree) tree;
      ExpressionTree leftOperand = binaryExpressionTree.leftOperand();
      ExpressionTree rightOperand = binaryExpressionTree.rightOperand();
      // Check if left is method call and right is literal
      if (leftOperand instanceof MethodInvocationTree mit) {
        checkComparison(binaryExpressionTree, mit, rightOperand, false);
        return;
      }
      // Check if right is method call and left is literal
      if (rightOperand instanceof MethodInvocationTree mit) {
        checkComparison(binaryExpressionTree, mit, leftOperand, true);
      }
    }
  }

  private void checkComparison(BinaryExpressionTree binaryExpressionTree,
    MethodInvocationTree methodInvocationSide, ExpressionTree literalSide, boolean isReversed) {
    int intLiteral = getIntLiteral(literalSide);
    if (intLiteral != -1) {
      if (GET_MONTH_VALUE_MATCHER.matches(methodInvocationSide) && isValidMonth(intLiteral)) {
        reportAndCreateQuickfix(binaryExpressionTree, getMonthValueReplacement(methodInvocationSide,
          binaryExpressionTree, intLiteral, isReversed), MONTH_ISSUE_MESSAGE, JAVA_TIME_MONTH);
        return;
      }
      if (MONTH_GET_VALUE_MATCHER.matches(methodInvocationSide) && isValidMonth(intLiteral)) {
        reportAndCreateQuickfix(binaryExpressionTree, getValueReplacement(methodInvocationSide,
          binaryExpressionTree, getMonthEnumName(intLiteral), isReversed), MONTH_ISSUE_MESSAGE, JAVA_TIME_MONTH);
        return;
      }
      if (DAY_OF_WEEK_GET_VALUE_MATCHER.matches(methodInvocationSide) && isValidDay(intLiteral)) {
        reportAndCreateQuickfix(binaryExpressionTree, getValueReplacement(methodInvocationSide,
          binaryExpressionTree, getDayOfWeekEnumName(intLiteral), isReversed), DAY_ISSUE_MESSAGE, JAVA_TIME_DAY_OF_WEEK);
      }
    }
  }

  private String getMonthValueReplacement(MethodInvocationTree methodInvocationSide, BinaryExpressionTree binaryExpressionTree, int literal, boolean isReversed) {
    ExpressionTree receiver = ((MemberSelectExpressionTree) methodInvocationSide.methodSelect()).expression();
    String receiverText = QuickFixHelper.contentForTree(receiver, context);
    String enumName = getMonthEnumName(literal);
    String replacement = isReversed ? (String.format("%s.equals(%s.getMonth())", enumName, receiverText))
      : (String.format("%s.getMonth().equals(%s)", receiverText, enumName));
    return binaryExpressionTree.is(Tree.Kind.NOT_EQUAL_TO) ? ("!" + replacement) : replacement;
  }

  private String getValueReplacement(MethodInvocationTree methodInvocationSide, BinaryExpressionTree binaryExpressionTree, String enumName, boolean isReversed) {
    ExpressionTree receiver = ((MemberSelectExpressionTree) methodInvocationSide.methodSelect()).expression();
    String receiverText = QuickFixHelper.contentForTree(receiver, context);
    String replacement = isReversed ? (String.format("%s.equals(%s)", enumName, receiverText))
      : (String.format("%s.equals(%s)", receiverText, enumName));
    return binaryExpressionTree.is(Tree.Kind.NOT_EQUAL_TO) ? ("!" + replacement) : replacement;
  }

  private static int getIntLiteral(ExpressionTree arg) {
    if (arg.is(Tree.Kind.INT_LITERAL)) {
      return Objects.requireNonNull(LiteralUtils.intLiteralValue(arg));
    }
    return -1;
  }

  private static boolean isValidMonth(int month) {
    return month >= 1 && month <= 12;
  }

  private static boolean isValidDay(int day) {
    return day >= 1 && day <= 7;
  }
}
