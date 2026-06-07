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
package org.sonar.java.checks.spring.context;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.java.checks.helpers.HashCacheTestHelper;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.java.checks.verifier.internal.InternalReadCache;
import org.sonar.java.checks.verifier.internal.InternalWriteCache;
import org.sonar.java.model.springcontext.ComponentScanPackageGatherer;
import org.sonar.java.model.springcontext.SpringContextModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class ComponentScanPackageGathererTest {

  private static final String BASE_PATH = "checks/spring/context/";
  // Module key used by CheckVerifier's SonarComponents (no projectDefinition → empty string)
  private static final String MODULE_KEY = "";

  private SpringContextModel model;
  private ComponentScanPackageGatherer gatherer;
  private InternalReadCache readCache;
  private InternalWriteCache writeCache;

  @BeforeEach
  void setUp() {
    gatherer = new ComponentScanPackageGatherer();
    readCache = new InternalReadCache();
    writeCache = new InternalWriteCache().bind(readCache);
  }

  // ---- @ComponentScan -------------------------------------------------------

  @Test
  void componentScan_value_and_basePackages_attributes_are_collected() {
    scan(mainCodeSourcesPath(BASE_PATH + "ComponentScanWithBasePackages.java"));

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
  void springBootApplication_without_scan_attributes_collects_own_package() {
    scan(mainCodeSourcesPath(BASE_PATH + "SpringBootAppNoScanAttributes.java"));

    assertThat(model.getProjectPackageScan().getPackagesForModule(MODULE_KEY))
      .containsExactly("checks.spring.context");
  }

  @Test
  void springBootApplication_scanBasePackages_are_collected() {
    scan(mainCodeSourcesPath(BASE_PATH + "SpringBootAppWithScanBasePackages.java"));

    assertThat(model.getProjectPackageScan().getPackagesForModule(MODULE_KEY))
      .containsExactlyInAnyOrder("com.example.service", "com.example.web");
  }

  @Test
  void springBootApplication_scanBasePackageClasses_resolves_to_class_package() {
    scan(mainCodeSourcesPath(BASE_PATH + "SpringBootAppWithScanBasePackageClasses.java"));

    assertThat(model.getProjectPackageScan().getPackagesForModule(MODULE_KEY))
      .containsExactly("checks.spring.context");
  }

  @Test
  void componentScan_with_packages_overrides_springBootApplication_default_own_package() {
    scan(mainCodeSourcesPath(BASE_PATH + "SpringBootAppWithComponentScanPackages.java"));

    assertThat(model.getProjectPackageScan().getPackagesForModule(MODULE_KEY))
      .containsExactlyInAnyOrder("com.example.service", "com.example.web");
  }

  // ---- DependencyVersionAware -----------------------------------------------

  @Test
  void gatherer_is_skipped_when_spring_context_is_not_in_classpath() {
    CheckVerifier checkVerifier = CheckVerifier.newVerifier()
      .removeJarsFromClasspath("spring-context")
      .onFiles(mainCodeSourcesPath(BASE_PATH + "SpringBootAppNoScanAttributes.java"))
      .withCheck(gatherer);
    checkVerifier.verifyNoIssues();

    assertThat(checkVerifier.getSpringContextModel().getProjectPackageScan().getModules()).isEmpty();
  }

  // ---- No annotations -------------------------------------------------------

  @Test
  void no_scan_annotations_collects_nothing() {
    scan(mainCodeSourcesPath(BASE_PATH + "NoScanAnnotations.java"));

    assertThat(model.getProjectPackageScan().getPackagesForModule(MODULE_KEY)).isEmpty();
  }

  // ---- Multiple files -------------------------------------------------------

  @Test
  void packages_from_multiple_files_are_merged() {
    scan(
      mainCodeSourcesPath(BASE_PATH + "SpringBootAppNoScanAttributes.java"),
      mainCodeSourcesPath(BASE_PATH + "SpringBootAppWithScanBasePackages.java")
    );

    assertThat(model.getProjectPackageScan().getPackagesForModule(MODULE_KEY))
      .containsExactlyInAnyOrder("checks.spring.context", "com.example.service", "com.example.web");
  }

  // ---- Caching --------------------------------------------------------------

  @Test
  void packages_are_written_to_cache_after_scanning() {
    scan(mainCodeSourcesPath(BASE_PATH + "SpringBootAppWithScanBasePackages.java"));

    assertThat(writeCache.getData().keySet())
      .isNotEmpty()
      .anySatisfy(key -> assertThat(key).endsWith(BASE_PATH + "SpringBootAppWithScanBasePackages.java"));
  }

  @Test
  void scanWithoutParsing_populates_model_from_cache() throws NoSuchAlgorithmException, IOException {
    String filePath = mainCodeSourcesPath(BASE_PATH + "SpringBootAppWithScanBasePackages.java");

    // First pass: scan with a read cache that has the content hash
    ReadCache firstReadCache = HashCacheTestHelper.internalReadCacheFromFile(filePath);
    InternalWriteCache firstWriteCache = new InternalWriteCache().bind(firstReadCache);
    CheckVerifier checkVerifier = CheckVerifier.newVerifier()
      .withCache(firstReadCache, firstWriteCache)
      .onFiles(filePath)
      .withCheck(gatherer);
    checkVerifier.verifyNoIssues();

    assertThat(checkVerifier.getSpringContextModel().getProjectPackageScan().getPackagesForModule(MODULE_KEY))
      .containsExactlyInAnyOrder("com.example.service", "com.example.web");

    // Second pass: the file is SAME — scanWithoutParsing restores packages from cache
    var populatedReadCache = new InternalReadCache().putAll(firstWriteCache);
    var secondGatherer = new ComponentScanPackageGatherer();
    CheckVerifier secondCheckVerifier = CheckVerifier.newVerifier()
      .withCache(populatedReadCache, new InternalWriteCache().bind(populatedReadCache))
      .addFiles(InputFile.Status.SAME, filePath)
      .withCheck(secondGatherer);
    secondCheckVerifier.verifyNoIssues();

    assertThat(secondCheckVerifier.getSpringContextModel().getProjectPackageScan().getPackagesForModule(MODULE_KEY))
      .containsExactlyInAnyOrder("com.example.service", "com.example.web");
  }

  @Test
  void scanWithoutParsing_returns_false_on_cache_miss() throws NoSuchAlgorithmException, IOException {
    String filePath = mainCodeSourcesPath(BASE_PATH + "SpringBootAppWithScanBasePackages.java");

    // Read cache has content hash but no package data → cache miss for package key
    var readCacheWithHashOnly = HashCacheTestHelper.internalReadCacheFromFile(filePath);
    CheckVerifier checkVerifier = CheckVerifier.newVerifier()
      .withCache(readCacheWithHashOnly, new InternalWriteCache().bind(readCacheWithHashOnly))
      .addFiles(InputFile.Status.SAME, filePath)
      .withCheck(gatherer);
    checkVerifier.verifyNoIssues();

    // Full parse was forced, packages still collected correctly
    assertThat(checkVerifier.getSpringContextModel().getProjectPackageScan().getPackagesForModule(MODULE_KEY))
      .containsExactlyInAnyOrder("com.example.service", "com.example.web");
  }

  @Test
  void scanWithoutParsing_with_no_scan_annotations_restores_empty_packages_from_cache() throws NoSuchAlgorithmException, IOException {
    String filePath = mainCodeSourcesPath(BASE_PATH + "NoScanAnnotations.java");

    // First pass: scan and write empty package data to cache
    ReadCache firstReadCache = HashCacheTestHelper.internalReadCacheFromFile(filePath);
    InternalWriteCache firstWriteCache = new InternalWriteCache().bind(firstReadCache);
    CheckVerifier.newVerifier()
      .withCache(firstReadCache, firstWriteCache)
      .onFiles(filePath)
      .withCheck(gatherer)
      .verifyNoIssues();

    // Second pass: file is SAME — scanWithoutParsing must restore an empty package set, not [""]
    var populatedReadCache = new InternalReadCache().putAll(firstWriteCache);
    var secondGatherer = new ComponentScanPackageGatherer();
    CheckVerifier secondCheckVerifier = CheckVerifier.newVerifier()
      .withCache(populatedReadCache, new InternalWriteCache().bind(populatedReadCache))
      .addFiles(InputFile.Status.SAME, filePath)
      .withCheck(secondGatherer);
    secondCheckVerifier.verifyNoIssues();

    assertThat(secondCheckVerifier.getSpringContextModel().getProjectPackageScan().getPackagesForModule(MODULE_KEY)).isEmpty();
  }

  @Test
  void duplicate_cache_write_IllegalArgumentException_is_caught() {
    String filePath = mainCodeSourcesPath(BASE_PATH + "SpringBootAppWithScanBasePackages.java");

    var verifier = CheckVerifier.newVerifier()
      .withCache(readCache, writeCache)
      .onFiles(filePath)
      .withCheck(gatherer);
    verifier.verifyNoIssues();

    // Second run attempts to write the same cache key; the IllegalArgumentException must be caught internally
    assertThatCode(verifier::verifyNoIssues).doesNotThrowAnyException();
  }

  // ---- Helpers --------------------------------------------------------------

  private void scan(String... filePaths) {
    CheckVerifier checkVerifier = CheckVerifier.newVerifier()
      .withCache(readCache, writeCache)
      .onFiles(List.of(filePaths))
      .withCheck(gatherer);
    checkVerifier.verifyNoIssues();
    model = checkVerifier.getSpringContextModel();
  }
}
