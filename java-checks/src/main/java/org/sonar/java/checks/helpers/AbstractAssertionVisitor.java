/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.helpers;

import javax.annotation.Nullable;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.NewClassTree;

import static org.sonar.java.checks.helpers.UnitTestUtils.ASSERTION_INVOCATION_MATCHERS;
import static org.sonar.java.checks.helpers.UnitTestUtils.methodNameMatchesAssertionMethodPattern;
import static org.sonar.java.model.ExpressionUtils.methodName;

public abstract class AbstractAssertionVisitor extends BaseTreeVisitor {
  private boolean hasAssertion = false;

  @Override
  public void visitMethodInvocation(MethodInvocationTree mit) {
    super.visitMethodInvocation(mit);
    if (!hasAssertion && isAssertion(methodName(mit), mit.methodSymbol())) {
      hasAssertion = true;
    }
  }

  @Override
  public void visitMethodReference(MethodReferenceTree methodReferenceTree) {
    super.visitMethodReference(methodReferenceTree);
    if (!hasAssertion && isAssertion(methodReferenceTree.method(), methodReferenceTree.method().symbol())) {
      hasAssertion = true;
    }
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    super.visitNewClass(tree);
    if (!hasAssertion && isAssertion(null, tree.methodSymbol())) {
      hasAssertion = true;
    }
  }

  public boolean hasAssertion() {
    return hasAssertion;
  }

  protected abstract boolean isAssertion(Symbol methodSymbol);

  private boolean isAssertion(@Nullable IdentifierTree method, Symbol methodSymbol) {
    // To avoid FP, we consider unknown methods as assertions
    return methodSymbol.isUnknown()
      || matchesMethodPattern(method, methodSymbol)
      || ASSERTION_INVOCATION_MATCHERS.matches(methodSymbol)
      || isAssertion(methodSymbol);
  }

  private static boolean matchesMethodPattern(@Nullable IdentifierTree method, Symbol methodSymbol) {
    if (method == null) {
      return false;
    }
    return methodNameMatchesAssertionMethodPattern(method.name(), methodSymbol);
  }

}
