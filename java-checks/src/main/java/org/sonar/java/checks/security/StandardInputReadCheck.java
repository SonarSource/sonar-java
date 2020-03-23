/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java.checks.security;

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4829")
public class StandardInputReadCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatchers METHOD_MATCHERS = MethodMatchers.or(
    MethodMatchers.create().ofTypes("java.lang.System").names("setIn").withAnyParameters().build(),
    MethodMatchers.create().ofTypes("java.io.Console").name(name -> name.startsWith("read")).withAnyParameters().build());

  private static final MethodMatchers CLOSE_METHOD = MethodMatchers.create().ofAnyType().names("close").addWithoutParametersMatcher().build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD_INVOCATION, Tree.Kind.METHOD_REFERENCE, Tree.Kind.IDENTIFIER);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }

    if (tree.is(Tree.Kind.METHOD_INVOCATION) && METHOD_MATCHERS.matches((MethodInvocationTree) tree)) {
      reportIssue(tree);
    } else if (tree.is(Tree.Kind.METHOD_REFERENCE) && METHOD_MATCHERS.matches((MethodReferenceTree) tree)) {
      reportIssue(tree);
    } else if (tree.is(Tree.Kind.IDENTIFIER)) {
      checkIdentifier((IdentifierTree) tree);
    }
  }

  private void checkIdentifier(IdentifierTree identifier) {
    Symbol.TypeSymbol enclosingClass = identifier.symbol().enclosingClass();
    if (enclosingClass != null
      && enclosingClass.type().is("java.lang.System")
      && identifier.symbolType().is("java.io.InputStream")
      && identifier.name().equals("in")
      && !isClosingStream(identifier.parent())) {
      reportIssue(identifier);
    }
  }

  private static boolean isClosingStream(Tree parentExpression) {
    if (parentExpression.is(Tree.Kind.PARENTHESIZED_EXPRESSION) || parentExpression.is(Tree.Kind.MEMBER_SELECT)) {
      return isClosingStream(parentExpression.parent());
    } else if (parentExpression.is(Tree.Kind.METHOD_INVOCATION)) {
      return CLOSE_METHOD.matches((MethodInvocationTree) parentExpression);
    } else if (parentExpression.is(Tree.Kind.METHOD_REFERENCE)) {
      return CLOSE_METHOD.matches((MethodReferenceTree) parentExpression);
    }
    return false;
  }

  private void reportIssue(Tree tree) {
    reportIssue(tree, "Make sure that reading the standard input is safe here.");
  }

}
