/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

public class UnresolvedIdentifiersVisitor extends BaseTreeVisitor {

  private Set<Symbol> unresolvedIdentifierNames = new HashSet<>();

  @Override
  public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
    // skip annotations and identifier, a method parameter will only be used in expression side (before the dot)
    scan(tree.expression());
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    ExpressionTree methodSelect = tree.methodSelect();
    if (!methodSelect.is(Tree.Kind.IDENTIFIER)) {
      // not interested in simple method invocations, we are targeting usage of method parameters
      scan(methodSelect);
    }
    scan(tree.typeArguments());
    scan(tree.arguments());
  }

  @Override
  public void visitIdentifier(IdentifierTree tree) {
    if (tree.symbol().isUnknown()) {
      unresolvedIdentifierNames.add(tree.symbol());
    }
    super.visitIdentifier(tree);
  }

  public Set<String> check(Tree tree) {
    unresolvedIdentifierNames.clear();
    tree.accept(this);
    return unresolvedNames();
  }

  public Set<String> check(List<? extends Tree> trees) {
    unresolvedIdentifierNames.clear();
    trees.forEach(tree -> tree.accept(this));
    return unresolvedNames();
  }

  private Set<String> unresolvedNames() {
    return unresolvedIdentifierNames.stream().map(Symbol::name).collect(Collectors.toSet());
  }

  public boolean isUnresolved(Symbol candidate) {
    return unresolvedIdentifierNames.contains(candidate);
  }
}
