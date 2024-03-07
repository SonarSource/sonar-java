/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

import static org.sonar.java.model.SyntacticEquivalence.areEquivalentIncludingSameVariables;

@Rule(key = "S6913")
public class MathClampRangeCheck extends AbstractMethodDetection implements JavaVersionAwareVisitor {

  public static final String DOUBLE = "double";
  public static final String FLOAT = "float";
  public static final String INT = "int";
  public static final String LONG = "long";

  public static final String MIN = "min";
  public static final String MAX = "max";
  public static final String VALUE = "value";

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava21Compatible();
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create()
      .ofTypes("java.lang.Math")
      .names("clamp")
      .addParametersMatcher(DOUBLE, DOUBLE, DOUBLE)
      .addParametersMatcher(FLOAT, FLOAT, FLOAT)
      .addParametersMatcher(LONG, LONG, LONG)
      .addParametersMatcher(LONG, INT, INT)
      .build();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    // according to the MethodMatchers, MethodSymbol.parameterTypes().size() is always 3
    // but it's not a 100% guarantee that mit.arguments().size() is 3 when there is a semantic recovery approximation
    if (mit.arguments().size() == 3) {
      checkMathClampArguments(
        mit.arguments().get(0),
        mit.arguments().get(1),
        mit.arguments().get(2));
    }
  }

  private boolean checkMathClampArguments(ExpressionTree valueExpression, ExpressionTree minExpression, ExpressionTree maxExpression) {
    return checkEquals(minExpression, MIN, maxExpression, MAX) ||
      checkEquals(minExpression, MIN, valueExpression, VALUE) ||
      checkEquals(maxExpression, MAX, valueExpression, VALUE) ||
      checkLessThan(maxExpression, MAX, minExpression, MIN) ||
      checkLessThan(minExpression, MIN, valueExpression, VALUE) ||
      checkLessThan(valueExpression, VALUE, maxExpression, MAX);
  }

  private boolean checkEquals(ExpressionTree exprA, String nameA, ExpressionTree exprB, String nameB) {
    if (!areEquivalentIncludingSameVariables(exprA, exprB)) {
      return false;
    }
    reportIssue(
      exprA, String.format("Change the \"clamp(value,min,max)\"'s arguments so \"%s\" is not equals to \"%s\".", nameA, nameB),
      List.of(new JavaFileScannerContext.Location(nameB + " argument", exprB)),
      null);
    return true;
  }

  private boolean checkLessThan(ExpressionTree exprA, String nameA, ExpressionTree exprB, String nameB) {
    if (!isLessThan(exprA, exprB)) {
      return false;
    }
    QuickFixHelper.newIssue(context)
      .forRule(this)
      .onTree(exprA)
      .withMessage("Change the \"clamp(value,min,max)\"'s arguments so \"%s\" is not always less than \"%s\".", nameA, nameB)
      .withSecondaries(new JavaFileScannerContext.Location(nameB + " argument", exprB))
      .withQuickFix(() -> JavaQuickFix.newQuickFix("Swap \"" + nameA + "\" and \"" + nameB + "\" arguments")
        .addTextEdit(JavaTextEdit.replaceTree(exprA, QuickFixHelper.contentForTree(exprB, context)))
        .addTextEdit(JavaTextEdit.replaceTree(exprB, QuickFixHelper.contentForTree(exprA, context)))
        .build())
      .report();
    return true;
  }

  private static boolean isLessThan(ExpressionTree exprA, ExpressionTree exprB) {
    return exprA.asConstant().orElse(null) instanceof Number a &&
      exprB.asConstant().orElse(null) instanceof Number b &&
      isLessThan(a, b);
  }

  @VisibleForTesting
  static boolean isLessThan(Number a, Number b) {
    if (a instanceof Double || b instanceof Double) {
      return a.doubleValue() < b.doubleValue();
    } else if (a instanceof Float || b instanceof Float) {
      return a.floatValue() < b.floatValue();
    } else {
      // Byte, Short, Integer, Long
      return a.longValue() < b.longValue();
    }
  }

}
