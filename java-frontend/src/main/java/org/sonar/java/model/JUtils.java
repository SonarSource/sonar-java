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

import org.eclipse.jdt.core.dom.Modifier;
import org.sonar.java.resolve.Flags;
import org.sonar.java.resolve.JavaSymbol.MethodJavaSymbol;
import org.sonar.java.resolve.JavaSymbol.TypeJavaSymbol;
import org.sonar.java.resolve.JavaType;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public final class JUtils {

  private JUtils() {
  }

  private static Map<String, String> WRAPPER_TO_PRIMITIVE = new HashMap<>();

  static {
    WRAPPER_TO_PRIMITIVE.put("java.lang.Byte", "byte");
    WRAPPER_TO_PRIMITIVE.put("java.lang.Character", "char");
    WRAPPER_TO_PRIMITIVE.put("java.lang.Short", "short");
    WRAPPER_TO_PRIMITIVE.put("java.lang.Integer", "int");
    WRAPPER_TO_PRIMITIVE.put("java.lang.Long", "long");
    WRAPPER_TO_PRIMITIVE.put("java.lang.Float", "float");
    WRAPPER_TO_PRIMITIVE.put("java.lang.Double", "double");
    WRAPPER_TO_PRIMITIVE.put("java.lang.Boolean", "boolean");
  }

  /**
   * Replacement for {@link JavaType#isPrimitiveWrapper()}
   */
  public static boolean isPrimitiveWrapper(Type type) {
    return type.isClass() && WRAPPER_TO_PRIMITIVE.containsKey(type.fullyQualifiedName());
  }

  /**
   * Replacement for {@link JavaType#primitiveType()}
   */
  @Nullable
  public static Type primitiveType(Type type) {
    if (!(type instanceof JType)) {
      return ((JavaType) type).primitiveType();
    }
    String name = WRAPPER_TO_PRIMITIVE.get(type.fullyQualifiedName());
    if (name == null) {
      return null;
    }
    JSema sema = ((JType) type).sema;
    return sema.type(sema.resolveType(name));
  }

  public static boolean isNullType(Type type) {
    if (!(type instanceof JType)) {
      return ((JavaType) type).isTagged(JavaType.BOT);
    }
    return ((JType) type).typeBinding.isNullType();
  }

  /**
   * Replacement for {@link JavaType#isTagged(int)} {@link JavaType#TYPEVAR}
   */
  public static boolean isTypeVar(Type type) {
    if (!(type instanceof JType)) {
      return ((JavaType) type).isTagged(JavaType.TYPEVAR);
    }
    return ((JType) type).typeBinding.isTypeVariable();
  }

  /**
   * Replacement for {@link TypeJavaSymbol#isAnnotation()}
   */
  public static boolean isAnnotation(Symbol.TypeSymbol typeSymbol) {
    if (!(typeSymbol instanceof JSymbol)) {
      return ((TypeJavaSymbol) typeSymbol).isAnnotation();
    }
    return ((JTypeSymbol) typeSymbol).typeBinding().isAnnotation();
  }

  /**
   * Replacement for {@link MethodJavaSymbol#isVarArgs()}
   */
  public static boolean isVarArgsMethod(Symbol.MethodSymbol method) {
    if (!(method instanceof JMethodSymbol)) {
      return ((MethodJavaSymbol) method).isVarArgs();
    }
    return ((JMethodSymbol) method).methodBinding().isVarargs();
  }

  public static boolean isSynchronizedMethod(Symbol.MethodSymbol method) {
    if (!(method instanceof JMethodSymbol)) {
      return Flags.isFlagged(((MethodJavaSymbol) method).flags(), Flags.SYNCHRONIZED);
    }
    return Modifier.isSynchronized(((JMethodSymbol) method).binding.getModifiers());
  }

  /**
   * Replacement for {@link MethodJavaSymbol#isOverridable()}
   */
  public static boolean isOverridable(Symbol.MethodSymbol method) {
    return !(method.isPrivate() || method.isStatic() || method.isFinal() || method.owner().isFinal());
  }

}
