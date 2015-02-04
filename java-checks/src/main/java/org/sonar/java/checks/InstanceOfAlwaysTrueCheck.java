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
import org.sonar.plugins.java.api.tree.InstanceOfTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S1850",
  name = "\"instanceof\" operators that always return \"true\" should be removed",
  tags = {"bug", "cwe"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.ARCHITECTURE_RELIABILITY)
@SqaleConstantRemediation("5min")
public class InstanceOfAlwaysTrueCheck extends SubscriptionBaseVisitor {
  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.INSTANCE_OF);
  }

  @Override
  public void visitNode(Tree tree) {
    InstanceOfTree instanceOfTree = (InstanceOfTree) tree;
    Type type = ((AbstractTypedTree) instanceOfTree.expression()).getSymbolType();
    Type instanceOf = ((AbstractTypedTree) instanceOfTree.type()).getSymbolType();
    if (typeInherits(type, instanceOf)) {
      addIssue(tree, "Remove this useless \"instanceof\" operator; it will always return \"true\". ");
    }
  }


  private boolean typeInherits(Type type, Type instanceOf) {
    if (type.isTagged(Type.ARRAY) && instanceOf.isTagged(Type.ARRAY)) {
      //Handle covariance of arrays.
      return typeInherits(((Type.ArrayType) type).elementType(), ((Type.ArrayType) instanceOf).elementType());
    } else if (type.isTagged(Type.CLASS) && instanceOf.isTagged(Type.CLASS)) {
      Type.ClassType expressionType = (Type.ClassType) type;
      Type.ClassType instanceOfType = (Type.ClassType) instanceOf;
      return expressionType == instanceOfType || expressionType.getSymbol().superTypes().contains(instanceOfType);
    }
    return false;
  }

}
