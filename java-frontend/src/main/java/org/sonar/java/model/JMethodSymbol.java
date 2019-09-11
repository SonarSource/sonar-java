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

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodTree;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final class JMethodSymbol extends JSymbol implements Symbol.MethodSymbol {

  JMethodSymbol(JSema sema, IMethodBinding methodBinding) {
    super(sema, methodBinding);
  }

  IMethodBinding methodBinding() {
    return (IMethodBinding) binding;
  }

  @Override
  public List<Type> parameterTypes() {
    return Arrays.stream(methodBinding().getParameterTypes())
      .map(sema::type)
      .collect(Collectors.toList());
  }

  @Override
  public TypeSymbol returnType() {
    return sema.typeSymbol(methodBinding().getReturnType());
  }

  @Override
  public List<Type> thrownTypes() {
    return Arrays.stream(methodBinding().getExceptionTypes())
      .map(sema::type)
      .collect(Collectors.toList());
  }

  @Nullable
  @Override
  public MethodSymbol overriddenSymbol() {
    IMethodBinding overrides = find(
      methodBinding()::overrides,
      methodBinding().getDeclaringClass()
    );
    if (overrides == null) {
      return null;
    }
    return sema.methodSymbol(overrides);
  }

  @Nullable
  private IMethodBinding find(Predicate<IMethodBinding> predicate, ITypeBinding t) {
    for (IMethodBinding candidate : t.getDeclaredMethods()) {
      if (predicate.test(candidate)) {
        return candidate;
      }
    }
    for (ITypeBinding i : t.getInterfaces()) {
      IMethodBinding r = find(predicate, i);
      if (r != null) {
        return r;
      }
    }
    if (t.getSuperclass() != null) {
      return find(predicate, t.getSuperclass());
    }
    ITypeBinding objectTypeBinding = Objects.requireNonNull(sema.resolveType("java.lang.Object"));
    if (t != objectTypeBinding) {
      return find(predicate, objectTypeBinding);
    }
    return null;
  }

  @Override
  public String signature() {
    return null; // FIXME stub for future implementation
  }

  @Nullable
  @Override
  public MethodTree declaration() {
    return (MethodTree) super.declaration();
  }

}
