/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonarsource.analyzer.commons.collections.MapBuilder;

public final class JUtils {

  private JUtils() {
  }

  private static final Map<String, String> WRAPPER_TO_PRIMITIVE = MapBuilder.<String, String>newMap()
    .put("java.lang.Byte", "byte")
    .put("java.lang.Character", "char")
    .put("java.lang.Short", "short")
    .put("java.lang.Integer", "int")
    .put("java.lang.Long", "long")
    .put("java.lang.Float", "float")
    .put("java.lang.Double", "double")
    .put("java.lang.Boolean", "boolean")
    .build();

  private static final Map<String, String> PRIMITIVE_TO_WRAPPER = MapBuilder.<String, String>newMap()
    .put("byte", "java.lang.Byte")
    .put("char", "java.lang.Character")
    .put("short", "java.lang.Short")
    .put("int", "java.lang.Integer")
    .put("long","java.lang.Long")
    .put("float", "java.lang.Float")
    .put("double", "java.lang.Double")
    .put("boolean", "java.lang.Boolean")
    .build();

  public static boolean isPrimitiveWrapper(Type type) {
    return type.isClass() && WRAPPER_TO_PRIMITIVE.containsKey(type.fullyQualifiedName());
  }

  @Nullable
  public static Type primitiveWrapperType(Type type) {
    String name = PRIMITIVE_TO_WRAPPER.get(type.fullyQualifiedName());
    if (name == null) {
      return null;
    }
    JSema sema = ((JType) type).sema;
    return sema.type(sema.resolveType(name));
  }

  @Nullable
  public static Type primitiveType(Type type) {
    String name = WRAPPER_TO_PRIMITIVE.get(type.fullyQualifiedName());
    if (name == null) {
      return null;
    }
    JSema sema = ((JType) type).sema;
    return sema.type(sema.resolveType(name));
  }

  public static boolean isNullType(Type type) {
    return !type.isUnknown() && ((JType) type).typeBinding.isNullType();
  }

  public static boolean isIntersectionType(Type type) {
    return !type.isUnknown() && ((JType) type).typeBinding.isIntersectionType();
  }

  public static boolean isTypeVar(Type type) {
    return !type.isUnknown() && ((JType) type).typeBinding.isTypeVariable();
  }

  public static boolean isAnnotation(Symbol.TypeSymbol typeSymbol) {
    return !typeSymbol.isUnknown() && ((JTypeSymbol) typeSymbol).typeBinding().isAnnotation();
  }

  public static boolean isEffectivelyFinal(Symbol.VariableSymbol variableSymbol) {
    return (variableSymbol instanceof JVariableSymbol) && ((IVariableBinding) ((JVariableSymbol) variableSymbol).binding).isEffectivelyFinal();
  }

  public static boolean isLocalVariable(Symbol symbol) {
    return symbol.isVariableSymbol() && symbol.owner().isMethodSymbol();
  }

  public static boolean isParameter(Symbol symbol) {
    if (symbol instanceof JTypeSymbol.SpecialField) {
      return false;
    }
    return symbol.isVariableSymbol() && 
      ((symbol instanceof JVariableSymbol.ParameterPlaceholderSymbol)
        || ((IVariableBinding) ((JVariableSymbol) symbol).binding).isParameter());
  }

  public static Optional<Object> constantValue(Symbol.VariableSymbol symbol) {
    if (!symbol.isFinal() || !symbol.isStatic() || !(symbol instanceof JVariableSymbol)) {
      return Optional.empty();
    }
    Object c = ((IVariableBinding) ((JVariableSymbol) symbol).binding).getConstantValue();
    if (c instanceof Short) {
      c = Integer.valueOf((Short) c);
    } else if (c instanceof Byte) {
      c = Integer.valueOf((Byte) c);
    } else if (c instanceof Character) {
      c = Integer.valueOf((Character) c);
    }
    return Optional.ofNullable(c);
  }

  public static Set<Type> superTypes(Symbol.TypeSymbol typeSymbol) {
    if (typeSymbol.isUnknown()) {
      return Collections.emptySet();
    }
    Set<Type> result = new HashSet<>();
    collectSuperTypes(result, ((JTypeSymbol) typeSymbol).sema, ((JTypeSymbol) typeSymbol).typeBinding());
    return result;
  }

  private static void collectSuperTypes(Set<Type> result, JSema sema, ITypeBinding typeBinding) {
    ITypeBinding s = typeBinding.getSuperclass();
    if (s != null) {
      result.add(sema.type(s));
      collectSuperTypes(result, sema, s);
    }
    for (ITypeBinding i : typeBinding.getInterfaces()) {
      result.add(sema.type(i));
      collectSuperTypes(result, sema, i);
    }
  }

  public static Symbol.TypeSymbol outermostClass(Symbol.TypeSymbol typeSymbol) {
    Symbol symbol = typeSymbol;
    Symbol result = null;
    while (!symbol.isPackageSymbol()) {
      result = symbol;
      symbol = symbol.owner();
    }
    return (Symbol.TypeSymbol) result;
  }

  public static Symbol getPackage(Symbol symbol) {
    while (!symbol.isPackageSymbol()) {
      symbol = symbol.owner();
    }
    return symbol;
  }

  public static boolean isVarArgsMethod(Symbol.MethodSymbol method) {
    return !method.isUnknown() && ((JMethodSymbol) method).methodBinding().isVarargs();
  }

  public static boolean isSynchronizedMethod(Symbol.MethodSymbol method) {
    return !method.isUnknown() && Modifier.isSynchronized(((JMethodSymbol) method).binding.getModifiers());
  }

  public static boolean isNativeMethod(Symbol.MethodSymbol method) {
    return !method.isUnknown() && Modifier.isNative(((JMethodSymbol) method).binding.getModifiers());
  }

  public static boolean isDefaultMethod(Symbol.MethodSymbol method) {
    return !method.isUnknown() && Modifier.isDefault(((JMethodSymbol) method).binding.getModifiers());
  }

  @Nullable
  public static Object defaultValue(Symbol.MethodSymbol method) {
    if (method.isUnknown()) {
      return null;
    }
    return ((JMethodSymbol) method).methodBinding().getDefaultValue();
  }

  public static boolean isOverridable(Symbol.MethodSymbol method) {
    return !method.isUnknown() && !(method.isPrivate() || method.isStatic() || method.isFinal() || method.owner().isFinal());
  }

  public static boolean isParametrizedMethod(Symbol.MethodSymbol method) {
    if (method.isUnknown()) {
      return false;
    }
    return ((JMethodSymbol) method).methodBinding().isParameterizedMethod()
      || ((JMethodSymbol) method).methodBinding().isGenericMethod();
  }

  public static boolean isRawType(Type type) {
    if (type.isUnknown()) {
      return false;
    }
    JType t = (JType) type;
    return t.typeBinding.isRawType();
  }

  public static Type declaringType(Type type) {
    if (type.isUnknown()) {
      return type;
    }
    JType t = (JType) type;
    return t.sema.type(t.typeBinding.getTypeDeclaration());
  }

  public static Set<Type> directSuperTypes(Type type) {
    if (type.isUnknown()) {
      return Collections.emptySet();
    }
    Set<Type> result = new HashSet<>();
    JType t = (JType) type;
    ITypeBinding superclass = t.typeBinding.getSuperclass();
    if (superclass != null) {
      result.add(t.sema.type(superclass));
    }
    for (ITypeBinding i : t.typeBinding.getInterfaces()) {
      result.add(t.sema.type(i));
    }
    return result;
  }

  @Nullable
  public static Symbol enclosingClass(Tree t) {
    do {
      if (t == null) {
        return null;
      }
      if (t.is(Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE)) {
        return ((ClassTree) t).symbol();
      }
      t = t.parent();
    } while (true);
  }

  @Nullable
  public static Symbol importTreeSymbol(ImportTree tree) {
    return ((JavaTree.ImportTreeImpl) tree).symbol();
  }

  public static Symbol typeParameterTreeSymbol(TypeParameterTree tree) {
    return ((TypeParameterTreeImpl) tree).symbol();
  }

  public static SymbolMetadata parameterAnnotations(Symbol.MethodSymbol method, int param) {
    if (method.isUnknown()) {
      return Symbols.EMPTY_METADATA;
    }
    return method.declarationParameters().get(param).metadata();
  }

}
