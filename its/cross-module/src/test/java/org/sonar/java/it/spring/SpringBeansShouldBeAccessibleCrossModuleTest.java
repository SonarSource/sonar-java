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

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.sonar.java.it.ScannerIntegrationAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

class SpringBeansShouldBeAccessibleCrossModuleTest extends ScannerIntegrationAbstractTest {

  @Test
  void test() {
    var issues = analyze(Path.of("spring-beans-should-be-accessible"));
    assertThat(issues).hasSize(2);
    assertThat(issues).anySatisfy(
      issue -> {
        assertThat(issue.ruleKey()).isEqualTo("java:S4605");
        assertThat(issue.componentPath()).isEqualTo("moduleA/packageB/MyComponentB.java");
      }
    );
  }
}
