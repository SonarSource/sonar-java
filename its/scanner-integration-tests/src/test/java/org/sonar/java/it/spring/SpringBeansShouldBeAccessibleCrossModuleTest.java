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
package org.sonar.java.it.spring;

import com.sonarsource.scanner.integrationtester.dsl.issue.TextRange;
import com.sonarsource.scanner.integrationtester.dsl.issue.TextRangeIssue;
import org.junit.jupiter.api.Test;
import org.sonar.java.it.ScannerIntegrationAbstractTest;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SpringBeansShouldBeAccessibleCrossModuleTest extends ScannerIntegrationAbstractTest {

  @Test
  void test() {
    var issues = analyze(Path.of("spring-beans-should-be-accessible"), "S4605", "S1874");
    assertThat(issues)
      .hasSize(3)
      .contains(new TextRangeIssue(
        "moduleA/src/main/java/packageB/MyComponentB.java",
        "java:S4605",
        "'MyComponentB' is not reachable by @ComponentScan or @SpringBootApplication. Either move it to a package configured in @ComponentScan or update your @ComponentScan configuration.",
        new TextRange(6, 6, 13, 25)));
  }
}
