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
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.JParser;

import static org.assertj.core.api.Assertions.assertThat;

class JDTSemaTest {

  @Test
  void type() {
    ITypeBinding typeBinding = Objects.requireNonNull(sema.resolveType("int"));
    assertThat(sema.type(typeBinding))
      .isNotNull()
      .isSameAs(sema.type(typeBinding));
  }

  private JDTSema sema;

  @BeforeEach
  void setup() {
    ASTParser astParser = ASTParser.newParser(AST.getJLSLatest());
    String version = JParser.MAXIMUM_SUPPORTED_JAVA_VERSION.effectiveJavaVersionAsString();
    JavaCore.setComplianceOptions(version, JavaCore.getOptions());
    astParser.setEnvironment(
      new String[]{"target/test-classes"},
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
