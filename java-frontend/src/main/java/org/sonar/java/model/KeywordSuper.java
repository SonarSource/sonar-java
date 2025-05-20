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

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;

import javax.annotation.Nullable;

final class KeywordSuper extends IdentifierTreeImpl {

  KeywordSuper(InternalSyntaxToken token, @Nullable ITypeBinding typeBinding) {
    super(token);
    this.typeBinding = typeBinding;
  }

  @Override
  public Type symbolType() {
    return symbol().type();
  }

  @Override
  public Symbol symbol() {
    if (typeBinding == null) {
      return Symbol.UNKNOWN_SYMBOL;
    }
    return root.sema.typeSymbol(typeBinding).superSymbol;
  }

}
