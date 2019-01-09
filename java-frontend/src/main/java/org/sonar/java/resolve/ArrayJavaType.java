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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.sonar.plugins.java.api.semantic.Type;

public class ArrayJavaType extends JavaType implements Type.ArrayType {

  private final ArrayJavaType erasure;
  /**
   * Type of elements of this array.
   */
  JavaType elementType;

  /**
   * @param arrayClass {@link Symbols#arrayClass}
   */
  public ArrayJavaType(JavaType elementType, JavaSymbol.TypeJavaSymbol arrayClass) {
    super(ARRAY, arrayClass);
    this.elementType = elementType;
    // element
    this.erasure = new ArrayJavaType(arrayClass);
  }

  private ArrayJavaType(JavaSymbol.TypeJavaSymbol arrayClass) {
    super(ARRAY, arrayClass);
    this.erasure = this;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(31, 37).append(elementType).hashCode();
  }

  @Override
  public boolean is(String fullyQualifiedName) {
    return fullyQualifiedName.endsWith("[]") && elementType.is(fullyQualifiedName.substring(0, fullyQualifiedName.length() - 2));
  }

  @Override
  public boolean isSubtypeOf(String fullyQualifiedName) {
    return "java.lang.Object".equals(fullyQualifiedName)
      || (fullyQualifiedName.endsWith("[]") && elementType.isSubtypeOf(fullyQualifiedName.substring(0, fullyQualifiedName.length() - 2)));
  }

  @Override
  public boolean isSubtypeOf(Type superType) {
    JavaType supType = (JavaType) superType;
    // Handle covariance of arrays.
    if (supType.isTagged(ARRAY)) {
      return elementType.isSubtypeOf(((ArrayType) supType).elementType());
    }
    if (supType.isTagged(WILDCARD)) {
      return ((WildCardType) superType).isSubtypeOfBound(this);
    }
    // Only possibility to be supertype of array without being an array is to be Object.
    return "java.lang.Object".equals(supType.fullyQualifiedName());
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof ArrayJavaType)) {
      return false;
    }
    ArrayJavaType rhs = (ArrayJavaType) obj;
    return new EqualsBuilder()
      .append(elementType, rhs.elementType)
      .isEquals();
  }

  @Override
  public String toString() {
    return elementType.toString() + "[]";
  }

  @Override
  public JavaType elementType() {
    return elementType;
  }

  @Override
  public JavaType erasure() {
    if (erasure.elementType == null) {
      erasure.elementType = elementType.erasure();
    }
    return erasure;
  }
}
