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
import org.apache.commons.lang.StringUtils;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.npe.NpeVisitor;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2259",
  name = "Null pointers should not be dereferenced",
  tags = {"bug", "cert", "cwe", "owasp-a1", "owasp-a2", "owasp-a6", "security"},
  priority = Priority.BLOCKER)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.LOGIC_RELIABILITY)
@SqaleConstantRemediation("10min")
public class NullPointerDereferenceCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }

    MethodTree methodTree = (MethodTree) tree;
    BlockTree block = methodTree.block();
    if (block != null) {
      NpeVisitor visitor = new NpeVisitor(methodTree.parameters());
      block.accept(visitor);
      for (Tree issueTree : visitor.getIssueTrees()) {
        String name = getNameFrom(issueTree);
        String methodName = visitor.isTreeMethodParam(issueTree);
        if (StringUtils.isNotBlank(methodName)) {
          if (issueTree.is(Tree.Kind.NULL_LITERAL)) {
            addIssue(issueTree, "method '" + methodName + "' does not accept nullable argument");
          } else {
            addIssue(issueTree, "'" + name + "' is nullable here and method '" + methodName + "' does not accept nullable argument");
          }
        } else {
          addIssue(issueTree, "NullPointerException might be thrown as '" + name + "' is nullable here");
        }
      }
    }
  }

  private String getNameFrom(Tree tree) {
    if (tree.is(Tree.Kind.IDENTIFIER)) {
      return ((IdentifierTree) tree).symbol().name();
    } else if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      return ((MethodInvocationTree) tree).symbol().name();
    } else if (tree.is(Tree.Kind.ARRAY_ACCESS_EXPRESSION)) {
      return getNameFrom(((ArrayAccessExpressionTree) tree).expression());
    } else if (tree.is(Tree.Kind.MEMBER_SELECT)) {
      return getNameFrom(((MemberSelectExpressionTree) tree).identifier());
    }
    return "";
  }

}
