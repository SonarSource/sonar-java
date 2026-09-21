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

import com.sonarsource.scanner.engine.sensor.test.fixtures.SensorContextTester;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
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

  @Test
  void componentScan_on_interface_is_collected() {
    scan("src/test/files/springcontext/ComponentScanOnInterface.java");

    assertThat(model.getProjectPackageScan().getPackagesForModule(MODULE_KEY))
      .containsExactly("checks.spring.context");
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
  void springBootApplication_mixed_scan_attributes_and_constants_are_collected() {
    scan("src/test/files/springcontext/SpringBootAppWithMixedScanAttributes.java");

    assertThat(model.getProjectPackageScan().getPackagesForModule(MODULE_KEY))
      .containsExactlyInAnyOrder("com.example.extra", "com.example.controller", "checks.spring.context");
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

  // ---- Caching: wiring only, the cache format is covered by SpringContextCacheHelperTest ----

  @Test
  void leaveFile_writes_collected_packages_to_cache() {
    WriteCache writeCache = mock(WriteCache.class);
    SensorContextTester ctx = SensorContextTester.create(new File(""));
    ctx.setCacheEnabled(true);
    ctx.setNextCache(writeCache);

    scan(ctx, "src/test/files/springcontext/SpringBootAppWithScanBasePackages.java");

    var dataCaptor = ArgumentCaptor.forClass(byte[].class);
    verify(writeCache).write(startsWith("java:spring:component-scan-packages:"), dataCaptor.capture());
    assertThat(new String(dataCaptor.getValue(), StandardCharsets.UTF_8))
      .contains("com.example.service", "com.example.web");
  }

  @Test
  void scanWithoutParsing_restores_packages_from_cache() {
    InputFile inputFile = TestUtils.inputFile(new File("src/test/files/springcontext/SpringBootAppWithScanBasePackages.java"));
    String cacheKey = "java:spring:component-scan-packages:" + inputFile.key();

    JavaReadCache readCache = mock(JavaReadCache.class);
    when(readCache.readBytes(cacheKey))
      .thenReturn("{\"version\":1,\"packages\":[\"com.example.service\",\"com.example.web\"]}".getBytes(StandardCharsets.UTF_8));
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
  void parsed_packages_replace_packages_restored_for_the_same_file() {
    String filePath = "src/test/files/springcontext/SpringBootAppWithScanBasePackages.java";
    InputFile inputFile = TestUtils.inputFile(new File(filePath));
    String cacheKey = "java:spring:component-scan-packages:" + inputFile.key();

    JavaReadCache readCache = mock(JavaReadCache.class);
    when(readCache.readBytes(cacheKey))
      .thenReturn("{\"version\":1,\"packages\":[\"cached.only\"]}".getBytes(StandardCharsets.UTF_8));
    CacheContext cacheContext = mockCacheContext(readCache, mock(JavaWriteCache.class));
    InputFileScannerContext context = mock(InputFileScannerContext.class);
    when(context.getInputFile()).thenReturn(inputFile);
    when(context.getCacheContext()).thenReturn(cacheContext);

    assertThat(gatherer.scanWithoutParsing(context)).isTrue();

    scan(filePath);

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
}
