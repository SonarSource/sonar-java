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
import com.google.common.collect.Lists;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.model.expression.NewClassTreeImpl;
import org.sonar.java.resolve.Symbol;
import org.sonar.java.resolve.Symbol.MethodSymbol;
import org.sonar.java.resolve.Type;
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
    List<Type> parametersTypes;
    List<ExpressionTree> arguments;
    if (tree.is(Kind.NEW_CLASS)) {
      NewClassTreeImpl nct = (NewClassTreeImpl) tree;
      parametersTypes = getParametersTypes(getSemanticModel().getReference(nct.getConstructorIdentifier()));
      arguments = nct.arguments();
    } else {
      MethodInvocationTreeImpl mit = (MethodInvocationTreeImpl) tree;
      parametersTypes = getParametersTypes(mit.getSymbol());
      arguments = mit.arguments();
    }
    checkArgumentsTypes(arguments, parametersTypes);
  }

  private List<Type> getParametersTypes(Symbol symbol) {
    if (symbol instanceof MethodSymbol) {
      return ((MethodSymbol) symbol).getParametersTypes();
    }
    return Lists.newArrayList();
  }

  private void checkArgumentsTypes(List<ExpressionTree> arguments, List<Type> parametersTypes) {
    if (!arguments.isEmpty() && arguments.size() == parametersTypes.size()) {
      for (int index = 0; index < arguments.size(); index++) {
        AbstractTypedTree argument = (AbstractTypedTree) arguments.get(index);
        Type expectedType = parametersTypes.get(index);
        Type providedType = argument.getSymbolType();
        if (expectedType.is("java.lang.Runnable") && providedType.isSubtypeOf("java.lang.Thread")) {
          addIssue(argument, MessageFormat.format("\"{0}\" is a \"Thread\".", getArgName(argument, index)));
        }
      }
    }
  }

  private String getArgName(AbstractTypedTree tree, int index) {
    if (tree.is(Kind.IDENTIFIER)) {
      return ((IdentifierTree) tree).name();
    }
    return "arg" + index;
  }
}
