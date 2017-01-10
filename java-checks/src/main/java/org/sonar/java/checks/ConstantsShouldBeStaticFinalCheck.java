/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import org.sonar.check.Rule;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Arrays;
import java.util.List;

@Rule(key = "S1170")
public class ConstantsShouldBeStaticFinalCheck extends IssuableSubscriptionVisitor {

  private int nestedClassesLevel;


  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS);
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    nestedClassesLevel = 0;
    super.scanFile(context);
  }

  @Override
  public void visitNode(Tree tree) {
    nestedClassesLevel++;
    for (Tree member : ((ClassTree) tree).members()) {
      if (member.is(Tree.Kind.VARIABLE)) {
        VariableTree variableTree = (VariableTree) member;
        if (staticNonFinal(variableTree) && hasConstantInitializer(variableTree) && !isObjectInInnerClass(variableTree)) {
          reportIssue(variableTree.simpleName(), "Make this final field static too.");
        }
      }
    }
  }

  private boolean isObjectInInnerClass(VariableTree variableTree) {
    if (nestedClassesLevel > 1) {
      if (variableTree.type().is(Tree.Kind.IDENTIFIER)) {
        return !"String".equals(((IdentifierTree) variableTree.type()).name());
      } else {
        return !variableTree.type().is(Tree.Kind.PRIMITIVE_TYPE);
      }
    }
    return false;
  }

  private static boolean staticNonFinal(VariableTree variableTree) {
    return isFinal(variableTree) && !isStatic(variableTree);
  }

  @Override
  public void leaveNode(Tree tree) {
    nestedClassesLevel--;
  }

  private static boolean hasConstantInitializer(VariableTree variableTree) {
    Tree init = variableTree.initializer();
    if (init != null) {
      if (init.is(Tree.Kind.NEW_ARRAY)) {
        //exclude allocations : new int[6] but allow initialization new int[]{1,2};
        NewArrayTree newArrayTree = (NewArrayTree) init;
        return newArrayTree.dimensions().isEmpty() || newArrayTree.openBraceToken() != null;
      }
      return !containsChildrenOfKind((JavaTree) init, Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS);
    }
    return false;
  }

  private static boolean containsChildrenOfKind(JavaTree tree, Tree.Kind... kinds) {
    if (Arrays.asList(kinds).contains(tree.kind())) {
      return true;
    }
    if (!tree.isLeaf()) {
      for (Tree javaTree : tree.getChildren()) {
        if (javaTree != null && containsChildrenOfKind((JavaTree) javaTree, kinds)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isFinal(VariableTree variableTree) {
    return ModifiersUtils.hasModifier(variableTree.modifiers(), Modifier.FINAL);
  }

  private static boolean isStatic(VariableTree variableTree) {
    return ModifiersUtils.hasModifier(variableTree.modifiers(), Modifier.STATIC);
  }
}
