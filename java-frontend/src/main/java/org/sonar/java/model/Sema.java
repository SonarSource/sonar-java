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

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@MethodsAreNonnullByDefault
final class Sema implements ISemanticModel {

  final AST ast;

  final Map<IBinding, Tree> declarations = new HashMap<>();

  final Map<IBinding, List<IdentifierTree>> usages = new HashMap<>();

  Sema(AST ast) {
    this.ast = ast;
  }

  void declaration(IBinding binding, Tree node) {
    Objects.requireNonNull(binding);
    Objects.requireNonNull(node);
    declarations.put(binding, node);
  }

  void usage(IBinding binding, IdentifierTree identifier) {
    Objects.requireNonNull(binding);
    Objects.requireNonNull(identifier);
    usages.computeIfAbsent(binding, k -> new ArrayList<>()).add(identifier);
  }

  private final Map<IBinding, JSymbol> symbolsCache = new HashMap<>();

  JTypeSymbol typeSymbol(ITypeBinding binding) {
    return (JTypeSymbol) symbolsCache.computeIfAbsent(binding, k -> new JTypeSymbol(this, (ITypeBinding) k));
  }

  JMethodSymbol methodSymbol(IMethodBinding binding) {
    return (JMethodSymbol) symbolsCache.computeIfAbsent(binding, k -> new JMethodSymbol(this, (IMethodBinding) k));
  }

  JVariableSymbol variableSymbol(IVariableBinding binding) {
    return (JVariableSymbol) symbolsCache.computeIfAbsent(binding, k -> new JVariableSymbol(this, (IVariableBinding) k));
  }

  private final Map<ITypeBinding, JType> typesCache = new HashMap<>();

  JType type(ITypeBinding typeBinding) {
    return typesCache.computeIfAbsent(typeBinding, k -> new JType(this, k));
  }

  private final Map<IAnnotationBinding, JSymbolMetadata.JAnnotationInstance> annotationsCache = new HashMap<>();

  JSymbolMetadata.JAnnotationInstance annotation(IAnnotationBinding annotationBinding) {
    return annotationsCache.computeIfAbsent(annotationBinding, k -> new JSymbolMetadata.JAnnotationInstance(this, k));
  }

  @Nullable
  ITypeBinding resolveType(String name) {
    // for example in InstanceOfAlwaysTrueCheck
    int i = name.length();
    while (name.charAt(i - 1) == ']') {
      i -= 2;
    }
    int dimensions = (name.length() - i) / 2;
    String nameWithoutDimensions = name.substring(0, i);

    // for example "byte" in IntegerToHexStringCheck
    ITypeBinding result = ast.resolveWellKnownType(nameWithoutDimensions);

    if (result == null) {
      result = findNonPrimitiveType(ast, nameWithoutDimensions);
    }

    if (result == null) {
      return null;
    }

    return dimensions > 0 ? result.createArrayType(dimensions) : result;
  }

  @Nullable
  private static ITypeBinding findNonPrimitiveType(AST ast, String name) {
    // BindingResolver bindingResolver = ast.getBindingResolver();
    // ReferenceBinding referenceBinding = bindingResolver
    //   .lookupEnvironment()
    //   .getType(CharOperation.splitOn('.', fqn.toCharArray()));
    // return bindingResolver.getTypeBinding(referenceBinding);
    try {
      Method methodGetBindingResolver = ast.getClass().getDeclaredMethod("getBindingResolver");
      methodGetBindingResolver.setAccessible(true);
      Object bindingResolver = methodGetBindingResolver.invoke(ast);

      Method methodLookupEnvironment = bindingResolver.getClass().getDeclaredMethod("lookupEnvironment");
      methodLookupEnvironment.setAccessible(true);
      LookupEnvironment lookupEnvironment = (LookupEnvironment) methodLookupEnvironment.invoke(bindingResolver);

      ReferenceBinding referenceBinding = lookupEnvironment.getType(CharOperation.splitOn('.', name.toCharArray()));

      Method methodGetTypeBinding = bindingResolver.getClass().getDeclaredMethod("getTypeBinding", TypeBinding.class);
      methodGetTypeBinding.setAccessible(true);
      return (ITypeBinding) methodGetTypeBinding.invoke(bindingResolver, referenceBinding);

    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  @Nullable
  @Override
  public Type getClassType(String type) {
    ITypeBinding typeBinding = resolveType(type);
    if (typeBinding == null) {
      return null;
    }
    return type(typeBinding);
  }

}
