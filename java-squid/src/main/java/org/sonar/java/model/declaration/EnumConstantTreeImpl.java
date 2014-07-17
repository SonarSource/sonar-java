/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.model.declaration;

import com.google.common.base.Preconditions;
import com.sonar.sslr.api.AstNode;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

public class EnumConstantTreeImpl extends VariableTreeImpl implements EnumConstantTree {
  public EnumConstantTreeImpl(AstNode astNode, ModifiersTree modifiers, Tree type, IdentifierTree simpleName, ExpressionTree initializer) {
    super(astNode, modifiers, type, simpleName, Preconditions.checkNotNull(initializer));
  }

  @Override
  public Kind getKind() {
    return Kind.ENUM_CONSTANT;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitEnumConstant(this);
  }
}
