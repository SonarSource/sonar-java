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
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.cache.WriteCache;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.java.TestUtils;
import org.sonar.plugins.java.api.InputFileScannerContext;
import org.sonar.plugins.java.api.ModuleScannerContext;
import org.sonar.plugins.java.api.caching.CacheContext;
import org.sonar.plugins.java.api.caching.JavaReadCache;
import org.sonar.plugins.java.api.caching.JavaWriteCache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BeanDefinitionGathererTest extends SpringContextGathererTest {

  @BeforeEach
  void setUp() {
    gatherer = new BeanDefinitionGatherer();
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

  // ---- Explicit bean names ---------------------------------------------------

  @Test
  void explicit_bean_name_from_annotation_value() {
    scan("src/test/files/springcontext/ExplicitNameComponent.java");

    var beans = model.getBeanDefinitionRegistry().getByName("myBean");
    assertThat(beans).hasSize(1);
    assertThat(beans.get(0).getType()).isEqualTo("checks.spring.context.ExplicitNameComponent");
    // Default name should NOT be registered
    assertThat(model.getBeanDefinitionRegistry().getByName("explicitNameComponent")).isEmpty();
  }

  // ---- @Bean methods --------------------------------------------------------

  @Test
  void bean_method_with_explicit_name() {
    scan("src/test/files/springcontext/ConfigurationWithBeanMethods.java");

    var beans = model.getBeanDefinitionRegistry().getByName("namedBean");
    assertThat(beans).hasSize(1);
    assertThat(beans.get(0).getType()).isEqualTo("org.springframework.context.ApplicationContext");
    // Method name should NOT be registered
    assertThat(model.getBeanDefinitionRegistry().getByName("namedBeanMethod")).isEmpty();
  }

  @Test
  void bean_method_with_array_of_names_uses_first_name() {
    scan("src/test/files/springcontext/ConfigurationWithBeanMethods.java");

    var beans = model.getBeanDefinitionRegistry().getByName("arrayNamedBean");
    assertThat(beans).hasSize(1);
    assertThat(beans.get(0).getType()).isEqualTo("org.springframework.context.ApplicationContext");
  }

  @Test
  void bean_method_with_empty_name_array_falls_back_to_method_name() {
    scan("src/test/files/springcontext/ConfigurationWithBeanMethods.java");

    var beans = model.getBeanDefinitionRegistry().getByName("emptyNameArrayMethod");
    assertThat(beans).hasSize(1);
    assertThat(beans.get(0).getType()).isEqualTo("org.springframework.context.ApplicationContext");
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

  // ---- Anonymous / no annotation --------------------------------------------

  @Test
  void anonymous_class_is_skipped() {
    scan("src/test/files/springcontext/SpringBootAppWithAnonymousClass.java");

    // Anonymous class (no simpleName) should be skipped — it would not be registered as a bean
    // SpringBootApplication itself is not a stereotype bean
    assertThat(model.getBeanDefinitionRegistry().getByName("")).isEmpty();
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

  // ---- @Autowired dependencies ----------------------------------------------

  @ParameterizedTest(name = "{0}")
  @MethodSource("dependencyCollectionArguments")
  void dependencies_collected_as_depending_beans(String filePath, String expectedBeanName) {
    scan(filePath);

    var beans = model.getBeanDefinitionRegistry().getByName(expectedBeanName);
    assertThat(beans).hasSize(1);
    assertThat(beans.get(0).getDependingBeans().keySet())
      .containsExactlyInAnyOrder(
        "org.springframework.context.ApplicationContext",
        "org.springframework.core.env.Environment"
      );
  }

  static Stream<Arguments> dependencyCollectionArguments() {
    return Stream.of(
      Arguments.of("src/test/files/springcontext/AutowiredDependencies.java", "autowiredDependencies"),
      Arguments.of("src/test/files/springcontext/AutowiredConstructorDependencies.java", "autowiredConstructorDependencies"),
      Arguments.of("src/test/files/springcontext/BeanMethodWithDependencies.java", "myBean"),
      Arguments.of("src/test/files/springcontext/SingleConstructorDependencies.java", "singleConstructorDependencies")
    );
  }

  // ---- Implicit single-constructor injection --------------------------------

  @Test
  void multiple_constructors_without_autowired_yields_no_dependencies() {
    scan("src/test/files/springcontext/MultipleConstructorsNoDependencies.java");

    var beans = model.getBeanDefinitionRegistry().getByName("multipleConstructorsNoDependencies");
    assertThat(beans).hasSize(1);
    assertThat(beans.get(0).getDependingBeans()).isEmpty();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("mixedInjectionArguments")
  void both_injection_sources_collected_when_mixing_injection_styles(String filePath, String beanName) {
    scan(filePath);

    var beans = model.getBeanDefinitionRegistry().getByName(beanName);
    assertThat(beans).hasSize(1);
    var deps = beans.get(0).getDependingBeans();
    assertThat(deps.get("org.springframework.context.ApplicationContext")).containsOnly("applicationContext");
    assertThat(deps.get("org.springframework.core.env.Environment")).containsOnly("environment");
  }

  static Stream<Arguments> mixedInjectionArguments() {
    return Stream.of(
      Arguments.of("src/test/files/springcontext/AutowiredConstructorWithUnannotatedConstructor.java", "autowiredConstructorWithUnannotatedConstructor"),
      Arguments.of("src/test/files/springcontext/MixedInjectionDependencies.java", "mixedInjectionDependencies")
    );
  }

  // ---- @Qualifier handling --------------------------------------------------

  @ParameterizedTest(name = "{0}")
  @MethodSource("qualifiedDependencyArguments")
  void qualifier_is_captured_on_qualified_dependency(String filePath, String expectedBeanName) {
    scan(filePath);

    var beans = model.getBeanDefinitionRegistry().getByName(expectedBeanName);
    assertThat(beans).hasSize(1);
    var deps = beans.get(0).getDependingBeans();
    assertThat(deps.get("org.springframework.context.ApplicationContext")).containsOnly("primaryContext");
    assertThat(deps.get("org.springframework.core.env.Environment")).containsOnly("environment");
  }

  static Stream<Arguments> qualifiedDependencyArguments() {
    return Stream.of(
      Arguments.of("src/test/files/springcontext/QualifiedFieldDependencies.java", "qualifiedFieldDependencies"),
      Arguments.of("src/test/files/springcontext/QualifiedConstructorDependencies.java", "qualifiedConstructorDependencies"),
      Arguments.of("src/test/files/springcontext/QualifiedBeanMethodDependencies.java", "myBean")
    );
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

  @Test
  void unqualified_dependency_stores_field_name_in_names_set() {
    scan("src/test/files/springcontext/AutowiredDependencies.java");

    var beans = model.getBeanDefinitionRegistry().getByName("autowiredDependencies");
    assertThat(beans).hasSize(1);
    var deps = beans.get(0).getDependingBeans();
    assertThat(deps.get("org.springframework.context.ApplicationContext")).containsOnly("applicationContext");
    assertThat(deps.get("org.springframework.core.env.Environment")).containsOnly("environment");
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

  // ---- Caching --------------------------------------------------------------

  @Test
  void leaveFile_writes_beans_to_cache() {
    WriteCache writeCache = mock(WriteCache.class);
    SensorContextTester ctx = SensorContextTester.create(new File(""));
    ctx.setCacheEnabled(true);
    ctx.setNextCache(writeCache);

    scan(ctx, "src/test/files/springcontext/SimpleComponent.java");

    var dataCaptor = ArgumentCaptor.forClass(byte[].class);
    verify(writeCache).write(
      anyString(),
      dataCaptor.capture());
    String serialized = new String(dataCaptor.getValue(), StandardCharsets.UTF_8);
    String encodedName = Base64.getEncoder().encodeToString("simpleComponent".getBytes(StandardCharsets.UTF_8));
    assertThat(serialized)
      .contains(encodedName)
      .contains("checks.spring.context.SimpleComponent")
      .contains("checks.spring.context")
      .contains("false");
  }

  @Test
  void scanWithoutParsing_returns_true_and_restores_beans_on_cache_hit() {
    InputFile inputFile = TestUtils.inputFile(new File("src/test/files/springcontext/SimpleComponent.java"));
    String cacheKey = "java:spring:bean-definitions:" + inputFile.key();
    String encodedName = Base64.getEncoder().encodeToString("simpleComponent".getBytes(StandardCharsets.UTF_8));
    String serialized = encodedName + "|checks.spring.context.SimpleComponent|checks.spring.context|6:6:6:21|false|";

    JavaReadCache readCache = mock(JavaReadCache.class);
    when(readCache.readBytes(cacheKey)).thenReturn(serialized.getBytes(StandardCharsets.UTF_8));
    CacheContext cacheContext = mockCacheContext(readCache, mock(JavaWriteCache.class));

    InputFileScannerContext context = mock(InputFileScannerContext.class);
    when(context.getInputFile()).thenReturn(inputFile);
    when(context.getCacheContext()).thenReturn(cacheContext);

    assertThat(gatherer.scanWithoutParsing(context)).isTrue();

    ModuleScannerContext moduleScannerContext = mock(ModuleScannerContext.class);
    when(moduleScannerContext.getModuleKey()).thenReturn("");
    gatherer.gatherSpringContextData(moduleScannerContext, model);

    var beans = model.getBeanDefinitionRegistry().getByName("simpleComponent");
    assertThat(beans).hasSize(1);
    assertThat(beans.get(0).getType()).isEqualTo("checks.spring.context.SimpleComponent");
    assertThat(beans.get(0).isPrimary()).isFalse();
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

  @Test
  void scanWithoutParsing_restores_empty_bean_list_from_cache() {
    InputFile inputFile = TestUtils.inputFile(new File("src/test/files/springcontext/NoScanAnnotations.java"));
    String cacheKey = "java:spring:bean-definitions:" + inputFile.key();

    JavaReadCache readCache = mock(JavaReadCache.class);
    when(readCache.readBytes(cacheKey)).thenReturn("".getBytes(StandardCharsets.UTF_8));
    CacheContext cacheContext = mockCacheContext(readCache, mock(JavaWriteCache.class));

    InputFileScannerContext context = mock(InputFileScannerContext.class);
    when(context.getInputFile()).thenReturn(inputFile);
    when(context.getCacheContext()).thenReturn(cacheContext);

    assertThat(gatherer.scanWithoutParsing(context)).isTrue();

    ModuleScannerContext moduleScannerContext = mock(ModuleScannerContext.class);
    when(moduleScannerContext.getModuleKey()).thenReturn("");
    gatherer.gatherSpringContextData(moduleScannerContext, model);

    assertThat(model.getBeanDefinitionRegistry().getByName("noScanAnnotations")).isEmpty();
  }

  @Test
  void duplicate_cache_write_is_silently_ignored() {
    WriteCache writeCache = mock(WriteCache.class);
    doThrow(new IllegalArgumentException("duplicate key")).when(writeCache).write(anyString(), any(byte[].class));
    SensorContextTester ctx = SensorContextTester.create(new File(""));
    ctx.setCacheEnabled(true);
    ctx.setNextCache(writeCache);

    assertThatCode(() -> scan(ctx, "src/test/files/springcontext/SimpleComponent.java"))
      .doesNotThrowAnyException();
  }

  @Test
  void scanWithoutParsing_returns_false_on_corrupted_cache_entry() {
    InputFile inputFile = TestUtils.inputFile(new File("src/test/files/springcontext/SimpleComponent.java"));
    String cacheKey = "java:spring:bean-definitions:" + inputFile.key();

    JavaReadCache readCache = mock(JavaReadCache.class);
    when(readCache.readBytes(cacheKey)).thenReturn("not|valid|cache|content".getBytes(StandardCharsets.UTF_8));
    CacheContext cacheContext = mockCacheContext(readCache, mock(JavaWriteCache.class));

    InputFileScannerContext context = mock(InputFileScannerContext.class);
    when(context.getInputFile()).thenReturn(inputFile);
    when(context.getCacheContext()).thenReturn(cacheContext);

    assertThat(gatherer.scanWithoutParsing(context)).isFalse();
  }

  @Test
  void leaveFile_writes_dependencies_with_qualifiers_to_cache() {
    WriteCache writeCache = mock(WriteCache.class);
    SensorContextTester ctx = SensorContextTester.create(new File(""));
    ctx.setCacheEnabled(true);
    ctx.setNextCache(writeCache);

    scan(ctx, "src/test/files/springcontext/QualifiedFieldDependencies.java");

    var dataCaptor = ArgumentCaptor.forClass(byte[].class);
    verify(writeCache).write(anyString(), dataCaptor.capture());
    String serialized = new String(dataCaptor.getValue(), StandardCharsets.UTF_8);

    String encodedAppContext = Base64.getEncoder().encodeToString("org.springframework.context.ApplicationContext".getBytes(StandardCharsets.UTF_8));
    String encodedEnvType = Base64.getEncoder().encodeToString("org.springframework.core.env.Environment".getBytes(StandardCharsets.UTF_8));
    String encodedPrimaryContext = Base64.getEncoder().encodeToString("primaryContext".getBytes(StandardCharsets.UTF_8));
    String encodedEnvironment = Base64.getEncoder().encodeToString("environment".getBytes(StandardCharsets.UTF_8));
    assertThat(serialized)
      .contains(encodedAppContext + ":" + encodedPrimaryContext)
      .contains(encodedEnvType + ":" + encodedEnvironment);
  }

  @Test
  void scanWithoutParsing_restores_dependencies_with_and_without_qualifier_from_cache() {
    InputFile inputFile = TestUtils.inputFile(new File("src/test/files/springcontext/QualifiedFieldDependencies.java"));
    String cacheKey = "java:spring:bean-definitions:" + inputFile.key();
    String encodedName = Base64.getEncoder().encodeToString("qualifiedFieldDependencies".getBytes(StandardCharsets.UTF_8));
    String encodedAppContext = Base64.getEncoder().encodeToString("org.springframework.context.ApplicationContext".getBytes(StandardCharsets.UTF_8));
    String encodedEnvType = Base64.getEncoder().encodeToString("org.springframework.core.env.Environment".getBytes(StandardCharsets.UTF_8));
    String encodedPrimaryContext = Base64.getEncoder().encodeToString("primaryContext".getBytes(StandardCharsets.UTF_8));
    String encodedEnvironment = Base64.getEncoder().encodeToString("environment".getBytes(StandardCharsets.UTF_8));
    String serialized = encodedName + "|checks.spring.context.QualifiedFieldDependencies|checks.spring.context|10:6:10:30|false|"
      + encodedAppContext + ":" + encodedPrimaryContext
      + "," + encodedEnvType + ":" + encodedEnvironment;

    JavaReadCache readCache = mock(JavaReadCache.class);
    when(readCache.readBytes(cacheKey)).thenReturn(serialized.getBytes(StandardCharsets.UTF_8));
    CacheContext cacheContext = mockCacheContext(readCache, mock(JavaWriteCache.class));

    InputFileScannerContext context = mock(InputFileScannerContext.class);
    when(context.getInputFile()).thenReturn(inputFile);
    when(context.getCacheContext()).thenReturn(cacheContext);

    assertThat(gatherer.scanWithoutParsing(context)).isTrue();

    ModuleScannerContext moduleScannerContext = mock(ModuleScannerContext.class);
    when(moduleScannerContext.getModuleKey()).thenReturn("");
    gatherer.gatherSpringContextData(moduleScannerContext, model);

    var beans = model.getBeanDefinitionRegistry().getByName("qualifiedFieldDependencies");
    assertThat(beans).hasSize(1);
    var deps = beans.get(0).getDependingBeans();
    assertThat(deps.get("org.springframework.context.ApplicationContext")).containsOnly("primaryContext");
    assertThat(deps.get("org.springframework.core.env.Environment")).containsOnly("environment");
  }

  @Test
  void blank_qualifier_value_is_treated_as_no_qualifier() {
    scan("src/test/files/springcontext/BlankQualifierDependency.java");

    var beans = model.getBeanDefinitionRegistry().getByName("blankQualifierDependency");
    assertThat(beans).hasSize(1);
    var deps = beans.get(0).getDependingBeans();
    assertThat(deps).containsOnlyKeys("org.springframework.context.ApplicationContext");
    assertThat(deps.get("org.springframework.context.ApplicationContext")).containsOnly("applicationContext");
  }

  private static CacheContext mockCacheContext(JavaReadCache readCache, JavaWriteCache writeCache) {
    CacheContext cacheContext = mock(CacheContext.class);
    when(cacheContext.isCacheEnabled()).thenReturn(true);
    when(cacheContext.getReadCache()).thenReturn(readCache);
    when(cacheContext.getWriteCache()).thenReturn(writeCache);
    return cacheContext;
  }
}
