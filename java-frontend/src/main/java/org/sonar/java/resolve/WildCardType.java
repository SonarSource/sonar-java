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

public class WildCardType extends JavaType {

  public enum BoundType {
    UNBOUNDED("?"),
    SUPER("? super "),
    EXTENDS("? extends ");

    private final String name;

    BoundType(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  final JavaType bound;
  final BoundType boundType;

  public WildCardType(JavaType bound, BoundType boundType) {
    super(WILDCARD, new JavaSymbol.WildcardSymbol(boundType == BoundType.UNBOUNDED ? boundType.toString() : (boundType + bound.symbol.name())));
    this.bound = bound;
    this.boundType = boundType;
    this.symbol.type = this;
  }

  @Override
  public boolean isSubtypeOf(String fullyQualifiedName) {
    return "java.lang.Object".equals(fullyQualifiedName) || (boundType == BoundType.EXTENDS && bound.isSubtypeOf(fullyQualifiedName));
  }

  @Override
  public boolean isSubtypeOf(Type superType) {
    if (((JavaType) superType).isTagged(WILDCARD)) {
      WildCardType superTypeWildcard = (WildCardType) superType;
      JavaType superTypeBound = superTypeWildcard.bound;
      switch (superTypeWildcard.boundType) {
        case UNBOUNDED:
          return true;
        case SUPER:
          return boundType == BoundType.SUPER && superTypeBound.isSubtypeOf(bound);
        case EXTENDS:
          return boundType != BoundType.SUPER && bound.isSubtypeOf(superTypeBound);
      }
    }
    return "java.lang.Object".equals(superType.fullyQualifiedName()) || (boundType == BoundType.EXTENDS && bound.isSubtypeOf(superType));
  }

  public boolean isSubtypeOfBound(JavaType type) {
    switch (boundType) {
      case SUPER:
        return bound.isSubtypeOf(type);
      case EXTENDS:
        return !boundIsTypeVarAndNotType(type) && type.isSubtypeOf(bound);
      case UNBOUNDED:
      default:
        return true;
    }
  }

  private boolean boundIsTypeVarAndNotType(JavaType type) {
    return bound.isTagged(TYPEVAR) && !type.isTagged(TYPEVAR);
  }
}
