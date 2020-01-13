/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.sonar.java.resolve.Symbols;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

final class JTypeSymbol extends JSymbol implements Symbol.TypeSymbol {

  final SpecialField superSymbol = new SpecialField() {
    @Override
    public String name() {
      return "super";
    }

    @Override
    public Type type() {
      if (typeBinding().isInterface()) {
        // JLS § 15.12.1:
        // for "T.super.foo()", if T is an interface, 'super' keyword is used to access method of the interface itself
        return sema.type(typeBinding());
      }
      return sema.type(typeBinding().getSuperclass());
    }
  };
  final SpecialField thisSymbol = new SpecialField() {
    @Override
    public String name() {
      return "this";
    }

    @Override
    public Type type() {
      return sema.type(typeBinding());
    }
  };

  JTypeSymbol(JSema sema, ITypeBinding typeBinding) {
    super(sema, typeBinding);
  }

  ITypeBinding typeBinding() {
    return (ITypeBinding) binding;
  }

  @CheckForNull
  @Override
  public Type superClass() {
    if (typeBinding().isInterface() || typeBinding().isArray()) {
      return sema.type(Objects.requireNonNull(sema.resolveType("java.lang.Object")));
    } else if (typeBinding().getSuperclass() == null) {
      // java.lang.Object
      return null;
    } else {
      return sema.type(typeBinding().getSuperclass());
    }
  }

  @Override
  public List<Type> interfaces() {
    return Arrays.stream(typeBinding().getInterfaces())
      .map(sema::type)
      .collect(Collectors.toList());
  }

  @Override
  public Collection<Symbol> memberSymbols() {
    Collection<Symbol> members = new ArrayList<>();
    for (ITypeBinding b : typeBinding().getDeclaredTypes()) {
      members.add(sema.typeSymbol(b));
    }
    for (IVariableBinding b : typeBinding().getDeclaredFields()) {
      members.add(sema.variableSymbol(b));
    }
    for (IMethodBinding b : typeBinding().getDeclaredMethods()) {
      members.add(sema.methodSymbol(b));
    }
    return members;
  }

  @Override
  public Collection<Symbol> lookupSymbols(String name) {
    return memberSymbols().stream()
      .filter(m -> name.equals(m.name()))
      .collect(Collectors.toSet());
  }

  @Nullable
  @Override
  public ClassTree declaration() {
    return (ClassTree) super.declaration();
  }

  abstract class SpecialField extends Symbols.DefaultSymbol implements Symbol.VariableSymbol {
    @Override
    public final Symbol owner() {
      return JTypeSymbol.this;
    }

    @Override
    public final boolean isVariableSymbol() {
      return true;
    }

    @Override
    public final boolean isFinal() {
      return true;
    }

    @Override
    public final boolean isUnknown() {
      return false;
    }

    @Override
    public final TypeSymbol enclosingClass() {
      return JTypeSymbol.this;
    }

    @Override
    public final List<IdentifierTree> usages() {
      return Collections.emptyList();
    }

    @Nullable
    @Override
    public final VariableTree declaration() {
      return null;
    }
  }

}
