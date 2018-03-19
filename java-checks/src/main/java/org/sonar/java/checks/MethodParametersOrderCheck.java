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

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2234")
public class MethodParametersOrderCheck extends IssuableSubscriptionVisitor {

  private Map<Symbol, LinkedHashSet<String>> methodNameKeyAndParameteresValue = new HashMap<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    methodNameKeyAndParameteresValue.clear();
    super.scanFile(context);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD)) {
      MethodTree methodTree = (MethodTree) tree;
      methodNameKeyAndParameteresValue.put(methodTree.symbol(), new LinkedHashSet<>(methodTree.parameters().stream().map(param -> param.simpleName().name()
        .toLowerCase(Locale.ENGLISH)).collect(Collectors.toList())));
      return;
    } else {
      MethodInvocationTree methodInvTree = (MethodInvocationTree) tree;
      LinkedHashSet<String> definedFunctionParameters = methodNameKeyAndParameteresValue.get(methodInvTree.symbol());
      LinkedHashSet<String> invocationParameters = new LinkedHashSet<>(
        methodInvTree.arguments().stream().map(param -> param.toString().toLowerCase(Locale.ENGLISH)).collect(Collectors.toList()));
      if (definedFunctionParameters == null || invocationParameters.isEmpty()) {
        return;
      }
      if (matchParameters(definedFunctionParameters, invocationParameters)) {
        return;
      } else {
        reportIssue(methodInvTree, "Parameters to " + methodInvTree.symbol().name() + " have the same names but not the same order as the method arguments.");
      }
    }
  }

  private static boolean matchParameters(LinkedHashSet<String> functionParameters, LinkedHashSet<String> invocationParameters) {
    if (invocationParameters.containsAll(functionParameters)) {
      return new ArrayList<>(functionParameters).equals(new ArrayList<>(invocationParameters));
    }
    return true;
  }
}
