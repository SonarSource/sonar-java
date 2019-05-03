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
import org.sonar.java.RspecKey;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.TryStatementTree;

import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.List;

@Rule(key = "ObjectFinalizeOverridenCallsSuperFinalizeCheck")
@RspecKey("S1114")
public class ObjectFinalizeOverridenCallsSuperFinalizeCheck extends IssuableSubscriptionVisitor {

  private static final String FINALIZE = "finalize";
  private static final MethodMatcher FINALIZE_MATCHER = MethodMatcher.create().name(FINALIZE).withoutParameter();

  private MethodInvocationTree lastStatementTree;

  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD, Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    if (tree.is(Kind.METHOD_INVOCATION)) {
      MethodInvocationTree methodInvocationTree = (MethodInvocationTree) tree;
      if (methodInvocationTree.methodSelect().is(Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree mset = (MemberSelectExpressionTree) methodInvocationTree.methodSelect();
        if (FINALIZE.equals(mset.identifier().name()) && mset.expression().is(Kind.IDENTIFIER) && "super".equals(((IdentifierTree) mset.expression()).name())) {
          lastStatementTree = methodInvocationTree;
        }
      }
    } else if (FINALIZE_MATCHER.matches((MethodTree) tree)) {
      lastStatementTree = null;
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (hasSemantic() && tree.is(Kind.METHOD)) {
      MethodTree methodTree = (MethodTree) tree;
      if (FINALIZE_MATCHER.matches(methodTree) && doesOverrideFinalize(methodTree.symbol().owner())) {
        if (lastStatementTree == null) {
          reportIssue(methodTree.simpleName(), "Add a call to super.finalize() at the end of this Object.finalize() implementation.");
        } else if (!isLastStatement(methodTree, lastStatementTree)) {
          reportIssue(lastStatementTree, "Move this super.finalize() call to the end of this Object.finalize() implementation.");
        }
      }
    }
  }

  private static boolean isLastStatement(MethodTree methodTree, MethodInvocationTree lastStatementTree) {
    BlockTree blockTree = methodTree.block();
    if (blockTree != null
      && blockTree.body().stream().anyMatch(statement -> statement.is(Kind.TRY_STATEMENT) && isLastStatement(((TryStatementTree) statement).finallyBlock(), lastStatementTree))) {
      return true;
    }
    return isLastStatement(blockTree, lastStatementTree);
  }

  private static boolean isLastStatement(@Nullable BlockTree blockTree, MethodInvocationTree lastStatementTree) {
    if (blockTree != null) {
      StatementTree last = Iterables.getLast(blockTree.body());
      if (last.is(Kind.EXPRESSION_STATEMENT)) {
        return lastStatementTree.equals(((ExpressionStatementTree) last).expression());
      } else if (last.is(Kind.TRY_STATEMENT)) {
        return isLastStatement(((TryStatementTree) last).finallyBlock(), lastStatementTree);
      }
    }
    return false;
  }

  private static boolean doesOverrideFinalize(Symbol classSymbol) {
    if (classSymbol.isTypeSymbol()) {
      Type superClassType = ((Symbol.TypeSymbol) classSymbol).superClass();
      while (superClassType != null && !superClassType.is("java.lang.Object")) {
        Symbol.TypeSymbol currentClass = superClassType.symbol();
        if (currentClass.lookupSymbols(FINALIZE).stream().anyMatch(FINALIZE_MATCHER::matches)) {
          return true;
        }
        superClassType = currentClass.superClass();
      }
    }
    return false;
  }

}
