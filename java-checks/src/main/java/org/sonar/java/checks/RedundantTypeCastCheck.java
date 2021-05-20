/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.JWarning;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;

@Rule(key = "S1905")
public class RedundantTypeCastCheck extends IssuableSubscriptionVisitor {

  private List<JWarning> warnings;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.COMPILATION_UNIT, Tree.Kind.TYPE_CAST);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.COMPILATION_UNIT)) {
      warnings = ((JavaTree.CompilationUnitTreeImpl) tree).warnings(JWarning.Type.REDUNDANT_CAST);
      return;
    }

    TypeCastTree typeCastTree = (TypeCastTree) tree;
    Type cast = typeCastTree.type().symbolType();
    if (isUnnecessaryCast(typeCastTree)) {
      reportIssue(typeCastTree.type(), "Remove this unnecessary cast to \"" + cast.erasure() + "\".");
    }
  }

  public static Tree skipParentheses(Tree tree) {
    if (tree instanceof ExpressionTree) return ExpressionUtils.skipParentheses((ExpressionTree) tree);
    return tree;
  }

  private boolean isUnnecessaryCast(TypeCastTree typeCastTree) {
    if (skipParentheses(typeCastTree.expression()).is(Tree.Kind.NULL_LITERAL)) {
      Tree parentTree = skipParentheses(typeCastTree.parent());
      return !parentTree.is(Tree.Kind.ARGUMENTS);
    }
    return warnings.stream().anyMatch(warning -> matchesWarning(warning, typeCastTree));
  }

  private static boolean matchesWarning(JWarning warning, TypeCastTree tree) {
    SyntaxToken startToken = tree.openParenToken();
    // When a cast expression is nested inside one or more parenthesized expression, Eclipse raises the warning on
    // the outermost parenthesized expression rather than the cast expression, so we need to take that into account
    if (tree.parent().is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
      ParenthesizedTree parent = (ParenthesizedTree) tree.parent();
      while (parent.parent().is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
        parent = (ParenthesizedTree) parent.parent();
      }
      startToken = parent.openParenToken();
    }
    return warning.getStartLine() == startToken.line() && warning.getStartColumn() == startToken.column();
  }

}
