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
import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.InstanceOfTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S2118")
public class NonSerializableWriteCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatcher WRITE_OBJECT_MATCHER = MethodMatcher.create()
    .typeDefinition("java.io.ObjectOutputStream")
    .name("writeObject")
    .addParameter("java.lang.Object");

  private final List<Symbol> testedSymbols = new ArrayList<>();

  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD_INVOCATION, Tree.Kind.INSTANCE_OF);
  }

  @Override
  public void setContext(JavaFileScannerContext context) {
    testedSymbols.clear();
    super.setContext(context);
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
        visitMethodInvocation((MethodInvocationTree) tree);
      } else {
        visitInstanceOf((InstanceOfTree) tree);
      }
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
      if (!isAcceptableType(argument.symbolType()) && !isTestedSymbol(argument) && !hasSerializableConcreteType(argument)) {
        reportIssue(argument, "Make the \"" + argument.symbolType().fullyQualifiedName() + "\" class \"Serializable\" or don't write it.");
      }
    }
  }

  private static boolean hasSerializableConcreteType(ExpressionTree argument) {
    if (argument.is(Kind.IDENTIFIER)) {
      IdentifierTree argument1 = (IdentifierTree) argument;
      Tree declaration = argument1.symbol().declaration();
      if (argument1.symbol().isFinal() && declaration != null && declaration.is(Kind.VARIABLE)) {
        ExpressionTree initializer = ((VariableTree) declaration).initializer();
        return initializer != null && isAcceptableType(initializer.symbolType());
      }
    }
    return false;
  }

  private static boolean isAcceptableType(org.sonar.plugins.java.api.semantic.Type argType) {
    return argType.isSubtypeOf("java.io.Serializable")
      || argType.is("java.lang.Object")
      || argType.isPrimitive()
      || !argType.isClass();
  }

}
