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
import org.sonar.java.RspecKey;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

@Rule(key = "ClassVariableVisibilityCheck")
@RspecKey("S1104")
public class ClassVariableVisibilityCheck extends BaseTreeVisitor implements JavaFileScanner {

  private Deque<Boolean> isClassStack = new ArrayDeque<>();

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitClass(ClassTree tree) {
    isClassStack.push(tree.is(Tree.Kind.CLASS) || tree.is(Tree.Kind.ENUM));
    super.visitClass(tree);
    isClassStack.pop();
  }

  @Override
  public void visitVariable(VariableTree tree) {
    ModifiersTree modifiers = tree.modifiers();
    List<AnnotationTree> annotations = modifiers.annotations();

    if (isClass() && isPublic(modifiers) && !(isFinal(modifiers) || !annotations.isEmpty())) {
      context.reportIssue(this, tree.simpleName(), "Make " + tree.simpleName() + " a static final constant or non-public and provide accessors if needed.");
    }
    super.visitVariable(tree);
  }

  private boolean isClass() {
    return !isClassStack.isEmpty() && isClassStack.peek();
  }

  private static boolean isFinal(ModifiersTree modifiers) {
    return ModifiersUtils.hasModifier(modifiers, Modifier.FINAL);
  }

  private static boolean isPublic(ModifiersTree modifiers) {
    return ModifiersUtils.hasModifier(modifiers, Modifier.PUBLIC);
  }

}
