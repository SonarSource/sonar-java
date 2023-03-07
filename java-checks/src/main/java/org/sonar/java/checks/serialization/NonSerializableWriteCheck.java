/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.checks.serialization;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.InstanceOfTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2118")
public class NonSerializableWriteCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatchers WRITE_OBJECT_MATCHER = MethodMatchers.create()
    .ofTypes("java.io.ObjectOutputStream")
    .names("writeObject")
    .addParametersMatcher("java.lang.Object")
    .build();

  private final Set<Symbol> testedSymbols = new HashSet<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD_INVOCATION, Tree.Kind.INSTANCE_OF);
  }

  @Override
  public void setContext(JavaFileScannerContext context) {
    testedSymbols.clear();
    super.setContext(context);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      visitMethodInvocation((MethodInvocationTree) tree);
    } else {
      visitInstanceOf((InstanceOfTree) tree);
    }
  }

  private void visitInstanceOf(InstanceOfTree instanceOfTree) {
    ExpressionTree expression = instanceOfTree.expression();
    if (expression.is(Tree.Kind.IDENTIFIER) && instanceOfTree.type().symbolType().is("java.io.Serializable")) {
      testedSymbols.add(((IdentifierTree) expression).symbol());
    }
  }

  // If we met a test such as "x instanceof Serializable", we suppose that symbol x is Serializable
  private boolean isTestedSymbol(ExpressionTree tree) {
    if (tree.is(Tree.Kind.IDENTIFIER)) {
      Symbol symbol = ((IdentifierTree) tree).symbol();
      return testedSymbols.contains(symbol);
    }
    return false;
  }

  private void visitMethodInvocation(MethodInvocationTree methodInvocation) {
    if (WRITE_OBJECT_MATCHER.matches(methodInvocation)) {
      ExpressionTree argument = methodInvocation.arguments().get(0);
      if (!isTestedSymbol(argument) && ExpressionsHelper.isNotSerializable(argument)) {
        reportIssue(argument, "Make the \"" + argument.symbolType().fullyQualifiedName() + "\" class \"Serializable\" or don't write it.");
      }
    }
  }

}
