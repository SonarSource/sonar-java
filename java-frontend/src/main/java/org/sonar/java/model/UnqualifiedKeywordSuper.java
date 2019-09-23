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
package org.sonar.java.model;

import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.plugins.java.api.semantic.Type;

final class UnqualifiedKeywordSuper extends IdentifierTreeImpl {

  UnqualifiedKeywordSuper(InternalSyntaxToken token) {
    super(token);
  }

  Type resolveType() {
    return classNode().symbol().superClass();
  }

  @Override
  public Type symbolType() {
    if (root.useNewSema) {
      return resolveType();
    }
    return super.symbolType();
  }

  private ClassTreeImpl classNode() {
    JavaTree node = this;
    while (!node.is(Kind.CLASS, Kind.ENUM)) {
      node = (JavaTree) node.parent();
    }
    return (ClassTreeImpl) node;
  }

}
