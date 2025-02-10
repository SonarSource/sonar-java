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
package org.sonar.java.checks.helpers;

import org.junit.jupiter.api.Test;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;

import static org.assertj.core.api.Assertions.assertThat;

class SpringUtilsTest {

  @Test
  void isAutowired() {
    var cu = JParserTestUtils.parse("""
      class A {
        @org.springframework.beans.factory.annotation.Autowired
        Object o;
        
        @Autowired
        Object goo;
      }
      """);
    var clazz = (ClassTreeImpl) cu.types().get(0);
    var obj = (VariableTreeImpl) clazz.members().get(0);
    assertThat(SpringUtils.isAutowired(obj.symbol())).isTrue();
    var goo = (VariableTreeImpl) clazz.members().get(1);
    assertThat(SpringUtils.isAutowired(goo.symbol())).isFalse();
  }

}
