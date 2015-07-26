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
import org.apache.commons.lang.BooleanUtils;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2156",
  name = "\"final\" classes should not have \"protected\" members",
  tags = {"confusing"},
  priority = Priority.MAJOR)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("5min")
public class ProtectedMemberInFinalClassCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (ModifiersUtils.hasModifier(classTree.modifiers(), Modifier.FINAL)) {
      for (Tree member : classTree.members()) {
        checkMember(member);
      }
    }
  }

  private void checkMember(Tree member) {
    if (member.is(Kind.VARIABLE)) {
      VariableTree variableTree = (VariableTree) member;
      checkMemberModifier(variableTree.modifiers(), variableTree);
    } else if (member.is(Kind.METHOD)) {
      MethodTreeImpl methodTree = (MethodTreeImpl) member;
      if (BooleanUtils.isFalse(methodTree.isOverriding())) {
        checkMemberModifier(methodTree.modifiers(), methodTree.simpleName());
      }
    }
  }

  private void checkMemberModifier(ModifiersTree modifiers, Tree reportingTree) {
    if (ModifiersUtils.hasModifier(modifiers, Modifier.PROTECTED)) {
      addIssue(reportingTree, "Remove this \"protected\" modifier.");
    }
  }

}
