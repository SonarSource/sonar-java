/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S2234")
public class MethodParametersOrderCheck extends IssuableSubscriptionVisitor {

  private Map<Symbol, List<Symbol>> parameterNamesByMethod = new HashMap<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    parameterNamesByMethod.clear();
    super.scanFile(context);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree methodInvTree = (MethodInvocationTree) tree;
    List<Symbol> definedFunctionParameters = parameterNamesByMethod.get(methodInvTree.symbol());
    MethodTree methodTree = (MethodTree) methodInvTree.symbol().declaration();
    if (definedFunctionParameters == null) {
      if (methodTree != null) {
        definedFunctionParameters = methodTree.parameters().stream().map(VariableTree::symbol).collect(Collectors.toCollection(ArrayList::new));
        parameterNamesByMethod.put(methodTree.symbol(), definedFunctionParameters);
      } else {
        return;
      }
    }
    List<IdentifierTree> argumentsList = methodInvTree.arguments().stream().map(arg -> {
      if (arg.is(Tree.Kind.IDENTIFIER)) {
        return ((IdentifierTree) arg);
      } else if (arg.is(Tree.Kind.MEMBER_SELECT)) {
        return (IdentifierTree) ExpressionUtils.skipParentheses(((MemberSelectExpressionTree) arg).identifier());
      } else {
        return null;
      }
    }).collect(Collectors.toList());
    if (argumentsNamesMatchParametersNames(definedFunctionParameters, argumentsList)
      && argumentsTypesMatchParametersTypesAndNamesNotOrdered(definedFunctionParameters, argumentsList)) {
      List<JavaFileScannerContext.Location> flow = methodTree.parameters().stream().map(param -> new JavaFileScannerContext.Location("Formal Parameters", param))
        .collect(Collectors.toList());
      reportIssue(methodInvTree.arguments(), "Parameters to " + methodInvTree.symbol().name() + " have the same names but not the same order as the method arguments.",
        flow, null);

    }
  }

  private static boolean argumentsNamesMatchParametersNames(List<Symbol> parametersList, List<IdentifierTree> argumentsList) {
    List<String> argListNames = argumentsList.stream().filter(Objects::nonNull).map(parameter -> parameter.name().toLowerCase(Locale.ENGLISH)).collect(Collectors.toList());
    return allUnique(argListNames)
      && argListNames.stream().allMatch(arg -> parametersList.stream().map(parameter -> parameter.name().toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()).contains(arg));

  }

  public static boolean allUnique(List<String> argListNames) {
    return argListNames.stream().allMatch(new HashSet<>()::add);
  }

  private static boolean argumentsTypesMatchParametersTypesAndNamesNotOrdered(List<Symbol> parameterList, List<IdentifierTree> argumentList) {
    Iterator<IdentifierTree> argumentsIterator = argumentList.stream().filter(Objects::nonNull).iterator();
    int countArgumentsNotOrdered = 0;
    while (argumentsIterator.hasNext()) {
      IdentifierTree argument = argumentsIterator.next();
      int index = (parameterList.stream().map(param -> param.name().toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()))
        .indexOf(argument.name().toLowerCase(Locale.ENGLISH));
      if (parameterList.get(index).type().equals(argument.symbolType())) {
        if (argumentList.indexOf(argument) != index) {
          countArgumentsNotOrdered++;
        }
        continue;
      } else {
        return false;
      }
    }
    return countArgumentsNotOrdered >= 2;
  }
}
