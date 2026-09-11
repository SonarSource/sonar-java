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
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.TestUtils;
import org.sonar.java.model.springcontext.BeanDefinitionGatherer.BeanData;
import org.sonar.java.reporting.AnalyzerMessage.TextSpan;
import org.sonar.java.serialization.BeanDataTypeAdapter;
import org.sonar.plugins.java.api.InputFileScannerContext;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.caching.CacheContext;
import org.sonar.plugins.java.api.caching.JavaReadCache;
import org.sonar.plugins.java.api.caching.JavaWriteCache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.java.model.springcontext.SpringContextGathererTest.assertInjectionPoint;
import static org.sonar.java.model.springcontext.SpringContextGathererTest.mockCacheContext;

class SpringContextCacheHelperTest {

  private static final Logger LOG = LoggerFactory.getLogger(SpringContextCacheHelperTest.class);

  private static final InputFile INPUT_FILE = TestUtils.inputFile(new File("src/test/files/springcontext/SimpleComponent.java"));
  private static final String BEAN_CACHE_KEY = "java:spring:bean-definitions:" + INPUT_FILE.key();
  private static final String PACKAGES_CACHE_KEY = "java:spring:component-scan-packages:" + INPUT_FILE.key();

  @Nested
  class BeanDefinitions {

    @Test
    void write_uses_the_bean_definitions_cache_key() {
      JavaWriteCache writeCache = mock(JavaWriteCache.class);

      SpringContextCacheHelper.writeBeanDefinitionsToCache(writeContext(writeCache), LOG, List.of(simpleComponent()));

      verify(writeCache).write(eq(BEAN_CACHE_KEY), any(byte[].class));
    }

    @Test
    void writes_beans_in_the_documented_json_shape() {
      var written = writeBeans(List.of(beanWithDependencies()));

      var document = JsonParser.parseString(written).getAsJsonObject();
      assertThat(document.get("version").getAsInt()).isOne();
      var bean = document.getAsJsonArray("beans").get(0).getAsJsonObject();
      assertThat(bean.get("name").getAsString()).isEqualTo("qualifiedFieldDependencies");
      assertThat(bean.get("type").getAsString()).isEqualTo("checks.spring.context.QualifiedFieldDependencies");
      assertThat(bean.get("package").getAsString()).isEqualTo("checks.spring.context");
      assertThat(bean.get("primary").getAsBoolean()).isFalse();
      assertThat(bean.get("profiles").getAsString()).isEqualTo("prod");
      assertThat(bean.getAsJsonObject("span").get("startLine").getAsInt()).isEqualTo(12);
      assertThat(bean.getAsJsonArray("typeHierarchy")).extracting(JsonElement::getAsString)
        .containsExactlyInAnyOrder("checks.spring.context.QualifiedFieldDependencies");
      assertThat(bean.getAsJsonArray("dependencies")).extracting(element -> element.getAsJsonObject().get("type").getAsString())
        .containsExactlyInAnyOrder("org.springframework.context.ApplicationContext", "org.springframework.core.env.Environment");
    }

    @Test
    void the_input_file_is_not_serialized_but_restored_from_the_entry_being_read() {
      var written = writeBeans(List.of(simpleComponent()));

      assertThat(written).doesNotContain(INPUT_FILE.key());
      assertThat(readBeans(written)).hasValueSatisfying(beans -> assertThat(beans)
        .extracting(BeanData::inputFile)
        .containsExactly(INPUT_FILE));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("beansToRoundTrip")
    void beans_round_trip_through_the_cache(String description, List<BeanData> beans) {
      var restored = readBeans(writeBeans(beans));

      assertThat(restored).contains(beans);
    }

    static Stream<Arguments> beansToRoundTrip() {
      return Stream.of(
        Arguments.of("no bean at all", List.of()),
        Arguments.of("bean without profiles nor dependencies", List.of(simpleComponent())),
        Arguments.of("bean with profiles and dependencies", List.of(beanWithDependencies())),
        Arguments.of("primary bean", List.of(primaryBean())),
        Arguments.of("several beans defined in the same file", List.of(simpleComponent(), primaryBean(), beanWithDependencies())));
    }

    @Test
    void injection_point_locations_survive_the_round_trip() {
      var restored = readBeans(writeBeans(List.of(beanWithDependencies())));

      assertThat(restored).hasValueSatisfying(beans -> {
        var dependencies = beans.getFirst().dependencyInjectionPoints();
        assertInjectionPoint(dependencies.get("org.springframework.context.ApplicationContext"), "primaryContext", INPUT_FILE, 16);
        assertInjectionPoint(dependencies.get("org.springframework.core.env.Environment"), "environment", INPUT_FILE, 19);
      });
    }

    @Test
    void read_copies_the_entry_forward_on_success() {
      JavaWriteCache writeCache = mock(JavaWriteCache.class);
      var context = readContext("{\"version\":1,\"beans\":[]}", BEAN_CACHE_KEY, writeCache);

      assertThat(SpringContextCacheHelper.readBeanDefinitionsFromCache(context, LOG)).isPresent();

      verify(writeCache).copyFromPrevious(BEAN_CACHE_KEY);
    }

    @Test
    void read_returns_empty_when_there_is_no_entry() {
      JavaWriteCache writeCache = mock(JavaWriteCache.class);
      var context = readContext(null, BEAN_CACHE_KEY, writeCache);

      assertThat(SpringContextCacheHelper.readBeanDefinitionsFromCache(context, LOG)).isEmpty();

      verify(writeCache, never()).copyFromPrevious(anyString());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("corruptedEntries")
    void read_returns_empty_on_corrupted_entry(String description, String content) {
      JavaWriteCache writeCache = mock(JavaWriteCache.class);
      var context = readContext(content, BEAN_CACHE_KEY, writeCache);

      assertThat(SpringContextCacheHelper.readBeanDefinitionsFromCache(context, LOG)).isEmpty();

      verify(writeCache, never()).copyFromPrevious(BEAN_CACHE_KEY);
    }

    static Stream<Arguments> corruptedEntries() {
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
            .replace("\"dependencies\":[]", "\"dependencies\":[{\"type\":\"T\",\"injectionPoints\":[{\"span\":" + span + "}]}]") + "]}"),
        Arguments.of("name given as a number",
          "{\"version\":1,\"beans\":[" + bean.formatted(span, "false").replace("\"name\":\"n\"", "\"name\":1") + "]}"),
        Arguments.of("span with a line number given as a string",
          "{\"version\":1,\"beans\":[" + bean.formatted(span.replace("\"startLine\":1", "\"startLine\":\"1\""), "false") + "]}"),
        Arguments.of("missing primary",
          "{\"version\":1,\"beans\":[" + bean.formatted(span, "false").replace("\"primary\":false,", "") + "]}"),
        Arguments.of("missing profiles",
          "{\"version\":1,\"beans\":[" + bean.formatted(span, "false").replace("\"profiles\":null,", "") + "]}"));
    }

    @Test
    void read_ignores_unknown_properties() {
      String content = """
        {"version":1,"beans":[{"name":"simpleComponent","type":"checks.spring.context.SimpleComponent","package":"checks.spring.context",\
        "span":{"startLine":8,"startCharacter":13,"endLine":8,"endCharacter":28,"unknown":[]},"primary":false,"profiles":null,\
        "dependencies":[{"type":"T","injectionPoints":[{"name":"t","span":{"startLine":9,"startCharacter":2,"endLine":9,"endCharacter":5},\
        "unknown":{}}],"unknown":0}],"typeHierarchy":[],"unknown":"ignored"}],"unknown":true}
        """;
      JavaWriteCache writeCache = mock(JavaWriteCache.class);
      var context = readContext(content, BEAN_CACHE_KEY, writeCache);

      assertThat(SpringContextCacheHelper.readBeanDefinitionsFromCache(context, LOG)).hasValueSatisfying(beans -> {
        assertThat(beans).hasSize(1);
        assertThat(beans.getFirst().beanName()).isEqualTo("simpleComponent");
        assertThat(beans.getFirst().dependingBeans().get("T")).containsOnly("t");
      });
      verify(writeCache).copyFromPrevious(BEAN_CACHE_KEY);
    }

    @Test
    void second_write_under_the_same_key_is_ignored() {
      JavaWriteCache writeCache = mock(JavaWriteCache.class);
      doThrow(new IllegalArgumentException("duplicate key")).when(writeCache).write(anyString(), any(byte[].class));
      var context = writeContext(writeCache);

      assertThatCode(() -> SpringContextCacheHelper.writeBeanDefinitionsToCache(context, LOG, List.of(simpleComponent())))
        .doesNotThrowAnyException();
    }
  }

  @Nested
  class ComponentScanPackages {

    @Test
    void write_uses_the_component_scan_packages_cache_key() {
      JavaWriteCache writeCache = mock(JavaWriteCache.class);

      SpringContextCacheHelper.writeComponentScanPackagesToCache(writeContext(writeCache), LOG, Set.of("com.example.service"));

      verify(writeCache).write(eq(PACKAGES_CACHE_KEY), any(byte[].class));
    }

    @Test
    void writes_packages_in_the_documented_json_shape() {
      var written = writePackages(List.of("com.example.service", "com.example.web"));

      var document = JsonParser.parseString(written).getAsJsonObject();
      assertThat(document.get("version").getAsInt()).isOne();
      assertThat(document.getAsJsonArray("packages")).extracting(JsonElement::getAsString)
        .containsExactlyInAnyOrder("com.example.service", "com.example.web");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("packagesToRoundTrip")
    void packages_round_trip_through_the_cache(String description, List<String> packages) {
      var restored = readPackages(writePackages(packages));

      assertThat(restored).contains(packages);
    }

    static Stream<Arguments> packagesToRoundTrip() {
      return Stream.of(
        Arguments.of("no package at all", List.of()),
        Arguments.of("a single package", List.of("com.example.service")),
        Arguments.of("several packages", List.of("com.example.service", "com.example.web", "checks.spring.context")));
    }

    @Test
    void read_copies_the_entry_forward_on_success() {
      JavaWriteCache writeCache = mock(JavaWriteCache.class);
      var context = readContext("{\"version\":1,\"packages\":[]}", PACKAGES_CACHE_KEY, writeCache);

      assertThat(SpringContextCacheHelper.readComponentScanPackagesFromCache(context, LOG)).isPresent();

      verify(writeCache).copyFromPrevious(PACKAGES_CACHE_KEY);
    }

    @Test
    void read_returns_empty_when_there_is_no_entry() {
      JavaWriteCache writeCache = mock(JavaWriteCache.class);
      var context = readContext(null, PACKAGES_CACHE_KEY, writeCache);

      assertThat(SpringContextCacheHelper.readComponentScanPackagesFromCache(context, LOG)).isEmpty();

      verify(writeCache, never()).copyFromPrevious(anyString());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("corruptedEntries")
    void read_returns_empty_on_corrupted_entry(String description, String content) {
      JavaWriteCache writeCache = mock(JavaWriteCache.class);
      var context = readContext(content, PACKAGES_CACHE_KEY, writeCache);

      assertThat(SpringContextCacheHelper.readComponentScanPackagesFromCache(context, LOG)).isEmpty();

      verify(writeCache, never()).copyFromPrevious(PACKAGES_CACHE_KEY);
    }

    static Stream<Arguments> corruptedEntries() {
      return Stream.of(
        Arguments.of("not json at all", "com.example.service;com.example.web"),
        Arguments.of("empty content", ""),
        Arguments.of("json array instead of object", "[\"com.example.service\"]"),
        Arguments.of("unsupported version", "{\"version\":2,\"packages\":[]}"),
        Arguments.of("missing version", "{\"packages\":[]}"),
        Arguments.of("non-numeric version", "{\"version\":\"1\",\"packages\":[]}"),
        Arguments.of("missing packages", "{\"version\":1}"),
        Arguments.of("packages not an array", "{\"version\":1,\"packages\":\"com.example.service\"}"),
        Arguments.of("package not a string", "{\"version\":1,\"packages\":[1]}"),
        Arguments.of("package given as an object", "{\"version\":1,\"packages\":[{\"name\":\"com.example.service\"}]}"));
    }

    @Test
    void second_write_under_the_same_key_is_ignored() {
      JavaWriteCache writeCache = mock(JavaWriteCache.class);
      doThrow(new IllegalArgumentException("duplicate key")).when(writeCache).write(anyString(), any(byte[].class));
      var context = writeContext(writeCache);

      assertThatCode(() -> SpringContextCacheHelper.writeComponentScanPackagesToCache(context, LOG, Set.of("com.example.service")))
        .doesNotThrowAnyException();
    }
  }

  // ---- Bean fixtures --------------------------------------------------------

  private static BeanData simpleComponent() {
    return beanData("simpleComponent", "checks.spring.context.SimpleComponent", new TextSpan(8, 13, 8, 28), false, null,
      Map.of(), Set.of("checks.spring.context.SimpleComponent"));
  }

  private static BeanData primaryBean() {
    return beanData("primaryBean", "checks.spring.context.PrimaryBean", new TextSpan(9, 13, 9, 24), true, null,
      Map.of(), Set.of("checks.spring.context.PrimaryBean", "java.lang.Object"));
  }

  private static BeanData beanWithDependencies() {
    Map<String, Set<InjectionPoint>> injectionPoints = new LinkedHashMap<>();
    injectionPoints.put("org.springframework.context.ApplicationContext",
      Set.of(injectionPoint("primaryContext", new TextSpan(16, 2, 16, 55))));
    injectionPoints.put("org.springframework.core.env.Environment",
      Set.of(injectionPoint("environment", new TextSpan(19, 2, 19, 38))));
    return beanData("qualifiedFieldDependencies", "checks.spring.context.QualifiedFieldDependencies",
      new TextSpan(12, 6, 12, 32), false, "prod", injectionPoints,
      Set.of("checks.spring.context.QualifiedFieldDependencies"));
  }

  private static InjectionPoint injectionPoint(String name, TextSpan span) {
    return new InjectionPoint(name, new BeanLocation(INPUT_FILE, span));
  }

  /**
   * Builds a bean the way the gatherer does: {@code dependingBeans} is the name-only projection of the injection
   * points, which is what {@link BeanDataTypeAdapter} recomputes when reading an entry back rather than serializing it.
   */
  private static BeanData beanData(String beanName, String type, TextSpan span, boolean isPrimary, @Nullable String profiles,
    Map<String, Set<InjectionPoint>> injectionPoints, Set<String> typeHierarchy) {
    return new BeanData(beanName, type, "checks.spring.context", INPUT_FILE, span, isPrimary, profiles,
      BeanDefinitionGatherer.projectToNames(injectionPoints), injectionPoints, typeHierarchy);
  }

  // ---- Cache plumbing -------------------------------------------------------

  private static String writeBeans(List<BeanData> beans) {
    JavaWriteCache writeCache = mock(JavaWriteCache.class);
    SpringContextCacheHelper.writeBeanDefinitionsToCache(writeContext(writeCache), LOG, beans);
    return captureWrittenData(writeCache);
  }

  private static Optional<List<BeanData>> readBeans(String content) {
    return SpringContextCacheHelper.readBeanDefinitionsFromCache(
      readContext(content, BEAN_CACHE_KEY, mock(JavaWriteCache.class)), LOG);
  }

  private static String writePackages(List<String> packages) {
    JavaWriteCache writeCache = mock(JavaWriteCache.class);
    SpringContextCacheHelper.writeComponentScanPackagesToCache(writeContext(writeCache), LOG, packages);
    return captureWrittenData(writeCache);
  }

  private static Optional<List<String>> readPackages(String content) {
    return SpringContextCacheHelper.readComponentScanPackagesFromCache(
      readContext(content, PACKAGES_CACHE_KEY, mock(JavaWriteCache.class)), LOG);
  }

  private static String captureWrittenData(JavaWriteCache writeCache) {
    var dataCaptor = ArgumentCaptor.forClass(byte[].class);
    verify(writeCache).write(anyString(), dataCaptor.capture());
    return new String(dataCaptor.getValue(), StandardCharsets.UTF_8);
  }

  private static JavaFileScannerContext writeContext(JavaWriteCache writeCache) {
    CacheContext cacheContext = mockCacheContext(mock(JavaReadCache.class), writeCache);
    var context = mock(JavaFileScannerContext.class);
    when(context.getInputFile()).thenReturn(INPUT_FILE);
    when(context.getCacheContext()).thenReturn(cacheContext);
    return context;
  }

  private static InputFileScannerContext readContext(@Nullable String content, String cacheKey, JavaWriteCache writeCache) {
    JavaReadCache readCache = mock(JavaReadCache.class);
    if (content != null) {
      when(readCache.readBytes(cacheKey)).thenReturn(content.getBytes(StandardCharsets.UTF_8));
    }
    CacheContext cacheContext = mockCacheContext(readCache, writeCache);
    var context = mock(InputFileScannerContext.class);
    when(context.getInputFile()).thenReturn(INPUT_FILE);
    when(context.getCacheContext()).thenReturn(cacheContext);
    return context;
  }
}
