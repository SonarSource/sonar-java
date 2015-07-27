/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.resolve.JavaType.ParametrizedTypeJavaType;
import org.sonar.java.resolve.JavaType.TypeVariableJavaType;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

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
      ExpressionTree initializer = tree.initializer();
      if (initializer != null) {
        checkNewMap(tree, initializer, hasEnumKey(tree.type().symbolType()));
      }
    } else {
      super.visitVariable(tree);
    }
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    if (tree.variable().symbolType().isSubtypeOf("java.util.Map")) {
      checkNewMap(tree, tree.expression(), hasEnumKey(tree.variable().symbolType()));
    } else {
      super.visitAssignmentExpression(tree);
    }
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    if (tree.symbolType().isSubtypeOf("java.util.HashMap") && hasEnumKey(tree.identifier().symbolType())) {
      addIssue(tree);
    } else {
      super.visitNewClass(tree);
    }
  }

  private void checkNewMap(Tree tree, ExpressionTree given, boolean useEnumKey) {
    ExpressionTree expression = ExpressionsHelper.skipParentheses(given);
    if (expression.is(Tree.Kind.NEW_CLASS)) {
      NewClassTree newClassTree = (NewClassTree) expression;
      if (newClassTree.symbolType().isSubtypeOf("java.util.HashMap") && (useEnumKey || hasEnumKey(newClassTree.identifier().symbolType()))) {
        addIssue(tree);
      }
    }
  }

  private static boolean hasEnumKey(Type type) {
    if (type instanceof ParametrizedTypeJavaType) {
      ParametrizedTypeJavaType parametrizedTypeJavaType = (ParametrizedTypeJavaType) type;
      List<TypeVariableJavaType> typeParameters = parametrizedTypeJavaType.typeParameters();
      if (!typeParameters.isEmpty()) {
        return parametrizedTypeJavaType.substitution(typeParameters.get(0)).symbol().isEnum();
      }
    }
    return false;
  }

  private void addIssue(Tree typeTree) {
    context.addIssue(typeTree, this, "Convert this Map to an EnumMap.");
  }

}
