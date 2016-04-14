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
import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.List;

@Rule(key = "S1873")
public class StaticFinalArrayNotPrivateCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.VARIABLE);
  }

  @Override
  public void visitNode(Tree tree) {
    VariableTree variableTree = (VariableTree) tree;
    if (variableTree.type().is(Kind.ARRAY_TYPE) && isStaticFinalNotPrivate(variableTree) && !isExcluded(variableTree)) {
      reportIssue(variableTree.simpleName(), "Make this array \"private\".");
    }
  }

  private static boolean isStaticFinalNotPrivate(VariableTree variableTree) {
    return isStatic(variableTree) && isFinal(variableTree) && !isPrivate(variableTree);
  }

  private static boolean isStatic(VariableTree variableTree) {
    return hasModifier(variableTree, Modifier.STATIC);
  }

  private static boolean isFinal(VariableTree variableTree) {
    return hasModifier(variableTree, Modifier.FINAL);
  }

  private static boolean isPrivate(VariableTree variableTree) {
    return hasModifier(variableTree, Modifier.PRIVATE);
  }

  private static boolean hasModifier(VariableTree variableTree, Modifier modifier) {
    return ModifiersUtils.hasModifier(variableTree.modifiers(), modifier);
  }
  
  private static boolean isExcluded(VariableTree variableTree) {
    return hasVisibleForTestingAnnotation(variableTree.modifiers().annotations());
  }

  private static boolean hasVisibleForTestingAnnotation(Iterable<AnnotationTree> annotations) {
    for (AnnotationTree annotationTree : annotations) {
      if (hasVisibleForTestingAnnotation(annotationTree)) {
        return true;
      }
    }
    return false;
  }

  public static boolean hasVisibleForTestingAnnotation(AnnotationTree tree) {
    String id = null;
    if (tree.annotationType().is(Kind.IDENTIFIER)) {
      id = ((IdentifierTree) tree.annotationType()).name();
    } else if (tree.annotationType().is(Kind.MEMBER_SELECT)) {
      id = ((MemberSelectExpressionTree) tree.annotationType()).identifier().name();
    }
    return "VisibleForTesting".equals(id);
  }

}
