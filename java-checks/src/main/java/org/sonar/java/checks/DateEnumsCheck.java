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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.check.Rule;
import org.sonar.java.checks.ConditionalRuleCacheUtils.CachedFileData;
import org.sonar.java.checks.ConditionalRuleCacheUtils.CachedIssue;
import org.sonar.java.checks.ConditionalRuleCacheUtils.ImportEditData;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.DefaultModuleScannerContext;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.InputFileScannerContext;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.ModuleScannerContext;
import org.sonar.plugins.java.api.caching.CacheContext;
import org.sonar.plugins.java.api.internal.EndOfAnalysis;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S8694")
public class DateEnumsCheck extends AbstractMethodDetection implements JavaVersionAwareVisitor, EndOfAnalysis {

  private static final String JAVA_TIME_MONTH = "java.time.Month";
  private static final String JAVA_TIME_DAY_OF_WEEK = "java.time.DayOfWeek";
  private static final String JAVA_TIME_LOCAL_DATE = "java.time.LocalDate";
  private static final String JAVA_TIME_LOCAL_DATE_TIME = "java.time.LocalDateTime";
  private static final String JAVA_TIME_YEAR_MONTH = "java.time.YearMonth";
  private static final String JAVA_TIME_MONTH_DAY = "java.time.MonthDay";
  private static final int RAISED_PERCENTAGE_THRESHOLD = 80;
  private static final String CACHE_KEY_PREFIX = "java:S8694:";

  private static final MethodMatchers METHOD_WITH_MONTH_AS_SECOND_ARGUMENT = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes(JAVA_TIME_LOCAL_DATE)
      .names("of")
      .addParametersMatcher("int", "int", "int")
      .build(),
    MethodMatchers.create()
      .ofTypes(JAVA_TIME_LOCAL_DATE_TIME)
      .names("of")
      .addParametersMatcher("int", "int", "int", "int", "int")
      .addParametersMatcher("int", "int", "int", "int", "int", "int")
      .addParametersMatcher("int", "int", "int", "int", "int", "int", "int")
      .build(),
    MethodMatchers.create()
      .ofTypes(JAVA_TIME_YEAR_MONTH)
      .names("of")
      .addParametersMatcher("int", "int")
      .build());

  // Compliant overloads that accept a Month enum — counted toward total but never raise issues
  private static final MethodMatchers METHOD_WITH_MONTH_ENUM_AS_SECOND_ARGUMENT = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes(JAVA_TIME_LOCAL_DATE)
      .names("of")
      .addParametersMatcher("int", JAVA_TIME_MONTH, "int")
      .build(),
    MethodMatchers.create()
      .ofTypes(JAVA_TIME_LOCAL_DATE_TIME)
      .names("of")
      .addParametersMatcher("int", JAVA_TIME_MONTH, "int", "int", "int")
      .addParametersMatcher("int", JAVA_TIME_MONTH, "int", "int", "int", "int")
      .addParametersMatcher("int", JAVA_TIME_MONTH, "int", "int", "int", "int", "int")
      .build(),
    MethodMatchers.create()
      .ofTypes(JAVA_TIME_YEAR_MONTH)
      .names("of")
      .addParametersMatcher("int", JAVA_TIME_MONTH)
      .build());

  private static final MethodMatchers MONTH_DAY_OF_MATCHER = MethodMatchers.create()
    .ofTypes(JAVA_TIME_MONTH_DAY)
    .names("of")
    .addParametersMatcher("int", "int")
    .build();

  // Compliant overload that accepts a Month enum — counted toward total but never raises issues
  private static final MethodMatchers MONTH_DAY_WITH_ENUM_MATCHER = MethodMatchers.create()
    .ofTypes(JAVA_TIME_MONTH_DAY)
    .names("of")
    .addParametersMatcher(JAVA_TIME_MONTH, "int")
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
    .ofTypes(JAVA_TIME_LOCAL_DATE, JAVA_TIME_LOCAL_DATE_TIME, "java.time.OffsetDateTime", "java.time.ZonedDateTime",
      JAVA_TIME_YEAR_MONTH, JAVA_TIME_MONTH_DAY)
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

  // Per-file state — reset at setContext, consumed at leaveFile
  private int currentFileTotalCount;
  private int currentFileNoEnumCount;
  private final List<CachedIssue> currentFileIssues = new ArrayList<>();

  // Project-level accumulators — contributions from both fresh and cached files
  private int projectTotalMethodsUsageCount;
  private int projectTotalNoEnumUsageCount;

  // All potential issue locations keyed by file — populated from both fresh scans and cache reads
  private final Map<InputFile, List<CachedIssue>> issuesByFile = new HashMap<>();

  @Override
  public void setContext(JavaFileScannerContext context) {
    super.setContext(context);
    importSupplier = null;
    currentFileTotalCount = 0;
    currentFileNoEnumCount = 0;
    currentFileIssues.clear();
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    importSupplier = null;
    projectTotalMethodsUsageCount += currentFileTotalCount;
    projectTotalNoEnumUsageCount += currentFileNoEnumCount;
    if (!currentFileIssues.isEmpty()) {
      issuesByFile.put(context.getInputFile(), new ArrayList<>(currentFileIssues));
    }

    CacheContext cacheContext = context.getCacheContext();
    if (cacheContext.isCacheEnabled()) {
      cacheContext.getWriteCache().write(
        cacheKey(context.getInputFile()),
        ConditionalRuleCacheUtils.serialize(currentFileTotalCount, currentFileNoEnumCount, currentFileIssues));
    }

    currentFileTotalCount = 0;
    currentFileNoEnumCount = 0;
    currentFileIssues.clear();
  }

  @Override
  public boolean scanWithoutParsing(InputFileScannerContext context) {
    CacheContext cacheContext = context.getCacheContext();
    if (!cacheContext.isCacheEnabled()) {
      return false;
    }
    String key = cacheKey(context.getInputFile());
    byte[] data = cacheContext.getReadCache().readBytes(key);
    if (data == null) {
      return false;
    }
    CachedFileData cached = ConditionalRuleCacheUtils.deserialize(data);
    projectTotalMethodsUsageCount += cached.totalCount();
    projectTotalNoEnumUsageCount += cached.noEnumCount();
    if (!cached.issues().isEmpty()) {
      issuesByFile.put(context.getInputFile(), cached.issues());
    }
    cacheContext.getWriteCache().copyFromPrevious(key);
    return true;
  }

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava8Compatible();
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(
      METHOD_WITH_MONTH_AS_SECOND_ARGUMENT, METHOD_WITH_MONTH_ENUM_AS_SECOND_ARGUMENT,
      MONTH_OF_MATCHER, MONTH_DAY_OF_MATCHER, MONTH_DAY_WITH_ENUM_MATCHER, DAY_OF_WEEK_OF_MATCHER);
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
    currentFileTotalCount++;

    if (METHOD_WITH_MONTH_AS_SECOND_ARGUMENT.matches(mit)) {
      ExpressionTree secondArgument = mit.arguments().get(1);
      int secondArgumentLiteral = getIntLiteral(secondArgument);
      if (isValidMonth(secondArgumentLiteral)) {
        collectIssue(secondArgument, getMonthEnumName(secondArgumentLiteral), MONTH_ISSUE_MESSAGE, JAVA_TIME_MONTH);
        return;
      }
    }
    ExpressionTree firstArgument = mit.arguments().get(0);
    int firstArgumentLiteral = getIntLiteral(firstArgument);
    if (DAY_OF_WEEK_OF_MATCHER.matches(mit) && isValidDay(firstArgumentLiteral)) {
      collectIssue(mit, getDayOfWeekEnumName(firstArgumentLiteral), DAY_ISSUE_MESSAGE, JAVA_TIME_DAY_OF_WEEK);
      return;
    }
    if (MONTH_OF_MATCHER.matches(mit) && isValidMonth(firstArgumentLiteral)) {
      collectIssue(mit, getMonthEnumName(firstArgumentLiteral), MONTH_ISSUE_MESSAGE, JAVA_TIME_MONTH);
      return;
    }
    if (MONTH_DAY_OF_MATCHER.matches(mit) && isValidMonth(firstArgumentLiteral)) {
      collectIssue(firstArgument, getMonthEnumName(firstArgumentLiteral), MONTH_ISSUE_MESSAGE, JAVA_TIME_MONTH);
    }
  }

  private void collectIssue(ExpressionTree arg, String replacement, String issueMessage, String importName) {
    currentFileNoEnumCount++;
    AnalyzerMessage.TextSpan span = AnalyzerMessage.textSpanFor(arg);
    if (importSupplier == null) {
      importSupplier = QuickFixHelper.newImportSupplier(context);
    }
    ImportEditData importEdit = importSupplier.newImportEdit(importName)
      .map(ie -> {
        AnalyzerMessage.TextSpan s = ie.getTextSpan();
        return new ImportEditData(s.startLine, s.startCharacter, s.endLine, s.endCharacter, ie.getReplacement());
      })
      .orElse(null);
    currentFileIssues.add(new CachedIssue(
      span.startLine, span.startCharacter, span.endLine, span.endCharacter,
      issueMessage, replacement, importEdit));
  }

  @Override
  public void visitNode(Tree tree) {
    super.visitNode(tree);
    if (tree.is(Tree.Kind.EQUAL_TO, Tree.Kind.NOT_EQUAL_TO)) {
      BinaryExpressionTree binaryExpressionTree = (BinaryExpressionTree) tree;
      ExpressionTree leftOperand = binaryExpressionTree.leftOperand();
      ExpressionTree rightOperand = binaryExpressionTree.rightOperand();
      if (leftOperand instanceof MethodInvocationTree mit) {
        checkComparison(binaryExpressionTree, mit, rightOperand, false);
        return;
      }
      if (rightOperand instanceof MethodInvocationTree mit) {
        checkComparison(binaryExpressionTree, mit, leftOperand, true);
      }
    }
  }

  private void checkComparison(BinaryExpressionTree binaryExpressionTree,
    MethodInvocationTree methodInvocationSide, ExpressionTree literalSide, boolean isReversed) {
    boolean isMatchedMethod = GET_MONTH_VALUE_MATCHER.matches(methodInvocationSide)
      || MONTH_GET_VALUE_MATCHER.matches(methodInvocationSide)
      || DAY_OF_WEEK_GET_VALUE_MATCHER.matches(methodInvocationSide);
    if (!isMatchedMethod) {
      return;
    }

    currentFileTotalCount++;

    int intLiteral = getIntLiteral(literalSide);
    if (intLiteral == -1) {
      return;
    }
    if (GET_MONTH_VALUE_MATCHER.matches(methodInvocationSide) && isValidMonth(intLiteral)) {
      collectIssue(binaryExpressionTree, getMonthValueReplacement(methodInvocationSide, binaryExpressionTree, intLiteral, isReversed),
        MONTH_ISSUE_MESSAGE, JAVA_TIME_MONTH);
    } else if (MONTH_GET_VALUE_MATCHER.matches(methodInvocationSide) && isValidMonth(intLiteral)) {
      collectIssue(binaryExpressionTree, getValueReplacement(methodInvocationSide, binaryExpressionTree, getMonthEnumName(intLiteral), isReversed),
        MONTH_ISSUE_MESSAGE, JAVA_TIME_MONTH);
    } else if (DAY_OF_WEEK_GET_VALUE_MATCHER.matches(methodInvocationSide) && isValidDay(intLiteral)) {
      collectIssue(binaryExpressionTree, getValueReplacement(methodInvocationSide, binaryExpressionTree, getDayOfWeekEnumName(intLiteral), isReversed),
        DAY_ISSUE_MESSAGE, JAVA_TIME_DAY_OF_WEEK);
    }
  }

  @Override
  public void endOfAnalysis(ModuleScannerContext context) {
    if (projectTotalMethodsUsageCount == 0
      || projectTotalNoEnumUsageCount * 100 >= RAISED_PERCENTAGE_THRESHOLD * projectTotalMethodsUsageCount) {
      return;
    }
    DefaultModuleScannerContext defaultContext = (DefaultModuleScannerContext) context;
    issuesByFile.forEach((inputFile, issues) ->
      issues.forEach(issue ->
        defaultContext.newIssueForFile(inputFile)
          .forRule(this)
          .onRange(issue.startLine(), issue.startCol(), issue.endLine(), issue.endCol())
          .withMessage(issue.message())
          .withQuickFix(() -> buildQuickFix(issue))
          .report()
      )
    );
  }

  private static JavaQuickFix buildQuickFix(CachedIssue issue) {
    var span = new AnalyzerMessage.TextSpan(issue.startLine(), issue.startCol(), issue.endLine(), issue.endCol());
    JavaQuickFix.Builder builder = JavaQuickFix.newQuickFix(String.format("Replace with %s.", issue.replacement()))
      .addTextEdit(JavaTextEdit.replaceTextSpan(span, issue.replacement()));
    if (issue.importEdit() != null) {
      ImportEditData ie = issue.importEdit();
      builder.addTextEdit(JavaTextEdit.replaceTextSpan(
        new AnalyzerMessage.TextSpan(ie.startLine(), ie.startCol(), ie.endLine(), ie.endCol()),
        ie.replacement()));
    }
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

  private static String cacheKey(InputFile inputFile) {
    return CACHE_KEY_PREFIX + inputFile.key();
  }
}
