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
package org.sonar.plugins.java.api.semantic;

/**
 * Interface to access resolved type of an expression or a Type.
 */
public interface Type {

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
   * This method will consider implementing interfaces as well as superclasses.
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
   * Check if this type is a primitive.
   * @return true if this is a primitive type.
   */
  boolean isPrimitive();

  /**
   * Fully qualified name of the type.
   * @return complete name of type, followed by [] for arrays.
   */
  String name();

}
