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
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class JMethodParameter implements Symbol.VariableSymbol {
  private final JMethodSymbol methodSymbol;
  private final int paramIndex;

  JMethodParameter(JMethodSymbol methodSymbol, int paramIndex) {
    this.methodSymbol = methodSymbol;
    this.paramIndex = paramIndex;
  }

  @Override
  public String name() {
    return "";
  }

  @Override
  public Symbol owner() {
    return methodSymbol;
  }

  @Override
  public Type type() {
    return methodSymbol.ast.type(
      ((IMethodBinding) methodSymbol.binding).getParameterTypes()[paramIndex]
    );
  }

  @Override
  public boolean isVariableSymbol() {
    return true;
  }

  @Override
  public boolean isTypeSymbol() {
    return false;
  }

  @Override
  public boolean isMethodSymbol() {
    return false;
  }

  @Override
  public boolean isPackageSymbol() {
    return false;
  }

  @Override
  public boolean isStatic() {
    return false;
  }

  @Override
  public boolean isFinal() {
    return false;
  }

  @Override
  public boolean isEnum() {
    return false;
  }

  @Override
  public boolean isInterface() {
    return false;
  }

  @Override
  public boolean isAbstract() {
    return false;
  }

  @Override
  public boolean isPublic() {
    return false;
  }

  @Override
  public boolean isPrivate() {
    return false;
  }

  @Override
  public boolean isProtected() {
    return false;
  }

  @Override
  public boolean isPackageVisibility() {
    return false;
  }

  @Override
  public boolean isDeprecated() {
    return false;
  }

  @Override
  public boolean isVolatile() {
    return false;
  }

  @Override
  public boolean isUnknown() {
    return false;
  }

  @Override
  public SymbolMetadata metadata() {
    return new JSymbolMetadata() {
      @Override
      public List<AnnotationInstance> annotations() {
        return Arrays.stream(((IMethodBinding) methodSymbol.binding).getParameterAnnotations(paramIndex))
          .map(methodSymbol.ast::annotation)
          .collect(Collectors.toList());
      }
    };
  }

  @Nullable
  @Override
  public TypeSymbol enclosingClass() {
    return methodSymbol.enclosingClass();
  }

  @Override
  public List<IdentifierTree> usages() {
    throw new NotImplementedException();
  }

  @Nullable
  @Override
  public VariableTree declaration() {
    throw new NotImplementedException();
  }
}
