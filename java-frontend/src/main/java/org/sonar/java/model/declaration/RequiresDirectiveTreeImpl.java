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
package org.sonar.java.model.declaration;

import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.ModuleNameTree;
import org.sonar.plugins.java.api.tree.RequiresDirectiveTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import java.util.Arrays;
import java.util.Collections;

public class RequiresDirectiveTreeImpl extends ModuleDirectiveTreeImpl implements RequiresDirectiveTree {

  private final ModifiersTree modifiers;
  private final ModuleNameTree moduleName;

  public RequiresDirectiveTreeImpl(InternalSyntaxToken requiresKeyword, ModifiersTree modifiers, ModuleNameTree moduleName, InternalSyntaxToken semicolonToken) {
    super(Tree.Kind.REQUIRES_DIRECTIVE, requiresKeyword, semicolonToken);
    this.modifiers = modifiers;
    this.moduleName = moduleName;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitRequiresDirectiveTree(this);
  }

  @Override
  public Kind kind() {
    return Tree.Kind.REQUIRES_DIRECTIVE;
  }

  @Override
  public ModifiersTree modifiers() {
    return modifiers;
  }

  @Override
  public ModuleNameTree moduleName() {
    return moduleName;
  }

  @Override
  protected Iterable<Tree> children() {
    return Collections.unmodifiableList(Arrays.asList(
      directiveKeyword(),
      modifiers,
      moduleName,
      semicolonToken()));
  }

}
