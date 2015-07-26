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
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2333",
  name = "Redundant modifiers should not be used",
  tags = {"clumsy"},
  priority = Priority.MINOR)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("2min")
public class RedundantModifierCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE, Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    for (Tree member : classTree.members()) {
      if (member.is(Tree.Kind.METHOD)) {
        MethodTree methodTree = (MethodTree) member;
        ModifiersTree modifiers = methodTree.modifiers();
        if (isInterfaceOrAnnotation(tree)) {
          checkRedundantModifier(modifiers, Modifier.ABSTRACT);
          checkRedundantModifier(modifiers, Modifier.PUBLIC);
        } else if (ModifiersUtils.hasModifier(classTree.modifiers(), Modifier.FINAL)) {
          checkRedundantModifier(modifiers, Modifier.FINAL);
        }
      } else if (member.is(Tree.Kind.VARIABLE) && isInterfaceOrAnnotation(tree)) {
        VariableTree variableTree = (VariableTree) member;
        ModifiersTree modifiers = variableTree.modifiers();
        checkRedundantModifier(modifiers, Modifier.PUBLIC);
        checkRedundantModifier(modifiers, Modifier.STATIC);
        checkRedundantModifier(modifiers, Modifier.FINAL);
      }
    }
  }

  private static boolean isInterfaceOrAnnotation(Tree tree) {
    return tree.is(Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE);
  }

  private void checkRedundantModifier(ModifiersTree modifiersTree, Modifier modifier) {
    if (ModifiersUtils.hasModifier(modifiersTree, modifier)) {
      addIssue(modifiersTree, "\"" + modifier.toString().toLowerCase() + "\" is redundant in this context.");
    }
  }

}
