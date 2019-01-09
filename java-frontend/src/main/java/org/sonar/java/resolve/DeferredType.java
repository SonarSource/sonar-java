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
package org.sonar.java.resolve;

import org.sonar.java.model.AbstractTypedTree;

public class DeferredType extends JavaType {
  private final JavaType uninferedType;
  private AbstractTypedTree tree;

  public DeferredType(AbstractTypedTree tree) {
    super(JavaType.DEFERRED, Symbols.unknownSymbol);
    this.tree = tree;
    uninferedType = null;
  }

  public DeferredType(JavaType uninferedType) {
    super(JavaType.DEFERRED, Symbols.unknownSymbol);
    this.tree = null;
    this.uninferedType = uninferedType;
  }

  public AbstractTypedTree tree() {
    return tree;
  }

  public void setTree(AbstractTypedTree tree) {
    this.tree = tree;
  }

  @Override
  public String toString() {
    return "!Defered type!";
  }

  public JavaType getUninferedType() {
    if (uninferedType == null) {
      return this;
    }
    if (uninferedType.isTagged(JavaType.TYPEVAR)) {
      // produced by a parameterized method where we have not been able to infer return type, so fallback on its leftmost bound
      return uninferedType.erasure();
    }
    return uninferedType;
  }
}
