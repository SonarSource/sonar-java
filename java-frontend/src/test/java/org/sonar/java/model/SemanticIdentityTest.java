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

import org.junit.jupiter.api.Test;
import org.sonar.java.test.TestResourceScope;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;

import static org.assertj.core.api.Assertions.assertThat;

@TestResourceScope.Scoped
class SemanticIdentityTest {
  @Test
  void generic_invocations_preserve_declaration_and_instantiation_identity() {
    var tree = JParserTestUtils.parse("class A { <T> T id(T t) { return t; } String a() { return id(\"\"); } Integer b() { return id(1); } }");
    var type = (ClassTree) tree.types().get(0);
    var first = invocation((MethodTree) type.members().get(1));
    var second = invocation((MethodTree) type.members().get(2));
    assertThat(first.methodSymbol()).isSameAs(first.methodSymbol()).isNotEqualTo(second.methodSymbol());
    assertThat(first.methodSymbol().declaration()).isSameAs(second.methodSymbol().declaration());
    assertThat(first.methodSymbol().parameterTypes().get(0).fullyQualifiedName()).isEqualTo("java.lang.String");
    assertThat(second.methodSymbol().parameterTypes().get(0).fullyQualifiedName()).isEqualTo("java.lang.Integer");
    assertThat(first.symbolType()).isSameAs(first.symbolType());
  }

  private static MethodInvocationTree invocation(MethodTree method) {
    return (MethodInvocationTree) ((ReturnStatementTree) method.block().body().get(0)).expression();
  }
}
