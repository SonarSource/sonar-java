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
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ArrayDimensionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VarTypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S9439")
public class ShortArrayInitializerCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Remove the \"%s\" prefix; the declaration already specifies the array type.";
  private static final String QUICK_FIX_MESSAGE = "Remove the redundant array creation";
  private static final Pattern WHITESPACE = Pattern.compile("\\s+");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.VARIABLE);
  }

  @Override
  public void visitNode(Tree tree) {
    VariableTree variable = (VariableTree) tree;
    if (variable.type() instanceof VarTypeTree) {
      return;
    }
    ExpressionTree initializer = variable.initializer();
    if (initializer == null || !initializer.is(Tree.Kind.NEW_ARRAY)) {
      return;
    }
    NewArrayTree newArray = (NewArrayTree) initializer;
    SyntaxToken newKeyword = newArray.newKeyword();
    SyntaxToken openBrace = newArray.openBraceToken();
    if (newKeyword == null || openBrace == null || isAnnotated(newArray) || !isSameType(variable, newArray)) {
      return;
    }
    SyntaxToken lastBracket = lastCloseBracketToken(newArray);
    String prefix = WHITESPACE.matcher(QuickFixHelper.contentForRange(newKeyword, lastBracket, context)).replaceAll(" ");
    QuickFixHelper.newIssue(context)
      .forRule(this)
      .onRange(newKeyword, lastBracket)
      .withMessage(MESSAGE, prefix)
      .withQuickFix(() -> JavaQuickFix.newQuickFix(QUICK_FIX_MESSAGE)
        .addTextEdit(JavaTextEdit.replaceBetweenTree(newKeyword, true, openBrace, false, ""))
        .build())
      .report();
  }

  /**
   * The shorthand initializer has nowhere to carry a type annotation, so removing the array creation
   * would silently discard annotations such as the one in {@code new @Marker String[] {"a"}}.
   */
  private static boolean isAnnotated(NewArrayTree newArray) {
    TypeTree type = newArray.type();
    return (type != null && !type.annotations().isEmpty())
      || newArray.dimensions().stream().anyMatch(dimension -> !dimension.annotations().isEmpty());
  }

  private static boolean isSameType(VariableTree variable, NewArrayTree newArray) {
    Type declaredType = variable.symbol().type();
    Type createdType = newArray.symbolType();
    return !declaredType.isUnknown() && !createdType.isUnknown() && declaredType.equals(createdType);
  }

  private static SyntaxToken lastCloseBracketToken(NewArrayTree newArray) {
    List<ArrayDimensionTree> dimensions = newArray.dimensions();
    return dimensions.get(dimensions.size() - 1).closeBracketToken();
  }
}
