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

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

@Rule(key = "S9386")
public class CastDoubleToFloatCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Use a float literal instead of casting a double literal to float.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.TYPE_CAST);
  }

  @Override
  public void visitNode(Tree tree) {
    TypeCastTree typeCastTree = (TypeCastTree) tree;
    if (!isFloatCast(typeCastTree)) {
      return;
    }
    ExpressionTree expression = ExpressionUtils.skipParentheses(typeCastTree.expression());
    boolean negated = false;
    if (expression.is(Tree.Kind.UNARY_MINUS, Tree.Kind.UNARY_PLUS)) {
      negated = expression.is(Tree.Kind.UNARY_MINUS);
      expression = ((UnaryExpressionTree) expression).expression();
    }
    if (!expression.is(Tree.Kind.DOUBLE_LITERAL)) {
      return;
    }
    String literalValue = ((LiteralTree) expression).value();
    String stripped = stripDoubleSuffix(literalValue);
    if (!isEquivalentFloatLiteral(stripped, negated)) {
      return;
    }
    String replacement = (negated ? "-" : "") + stripped + "f";
    QuickFixHelper.newIssue(context)
      .forRule(this)
      .onTree(typeCastTree)
      .withMessage(MESSAGE)
      .withQuickFix(() -> JavaQuickFix.newQuickFix("Replace with a float literal")
        .addTextEdit(JavaTextEdit.replaceTree(typeCastTree, replacement))
        .build())
      .report();
  }

  private static boolean isFloatCast(TypeCastTree typeCastTree) {
    if (typeCastTree.type() instanceof PrimitiveTypeTree primitiveType) {
      return "float".equals(primitiveType.keyword().text());
    }
    return false;
  }

  private static boolean isEquivalentFloatLiteral(String stripped, boolean negated) {
    String parseable = stripped.replace("_", "");
    try {
      double asDouble = Double.parseDouble(parseable);
      float asFloat = Float.parseFloat(parseable);
      if (!Float.isFinite(asFloat)) {
        return false;
      }
      if (asFloat == 0.0f && asDouble != 0.0) {
        return false;
      }
      if (negated) {
        asDouble = -asDouble;
        asFloat = -asFloat;
      }
      return asFloat == (float) asDouble;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private static String stripDoubleSuffix(String literal) {
    if (literal.endsWith("d") || literal.endsWith("D")) {
      return literal.substring(0, literal.length() - 1);
    }
    return literal;
  }
}
