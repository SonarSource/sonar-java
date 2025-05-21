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
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordThisTest {

  @Test
  void test_symbol_and_symbolType() {
    var ast = JParserTestUtils.parse("class A { String s = this.toString(); }");
    ClassTree cls = (ClassTree) ast.types().get(0);
    VariableTree field = (VariableTree) cls.members().get(0);
    var initializer = field.initializer();
    var memberSelect = (MemberSelectExpressionTree) ((MethodInvocationTree) initializer).methodSelect();

    // super keyword with a valid binding
    var keywordThis = (KeywordThis) memberSelect.expression();

    assertThat(keywordThis.symbol().isUnknown()).isFalse();
    assertThat(keywordThis.symbolType().isUnknown()).isFalse();
    assertThat(keywordThis.symbol().name()).isEqualTo("this");
    assertThat(keywordThis.symbolType().name()).isEqualTo("A");

    // simulate a binding problem
    keywordThis.binding = null;
    keywordThis.typeBinding = null;

    assertThat(keywordThis.symbol().isUnknown()).isTrue();
    assertThat(keywordThis.symbolType().isUnknown()).isTrue();
  }

}
