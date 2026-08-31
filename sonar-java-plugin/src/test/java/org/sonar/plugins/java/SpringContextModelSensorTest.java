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
package org.sonar.plugins.java;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.rule.RuleKey;
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

class SpringContextModelSensorTest {

  private static final String BASE_PATH = "checks/spring/s9352/";

  @Test
  void test_toString() {
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();
    SpringContextModelSensor sensor = new SpringContextModelSensor(new SpringContextModel());
    sensor.describe(descriptor);
    assertThat(descriptor.name()).isEqualTo("Java SpringContextModelSensor");
    assertThat(descriptor.languages()).containsExactly("java", "jsp");
  }

  @Test
  void reports_an_issue_for_an_ambiguous_dependency() {
    SensorContextTester context = SensorContextTester.create(new File(""));
    SpringContextModel model = buildModel(context, "ComponentOne.java", "ComponentTwo.java", "UnresolvedConsumer.java");

    new SpringContextModelSensor(model).execute(context);

    assertThat(context.allIssues()).hasSize(1);
    Issue issue = context.allIssues().iterator().next();
    assertThat(issue.ruleKey()).isEqualTo(RuleKey.of("java", "S9352"));
    assertThat(issue.primaryLocation().message())
      .isEqualTo("Multiple beans match this dependency"
        + " (componentOne, componentTwo); disambiguate it with \"@Qualifier\" or mark one bean as \"@Primary\".");
    assertThat(issue.primaryLocation().textRange().start().line()).isEqualTo(13);
  }

  @Test
  void reports_no_issue_when_only_one_candidate_exists() {
    SensorContextTester context = SensorContextTester.create(new File(""));
    SpringContextModel model = buildModel(context, "ResourceLoaderComponent.java", "SingleCandidateConsumer.java");

    new SpringContextModelSensor(model).execute(context);

    assertThat(context.allIssues()).isEmpty();
  }

  /**
   * Runs {@link BeanDefinitionGatherer} over the given files (relative to {@link #BASE_PATH}) into a single,
   * freshly built {@link SpringContextModel}, registering each file's {@link InputFile} on the given
   * {@link SensorContextTester} so that issues reported against it can be resolved.
   */
  private static SpringContextModel buildModel(SensorContextTester context, String... relativeFilePaths) {
    List<File> classpath = TestClasspathUtils.DEFAULT_MODULE.getClassPath();
    SonarComponents sonarComponents = new SonarComponents(null, null, null, null, null, null);
    sonarComponents.setSensorContext(context);
    SpringContextModel model = new SpringContextModel();
    sonarComponents.setSpringContextModel(model);

    BeanDefinitionGatherer gatherer = new BeanDefinitionGatherer();
    VisitorsBridge visitorsBridge = new VisitorsBridge(List.of((JavaCheck) gatherer), classpath, sonarComponents);
    for (String relativeFilePath : relativeFilePaths) {
      File file = new File(TestUtils.mainCodeSourcesPath(BASE_PATH + relativeFilePath));
      CompilationUnitTree compilationUnit = parse(file, classpath);
      InputFile inputFile = inputFile(file);
      context.fileSystem().add(inputFile);
      visitorsBridge.setCurrentFile(inputFile);
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
