/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifierKeywordTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Rule(key = "S2333")
public class RedundantModifierCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE, Tree.Kind.CLASS, Kind.ENUM);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    for (Tree member : classTree.members()) {
      switch (member.kind()) {
        case METHOD:
          checkMethod((MethodTree) member, classTree);
          break;
        case VARIABLE:
          checkVariable((VariableTree) member, classTree);
          break;
        case CONSTRUCTOR:
          if (tree.is(Kind.ENUM)) {
            checkRedundantModifier(((MethodTree) member).modifiers(), Modifier.PRIVATE);
          }
          break;
        case INTERFACE:
          ClassTree nestedClass = (ClassTree) member;
          checkNestedInterface(nestedClass, classTree);
          checkNestedClass(nestedClass, classTree);
          break;
        case CLASS:
          checkNestedClass((ClassTree) member, classTree);
          break;
        default:
          // Do nothing for others members
      }
    }
  }

  private void checkMethod(MethodTree methodTree, ClassTree classTree) {
    ModifiersTree modifiers = methodTree.modifiers();
    if (isInterfaceOrAnnotation(classTree)) {
      checkRedundantModifier(modifiers, Modifier.ABSTRACT, Modifier.PUBLIC);
    } else if (ModifiersUtils.hasModifier(classTree.modifiers(), Modifier.FINAL)) {
      checkRedundantModifier(modifiers, Modifier.FINAL);
    }
  }

  private void checkVariable(VariableTree variableTree, ClassTree classTree) {
    if (isInterfaceOrAnnotation(classTree)) {
      ModifiersTree modifiers = variableTree.modifiers();
      checkRedundantModifier(modifiers, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);
    }
  }

  private void checkNestedClass(ClassTree nested, ClassTree classTree) {
    if (isInterfaceOrAnnotation(classTree)) {
      ModifiersTree modifiers = nested.modifiers();
      checkRedundantModifier(modifiers, Modifier.PUBLIC, Modifier.STATIC);
    }
  }

  private void checkNestedInterface(ClassTree nested, ClassTree classTree) {
    if (classTree.is(Kind.CLASS, Kind.ENUM)) {
      checkRedundantModifier(nested.modifiers(), Modifier.STATIC);
    }
  }

  private static boolean isInterfaceOrAnnotation(Tree tree) {
    return tree.is(Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE);
  }

  private void checkRedundantModifier(ModifiersTree modifiersTree, Modifier... modifiers) {
    for (Modifier modifier : modifiers) {
      ModifierKeywordTree foundModifier = ModifiersUtils.getModifier(modifiersTree, modifier);
      if (foundModifier != null) {
        reportIssue(foundModifier, "\"" + modifier.toString().toLowerCase(Locale.US) + "\" is redundant in this context.");
      }
    }
  }

}
