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
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S1215",
  name = "Execution of the Garbage Collector should be triggered only by the JVM",
  tags = {"unpredictable"},
  priority = Priority.CRITICAL)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.INSTRUCTION_RELIABILITY)
@SqaleConstantRemediation("30min")
public class GarbageCollectorCalledCheck extends SubscriptionBaseVisitor {


  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree mit = (MethodInvocationTree) tree;
    if (mit.arguments().isEmpty() && mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mset = (MemberSelectExpressionTree) mit.methodSelect();
      if (isGarbageCollectorCall(mset)) {
        addIssue(tree, "Don't try to be smarter than the JVM, remove this call to run the garbage collector.");
      }
    }
  }

  private static boolean isGarbageCollectorCall(MemberSelectExpressionTree mset) {
    if ("gc".equals(mset.identifier().name())) {
      if (mset.expression().is(Tree.Kind.IDENTIFIER)) {
        //detect call to System.gc()
        return "System".equals(((IdentifierTree) mset.expression()).name());
      } else if (mset.expression().is(Tree.Kind.METHOD_INVOCATION)) {
        MethodInvocationTree mit = (MethodInvocationTree) mset.expression();
        if(mit.arguments().isEmpty() && mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
          MemberSelectExpressionTree subMset = (MemberSelectExpressionTree) mit.methodSelect();
          //detect call to Runtime.getRuntime().gc()
          return "getRuntime".equals(subMset.identifier().name())
              && subMset.expression().is(Tree.Kind.IDENTIFIER)
              && "Runtime".equals(((IdentifierTree) subMset.expression()).name());
        }
      }
    }
    return false;
  }

}
