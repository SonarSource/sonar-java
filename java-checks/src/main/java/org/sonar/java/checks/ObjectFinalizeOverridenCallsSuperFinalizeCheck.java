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
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.Nullable;
import java.util.List;

@Rule(
  key = "ObjectFinalizeOverridenCallsSuperFinalizeCheck",
  name = "super.finalize() should be called at the end of Object.finalize() implementations",
  tags = {"bug", "cert", "cwe"},
  priority = Priority.BLOCKER)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.INSTRUCTION_RELIABILITY)
@SqaleConstantRemediation("5min")
public class ObjectFinalizeOverridenCallsSuperFinalizeCheck extends SubscriptionBaseVisitor {


  private MethodInvocationTree lastStatementTree;

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD, Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Kind.METHOD_INVOCATION)) {
      MethodInvocationTree methodInvocationTree = (MethodInvocationTree) tree;
      if (methodInvocationTree.methodSelect().is(Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree mset = (MemberSelectExpressionTree) methodInvocationTree.methodSelect();
        if ("finalize".equals(mset.identifier().name()) && mset.expression().is(Kind.IDENTIFIER) && "super".equals(((IdentifierTree) mset.expression()).name())) {
          lastStatementTree = methodInvocationTree;
        }
      }
    } else {
      if (isFinalize((MethodTree) tree)) {
        lastStatementTree = null;
      }
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Kind.METHOD) && isFinalize((MethodTree) tree)) {
      MethodTree methodTree = (MethodTree) tree;
      if (lastStatementTree == null) {
        addIssue(methodTree.simpleName(), "Add a call to super.finalize() at the end of this Object.finalize() implementation.");
      } else if (!isLastStatement(methodTree, lastStatementTree)) {
        addIssue(lastStatementTree, "Move this super.finalize() call to the end of this Object.finalize() implementation.");
      }
    }
  }

  private static boolean isLastStatement(MethodTree methodTree, MethodInvocationTree lastStatementTree) {
    BlockTree blockTree = methodTree.block();
    if(blockTree != null) {
      for (StatementTree statementTree : blockTree.body()) {
        if(statementTree.is(Kind.TRY_STATEMENT) && isLastStatement(((TryStatementTree) statementTree).finallyBlock(), lastStatementTree)) {
          return true;
        }
      }
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

  private static boolean isFinalize(MethodTree methodTree) {
    if ("finalize".equals(methodTree.simpleName().name()) && methodTree.parameters().isEmpty()) {
      Tree returnType = methodTree.returnType();
      if (returnType != null && returnType.is(Tree.Kind.PRIMITIVE_TYPE)) {
        return "void".equals(((PrimitiveTypeTree) returnType).keyword().text());
      }
    }
    return false;
  }
}
