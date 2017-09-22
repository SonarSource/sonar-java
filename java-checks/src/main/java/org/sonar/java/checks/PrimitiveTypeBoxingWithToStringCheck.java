/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.MethodMatcherCollection;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.resolve.JavaType;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S2131")
public class PrimitiveTypeBoxingWithToStringCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final MethodMatcherCollection TO_STRING_MATCHERS = getToStringMatchers(
    "java.lang.Byte",
    "java.lang.Character",
    "java.lang.Short",
    "java.lang.Integer",
    "java.lang.Long",
    "java.lang.Float",
    "java.lang.Double",
    "java.lang.Boolean");

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  private static MethodMatcherCollection getToStringMatchers(String... typeFullyQualifiedNames) {
    MethodMatcherCollection matchers = MethodMatcherCollection.create();
    for (String fullyQualifiedName : typeFullyQualifiedNames) {
      matchers.add(MethodMatcher.create()
        .typeDefinition(TypeCriteria.subtypeOf(fullyQualifiedName))
        .name("toString")
        .withoutParameter());
    }
    return matchers;
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    if (TO_STRING_MATCHERS.anyMatch(tree)) {
      ExpressionTree abstractTypedTree = ((MemberSelectExpressionTree) tree.methodSelect()).expression();
      if (abstractTypedTree.is(Kind.NEW_CLASS) || isValueOfInvocation(abstractTypedTree)) {
        String typeName = abstractTypedTree.symbolType().toString();
        createIssue(tree, typeName);
      }
    }
    super.visitMethodInvocation(tree);
  }

  @Override
  public void visitBinaryExpression(BinaryExpressionTree tree) {
    if (tree.is(Kind.PLUS)) {
      checkConcatenation(tree, tree.leftOperand(), tree.rightOperand());
    }
    super.visitBinaryExpression(tree);
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    Type wrapper = ((JavaType) tree.expression().symbolType()).primitiveWrapperType();
    if (tree.is(Kind.PLUS_ASSIGNMENT) && tree.variable().symbolType().is("java.lang.String") && wrapper != null) {
      createIssue(tree, wrapper.name());
    }
    super.visitAssignmentExpression(tree);
  }

  private void createIssue(Tree reportingTree, String wrapperName) {
    context.reportIssue(this, reportingTree, "Use \"" + wrapperName + ".toString\" instead.");
  }

  @Override
  public void visitAnnotation(AnnotationTree annotationTree) {
    scan(annotationTree.annotationType());
    // skip arguments of annotation as it should be compile time constant so it is not relevant here.
  }

  private void checkConcatenation(Tree tree, ExpressionTree leftOperand, ExpressionTree rightOperand) {
    Type wrapper = null;
    ExpressionTree leftOp = ExpressionUtils.skipParentheses(leftOperand);
    ExpressionTree rightOp = ExpressionUtils.skipParentheses(rightOperand);
    if (LiteralUtils.isEmptyString(leftOp) && !isConstant(rightOp)) {
      wrapper = ((JavaType) rightOp.symbolType()).primitiveWrapperType();
    } else if (LiteralUtils.isEmptyString(rightOp) && !isConstant(leftOp)) {
      wrapper = ((JavaType) leftOperand.symbolType()).primitiveWrapperType();
    }
    if (wrapper != null) {
      createIssue(tree, wrapper.name());
    }
  }

  private static boolean isConstant(ExpressionTree operand) {
    switch (operand.kind()) {
      case BOOLEAN_LITERAL:
      case CHAR_LITERAL:
      case DOUBLE_LITERAL:
      case FLOAT_LITERAL:
      case INT_LITERAL:
      case LONG_LITERAL:
      case STRING_LITERAL:
        return true;
      case NULL_LITERAL:
        return false;
      case IDENTIFIER:
        return isPrimitiveConstant(((IdentifierTree) operand).symbol());
      case MEMBER_SELECT:
        MemberSelectExpressionTree mset = (MemberSelectExpressionTree) operand;
        if (ExpressionUtils.isSelectOnThisOrSuper(mset)) {
          // using "this.MY_CONSTANT" in concatenation forces boxing, while using A.MY_CONSTANT allow compiler to optimize code
          return false;
        }
        return isPrimitiveConstant(mset.identifier().symbol());
      default:
        return false;
    }
  }

  private static boolean isPrimitiveConstant(Symbol symbol) {
    // Static or final is enough to get optimization occurring when initializer is a literal,
    // as long as it is not access with "this."
    // For constants declared outside the current file, result will depend of the initializer,
    // so ignoring initializers to not raise FPs (prefer FNs over FPs)
    // JLS8 : §15.28 & §15.18.1
    return (symbol.isStatic() || symbol.isFinal()) && symbol.type().isPrimitive();
  }

  private static boolean isValueOfInvocation(ExpressionTree abstractTypedTree) {
    if (!abstractTypedTree.is(Kind.METHOD_INVOCATION)) {
      return false;
    }
    Type type = abstractTypedTree.symbolType();
    MethodMatcher valueOfMatcher = MethodMatcher.create()
      .typeDefinition(type.fullyQualifiedName())
      .name("valueOf")
      .addParameter(((JavaType) type).primitiveType().fullyQualifiedName());
    return valueOfMatcher.matches((MethodInvocationTree) abstractTypedTree);
  }
}
