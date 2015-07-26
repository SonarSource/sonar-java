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
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S1610",
  name = "Abstract classes without fields should be converted to interfaces",
  tags = {"java8"},
  priority = Priority.MAJOR)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.ARCHITECTURE_CHANGEABILITY)
@SqaleConstantRemediation("10min")
public class AbstractClassNoFieldShouldBeInterfaceCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (classIsAbstract(classTree) && classHasNoField(classTree) && !classHasProtectedMethod(classTree)) {
      addIssue(classTree, "Convert the abstract class \"" + classTree.simpleName().name() + "\" into an interface");
    }
  }

  private static boolean classHasProtectedMethod(ClassTree tree) {
    for (Tree member : tree.members()) {
      if (member.is(Tree.Kind.METHOD) && ModifiersUtils.hasModifier(((MethodTree) member).modifiers(), Modifier.PROTECTED)) {
        return true;
      }
    }
    return false;
  }

  private static boolean classIsAbstract(ClassTree tree) {
    return ModifiersUtils.hasModifier(tree.modifiers(), Modifier.ABSTRACT);
  }

  private static boolean classHasNoField(ClassTree tree) {
    for (Tree member : tree.members()) {
      if (member.is(Tree.Kind.VARIABLE)) {
        return false;
      }
    }
    return true;
  }
}
