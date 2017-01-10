/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

public class ClassJavaType extends JavaType {

  /**
   * Supertype of this class.
   */
  JavaType supertype;

  /**
   * Interfaces of this class.
   */
  List<JavaType> interfaces;

  public ClassJavaType(JavaSymbol.TypeJavaSymbol symbol) {
    this(CLASS, symbol);
  }

  protected ClassJavaType(int tag, JavaSymbol.TypeJavaSymbol symbol) {
    super(tag, symbol);
  }

  @Override
  public boolean is(String fullyQualifiedName) {
    return isTagged(BOT) || fullyQualifiedName.equals(symbol.getFullyQualifiedName());
  }

  @Override
  public boolean isSubtypeOf(String fullyQualifiedName) {
    return isTagged(BOT) || is(fullyQualifiedName) || superTypeContains(fullyQualifiedName);
  }

  @Override
  public boolean isSubtypeOf(Type superType) {
    if (isTagged(BOT)) {
      return ((JavaType) superType).isTagged(BOT) || superType.isClass() || superType.isArray();
    }
    if (((JavaType) superType).isTagged(JavaType.WILDCARD)) {
      return ((WildCardType) superType).isSubtypeOfBound(this);
    }
    if (superType.isClass()) {
      ClassJavaType superClassType = (ClassJavaType) superType;
      return this.equals(superClassType) || superTypeIsSubTypeOf(superClassType);
    }
    return false;
  }

  private boolean superTypeIsSubTypeOf(ClassJavaType superClassType) {
    for (ClassJavaType classType : symbol.superTypes()) {
      if (classType.isSubtypeOf(superClassType)) {
        return true;
      }
    }
    return false;
  }

  private boolean superTypeContains(String fullyQualifiedName) {
    for (ClassJavaType classType : symbol.superTypes()) {
      if (classType.is(fullyQualifiedName)) {
        return true;
      }
    }
    return false;
  }
}
