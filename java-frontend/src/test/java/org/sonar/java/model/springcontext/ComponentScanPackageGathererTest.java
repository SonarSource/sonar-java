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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ComponentScanPackageGathererTest extends SpringContextGathererTest {

  private static final String MODULE_KEY = "";

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
  void anonymous_class_inside_springBootApplication_class_is_skipped() {
    scan("src/test/files/springcontext/SpringBootAppWithAnonymousClass.java");

    assertThat(model.getProjectPackageScan().getPackagesForModule(MODULE_KEY))
      .containsExactly("checks.spring.context");
  }

  @Test
  void springBootApplication_blank_scanBasePackage_is_ignored() {
    scan("src/test/files/springcontext/SpringBootAppWithBlankScanBasePackage.java");

    assertThat(model.getProjectPackageScan().getPackagesForModule(MODULE_KEY))
      .containsExactly("com.example.service");
  }

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

  // ---- Caching --------------------------------------------------------------

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {
    "src/test/files/springcontext/SpringBootAppWithScanBasePackages.java",
    "src/test/files/springcontext/NoScanAnnotations.java"
  })
  void packages_written_to_cache_are_restored_identically(String filePath) {
    gatherer = new ComponentScanPackageGatherer();
    model = new SpringContextModel();
    scan(filePath);
    var expected = model.getProjectPackageScan().getPackagesForModule(MODULE_KEY);

    WriteCache writeCache = mock(WriteCache.class);
    SensorContextTester ctx = SensorContextTester.create(new File(""));
    ctx.setCacheEnabled(true);
    ctx.setNextCache(writeCache);
    gatherer = new ComponentScanPackageGatherer();
    model = new SpringContextModel();
    scan(ctx, filePath);
    var dataCaptor = ArgumentCaptor.forClass(byte[].class);
    verify(writeCache).write(anyString(), dataCaptor.capture());

    InputFile inputFile = TestUtils.inputFile(new File(filePath));
    String cacheKey = "java:spring:component-scan-packages:" + inputFile.key();
    JavaReadCache readCache = mock(JavaReadCache.class);
    when(readCache.readBytes(cacheKey)).thenReturn(dataCaptor.getValue());
    JavaWriteCache javaWriteCache = mock(JavaWriteCache.class);
    CacheContext cacheContext = mockCacheContext(readCache, javaWriteCache);

    InputFileScannerContext context = mock(InputFileScannerContext.class);
    when(context.getInputFile()).thenReturn(inputFile);
    when(context.getCacheContext()).thenReturn(cacheContext);

    gatherer = new ComponentScanPackageGatherer();
    model = new SpringContextModel();
    assertThat(gatherer.scanWithoutParsing(context)).isTrue();
    verify(javaWriteCache).copyFromPrevious(cacheKey);

    ModuleScannerContext moduleScannerContext = mock(ModuleScannerContext.class);
    when(moduleScannerContext.getModuleKey()).thenReturn(MODULE_KEY);
    gatherer.gatherSpringContextData(moduleScannerContext, model);

    assertThat(model.getProjectPackageScan().getPackagesForModule(MODULE_KEY))
      .containsExactlyInAnyOrderElementsOf(expected);
  }

  @Test
  void leaveFile_writes_packages_to_cache() {
    WriteCache writeCache = mock(WriteCache.class);
    SensorContextTester ctx = SensorContextTester.create(new File(""));
    ctx.setCacheEnabled(true);
    ctx.setNextCache(writeCache);

    scan(ctx, "src/test/files/springcontext/SpringBootAppWithScanBasePackages.java");

    var dataCaptor = ArgumentCaptor.forClass(byte[].class);
    verify(writeCache).write(
      org.mockito.ArgumentMatchers.endsWith("SpringBootAppWithScanBasePackages.java"),
      dataCaptor.capture());
    var document = JsonParser.parseString(new String(dataCaptor.getValue(), StandardCharsets.UTF_8)).getAsJsonObject();
    assertThat(document.get("version").getAsInt()).isOne();
    assertThat(document.getAsJsonArray("packages")).extracting(JsonElement::getAsString)
      .containsExactlyInAnyOrder("com.example.service", "com.example.web");
  }

  @Test
  void scanWithoutParsing_returns_true_and_restores_packages_on_cache_hit() {
    InputFile inputFile = TestUtils.inputFile(new File("src/test/files/springcontext/SpringBootAppWithScanBasePackages.java"));
    String cacheKey = "java:spring:component-scan-packages:" + inputFile.key();

    JavaReadCache readCache = mock(JavaReadCache.class);
    when(readCache.readBytes(cacheKey)).thenReturn("{\"version\":1,\"packages\":[\"com.example.service\",\"com.example.web\"]}".getBytes(StandardCharsets.UTF_8));
    CacheContext cacheContext = mockCacheContext(readCache, mock(JavaWriteCache.class));

    InputFileScannerContext context = mock(InputFileScannerContext.class);
    when(context.getInputFile()).thenReturn(inputFile);
    when(context.getCacheContext()).thenReturn(cacheContext);

    assertThat(gatherer.scanWithoutParsing(context)).isTrue();

    ModuleScannerContext moduleScannerContext = mock(ModuleScannerContext.class);
    when(moduleScannerContext.getModuleKey()).thenReturn(MODULE_KEY);
    gatherer.gatherSpringContextData(moduleScannerContext, model);

    assertThat(model.getProjectPackageScan().getPackagesForModule(MODULE_KEY))
      .containsExactlyInAnyOrder("com.example.service", "com.example.web");
  }

  @Test
  void scanWithoutParsing_returns_false_on_cache_miss() {
    InputFile inputFile = TestUtils.inputFile(new File("src/test/files/springcontext/SpringBootAppWithScanBasePackages.java"));

    JavaReadCache readCache = mock(JavaReadCache.class);
    when(readCache.readBytes(anyString())).thenReturn(null);
    CacheContext cacheContext = mockCacheContext(readCache, mock(JavaWriteCache.class));

    InputFileScannerContext context = mock(InputFileScannerContext.class);
    when(context.getInputFile()).thenReturn(inputFile);
    when(context.getCacheContext()).thenReturn(cacheContext);

    assertThat(gatherer.scanWithoutParsing(context)).isFalse();
  }

  @Test
  void scanWithoutParsing_restores_empty_package_set_from_cache() {
    InputFile inputFile = TestUtils.inputFile(new File("src/test/files/springcontext/NoScanAnnotations.java"));
    String cacheKey = "java:spring:component-scan-packages:" + inputFile.key();

    JavaReadCache readCache = mock(JavaReadCache.class);
    when(readCache.readBytes(cacheKey)).thenReturn("{\"version\":1,\"packages\":[]}".getBytes(StandardCharsets.UTF_8));
    CacheContext cacheContext = mockCacheContext(readCache, mock(JavaWriteCache.class));

    InputFileScannerContext context = mock(InputFileScannerContext.class);
    when(context.getInputFile()).thenReturn(inputFile);
    when(context.getCacheContext()).thenReturn(cacheContext);

    assertThat(gatherer.scanWithoutParsing(context)).isTrue();

    ModuleScannerContext moduleScannerContext = mock(ModuleScannerContext.class);
    when(moduleScannerContext.getModuleKey()).thenReturn(MODULE_KEY);
    gatherer.gatherSpringContextData(moduleScannerContext, model);

    assertThat(model.getProjectPackageScan().getPackagesForModule(MODULE_KEY)).isEmpty();
  }

  @Test
  void duplicate_cache_write_IllegalArgumentException_is_caught() {
    WriteCache writeCache = mock(WriteCache.class);
    doThrow(new IllegalArgumentException("duplicate key")).when(writeCache).write(anyString(), any(byte[].class));
    SensorContextTester ctx = SensorContextTester.create(new File(""));
    ctx.setCacheEnabled(true);
    ctx.setNextCache(writeCache);

    assertThatCode(() -> scan(ctx, "src/test/files/springcontext/SpringBootAppWithScanBasePackages.java"))
      .doesNotThrowAnyException();
  }

  @Test
  void scanWithoutParsing_returns_false_on_corrupted_cache_entry() {
    InputFile inputFile = TestUtils.inputFile(new File("src/test/files/springcontext/SpringBootAppWithScanBasePackages.java"));
    String cacheKey = "java:spring:component-scan-packages:" + inputFile.key();

    JavaReadCache readCache = mock(JavaReadCache.class);
    when(readCache.readBytes(cacheKey)).thenReturn("com.example.service;com.example.web".getBytes(StandardCharsets.UTF_8));
    CacheContext cacheContext = mockCacheContext(readCache, mock(JavaWriteCache.class));

    InputFileScannerContext context = mock(InputFileScannerContext.class);
    when(context.getInputFile()).thenReturn(inputFile);
    when(context.getCacheContext()).thenReturn(cacheContext);

    assertThat(gatherer.scanWithoutParsing(context)).isFalse();
  }

}
