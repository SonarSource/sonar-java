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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FrontendArchitectureTest {
  @Test
  void obsolete_classes_are_unavailable_on_the_test_classpath() throws Exception {
    for (String name : java.util.List.of("org.eclipse.jdt.core.dom.ASTParser", "org.eclipse.jdt.internal.compiler.Compiler",
      "org.sonar.java.model.PsiSema", "org.sonar.java.model.PsiType", "org.sonar.java.model.PsiSymbol",
      "org.sonar.java.model.PsiTypeSymbol", "org.sonar.java.model.PsiMethodSymbol", "org.sonar.java.model.PsiVariableSymbol",
      "org.sonar.java.model.PsiPackageSymbol", "org.sonar.java.model.PsiSymbolMetadata", "org.sonar.java.model.PsiTreeConverter",
      "org.sonar.java.model.PsiFrontendParser")) {
      assertThatThrownBy(() -> Class.forName(name)).isInstanceOf(ClassNotFoundException.class);
    }
    for (String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
      var path = Path.of(entry);
      if (entry.endsWith(".jar")) {
        try (var jar = new JarFile(path.toFile())) {
          assertThat(jar.stream().map(java.util.jar.JarEntry::getName).filter(name -> name.endsWith(".class")))
            .as(entry).noneMatch(FrontendArchitectureTest::isCompilerClass);
        }
      } else if (Files.isDirectory(path)) {
        try (var files = Files.walk(path)) {
          assertThat(files.filter(file -> file.toString().endsWith(".class")).map(file -> path.relativize(file).toString().replace('\\', '/')))
            .as(entry).noneMatch(FrontendArchitectureTest::isCompilerClass);
        }
      }
    }
  }

  @Test
  void frontend_sources_have_no_compiler_imports() throws Exception {
    try (var files = Files.walk(Path.of("src"))) {
      for (var path : files.filter(file -> file.toString().endsWith(".java")).toList()) {
        assertThat(Files.readAllLines(path).stream().filter(line -> line.startsWith("import ")))
          .as(path.toString()).noneMatch(line -> line.contains("org.eclipse.jdt.") && !line.contains("org.eclipse.jdt.annotation."));
      }
    }
  }

  @Test
  void unresolved_lookup_recovers_without_a_compiler_binding() {
    try (var parsed = FrontendParser.parse(JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION, java.util.List.of(), "C.java", "class C {}")) {
      assertThat(parsed.tree().sema).isInstanceOf(JSema.class);
      var string = parsed.tree().sema.getClassType("java.lang.String");
      assertThat(string).isInstanceOf(JType.class);
      assertThat(string.symbol()).isInstanceOf(JTypeSymbol.class);
      assertThat(org.sonar.java.psi.JavaPsiEnvironment.class.getProtectionDomain().getCodeSource().getLocation())
        .isEqualTo(JSema.class.getProtectionDomain().getCodeSource().getLocation());
      assertThat(parsed.tree().sema.getClassType("missing.Unknown").isUnknown()).isTrue();
      assertThat(parsed.tree().sema.getClassType("")).isSameAs(org.sonar.plugins.java.api.semantic.Type.UNKNOWN);
    }
  }

  private static boolean isCompilerClass(String name) {
    return name.contains("org/eclipse/jdt/") && !name.contains("org/eclipse/jdt/annotation/");
  }
}
