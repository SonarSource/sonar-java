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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.MethodMatcher;
import org.sonar.java.checks.methods.MethodInvocationMatcherCollection;
import org.sonar.java.checks.methods.TypeCriteria;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@Rule(
  key = "S2153",
  name = "Boxing and unboxing should not be immediately reversed",
  tags = {"clumsy"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.MEMORY_EFFICIENCY)
@SqaleConstantRemediation("5min")
public class ImmediateReverseBoxingCheck extends SubscriptionBaseVisitor {

  private static final Map<String, String> PRIMITIVE_TYPES_BY_WRAPPER = ImmutableMap.<String, String>builder()
    .put("java.lang.Boolean", "boolean")
    .put("java.lang.Byte", "byte")
    .put("java.lang.Double", "double")
    .put("java.lang.Float", "float")
    .put("java.lang.Integer", "int")
    .put("java.lang.Long", "long")
    .put("java.lang.Short", "short")
    .build();

  private static final MethodInvocationMatcherCollection unboxingInvocationMatchers = unboxingInvocationMatchers();
  private static final MethodInvocationMatcherCollection valueOfInvocationMatchers = valueOfInvocationMatchers();

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD_INVOCATION, Tree.Kind.VARIABLE, Tree.Kind.ASSIGNMENT, Tree.Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
        visitMethodInvocationTree((MethodInvocationTree) tree);
      } else if (tree.is(Tree.Kind.VARIABLE)) {
        VariableTree variableTree = (VariableTree) tree;
        ExpressionTree initializer = variableTree.initializer();
        if (initializer != null) {
          checkExpression(initializer, variableTree.type().symbolType());
        }
      } else if (tree.is(Tree.Kind.ASSIGNMENT)) {
        AssignmentExpressionTree assignmentTree = (AssignmentExpressionTree) tree;
        checkExpression(assignmentTree.expression(), assignmentTree.symbolType());
      } else {
        NewClassTree newClassTree = (NewClassTree) tree;
        Symbol.TypeSymbol classSymbol = wrapperClassSymbol(newClassTree);
        if (classSymbol != null) {
          checkForUnboxing(newClassTree.arguments().get(0));
        }
      }
    }
  }

  private void checkExpression(ExpressionTree expression, org.sonar.plugins.java.api.semantic.Type implicitType) {
    if (implicitType.isPrimitive()) {
      checkForBoxing(expression);
    } else {
      checkForUnboxing(expression);
    }
  }

  private void visitMethodInvocationTree(MethodInvocationTree methodInvocationTree) {
    if (isValueOfInvocation(methodInvocationTree)) {
      checkForUnboxing(methodInvocationTree.arguments().get(0));
    } else if (isUnboxingMethodInvocation(methodInvocationTree)) {
      ExpressionTree methodSelect = methodInvocationTree.methodSelect();
      if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree memberSelectExpressionTree = (MemberSelectExpressionTree) methodSelect;
        checkForBoxing(memberSelectExpressionTree.expression());
      }
    } else {
      Symbol symbol = methodInvocationTree.symbol();
      if (symbol.isMethodSymbol()) {
        List<Type> parametersTypes = ((Symbol.MethodSymbol) symbol).parameterTypes();
        checkMethodInvocationArguments(methodInvocationTree, parametersTypes);
      }
    }
  }

  private void checkMethodInvocationArguments(MethodInvocationTree methodInvocationTree, List<Type> parametersTypes) {
    List<ExpressionTree> arguments = methodInvocationTree.arguments();
    int position = 0;
    for (Type paramType : parametersTypes) {
      if (arguments.size() > position) {
        checkExpression(arguments.get(position), paramType);
      }
      position++;
    }
  }

  private void checkForBoxing(ExpressionTree expression) {
    if (expression.is(Tree.Kind.NEW_CLASS)) {
      NewClassTree newClassTree = (NewClassTree) expression;
      Symbol.TypeSymbol classSymbol = wrapperClassSymbol(newClassTree);
      if (classSymbol != null) {
        ExpressionTree boxingArg = newClassTree.arguments().get(0);
        if (boxingArg.symbolType().isPrimitive()) {
          addBoxingIssue(newClassTree, classSymbol, boxingArg);
        }
      }
    } else if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree methodInvocationTree = (MethodInvocationTree) expression;
      if (isValueOfInvocation(methodInvocationTree)) {
        ExpressionTree boxingArg = methodInvocationTree.arguments().get(0);
        addBoxingIssue(expression, methodInvocationTree.symbol().owner(), boxingArg);
      }
    }
  }

  private static Symbol.TypeSymbol wrapperClassSymbol(NewClassTree newClassTree) {
    Symbol.TypeSymbol classSymbol = newClassTree.symbolType().symbol();
    if (PRIMITIVE_TYPES_BY_WRAPPER.containsKey(newClassTree.symbolType().fullyQualifiedName())) {
      return classSymbol;
    }
    return null;
  }

  private void addBoxingIssue(Tree tree, Symbol classSymbol, Tree boxingArg) {
    if (boxingArg.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree identifier = (IdentifierTree) boxingArg;
      addIssue(tree, "Remove the boxing of \"" + identifier.name() + "\".");
    } else {
      addIssue(tree, "Remove the boxing to \"" + classSymbol.name() + "\".");
    }
  }

  private void checkForUnboxing(ExpressionTree expressionTree) {
    if (!expressionTree.is(Tree.Kind.METHOD_INVOCATION)) {
      return;
    }
    MethodInvocationTree methodInvocationTree = (MethodInvocationTree) expressionTree;
    if (isUnboxingMethodInvocation(methodInvocationTree)) {
      ExpressionTree methodSelect = methodInvocationTree.methodSelect();
      if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree memberSelectExpressionTree = (MemberSelectExpressionTree) methodSelect;
        ExpressionTree unboxedExpression = memberSelectExpressionTree.expression();
        String unboxingResultTypeName = methodInvocationTree.symbolType().fullyQualifiedName();
        if (unboxingResultTypeName.equals(PRIMITIVE_TYPES_BY_WRAPPER.get(unboxedExpression.symbolType().fullyQualifiedName()))) {
          addUnboxingIssue(expressionTree, unboxedExpression);
        }
      }
    }
  }

  private void addUnboxingIssue(ExpressionTree expressionTree, ExpressionTree expression) {
    if (expression.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree identifier = (IdentifierTree) expression;
      addIssue(expressionTree, "Remove the unboxing of \"" + identifier.name() + "\".");
    } else {
      String name = expression.symbolType().name();
      addIssue(expressionTree, "Remove the unboxing from \"" + name + "\".");
    }
  }

  private static MethodInvocationMatcherCollection unboxingInvocationMatchers() {
    MethodInvocationMatcherCollection matchers = MethodInvocationMatcherCollection.create();
    for (String primitiveType : PRIMITIVE_TYPES_BY_WRAPPER.values()) {
      matchers.add(
        MethodMatcher.create()
          .callSite("boolean".equals(primitiveType) ? TypeCriteria.is("java.lang.Boolean") : TypeCriteria.subtypeOf("java.lang.Number"))
          .name(primitiveType + "Value"));
    }
    return matchers;
  }

  private static MethodInvocationMatcherCollection valueOfInvocationMatchers() {
    MethodInvocationMatcherCollection matchers = MethodInvocationMatcherCollection.create();
    for (Entry<String, String> primitiveMapping : PRIMITIVE_TYPES_BY_WRAPPER.entrySet()) {
      matchers.add(
        MethodMatcher.create()
          .typeDefinition(primitiveMapping.getKey())
          .name("valueOf")
          .addParameter(primitiveMapping.getValue()));
    }
    return matchers;
  }

  private static boolean isUnboxingMethodInvocation(MethodInvocationTree mit) {
    return unboxingInvocationMatchers.anyMatch(mit);
  }

  private static boolean isValueOfInvocation(MethodInvocationTree mit) {
    return valueOfInvocationMatchers.anyMatch(mit);
  }
}
