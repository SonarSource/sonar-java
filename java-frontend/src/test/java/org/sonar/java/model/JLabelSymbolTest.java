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

import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.semantic.Type;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class JLabelSymbolTest {

  @Test
  void test() {
    JLabelSymbol symbol = new JLabelSymbol("");
    assertAll(
      () -> assertThat(symbol.owner()).isNull(), // see e.g. InnerStaticClassesCheck
      () -> assertThat(symbol.type()).isSameAs(Type.UNKNOWN), // see e.g. MethodIdenticalImplementationsCheck
      () -> assertThat(symbol.isVariableSymbol()).isFalse(), // see e.g. LombokFilter
      () -> assertThat(symbol.isTypeSymbol()).isFalse(),
      () -> assertThat(symbol.isMethodSymbol()).isFalse(),
      () -> assertThat(symbol.isPackageSymbol()).isFalse(),
      () -> assertThat(symbol.isStatic()).isFalse(),
      () -> assertThat(symbol.isFinal()).isFalse(),
      () -> assertThat(symbol.isEnum()).isFalse(),
      () -> assertThat(symbol.isInterface()).isFalse(),
      () -> assertThat(symbol.isAbstract()).isFalse(),
      () -> assertThat(symbol.isPublic()).isFalse(),
      () -> assertThat(symbol.isPrivate()).isFalse(),
      () -> assertThat(symbol.isProtected()).isFalse(),
      () -> assertThat(symbol.isPackageVisibility()).isFalse(),
      () -> assertThat(symbol.isDeprecated()).isFalse(), // see e.g. CallToDeprecatedMethodCheck
      () -> assertThat(symbol.isVolatile()).isFalse(),
      () -> assertThat(symbol.isUnknown()).isFalse(), // see e.g. StaticMethodCheck
      () -> assertThat(symbol.metadata()).isNotNull(),
      () -> assertThat(symbol.enclosingClass()).isNull() // see e.g. StandardInputReadCheck
    );
  }

}
