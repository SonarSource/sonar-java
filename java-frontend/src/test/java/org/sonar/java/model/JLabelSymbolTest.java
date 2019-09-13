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

import org.junit.jupiter.api.Test;
import org.sonar.java.resolve.Symbols;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class JLabelSymbolTest {

  @Test
  void test() {
    JLabelSymbol symbol = new JLabelSymbol("");
    assertAll(
      () -> assertThat(symbol.owner()).isNull(), // see e.g. InnerStaticClassesCheck
      () -> assertThat(symbol.type()).isSameAs(Symbols.unknownType), // see e.g. MethodIdenticalImplementationsCheck
      () -> assertThat(symbol.isVariableSymbol()).isFalse(), // see e.g. LombokFilter
      () -> assertThrows(UnsupportedOperationException.class, symbol::isTypeSymbol),
      () -> assertThrows(UnsupportedOperationException.class, symbol::isMethodSymbol),
      () -> assertThrows(UnsupportedOperationException.class, symbol::isPackageSymbol),
      () -> assertThrows(UnsupportedOperationException.class, symbol::isStatic),
      () -> assertThrows(UnsupportedOperationException.class, symbol::isFinal),
      () -> assertThrows(UnsupportedOperationException.class, symbol::isEnum),
      () -> assertThrows(UnsupportedOperationException.class, symbol::isInterface),
      () -> assertThrows(UnsupportedOperationException.class, symbol::isAbstract),
      () -> assertThrows(UnsupportedOperationException.class, symbol::isPublic),
      () -> assertThrows(UnsupportedOperationException.class, symbol::isPrivate),
      () -> assertThrows(UnsupportedOperationException.class, symbol::isProtected),
      () -> assertThrows(UnsupportedOperationException.class, symbol::isPackageVisibility),
      () -> assertThrows(UnsupportedOperationException.class, symbol::isDeprecated),
      () -> assertThrows(UnsupportedOperationException.class, symbol::isVolatile),
      () -> assertThat(symbol.isUnknown()).isFalse(), // see e.g. StaticMethodCheck
      () -> assertThat(symbol.metadata()).isNotNull(),
      () -> assertThat(symbol.enclosingClass()).isNull() // see e.g. StandardInputReadCheck
    );
  }

}
