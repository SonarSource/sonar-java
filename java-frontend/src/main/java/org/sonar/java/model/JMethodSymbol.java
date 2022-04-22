/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.eclipse.jdt.core.dom.ASTUtils;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
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
      if (declaration != null) {
        parameters = declaration.parameters().stream().map(VariableTree::symbol).collect(Collectors.toList());
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

  /**
   * @since 6.0 returns void type for constructors instead of {@code null}
   */
  @Override
  public TypeSymbol returnType() {
    if (returnType == null) {
      ITypeBinding methodBindingReturnType = methodBinding().getReturnType();
      // In rare circumstances, when the semantic information is incomplete, methodBindingReturnType can be null.
      if (methodBindingReturnType == null) {
        return Symbols.unknownTypeSymbol;
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

  @Nullable
  @Override
  public MethodTree declaration() {
    return (MethodTree) super.declaration();
  }

}
