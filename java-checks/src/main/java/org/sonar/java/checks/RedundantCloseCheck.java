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

import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Rule(key = "S4087")
public class RedundantCloseCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatcher AUTOCLOSEABLE_CLOSE = MethodMatcher.create()
    .typeDefinition(TypeCriteria.subtypeOf("java.lang.AutoCloseable"))
    .name("close")
    .withoutParameter();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.TRY_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    TryStatementTree tryStatementTree = (TryStatementTree) tree;
    Set<Symbol> resourceSymbols = tryStatementTree.resourceList().stream()
      .map(RedundantCloseCheck::resourceSymbol)
      .filter(s -> !s.isUnknown())
      .collect(Collectors.toSet());
    if (resourceSymbols.isEmpty()) {
      return;
    }
    tryStatementTree.block().accept(new CloseVisitor(resourceSymbols));
  }

  private static Symbol resourceSymbol(Tree resource) {
    // java 7 try-with resource
    if (resource.is(Tree.Kind.VARIABLE)) {
      return ((VariableTree) resource).symbol();
    }
    // java 9 try-with-resource
    if (resource.is(Tree.Kind.IDENTIFIER)) {
      return ((IdentifierTree) resource).symbol();
    }
    return ((MemberSelectExpressionTree) resource).identifier().symbol();
  }

  private class CloseVisitor extends BaseTreeVisitor {

    final Set<Symbol> resourceSymbols;

    public CloseVisitor(Set<Symbol> resourceSymbols) {
      this.resourceSymbols = resourceSymbols;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (!AUTOCLOSEABLE_CLOSE.matches(tree)) {
        return;
      }
      ExpressionTree methodSelect = tree.methodSelect();
      if (!methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
        return;
      }
      MemberSelectExpressionTree mset = (MemberSelectExpressionTree) methodSelect;
      ExpressionTree expression = mset.expression();
      if (!expression.is(Tree.Kind.IDENTIFIER)) {
        return;
      }
      Symbol closedSymbol = ((IdentifierTree) expression).symbol();
      if (resourceSymbols.contains(closedSymbol)) {
        reportIssue(mset.identifier(), tree.arguments().closeParenToken(),
          "Remove this \"close\" call; closing the resource is handled automatically by the try-with-resources.");
      }
    }
  }

}
