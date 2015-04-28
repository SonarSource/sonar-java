/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.MethodInvocationMatcher;
import org.sonar.java.checks.methods.TypeCriteria;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.CheckForNull;

import java.util.List;

@Rule(
  key = "S2864",
  name = "\"entrySet()\" should be iterated when both the key and value are needed",
  tags = {"performance"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.CPU_EFFICIENCY)
@SqaleConstantRemediation("5min")
public class KeySetInsteadOfEntrySetCheck extends SubscriptionBaseVisitor {

  private static final MethodInvocationMatcher MAP_GET_METHOD = MethodInvocationMatcher.create()
    .typeDefinition(TypeCriteria.subtypeOf("java.util.Map"))
    .name("get")
    .addParameter("java.lang.Object");

  private static final MethodInvocationMatcher MAP_KEYSET_METHOD = MethodInvocationMatcher.create()
    .typeDefinition(TypeCriteria.subtypeOf("java.util.Map"))
    .name("keySet");

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.FOR_EACH_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      ForEachStatement forEachTree = (ForEachStatement) tree;
      ExpressionTree expressionTree = forEachTree.expression();
      if (expressionTree.is(Tree.Kind.METHOD_INVOCATION)) {
        MethodInvocationTree methodTree = (MethodInvocationTree) expressionTree;
        Symbol mapSymbol = getMapSymbol(methodTree);
        if (mapSymbol != null && MAP_KEYSET_METHOD.matches(methodTree)) {
          new GetUsageVisitor().isCallingGetWithSymbol(forEachTree, forEachTree.variable().symbol(), mapSymbol);
        }
      }
    }
  }

  @CheckForNull
  private Symbol getMapSymbol(MethodInvocationTree tree) {
    ExpressionTree expressionTree = tree.methodSelect();
    // direct invocation: symbol is implicitly this
    if (expressionTree.is(Tree.Kind.IDENTIFIER)) {
      return tree.symbol().owner();
    }
    expressionTree = ((MemberSelectExpressionTree) expressionTree).expression();
    if (expressionTree.is(Tree.Kind.IDENTIFIER)) {
      return ((IdentifierTree) expressionTree).symbol();
    } else {
      return isFieldAccessedUsingSuperOrThis(expressionTree);
    }
  }

  @CheckForNull
  private Symbol isFieldAccessedUsingSuperOrThis(ExpressionTree expressionTree) {
    if (expressionTree.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree memberSelectTree = ((MemberSelectExpressionTree) expressionTree);
      if (memberSelectTree.expression().is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree identifierTree = (IdentifierTree) memberSelectTree.expression();
        if (identifierTree.identifierToken().text().equals("super") || identifierTree.identifierToken().text().equals("this")) {
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
        addIssue(forEachTree, "Iterate over the \"entrySet\" instead of the \"keySet\".");
      }
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (MAP_GET_METHOD.matches(tree)) {
        Tree firstArgument = Iterables.getOnlyElement(tree.arguments());
        if (getMapSymbol(tree).equals(mapSymbol) && firstArgument.is(Tree.Kind.IDENTIFIER) && ((IdentifierTree) firstArgument).symbol().equals(variable)) {
          result = true;
        }
      }
      super.visitMethodInvocation(tree);
    }
  }

}
