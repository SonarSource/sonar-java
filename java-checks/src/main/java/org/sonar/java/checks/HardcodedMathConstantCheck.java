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
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S9133")
public class HardcodedMathConstantCheck extends IssuableSubscriptionVisitor {

  private static final int MIN_SIGNIFICANT_DIGITS = 4;

  private enum MathConstant {
    PI(Math.PI, "Math.PI", "pi"),
    E(Math.E, "Math.E", "Euler's number"),
    SQRT2(Math.sqrt(2), "Math.sqrt(2)", "the square root of 2"),
    LN2(Math.log(2), "Math.log(2)", "the natural logarithm of 2");

    final double value;
    final String replacement;
    final String description;

    MathConstant(double value, String replacement, String description) {
      this.value = value;
      this.replacement = replacement;
      this.description = description;
    }
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.DOUBLE_LITERAL, Tree.Kind.FLOAT_LITERAL);
  }

  @Override
  public void visitNode(Tree tree) {
    LiteralTree literalTree = (LiteralTree) tree;
    String rawValue = literalTree.value();

    String normalized = normalize(rawValue);
    if (normalized == null) {
      return;
    }

    double parsedValue;
    try {
      parsedValue = Double.parseDouble(normalized);
    } catch (NumberFormatException e) {
      return;
    }

    double absoluteValue = Math.abs(parsedValue);
    if (absoluteValue == 0.0) {
      return;
    }

    int significantDigits = countSignificantDigits(normalized);
    if (significantDigits < MIN_SIGNIFICANT_DIGITS) {
      return;
    }

    // Tolerance is based on the literal's own precision: half a unit in the last significant digit.
    // This ensures we only flag values that match the constant across all their significant digits.
    double relativeTolerance = 5.0 * Math.pow(10, -significantDigits);

    for (MathConstant constant : MathConstant.values()) {
      double relativeError = Math.abs(absoluteValue - constant.value) / constant.value;
      if (relativeError < relativeTolerance) {
        reportIssue(tree, "Use \"" + constant.replacement + "\" instead of this approximation of " + constant.description + ".");
        return;
      }
    }
  }

  private static String normalize(String rawValue) {
    String value = rawValue.replace("_", "");
    // Strip type suffix
    char last = value.charAt(value.length() - 1);
    if (last == 'f' || last == 'F' || last == 'd' || last == 'D') {
      value = value.substring(0, value.length() - 1);
    }
    // Skip hex float literals
    if (value.startsWith("0x") || value.startsWith("0X")) {
      return null;
    }
    // Skip scientific notation
    if (value.indexOf('e') >= 0 || value.indexOf('E') >= 0) {
      return null;
    }
    return value;
  }

  private static int countSignificantDigits(String normalized) {
    boolean foundNonZero = false;
    int count = 0;
    for (int i = 0; i < normalized.length(); i++) {
      char c = normalized.charAt(i);
      if (c == '.' || c == '-' || c == '+') {
        continue;
      }
      if (c >= '0' && c <= '9') {
        if (c != '0') {
          foundNonZero = true;
        }
        if (foundNonZero) {
          count++;
        }
      }
    }
    return count;
  }
}
