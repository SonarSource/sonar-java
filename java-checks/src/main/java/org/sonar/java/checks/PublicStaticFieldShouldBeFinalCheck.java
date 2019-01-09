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
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifierKeywordTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S1444")
public class PublicStaticFieldShouldBeFinalCheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitClass(ClassTree tree) {
    if (tree.is(Tree.Kind.CLASS) || tree.is(Tree.Kind.ENUM)) {
      for (Tree member : tree.members()) {
        if (member.is(Tree.Kind.VARIABLE) && isPublicStaticNotFinal((VariableTree) member)) {
          context.reportIssue(this, ((VariableTree) member).simpleName(), "Make this \"public static " + ((VariableTree) member).simpleName() + "\" field final");
        }
      }
    }
    super.visitClass(tree);
  }

  private static boolean isPublicStaticNotFinal(VariableTree tree) {
    boolean isPublic = false;
    boolean isStatic = false;
    boolean isFinal = false;

    for (ModifierKeywordTree modifierKeywordTree : tree.modifiers().modifiers()) {
      Modifier modifier = modifierKeywordTree.modifier();
      if (modifier == Modifier.PUBLIC) {
        isPublic = true;
      } else if (modifier == Modifier.STATIC) {
        isStatic = true;
      } else if (modifier == Modifier.FINAL) {
        isFinal = true;
      }
    }
    return isPublic && isStatic && !isFinal;
  }
}
