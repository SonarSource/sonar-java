/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
import java.util.Optional;
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

  // cache for this.constantValue()
  private boolean constantValueComputed = false;
  private Optional<Object> constantValue;

  JVariableSymbol(JSema sema, IVariableBinding variableBinding) {
    super(sema, variableBinding);
  }

  @Nullable
  @Override
  public VariableTree declaration() {
    return (VariableTree) super.declaration();
  }

  @Override
  public boolean isEffectivelyFinal() {
    return ((IVariableBinding)binding).isEffectivelyFinal();
  }

  @Override
  public Optional<Object> constantValue() {
    if (!constantValueComputed) {
      constantValueComputed = true;
      if (!isFinal() || !isStatic()) {
        constantValue = Optional.empty();
      } else {
        Object c = ((IVariableBinding) binding).getConstantValue();
        if (c instanceof Short shortValue) {
          c = Integer.valueOf(shortValue);
        } else if (c instanceof Byte byteValue) {
          c = Integer.valueOf(byteValue);
        } else if (c instanceof Character characterValue) {
          c = Integer.valueOf(characterValue);
        }
        constantValue = Optional.ofNullable(c);
      }
    }
    return constantValue;
  }

  @Override
  public boolean isLocalVariable() {
    Symbol owner = owner();
    return owner != null && owner.isMethodSymbol();
  }

  @Override
  public boolean isParameter() {
    return ((IVariableBinding) binding).isParameter();
  }


  static class ParameterPlaceholderSymbol extends Symbols.DefaultSymbol implements Symbol.VariableSymbol {
    private final String name;
    private final Symbol owner;
    private final Type type;
    private final JSymbolMetadata metadata;


    ParameterPlaceholderSymbol(int index, JSema sema, IMethodBinding owner, ITypeBinding typeBinding) {
      name = "arg" + index;
      this.owner = sema.methodSymbol(owner);
      this.type = sema.type(typeBinding);

      metadata = new JSymbolMetadata(
        sema,
        this,
        typeBinding.getTypeAnnotations(),
        owner.getParameterAnnotations(index)
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
    public boolean isEffectivelyFinal() {
      return false;
    }

    @Override
    public Optional<Object> constantValue() {
      return Optional.empty();
    }

    @Override
    public boolean isLocalVariable() {
      return false;
    }

    @Override
    public boolean isParameter() {
      return true;
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
