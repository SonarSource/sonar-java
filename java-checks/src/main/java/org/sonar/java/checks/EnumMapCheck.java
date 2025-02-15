/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S1640")
public class EnumMapCheck extends BaseTreeVisitor implements JavaFileScanner {
  private JavaFileScannerContext context;
  private static final String JAVA_UTIL_MAP = "java.util.Map";
  private static final MethodMatchers mapPut = MethodMatchers.create().ofTypes(JAVA_UTIL_MAP).names("put").withAnyParameters().build();

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitVariable(VariableTree tree) {
    if (tree.type().symbolType().isSubtypeOf(JAVA_UTIL_MAP)) {
      ExpressionTree initializer = tree.initializer();
      if (initializer != null && !usesNullKey(tree.symbol())) {
        checkNewMap(initializer, hasEnumKey(tree.type().symbolType()));
      }
    } else {
      super.visitVariable(tree);
    }
  }

  private static boolean usesNullKey(Symbol symbol) {
    List<IdentifierTree> usages = symbol.usages();
    for (IdentifierTree usage : usages) {
      if (MethodTreeUtils.consecutiveMethodInvocation(usage)
        .filter(mit -> mapPut.matches(mit) && mit.arguments().get(0).is(Tree.Kind.NULL_LITERAL))
        .isPresent()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    if (tree.variable().symbolType().isSubtypeOf(JAVA_UTIL_MAP)) {
      checkNewMap(tree.expression(), hasEnumKey(tree.variable().symbolType()));
    } else {
      super.visitAssignmentExpression(tree);
    }
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    if (isUnorderedMap(tree.symbolType()) && hasEnumKey(tree.identifier().symbolType())) {
      addIssue(tree);
    } else {
      super.visitNewClass(tree);
    }
  }

  private void checkNewMap(ExpressionTree given, boolean useEnumKey) {
    ExpressionTree expression = ExpressionUtils.skipParentheses(given);
    if (expression.is(Tree.Kind.NEW_CLASS)) {
      NewClassTree newClassTree = (NewClassTree) expression;
      if (isUnorderedMap(newClassTree.symbolType()) && (useEnumKey || hasEnumKey(newClassTree.identifier().symbolType()))) {
        addIssue(newClassTree);
      }
    }
  }

  private static boolean isUnorderedMap(Type type) {
    return type.isSubtypeOf("java.util.HashMap") &&
      !type.isSubtypeOf("java.util.LinkedHashMap");
  }

  private static boolean hasEnumKey(Type type) {
    return type.isParameterized() && type.typeArguments().get(0).symbol().isEnum();
  }

  private void addIssue(Tree typeTree) {
    context.reportIssue(this, typeTree, "Convert this Map to an EnumMap.");
  }

}
