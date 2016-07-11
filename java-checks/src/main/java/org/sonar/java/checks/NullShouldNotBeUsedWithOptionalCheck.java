/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.TypeTree; 

@Rule(key = "S2789")
public class NullShouldNotBeUsedWithOptionalCheck extends BaseTreeVisitor implements JavaFileScanner {
  
  private static final String NULLABLE = "javax.annotation.Nullable";

  private JavaFileScannerContext context;

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitMethod(MethodTree method) {
    if (returnsOptional(method)) {

      // check that the method is not annotated with @Nullable
      ModifiersTree modifiers = method.modifiers();
      for (AnnotationTree annotation : modifiers.annotations()) {
        Type type = annotation.annotationType().symbolType();
        if (type.is(NULLABLE)) {
          context.reportIssue(this, annotation, "Methods with an \"Optional\" return type should not be \"@Nullable\".");
        }
      }

      // check that the method does not return "null"
      method.accept(new ReturnVisitor());
    }

    super.visitMethod(method);
  }

  @Override
  public void visitBinaryExpression(BinaryExpressionTree binaryExpression) {
    String message = "Remove this null-check of an \"Optional\".";
    if (isComparisonOperator(binaryExpression.operatorToken())) {
      ExpressionTree left = binaryExpression.leftOperand();
      ExpressionTree right = binaryExpression.rightOperand();
      if (isOptional(left) && isNull(right) ||
          isNull(left) && isOptional(right)) {
        context.reportIssue(this, binaryExpression, message);
      }
    }

    super.visitBinaryExpression(binaryExpression);
  }

  /**
   * A visitor that raises an issue on any <code>return null</code> statement. 
   */
  private class ReturnVisitor extends BaseTreeVisitor {

    @Override
    public void visitReturnStatement(ReturnStatementTree returnStatement) {
      ExpressionTree expression = returnStatement.expression();
      if (expression.is(Kind.NULL_LITERAL)) {
        context.reportIssue(NullShouldNotBeUsedWithOptionalCheck.this, expression.firstToken(), "Methods with an \"Optional\" return type should never return null.");
      }
    }

    @Override
    public void visitClass(ClassTree tree) {
      // don't visit subclasstree, methods in there will be visited by outer class
    }

  }

  /**
   * Returns <code>true</code> iff the specified method signature returns an <code>Optional</code>.
   */
  private static boolean returnsOptional(MethodTree method) {
    TypeTree returnType = method.returnType();
    if (returnType != null) {
      return returnType.symbolType().is(Optional.class.getName());
    }
    return false;
  }

  private static boolean isComparisonOperator(SyntaxToken token) {
    return JavaPunctuator.EQUAL.getValue().equals(token.text()) ||
           JavaPunctuator.NOTEQUAL.getValue().equals(token.text());
  }

  private static boolean isOptional(ExpressionTree expression) {
    // warning: "null" is an Optional!
    return expression.symbolType().is(Optional.class.getName()) && !expression.is(Tree.Kind.NULL_LITERAL);
  }

  private static boolean isNull(ExpressionTree expression) {
    return expression.is(Tree.Kind.NULL_LITERAL);
  }
  
}
