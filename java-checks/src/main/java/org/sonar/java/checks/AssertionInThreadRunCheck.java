/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
import com.google.common.collect.Lists;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2186",
  name = "JUnit assertions should not be used in \"run\" methods",
  tags = {"junit", "pitfall"},
  priority = Priority.CRITICAL)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNIT_TESTABILITY)
@SqaleConstantRemediation("30min")
@ActivatedByDefault
public class AssertionInThreadRunCheck extends SubscriptionBaseVisitor {

  private static final Iterable<String> CHECKED_TYPES = Lists.newArrayList("org.junit.Assert",
      "junit.framework.Assert",
      "junit.framework.TestCase",
      "org.fest.assertions.Assertions");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    BlockTree block = methodTree.block();
    if (block != null && isRunMethod(methodTree)) {
      block.accept(new AssertionsVisitor());
    }
  }

  private static boolean isRunMethod(MethodTree methodTree) {
    return methodTree.symbol().owner().type().isSubtypeOf("java.lang.Runnable") && "run".equals(methodTree.simpleName().name()) && methodTree.parameters().isEmpty();
  }

  private class AssertionsVisitor extends BaseTreeVisitor {
    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      Type type = tree.symbol().owner().type();
      if (isCheckedType(type)) {
        addIssue(tree, "Remove this assertion.");
      }
      super.visitMethodInvocation(tree);
    }

    private boolean isCheckedType(Type type) {
      for (String checkedType : CHECKED_TYPES) {
        if(type.is(checkedType)) {
          return true;
        }
      }
      return false;
    }
  }
}
