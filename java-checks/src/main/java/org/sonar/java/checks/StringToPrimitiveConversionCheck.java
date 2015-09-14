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
import org.apache.commons.lang.StringUtils;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.MethodMatcher;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
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

@Rule(
  key = "S2130",
  name = "Parsing should be used to convert \"Strings\" to primitives",
  tags = {"performance"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.MEMORY_EFFICIENCY)
@SqaleConstantRemediation("5min")
public class StringToPrimitiveConversionCheck extends SubscriptionBaseVisitor {

  private final List<PrimitiveCheck> primitiveChecks = buildPrimitiveChecks();

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.VARIABLE, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      if (tree.is(Tree.Kind.VARIABLE)) {
        VariableTreeImpl variableTree = (VariableTreeImpl) tree;
        Type variableType = variableTree.type().symbolType();
        PrimitiveCheck primitiveCheck = getPrimitiveCheck(variableType);
        ExpressionTree initializer = variableTree.initializer();
        if (primitiveCheck != null && initializer != null) {
          primitiveCheck.checkInstanciation(initializer);
        }
      } else {
        MethodInvocationTree methodInvocationTree = (MethodInvocationTree) tree;
        for (PrimitiveCheck primitiveCheck : primitiveChecks) {
          primitiveCheck.checkMethodInvocation(methodInvocationTree);
        }
      }
    }
  }

  private PrimitiveCheck getPrimitiveCheck(Type type) {
    if (!type.isPrimitive()) {
      return null;
    }
    for (PrimitiveCheck primitiveCheck : primitiveChecks) {
      if (type.isPrimitive(primitiveCheck.tag)) {
        return primitiveCheck;
      }
    }
    return null;
  }

  private List<PrimitiveCheck> buildPrimitiveChecks() {
    return ImmutableList.of(
      new PrimitiveCheck("int", "Integer", Type.Primitives.INT),
      new PrimitiveCheck("boolean", "Boolean", Type.Primitives.BOOLEAN),
      new PrimitiveCheck("byte", "Byte", Type.Primitives.BYTE),
      new PrimitiveCheck("double", "Double", Type.Primitives.DOUBLE),
      new PrimitiveCheck("float", "Float", Type.Primitives.FLOAT),
      new PrimitiveCheck("long", "Long", Type.Primitives.LONG),
      new PrimitiveCheck("short", "Short", Type.Primitives.SHORT));
  }

  private class PrimitiveCheck {
    private final String primitiveName;
    private final String className;
    private final Type.Primitives tag;
    private final String message;
    private final MethodMatcher unboxingInvocationMatcher;
    private final MethodMatcher valueOfInvocationMatcher;

    private PrimitiveCheck(String primitiveName, String className, Type.Primitives tag) {
      this.primitiveName = primitiveName;
      this.className = className;
      this.tag = tag;
      this.message = "Use \"" + parseMethodName() + "\" for this string-to-" + primitiveName + " conversion.";
      this.unboxingInvocationMatcher = MethodMatcher.create()
        .typeDefinition("java.lang." + className)
        .name(primitiveName + "Value");
      this.valueOfInvocationMatcher = MethodMatcher.create()
        .typeDefinition("java.lang." + className)
        .name("valueOf")
        .addParameter("java.lang.String");
    }

    private void checkMethodInvocation(MethodInvocationTree methodInvocationTree) {
      if (unboxingInvocationMatcher.matches(methodInvocationTree)) {
        MemberSelectExpressionTree methodSelect = (MemberSelectExpressionTree) methodInvocationTree.methodSelect();
        checkInstanciation(methodSelect.expression());
      }
    }

    private void checkInstanciation(ExpressionTree expression) {
      if (isBadlyInstanciated(expression)) {
        addIssue(expression, message);
      }
    }

    private boolean isBadlyInstanciated(ExpressionTree expression) {
      boolean result = false;
      if (expression.is(Tree.Kind.NEW_CLASS)) {
        result = isStringBasedConstructor((NewClassTree) expression);
      } else if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
        result = valueOfInvocationMatcher.matches((MethodInvocationTree) expression);
      } else if (expression.is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree identifier = (IdentifierTree) expression;
        Symbol reference = identifier.symbol();
        if (reference.isVariableSymbol() && reference.usages().size() == 1) {
          Symbol.VariableSymbol variableSymbol = (Symbol.VariableSymbol) reference;
          result = isBadlyInstanciatedVariable(variableSymbol);
        }
      }
      return result;
    }

    private boolean isBadlyInstanciatedVariable(Symbol.VariableSymbol variableSymbol) {
      VariableTree variableTree = variableSymbol.declaration();
      if (variableTree != null) {
        ExpressionTree initializer = variableTree.initializer();
        if (initializer != null) {
          return isBadlyInstanciated(initializer);
        }
      }
      return false;
    }

    private boolean isStringBasedConstructor(NewClassTree newClassTree) {
      List<ExpressionTree> arguments = newClassTree.arguments();
      return arguments.get(0).symbolType().is("java.lang.String");
    }

    private String parseMethodName() {
      return className + ".parse" + StringUtils.capitalize(primitiveName);
    }
  }

}
