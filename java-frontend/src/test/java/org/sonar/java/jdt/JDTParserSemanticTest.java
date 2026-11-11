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

import java.util.HashMap;
import java.util.Map;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.JParserConfig;
import org.sonar.plugins.java.api.JavaVersion;

import static org.assertj.core.api.Assertions.assertThat;

class JDTParserSemanticTest {

  @Test
  void not_need_to_skip_implicit_break_statement() {
    String source = "class C { void m() { switch (0) { case 0 -> { } } } }";
    CompilationUnit cu = createAST(source);
    TypeDeclaration c = (TypeDeclaration) cu.types().get(0);
    MethodDeclaration m = c.getMethods()[0];
    SwitchStatement s = (SwitchStatement) m.getBody().statements().get(0);
    Block block = (Block) s.statements().get(1);
    assertThat(block.statements()).isEmpty();
  }

  private CompilationUnit createAST(String source) {
    JavaVersion version = JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION;
    ASTParser astParser = ASTParser.newParser(AST.getJLSLatest());
    Map<String, String> options =  new HashMap<>(JavaCore.getOptions());
    JavaCore.setComplianceOptions(version.effectiveJavaVersionAsString(), options);
    options.put(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES, "enabled");
    astParser.setCompilerOptions(options);
    astParser.setEnvironment(
      new String[]{},
      new String[]{},
      new String[]{},
      true
    );
    astParser.setUnitName("File.java");
    astParser.setResolveBindings(true);
    astParser.setBindingsRecovery(true);
    astParser.setSource(source.toCharArray());
    return (CompilationUnit) astParser.createAST(null);
  }
}
