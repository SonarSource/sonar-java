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
package org.sonar.java.checks.sit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonarsource.scanner.integrationtester.dsl.issue.FileIssue;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpringBeansShouldBeAccessibleCrossModuleTest extends ScannerIntegrationAbstractTest {

  private static final String RULE_KEY = "S4605";

  @TempDir
  Path sourceDir;


  @Test
  void sanity_check() throws IOException {
    Path appFile = writeSource(sourceDir, "SpringBootApp.java", """
      package com.example;

      import org.springframework.boot.autoconfigure.SpringBootApplication;

      @SpringBootApplication
      public class SpringBootApp {
      }
      """);

    Path componentFile = writeSource(sourceDir, "Component.java", """
      package com.example;

      import org.springframework.stereotype.Component;

      @Component
      public class Component {
      }
      """);

    Path otherComponentFile = writeSource(sourceDir, "OtherComponent.java", """
      package com.other;

      import org.springframework.stereotype.Component;

      @Component
      public class OtherComponent {
      }
      """);

    List<FileIssue> issues = analyze(
      List.of(RULE_KEY),
      module("same-module").withInputFiles(appFile, componentFile, otherComponentFile).withLibraries(defaultClasspath()));

    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).ruleKey()).isEqualTo("java:S4605");
    assertThat(issues.get(0).componentPath()).isEqualTo("same-module/src/OtherComponent.java");
  }

  /**
   * Module "app" has @SpringBootApplication in package "com.example.app".
   * Module "services" has a @Service in package "com.example.app.service" (sub-package of app).
   * Even though the bean is in a sub-package, it is reported as unreachable because each module
   * is analyzed independently — the @SpringBootApplication in "app" is not visible to "services".
   */
  @Test
  void springBean_in_subPackage_of_another_module_is_not_reachable_cross_module() throws IOException {
    Path appFile = writeSource(sourceDir, "SpringBootApp.java", """
      package com.example.app;

      import org.springframework.boot.autoconfigure.SpringBootApplication;

      @SpringBootApplication
      public class SpringBootApp {
      }
      """);

    Path serviceFile = writeSource(sourceDir, "MyService.java", """
      package com.example.app.service;

      import org.springframework.stereotype.Service;

      @Service
      public class MyService {
      }
      """);

    List<FileIssue> issues = analyze(
      List.of(RULE_KEY),
      module("app").withInputFiles(appFile).withLibraries(defaultClasspath()),
      module("services").withInputFiles(serviceFile).withLibraries(defaultClasspath()));

    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).ruleKey()).isEqualTo("java:S4605");
  }

  /**
   * Module "app" has @SpringBootApplication in package "com.example.app".
   * Module "external" has a @Component in package "com.other" (not a sub-package of app).
   * The bean in "external" is NOT reachable — issue expected.
   */
  @Test
  void springBean_outside_componentScan_range_raises_issue() throws IOException {
    Path appFile = writeSource(sourceDir, "SpringBootApp.java", """
      package com.example.app;

      import org.springframework.boot.autoconfigure.SpringBootApplication;

      @SpringBootApplication
      public class SpringBootApp {
      }
      """);

    Path externalFile = writeSource(sourceDir, "ExternalComponent.java", """
      package com.other;

      import org.springframework.stereotype.Component;

      @Component
      public class ExternalComponent {
      }
      """);

    List<FileIssue> issues = analyze(
      List.of(RULE_KEY),
      module("app").withInputFiles(appFile).withLibraries(defaultClasspath()),
      module("external").withInputFiles(externalFile).withLibraries(defaultClasspath()));

    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).ruleKey()).isEqualTo("java:S4605");
  }

  /**
   * Module "app" has @SpringBootApplication with explicit scanBasePackages
   * including "com.extra". Module "extra" has a @Service in that package.
   * Despite the explicit scanBasePackages, the bean is reported as unreachable because
   * each module is analyzed independently — the scanBasePackages config is not shared.
   */
  @Test
  void springBean_in_explicitly_scanned_package_of_another_module_is_not_reachable_cross_module() throws IOException {
    Path appFile = writeSource(sourceDir, "SpringBootApp.java", """
      package com.example.app;

      import org.springframework.boot.autoconfigure.SpringBootApplication;

      @SpringBootApplication(scanBasePackages = {"com.example.app", "com.extra"})
      public class SpringBootApp {
      }
      """);

    Path extraFile = writeSource(sourceDir, "ExtraService.java", """
      package com.extra;

      import org.springframework.stereotype.Service;

      @Service
      public class ExtraService {
      }
      """);

    List<FileIssue> issues = analyze(
      List.of(RULE_KEY),
      module("app").withInputFiles(appFile).withLibraries(defaultClasspath()),
      module("extra").withInputFiles(extraFile).withLibraries(defaultClasspath()));

    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).ruleKey()).isEqualTo("java:S4605");
  }

  private static Path writeSource(Path dir, String fileName, String content) throws IOException {
    Path file = dir.resolve(fileName);
    Files.writeString(file, content);
    return file;
  }
}
