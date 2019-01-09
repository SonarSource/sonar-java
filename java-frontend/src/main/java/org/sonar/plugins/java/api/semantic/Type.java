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
package org.sonar.plugins.java.api.semantic;

/**
 * Interface to access resolved type of an expression or a Type.
 */
public interface Type {

  /**
   * Primitive java types.
   */
  enum Primitives {
    BYTE,
    CHAR,
    SHORT,
    INT,
    LONG,
    FLOAT,
    DOUBLE,
    BOOLEAN
  }

  /**
   * Check whether a type is the one designed by the fully qualified name.
   *
   * <code><pre>
   *   Type type;
   *   type.is("int");
   *   type.is("int[]");
   *   type.is("java.lang.String[]");
   *   type.is("java.lang.Object");
   *</pre></code>
   *
   * @param fullyQualifiedName fully qualified name to check. Use "[]" for arrays and the simple name for primitive type ("int", "byte"...).
   * @return true if the type is the one looked for. false otherwise.
   */
  boolean is(String fullyQualifiedName);

  /**
   * Check whether a type is a subtype of the one designed by the fully qualified name.
   *<p>
   * This method will consider implemented interfaces as well as superclasses.
   * <code><pre>
   *   Type type;
   *   type.isSubtypeOf("Object[]");
   *   type.isSubtypeOf("org.mypackage.MyClass");
   *   type.isSubtypeOf("org.mypackage.MyInterface");
   *   type.isSubtypeOf("java.lang.Object");
   *</pre></code>
   *
   * @param fullyQualifiedName fully qualified name to check in the type hierarchy. Use "[]" for arrays.
   * @return true if the type is the one passed in parameter or have this type in its hierarchy. false otherwise.
   */
  boolean isSubtypeOf(String fullyQualifiedName);

  /**
   * Check whether a type is a subtype of another.
   *<p>
   * This method will consider implemented interfaces as well as superclasses.
   * <code><pre>
   *   Type type, myOtherType;
   *   type.isSubtypeOf(myOtherType);
   *</pre></code>
   *
   * @param  superType instance of a potential superType.
   * @return true if types are equivalent or if the one passed in parameter is in the hierarchy. false otherwise.
   */
  boolean isSubtypeOf(Type superType);

  /**
   * Check if this type is an array.
   * @return true if this is an array.
   */
  boolean isArray();

  /**
   * Check if this type is a class, an enum, an interface or an annotation.
   * @return true if this is a class, enum, interface or annotation.
   */
  boolean isClass();

  /**
   * Check if type is Void type. This is used to check type of method invocation expressions.
   * @return true if the type is void.
   */
  boolean isVoid();

  /**
   * Check if this type is a primitive.
   * @return true if this is a primitive type.
   */
  boolean isPrimitive();

  /**
   * Check if this type is the given primitive.
   * 
   * <code><pre>
   *   Type type;
   *   type.isPrimitive(Primitives.INT);
   *</pre></code>
   *
   * @param primitive primitive type to be checked with.
   * @return true if this is the primitive type
   */
  boolean isPrimitive(Primitives primitive);

  /**
   * Check if this type has been resolved.
   * Type can be unknown in incomplete part of Semantic Analysis or when bytecode for a type is not provided and a method cannot be resolved.
   * @return true if type has not been resolved by semantic analysis.
   */
  boolean isUnknown();

  /**
   * Check if this type is a primitive numerical type.
   * @return true if type is byte, char, short, int, long, float or double.
   */
  boolean isNumerical();

  /**
   * Fully qualified name of the type.
   * @return complete name of type.
   */
  String fullyQualifiedName();

  /**
   * simple name of the type.
   * @return simple name of type.
   */
  String name();

  /**
   * Symbol of this type.
   * @return the symbol declaring this type.
   */
  Symbol.TypeSymbol symbol();

  /**
   *Erasure of this type.
   * @return erased type.
   */
  Type erasure();

  /**
   * Type for arrays.
   */
  interface ArrayType extends Type {

    /**
     * Type of elements in this array.
     * @return type of an element.
     */
    Type elementType();

  }

}
