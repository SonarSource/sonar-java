/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.resolve.JavaType;
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
  private JavaFileScannerContext context;

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
    return isBot(leftOpType) || isBot(rightOpType);
  }

  private static boolean isBot(Type type) {
    return ((JavaType) type).isTagged(JavaType.BOT);
  }

  protected static boolean isStringType(Type leftOpType, Type rightOpType) {
    return leftOpType.is(JAVA_LANG_STRING) && rightOpType.is(JAVA_LANG_STRING);
  }

  protected static boolean isBoxedType(Type leftOpType, Type rightOpType) {
    return ((JavaType)leftOpType).isPrimitiveWrapper() && ((JavaType)rightOpType).isPrimitiveWrapper();
  }

  protected void reportIssue(SyntaxToken opToken) {
    context.reportIssue(this, opToken, "Use the \"equals\" method if value comparison was intended.");
  }
}
