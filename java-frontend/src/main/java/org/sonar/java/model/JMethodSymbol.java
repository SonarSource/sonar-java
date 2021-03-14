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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import org.eclipse.jdt.core.dom.ASTUtils;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodTree;

final class JMethodSymbol extends JSymbol implements Symbol.MethodSymbol {

  /**
   * Cache for {@link #parameterTypes()}.
   */
  private List<Type> parameterTypes;

  /**
   * Cache for {@link #returnType()}.
   */
  private TypeSymbol returnType;

  /**
   * Cache for {@link #thrownTypes()}.
   */
  private List<Type> thrownTypes;

  /**
   * Cache for {@link #overriddenSymbol()}.
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

  @Override
  public List<Type> parameterTypes() {
    if (parameterTypes == null) {
      parameterTypes = sema.types(methodBinding().getParameterTypes());
    }
    return parameterTypes;
  }

  /**
   * @since 6.0 returns void type for constructors instead of {@code null}
   */
  @Override
  public TypeSymbol returnType() {
    if (returnType == null) {
      returnType = sema.typeSymbol(methodBinding().getReturnType());
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

  @Nullable
  @Override
  public MethodSymbol overriddenSymbol() {
    return overriddenSymbols().stream()
      .findFirst()
      .orElse(null);
  }

  @Override
  public List<MethodSymbol> overriddenSymbols() {
    if (overriddenSymbols == null) {
      overriddenSymbols = convertOverriddenSymbols();
    }
    return overriddenSymbols;
  }

  private List<MethodSymbol> convertOverriddenSymbols() {
    IMethodBinding methodBinding = methodBinding();
    return find(methodBinding::overrides, methodBinding.getDeclaringClass()).stream()
      .map(sema::methodSymbol)
      .collect(Collectors.toList());
  }

  private List<IMethodBinding> find(Predicate<IMethodBinding> predicate, ITypeBinding typeBinding) {
    List<IMethodBinding> bindings = new ArrayList<>();

    addMissing(findSuperclassBindings(predicate, typeBinding.getSuperclass(), true), bindings);
    addMissing(findInterfaceBindings(predicate, typeBinding.getInterfaces()), bindings);

    ITypeBinding objectTypeBinding = Objects.requireNonNull(sema.resolveType("java.lang.Object"));
    addMissing(findSuperclassBindings(predicate, objectTypeBinding, true), bindings);

    return bindings;
  }

  private static List<IMethodBinding> findInterfaceBindings(Predicate<IMethodBinding> predicate, ITypeBinding[] interfaceBindings) {
    List<IMethodBinding> bindings = new ArrayList<>();

    for (ITypeBinding typeBinding : interfaceBindings) {
      for (IMethodBinding candidate : typeBinding.getDeclaredMethods()) {
        if (predicate.test(candidate)) {
          bindings.add(candidate);
        }
      }

      bindings.addAll(findInterfaceBindings(predicate, typeBinding.getInterfaces()));
    }
    return bindings;
  }

  private static List<IMethodBinding> findSuperclassBindings(Predicate<IMethodBinding> predicate, ITypeBinding typeBinding, boolean checkClassMethods) {
    if (typeBinding == null) {
      return new ArrayList<>();
    }

    List<IMethodBinding> bindings = new ArrayList<>();

    if (checkClassMethods) {
      for (IMethodBinding candidate : typeBinding.getDeclaredMethods()) {
        if (predicate.test(candidate)) {
          bindings.add(candidate);
          checkClassMethods = false;
        }
      }
    }

    bindings.addAll(findSuperclassBindings(predicate, typeBinding.getSuperclass(), checkClassMethods));
    bindings.addAll(findInterfaceBindings(predicate, typeBinding.getInterfaces()));

    return bindings;
  }

  private static <T> void addMissing(Collection<T> items, Collection<T> target) {
    items.forEach(item -> {
      if (!target.contains(item)) {
        target.add(item);
      }
    });
  }

  @Override
  public String signature() {
    return signature;
  }

  @Nullable
  @Override
  public MethodTree declaration() {
    return (MethodTree) super.declaration();
  }

}
