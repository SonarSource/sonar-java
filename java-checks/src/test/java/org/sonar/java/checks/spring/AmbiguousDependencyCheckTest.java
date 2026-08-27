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
package org.sonar.java.checks.spring;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.java.SonarComponents;
import org.sonar.java.checks.verifier.TestUtils;
import org.sonar.java.model.JParser;
import org.sonar.java.model.JParserConfig;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.java.model.springcontext.BeanDefinitionGatherer;
import org.sonar.java.model.springcontext.SpringContextModel;
import org.sonar.java.test.classpath.TestClasspathUtils;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;

import static org.assertj.core.api.Assertions.assertThat;

class AmbiguousDependencyCheckTest {

  private static final String BASE_PATH = "checks/spring/s9352/";

  private final AmbiguousDependencyCheck check = new AmbiguousDependencyCheck();

  @Test
  void ambiguous_dependency_with_no_disambiguation_raises_issue() {
    SpringContextModel model = buildModel("ComponentOne.java", "ComponentTwo.java", "UnresolvedConsumer.java");
    assertThat(check.findAmbiguousDependencies(model)).hasSize(1);
  }

  @Test
  void primary_candidate_resolves_ambiguity() {
    SpringContextModel model = buildModel("BeanNameComponent.java", "PrimaryComponent.java", "PrimaryConsumer.java");
    assertThat(check.findAmbiguousDependencies(model)).isEmpty();
  }

  @Test
  void two_primary_candidates_still_raise_issue() {
    SpringContextModel model = buildModel("TwoPrimaryComponentA.java", "TwoPrimaryComponentB.java", "TwoPrimaryConsumer.java");
    assertThat(check.findAmbiguousDependencies(model)).hasSize(1);
  }

  @Test
  void field_name_matching_bean_name_resolves_ambiguity() {
    SpringContextModel model = buildModel("BeanFactoryComponentA.java", "BeanFactoryComponentB.java", "NameMatchConsumer.java");
    assertThat(check.findAmbiguousDependencies(model)).isEmpty();
  }

  @Test
  void qualifier_resolves_ambiguity() {
    SpringContextModel model = buildModel("EnvironmentComponentA.java", "EnvironmentComponentB.java", "QualifierConsumer.java");
    assertThat(check.findAmbiguousDependencies(model)).isEmpty();
  }

  @Test
  void single_candidate_does_not_raise_issue() {
    SpringContextModel model = buildModel("ResourceLoaderComponent.java", "SingleCandidateConsumer.java");
    assertThat(check.findAmbiguousDependencies(model)).isEmpty();
  }

  @Test
  void one_resolved_injection_point_does_not_hide_another_ambiguous_one_of_the_same_type() {
    SpringContextModel model = buildModel("MixedInjectionComponentA.java", "MixedInjectionComponentB.java", "MixedInjectionConsumer.java");
    assertThat(check.findAmbiguousDependencies(model)).hasSize(1);
  }

  @Test
  void qualifier_declared_on_the_bean_itself_resolves_ambiguity() {
    SpringContextModel model = buildModel("QualifierOnBeanComponentA.java", "QualifierOnBeanComponentB.java", "QualifierOnBeanConsumer.java");
    assertThat(check.findAmbiguousDependencies(model)).isEmpty();
  }

  @Test
  void fallback_candidate_resolves_ambiguity_when_it_is_the_sole_remaining_candidate() {
    SpringContextModel model = buildModelFromNonCompilingSources(
      "FallbackRegularComponent.java", "FallbackComponent.java", "FallbackConsumer.java");
    assertThat(check.findAmbiguousDependencies(model)).isEmpty();
  }

  @Test
  void fallback_candidate_does_not_resolve_ambiguity_with_two_other_candidates() {
    SpringContextModel model = buildModelFromNonCompilingSources(
      "FallbackTwoCandidatesComponentA.java", "FallbackTwoCandidatesComponentB.java",
      "FallbackTwoCandidatesFallbackComponent.java", "FallbackTwoCandidatesConsumer.java");
    assertThat(check.findAmbiguousDependencies(model)).hasSize(1);
  }

  /**
   * Runs {@link BeanDefinitionGatherer} over the given files (relative to {@link #BASE_PATH} under
   * {@code src/main/java}) into a single, freshly built {@link SpringContextModel}, mirroring how
   * {@code JavaSensor} drives gatherers during a real analysis, without needing java-frontend's test-only
   * scanning helpers.
   */
  private static SpringContextModel buildModel(String... relativeFilePaths) {
    return buildModel(Arrays.stream(relativeFilePaths)
      .map(relativeFilePath -> TestUtils.mainCodeSourcesPath(BASE_PATH + relativeFilePath))
      .toList());
  }

  /**
   * Same as {@link #buildModel(String...)}, but resolving files under {@code src/main/files/non-compiling}
   * instead, for fixtures relying on annotations not present on this module's classpath.
   */
  private static SpringContextModel buildModelFromNonCompilingSources(String... relativeFilePaths) {
    return buildModel(Arrays.stream(relativeFilePaths)
      .map(relativeFilePath -> TestUtils.nonCompilingTestSourcesPath(BASE_PATH + relativeFilePath))
      .toList());
  }

  private static SpringContextModel buildModel(List<String> filePaths) {
    List<File> classpath = TestClasspathUtils.DEFAULT_MODULE.getClassPath();
    SonarComponents sonarComponents = new SonarComponents(null, null, null, null, null, null);
    sonarComponents.setSensorContext(SensorContextTester.create(new File("")));
    SpringContextModel model = new SpringContextModel();
    sonarComponents.setSpringContextModel(model);

    BeanDefinitionGatherer gatherer = new BeanDefinitionGatherer();
    VisitorsBridge visitorsBridge = new VisitorsBridge(List.of((JavaCheck) gatherer), classpath, sonarComponents);
    for (String filePath : filePaths) {
      File file = new File(filePath);
      CompilationUnitTree compilationUnit = parse(file, classpath);
      visitorsBridge.setCurrentFile(inputFile(file));
      visitorsBridge.visitFile(compilationUnit, false);
    }
    visitorsBridge.endOfAnalysis();
    return model;
  }

  private static InputFile inputFile(File file) {
    try {
      return new TestInputFileBuilder("", file.getParentFile(), file)
        .setContents(Files.readString(file.toPath(), StandardCharsets.UTF_8))
        .setCharset(StandardCharsets.UTF_8)
        .setLanguage("java")
        .setType(InputFile.Type.MAIN)
        .build();
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read file '" + file.getAbsolutePath() + "'", e);
    }
  }

  private static CompilationUnitTree parse(File file, List<File> classpath) {
    String source;
    try {
      source = Files.readString(file.toPath(), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to read file '" + file.getAbsolutePath() + "'", e);
    }
    JavaVersion version = JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION;
    return JParser.parse(JParserConfig.Mode.FILE_BY_FILE.create(version, classpath).astParser(), version.toString(), file.getName(), source);
  }

}
