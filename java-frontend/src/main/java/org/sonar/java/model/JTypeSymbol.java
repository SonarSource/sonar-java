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
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.sonar.java.resolve.Symbols;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@MethodsAreNonnullByDefault
final class JTypeSymbol extends JSymbol implements Symbol.TypeSymbol {

  JTypeSymbol(Sema sema, ITypeBinding typeBinding) {
    super(sema, typeBinding);
  }

  @Nullable
  @Override
  public ClassTree declaration() {
    return (ClassTree) super.declaration();
  }

  @CheckForNull
  @Override
  public Type superClass() {
    if ("java.lang.Object".equals(((ITypeBinding) binding).getQualifiedName())) {
      return null;
    }
    if (((ITypeBinding) binding).getSuperclass() == null) {
      return Symbols.unknownType;
    }
    return ast.type(((ITypeBinding) binding).getSuperclass());
  }

  @Override
  public List<Type> interfaces() {
    return Arrays.stream(((ITypeBinding) binding).getInterfaces())
      .map(ast::type)
      .collect(Collectors.toList());
  }

  @Override
  public Collection<Symbol> memberSymbols() {
    Collection<Symbol> members = new ArrayList<>();
    ITypeBinding typeBinding = (ITypeBinding) binding;
    for (ITypeBinding b : typeBinding.getDeclaredTypes()) {
      members.add(ast.typeSymbol(b));
    }
    for (IVariableBinding b : typeBinding.getDeclaredFields()) {
      members.add(ast.variableSymbol(b));
    }
    for (IMethodBinding b : typeBinding.getDeclaredMethods()) {
      members.add(ast.methodSymbol(b));
    }
    // TODO old implementation also provides "this" and "super" - see AbstractClassWithoutAbstractMethodCheck
    return members;
  }

  @Override
  public Collection<Symbol> lookupSymbols(String name) {
    // FIXME
    throw new NotImplementedException();
  }

}
