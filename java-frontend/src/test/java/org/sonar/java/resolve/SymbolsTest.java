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
package org.sonar.java.resolve;

import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.semantic.Type.Primitives;

import static org.assertj.core.api.Assertions.assertThat;

public class SymbolsTest {

  @Test
  public void unknown_type() {
    Type unknownType = Symbols.unknownType;

    assertThat(unknownType.isUnknown()).isTrue();
    assertThat(unknownType).isEqualTo(Symbols.unknownType);

    assertThat(unknownType.is("!Unknown!")).isFalse();
    assertThat(unknownType.isSubtypeOf("!Unknown!")).isFalse();
    assertThat(unknownType.isSubtypeOf(Symbols.unknownType)).isFalse();

    assertThat(unknownType.isArray()).isFalse();
    assertThat(unknownType.isClass()).isFalse();
    assertThat(unknownType.isVoid()).isFalse();
    assertThat(unknownType.isPrimitive()).isFalse();
    assertThat(unknownType.isPrimitive(Primitives.BOOLEAN)).isFalse();
    assertThat(unknownType.isNumerical()).isFalse();

    assertThat(unknownType.fullyQualifiedName()).isEqualTo("!Unknown!");
    assertThat(unknownType.name()).isEqualTo("!Unknown!");

    assertThat(unknownType.symbol()).isEqualTo(Symbols.unknownTypeSymbol);
    assertThat(unknownType.erasure()).isEqualTo(Symbols.unknownType);

    // since SonarJava 6.3
    assertThat(unknownType.typeArguments()).isEmpty();
    assertThat(unknownType.isParameterized()).isFalse();
  }
}
