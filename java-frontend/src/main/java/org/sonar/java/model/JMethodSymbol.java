/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.eclipse.jdt.core.dom.ASTUtils;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.VariableTree;

final class JMethodSymbol extends JSymbol implements Symbol.MethodSymbol {

  /**
   * Cache for {@link #parameterTypes()}.
   */
  private List<Type> parameterTypes;

  private List<Symbol> parameters;

  /**
   * Cache for {@link #returnType()}.
   */
  private TypeSymbol returnType;

  /**
   * Cache for {@link #thrownTypes()}.
   */
  private List<Type> thrownTypes;

  /**
   * Cache for {@link #overriddenSymbols()}.
   */
  private List<MethodSymbol> overriddenSymbols;

  private final String signature;

  JMethodSymbol(JSema sema, IMethodBinding methodBinding) {
    super(sema, methodBinding);
    this.signature = methodBinding().getDeclaringClass().getBinaryName()
      + "#" + name()
      + ASTUtils.signature(methodBinding().getMethodDeclaration());
  }

  IMethodBinding methodBinding() {
    return (IMethodBinding) binding;
  }

  public boolean isLambda() {
    return methodBinding().getDeclaringMember() != null;
  }

  @Override
  public List<Type> parameterTypes() {
    if (parameterTypes == null) {
      parameterTypes = sema.types(methodBinding().getParameterTypes());
    }
    return parameterTypes;
  }

  @Override
  public List<Symbol> declarationParameters() {
    if (parameters == null) {
      MethodTree declaration = declaration();
      if (declaration != null && !isCompactConstructor(declaration)) {
        parameters = declaration.parameters().stream().map(VariableTree::symbol).toList();
      } else {
        parameters = new ArrayList<>();
        IMethodBinding methodBinding = methodBinding();
        ITypeBinding[] parameterTypeBindings = methodBinding.getParameterTypes();
        for (int i = 0; i < parameterTypeBindings.length; i++) {
          parameters.add(new JVariableSymbol.ParameterPlaceholderSymbol(i, sema, methodBinding.getMethodDeclaration(), parameterTypeBindings[i]));
        }
      }
    }
    return parameters;
  }

  private static boolean isCompactConstructor(MethodTree methodTree) {
    return methodTree.closeParenToken() == null;
  }

  /**
   * @since 6.0 returns void type for constructors instead of {@code null}
   */
  @Override
  public TypeSymbol returnType() {
    if (returnType == null) {
      ITypeBinding methodBindingReturnType = methodBinding().getReturnType();
      // In rare circumstances, when the semantic information is incomplete, methodBindingReturnType can be null.
      if (methodBindingReturnType == null) {
        return TypeSymbol.UNKNOWN_TYPE;
      }
      this.returnType = sema.typeSymbol(methodBindingReturnType);
    }
    return returnType;
  }

  @Override
  public List<Type> thrownTypes() {
    if (thrownTypes == null) {
      thrownTypes = sema.types(methodBinding().getExceptionTypes());
    }
    return thrownTypes;
  }

  @Override
  public List<MethodSymbol> overriddenSymbols() {
    if (overriddenSymbols == null) {
      overriddenSymbols = findOverriddenSymbols();
    }
    return overriddenSymbols;
  }

  private List<MethodSymbol> findOverriddenSymbols() {
    Set<MethodSymbol> results = new LinkedHashSet<>();
    IMethodBinding methodBinding = methodBinding();
    findOverridesInParentTypes(results, methodBinding::overrides, methodBinding.getDeclaringClass());
    return new ArrayList<>(results);
  }

  @VisibleForTesting
  void findOverridesInParentTypes(Collection<MethodSymbol> accumulator, Predicate<IMethodBinding> overridesCondition, ITypeBinding type) {
    if (type.isInterface()) {
      // check Object for interfaces forcing overrides from Object
      findOverridesInTypes(accumulator, overridesCondition, sema.resolveType("java.lang.Object"));
    } else if (!"java.lang.Object".equals(type.getQualifiedName())) {
      findOverridesInTypes(accumulator, overridesCondition, type.getSuperclass());
    }
    findOverridesInTypes(accumulator, overridesCondition, type.getInterfaces());
  }

  private void findOverridesInTypes(Collection<MethodSymbol> accumulator, Predicate<IMethodBinding> overridesCondition, ITypeBinding... types) {
    for (ITypeBinding type : types) {
      if (type == null) {
        // Can happen for unknown reason.
        continue;
      }
      // check current type
      Stream.of(type.getDeclaredMethods())
        .filter(overridesCondition)
        .findFirst()
        .map(sema::methodSymbol)
        .ifPresent(accumulator::add);
      // check other inheritance levels
      findOverridesInParentTypes(accumulator, overridesCondition, type);
    }
  }

  @Override
  public String signature() {
    return signature;
  }

  @Override
  public boolean isOverridable() {
    return !isUnknown() && !(isPrivate() || isStatic() || isFinal() || owner().isFinal());
  }

  @Override
  public boolean isParametrizedMethod() {
    return !isUnknown() && (methodBinding().isParameterizedMethod() || methodBinding().isGenericMethod());
  }

  @Override
  public boolean isDefaultMethod() {
    return !isUnknown() && Modifier.isDefault(binding.getModifiers());
  }

  @Override
  public boolean isSynchronizedMethod() {
    return !isUnknown() && Modifier.isSynchronized(binding.getModifiers());
  }

  @Override
  public boolean isVarArgsMethod() {
    return !isUnknown() && methodBinding().isVarargs();
  }

  @Nullable
  @Override
  public MethodTree declaration() {
    return (MethodTree) super.declaration();
  }

  @Override
  public boolean isNativeMethod() {
    return !isUnknown() && Modifier.isNative(binding.getModifiers());
  }

  /** This is for debugging and doesn't follow a guaranteed format. */
  @Override
  public String toString() {
    return binding.getKey();
  }
}
