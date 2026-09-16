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

import java.util.List;
import java.util.Locale;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S9366")
public class DurationTimeUnitAgreementCheck extends IssuableSubscriptionVisitor {

  private static final String JAVA_TIME_DURATION = "java.time.Duration";
  private static final String JAVA_UTIL_CONCURRENT_TIME_UNIT = "java.util.concurrent.TimeUnit";

  private static final MethodMatchers DURATION_CONVERSIONS = MethodMatchers.create()
    .ofTypes(JAVA_TIME_DURATION)
    .names("toNanos", "toMillis", "toSeconds", "getSeconds", "toMinutes", "toHours", "toDays")
    .addWithoutParametersMatcher()
    .build();

  private static final MethodMatchers TIME_UNIT_CONVERT = MethodMatchers.create()
    .ofTypes(JAVA_UTIL_CONCURRENT_TIME_UNIT)
    .names("convert")
    .addParametersMatcher(JAVA_TIME_DURATION)
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (context.getSemanticModel() == null) {
      return;
    }
    Arguments arguments = getArguments(tree);
    if (arguments == null || arguments.size() < 2) {
      return;
    }
    for (int i = 0; i < arguments.size() - 1; i++) {
      checkArgumentPair(arguments.get(i), arguments.get(i + 1));
    }
  }

  private void checkArgumentPair(ExpressionTree durationArg, ExpressionTree timeUnitArg) {
    Type durationArgType = durationArg.symbolType();
    Type timeUnitArgType = timeUnitArg.symbolType();
    if (durationArgType.isUnknown() || timeUnitArgType.isUnknown()) {
      return;
    }
    if (!isLongType(durationArgType) || !isTimeUnitType(timeUnitArgType)) {
      return;
    }
    String argumentUnit = getTimeUnitConstantName(timeUnitArg);
    if (argumentUnit == null) {
      return;
    }
    String conversionUnit = getDurationConversionUnit(durationArg);
    if (conversionUnit == null || conversionUnit.equals(argumentUnit)) {
      return;
    }
    String message = String.format("Change this TimeUnit to \"%s\" or convert the duration to %s.", conversionUnit, getUnitDescription(argumentUnit));
    QuickFixHelper.newIssue(context)
      .forRule(this)
      .onTree(timeUnitArg)
      .withMessage(message)
      .withQuickFix(() -> createQuickFix(timeUnitArg, conversionUnit))
      .report();
  }

  private static boolean isLongType(Type type) {
    return type.isPrimitive(Type.Primitives.LONG) || type.is("java.lang.Long");
  }

  private static boolean isTimeUnitType(Type type) {
    return type.is(JAVA_UTIL_CONCURRENT_TIME_UNIT);
  }

  private static String getDurationConversionUnit(ExpressionTree expr) {
    ExpressionTree unwrapped = ExpressionUtils.skipParentheses(expr);
    if (!unwrapped.is(Tree.Kind.METHOD_INVOCATION)) {
      return null;
    }
    MethodInvocationTree mit = (MethodInvocationTree) unwrapped;
    if (DURATION_CONVERSIONS.matches(mit)) {
      return switch (mit.methodSymbol().name()) {
        case "toNanos" -> "NANOSECONDS";
        case "toMillis" -> "MILLISECONDS";
        case "toSeconds", "getSeconds" -> "SECONDS";
        case "toMinutes" -> "MINUTES";
        case "toHours" -> "HOURS";
        case "toDays" -> "DAYS";
        default -> null;
      };
    }
    if (TIME_UNIT_CONVERT.matches(mit) && mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mse = (MemberSelectExpressionTree) mit.methodSelect();
      return getTimeUnitConstantName(mse.expression());
    }
    return null;
  }

  private static String getTimeUnitConstantName(ExpressionTree expr) {
    ExpressionTree unwrapped = ExpressionUtils.skipParentheses(expr);
    Symbol symbol = null;
    if (unwrapped.is(Tree.Kind.IDENTIFIER)) {
      symbol = ((IdentifierTree) unwrapped).symbol();
    } else if (unwrapped.is(Tree.Kind.MEMBER_SELECT)) {
      symbol = ((MemberSelectExpressionTree) unwrapped).identifier().symbol();
    }
    if (symbol != null && !symbol.isUnknown() && symbol.isVariableSymbol() && symbol.isEnum()) {
      Symbol owner = symbol.owner();
      if (owner != null && owner.type().is(JAVA_UTIL_CONCURRENT_TIME_UNIT)) {
        return symbol.name();
      }
    }
    return null;
  }

  private static String getUnitDescription(String timeUnitConstant) {
    return switch (timeUnitConstant) {
      case "NANOSECONDS" -> "nanoseconds";
      case "MICROSECONDS" -> "microseconds";
      case "MILLISECONDS" -> "milliseconds";
      case "SECONDS" -> "seconds";
      case "MINUTES" -> "minutes";
      case "HOURS" -> "hours";
      case "DAYS" -> "days";
      default -> timeUnitConstant.toLowerCase(Locale.ROOT);
    };
  }

  private JavaQuickFix createQuickFix(ExpressionTree timeUnitArg, String conversionUnit) {
    ExpressionTree unwrapped = ExpressionUtils.skipParentheses(timeUnitArg);
    if (unwrapped.is(Tree.Kind.MEMBER_SELECT)) {
      IdentifierTree identifier = ((MemberSelectExpressionTree) unwrapped).identifier();
      return JavaQuickFix.newQuickFix("Change TimeUnit to \"" + conversionUnit + "\"")
        .addTextEdit(JavaTextEdit.replaceTree(identifier, conversionUnit))
        .build();
    }
    var builder = JavaQuickFix.newQuickFix("Change TimeUnit to \"" + conversionUnit + "\"")
      .addTextEdit(JavaTextEdit.replaceTree(unwrapped, "TimeUnit." + conversionUnit));
    QuickFixHelper.newImportSupplier(context).newImportEdit(JAVA_UTIL_CONCURRENT_TIME_UNIT)
      .ifPresent(builder::addTextEdit);
    return builder.build();
  }

  private static Arguments getArguments(Tree tree) {
    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      return ((MethodInvocationTree) tree).arguments();
    } else if (tree.is(Tree.Kind.NEW_CLASS)) {
      return ((NewClassTree) tree).arguments();
    }
    return null;
  }
}
