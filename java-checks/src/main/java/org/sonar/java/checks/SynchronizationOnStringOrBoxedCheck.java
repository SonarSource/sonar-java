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
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S1860",
  name = "Synchronisation should not be based on Strings or boxed primitives",
  tags = {"bug", "cert"},
  priority = Priority.BLOCKER)
@BelongsToProfile(title = "Sonar way", priority = Priority.BLOCKER)
@SqaleSubCharacteristic(value = RulesDefinition.SubCharacteristics.SYNCHRONIZATION_RELIABILITY)
@SqaleConstantRemediation(value = "15min")
public class SynchronizationOnStringOrBoxedCheck extends SubscriptionBaseVisitor {

  private static final List<String> FORBIDDEN_TYPES = ImmutableList.of(
    Boolean.class.getName(),
    Byte.class.getName(),
    Character.class.getName(),
    Double.class.getName(),
    Float.class.getName(),
    Integer.class.getName(),
    Long.class.getName(),
    Short.class.getName(),
    String.class.getName());

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.SYNCHRONIZED_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    SynchronizedStatementTree syncStatement = (SynchronizedStatementTree) tree;
    AbstractTypedTree expression = (AbstractTypedTree) syncStatement.expression();
    Type expressionType = expression.getSymbolType();
    if (expressionType.isPrimitive() || isForbiddenType(expressionType)) {
      addIssue(syncStatement, "Synchronize on a new \"Object\" instead.");
    }
  }

  private boolean isForbiddenType(Type expressionType) {
    for (String forbiddenType : FORBIDDEN_TYPES) {
      if (expressionType.is(forbiddenType)) {
        return true;
      }
    }
    return false;
  }

}
