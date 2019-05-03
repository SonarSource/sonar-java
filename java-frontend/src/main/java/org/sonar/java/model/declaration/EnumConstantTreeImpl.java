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

import com.google.common.collect.ImmutableList;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.expression.NewClassTreeImpl;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public class EnumConstantTreeImpl extends VariableTreeImpl implements EnumConstantTree {

  public EnumConstantTreeImpl(ModifiersTree modifiers, IdentifierTree simpleName, NewClassTreeImpl initializer,
    @Nullable InternalSyntaxToken separatorToken) {
    super(Kind.ENUM_CONSTANT, modifiers, simpleName, Objects.requireNonNull(initializer));
    if (separatorToken != null) {
      this.setEndToken(separatorToken);
    }
  }

  @Override
  @Nonnull
  public NewClassTree initializer() {
    return (NewClassTree) super.initializer();
  }

  @Override
  public Kind kind() {
    return Kind.ENUM_CONSTANT;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitEnumConstant(this);
  }

  @Override
  public Iterable<Tree> children() {
    ImmutableList.Builder<Tree> iteratorBuilder = ImmutableList.builder();
    // the identifierTree simpleName is also present in initializer
    iteratorBuilder.add(modifiers(), initializer());
    SyntaxToken endToken = endToken();
    if (endToken != null) {
      iteratorBuilder.add(endToken);
    }
    return iteratorBuilder.build();
  }

  @Nullable
  @Override
  public SyntaxToken separatorToken() {
    return endToken();
  }

}
