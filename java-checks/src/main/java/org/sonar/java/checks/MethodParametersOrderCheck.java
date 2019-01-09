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
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S2234")
public class MethodParametersOrderCheck extends IssuableSubscriptionVisitor {

  private Map<Symbol, ParametersList> parametersByMethod = new HashMap<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void setContext(JavaFileScannerContext context) {
    parametersByMethod.clear();
    super.setContext(context);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree methodInvTree = (MethodInvocationTree) tree;
    MethodTree methodDeclaration = (MethodTree) methodInvTree.symbol().declaration();
    if (methodDeclaration == null) {
      return;
    }
    ParametersList formalParameterList = parametersByMethod.computeIfAbsent(methodInvTree.symbol(), m -> new ParametersList(methodDeclaration));
    List<IdentifierTree> argumentsList = methodInvTree.arguments().stream().map(this::argumentToIdentifier).collect(Collectors.toList());
    if (matchingNames(formalParameterList, argumentsList)) {
      List<VariableTree> matchingTypesWrongOrder = matchingTypesWrongOrder(formalParameterList, argumentsList);
      if (!matchingTypesWrongOrder.isEmpty()) {
        List<JavaFileScannerContext.Location> flow = matchingTypesWrongOrder.stream().map(param -> new JavaFileScannerContext.Location("Misplaced Parameter", param))
          .collect(Collectors.toList());
        reportIssue(methodInvTree.arguments(), "Parameters to " + methodInvTree.symbol().name() + " have the same names but not the same order as the method arguments.", flow,
          null);
      }
    }
  }

  private static boolean matchingNames(ParametersList formalParameters, List<IdentifierTree> argumentsList) {
    List<String> argListNames = argumentsList.stream().filter(Objects::nonNull).map(arg -> arg.name().toLowerCase(Locale.ENGLISH)).collect(Collectors.toList());
    return allUnique(argListNames)
      && argListNames.stream().allMatch(formalParameters::hasArgumentWithName);
  }

  public IdentifierTree argumentToIdentifier(ExpressionTree expr) {
    if (expr.is(Tree.Kind.IDENTIFIER)) {
      return (IdentifierTree) ExpressionUtils.skipParentheses(expr);
    } else if (expr.is(Tree.Kind.MEMBER_SELECT)) {
      return (IdentifierTree) ExpressionUtils.skipParentheses(((MemberSelectExpressionTree) expr).identifier());
    } else {
      return null;
    }
  }

  public static boolean allUnique(List<String> argListNames) {
    return argListNames.size() == new HashSet<>(argListNames).size();
  }

  private static List<VariableTree> matchingTypesWrongOrder(ParametersList formalParameterList, List<IdentifierTree> argumentList) {
    Iterator<IdentifierTree> argumentsIterator = argumentList.stream().filter(Objects::nonNull).iterator();
    List<VariableTree> misplacedParameters = new ArrayList<>();
    while (argumentsIterator.hasNext()) {
      IdentifierTree argument = argumentsIterator.next();
      int index = formalParameterList.indexOf(argument.name().toLowerCase(Locale.ENGLISH));
      Type formalType = formalParameterList.typeOfIndex(index);
      Type argType = argument.symbolType();
      if (!formalType.is(argType.fullyQualifiedName()) || formalType.isUnknown() || argType.isUnknown()) {
        return Collections.emptyList();
      }
      if (argumentList.indexOf(argument) != index) {
        misplacedParameters.add(formalParameterList.parameterAt(index));
      }
    }
    if (misplacedParameters.size() >= 2) {
      return misplacedParameters;
    }
    return Collections.emptyList();
  }

  private static class ParametersList {

    private List<String> parameterNames;
    private List<Type> parameterTypes;
    private List<VariableTree> parameters;

    public ParametersList(MethodTree methodTree) {
      parameterNames = new ArrayList<>();
      parameterTypes = new ArrayList<>();
      methodTree.parameters().stream().map(VariableTree::symbol).forEach(symbol -> {
        parameterNames.add(symbol.name().toLowerCase(Locale.ENGLISH));
        parameterTypes.add(symbol.type());
      });
      parameters = methodTree.parameters();
    }

    public boolean hasArgumentWithName(String argument) {
      return parameterNames.contains(argument);
    }

    public int indexOf(String argName) {
      return parameterNames.indexOf(argName);
    }

    public Type typeOfIndex(int index) {
      return parameterTypes.get(index);
    }

    public VariableTree parameterAt(int index) {
      return parameters.get(index);
    }
  }
}
