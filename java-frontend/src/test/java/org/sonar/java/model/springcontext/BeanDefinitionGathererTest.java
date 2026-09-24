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

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.sonarsource.scanner.engine.sensor.test.fixtures.SensorContextTester;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.cache.WriteCache;
import org.sonar.java.TestUtils;
import org.sonar.java.telemetry.NoOpTelemetry;
import org.sonar.plugins.java.api.InputFileScannerContext;
import org.sonar.plugins.java.api.ModuleScannerContext;
import org.sonar.plugins.java.api.caching.CacheContext;
import org.sonar.plugins.java.api.caching.JavaReadCache;
import org.sonar.plugins.java.api.caching.JavaWriteCache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BeanDefinitionGathererTest extends SpringContextGathererTest {

  @BeforeEach
  void setUp() {
    gatherer = new BeanDefinitionGatherer(new NoOpTelemetry());
    model = new SpringContextModel();
  }

  // ---- Stereotype annotations -----------------------------------------------

  @ParameterizedTest(name = "{0}")
  @MethodSource("stereotypeAnnotationArguments")
  void stereotype_annotation_registers_bean(String filePath, String expectedBeanName, String expectedType) {
    scan(filePath);

    var beans = model.getBeanDefinitionRegistry().getByName(expectedBeanName);
    assertThat(beans).hasSize(1);
    assertThat(beans.get(0).getType()).isEqualTo(expectedType);
  }

  static Stream<Arguments> stereotypeAnnotationArguments() {
    return Stream.of(
      Arguments.of("src/test/files/springcontext/SimpleComponent.java", "simpleComponent", "checks.spring.context.SimpleComponent"),
      Arguments.of("src/test/files/springcontext/SimpleService.java", "simpleService", "checks.spring.context.SimpleService"),
      Arguments.of("src/test/files/springcontext/SimpleRepository.java", "simpleRepository", "checks.spring.context.SimpleRepository"),
      Arguments.of("src/test/files/springcontext/SimpleController.java", "simpleController", "checks.spring.context.SimpleController"),
      Arguments.of("src/test/files/springcontext/SimpleRestController.java", "simpleRestController", "checks.spring.context.SimpleRestController"),
      Arguments.of("src/test/files/springcontext/SimpleConfiguration.java", "simpleConfiguration", "checks.spring.context.SimpleConfiguration"),
      Arguments.of("src/test/files/springcontext/ConfigurationWithBeanMethods.java", "simpleServiceBean", "org.springframework.context.ApplicationContext")
    );
  }

  // ---- @Primary -------------------------------------------------------------

  @Test
  void primary_annotation_is_captured() {
    scan("src/test/files/springcontext/PrimaryBean.java");

    var beans = model.getBeanDefinitionRegistry().getByName("primaryBean");
    assertThat(beans).hasSize(1);
    assertThat(beans.get(0).isPrimary()).isTrue();
  }

  @Test
  void non_primary_bean_has_isPrimary_false() {
    scan("src/test/files/springcontext/SimpleComponent.java");

    var beans = model.getBeanDefinitionRegistry().getByName("simpleComponent");
    assertThat(beans).hasSize(1);
    assertThat(beans.get(0).isPrimary()).isFalse();
  }

  // ---- @Profile -------------------------------------------------------------

  @Test
  void profile_expression_is_parsed_for_a_stereotype_annotated_class() {
    scan("src/test/files/springcontext/ProfileExpressionComponent.java");

    var beans = model.getBeanDefinitionRegistry().getByName("profileExpressionComponent");
    assertThat(beans).hasSize(1);
    assertThat(beans.getFirst().getProfileExpression())
      .isEqualTo(ProfileExpression.and(List.of(ProfileExpression.profile("dev"), ProfileExpression.not(ProfileExpression.profile("test")))));
  }

  @Test
  void a_bean_without_profile_annotation_is_unconditional() {
    scan("src/test/files/springcontext/SimpleComponent.java");

    var beans = model.getBeanDefinitionRegistry().getByName("simpleComponent");
    assertThat(beans).hasSize(1);
    assertThat(beans.getFirst().getProfileExpression().isUnconditional()).isTrue();
  }

  @Test
  void a_bean_method_profile_is_conjoined_with_the_one_of_its_configuration_class() {
    scan("src/test/files/springcontext/ProfileExpressionConfiguration.java");

    ProfileExpression classExpression = ProfileExpression.or(List.of(ProfileExpression.profile("prod"), ProfileExpression.profile("staging")));
    assertThat(profileExpressionOf("profileExpressionConfiguration")).isEqualTo(classExpression);
    assertThat(profileExpressionOf("unprofiledBean")).isEqualTo(classExpression);
    assertThat(profileExpressionOf("profiledBean"))
      .isEqualTo(ProfileExpression.and(List.of(classExpression, ProfileExpression.not(ProfileExpression.profile("cloud")))));
  }

  @Test
  void a_bean_method_profile_alone_gates_a_bean_of_an_unprofiled_class() {
    scan("src/test/files/springcontext/ConfigurationWithBeanMethods.java");

    assertThat(profileExpressionOf("methodOnlyProfileBean")).isEqualTo(ProfileExpression.profile("test"));
    assertThat(profileExpressionOf("simpleServiceBean").isUnconditional()).isTrue();
  }

  @Test
  void a_malformed_profile_expression_is_unknown() {
    scan("src/test/files/springcontext/MalformedProfileComponent.java");

    assertThat(profileExpressionOf("malformedProfileComponent").isUnknown()).isTrue();
  }

  @Test
  void injection_points_carry_the_profile_expression_of_the_bean_declaring_them() {
    scan("src/test/files/springcontext/QualifiedFieldDependencies.java");

    assertThat(model.getTypeToDependenciesIndex().getDependenciesForType("org.springframework.core.env.Environment"))
      .extracting(InjectionPoint::profileExpression)
      .containsExactly(ProfileExpression.profile("prod"));
  }

  private ProfileExpression profileExpressionOf(String beanName) {
    var beans = model.getBeanDefinitionRegistry().getByName(beanName);
    assertThat(beans).hasSize(1);
    return beans.getFirst().getProfileExpression();
  }

  // ---- Anonymous / no annotation --------------------------------------------

  @Test
  void anonymous_class_is_skipped() {
    scan("src/test/files/springcontext/SpringBootAppWithAnonymousClass.java");

    // Anonymous class (no simpleName) should be skipped — it would not be registered as a bean
    // SpringBootApplication itself is not a stereotype bean
    assertThat(model.getBeanDefinitionRegistry().getByName("")).isEmpty();
    assertThat(model.getTypeToBeansIndex().getNamesForType("", "", Set.of())).isEmpty();
  }

  @Test
  void no_spring_annotations_registers_nothing() {
    scan("src/test/files/springcontext/NoScanAnnotations.java");

    assertThat(model.getBeanDefinitionRegistry().getByName("noScanAnnotations")).isEmpty();
  }

  // ---- DependencyVersionAware -----------------------------------------------

  @Test
  void gatherer_skipped_when_spring_not_in_classpath() {
    scan(List.of(), "src/test/files/springcontext/SimpleComponent.java");

    assertThat(model.getBeanDefinitionRegistry().getByName("simpleComponent")).isEmpty();
  }

  // ---- Multiple files -------------------------------------------------------

  @Test
  void beans_from_multiple_files_are_merged() {
    scan(
      "src/test/files/springcontext/SimpleComponent.java",
      "src/test/files/springcontext/SimpleService.java"
    );

    assertThat(model.getBeanDefinitionRegistry().getByName("simpleComponent")).hasSize(1);
    assertThat(model.getBeanDefinitionRegistry().getByName("simpleService")).hasSize(1);
  }

  // ---- @Qualifier handling --------------------------------------------------

  @Test
  void qualifier_on_bean_method_is_captured() {
    scan("src/test/files/springcontext/QualifiedBeanMethod.java");

    var beans = model.getBeanDefinitionRegistry().getByName("myBean");
    assertThat(beans).hasSize(1);
    assertThat(beans.get(0).getQualifier()).isEqualTo("myAlias");
  }

  @Test
  void qualifier_selects_specific_bean_among_multiple_candidates() {
    scan(
      "src/test/files/springcontext/PaymentProcessor.java",
      "src/test/files/springcontext/CreditCardProcessor.java",
      "src/test/files/springcontext/PayPalProcessor.java",
      "src/test/files/springcontext/OrderService.java"
    );

    var beans = model.getBeanDefinitionRegistry().getByName("orderService");
    assertThat(beans).hasSize(1);
    var deps = beans.get(0).getDependingBeans();
    // @Qualifier("paypal") takes precedence over the parameter name "paymentProcessor"
    // Note: PaymentProcessor resolves without package since it's not on the compiled classpath
    assertThat(deps).containsOnlyKeys("PaymentProcessor");
    assertThat(deps.get("PaymentProcessor")).containsOnly("paypal");
  }

  // ---- Bean location --------------------------------------------------------

  @Test
  void bean_location_is_captured() {
    scan("src/test/files/springcontext/SimpleComponent.java");

    var beans = model.getBeanDefinitionRegistry().getByName("simpleComponent");
    assertThat(beans).hasSize(1);
    var location = beans.get(0).getLocation();
    assertThat(location).isNotNull();
    assertThat(location.inputFile()).isNotNull();
    assertThat(location.mainLocation()).isNotNull();
  }

  // ---- Bean package ---------------------------------------------------------

  @Test
  void bean_package_is_captured() {
    scan("src/test/files/springcontext/SimpleComponent.java");

    var beans = model.getBeanDefinitionRegistry().getByName("simpleComponent");
    assertThat(beans).hasSize(1);
    assertThat(beans.get(0).getBeanPackage()).isEqualTo("checks.spring.context");
  }

  // ---- Caching: what the gatherer collects reaches the cache and comes back ----

  @Test
  void leaveFile_writes_collected_beans_to_cache() {
    InputFile inputFile = TestUtils.inputFile(new File("src/test/files/springcontext/QualifiedFieldDependencies.java"));
    var entry = scanAndCaptureCacheEntry("src/test/files/springcontext/QualifiedFieldDependencies.java");

    assertThat(entry.key()).isEqualTo("java:spring:bean-definitions:" + inputFile.key());
    var bean = JsonParser.parseString(new String(entry.data(), StandardCharsets.UTF_8))
      .getAsJsonObject().getAsJsonArray("beans").get(0).getAsJsonObject();
    assertThat(bean.get("name").getAsString()).isEqualTo("qualifiedFieldDependencies");
    assertThat(bean.get("type").getAsString()).isEqualTo("checks.spring.context.QualifiedFieldDependencies");
    assertThat(bean.get("package").getAsString()).isEqualTo("checks.spring.context");
    assertThat(bean.get("primary").getAsBoolean()).isFalse();
    assertThat(bean.get("profiles").getAsString()).isEqualTo("prod");
    assertThat(bean.getAsJsonObject("span").get("startLine").getAsInt()).isEqualTo(12);
    assertThat(bean.getAsJsonArray("typeHierarchy")).extracting(JsonElement::getAsString)
      .containsExactly("checks.spring.context.QualifiedFieldDependencies");
    assertThat(bean.getAsJsonArray("dependencies"))
      .extracting(element -> element.getAsJsonObject().get("type").getAsString())
      .containsExactlyInAnyOrder("org.springframework.context.ApplicationContext", "org.springframework.core.env.Environment");
  }

  @Test
  void scanWithoutParsing_restores_beans_from_cache() {
    InputFile inputFile = TestUtils.inputFile(new File("src/test/files/springcontext/QualifiedFieldDependencies.java"));
    var entry = scanAndCaptureCacheEntry("src/test/files/springcontext/QualifiedFieldDependencies.java");

    JavaReadCache readCache = mock(JavaReadCache.class);
    when(readCache.readBytes(entry.key())).thenReturn(entry.data());
    JavaWriteCache writeCache = mock(JavaWriteCache.class);
    CacheContext cacheContext = mockCacheContext(readCache, writeCache);
    InputFileScannerContext context = mock(InputFileScannerContext.class);
    when(context.getInputFile()).thenReturn(inputFile);
    when(context.getCacheContext()).thenReturn(cacheContext);

    gatherer = new BeanDefinitionGatherer(new NoOpTelemetry());
    model = new SpringContextModel();
    assertThat(gatherer.scanWithoutParsing(context)).isTrue();
    verify(writeCache).copyFromPrevious(entry.key());

    ModuleScannerContext moduleScannerContext = mock(ModuleScannerContext.class);
    when(moduleScannerContext.getModuleKey()).thenReturn("");
    gatherer.gatherSpringContextData(moduleScannerContext, model);

    var beans = model.getBeanDefinitionRegistry().getByName("qualifiedFieldDependencies");
    assertThat(beans).hasSize(1);
    assertThat(beans.getFirst().getType()).isEqualTo("checks.spring.context.QualifiedFieldDependencies");
    assertThat(beans.getFirst().getProfileExpression().profileNames()).containsExactly("prod");
    assertThat(model.getTypeToBeansIndex().getNamesForType("checks.spring.context.QualifiedFieldDependencies", "", Set.of()))
      .containsExactly("qualifiedFieldDependencies");
    assertInjectionPoint(
      model.getTypeToDependenciesIndex().getDependenciesForType("org.springframework.context.ApplicationContext"),
      "primaryContext", inputFile, 16);
    assertInjectionPoint(
      model.getTypeToDependenciesIndex().getDependenciesForType("org.springframework.core.env.Environment"),
      "environment", inputFile, 19);
  }

  @Test
  void parsed_beans_replace_beans_restored_for_the_same_file() {
    String filePath = "src/test/files/springcontext/QualifiedFieldDependencies.java";
    InputFile inputFile = TestUtils.inputFile(new File(filePath));
    var entry = scanAndCaptureCacheEntry(filePath);

    JavaReadCache readCache = mock(JavaReadCache.class);
    when(readCache.readBytes(entry.key())).thenReturn(entry.data());
    CacheContext cacheContext = mockCacheContext(readCache, mock(JavaWriteCache.class));
    InputFileScannerContext context = mock(InputFileScannerContext.class);
    when(context.getInputFile()).thenReturn(inputFile);
    when(context.getCacheContext()).thenReturn(cacheContext);

    gatherer = new BeanDefinitionGatherer(new NoOpTelemetry());
    model = new SpringContextModel();
    assertThat(gatherer.scanWithoutParsing(context)).isTrue();

    scan(filePath);

    assertThat(model.getBeanDefinitionRegistry().getByName("qualifiedFieldDependencies")).hasSize(1);
  }

  @Test
  void scanWithoutParsing_returns_false_on_cache_miss() {
    InputFile inputFile = TestUtils.inputFile(new File("src/test/files/springcontext/SimpleComponent.java"));

    JavaReadCache readCache = mock(JavaReadCache.class);
    when(readCache.readBytes(anyString())).thenReturn(null);
    CacheContext cacheContext = mockCacheContext(readCache, mock(JavaWriteCache.class));
    InputFileScannerContext context = mock(InputFileScannerContext.class);
    when(context.getInputFile()).thenReturn(inputFile);
    when(context.getCacheContext()).thenReturn(cacheContext);

    assertThat(gatherer.scanWithoutParsing(context)).isFalse();
  }

  // ---- TypeToDependenciesIndex -------------------------------------------------

  @Test
  void dependencies_in_multiple_files_all_registered() {
    scan("src/test/files/springcontext/OrderService.java", "src/test/files/springcontext/BlankQualifierDependency.java");
    InputFile orderServiceFile = TestUtils.inputFile(new File("src/test/files/springcontext/OrderService.java"));
    InputFile blankQualifierFile = TestUtils.inputFile(new File("src/test/files/springcontext/BlankQualifierDependency.java"));

    assertInjectionPoint(
      model.getTypeToDependenciesIndex().getDependenciesForType("PaymentProcessor"),
      "paypal", orderServiceFile, 13);
    assertInjectionPoint(
      model.getTypeToDependenciesIndex().getDependenciesForType("org.springframework.context.ApplicationContext"),
      "applicationContext", blankQualifierFile, 13);
  }

  @Test
  void two_beans_depending_on_same_type_and_name_both_tracked_with_distinct_locations() {
    scan("src/test/files/springcontext/AutowiredDependencies.java", "src/test/files/springcontext/AutowiredConstructorDependencies.java");
    InputFile autowiredDependenciesFile = TestUtils.inputFile(new File("src/test/files/springcontext/AutowiredDependencies.java"));
    InputFile autowiredConstructorFile = TestUtils.inputFile(new File("src/test/files/springcontext/AutowiredConstructorDependencies.java"));

    var injectionPoints = model.getTypeToDependenciesIndex().getDependenciesForType("org.springframework.context.ApplicationContext");
    assertThat(injectionPoints).hasSize(2);
    assertThat(injectionPoints).extracting(InjectionPoint::name).containsOnly("applicationContext");
    assertThat(injectionPoints)
      .extracting(p -> p.location().inputFile(), p -> p.location().mainLocation().startLine)
      .containsExactlyInAnyOrder(
        tuple(autowiredDependenciesFile, 12),
        tuple(autowiredConstructorFile, 15));
  }

  private record CacheEntry(String key, byte[] data) {
  }

  /**
   * Scans the given file with caching enabled and returns the entry the gatherer wrote for it, so that
   * assertions can be made on what an actual scan produces rather than on a hand-written payload.
   */
  private CacheEntry scanAndCaptureCacheEntry(String filePath) {
    WriteCache writeCache = mock(WriteCache.class);
    SensorContextTester ctx = SensorContextTester.create(new File(""));
    ctx.setCacheEnabled(true);
    ctx.setNextCache(writeCache);

    scan(ctx, filePath);

    var keyCaptor = ArgumentCaptor.forClass(String.class);
    var dataCaptor = ArgumentCaptor.forClass(byte[].class);
    verify(writeCache).write(keyCaptor.capture(), dataCaptor.capture());
    return new CacheEntry(keyCaptor.getValue(), dataCaptor.getValue());
  }
}
