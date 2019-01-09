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

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S2789")
public class NullShouldNotBeUsedWithOptionalCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final String NULLABLE = "javax.annotation.Nullable";

  private static final String OPTIONAL = "java.util.Optional";

  private JavaFileScannerContext context;

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitMethod(MethodTree method) {
    if (!method.is(Tree.Kind.CONSTRUCTOR) && returnsOptional(method)) {

      // check that the method is not annotated with @Nullable
      checkNullableAnnotation(method.modifiers(), "Methods with an \"Optional\" return type should not be \"@Nullable\".");

      // check that the method does not return "null"
      method.accept(new ReturnNullVisitor());
    }

    super.visitMethod(method);
  }

  @Override
  public void visitBinaryExpression(BinaryExpressionTree binaryExpression) {
    // check that an @Optional is not compared to "null"
    if (binaryExpression.is(Tree.Kind.EQUAL_TO, Tree.Kind.NOT_EQUAL_TO)) {
      ExpressionTree left = binaryExpression.leftOperand();
      ExpressionTree right = binaryExpression.rightOperand();
      if ((isOptional(left) && isNull(right)) || (isNull(left) && isOptional(right))) {
        context.reportIssue(this, binaryExpression, "Remove this null-check of an \"Optional\".");
      }
    }

    super.visitBinaryExpression(binaryExpression);
  }

  @Override
  public void visitVariable(VariableTree variable) {
    if (variable.type().symbolType().is(OPTIONAL)) {
      checkNullableAnnotation(variable.modifiers(), "\"Optional\" variables should not be \"@Nullable\".");
    }

    super.visitVariable(variable);
  }

  private class ReturnNullVisitor extends BaseTreeVisitor {

    @Override
    public void visitReturnStatement(ReturnStatementTree returnStatement) {
      checkNull(returnStatement.expression());

      super.visitReturnStatement(returnStatement);
    }

    @Override
    public void visitConditionalExpression(ConditionalExpressionTree conditionalExpression) {
      if (conditionalExpression.symbolType().is(OPTIONAL)) {
        checkNull(conditionalExpression.trueExpression());
        checkNull(conditionalExpression.falseExpression());
      }

      super.visitConditionalExpression(conditionalExpression);
    }

    private void checkNull(ExpressionTree expression) {
      if (isNull(expression)) {
        context.reportIssue(NullShouldNotBeUsedWithOptionalCheck.this, expression, "Methods with an \"Optional\" return type should never return null.");
      }
    }

    @Override
    public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
      // don't visit lambdas, as they are allowed to return null
    }

    @Override
    public void visitClass(ClassTree tree) {
      // don't visit inner class tree, as methods in there will be visited by outer class
    }

  }

  private static boolean returnsOptional(MethodTree method) {
    return method.returnType().symbolType().is(OPTIONAL);
  }

  private static boolean isOptional(ExpressionTree expression) {
    return expression.symbolType().is(OPTIONAL) && !isNull(expression);
  }

  private static boolean isNull(ExpressionTree expression) {
    return expression.is(Tree.Kind.NULL_LITERAL);
  }

  private void checkNullableAnnotation(ModifiersTree modifiers, String message) {
    for (AnnotationTree annotation : modifiers.annotations()) {
      Type type = annotation.annotationType().symbolType();
      if (type.is(NULLABLE)) {
        context.reportIssue(this, annotation, message);
      }
    }
  }

}
