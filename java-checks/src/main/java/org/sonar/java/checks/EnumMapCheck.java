/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeArguments;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
  key = "S1640",
  name = "Maps with keys that are enum values should be replaced with EnumMap",
  tags = {"performance"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.CPU_EFFICIENCY)
@SqaleConstantRemediation("5min")
public class EnumMapCheck extends BaseTreeVisitor implements JavaFileScanner {
  private JavaFileScannerContext context;

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitVariable(VariableTree tree) {
    if (tree.type().symbolType().isSubtypeOf("java.util.Map")) {
      checkMap(tree);
    } else {
      super.visitVariable(tree);
    }
  }

  private void checkMap(VariableTree tree) {
    boolean returnTypeHasEnumKey = hasEnumKey(tree.type());
    ExpressionTree initializer = tree.initializer();
    if (initializer != null) {
      initializer = removeParenthesis(initializer);
      if (initializer.is(Tree.Kind.NEW_CLASS)) {
        NewClassTree newClassTree = (NewClassTree) initializer;
        if (newClassTree.symbolType().isSubtypeOf("java.util.HashMap") && (returnTypeHasEnumKey || hasEnumKey(newClassTree.identifier()))) {
          addIssue(tree);
        }
      }
    }
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    if (tree.symbolType().isSubtypeOf("java.util.HashMap") && hasEnumKey(tree.identifier())) {
      addIssue(tree);
    } else {
      super.visitNewClass(tree);
    }
  }

  private static boolean hasEnumKey(TypeTree typeTree) {
    if (typeTree.is(Tree.Kind.PARAMETERIZED_TYPE)) {
      TypeArguments typeArguments = ((ParameterizedTypeTree) typeTree).typeArguments();
      if (!typeArguments.isEmpty()) {
        Tree keyTree = typeArguments.get(0);
        if ((keyTree.is(Tree.Kind.MEMBER_SELECT) && ((MemberSelectExpressionTree) keyTree).identifier().symbol().isEnum()) ||
            (keyTree.is(Tree.Kind.IDENTIFIER) && ((IdentifierTree) keyTree).symbol().isEnum())) {
          return true;
        }
      }
    }
    return false;
  }

  private void addIssue(Tree typeTree) {
    context.addIssue(typeTree, this, "Convert this Map to an EnumMap.");
  }

  private static ExpressionTree removeParenthesis(ExpressionTree tree) {
    ExpressionTree result = tree;
    while (result.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
      result = ((ParenthesizedTree) result).expression();
    }
    return result;
  }
}
