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

import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
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

  private MethodInvocationTree lastStatementTree;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree methodInvocationTree = (MethodInvocationTree) tree;
      if (methodInvocationTree.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree mset = (MemberSelectExpressionTree) methodInvocationTree.methodSelect();
        if (FINALIZE.equals(mset.identifier().name()) && mset.expression().is(Tree.Kind.IDENTIFIER) && "super".equals(((IdentifierTree) mset.expression()).name())) {
          lastStatementTree = methodInvocationTree;
        }
      }
    } else if (FINALIZE_MATCHER.matches((MethodTree) tree)) {
      lastStatementTree = null;
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD)) {
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
      && blockTree.body().stream().anyMatch(statement -> 
        statement.is(Tree.Kind.TRY_STATEMENT)&& isLastStatement(((TryStatementTree) statement).finallyBlock(), lastStatementTree))) {
      return true;
    }
    return isLastStatement(blockTree, lastStatementTree);
  }

  private static boolean isLastStatement(@Nullable BlockTree blockTree, MethodInvocationTree lastStatementTree) {
    if (blockTree != null) {
      StatementTree last = ListUtils.getLast(blockTree.body());
      if (last.is(Tree.Kind.EXPRESSION_STATEMENT)) {
        return lastStatementTree.equals(((ExpressionStatementTree) last).expression());
      } else if (last.is(Tree.Kind.TRY_STATEMENT)) {
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
