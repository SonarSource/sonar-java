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
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2675",
  name = "\"readObject\" should not be \"synchronized\"",
  tags = {"confusing"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("2min")
public class ReadObjectSynchronizedCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      ClassTree classTree = (ClassTree) tree;
      if (implementsSerializable(classTree)) {
        for (Tree member : classTree.members()) {
          if (member.is(Tree.Kind.METHOD) && isSynchronized((MethodTree) member) && isReadObject((MethodTree) member)) {
            addIssue(member, "Remove the \"synchronized\" keyword from this method.");
          }
        }
      }
    }
  }

  private static boolean implementsSerializable(ClassTree classTree) {
    return classTree.symbol().type().isSubtypeOf("java.io.Serializable");
  }

  private static boolean isSynchronized(MethodTree methodTree) {
    return ModifiersUtils.hasModifier(methodTree.modifiers(), Modifier.SYNCHRONIZED);
  }

  private static boolean isReadObject(MethodTree methodTree) {
    if (!"readObject".equals(methodTree.simpleName().name())) {
      return false;
    }
    if (methodTree.parameters().size() != 1 || !methodTree.parameters().get(0).type().symbolType().is("java.io.ObjectInputStream")) {
      return false;
    }
    return true;
  }

}
