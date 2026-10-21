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

import java.util.List;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.checks.helpers.SpringContextModelTestUtils;
import org.sonar.java.model.springcontext.BeanDefinitionHolder;
import org.sonar.java.model.springcontext.ProfileExpression;
import org.sonar.java.model.springcontext.SpringContextModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.checks.helpers.SpringContextModelTestUtils.beanLocation;
import static org.sonar.java.checks.helpers.SpringContextModelTestUtils.buildModel;
import static org.sonar.java.checks.helpers.SpringContextModelTestUtils.dummyInputFile;
import static org.sonar.java.checks.helpers.SpringContextModelTestUtils.indexBeanByType;

class AmbiguousDependencyCheckTest {

  private static final String BASE_PATH = "checks/spring/s9352/";

  private final AmbiguousDependencyCheck check = new AmbiguousDependencyCheck();

  @Test
  void ambiguous_dependency_with_no_disambiguation_raises_issue() {
    SpringContextModel model = buildModel(BASE_PATH, "ComponentOne.java", "ComponentTwo.java", "UnresolvedConsumer.java");
    assertThat(check.execute(model)).hasSize(1);
  }

  @Test
  void primary_candidate_resolves_ambiguity() {
    SpringContextModel model = buildModel(BASE_PATH, "BeanNameComponent.java", "PrimaryComponent.java", "PrimaryConsumer.java");
    assertThat(check.execute(model)).isEmpty();
  }

  @Test
  void two_primary_candidates_still_raise_issue() {
    SpringContextModel model = buildModel(BASE_PATH, "TwoPrimaryComponentA.java", "TwoPrimaryComponentB.java", "TwoPrimaryConsumer.java");
    assertThat(check.execute(model)).hasSize(1);
  }

  @Test
  void field_name_matching_bean_name_resolves_ambiguity() {
    SpringContextModel model = buildModel(BASE_PATH, "ComponentOne.java", "ComponentTwo.java", "NameMatchConsumer.java");
    assertThat(check.execute(model)).isEmpty();
  }

  @Test
  void qualifier_resolves_ambiguity() {
    SpringContextModel model = buildModel(BASE_PATH, "ComponentOne.java", "ComponentTwo.java", "QualifierConsumer.java");
    assertThat(check.execute(model)).isEmpty();
  }

  @Test
  void qualifier_on_bean_definition_resolves_ambiguity() {
    SpringContextModel model = buildModel(BASE_PATH, "QualifiedBeanDefinitionConfig.java", "QualifiedBeanDefinitionConsumer.java");
    assertThat(check.execute(model)).isEmpty();
  }

  @Test
  void single_candidate_does_not_raise_issue() {
    SpringContextModel model = buildModel(BASE_PATH, "ResourceLoaderComponent.java", "SingleCandidateConsumer.java");
    assertThat(check.execute(model)).isEmpty();
  }

  @Test
  void collection_and_array_fields_are_never_ambiguous() {
    SpringContextModel model = buildModel(BASE_PATH, "ComponentOne.java", "ComponentTwo.java", "CollectionInjectionConsumer.java");
    assertThat(check.execute(model)).isEmpty();
  }

  @Test
  void collection_and_array_parameters_of_a_bean_method_are_never_ambiguous() {
    SpringContextModel model = buildModel(BASE_PATH, "ComponentOne.java", "ComponentTwo.java", "CollectionInjectionConfig.java");
    assertThat(check.execute(model)).isEmpty();
  }

  @Test
  void map_keyed_by_bean_name_is_never_ambiguous() {
    SpringContextModel model = buildModel(BASE_PATH, "ComponentOne.java", "ComponentTwo.java", "MapInjectionConsumer.java");
    assertThat(check.execute(model)).isEmpty();
  }

  @Test
  void value_annotated_constructor_parameters_are_not_injection_points() {
    SpringContextModel model = buildModel(BASE_PATH, "ComponentOne.java", "ComponentTwo.java", "ValueConstructorParameterConsumer.java");
    assertThat(check.execute(model)).isEmpty();
  }

  // ---- Generic types ---------------------------------------------------------

  @Test
  void beans_of_the_same_generic_type_with_different_arguments_are_not_ambiguous() {
    SpringContextModel model = buildModel(BASE_PATH, "GenericDistinctArgumentsConsumer.java");
    assertThat(check.execute(model)).isEmpty();
  }

  @Test
  void beans_of_the_same_generic_type_with_the_same_argument_are_still_ambiguous() {
    SpringContextModel model = buildModel(BASE_PATH, "GenericSameArgumentConsumer.java");
    assertThat(check.execute(model)).hasSize(1);
  }

  @Test
  void a_raw_injection_point_is_still_ambiguous_between_every_implementation() {
    SpringContextModel model = buildModel(BASE_PATH, "RawGenericConsumer.java");
    assertThat(check.execute(model)).hasSize(1);
  }

  @Test
  void bean_methods_returning_maps_that_differ_only_by_their_value_type_are_not_ambiguous() {
    SpringContextModel model = buildModel(BASE_PATH, "GenericMapBeanConfig.java", "GenericMapConsumer.java");
    assertThat(check.execute(model)).isEmpty();
  }

  @Test
  void a_qualifier_disambiguates_two_beans_of_the_same_parameterized_type() {
    SpringContextModel model = buildModel(BASE_PATH, "GenericQualifiedBeanConfig.java", "GenericQualifiedConsumer.java");
    assertThat(check.execute(model)).isEmpty();
  }

  @Test
  void two_beans_of_the_same_parameterized_type_are_ambiguous_without_a_qualifier() {
    SpringContextModel model = buildModel(BASE_PATH, "GenericQualifiedBeanConfig.java", "GenericUnqualifiedConsumer.java");
    assertThat(check.execute(model)).hasSize(1);
  }

  @Test
  void one_resolved_injection_point_does_not_hide_another_ambiguous_one_of_the_same_type() {
    SpringContextModel model = buildModel(BASE_PATH, "ComponentOne.java", "ComponentTwo.java", "MixedInjectionConsumer.java");
    assertThat(check.execute(model)).hasSize(1);
  }

  // ---- @Profile -------------------------------------------------------------

  @Test
  void two_primary_candidates_colliding_under_a_profile_raise_issue() {
    SpringContextModel model =
      buildModel(BASE_PATH, "UnprofiledPrimaryComponent.java", "ProfiledPrimaryComponent.java", "PlainMessageSourceComponent.java", "MessageSourceConsumer.java");
    assertThat(check.execute(model))
      .singleElement()
      .extracting(SpringContextIssue::message)
      .asString()
      .contains("unprofiledPrimaryComponent", "profiledPrimaryComponent", "plainMessageSourceComponent");
  }

  @Test
  void inactive_profiled_candidate_still_leaves_ambiguity_between_the_rest() {
    SpringContextModel model =
      buildModel(BASE_PATH, "PlainEventPublisherComponentA.java", "PlainEventPublisherComponentB.java", "ProfiledEventPublisherComponent.java", "EventPublisherConsumer.java");
    assertThat(check.execute(model))
      .singleElement()
      .extracting(SpringContextIssue::message)
      .asString()
      .contains("plainEventPublisherComponentA", "plainEventPublisherComponentB")
      .doesNotContain("profiledEventPublisherComponent");
  }

  @Test
  void profiled_primary_does_not_resolve_ambiguity_when_its_profile_is_inactive() {
    SpringContextModel model =
      buildModel(BASE_PATH, "ProfiledPrimaryClassLoaderComponent.java", "PlainClassLoaderComponentA.java", "PlainClassLoaderComponentB.java", "ClassLoaderConsumer.java");
    assertThat(check.execute(model))
      .singleElement()
      .extracting(SpringContextIssue::message)
      .asString()
      .contains("plainClassLoaderComponentA", "plainClassLoaderComponentB")
      .doesNotContain("profiledPrimaryClassLoaderComponent");
  }

  @Test
  void candidate_profiled_with_an_operator_expression_is_inactive_when_no_profile_is_active() {
    SpringContextModel model =
      buildModel(BASE_PATH, "PlainEnvironmentComponentA.java", "PlainEnvironmentComponentB.java", "ExpressionProfiledEnvironmentComponent.java", "EnvironmentConsumer.java");
    assertThat(check.execute(model))
      .singleElement()
      .extracting(SpringContextIssue::message)
      .asString()
      .contains("plainEnvironmentComponentA", "plainEnvironmentComponentB")
      .doesNotContain("expressionProfiledEnvironmentComponent");
  }

  @Test
  void qualifier_towards_profiled_candidate_is_not_flagged_as_ambiguous() {
    SpringContextModel model =
      buildModel(BASE_PATH, "PlainEventPublisherComponentA.java", "PlainEventPublisherComponentB.java", "ProfiledEventPublisherComponent.java", "EventPublisherQualifierConsumer.java");
    assertThat(check.execute(model)).isEmpty();
  }

  @Test
  void two_candidates_sharing_a_profile_raise_issue() {
    SpringContextModel model =
      buildModel(BASE_PATH, "SameProfileComponentA.java", "SameProfileComponentB.java", "SameProfileConsumer.java");
    assertThat(check.execute(model))
      .singleElement()
      .extracting(SpringContextIssue::message)
      .asString()
      .contains("sameProfileComponentA", "sameProfileComponentB");
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @ValueSource(strings = {"DistinctProfileCandidates.java", "NegatedProfileCandidates.java", "CombinedProfilesCandidates.java"})
  void profiled_candidates_without_reportable_ambiguity_are_not_reported(String fileName) {
    SpringContextModel model = buildModel(BASE_PATH, fileName);
    assertThat(check.execute(model)).isEmpty();
  }

  @Test
  void only_consumer_active_during_candidate_collision_is_flagged() {
    SpringContextModel model = buildModel(BASE_PATH, "ProfiledConsumersConfig.java");
    assertThat(check.execute(model))
      .singleElement()
      .extracting(SpringContextIssue::message)
      .asString()
      .contains("plainCollaborator", "stagingCollaborator", "in profile 'staging'");
  }

  @Test
  void profiled_consumer_is_still_flagged_for_a_collision_between_plain_candidates() {
    SpringContextModel model = buildModel(BASE_PATH, "ComponentOne.java", "ComponentTwo.java", "ProfiledPlainConsumer.java");
    assertThat(check.execute(model))
      .singleElement()
      .extracting(SpringContextIssue::message)
      .asString()
      .contains("componentOne", "componentTwo", "in profile 'prod'");
  }

  @Test
  void candidate_with_an_unreadable_profile_is_assumed_active() {
    SpringContextModel model = buildModel(BASE_PATH, "UnreadableProfileCandidates.java");
    assertThat(check.execute(model))
      .singleElement()
      .extracting(SpringContextIssue::message)
      .asString()
      .contains("plainCollaborator", "unreadableCollaborator");
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
    registerBean(model, "svcImplA", type, "module-a", "com.a", fileA, builder -> builder.qualifier("shared"));
    registerBean(model, "svcImplB", type, "module-a", "com.a", fileB, builder -> builder.qualifier("shared"));
    registerBean(model, "consumer", "com.a.Consumer", "module-a", "com.a", consumerFile);
    registerInjectionPoint(model, type, "shared", "module-a", consumerFile);

    assertThat(check.execute(model)).hasSize(1);
  }

  private static void registerBean(SpringContextModel model, String beanName, String type, String module, String beanPackage, InputFile file) {
    registerBean(model, beanName, type, module, beanPackage, file, UnaryOperator.identity());
  }

  private static void registerBean(SpringContextModel model, String beanName, String type, String module, String beanPackage, InputFile file,
    UnaryOperator<BeanDefinitionHolder.Builder> customizer) {
    SpringContextModelTestUtils.registerBean(model, beanName, type, module, beanPackage, beanLocation(file, 1), customizer);
    indexBeanByType(model, beanName, type, module, beanPackage);
  }

  private static void registerInjectionPoint(SpringContextModel model, String type, String fieldName, String module, InputFile consumerFile) {
    registerInjectionPoint(model, type, fieldName, module, ProfileExpression.UNCONDITIONAL, consumerFile);
  }

  private static void registerInjectionPoint(SpringContextModel model, String type, String fieldName, String module, ProfileExpression consumerProfile, InputFile consumerFile) {
    SpringContextModelTestUtils.registerInjectionPoint(model, type, fieldName, module, consumerProfile, beanLocation(consumerFile, 5));
  }

}
