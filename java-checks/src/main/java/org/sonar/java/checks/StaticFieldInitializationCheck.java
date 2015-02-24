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
import org.sonar.java.checks.methods.MethodInvocationMatcher;
import org.sonar.java.resolve.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2444",
  name = "Lazy initialization of \"static\" fields should be \"synchronized\"",
  tags = {"bug", "multi-threading"},
  priority = Priority.CRITICAL)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.SYNCHRONIZATION_RELIABILITY)
@SqaleConstantRemediation("30min")
public class StaticFieldInitializationCheck extends AbstractInSynchronizeChecker {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.ASSIGNMENT, Tree.Kind.METHOD, Tree.Kind.METHOD_INVOCATION, Tree.Kind.SYNCHRONIZED_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic() && tree.is(Tree.Kind.ASSIGNMENT)) {
      AssignmentExpressionTree aet = (AssignmentExpressionTree) tree;
      if (aet.variable().is(Tree.Kind.IDENTIFIER) && !isInSyncBlock()) {
        IdentifierTree variable = (IdentifierTree) aet.variable();
        if (isStaticNotVolatileObject(variable)) {
          addIssue(variable, "Synchronize this lazy initialization of '" + variable.name() + "'");
        }
      }
    }
    super.visitNode(tree);
  }

  private boolean isStaticNotVolatileObject(IdentifierTree variable) {
    Symbol symbol = getSemanticModel().getReference(variable);
    if( symbol != null ) {
      return isStaticNotFinalNotVolatile(symbol) && !symbol.getType().isPrimitive();
    }
    return false;
  }

  private boolean isStaticNotFinalNotVolatile(Symbol symbol) {
    return symbol.isStatic() && !symbol.isVolatile() && !symbol.isFinal();
  }

  @Override
  protected List<MethodInvocationMatcher> getMethodInvocationMatchers() {
    return ImmutableList.of();
  }

}
