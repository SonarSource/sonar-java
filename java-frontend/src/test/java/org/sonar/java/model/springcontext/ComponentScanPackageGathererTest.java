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
package org.sonar.java.model.springcontext;

import java.io.File;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.java.SonarComponents;
import org.sonar.java.TestUtils;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.java.test.classpath.TestClasspathUtils;
import org.sonar.plugins.java.api.JavaCheck;

import static org.assertj.core.api.Assertions.assertThat;

class ComponentScanPackageGathererTest {

  private static final String MODULE_KEY = "";

  private SpringContextModel model;
  private ComponentScanPackageGatherer gatherer;

  @BeforeEach
  void setUp() {
    gatherer = new ComponentScanPackageGatherer();
    model = new SpringContextModel();
  }

  // ---- @ComponentScan -------------------------------------------------------

  @Test
  void componentScan_value_and_basePackages_attributes_are_collected() {
    scan("src/test/files/springcontext/ComponentScanWithBasePackages.java");

    assertThat(model.getProjectPackageScan().getPackagesForModule(MODULE_KEY))
      .containsExactlyInAnyOrder(
        "com.example.service",
        "com.example.repository",
        "com.example.controller",
        "com.example.domain",
        "com.example.single",
        "checks.spring.context" // basePackageClasses = PackageMarker.class → its package
      );
  }

  // ---- @SpringBootApplication -----------------------------------------------

  @Test
  void springBootApplication_without_scan_attributes_collects_own_package() {
    scan("src/test/files/springcontext/SpringBootAppNoScanAttributes.java");

    assertThat(model.getProjectPackageScan().getPackagesForModule(MODULE_KEY))
      .containsExactly("checks.spring.context");
  }

  @Test
  void springBootApplication_scanBasePackages_are_collected() {
    scan("src/test/files/springcontext/SpringBootAppWithScanBasePackages.java");

    assertThat(model.getProjectPackageScan().getPackagesForModule(MODULE_KEY))
      .containsExactlyInAnyOrder("com.example.service", "com.example.web");
  }

  @Test
  void springBootApplication_scanBasePackageClasses_resolves_to_class_package() {
    scan("src/test/files/springcontext/SpringBootAppWithScanBasePackageClasses.java");

    assertThat(model.getProjectPackageScan().getPackagesForModule(MODULE_KEY))
      .containsExactly("checks.spring.context");
  }

  @Test
  void componentScan_with_packages_overrides_springBootApplication_default_own_package() {
    scan("src/test/files/springcontext/SpringBootAppWithComponentScanPackages.java");

    assertThat(model.getProjectPackageScan().getPackagesForModule(MODULE_KEY))
      .containsExactlyInAnyOrder("com.example.service", "com.example.web");
  }

  // ---- DependencyVersionAware -----------------------------------------------

  @Test
  void gatherer_is_skipped_when_spring_context_is_not_in_classpath() {
    scan(List.of(), "src/test/files/springcontext/SpringBootAppNoScanAttributes.java");

    assertThat(model.getProjectPackageScan().getModules()).isEmpty();
  }

  // ---- No annotations -------------------------------------------------------

  @Test
  void no_scan_annotations_collects_nothing() {
    scan("src/test/files/springcontext/NoScanAnnotations.java");

    assertThat(model.getProjectPackageScan().getPackagesForModule(MODULE_KEY)).isEmpty();
  }

  // ---- Multiple files -------------------------------------------------------

  @Test
  void packages_from_multiple_files_are_merged() {
    scan(
      "src/test/files/springcontext/SpringBootAppNoScanAttributes.java",
      "src/test/files/springcontext/SpringBootAppWithScanBasePackages.java"
    );

    assertThat(model.getProjectPackageScan().getPackagesForModule(MODULE_KEY))
      .containsExactlyInAnyOrder("checks.spring.context", "com.example.service", "com.example.web");
  }

  // ---- Helpers --------------------------------------------------------------

  private void scan(String... filePaths) {
    scan(TestClasspathUtils.DEFAULT_MODULE.getClassPath(), filePaths);
  }

  private void scan(List<File> classpath, String... filePaths) {
    SensorContextTester sensorContextTester = SensorContextTester.create(new File(""));
    var sonarComponents = new SonarComponents(null, null, null, null, null, null);
    sonarComponents.setSensorContext(sensorContextTester);
    sonarComponents.setSpringContextModel(model);

    VisitorsBridge visitorsBridge = new VisitorsBridge(List.of((JavaCheck) gatherer), classpath, sonarComponents);
    for (String filePath : filePaths) {
      File file = new File(filePath);
      var compilationUnit = JParserTestUtils.parse(file, classpath);
      visitorsBridge.setCurrentFile(TestUtils.inputFile(file));
      visitorsBridge.visitFile(compilationUnit, false);
    }
    visitorsBridge.endOfAnalysis();
  }
}