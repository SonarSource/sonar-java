/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sonar.java.SonarComponents;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.checks.verifier.TestUtils;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.testing.VisitorsBridgeForTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ParsingErrorCheckTest {

  @Test
  void test() {
    SonarComponents sonarComponents = mock(SonarComponents.class);
    when(sonarComponents.inputFileContents(any())).thenCallRealMethod();

    VisitorsBridgeForTests visitorsBridge = new VisitorsBridgeForTests(new ParsingErrorCheck(), sonarComponents);
    JavaAstScanner.scanSingleFileForTests(TestUtils.inputFile("src/test/files/checks/parsing/ParsingError.java"), visitorsBridge);
    Set<AnalyzerMessage> issues = visitorsBridge.lastCreatedTestContext().getIssues();
    assertThat(issues).hasSize(1);
    AnalyzerMessage issue = issues.iterator().next();
    assertThat(issue.getLine()).isEqualTo(1);
    assertThat(issue.getMessage()).isEqualTo("Parse error");
  }

  /**
   * Related to SONARJAVA-3486: discrepancies between JLS, javac (more tolerant than JLS) and ECJ (strict to JLS).
   * According to JLS (§7.3 Compilation Units), empty statements are not allowed in import section.
   */
  @Test
  void javacEmptyStatementsInImportsBug() {
    SonarComponents sonarComponents = mock(SonarComponents.class);
    when(sonarComponents.inputFileContents(any())).thenCallRealMethod();

    VisitorsBridgeForTests visitorsBridge = new VisitorsBridgeForTests(new ParsingErrorCheck(), sonarComponents);
    JavaAstScanner.scanSingleFileForTests(TestUtils.inputFile("src/test/files/checks/parsing/EmptyStatementsInImportsBug.java"), visitorsBridge);
    Set<AnalyzerMessage> issues = visitorsBridge.lastCreatedTestContext().getIssues();
    assertThat(issues).hasSize(1);
    AnalyzerMessage issue = issues.iterator().next();
    assertThat(issue.getLine()).isEqualTo(3);
    assertThat(issue.getMessage()).isEqualTo("Parse error");
  }
}
