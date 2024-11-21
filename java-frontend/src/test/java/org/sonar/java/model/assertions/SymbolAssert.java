/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.model.assertions;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;

public class SymbolAssert extends AbstractAssert<SymbolAssert, Symbol> {

  public SymbolAssert(Symbol actual) {
    super(actual, SymbolAssert.class);
  }

  public static SymbolAssert assertThat(Symbol actual) {
    return new SymbolAssert(actual);
  }

  public SymbolAssert hasName(String name) {
    Assertions.assertThat(actual.name()).isEqualTo(name);
    return this;
  }

  public SymbolAssert hasSameNameAs(Symbol other) {
    return hasName(other.name());
  }

  public SymbolAssert hasOwner(Symbol owner) {
    Assertions.assertThat(actual.owner()).isSameAs(owner);
    return this;
  }

  public SymbolAssert hasEnclosingClass(Symbol.TypeSymbol enclosingClass) {
    Assertions.assertThat(actual.enclosingClass()).isSameAs(enclosingClass);
    return this;
  }

  public SymbolAssert isUnknown() {
    Assertions.assertThat(actual.isUnknown()).isTrue();
    return this;
  }

  public SymbolAssert isOfType(Type type) {
    TypeAssert.assertThat(actual.type()).is(type);
    return this;
  }

  public SymbolAssert isOfType(String fullyQualifiedName) {
    TypeAssert.assertThat(actual.type()).is(fullyQualifiedName);
    return this;
  }

  public SymbolAssert isHavingNullType() {
    TypeAssert.assertThat(actual.type()).isNull();
    return this;
  }

  public SymbolAssert isOfUnknownType() {
    TypeAssert.assertThat(actual.type()).isUnknown();
    return this;
  }

  public SymbolAssert isOfSameTypeAs(Symbol symbol) {
    TypeAssert.assertThat(actual.type()).is(symbol.type());
    return this;
  }

}
