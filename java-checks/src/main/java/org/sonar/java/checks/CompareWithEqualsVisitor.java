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

import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

public abstract class CompareWithEqualsVisitor extends BaseTreeVisitor implements JavaFileScanner {

  private static final String JAVA_LANG_STRING = "java.lang.String";
  protected JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    if (context.getSemanticModel() != null) {
      scan(context.getTree());
    }
  }

  @Override
  public final void visitMethod(MethodTree tree) {
    if (!isEquals(tree)) {
      super.visitMethod(tree);
    }
  }

  @Override
  public final void visitBinaryExpression(BinaryExpressionTree tree) {
    super.visitBinaryExpression(tree);
    if (tree.is(Tree.Kind.EQUAL_TO, Tree.Kind.NOT_EQUAL_TO)) {
      checkEqualityExpression(tree);
    }
  }

  protected abstract void checkEqualityExpression(BinaryExpressionTree tree);

  private static boolean isEquals(MethodTree tree) {
    return MethodTreeUtils.isEqualsMethod(tree);
  }

  protected static boolean isNullComparison(Type leftOpType, Type rightOpType) {
    return leftOpType.isNullType() || rightOpType.isNullType();
  }

  protected static boolean isStringType(Type leftOpType, Type rightOpType) {
    return leftOpType.is(JAVA_LANG_STRING) && rightOpType.is(JAVA_LANG_STRING);
  }

  protected static boolean isBoxedType(Type leftOpType, Type rightOpType) {
    return leftOpType.isPrimitiveWrapper() && rightOpType.isPrimitiveWrapper();
  }

  protected void reportIssue(SyntaxToken opToken) {
    context.reportIssue(this, opToken, "Use the \"equals\" method if value comparison was intended.");
  }
}
