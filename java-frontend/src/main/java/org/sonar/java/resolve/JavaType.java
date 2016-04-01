/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;

import javax.annotation.Nullable;

import java.util.List;

public class JavaType implements Type {

  public static final int BYTE = 1;
  public static final int CHAR = 2;
  public static final int SHORT = 3;
  public static final int INT = 4;
  public static final int LONG = 5;
  public static final int FLOAT = 6;
  public static final int DOUBLE = 7;
  public static final int BOOLEAN = 8;
  public static final int VOID = 9;
  public static final int CLASS = 10;
  public static final int ARRAY = 11;
  public static final int METHOD = 12;
  public static final int BOT = 13;
  public static final int UNKNOWN = 14;
  public static final int TYPEVAR = 15;
  public static final int WILDCARD = 16;

  int tag;

  JavaType primitiveType = null;

  JavaType primitiveWrapperType = null;

  /**
   * Symbol, which defines this type.
   */
  JavaSymbol.TypeJavaSymbol symbol;

  public JavaType(int tag, JavaSymbol.TypeJavaSymbol symbol) {
    this.tag = tag;
    this.symbol = symbol;
  }

  public boolean isTagged(int tag) {
    return tag == this.tag;
  }

  @Override
  public boolean isNumerical() {
    // JLS8 4.2
    return tag <= DOUBLE;
  }

  public JavaSymbol.TypeJavaSymbol getSymbol() {
    symbol.complete();
    return symbol;
  }

  @Override
  public boolean is(String fullyQualifiedName) {
    if (tag < CLASS) {
      // primitive type
      return fullyQualifiedName.equals(symbol.name);
    } else if (isTagged(ARRAY)) {
      return fullyQualifiedName.endsWith("[]") && ((ArrayJavaType) this).elementType.is(fullyQualifiedName.substring(0, fullyQualifiedName.length() - 2));
    } else if (isTagged(TYPEVAR)) {
      return false;
    }
    return false;
  }

  @Override
  public boolean isSubtypeOf(String fullyQualifiedName) {
    if (isTagged(ARRAY)) {
      return "java.lang.Object".equals(fullyQualifiedName) ||
          (fullyQualifiedName.endsWith("[]") && ((ArrayJavaType) this).elementType.isSubtypeOf(fullyQualifiedName.substring(0, fullyQualifiedName.length() - 2)));
    } else if (isTagged(TYPEVAR)) {
      return erasure().isSubtypeOf(fullyQualifiedName);
    }
    return false;
  }

  @Override
  public boolean isSubtypeOf(Type superType) {
    JavaType supType = (JavaType) superType;
    if (isTagged(ARRAY)) {
      //Handle covariance of arrays.
      if(supType.isTagged(ARRAY)) {
        return ((ArrayType) this).elementType().isSubtypeOf(((ArrayType) supType).elementType());
      }
      //Only possibility to be supertype of array without being an array is to be Object.
      return "java.lang.Object".equals(supType.fullyQualifiedName());
    }
    return false;
  }

  /**
   * JLS8 4.6
   */
  @Override
  public JavaType erasure() {
    return this;
  }

  @Override
  public boolean isPrimitive() {
    return tag <= BOOLEAN;
  }

  @Override
  public boolean isPrimitive(Primitives primitive) {
    return tag == primitive.ordinal() + 1;
  }

  @Override
  public boolean isUnknown() {
    return false;
  }

  public boolean isPrimitiveWrapper() {
    if (!isTagged(CLASS)) {
      return false;
    }
    return is("java.lang.Byte") ||
      is("java.lang.Character") ||
      is("java.lang.Short") ||
      is("java.lang.Integer") ||
      is("java.lang.Long") ||
      is("java.lang.Float") ||
      is("java.lang.Double") ||
      is("java.lang.Boolean");
  }

  @Override
  public String toString() {
    return symbol == null ? "" : symbol.toString();
  }

  @Nullable
  public JavaType primitiveType() {
    return primitiveType;
  }

  @Nullable
  public JavaType primitiveWrapperType() {
    return primitiveWrapperType;
  }

  @Override
  public boolean isArray() {
    return isTagged(ARRAY);
  }

  @Override
  public boolean isClass() {
    return isTagged(CLASS);
  }

  @Override
  public boolean isVoid() {
    return isTagged(VOID);
  }

  @Override
  public String fullyQualifiedName() {
    return symbol.getFullyQualifiedName();
  }

  @Override
  public String name() {
    return symbol.name;
  }

  @Override
  public Symbol.TypeSymbol symbol() {
    return getSymbol();
  }

  public static class ClassJavaType extends JavaType {

    /**
     * Supertype of this class.
     */
    JavaType supertype;

    /**
     * Interfaces of this class.
     */
    List<JavaType> interfaces;

    public ClassJavaType(JavaSymbol.TypeJavaSymbol symbol) {
      super(CLASS, symbol);
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
      if(isTagged(BOT)) {
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

  public static class ArrayJavaType extends JavaType implements ArrayType {

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

  public static class MethodJavaType extends JavaType {

    List<JavaType> argTypes;
    // Return type of constructor is null.
    @Nullable
    JavaType resultType;
    List<JavaType> thrown;

    public MethodJavaType(List<JavaType> argTypes, @Nullable JavaType resultType, List<JavaType> thrown, JavaSymbol.TypeJavaSymbol symbol) {
      super(METHOD, symbol);
      this.argTypes = argTypes;
      this.resultType = resultType;
      this.thrown = thrown;
    }

    @Override
    public String toString() {
      return resultType == null ? "constructor" : ("returns " + resultType.toString());
    }

    @Nullable
    public JavaType resultType() {
      return resultType;
    }
  }

  public static class TypeVariableJavaType extends JavaType {

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
    public boolean isSubtypeOf(Type superType) {
      JavaType supType = (JavaType) superType;
      if (supType.isTagged(WILDCARD)) {
        return ((WildCardType) supType).isSubtypeOfBound(this);
      }
      if(supType == this) {
        return true;
      }
      for (JavaType bound : bounds()) {
        if(bound.isSubtypeOf(supType)) {
          return true;
        }
      }
      return false;
    }
  }

  public static class ParametrizedTypeJavaType extends JavaType.ClassJavaType {

    final TypeSubstitution typeSubstitution;
    final JavaType rawType;

    ParametrizedTypeJavaType(JavaSymbol.TypeJavaSymbol symbol, TypeSubstitution typeSubstitution) {
      super(symbol);
      this.rawType = symbol.getType();
      this.typeSubstitution = typeSubstitution;
    }



    @Override
    public JavaType erasure() {
      return rawType.erasure();
    }

    @Nullable
    public JavaType substitution(TypeVariableJavaType typeVariableType) {
      JavaType result = null;
      if (typeSubstitution != null) {
        result = typeSubstitution.substitutedType(typeVariableType);
      }
      return result;
    }

    public List<TypeVariableJavaType> typeParameters() {
      if (typeSubstitution != null) {
        return typeSubstitution.typeVariables();
      }
      return Lists.newArrayList();
    }

    @Override
    public boolean isSubtypeOf(Type superType) {
      if(((JavaType) superType).isTagged(TYPEVAR)) {
        return false;
      }
      if (erasure().isSubtypeOf(superType.erasure())) {
        boolean superTypeIsParametrizedJavaType = superType instanceof ParametrizedTypeJavaType;
        if (superTypeIsParametrizedJavaType) {
          return checkSubstitutedTypesCompatibility((ParametrizedTypeJavaType) superType);
        }
        return !superTypeIsParametrizedJavaType;
      }
      return false;
    }

    private boolean checkSubstitutedTypesCompatibility(ParametrizedTypeJavaType superType) {
      List<JavaType> myTypes = typeSubstitution.substitutedTypes();
      List<JavaType> itsTypes = superType.typeSubstitution.substitutedTypes();
      if (itsTypes.size() != myTypes.size()) {
        return false;
      }
      for (int i = 0; i < myTypes.size(); i++) {
        JavaType myType = myTypes.get(i);
        JavaType itsType = itsTypes.get(i);
        if (itsType.isTagged(WILDCARD)) {
          if (!myType.isSubtypeOf(itsType)) {
            return false;
          }
        } else if (!myType.equals(itsType)) {
          return false;
        }
      }
      return true;
    }
  }

  public static class WildCardType extends JavaType {

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
          return !boundIsTypeVarAndNotType(type) && bound.isSubtypeOf(type);
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

  public static class UnknownType extends ClassJavaType {
    public UnknownType(JavaSymbol.TypeJavaSymbol symbol) {
      super(symbol);
      tag = UNKNOWN;
      supertype = null;
      interfaces = ImmutableList.of();
    }

    @Override
    public boolean isSubtypeOf(String fullyQualifiedName) {
      return false;
    }

    @Override
    public boolean isSubtypeOf(Type superType) {
      return false;
    }

    @Override
    public boolean isUnknown() {
      return true;
    }

    @Override
    public String toString() {
      return "!unknown!";
    }
  }
}
