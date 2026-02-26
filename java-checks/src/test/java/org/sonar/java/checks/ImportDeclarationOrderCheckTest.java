/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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
package org.sonar.java.checks;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.java.checks.helpers.JParserTestUtils;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class ImportDeclarationOrderCheckTest {
  @Test
  @DisplayName("No issues with properly ordered imports including modules")
  void no_issues_with_modules_properly_ordered() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/ImportDeclarationsOrderCheck/ImportDeclarationOrderCheckWithModulesNoIssues.java"))
      .withCheck(new ImportDeclarationOrderCheck())
      .withJavaVersion(25)
      .verifyNoIssues();
  }

  @Test
  @DisplayName("No issues without module imports - regular imports first")
  void no_issues_without_modules_and_regular_first() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/ImportDeclarationsOrderCheck/ImportDeclarationOrderCheckWithoutModulesAndRegularFirstSample.java"))
      .withCheck(new ImportDeclarationOrderCheck())
      .verifyNoIssues();
  }

  @Test
  @DisplayName("No issues without module imports - static imports first")
  void no_issues_without_modules_and_static_first() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/ImportDeclarationsOrderCheck/ImportDeclarationOrderCheckWithoutModulesAndStaticFirstSample.java"))
      .withCheck(new ImportDeclarationOrderCheck())
      .verifyNoIssues();
  }

  @Test
  @DisplayName("Quickfix for module import not first and regular imports first")
  void with_quickfixes_with_modules_and_regular_first() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/ImportDeclarationsOrderCheck/ImportDeclarationOrderCheckWithModulesAndRegularFirstSample.java"))
      .withCheck(new ImportDeclarationOrderCheck())
      .withJavaVersion(25)
      .verifyIssues();
  }

  @Test
  @DisplayName("Quickfix for module import not first and static imports first")
  void with_quickfixes_with_modules_and_static_first() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/ImportDeclarationsOrderCheck/ImportDeclarationOrderCheckWithModulesAndStaticFirstSample.java"))
      .withCheck(new ImportDeclarationOrderCheck())
      .withJavaVersion(25)
      .verifyIssues();
  }

  @ParameterizedTest(name = "[{index}] {2}")
  @MethodSource("provideImportTestCases")
  void hasProblematicImport_test(List<String> importStatements, boolean expectedResult, String testDescription) {
    List<ImportTree> imports = parseImports(importStatements);
    assertThat(ImportDeclarationOrderCheck.hasProblematicImport(imports))
      .as(testDescription)
      .isEqualTo(expectedResult);
  }

  private static Stream<Arguments> provideImportTestCases() {
    return Stream.of(
      // Empty list
      Arguments.of(
        List.of(),
        false,
        "Empty list should return false"
      ),

      // Properly ordered: module, regular, static
      Arguments.of(
        List.of(
          "import module java.base;",
          "import java.util.List;",
          "import static java.lang.Math.PI;"
        ),
        false,
        "Properly ordered imports: module, regular, static"
      ),

      // Properly ordered: module, static, regular
      Arguments.of(
        List.of(
          "import module java.base;",
          "import static java.lang.Math.PI;",
          "import java.util.List;"
        ),
        false,
        "Properly ordered imports: module, static, regular"
      ),

      // Module import after regular import
      Arguments.of(
        List.of(
          "import java.util.List;",
          "import module java.base;"
        ),
        true,
        "Module import after regular import"
      ),

      // Module import after static import
      Arguments.of(
        List.of(
          "import static java.lang.Math.PI;",
          "import module java.base;"
        ),
        true,
        "Module import after static import"
      ),

      // Ungrouped regular imports (regular -> static -> regular)
      Arguments.of(
        List.of(
          "import module java.base;",
          "import java.util.List;",
          "import static java.lang.Math.PI;",
          "import java.util.Set;"
        ),
        true,
        "Ungrouped regular imports"
      ),

      // Ungrouped static imports (static -> regular -> static)
      Arguments.of(
        List.of(
          "import module java.base;",
          "import static java.lang.Math.PI;",
          "import java.util.List;",
          "import static java.lang.System.out;"
        ),
        true,
        "Ungrouped static imports"
      ),

      // Regular after static with no more static imports (valid)
      Arguments.of(
        List.of(
          "import module java.base;",
          "import static java.lang.Math.PI;",
          "import java.util.List;"
        ),
        false,
        "Regular after static with no more static imports"
      ),

      // Static after regular with no more regular imports (valid)
      Arguments.of(
        List.of(
          "import module java.base;",
          "import java.util.List;",
          "import static java.lang.Math.PI;"
        ),
        false,
        "Static after regular with no more regular imports"
      ),

      // Multiple module imports at the beginning (valid)
      Arguments.of(
        List.of(
          "import module java.base;",
          "import module java.sql;",
          "import java.util.List;",
          "import static java.lang.Math.PI;"
        ),
        false,
        "Multiple module imports at the beginning"
      ),

      // Module import in the middle
      Arguments.of(
        List.of(
          "import java.util.List;",
          "import module java.base;",
          "import static java.lang.Math.PI;"
        ),
        true,
        "Module import in the middle"
      ),

      // Only regular imports (no module imports - should return false per line 63)
      Arguments.of(
        List.of(
          "import java.util.List;",
          "import java.util.Set;"
        ),
        false,
        "Only regular imports without modules"
      ),

      // Mixed wildcard and single-type regular imports (valid)
      Arguments.of(
        List.of(
          "import module java.base;",
          "import java.util.*;",
          "import java.util.List;",
          "import static java.lang.Math.*;"
        ),
        false,
        "Mixed wildcard and single-type regular imports"
      )
    );
  }

  private static List<ImportTree> parseImports(List<String> importStatements) {
    if (importStatements.isEmpty()) {
      return List.of();
    }

    StringBuilder code = new StringBuilder("package test;\n");
    for (String importStatement : importStatements) {
      code.append(importStatement).append("\n");
    }
    code.append("class Test {}");

    CompilationUnitTree compilationUnit = JParserTestUtils.parse(code.toString());
    return compilationUnit.imports().stream()
      .filter(importTree -> importTree.is(Tree.Kind.IMPORT))
      .map(ImportTree.class::cast)
      .toList();
  }

}
