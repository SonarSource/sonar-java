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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.caching.FileCachingCheck;
import org.sonar.java.utils.PackageUtils;
import org.sonar.java.utils.SpringUtils;
import org.sonar.plugins.java.api.InputFileScannerContext;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.ModuleScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;

/**
 * Collects packages registered for Spring component scanning and stores them in
 * {@link org.sonar.java.model.springcontext.ProjectPackageScan} within the shared
 * {@link SpringContextModel}.
 *
 * <p>Scanned packages are derived from:
 * <ul>
 *   <li>{@code @ComponentScan} attributes: {@code basePackages}, {@code basePackageClasses}, {@code value}</li>
 *   <li>{@code @SpringBootApplication} attributes: {@code scanBasePackages}, {@code scanBasePackageClasses}</li>
 *   <li>{@code @SpringBootApplication} without explicit attributes — the annotated class's own package</li>
 * </ul>
 *
 * <p>Packages are grouped by module and written to {@link org.sonar.java.model.springcontext.ProjectPackageScan}
 * at the end of each module's analysis. Per-file results are cached to speed up incremental analyses.
 */
public class ComponentScanPackageGatherer extends SpringContextModelGatherer implements FileCachingCheck<Set<String>> {

  private static final String CACHE_KEY_PREFIX = "java:spring:component-scan-packages:";

  /**
   * Packages accumulated across all files in the current module.
   */
  private final Set<String> collectedPackages = new HashSet<>();

  /**
   * Packages found in the file currently being scanned, used for per-file cache writes.
   */
  private final Set<String> packagesCollectedAtFileLevel = new HashSet<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS, Tree.Kind.INTERFACE);
  }

  @Override
  public boolean scanWithoutParsing(InputFileScannerContext inputFileScannerContext) {
    return restoreFromCache(inputFileScannerContext);
  }

  @Override
  public String cacheKeyPrefix() {
    return CACHE_KEY_PREFIX;
  }

  @Override
  public byte[] serialize(Set<String> packages) {
    return SpringContextCacheHelper.serializeComponentScanPackages(packages);
  }

  @Override
  public Set<String> deserialize(InputFile inputFile, byte[] data) {
    return SpringContextCacheHelper.deserializeComponentScanPackages(data);
  }

  @Override
  public void restore(InputFileScannerContext context, Set<String> packages) {
    collectedPackages.addAll(packages);
  }

  @Override
  public void setContext(JavaFileScannerContext context) {
    packagesCollectedAtFileLevel.clear();
    super.setContext(context);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (classTree.simpleName() == null) {
      return;
    }

    SymbolMetadata metadata = classTree.symbol().metadata();
    collectFromComponentScan(metadata);
    collectFromSpringBootApplication(classTree.symbol(), metadata);
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    writeToCache(context, packagesCollectedAtFileLevel);
    packagesCollectedAtFileLevel.clear();
  }

  @Override
  public void gatherSpringContextData(ModuleScannerContext context, SpringContextModel springContextModel) {
    springContextModel.getProjectPackageScan().addPackages(context.getModuleKey(), collectedPackages);
  }

  private void collectFromComponentScan(SymbolMetadata metadata) {
    SpringUtils.componentScanBaseAttributes(metadata).stream()
      .map(SpringUtils::packagesFromAnnotationValue)
      .forEach(this::collect);
  }

  /**
   * Collects the packages a {@code @SpringBootApplication} class registers for scanning.
   *
   * <p>The annotation's implicit "scan my own package" behaviour is suppressed once something already
   * contributed a package for this file, so that an explicit {@code @ComponentScan} on the same class
   * wins over the implicit fallback.
   */
  private void collectFromSpringBootApplication(Symbol classSymbol, SymbolMetadata metadata) {
    if (!metadata.isAnnotatedWith(SpringUtils.SPRING_BOOT_APP_ANNOTATION)) {
      return;
    }
    collect(SpringUtils.springBootApplicationScanPackages(
      PackageUtils.packageNameOf(classSymbol), metadata, packagesCollectedAtFileLevel.isEmpty()));
  }

  private void collect(List<String> packages) {
    packages.stream()
      .filter(packageName -> !packageName.isBlank())
      .forEach(packageName -> {
        collectedPackages.add(packageName);
        packagesCollectedAtFileLevel.add(packageName);
      });
  }

}
