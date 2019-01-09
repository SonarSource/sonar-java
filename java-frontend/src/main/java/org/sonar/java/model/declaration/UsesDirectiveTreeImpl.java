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
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.UsesDirectiveTree;

import java.util.Arrays;
import java.util.Collections;

public class UsesDirectiveTreeImpl extends ModuleDirectiveTreeImpl implements UsesDirectiveTree {

  private final TypeTree typeName;

  public UsesDirectiveTreeImpl(InternalSyntaxToken usesKeyword, TypeTree typeName, InternalSyntaxToken semicolonToken) {
    super(Tree.Kind.USES_DIRECTIVE, usesKeyword, semicolonToken);
    this.typeName = typeName;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitUsesDirective(this);
  }

  @Override
  public Kind kind() {
    return Tree.Kind.USES_DIRECTIVE;
  }

  @Override
  public TypeTree typeName() {
    return typeName;
  }

  @Override
  protected Iterable<Tree> children() {
    return Collections.unmodifiableList(Arrays.asList(
      directiveKeyword(),
      typeName,
      semicolonToken()));
  }

}
