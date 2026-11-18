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
package org.sonar.java.jdt;

import java.util.Objects;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.java.testing.ThreadLocalLogTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.sonar.java.model.assertions.TypeAssert.assertThat;

class JDTTypeTest {

  @RegisterExtension
  public ThreadLocalLogTester logTester = new ThreadLocalLogTester().setLevel(Level.DEBUG);

  @Test
  void primitiveWrapperType_returns_null_when_semantic_cannot_resolve_wrapper_type() {
    // Sema that can resolve primitives, but fails to resolve wrapper types, which can happen when the classpath is not correctly set up
    var brokenSema = spy(sema);
    when(brokenSema.resolveType("java.lang.Byte")).thenReturn(null);

    var primitiveBinding = sema.resolveType("byte");
    var primitiveType = new JDTType(brokenSema, primitiveBinding);

    assertThat(primitiveType.primitiveWrapperType()).isNull();
  }

  @Test
  void primitiveType_returns_null_when_semantic_cannot_resolve_primitive_type() {
    // Sema that can resolve wrapper types, but fails to resolve a primitive, this should never happen in practice,
    // but we want to make sure it doesn't throw an exception and returns null instead
    var brokenSema = spy(sema);
    when(brokenSema.resolveType("byte")).thenReturn(null);

    var wrapperBinding = sema.resolveType("java.lang.Byte");
    var wrapperType = new JDTType(brokenSema, wrapperBinding);

    assertThat(wrapperType.primitiveType()).isNull();
  }

  @Test
  void is_subtype_of_should_not_throw_NPE() {
    JDTType objectType = type("java.lang.Object");
    ITypeBinding brokenStringBinding = spy(Objects.requireNonNull(sema.resolveType("java.lang.String")));
    // simulate the NullPointerException described in SONARJAVA-4390
    when(brokenStringBinding.isSubTypeCompatible(any()))
      .thenThrow(new NullPointerException("test NPE"));

    JDTType stringType = new JDTType(sema, brokenStringBinding);
    // should catch the NullPointerException
    assertThat(stringType.isSubtypeOf(objectType)).isFalse();

    assertThat(logTester.logs(Level.DEBUG))
      .containsExactly("NullPointerException while resolving isSubTypeCompatible()");
  }

  private JDTType type(String name) {
    ITypeBinding typeBinding = Objects.requireNonNull(sema.resolveType(name));
    return sema.type(typeBinding);
  }

  private JDTSema sema;

  @BeforeEach
  void setup() {
    ASTParser astParser = ASTParser.newParser(AST.getJLSLatest());
    astParser.setEnvironment(
      new String[]{},
      new String[]{},
      new String[]{},
      true
    );
    astParser.setResolveBindings(true);
    astParser.setUnitName("File.java");
    astParser.setSource("".toCharArray());
    AST ast = astParser.createAST(null).getAST();
    sema = new JDTSema(ast);
  }
}
