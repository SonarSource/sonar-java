/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.model.expression.NewClassTreeImpl;
import org.sonar.java.resolve.Symbol;
import org.sonar.java.resolve.Symbol.MethodSymbol;
import org.sonar.java.resolve.Type;
import org.sonar.java.resolve.Type.ArrayType;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.text.MessageFormat;
import java.util.List;

@Rule(
  key = "S2438",
  name = "\"Threads\" should not be used where \"Runnables\" are expected",
  tags = {"multi-threading", "pitfall"},
  priority = Priority.CRITICAL)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.INSTRUCTION_RELIABILITY)
@SqaleConstantRemediation("15min")
public class ThreadAsRunnableArgumentCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.NEW_CLASS, Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    List<ExpressionTree> arguments;
    Symbol methodSymbol;
    if (tree.is(Kind.NEW_CLASS)) {
      NewClassTreeImpl nct = (NewClassTreeImpl) tree;
      methodSymbol = getSemanticModel().getReference(nct.getConstructorIdentifier());
      arguments = nct.arguments();
    } else {
      MethodInvocationTreeImpl mit = (MethodInvocationTreeImpl) tree;
      methodSymbol = mit.getSymbol();
      arguments = mit.arguments();
    }
    // FIXME semantic - symbol should never be null!
    if (!arguments.isEmpty() && methodSymbol != null && methodSymbol.isKind(Symbol.MTH)) {
      checkArgumentsTypes(arguments, (MethodSymbol) methodSymbol);
    }
  }

  private void checkArgumentsTypes(List<ExpressionTree> arguments, MethodSymbol methodSymbol) {
    List<Type> parametersTypes = methodSymbol.getParametersTypes();
    // FIXME semantic - if there is arguments and method symbol has been resolved, the list of parameters should not be empty!
    if (!parametersTypes.isEmpty()) {
      for (int index = 0; index < arguments.size(); index++) {
        AbstractTypedTree argument = (AbstractTypedTree) arguments.get(index);
        Type providedType = argument.getSymbolType();
        Type expectedType = getExpectedType(providedType, parametersTypes, index, methodSymbol.isVarArgs());
        if (expectedType.is("java.lang.Runnable") && providedType.isSubtypeOf("java.lang.Thread")
          || (expectedType.is("java.lang.Runnable[]") && (providedType.isSubtypeOf("java.lang.Thread[]")))) {
          addIssue(argument, getMessage(argument, providedType, index));
        }
      }
    }
  }

  private Type getExpectedType(Type providedType, List<Type> parametersTypes, int index, boolean varargs) {
    int lastParameterIndex = parametersTypes.size() - 1;
    Type lastParameterType = parametersTypes.get(lastParameterIndex);
    Type lastExpectedType = varargs ? ((ArrayType) lastParameterType).elementType() : lastParameterType;
    if (index > lastParameterIndex || (index == lastParameterIndex && varargs && !providedType.isTagged(Type.ARRAY))) {
      return lastExpectedType;
    }
    return parametersTypes.get(index);
  }

  private String getMessage(AbstractTypedTree argument, Type providedType, int index) {
    String array = providedType.isTagged(Type.ARRAY) ? "[]" : "";
    return MessageFormat.format("\"{0}\" is a \"Thread{1}\".", getArgName(argument, index), array);
  }

  private String getArgName(AbstractTypedTree tree, int index) {
    if (tree.is(Kind.IDENTIFIER)) {
      return ((IdentifierTree) tree).name();
    }
    return "Argument " + (index + 1);
  }
}
