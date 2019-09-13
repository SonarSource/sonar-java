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

import static org.junit.jupiter.api.Assertions.*;

class JLabelSymbolTest {

  @Test
  void test() {
    JLabelSymbol symbol = new JLabelSymbol("");
    assertAll(
      () -> assertThrows(UnsupportedOperationException.class, symbol::owner),
      () -> assertThrows(UnsupportedOperationException.class, symbol::type),
      () -> assertThrows(UnsupportedOperationException.class, symbol::isVariableSymbol),
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
      () -> assertThrows(UnsupportedOperationException.class, symbol::isUnknown),
      () -> assertThrows(UnsupportedOperationException.class, symbol::metadata),
      () -> assertThrows(UnsupportedOperationException.class, symbol::enclosingClass)
    );
  }

}
