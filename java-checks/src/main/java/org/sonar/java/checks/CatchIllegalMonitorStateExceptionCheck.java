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
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.UnionTypeTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2235",
  name = "IllegalMonitorStateException should never be caught",
  tags = {"bug", "multi-threading"},
  priority = Priority.CRITICAL)
@ActivatedByDefault
@SqaleSubCharacteristic(value = RulesDefinition.SubCharacteristics.ARCHITECTURE_RELIABILITY)
@SqaleConstantRemediation(value = "20min")
public class CatchIllegalMonitorStateExceptionCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CATCH);
  }

  @Override
  public void visitNode(Tree tree) {
    CatchTree catchTree = (CatchTree) tree;
    Tree parameterTypeTree = catchTree.parameter().type();
    if (parameterTypeTree.is(Kind.UNION_TYPE)) {
      UnionTypeTree unionTypeTree = (UnionTypeTree) parameterTypeTree;
      for (Tree exceptionTypeTree : unionTypeTree.typeAlternatives()) {
        checkExceptionType(exceptionTypeTree);
      }
    } else {
      checkExceptionType(parameterTypeTree);
    }
  }

  private void checkExceptionType(Tree exceptionTypeTree) {
    Type exceptionType = ((AbstractTypedTree) exceptionTypeTree).getSymbolType();
    if (exceptionType.is("java.lang.IllegalMonitorStateException")) {
      addIssue(exceptionTypeTree, "Refactor this piece of code to not catch IllegalMonitorStateException");
    }
  }

}
