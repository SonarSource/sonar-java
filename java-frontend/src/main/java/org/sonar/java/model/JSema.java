/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTUtils;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.sonar.plugins.java.api.semantic.Sema;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;

public final class JSema implements Sema {

  private final AST ast;
  final Map<IBinding, Tree> declarations = new HashMap<>();
  final Map<IBinding, List<IdentifierTree>> usages = new HashMap<>();
  private final Map<ITypeBinding, JType> types = new HashMap<>();
  private final Map<IBinding, JSymbol> symbols = new HashMap<>();
  private final Map<Symbol.TypeSymbol, JInitializerBlockSymbol> initializerBlockSymbols = new HashMap<>();
  private final Map<Symbol.TypeSymbol, JInitializerBlockSymbol> staticInitializerBlockSymbols = new HashMap<>();
  private final Map<IAnnotationBinding, EcjSymbolMetadata.JAnnotationInstance> annotations = new HashMap<>();
  private final Map<String, Type> nameToTypeCache = new HashMap<>();

  JSema(AST ast) {
    this.ast = ast;
  }

  Supplier<Type> typeSupplier(@Nullable ITypeBinding binding) {
    return binding == null ? SemanticValues.UNKNOWN_TYPE : new EcjSemanticAttachment<>(binding, this::type);
  }

  Supplier<Symbol.MethodSymbol> methodSupplier(@Nullable IMethodBinding binding) {
    return binding == null ? SemanticValues.UNKNOWN_METHOD : new EcjSemanticAttachment<>(binding, this::methodSymbol);
  }

  Supplier<Symbol> symbolSupplier(@Nullable IBinding binding) {
    return binding == null ? SemanticValues.UNKNOWN_SYMBOL : new EcjSemanticAttachment<>(binding, this::symbol);
  }

  private Symbol symbol(IBinding binding) {
    return switch (binding.getKind()) {
      case IBinding.TYPE -> typeSymbol((ITypeBinding) binding);
      case IBinding.METHOD -> methodSymbol((IMethodBinding) binding);
      case IBinding.VARIABLE -> variableSymbol((IVariableBinding) binding);
      case IBinding.PACKAGE -> packageSymbol((IPackageBinding) binding);
      default -> Symbol.UNKNOWN_SYMBOL;
    };
  }

  public JType type(ITypeBinding typeBinding) {
    JType cached = types.get(typeBinding);
    if (cached != null) {
      return cached;
    }
    return types.computeIfAbsent(typeBinding, k -> new JType(this, JType.normalize(k)));
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
      return Symbol.ROOT_PACKAGE;
    }
    JSymbol cached = symbols.get(packageBinding);
    if (cached != null) {
      return cached;
    }
    return symbols.computeIfAbsent(packageBinding, k -> new JPackageSymbol(this, (IPackageBinding) k));
  }

  public JTypeSymbol typeSymbol(ITypeBinding typeBinding) {
    JTypeSymbol cached = (JTypeSymbol) symbols.get(typeBinding);
    if (cached != null) {
      return cached;
    }
    return (JTypeSymbol) symbols.computeIfAbsent(typeBinding, k -> new JTypeSymbol(this, JType.normalize((ITypeBinding) k)));
  }

  public JMethodSymbol methodSymbol(IMethodBinding methodBinding) {
    JMethodSymbol cached = (JMethodSymbol) symbols.get(methodBinding);
    if (cached != null) {
      return cached;
    }
    return (JMethodSymbol) symbols.computeIfAbsent(methodBinding, k -> new JMethodSymbol(this, (IMethodBinding) k));
  }

  public JInitializerBlockSymbol initializerBlockSymbol(JTypeSymbol owner) {
    JInitializerBlockSymbol cached = initializerBlockSymbols.get(owner);
    if (cached != null) {
      return cached;
    }
    return initializerBlockSymbols.computeIfAbsent(owner, k -> new JInitializerBlockSymbol(k, false));
  }

  public JInitializerBlockSymbol staticInitializerBlockSymbol(JTypeSymbol owner) {
    JInitializerBlockSymbol cached = staticInitializerBlockSymbols.get(owner);
    if (cached != null) {
      return cached;
    }
    return staticInitializerBlockSymbols.computeIfAbsent(owner, k -> new JInitializerBlockSymbol(k, true));
  }


  public JVariableSymbol variableSymbol(IVariableBinding variableBinding) {
    JVariableSymbol cached = (JVariableSymbol) symbols.get(variableBinding);
    if (cached != null) {
      return cached;
    }
    return (JVariableSymbol) symbols.computeIfAbsent(variableBinding, k -> new JVariableSymbol(this, (IVariableBinding) k));
  }

  EcjSymbolMetadata.JAnnotationInstance annotation(IAnnotationBinding annotationBinding) {
    EcjSymbolMetadata.JAnnotationInstance cached = annotations.get(annotationBinding);
    if (cached != null) {
      return cached;
    }
    return annotations.computeIfAbsent(annotationBinding, k -> new EcjSymbolMetadata.JAnnotationInstance(this, k));
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
    Type cached = nameToTypeCache.get(fullyQualifiedName);
    if (cached != null) {
      return cached;
    }
    return nameToTypeCache.computeIfAbsent(fullyQualifiedName, t -> {
      ITypeBinding typeBinding = resolveType(t);
      return typeBinding != null ? type(typeBinding) : Type.UNKNOWN;
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

  @Override
  public Runnable getEnvironmentCleaner() {
    return ASTUtils.getEnvironmentCleaner(ast);
  }

}
