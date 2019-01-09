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

import org.sonar.plugins.java.api.semantic.Type;

import java.util.List;

public class TypeVariableJavaType extends JavaType {

  List<JavaType> bounds;

  public TypeVariableJavaType(JavaSymbol.TypeVariableJavaSymbol symbol) {
    super(TYPEVAR, symbol);
  }

  /**
   * Erasure of a type variable is the erasure of its leftmost bound.
   */
  @Override
  public JavaType erasure() {
    return bounds.get(0).erasure();
  }

  public List<JavaType> bounds() {
    return bounds;
  }

  @Override
  public boolean isSubtypeOf(String fullyQualifiedName) {
    return erasure().isSubtypeOf(fullyQualifiedName);
  }

  @Override
  public boolean isSubtypeOf(Type superType) {
    JavaType supType = (JavaType) superType;
    if (supType.isTagged(WILDCARD)) {
      return ((WildCardType) supType).isSubtypeOfBound(this);
    }
    if (supType == this) {
      return true;
    }
    for (JavaType bound : bounds()) {
      if (bound.isSubtypeOf(supType) || (supType.isParameterized() && bound == supType.erasure())) {
        return true;
      }
    }
    return false;
  }
}
