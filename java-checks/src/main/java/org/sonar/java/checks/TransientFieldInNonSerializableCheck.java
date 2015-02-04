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
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.resolve.Symbol.TypeSymbol;
import org.sonar.java.resolve.Type;
import org.sonar.java.resolve.Type.ClassType;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2065",
  name = "Fields in non-serializable classes should not be \"transient\"",
  tags = {"unused", "serialization"},
  priority = Priority.MINOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("2min")
public class TransientFieldInNonSerializableCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTreeImpl classTree = (ClassTreeImpl) tree;
    if (hasSemantic() && isNotSerializable(classTree.getSymbol())) {
      for (Tree member : classTree.members()) {
        if (isTransient(member)) {
          addIssue(member, "Remove the \"transient\" modifier from this field.");
        }
      }
    }
  }

  private boolean isNotSerializable(TypeSymbol symbol) {
    for (ClassType superType : symbol.superTypes()) {
      if (superType.isTagged(Type.UNKNOWN)) {
        return false;
      }
    }
    return !symbol.getType().isSubtypeOf("java.io.Serializable");
  }

  private boolean isTransient(Tree tree) {
    if (tree.is(Tree.Kind.VARIABLE)) {
      VariableTree variable = (VariableTree) tree;
      return variable.modifiers().modifiers().contains(Modifier.TRANSIENT);
    }
    return false;
  }

}
