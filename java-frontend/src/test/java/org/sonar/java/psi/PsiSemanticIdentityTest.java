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
package org.sonar.java.psi;

import org.junit.jupiter.api.Test;
import org.sonar.java.model.JParserConfig;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;

import static org.assertj.core.api.Assertions.assertThat;

class PsiSemanticIdentityTest {
  private final java.util.List<PsiSemanticTestSupport> fixtures = new java.util.ArrayList<>();

  @org.junit.jupiter.api.AfterEach
  void close() {
    fixtures.forEach(PsiSemanticTestSupport::close);
  }

  @Test
  void generic_invocations_preserve_declaration_and_instantiation_identity() {
    var tree = parse("class A { <T> T id(T t) { return t; } String a() { return id(\"\"); } Integer b() { return id(1); } }");
    var type = (ClassTree) tree.types().get(0);
    var first = invocation((MethodTree) type.members().get(1));
    var second = invocation((MethodTree) type.members().get(2));
    assertThat(first.methodSymbol()).isSameAs(first.methodSymbol()).isNotEqualTo(second.methodSymbol());
    assertThat(first.methodSymbol().declaration()).isSameAs(second.methodSymbol().declaration());
    assertThat(first.methodSymbol().parameterTypes().get(0).fullyQualifiedName()).isEqualTo("java.lang.String");
    assertThat(second.methodSymbol().parameterTypes().get(0).fullyQualifiedName()).isEqualTo("java.lang.Integer");
    assertThat(first.symbolType()).isSameAs(first.symbolType());
  }

  private JavaTree.CompilationUnitTreeImpl parse(String source) {
    var fixture = PsiSemanticTestSupport.open("File.java", source, JParserTestUtils.DEFAULT_CLASSPATH, JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION);
    fixtures.add(fixture);
    return fixture.tree();
  }

  private static MethodInvocationTree invocation(MethodTree method) {
    return (MethodInvocationTree) ((ReturnStatementTree) method.block().body().get(0)).expression();
  }
}
