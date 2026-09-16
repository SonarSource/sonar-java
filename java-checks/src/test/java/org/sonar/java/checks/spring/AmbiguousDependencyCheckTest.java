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

import com.sonarsource.scanner.engine.sensor.test.fixtures.SensorContextTester;
import com.sonarsource.scanner.engine.sensor.test.fixtures.TestInputFileBuilder;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.SonarComponents;
import org.sonar.java.checks.verifier.TestUtils;
import org.sonar.java.model.JParser;
import org.sonar.java.model.JParserConfig;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.java.model.springcontext.BeanDefinitionGatherer;
import org.sonar.java.model.springcontext.BeanDefinitionHolder;
import org.sonar.java.model.springcontext.BeanLocation;
import org.sonar.java.model.springcontext.SpringContextModel;
import org.sonar.java.telemetry.NoOpTelemetry;
import org.sonar.java.reporting.AnalyzerMessage;
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
    assertThat(check.execute(model)).hasSize(1);
  }

  @Test
  void primary_candidate_resolves_ambiguity() {
    SpringContextModel model = buildModel("BeanNameComponent.java", "PrimaryComponent.java", "PrimaryConsumer.java");
    assertThat(check.execute(model)).isEmpty();
  }

  @Test
  void two_primary_candidates_still_raise_issue() {
    SpringContextModel model = buildModel("TwoPrimaryComponentA.java", "TwoPrimaryComponentB.java", "TwoPrimaryConsumer.java");
    assertThat(check.execute(model)).hasSize(1);
  }

  @Test
  void field_name_matching_bean_name_resolves_ambiguity() {
    SpringContextModel model = buildModel("ComponentOne.java", "ComponentTwo.java", "NameMatchConsumer.java");
    assertThat(check.execute(model)).isEmpty();
  }

  @Test
  void qualifier_resolves_ambiguity() {
    SpringContextModel model = buildModel("ComponentOne.java", "ComponentTwo.java", "QualifierConsumer.java");
    assertThat(check.execute(model)).isEmpty();
  }

  @Test
  void qualifier_on_bean_definition_resolves_ambiguity() {
    SpringContextModel model = buildModel("QualifiedBeanDefinitionConfig.java", "QualifiedBeanDefinitionConsumer.java");
    assertThat(check.execute(model)).isEmpty();
  }

  @Test
  void single_candidate_does_not_raise_issue() {
    SpringContextModel model = buildModel("ResourceLoaderComponent.java", "SingleCandidateConsumer.java");
    assertThat(check.execute(model)).isEmpty();
  }

  @Test
  void one_resolved_injection_point_does_not_hide_another_ambiguous_one_of_the_same_type() {
    SpringContextModel model = buildModel("ComponentOne.java", "ComponentTwo.java", "MixedInjectionConsumer.java");
    assertThat(check.execute(model)).hasSize(1);
  }

  // ---- @Profile -------------------------------------------------------------

  @Test
  void unprofiled_primary_resolves_ambiguity_despite_a_competing_profiled_primary() {
    SpringContextModel model = buildModel(
      "UnprofiledPrimaryComponent.java", "ProfiledPrimaryComponent.java", "PlainMessageSourceComponent.java", "MessageSourceConsumer.java");
    assertThat(check.execute(model)).isEmpty();
  }

  @Test
  void excluding_profiled_candidate_still_leaves_ambiguity_between_the_rest() {
    SpringContextModel model = buildModel(
      "PlainEventPublisherComponentA.java", "PlainEventPublisherComponentB.java", "ProfiledEventPublisherComponent.java", "EventPublisherConsumer.java");
    assertThat(check.execute(model)).hasSize(1);
  }

  @Test
  void profiled_primary_resolves_ambiguity_on_the_raw_candidate_set() {
    SpringContextModel model = buildModel(
      "ProfiledPrimaryClassLoaderComponent.java", "PlainClassLoaderComponentA.java", "PlainClassLoaderComponentB.java", "ClassLoaderConsumer.java");
    assertThat(check.execute(model)).isEmpty();
  }

  @Test
  void candidate_profiled_with_an_operator_expression_is_excluded_like_a_simply_profiled_one() {
    SpringContextModel model = buildModel(
      "PlainEnvironmentComponentA.java", "PlainEnvironmentComponentB.java", "ExpressionProfiledEnvironmentComponent.java", "EnvironmentConsumer.java");
    assertThat(check.execute(model)).hasSize(1);
  }

  @Test
  void qualifier_towards_profiled_candidate_is_not_flagged_as_ambiguous() {
    SpringContextModel model = buildModel(
      "PlainEventPublisherComponentA.java", "PlainEventPublisherComponentB.java", "ProfiledEventPublisherComponent.java",
      "EventPublisherQualifierConsumer.java");
    assertThat(check.execute(model)).isEmpty();
  }

  // ---- Multi-module context scoping -----------------------------------------

  @Test
  void beans_in_separate_modules_are_not_ambiguous_for_each_other() {
    String type = "com.example.PaymentGateway";
    InputFile fileA = dummyInputFile("com/a/PaymentGatewayImpl.java");
    InputFile fileB = dummyInputFile("com/b/PaymentGatewayImpl.java");
    InputFile consumerFileA = dummyInputFile("com/a/ConsumerA.java");
    InputFile consumerFileB = dummyInputFile("com/b/ConsumerB.java");

    SpringContextModel model = new SpringContextModel();
    registerBean(model, "paymentGatewayA", type, "module-a", "com.a", fileA);
    registerBean(model, "paymentGatewayB", type, "module-b", "com.b", fileB);
    registerBean(model, "consumerA", "com.a.ConsumerA", "module-a", "com.a", consumerFileA);
    registerBean(model, "consumerB", "com.b.ConsumerB", "module-b", "com.b", consumerFileB);
    registerInjectionPoint(model, type, "paymentGateway", "module-a", consumerFileA);
    registerInjectionPoint(model, type, "paymentGateway", "module-b", consumerFileB);

    assertThat(check.execute(model)).isEmpty();
  }

  @Test
  void within_same_module_ambiguity_is_still_detected() {
    String type = "com.example.PaymentGateway";
    InputFile fileA = dummyInputFile("com/a/PaymentGatewayImplA.java");
    InputFile fileB = dummyInputFile("com/a/PaymentGatewayImplB.java");
    InputFile consumerFile = dummyInputFile("com/a/Consumer.java");

    SpringContextModel model = new SpringContextModel();
    registerBean(model, "paymentGatewayImplA", type, "module-a", "com.a", fileA);
    registerBean(model, "paymentGatewayImplB", type, "module-a", "com.a", fileB);
    registerBean(model, "consumer", "com.a.Consumer", "module-a", "com.a", consumerFile);
    registerInjectionPoint(model, type, "paymentGateway", "module-a", consumerFile);

    assertThat(check.execute(model)).hasSize(1);
  }

  @Test
  void component_scan_cross_module_package_makes_bean_visible() {
    String type = "com.example.PaymentGateway";
    InputFile fileA = dummyInputFile("com/a/PaymentGatewayImpl.java");
    InputFile fileB = dummyInputFile("com/b/PaymentGatewayImpl.java");
    InputFile consumerFile = dummyInputFile("com/a/Consumer.java");

    SpringContextModel model = new SpringContextModel();
    registerBean(model, "paymentGatewayA", type, "module-a", "com.a", fileA);
    registerBean(model, "paymentGatewayB", type, "module-b", "com.b", fileB);
    registerBean(model, "consumer", "com.a.Consumer", "module-a", "com.a", consumerFile);
    registerInjectionPoint(model, type, "paymentGateway", "module-a", consumerFile);
    model.getProjectPackageScan().addPackages("module-a", List.of("com.a", "com.b"));

    assertThat(check.execute(model)).hasSize(1);
  }

  @Test
  void duplicate_qualifier_on_two_beans_still_raises_issue() {
    String type = "com.example.Svc";
    InputFile fileA = dummyInputFile("com/a/SvcImplA.java");
    InputFile fileB = dummyInputFile("com/a/SvcImplB.java");
    InputFile consumerFile = dummyInputFile("com/a/Consumer.java");

    SpringContextModel model = new SpringContextModel();
    registerBeanWithQualifier(model, "svcImplA", type, "module-a", "com.a", fileA, "shared");
    registerBeanWithQualifier(model, "svcImplB", type, "module-a", "com.a", fileB, "shared");
    registerBean(model, "consumer", "com.a.Consumer", "module-a", "com.a", consumerFile);
    registerInjectionPoint(model, type, "shared", "module-a", consumerFile);

    assertThat(check.execute(model)).hasSize(1);
  }

  private static void registerBean(SpringContextModel model, String beanName, String type, String module,
    String beanPackage, InputFile file) {
    registerBeanWithQualifier(model, beanName, type, module, beanPackage, file, null);
  }

  private static void registerBeanWithQualifier(SpringContextModel model, String beanName, String type, String module,
    String beanPackage, InputFile file, @Nullable String qualifier) {
    var location = new BeanLocation(file, new AnalyzerMessage.TextSpan(1));
    model.getBeanDefinitionRegistry().addBeanDefinition(beanName,
      new BeanDefinitionHolder.Builder(type, module, beanPackage, location).qualifier(qualifier).build());
    model.getTypeToBeansIndex().addBeanForType(type, beanName, module, beanPackage);
  }

  private static void registerInjectionPoint(SpringContextModel model, String type, String fieldName,
    String module, InputFile consumerFile) {
    model.getTypeToDependenciesIndex().addDependencyForType(type, fieldName, module,
      new BeanLocation(consumerFile, new AnalyzerMessage.TextSpan(5)));
  }

  private static InputFile dummyInputFile(String path) {
    return new TestInputFileBuilder("", path).setLanguage("java").setType(InputFile.Type.MAIN).build();
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

  private static SpringContextModel buildModel(List<String> filePaths) {
    List<File> classpath = TestClasspathUtils.DEFAULT_MODULE.getClassPath();
    SonarComponents sonarComponents = new SonarComponents(null, null, null, null, null, null);
    sonarComponents.setSensorContext(SensorContextTester.create(new File("")));
    SpringContextModel model = new SpringContextModel();
    sonarComponents.setSpringContextModel(model);

    BeanDefinitionGatherer gatherer = new BeanDefinitionGatherer(new NoOpTelemetry());
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
