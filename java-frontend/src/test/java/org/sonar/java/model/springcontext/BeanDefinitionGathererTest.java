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

import com.google.gson.JsonParser;
import com.sonarsource.scanner.engine.sensor.test.fixtures.SensorContextTester;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.cache.WriteCache;
import org.sonar.java.TestUtils;
import org.sonar.plugins.java.api.InputFileScannerContext;
import org.sonar.plugins.java.api.ModuleScannerContext;
import org.sonar.plugins.java.api.caching.CacheContext;
import org.sonar.plugins.java.api.caching.JavaReadCache;
import org.sonar.plugins.java.api.caching.JavaWriteCache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
    assertThat(model.getTypeToBeanNamesIndex().getNamesForType("")).isEmpty();
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

  // ---- Caching --------------------------------------------------------------

  @ParameterizedTest(name = "{0}")
  @MethodSource("roundTripArguments")
  void beans_written_to_cache_are_restored_identically(String filePath, String beanName, @Nullable String expectedProfiles) {
    byte[] written = writeToCacheAndCapture(filePath);

    InputFile inputFile = TestUtils.inputFile(new File(filePath));
    String cacheKey = "java:spring:bean-definitions:" + inputFile.key();
    JavaReadCache readCache = mock(JavaReadCache.class);
    when(readCache.readBytes(cacheKey)).thenReturn(written);
    JavaWriteCache writeCache = mock(JavaWriteCache.class);
    CacheContext cacheContext = mockCacheContext(readCache, writeCache);

    InputFileScannerContext context = mock(InputFileScannerContext.class);
    when(context.getInputFile()).thenReturn(inputFile);
    when(context.getCacheContext()).thenReturn(cacheContext);

    gatherer = new BeanDefinitionGatherer();
    model = new SpringContextModel();
    assertThat(gatherer.scanWithoutParsing(context)).isTrue();
    verify(writeCache).copyFromPrevious(cacheKey);

    ModuleScannerContext moduleScannerContext = mock(ModuleScannerContext.class);
    when(moduleScannerContext.getModuleKey()).thenReturn("");
    gatherer.gatherSpringContextData(moduleScannerContext, model);

    var restored = model.getBeanDefinitionRegistry().getByName(beanName);
    assertThat(restored).hasSize(1);
    assertThat(restored.get(0).getProfiles()).isEqualTo(expectedProfiles);

    gatherer = new BeanDefinitionGatherer();
    model = new SpringContextModel();
    scan(filePath);
    var parsed = model.getBeanDefinitionRegistry().getByName(beanName);
    assertThat(parsed).hasSize(1);

    assertThat(restored.get(0)).satisfies(bean -> {
      assertThat(bean.getType()).isEqualTo(parsed.get(0).getType());
      assertThat(bean.getBeanPackage()).isEqualTo(parsed.get(0).getBeanPackage());
      assertThat(bean.isPrimary()).isEqualTo(parsed.get(0).isPrimary());
      assertThat(bean.getProfiles()).isEqualTo(parsed.get(0).getProfiles());
      assertThat(bean.getDependingBeans()).isEqualTo(parsed.get(0).getDependingBeans());
      assertThat(bean.getLocation().mainLocation()).isEqualTo(parsed.get(0).getLocation().mainLocation());
      assertThat(bean.getLocation().inputFile().key()).isEqualTo(parsed.get(0).getLocation().inputFile().key());
    });
  }

  static Stream<Arguments> roundTripArguments() {
    return Stream.of(
      Arguments.of("src/test/files/springcontext/SimpleComponent.java", "simpleComponent", null),
      Arguments.of("src/test/files/springcontext/QualifiedFieldDependencies.java", "qualifiedFieldDependencies", "prod"),
      Arguments.of("src/test/files/springcontext/PrimaryBean.java", "primaryBean", null));
  }

  private byte[] writeToCacheAndCapture(String filePath) {
    WriteCache writeCache = mock(WriteCache.class);
    SensorContextTester ctx = SensorContextTester.create(new File(""));
    ctx.setCacheEnabled(true);
    ctx.setNextCache(writeCache);

    scan(ctx, filePath);

    var dataCaptor = ArgumentCaptor.forClass(byte[].class);
    verify(writeCache).write(anyString(), dataCaptor.capture());
    return dataCaptor.getValue();
  }

  @Test
  void leaveFile_writes_profile_beans_and_dependencies_to_cache() {
    WriteCache writeCache = mock(WriteCache.class);
    SensorContextTester ctx = SensorContextTester.create(new File(""));
    ctx.setCacheEnabled(true);
    ctx.setNextCache(writeCache);

    scan(ctx, "src/test/files/springcontext/QualifiedFieldDependencies.java");

    var dataCaptor = ArgumentCaptor.forClass(byte[].class);
    verify(writeCache).write(anyString(), dataCaptor.capture());
    String serialized = new String(dataCaptor.getValue(), StandardCharsets.UTF_8);
    var bean = JsonParser.parseString(serialized).getAsJsonObject().getAsJsonArray("beans").get(0).getAsJsonObject();
    assertThat(bean.get("name").getAsString()).isEqualTo("qualifiedFieldDependencies");
    assertThat(bean.get("type").getAsString()).isEqualTo("checks.spring.context.QualifiedFieldDependencies");
    assertThat(bean.get("package").getAsString()).isEqualTo("checks.spring.context");
    assertThat(bean.get("primary").getAsBoolean()).isFalse();
    assertThat(bean.get("profiles").getAsString()).isEqualTo("prod");
    assertThat(bean.getAsJsonObject("span").get("startLine").getAsInt()).isEqualTo(12);
    assertThat(bean.getAsJsonArray("typeHierarchy")).extracting(element -> element.getAsString())
      .contains("checks.spring.context.QualifiedFieldDependencies");
    assertThat(bean.getAsJsonArray("dependencies")).extracting(element -> element.getAsJsonObject().get("type").getAsString())
      .containsExactlyInAnyOrder("org.springframework.context.ApplicationContext", "org.springframework.core.env.Environment");
  }

  @Test
  void scanWithoutParsing_restores_profile_beans_and_dependencies_from_cache() {
    InputFile inputFile = TestUtils.inputFile(new File("src/test/files/springcontext/QualifiedFieldDependencies.java"));
    String cacheKey = "java:spring:bean-definitions:" + inputFile.key();
    String serialized = """
      {"version":1,"beans":[{"name":"qualifiedFieldDependencies","type":"checks.spring.context.QualifiedFieldDependencies","package":"checks.spring.context","span":{"startLine":12,"startCharacter":6,"endLine":12,"endCharacter":32},"primary":false,"profiles":"prod","dependencies":[{"type":"org.springframework.context.ApplicationContext","injectionPoints":[{"name":"primaryContext","span":{"startLine":16,"startCharacter":2,"endLine":16,"endCharacter":55}}]},{"type":"org.springframework.core.env.Environment","injectionPoints":[{"name":"environment","span":{"startLine":19,"startCharacter":2,"endLine":19,"endCharacter":38}}]}],"typeHierarchy":["checks.spring.context.QualifiedFieldDependencies"]}]}
      """;

    JavaReadCache readCache = mock(JavaReadCache.class);
    when(readCache.readBytes(cacheKey)).thenReturn(serialized.getBytes(StandardCharsets.UTF_8));
    JavaWriteCache writeCache = mock(JavaWriteCache.class);
    CacheContext cacheContext = mockCacheContext(readCache, writeCache);

    InputFileScannerContext context = mock(InputFileScannerContext.class);
    when(context.getInputFile()).thenReturn(inputFile);
    when(context.getCacheContext()).thenReturn(cacheContext);

    assertThat(gatherer.scanWithoutParsing(context)).isTrue();

    ModuleScannerContext moduleScannerContext = mock(ModuleScannerContext.class);
    when(moduleScannerContext.getModuleKey()).thenReturn("");
    gatherer.gatherSpringContextData(moduleScannerContext, model);

    var beans = model.getBeanDefinitionRegistry().getByName("qualifiedFieldDependencies");
    assertThat(beans).hasSize(1);
    assertThat(beans.get(0).getType()).isEqualTo("checks.spring.context.QualifiedFieldDependencies");
    assertThat(beans.get(0).isPrimary()).isFalse();
    assertThat(beans.get(0).getProfiles()).isEqualTo("prod");
    var deps = beans.get(0).getDependingBeans();
    assertThat(deps.get("org.springframework.context.ApplicationContext")).containsOnly("primaryContext");
    assertThat(deps.get("org.springframework.core.env.Environment")).containsOnly("environment");

    assertInjectionPoint(
      model.getTypeToDependenciesIndex().getDependenciesForType("org.springframework.context.ApplicationContext"),
      "primaryContext", inputFile, 16);
    assertInjectionPoint(
      model.getTypeToDependenciesIndex().getDependenciesForType("org.springframework.core.env.Environment"),
      "environment", inputFile, 19);
    verify(writeCache).copyFromPrevious(cacheKey);
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
    when(readCache.readBytes(cacheKey)).thenReturn("{\"version\":1,\"beans\":[]}".getBytes(StandardCharsets.UTF_8));
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

  @ParameterizedTest(name = "{0}")
  @MethodSource("corruptedCacheEntries")
  void scanWithoutParsing_returns_false_on_corrupted_cache_entry(String description, String content) {
    InputFile inputFile = TestUtils.inputFile(new File("src/test/files/springcontext/SimpleComponent.java"));
    String cacheKey = "java:spring:bean-definitions:" + inputFile.key();

    JavaReadCache readCache = mock(JavaReadCache.class);
    when(readCache.readBytes(cacheKey)).thenReturn(content.getBytes(StandardCharsets.UTF_8));
    JavaWriteCache writeCache = mock(JavaWriteCache.class);
    CacheContext cacheContext = mockCacheContext(readCache, writeCache);

    InputFileScannerContext context = mock(InputFileScannerContext.class);
    when(context.getInputFile()).thenReturn(inputFile);
    when(context.getCacheContext()).thenReturn(cacheContext);

    assertThat(gatherer.scanWithoutParsing(context)).isFalse();
    verify(writeCache, never()).copyFromPrevious(cacheKey);
  }

  static Stream<Arguments> corruptedCacheEntries() {
    String bean = "{\"name\":\"n\",\"type\":\"t\",\"package\":\"p\",\"span\":%s,\"primary\":%s,\"profiles\":null,"
      + "\"dependencies\":[],\"typeHierarchy\":[]}";
    String span = "{\"startLine\":1,\"startCharacter\":0,\"endLine\":1,\"endCharacter\":5}";
    return Stream.of(
      Arguments.of("not json at all", "not|valid|cache|content"),
      Arguments.of("empty content", ""),
      Arguments.of("json array instead of object", "[]"),
      Arguments.of("unsupported version", "{\"version\":2,\"beans\":[]}"),
      Arguments.of("missing version", "{\"beans\":[]}"),
      Arguments.of("non-numeric version", "{\"version\":\"1\",\"beans\":[]}"),
      Arguments.of("version not a primitive", "{\"version\":{},\"beans\":[]}"),
      Arguments.of("missing beans", "{\"version\":1}"),
      Arguments.of("beans not an array", "{\"version\":1,\"beans\":\"x\"}"),
      Arguments.of("bean not an object", "{\"version\":1,\"beans\":[\"x\"]}"),
      Arguments.of("bean missing properties", "{\"version\":1,\"beans\":[{}]}"),
      Arguments.of("primary not a boolean", "{\"version\":1,\"beans\":[" + bean.formatted(span, "\"yes\"") + "]}"),
      Arguments.of("profiles neither string nor null",
        "{\"version\":1,\"beans\":[" + bean.formatted(span, "false").replace("\"profiles\":null", "\"profiles\":42") + "]}"),
      Arguments.of("type hierarchy holds a non-string",
        "{\"version\":1,\"beans\":[" + bean.formatted(span, "false").replace("\"typeHierarchy\":[]", "\"typeHierarchy\":[1]") + "]}"),
      Arguments.of("span with a zero start line",
        "{\"version\":1,\"beans\":[" + bean.formatted(span.replace("\"startLine\":1", "\"startLine\":0"), "false") + "]}"),
      Arguments.of("span ending before it starts",
        "{\"version\":1,\"beans\":[" + bean.formatted(span.replace("\"endLine\":1", "\"endLine\":0"), "false") + "]}"),
      Arguments.of("span with a fractional line number",
        "{\"version\":1,\"beans\":[" + bean.formatted(span.replace("\"startLine\":1", "\"startLine\":1.5"), "false") + "]}"),
      Arguments.of("span not an object",
        "{\"version\":1,\"beans\":[" + bean.formatted("3", "false") + "]}"),
      Arguments.of("dependency not an object",
        "{\"version\":1,\"beans\":[" + bean.formatted(span, "false").replace("\"dependencies\":[]", "\"dependencies\":[\"x\"]") + "]}"),
      Arguments.of("dependency missing its injection points",
        "{\"version\":1,\"beans\":[" + bean.formatted(span, "false")
          .replace("\"dependencies\":[]", "\"dependencies\":[{\"type\":\"T\"}]") + "]}"),
      Arguments.of("injection point missing its name",
        "{\"version\":1,\"beans\":[" + bean.formatted(span, "false")
          .replace("\"dependencies\":[]", "\"dependencies\":[{\"type\":\"T\",\"injectionPoints\":[{\"span\":" + span + "}]}]") + "]}"));
  }

  private static CacheContext mockCacheContext(JavaReadCache readCache, JavaWriteCache writeCache) {
    CacheContext cacheContext = mock(CacheContext.class);
    when(cacheContext.isCacheEnabled()).thenReturn(true);
    when(cacheContext.getReadCache()).thenReturn(readCache);
    when(cacheContext.getWriteCache()).thenReturn(writeCache);
    return cacheContext;
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

  private static void assertInjectionPoint(Set<InjectionPoint> injectionPoints, String expectedName,
    InputFile expectedInputFile, int expectedLine) {
    assertThat(injectionPoints).hasSize(1);
    var point = injectionPoints.iterator().next();
    assertThat(point.name()).isEqualTo(expectedName);
    assertThat(point.location().inputFile()).isEqualTo(expectedInputFile);
    assertThat(point.location().mainLocation().startLine).isEqualTo(expectedLine);
  }
}
