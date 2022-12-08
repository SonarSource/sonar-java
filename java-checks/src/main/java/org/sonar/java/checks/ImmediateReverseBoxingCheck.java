/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import org.sonar.check.Rule;
import org.sonar.java.ast.visitors.ExtendedIssueBuilderSubscriptionVisitor;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonarsource.analyzer.commons.collections.MapBuilder;

@Rule(key = "S2153")
public class ImmediateReverseBoxingCheck extends ExtendedIssueBuilderSubscriptionVisitor {

  private static final Map<String, String> PRIMITIVE_TYPES_BY_WRAPPER = MapBuilder.<String, String>newMap()
    .put("java.lang.Boolean", "boolean")
    .put("java.lang.Byte", "byte")
    .put("java.lang.Double", "double")
    .put("java.lang.Float", "float")
    .put("java.lang.Integer", "int")
    .put("java.lang.Long", "long")
    .put("java.lang.Short", "short")
    .put("java.lang.Character", "char")
    .build();

  private static final MethodMatchers unboxingInvocationMatchers = unboxingInvocationMatchers();
  private static final MethodMatchers valueOfInvocationMatchers = valueOfInvocationMatchers();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD_INVOCATION, Tree.Kind.VARIABLE, Tree.Kind.ASSIGNMENT, Tree.Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
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
        ExpressionTree arg0 = newClassTree.arguments().get(0);
        checkForUnboxing(arg0);
        checkForUselessUnboxing(newClassTree.symbolType(), newClassTree.identifier(), arg0, newClassTree);
      }
    }
  }

  private void checkExpression(ExpressionTree expression, Type implicitType) {
    if (implicitType.isPrimitive()) {
      checkForBoxing(expression, expression);
    } else {
      checkForUnboxing(expression);
    }
  }

  private void visitMethodInvocationTree(MethodInvocationTree mit) {
    ExpressionTree methodSelect = mit.methodSelect();
    if (isValueOfInvocation(mit)) {
      ExpressionTree arg0 = mit.arguments().get(0);
      checkForUnboxing(arg0);
      checkForUselessUnboxing(mit.symbolType(), methodSelect, arg0, mit);
    } else if (isUnboxingMethodInvocation(mit)) {
      if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
        checkForBoxing(((MemberSelectExpressionTree) methodSelect).expression(), mit);
      }
    } else {
      Symbol symbol = mit.symbol();
      if (symbol.isMethodSymbol()) {
        checkMethodInvocationArguments(mit, ((Symbol.MethodSymbol) symbol).parameterTypes());
      }
    }
  }

  private void checkForUselessUnboxing(Type targetType, Tree reportTree, ExpressionTree arg0, Tree originalTree) {
    Type argType = arg0.symbolType();
    if (argType.is(targetType.fullyQualifiedName())) {
      newIssue()
        .onTree(reportTree)
        .withMessage("Remove the boxing to \"%s\"; The argument is already of the same type.", argType.name())
        .withQuickFix(() -> JavaQuickFix.newQuickFix("Remove the boxing")
          .addTextEdits(removeTreeExcept(originalTree, arg0))
          .build())
        .report();
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

  private void checkForBoxing(ExpressionTree expression, Tree originalTree) {
    if (expression.is(Tree.Kind.NEW_CLASS)) {
      NewClassTree newClassTree = (NewClassTree) expression;
      Symbol.TypeSymbol classSymbol = wrapperClassSymbol(newClassTree);
      if (classSymbol != null) {
        ExpressionTree boxingArg = newClassTree.arguments().get(0);
        if (boxingArg.symbolType().isPrimitive()) {
          addBoxingIssue(newClassTree, classSymbol, boxingArg, originalTree);
        }
      }
    } else if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree methodInvocationTree = (MethodInvocationTree) expression;
      if (isValueOfInvocation(methodInvocationTree)) {
        ExpressionTree boxingArg = methodInvocationTree.arguments().get(0);
        addBoxingIssue(expression, methodInvocationTree.symbol().owner(), boxingArg, originalTree);
      }
    }
  }

  private static Symbol.TypeSymbol wrapperClassSymbol(NewClassTree newClassTree) {
    Symbol.TypeSymbol classSymbol = newClassTree.symbolType().symbol();
    if (PRIMITIVE_TYPES_BY_WRAPPER.containsKey(newClassTree.symbolType().fullyQualifiedName()) && !newClassTree.arguments().isEmpty()) {
      return classSymbol;
    }
    return null;
  }

  private void addBoxingIssue(Tree tree, Symbol classSymbol, Tree boxingArg, Tree originalTree) {
    String message;
    if (boxingArg.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree identifier = (IdentifierTree) boxingArg;
      message = String.format("Remove the boxing of \"%s\".", identifier.name());
    } else {
      message = String.format("Remove the boxing to \"%s\".", classSymbol.name());
    }

    newIssue()
      .onTree(tree)
      .withMessage(message)
      .withQuickFix(() -> JavaQuickFix.newQuickFix("Remove the boxing")
        .addTextEdits(removeTreeExcept(originalTree, boxingArg))
        .build())
      .report();
  }

  private static List<JavaTextEdit> removeTreeExcept(Tree tree, Tree except) {
    return Arrays.asList(JavaTextEdit.removeTextSpan(AnalyzerMessage.textSpanBetween(tree, true, except, false)),
      JavaTextEdit.removeTextSpan(AnalyzerMessage.textSpanBetween(except, false, tree, true)));
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

  private void addUnboxingIssue(ExpressionTree expressionTree, ExpressionTree unboxedExpression) {
    String message;
    if (unboxedExpression.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree identifier = (IdentifierTree) unboxedExpression;
      message = String.format("Remove the unboxing of \"%s\".", identifier.name());
    } else {
      String name = unboxedExpression.symbolType().name();
      message = String.format("Remove the unboxing from \"%s\".", name);
    }

    newIssue()
      .onTree(expressionTree)
      .withMessage(message)
      .withQuickFix(() -> JavaQuickFix.newQuickFix("Remove the unboxing")
        .addTextEdit(JavaTextEdit.removeTextSpan(AnalyzerMessage.textSpanBetween(unboxedExpression, false, expressionTree, true)))
        .build())
      .report();
  }

  private static MethodMatchers unboxingInvocationMatchers() {
    List<MethodMatchers> matchers = new ArrayList<>();
    for (Entry<String, String> type : PRIMITIVE_TYPES_BY_WRAPPER.entrySet()) {
      String primitiveType = type.getValue();
      Predicate<Type> typeCriteria;
      if ("char".equals(primitiveType) || "boolean".equals(primitiveType)) {
        typeCriteria = t -> t.is(type.getKey());
      } else {
        typeCriteria = t -> t.isSubtypeOf("java.lang.Number");
      }
      matchers.add(MethodMatchers.create().ofType(typeCriteria).names(primitiveType + "Value").addWithoutParametersMatcher().build());
    }
    return MethodMatchers.or(matchers);
  }

  private static MethodMatchers valueOfInvocationMatchers() {
    List<MethodMatchers> matchers = new ArrayList<>();
    for (Entry<String, String> primitiveMapping : PRIMITIVE_TYPES_BY_WRAPPER.entrySet()) {
      matchers.add(
        MethodMatchers.create()
          .ofTypes(primitiveMapping.getKey())
          .names("valueOf")
          .addParametersMatcher(primitiveMapping.getValue()).build());
    }
    return MethodMatchers.or(matchers);
  }

  private static boolean isUnboxingMethodInvocation(MethodInvocationTree mit) {
    return unboxingInvocationMatchers.matches(mit);
  }

  private static boolean isValueOfInvocation(MethodInvocationTree mit) {
    return valueOfInvocationMatchers.matches(mit);
  }
}
