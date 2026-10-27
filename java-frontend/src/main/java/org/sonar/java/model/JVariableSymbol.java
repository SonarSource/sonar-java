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

  private final boolean parameter;
  private final boolean local;
  private boolean effectivelyFinal;
  private Optional<Object> constant = Optional.empty();

  JVariableSymbol(JSema sema, @Nullable String id, String name, String dumpKind) {
    super(sema, Kind.VARIABLE, id, name);
    this.parameter = "param".equals(dumpKind);
    this.local = "local".equals(dumpKind);
    this.effectivelyFinal = "constant".equals(dumpKind) || "enum_member".equals(dumpKind);
    enumSymbol("enum_member".equals(dumpKind));
  }

  void effectivelyFinal(boolean effectivelyFinal) {
    this.effectivelyFinal = effectivelyFinal;
  }

  void constantValue(@Nullable Object value) {
    this.constant = Optional.ofNullable(value);
  }

  @Nullable
  @Override
  public VariableTree declaration() {
    return (VariableTree) super.declaration();
  }

  @Override
  public boolean isEffectivelyFinal() {
    return isFinal() || effectivelyFinal;
  }

  @Override
  public Optional<Object> constantValue() {
    return constant;
  }

  @Override
  public boolean isLocalVariable() {
    return local || (owner().isMethodSymbol() && !parameter);
  }

  @Override
  public boolean isParameter() {
    return parameter;
  }

  static class ParameterPlaceholderSymbol extends Symbols.DefaultSymbol implements Symbol.VariableSymbol {

    private final String name;
    private final Symbol owner;
    private final Type type;

    ParameterPlaceholderSymbol(int index, Symbol owner, Type type) {
      this.name = "arg" + index;
      this.owner = owner;
      this.type = type == null ? Type.UNKNOWN : type;
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

    @Nullable
    @Override
    public VariableTree declaration() {
      return null;
    }

    @Override
    public boolean isEffectivelyFinal() {
      return true;
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
