/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTUtils;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;

public final class JSema implements Sema {

  private final AST ast;
  final Set<JProblem> undefinedTypes = new HashSet<>();
  final Map<IBinding, Tree> declarations = new HashMap<>();
  final Map<IBinding, List<IdentifierTree>> usages = new HashMap<>();
  private final Map<ITypeBinding, JType> types = new HashMap<>();
  private final Map<IBinding, JSymbol> symbols = new HashMap<>();
  private final Map<Symbol.TypeSymbol, JInitializerBlockSymbol> initializerBlockSymbols = new HashMap<>();
  private final Map<Symbol.TypeSymbol, JInitializerBlockSymbol> staticInitializerBlockSymbols = new HashMap<>();
  private final Map<IAnnotationBinding, JSymbolMetadata.JAnnotationInstance> annotations = new HashMap<>();
  private final Map<String, Type> nameToTypeCache = new HashMap<>();

  JSema(AST ast) {
    this.ast = ast;
  }

  public JType type(ITypeBinding typeBinding) {
    return types.computeIfAbsent(typeBinding, k -> new JType(this, JType.normalize(typeBinding)));
  }

  List<Type> types(ITypeBinding[] typeBindings) {
    if (typeBindings.length == 0) {
      return Collections.emptyList();
    }
    Type[] result = new Type[typeBindings.length];
    for (int i = 0; i < typeBindings.length; i++) {
      result[i] = type(typeBindings[i]);
    }
    return Arrays.asList(result);
  }

  public Symbol packageSymbol(@Nullable IPackageBinding packageBinding) {
    if (packageBinding == null) {
      return Symbols.rootPackage;
    }
    return symbols.computeIfAbsent(packageBinding, k -> new JPackageSymbol(this, (IPackageBinding) k));
  }

  public JTypeSymbol typeSymbol(ITypeBinding typeBinding) {
    return (JTypeSymbol) symbols.computeIfAbsent(typeBinding, k -> new JTypeSymbol(this, JType.normalize((ITypeBinding) k)));
  }

  public JMethodSymbol methodSymbol(IMethodBinding methodBinding) {
    return (JMethodSymbol) symbols.computeIfAbsent(methodBinding, k -> new JMethodSymbol(this, (IMethodBinding) k));
  }

  public JInitializerBlockSymbol initializerBlockSymbol(JTypeSymbol owner) {
    return initializerBlockSymbols.computeIfAbsent(owner, k -> new JInitializerBlockSymbol(owner, false));
  }

  public JInitializerBlockSymbol staticInitializerBlockSymbol(JTypeSymbol owner) {
    return staticInitializerBlockSymbols.computeIfAbsent(owner, k -> new JInitializerBlockSymbol(owner, true));
  }


  public JVariableSymbol variableSymbol(IVariableBinding variableBinding) {
    return (JVariableSymbol) symbols.computeIfAbsent(variableBinding, k -> new JVariableSymbol(this, (IVariableBinding) k));
  }

  JSymbolMetadata.JAnnotationInstance annotation(IAnnotationBinding annotationBinding) {
    return annotations.computeIfAbsent(annotationBinding, k -> new JSymbolMetadata.JAnnotationInstance(this, k));
  }

  static IBinding declarationBinding(IBinding binding) {
    switch (binding.getKind()) {
      case IBinding.TYPE:
        return ((ITypeBinding) binding).getTypeDeclaration();
      case IBinding.METHOD:
        return ((IMethodBinding) binding).getMethodDeclaration();
      case IBinding.VARIABLE:
        return ((IVariableBinding) binding).getVariableDeclaration();
      default:
        return binding;
    }
  }

  @Override
  public Type getClassType(String fullyQualifiedName) {
    return nameToTypeCache.computeIfAbsent(fullyQualifiedName, t -> {
      ITypeBinding typeBinding = resolveType(t);
      return typeBinding != null ? type(typeBinding) : Symbols.unknownType;
    });
  }

  @Nullable
  ITypeBinding resolveType(String name) {
    int dimensions = 0;
    int end = name.length() - 1;
    while (name.charAt(end) == ']') {
      end -= 2;
      dimensions++;
    }
    name = name.substring(0, end + 1);

    ITypeBinding typeBinding = ast.resolveWellKnownType(name);
    if (typeBinding == null) {
      typeBinding = ASTUtils.resolveType(ast, name);
      if (typeBinding == null) {
        return null;
      }
    }
    return dimensions == 0 ? typeBinding : typeBinding.createArrayType(dimensions);
  }

  IAnnotationBinding[] resolvePackageAnnotations(String packageName) {
    return ASTUtils.resolvePackageAnnotations(ast, packageName);
  }

  public Runnable getEnvironmentCleaner() {
    return ASTUtils.getEnvironmentCleaner(ast);
  }

  public Set<JProblem> undefinedTypes() {
    return Collections.unmodifiableSet(undefinedTypes);
  }
}
