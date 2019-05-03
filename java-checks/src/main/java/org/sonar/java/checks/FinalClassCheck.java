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
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.TypeTree;

import javax.annotation.Nullable;

import java.util.Collections;
import java.util.List;

@Rule(key = "S2974")
public class FinalClassCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    ClassTree classTree = (ClassTree) tree;
    if (hasOnlyPrivateConstructors(classTree) && !isExtended(classTree) && !ModifiersUtils.hasModifier(classTree.modifiers(), Modifier.FINAL)) {
      reportIssue(classTree.simpleName(), "Make this class \"final\" or add a public constructor.");
    }
  }

  private static boolean isExtended(ClassTree classTree) {
    IsExtendedVisitor isExtendedVisitor = new IsExtendedVisitor(classTree.symbol().type().erasure());
    classTree.accept(isExtendedVisitor);
    return isExtendedVisitor.isExtended;
  }

  private static boolean hasOnlyPrivateConstructors(ClassTree classTree) {
    boolean hasConstructor = false;
    for (Tree member : classTree.members()) {
      if (member.is(Kind.CONSTRUCTOR)) {
        hasConstructor = true;
        if (!ModifiersUtils.hasModifier(((MethodTree) member).modifiers(), Modifier.PRIVATE)) {
          // has a constructor not private.
          return false;
        }
      }
    }
    return hasConstructor;
  }

  private static class IsExtendedVisitor extends BaseTreeVisitor {
    private final Type type;
    boolean isExtended;

    IsExtendedVisitor(Type type) {
      this.type = type;
    }

    @Override
    protected void scan(@Nullable Tree tree) {
      if (!isExtended) {
        super.scan(tree);
      }
    }

    @Override
    public void visitClass(ClassTree tree) {
      TypeTree superClass = tree.superClass();
      if (superClass != null && superClass.symbolType().erasure() == type) {
        isExtended = true;
      }
      super.visitClass(tree);
    }

    @Override
    public void visitNewClass(NewClassTree tree) {
      if (tree.classBody() != null && tree.symbolType().isSubtypeOf(type)) {
        isExtended = true;
      }
      super.visitNewClass(tree);
    }
  }
}
