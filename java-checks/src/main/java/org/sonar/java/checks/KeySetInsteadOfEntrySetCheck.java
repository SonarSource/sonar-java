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

import com.google.common.collect.Iterables;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import javax.annotation.CheckForNull;
import java.util.Collections;
import java.util.List;

@Rule(key = "S2864")
public class KeySetInsteadOfEntrySetCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatcher MAP_GET_METHOD = MethodMatcher.create()
    .typeDefinition(TypeCriteria.subtypeOf("java.util.Map"))
    .name("get")
    .addParameter("java.lang.Object");

  private static final MethodMatcher MAP_KEYSET_METHOD = MethodMatcher.create()
    .typeDefinition(TypeCriteria.subtypeOf("java.util.Map"))
    .name("keySet")
    .withoutParameter();

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.FOR_EACH_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      ForEachStatement forEachTree = (ForEachStatement) tree;
      ExpressionTree expressionTree = forEachTree.expression();
      if (expressionTree.is(Tree.Kind.METHOD_INVOCATION)) {
        MethodInvocationTree methodTree = (MethodInvocationTree) expressionTree;
        Symbol ownerSymbol = getOwnerSymbol(methodTree);
        if (ownerSymbol != null && MAP_KEYSET_METHOD.matches(methodTree)) {
          new GetUsageVisitor().isCallingGetWithSymbol(forEachTree, forEachTree.variable().symbol(), ownerSymbol);
        }
      }
    }
  }

  @CheckForNull
  private static Symbol getOwnerSymbol(MethodInvocationTree tree) {
    ExpressionTree expressionTree = tree.methodSelect();
    // direct invocation: symbol is implicitly this
    if (expressionTree.is(Tree.Kind.IDENTIFIER)) {
      return tree.symbol().owner();
    }
    expressionTree = ((MemberSelectExpressionTree) expressionTree).expression();
    if (expressionTree.is(Tree.Kind.IDENTIFIER)) {
      return ((IdentifierTree) expressionTree).symbol();
    } else {
      return getFieldAccessedUsingSuperOrThis(expressionTree);
    }
  }

  @CheckForNull
  private static Symbol getFieldAccessedUsingSuperOrThis(ExpressionTree expressionTree) {
    if (expressionTree.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree memberSelectTree = (MemberSelectExpressionTree) expressionTree;
      if (memberSelectTree.expression().is(Tree.Kind.IDENTIFIER)) {
        String identifierText = ((IdentifierTree) memberSelectTree.expression()).identifierToken().text();
        if ("super".equals(identifierText) || "this".equals(identifierText)) {
          return memberSelectTree.identifier().symbol();
        }
      }
    }
    return null;
  }

  private class GetUsageVisitor extends BaseTreeVisitor {
    private Symbol variable;
    private boolean result;
    private Symbol mapSymbol;

    public void isCallingGetWithSymbol(ForEachStatement forEachTree, Symbol variable, Symbol mapSymbol) {
      this.variable = variable;
      result = false;
      this.mapSymbol = mapSymbol;
      scan(forEachTree.statement());
      if (result) {
        reportIssue(forEachTree.forKeyword(), "Iterate over the \"entrySet\" instead of the \"keySet\".");
      }
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (MAP_GET_METHOD.matches(tree)) {
        Tree firstArgument = Iterables.getOnlyElement(tree.arguments());
        if (mapSymbol.equals(getOwnerSymbol(tree)) && firstArgument.is(Tree.Kind.IDENTIFIER) && ((IdentifierTree) firstArgument).symbol().equals(variable)) {
          result = true;
          return;
        }
      }
      super.visitMethodInvocation(tree);
    }
  }

}
