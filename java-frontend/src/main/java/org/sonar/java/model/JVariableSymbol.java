/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.VariableTree;

final class JVariableSymbol extends JSymbol implements Symbol.VariableSymbol {

  JVariableSymbol(JSema sema) {
    super(sema, Kind.VARIABLE);
  }

  @Nullable
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
    return false;
  }

  static class ParameterPlaceholderSymbol extends Symbols.DefaultSymbol implements Symbol.VariableSymbol {
    private final String name;

    ParameterPlaceholderSymbol(int index) {
      this.name = "arg" + index;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public Symbol owner() {
      return Symbol.UNKNOWN_SYMBOL;
    }

    @Override
    public Type type() {
      return Type.UNKNOWN;
    }

    @Override
    public boolean isUnknown() {
      return true;
    }

    @Override
    public TypeSymbol enclosingClass() {
      return TypeSymbol.UNKNOWN_TYPE;
    }

    @Override
    public List<IdentifierTree> usages() {
      return Collections.emptyList();
    }

    @Nullable
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
      return Symbols.EMPTY_METADATA;
    }

    @Override
    public boolean isVariableSymbol() {
      return true;
    }

    @Override
    public boolean isFinal() {
      return false;
    }
  }

}
