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
import org.sonar.java.model.springcontext.ProfileExpression;
import org.sonar.java.model.springcontext.SpringContextModel;
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
  void competing_primary_candidates_are_ambiguous_when_the_profiled_one_is_active() {
    SpringContextModel model = buildModel(
      "UnprofiledPrimaryComponent.java", "ProfiledPrimaryComponent.java", "PlainMessageSourceComponent.java", "MessageSourceConsumer.java");
    assertThat(check.execute(model)).hasSize(1);
  }

  @Test
  void profiled_candidate_is_considered_with_unprofiled_candidates() {
    SpringContextModel model = buildModel(
      "PlainEventPublisherComponentA.java", "PlainEventPublisherComponentB.java", "ProfiledEventPublisherComponent.java", "EventPublisherConsumer.java");
    assertThat(check.execute(model)).hasSize(1);
  }

  @Test
  void profiled_primary_does_not_resolve_ambiguity_when_its_profile_is_inactive() {
    SpringContextModel model = buildModel(
      "ProfiledPrimaryClassLoaderComponent.java", "PlainClassLoaderComponentA.java", "PlainClassLoaderComponentB.java", "ClassLoaderConsumer.java");
    assertThat(check.execute(model)).hasSize(1);
  }

  @Test
  void candidate_profiled_with_an_operator_expression_is_evaluated_with_unprofiled_candidates() {
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

  @Test
  void candidates_with_the_same_profile_are_ambiguous() {
    SpringContextModel model = modelWithCandidates(
      candidate("first", ProfileExpression.profile("dev")),
      candidate("second", ProfileExpression.profile("dev")));

    assertThat(check.execute(model)).singleElement().satisfies(issue -> assertThat(issue.message())
      .isEqualTo("Multiple beans match this dependency (first, second); disambiguate it with \"@Qualifier\" or mark one bean as \"@Primary\"."));
  }

  @Test
  void mutually_exclusive_profile_expressions_are_not_ambiguous() {
    SpringContextModel model = modelWithCandidates(
      candidate("enabledInDev", ProfileExpression.profile("dev")),
      candidate("disabledInDev", ProfileExpression.not(ProfileExpression.profile("dev"))));

    assertThat(check.execute(model)).isEmpty();
  }

  @Test
  void different_profiles_can_be_active_together() {
    SpringContextModel model = modelWithCandidates(
      candidate("devCandidate", ProfileExpression.profile("dev")),
      candidate("prodCandidate", ProfileExpression.profile("prod")));

    assertThat(check.execute(model)).hasSize(1);
  }

  @Test
  void mutually_exclusive_compound_profile_expressions_are_not_ambiguous() {
    SpringContextModel model = modelWithCandidates(
      candidate("notTest", ProfileExpression.and(List.of(
        ProfileExpression.profile("dev"), ProfileExpression.not(ProfileExpression.profile("test"))))),
      candidate("test", ProfileExpression.profile("test")));

    assertThat(check.execute(model)).isEmpty();
  }

  @Test
  void overlapping_compound_profile_expressions_are_ambiguous() {
    SpringContextModel model = modelWithCandidates(
      candidate("devWithoutTest", ProfileExpression.and(List.of(
        ProfileExpression.profile("dev"), ProfileExpression.not(ProfileExpression.profile("test"))))),
      candidate("devOrTest", ProfileExpression.or(List.of(
        ProfileExpression.profile("dev"), ProfileExpression.profile("test")))));

    assertThat(check.execute(model)).hasSize(1);
  }

  @Test
  void unconditional_candidate_is_ambiguous_with_a_profiled_candidate_when_that_profile_is_active() {
    SpringContextModel model = modelWithCandidates(
      candidate("always", ProfileExpression.UNCONDITIONAL),
      candidate("dev", ProfileExpression.profile("dev")));

    assertThat(check.execute(model)).hasSize(1);
  }

  @Test
  void unknown_profile_expression_is_conservatively_considered_active() {
    SpringContextModel model = modelWithCandidates(
      candidate("always", ProfileExpression.UNCONDITIONAL),
      candidate("unknown", ProfileExpression.UNKNOWN));

    assertThat(check.execute(model)).hasSize(1);
  }

  @Test
  void primary_candidate_only_resolves_ambiguity_in_profiles_where_it_is_active() {
    SpringContextModel model = modelWithCandidates(
      candidate("first", ProfileExpression.UNCONDITIONAL),
      candidate("second", ProfileExpression.UNCONDITIONAL),
      primaryCandidate("primary", ProfileExpression.profile("prod")));

    assertThat(check.execute(model)).hasSize(1);
  }

  @Test
  void active_primary_candidate_resolves_ambiguity() {
    SpringContextModel model = modelWithCandidates(
      candidate("regular", ProfileExpression.profile("prod")),
      primaryCandidate("primary", ProfileExpression.profile("prod")));

    assertThat(check.execute(model)).isEmpty();
  }

  @Test
  void issue_message_only_lists_candidates_active_in_the_same_profile_configuration() {
    SpringContextModel model = modelWithCandidates(
      candidate("always", ProfileExpression.UNCONDITIONAL),
      candidate("enabledInDev", ProfileExpression.profile("dev")),
      candidate("disabledInDev", ProfileExpression.not(ProfileExpression.profile("dev"))));

    assertThat(check.execute(model)).singleElement().satisfies(issue -> assertThat(issue.message())
      .contains("(always, disabledInDev)"));
  }

  @Test
  void twelve_profile_names_are_enumerated() {
    SpringContextModel model = ambiguousModelWithProfileNames(12);

    assertThat(check.execute(model)).hasSize(1);
  }

  @Test
  void more_than_twelve_profile_names_are_not_enumerated() {
    SpringContextModel model = ambiguousModelWithProfileNames(13);

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
  void same_named_bean_in_another_module_does_not_affect_profile_activation() {
    String type = "com.example.ProfiledService";
    SpringContextModel model = new SpringContextModel();
    registerBean(model, "service", type, "module-a", "com.a", dummyInputFile("com/a/Service.java"),
      ProfileExpression.profile("dev"), false);
    registerBean(model, "otherService", type, "module-a", "com.a", dummyInputFile("com/a/OtherService.java"),
      ProfileExpression.not(ProfileExpression.profile("dev")), false);
    registerBean(model, "service", type, "module-b", "com.b", dummyInputFile("com/b/Service.java"),
      ProfileExpression.profile("prod"), false);
    registerInjectionPoint(model, type, "dependency", "module-a", dummyInputFile("com/a/Consumer.java"));

    assertThat(check.execute(model)).isEmpty();
  }

  @Test
  void same_named_primary_bean_in_another_module_does_not_resolve_ambiguity() {
    String type = "com.example.Service";
    SpringContextModel model = new SpringContextModel();
    registerBean(model, "service", type, "module-a", "com.a", dummyInputFile("com/a/Service.java"));
    registerBean(model, "otherService", type, "module-a", "com.a", dummyInputFile("com/a/OtherService.java"));
    registerBean(model, "service", type, "module-b", "com.b", dummyInputFile("com/b/Service.java"),
      ProfileExpression.UNCONDITIONAL, true);
    registerInjectionPoint(model, type, "dependency", "module-a", dummyInputFile("com/a/Consumer.java"));

    assertThat(check.execute(model)).hasSize(1);
  }

  private static void registerBean(SpringContextModel model, String beanName, String type, String module,
    String beanPackage, InputFile file) {
    registerBean(model, beanName, type, module, beanPackage, file, ProfileExpression.UNCONDITIONAL, false);
  }

  private static void registerBean(SpringContextModel model, String beanName, String type, String module,
    String beanPackage, InputFile file, ProfileExpression profileExpression, boolean primary) {
    var location = new BeanLocation(file, new AnalyzerMessage.TextSpan(1));
    var builder = new BeanDefinitionHolder.Builder(type, module, beanPackage, location)
      .profileExpression(profileExpression);
    if (primary) {
      builder.primary();
    }
    model.getBeanDefinitionRegistry().addBeanDefinition(beanName, builder.build());
    model.getTypeToBeansIndex().addBeanForType(type, beanName, module, beanPackage);
  }

  private static SpringContextModel modelWithCandidates(Candidate... candidates) {
    String type = "com.example.ProfiledService";
    String module = "module";
    String beanPackage = "com.example";
    SpringContextModel model = new SpringContextModel();
    for (Candidate candidate : candidates) {
      registerBean(model, candidate.name(), type, module, beanPackage,
        dummyInputFile(beanPackage.replace('.', '/') + "/" + candidate.name() + ".java"), candidate.profileExpression(), candidate.primary());
    }
    registerInjectionPoint(model, type, "dependency", module, dummyInputFile("com/example/Consumer.java"));
    return model;
  }

  private static SpringContextModel ambiguousModelWithProfileNames(int profileNameCount) {
    Candidate[] candidates = new Candidate[profileNameCount + 2];
    candidates[0] = candidate("alwaysFirst", ProfileExpression.UNCONDITIONAL);
    candidates[1] = candidate("alwaysSecond", ProfileExpression.UNCONDITIONAL);
    for (int i = 0; i < profileNameCount; i++) {
      String profileName = "profile" + i;
      candidates[i + 2] = candidate(profileName, ProfileExpression.profile(profileName));
    }
    return modelWithCandidates(candidates);
  }

  private static Candidate candidate(String name, ProfileExpression profileExpression) {
    return new Candidate(name, profileExpression, false);
  }

  private static Candidate primaryCandidate(String name, ProfileExpression profileExpression) {
    return new Candidate(name, profileExpression, true);
  }

  private record Candidate(String name, ProfileExpression profileExpression, boolean primary) {
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
