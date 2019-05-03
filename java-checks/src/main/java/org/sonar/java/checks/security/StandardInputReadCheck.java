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
package org.sonar.java.checks.security;

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.MethodMatcherCollection;
import org.sonar.java.matcher.NameCriteria;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4829")
public class StandardInputReadCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatcherCollection METHOD_MATCHERS = MethodMatcherCollection.create(
    MethodMatcher.create().typeDefinition("java.lang.System").name("setIn").withAnyParameters(),
    MethodMatcher.create().typeDefinition("java.io.Console").name(NameCriteria.startsWith("read")).withAnyParameters());

  private static final MethodMatcher CLOSE_METHOD = MethodMatcher.create().name("close").withoutParameter();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD_INVOCATION, Tree.Kind.METHOD_REFERENCE, Tree.Kind.IDENTIFIER);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }

    if (tree.is(Tree.Kind.METHOD_INVOCATION) && METHOD_MATCHERS.anyMatch((MethodInvocationTree) tree)) {
      reportIssue(tree);
    } else if (tree.is(Tree.Kind.METHOD_REFERENCE) && METHOD_MATCHERS.anyMatch((MethodReferenceTree) tree)) {
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
