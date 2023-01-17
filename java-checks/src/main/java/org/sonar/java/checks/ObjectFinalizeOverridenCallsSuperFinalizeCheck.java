/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import java.util.List;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonarsource.analyzer.commons.collections.ListUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
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
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;

@DeprecatedRuleKey(ruleKey = "ObjectFinalizeOverridenCallsSuperFinalizeCheck", repositoryKey = "squid")
@Rule(key = "S1114")
public class ObjectFinalizeOverridenCallsSuperFinalizeCheck extends IssuableSubscriptionVisitor {

  private static final String FINALIZE = "finalize";
  private static final MethodMatchers FINALIZE_MATCHER = MethodMatchers.create()
    .ofAnyType().names(FINALIZE).addWithoutParametersMatcher().build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    final MethodTree methodTree = (MethodTree) tree;
    if (isFinalizeOverriddenMethod(methodTree)) {
      final BlockTree blockTree = methodTree.block();
      if (blockTree != null) {
        final MethodInvocationTree lastSuperFinalizeInvocation = findLastSuperFinalizeInvocation(blockTree);
        if (lastSuperFinalizeInvocation == null) {
          reportIssue(methodTree.simpleName(), "Add a call to super.finalize() at the end of this Object.finalize() implementation.");
        } else if (!isLastStatement(blockTree, lastSuperFinalizeInvocation)) {
          reportIssue(lastSuperFinalizeInvocation, "Move this super.finalize() call to the end of this Object.finalize() implementation.");
        }
      }
    }
  }

  private static boolean isFinalizeOverriddenMethod(MethodTree methodTree) {
    return FINALIZE_MATCHER.matches(methodTree) && doesOverrideFinalize(methodTree.symbol().owner());
  }

  private static boolean doesOverrideFinalize(@Nullable Symbol classSymbol) {
    if ((classSymbol != null) && (classSymbol.isTypeSymbol())) {
      Type superClassType = ((Symbol.TypeSymbol) classSymbol).superClass();
      while (superClassType != null && !superClassType.is("java.lang.Object")) {
        final Symbol.TypeSymbol currentClass = superClassType.symbol();
        if (currentClass.lookupSymbols(FINALIZE).stream().anyMatch(FINALIZE_MATCHER::matches)) {
          return true;
        }
        superClassType = currentClass.superClass();
      }
    }
    return false;
  }

  @Nullable
  private static MethodInvocationTree findLastSuperFinalizeInvocation(BlockTree blockTree) {
    final FindLastSuperFinalizeInvocationVisitor visitor = new FindLastSuperFinalizeInvocationVisitor();
    blockTree.accept(visitor);
    return visitor.getLastSuperFinalizeInvocation();
  }

  private static boolean isLastStatement(BlockTree blockTree, MethodInvocationTree lastStatementTree) {
    if (blockTree.body().stream().anyMatch(statement ->
      statement.is(Tree.Kind.TRY_STATEMENT) && isLastStatementInner(((TryStatementTree) statement).finallyBlock(), lastStatementTree)
    )) {
      return true;
    }
    return isLastStatementInner(blockTree, lastStatementTree);
  }

  private static boolean isLastStatementInner(@Nullable BlockTree blockTree, MethodInvocationTree lastStatementTree) {
    if (blockTree != null) {
      final StatementTree last = ListUtils.getLast(blockTree.body());
      if (last.is(Tree.Kind.EXPRESSION_STATEMENT)) {
        return lastStatementTree.equals(((ExpressionStatementTree) last).expression());
      } else if (last.is(Tree.Kind.TRY_STATEMENT)) {
        return isLastStatementInner(((TryStatementTree) last).finallyBlock(), lastStatementTree);
      }
    }
    return false;
  }

  private static class FindLastSuperFinalizeInvocationVisitor extends BaseTreeVisitor {

    @Nullable
    private MethodInvocationTree lastSuperFinalizeInvocation;

    @Nullable
    MethodInvocationTree getLastSuperFinalizeInvocation() {
      return lastSuperFinalizeInvocation;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (tree.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
        final MemberSelectExpressionTree mset = (MemberSelectExpressionTree) tree.methodSelect();
        if (FINALIZE.equals(mset.identifier().name()) && mset.expression().is(Tree.Kind.IDENTIFIER) && "super".equals(((IdentifierTree) mset.expression()).name())) {
          lastSuperFinalizeInvocation = tree;
        }
      }
    }
  }
}
