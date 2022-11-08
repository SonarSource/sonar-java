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
package org.sonar.java.checks.tests;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5969")
public class MockingAllMethodsCheck extends AbstractMethodDetection {

  private final Map<Symbol, Set<Symbol>> mockedMethodsPerObject = new HashMap<>();
  private final Map<Symbol, MethodInvocationTree> whenCalls = new HashMap<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD_INVOCATION, Tree.Kind.METHOD);
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create().ofTypes("org.mockito.Mockito").names("when").withAnyParameters().build();
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD)) {
      mockedMethodsPerObject.clear();
      whenCalls.clear();
    } else {
      super.visitNode(tree);
    }
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    ExpressionTree argument = mit.arguments().get(0);
    if (argument.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mockedMethodCall = (MethodInvocationTree) argument;
      ExpressionTree methodSelect = mockedMethodCall.methodSelect();
      if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree mockedMethod = (MemberSelectExpressionTree) methodSelect;
        ExpressionTree object = mockedMethod.expression();
        if (object.is(Tree.Kind.IDENTIFIER)) {
          Symbol objectSymbol = ((IdentifierTree) object).symbol();
          Symbol method = mockedMethod.identifier().symbol();
          mockedMethodsPerObject.computeIfAbsent(objectSymbol, key -> new HashSet<>()).add(method);
          whenCalls.put(method, mit);
        }
      }
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD)) {
      for (Map.Entry<Symbol, Set<Symbol>> entry : mockedMethodsPerObject.entrySet()) {
        Symbol mockedObject = entry.getKey();
        Tree declaration = mockedObject.declaration();
        if (declaration != null) {
          Set<Symbol> mockedMethods = entry.getValue();
          Set<Symbol> declaredMethods = mockedObject.type().symbol().memberSymbols().stream()
            .filter(MockingAllMethodsCheck::isNonPrivateMethod)
            .collect(Collectors.toSet());
          if (declaredMethods.size() > 1 && mockedMethods.containsAll(declaredMethods)) {
            List<JavaFileScannerContext.Location> secondaries = mockedMethods.stream()
              .map(method -> new JavaFileScannerContext.Location("Method mocked here", whenCalls.get(method)))
              .collect(Collectors.toList());
            reportIssue(declaration, "Refactor this test instead of mocking every non-private member of this class.", secondaries, null);
          }
        }
      }
    }
  }

  private static boolean isNonPrivateMethod(Symbol symbol) {
    Tree declaration = symbol.declaration();
    return symbol.isMethodSymbol() && !symbol.isPrivate() && declaration != null
      && !declaration.is(Tree.Kind.CONSTRUCTOR);
  }
}
