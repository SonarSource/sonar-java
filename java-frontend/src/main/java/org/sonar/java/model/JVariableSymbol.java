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

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.VariableTree;

final class JVariableSymbol extends JSymbol implements Symbol.VariableSymbol {

  JVariableSymbol(JSema sema, IVariableBinding variableBinding) {
    super(sema, variableBinding);
  }

  @Nullable
  @Override
  public VariableTree declaration() {
    return (VariableTree) super.declaration();
  }


  static class ParameterPlaceholderSymbol extends Symbols.DefaultSymbol implements Symbol.VariableSymbol {
    private final String name;
    private final Symbol owner;
    private final Type type;
    private final JSymbolMetadata metadata;


    ParameterPlaceholderSymbol(int index, Symbol owner, ITypeBinding typeBinding) {
      name = "arg" + index;
      this.owner = owner;
      this.type = ((JSymbol) owner).sema.type(typeBinding);

      IMethodBinding methodBinding = (IMethodBinding) ((JSymbol) owner).binding;
      metadata = new JSymbolMetadata(
        ((JSymbol) owner).sema,
        this,
        typeBinding.getTypeAnnotations(),
        methodBinding.getParameterAnnotations(index)
      );
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public Symbol owner() {
      return owner;
    }

    @Override
    public Type type() {
      return type;
    }

    @Override
    public boolean isUnknown() {
      return false;
    }

    @Override
    public TypeSymbol enclosingClass() {
      return owner.enclosingClass();
    }

    @Override
    public List<IdentifierTree> usages() {
      return Collections.emptyList();
    }

    @Override
    public VariableTree declaration() {
      return null;
    }

    @Override
    public SymbolMetadata metadata() {
      return metadata;
    }

    @Override
    public boolean isVariableSymbol() {
      return true;
    }

    @Override
    public boolean isFinal() {
      // From a caller perspective, it is not useful (and even not possible) to know if the parameter is final.
      return false;
    }

  }
}
