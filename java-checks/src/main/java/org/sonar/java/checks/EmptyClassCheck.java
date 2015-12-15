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
import org.sonar.java.tag.Tag;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2094",
  name = "Classes should not be empty",
  priority = Priority.MAJOR,
  tags = {Tag.CLUMSY})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("5min")
public class EmptyClassCheck extends SubscriptionBaseVisitor {
  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (isEmptyClass((ClassTree) tree)) {
      addIssue(tree, "Remove this empty class, write its code or make it an \"interface\".");
    }
  }

  private static boolean isEmptyClass(ClassTree tree) {
    return tree.simpleName() != null && isNotExtending(tree) && isEmpty(tree);
  }

  private static boolean isNotExtending(ClassTree tree) {
    return tree.superClass() == null && tree.superInterfaces().isEmpty();
  }

  private static boolean isEmpty(ClassTree tree) {
    if(!tree.modifiers().annotations().isEmpty()) {
      return false;
    }
    for (Tree member : tree.members()) {
      if (!member.is(Tree.Kind.EMPTY_STATEMENT)) {
        return false;
      }
    }
    return true;
  }
}
