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

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.MethodInvocationMatcherCollection;
import org.sonar.java.checks.methods.MethodMatcher;
import org.sonar.java.checks.methods.TypeCriteria;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.resolve.JavaType;
import org.sonar.java.tag.Tag;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
  key = "S2131",
  name = "Primitives should not be boxed just for \"String\" conversion",
  priority = Priority.MAJOR,
  tags = {Tag.PERFORMANCE})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.MEMORY_EFFICIENCY)
@SqaleConstantRemediation("5min")
public class PrimitiveTypeBoxingWithToStringCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final MethodInvocationMatcherCollection TO_STRING_MATCHERS = getToStringMatchers(
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

  private static MethodInvocationMatcherCollection getToStringMatchers(String... typeFullyQualifiedNames) {
    MethodInvocationMatcherCollection matchers = MethodInvocationMatcherCollection.create();
    for (String fullyQualifiedName : typeFullyQualifiedNames) {
      matchers.add(MethodMatcher.create()
        .typeDefinition(TypeCriteria.subtypeOf(fullyQualifiedName))
        .name("toString"));
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
    context.addIssue(reportingTree, this, "Use \"" + wrapperName + ".toString\" instead.");
  }

  @Override
  public void visitAnnotation(AnnotationTree annotationTree) {
    scan(annotationTree.annotationType());
    // skip arguments of annotation as it should be compile time constant so it is not relevant here.
  }

  private void checkConcatenation(Tree tree, ExpressionTree leftOperand, ExpressionTree rightOperand) {
    Type wrapper = null;
    if (isEmptyString(leftOperand)) {
      wrapper = ((JavaType) rightOperand.symbolType()).primitiveWrapperType();
    } else if (isEmptyString(rightOperand)) {
      wrapper = ((JavaType) leftOperand.symbolType()).primitiveWrapperType();
    }
    if (wrapper != null) {
      createIssue(tree, wrapper.name());
    }
  }

  private static boolean isEmptyString(ExpressionTree expressionTree) {
    return expressionTree.is(Kind.STRING_LITERAL) && LiteralUtils.trimQuotes(((LiteralTree) expressionTree).value()).isEmpty();
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
