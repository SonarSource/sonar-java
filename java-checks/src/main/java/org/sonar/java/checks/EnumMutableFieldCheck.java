/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
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

import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
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

import java.util.Collections;
import java.util.List;

@Rule(key = "S3066")
public class EnumMutableFieldCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.ENUM);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree enumTree = (ClassTree) tree;
    for (Tree member : enumTree.members()) {
      if (member.is(Tree.Kind.VARIABLE)) {
        VariableTree variableTree = (VariableTree) member;
        ModifiersTree modifiers = variableTree.modifiers();
        ModifierKeywordTree publicModifier = ModifiersUtils.getModifier(modifiers, Modifier.PUBLIC);
        if (publicModifier != null && (isNotStaticOrFinal(variableTree.modifiers())|| isMutableFinalMember(variableTree))) {
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
  
  private static boolean isNotStaticOrFinal(ModifiersTree modifiersTree) {
    return !ModifiersUtils.hasModifier(modifiersTree, Modifier.STATIC)
      && !ModifiersUtils.hasModifier(modifiersTree, Modifier.FINAL);
  }
  
  private static boolean isMutableFinalMember(VariableTree variableTree) {
    ModifiersTree modifiersTree = variableTree.modifiers();
    return !ModifiersUtils.hasModifier(modifiersTree, Modifier.STATIC) && ModifiersUtils.hasModifier(modifiersTree, Modifier.FINAL) && isMutableMember(variableTree);
  }
  
  private static boolean isMutableMember(VariableTree variableTree) {
    return variableTree.type().is(Tree.Kind.ARRAY_TYPE) || isDateOrCollection(variableTree);
  }
  
  private static boolean isDateOrCollection(VariableTree variableTree) {
    Type type = variableTree.symbol().type();
    return type.is("java.util.Date") ||
      (type.isSubtypeOf("java.util.Collection") && !type.isSubtypeOf("com.google.common.collect.ImmutableCollection"));
  }

  private static boolean isSetter(MethodTree methodTree) {
    TypeTree returnType = methodTree.returnType();
    BlockTree block = methodTree.block();
    boolean returnsVoid = returnType.is(Tree.Kind.PRIMITIVE_TYPE) && "void".equals(((PrimitiveTypeTree) returnType).keyword().text());
    boolean hasAtLeastOneStatement = block == null || !block.body().isEmpty();
    return methodTree.simpleName().name().startsWith("set") && methodTree.parameters().size() == 1 && returnsVoid && hasAtLeastOneStatement;
  }
}
