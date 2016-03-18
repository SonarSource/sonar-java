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
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifierKeywordTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S3066",
  name = "\"enum\" fields should not be publicly mutable",
  priority = Priority.CRITICAL,
  tags = {Tag.BAD_PRACTICE, Tag.SECURITY})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.API_ABUSE)
@SqaleConstantRemediation("20min")
public class EnumMutableFieldCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.ENUM);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree enumTree = (ClassTree) tree;
    for (Tree member : enumTree.members()) {
      if (member.is(Tree.Kind.VARIABLE)) {
        ModifiersTree modifiers = ((VariableTree) member).modifiers();
        ModifierKeywordTree publicModifier = ModifiersUtils.getModifier(modifiers, Modifier.PUBLIC);
        if (publicModifier != null && !ModifiersUtils.hasModifier(modifiers, Modifier.STATIC)
        // FIXME SONARJAVA-1604 final mutable field should raise issues
          && !ModifiersUtils.hasModifier(modifiers, Modifier.FINAL)) {
          reportIssue(publicModifier, "Lower the visibility of this field.");
        }
      } else if (member.is(Tree.Kind.METHOD)) {
        MethodTree methodTree = (MethodTree) member;
        ModifierKeywordTree publicModifier = ModifiersUtils.getModifier(methodTree.modifiers(), Modifier.PUBLIC);
        if (publicModifier != null && isSetter(methodTree)) {
          reportIssue(publicModifier, "Lower the visibility of this setter or remove it altogether.");
        }
      }
    }
  }

  private static boolean isSetter(MethodTree methodTree) {
    TypeTree returnType = methodTree.returnType();
    BlockTree block = methodTree.block();
    boolean returnsVoid = returnType.is(Tree.Kind.PRIMITIVE_TYPE) && "void".equals(((PrimitiveTypeTree) returnType).keyword().text());
    boolean hasAtLeastOneStatement = block == null || !block.body().isEmpty();
    return methodTree.simpleName().name().startsWith("set") && methodTree.parameters().size() == 1 && returnsVoid && hasAtLeastOneStatement;
  }
}
