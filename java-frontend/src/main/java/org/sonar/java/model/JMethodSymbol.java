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
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@MethodsAreNonnullByDefault
final class JMethodSymbol extends JSymbol implements Symbol.MethodSymbol {

  JMethodSymbol(Sema sema, IMethodBinding methodBinding) {
    super(sema, methodBinding);
  }

  @Nullable
  @Override
  public MethodTree declaration() {
    return (MethodTree) super.declaration();
  }

  @Override
  public List<Type> parameterTypes() {
    return Arrays.stream(((IMethodBinding) binding).getParameterTypes())
      .map(ast::type)
      .collect(Collectors.toList());
  }

  @Override
  public TypeSymbol returnType() {
    return ast.typeSymbol(((IMethodBinding) binding).getReturnType());
  }

  @Override
  public List<Type> thrownTypes() {
    return Arrays.stream(((IMethodBinding) binding).getExceptionTypes())
      .map(ast::type)
      .collect(Collectors.toList());
  }

  @Nullable
  @Override
  public MethodSymbol overriddenSymbol() {
    // TODO what about unresolved?
    IMethodBinding overrides = find(
      ast,
      ((IMethodBinding) binding)::overrides,
      ((IMethodBinding) binding).getDeclaringClass()
    );
    if (overrides != null) {
      return ast.methodSymbol(overrides);
    }
    return null;
  }

  @Nullable
  private static IMethodBinding find(Sema ctx, Predicate<IMethodBinding> predicate, ITypeBinding t) {
    for (IMethodBinding candidate : t.getDeclaredMethods()) {
      if (predicate.test(candidate)) {
        return candidate;
      }
    }
    for (ITypeBinding i : t.getInterfaces()) {
      IMethodBinding r = find(ctx, predicate, i);
      if (r != null) {
        return r;
      }
    }
    if (t.getSuperclass() != null) {
      return find(ctx, predicate, t.getSuperclass());
    } else {
      ITypeBinding objectType = ctx.ast.resolveWellKnownType("java.lang.Object");
      if (t != objectType) {
        return find(ctx, predicate, objectType);
      }
    }
    return null;
  }

  @Override
  public String signature() {
    // FIXME
    throw new NotImplementedException();
  }

}
