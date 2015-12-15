/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.java.tag.Tag;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S1258",
  name = "Classes and enums with private members should have a constructor",
  priority = Priority.MAJOR,
  tags = {Tag.PITFALL})
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.INSTRUCTION_RELIABILITY)
@SqaleConstantRemediation("5min")
public class AtLeastOneConstructorCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.CLASS, Kind.ENUM);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree node = (ClassTree) tree;
    if (node.simpleName() != null && !ModifiersUtils.hasModifier(((ClassTree) tree).modifiers(), Modifier.ABSTRACT)) {
      List<Tree> members = node.members();
      boolean hasPrivateMember = false;
      for (Tree member : members) {
        if (member.is(Kind.CONSTRUCTOR)) {
          return;
        } else if (member.is(Kind.VARIABLE)) {
          VariableTree variable = (VariableTree) member;
          Symbol symbol = variable.symbol();
          hasPrivateMember |= variable.initializer() == null && symbol.isPrivate() && !symbol.isStatic();
        }
      }
      if (hasPrivateMember) {
        addIssue(tree, "Add a constructor to the " + node.declarationKeyword().text() + ".");
      }
    }
  }

}
