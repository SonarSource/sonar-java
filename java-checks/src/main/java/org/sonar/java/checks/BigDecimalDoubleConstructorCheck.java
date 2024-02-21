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

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.model.DefaultJavaFileScannerContext;
import org.sonar.java.reporting.InternalJavaIssueBuilder;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2111")
public class BigDecimalDoubleConstructorCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatchers BIG_DECIMAL_DOUBLE_FLOAT =
    MethodMatchers.create().ofTypes("java.math.BigDecimal")
      .constructor()
      .addParametersMatcher("double")
      .addParametersMatcher("float")
      .addParametersMatcher("double", MethodMatchers.ANY)
      .addParametersMatcher("float", MethodMatchers.ANY)
      .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    NewClassTree newClassTree = (NewClassTree) tree;
    if (BIG_DECIMAL_DOUBLE_FLOAT.matches(newClassTree)) {
      InternalJavaIssueBuilder builder = ((InternalJavaIssueBuilder) ((DefaultJavaFileScannerContext) context).newIssue())
        .forRule(this)
        .onTree(tree);

      Arguments arguments = newClassTree.arguments();
      if (arguments.size() == 1) {
        builder.withMessage("Use \"BigDecimal.valueOf\" instead.");
        builder.withQuickFix(() -> valueOfQuickFix(newClassTree));
      } else {
        builder.withMessage("Use \"new BigDecimal(String, MathContext)\" instead.");
        ExpressionTree firstArgument = arguments.get(0);
        if (firstArgument instanceof LiteralTree literalTree) {
          builder.withQuickFix(() -> stringConstructorQuickFix(literalTree));
        }
      }
      builder.report();
    }
  }

  private static JavaQuickFix valueOfQuickFix(NewClassTree newClassTree) {
    return JavaQuickFix.newQuickFix("Replace with BigDecimal.valueOf")
      .addTextEdit(JavaTextEdit.replaceBetweenTree(newClassTree.newKeyword(), newClassTree.identifier(), "BigDecimal.valueOf"))
      .build();
  }

  private static JavaQuickFix stringConstructorQuickFix(LiteralTree argument) {
    String argumentValue = argument.value();
    if (argumentValue.endsWith("f") || argumentValue.endsWith("d") || argumentValue.endsWith("F") || argumentValue.endsWith("D")) {
      argumentValue = argumentValue.substring(0, argumentValue.length() - 1);
    }
    String newArgument = String.format("\"%s\"", argumentValue);
    return JavaQuickFix.newQuickFix("Replace with BigDecimal(%s,", newArgument)
      .addTextEdit(JavaTextEdit.replaceTree(argument, newArgument))
      .build();
  }
}
