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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.utils.PackageUtils;
import org.sonar.java.utils.SpringUtils;
import org.sonar.plugins.java.api.InputFileScannerContext;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.ModuleScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.analyzer.commons.collections.SetUtils;

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
public class ComponentScanPackageGatherer extends SpringContextModelGatherer {

  private static final Logger LOG = LoggerFactory.getLogger(ComponentScanPackageGatherer.class);

  private static final String CACHE_KEY_PREFIX = "java:spring:component-scan-packages:";

  private static final String COMPONENT_SCAN_ANNOTATION = "org.springframework.context.annotation.ComponentScan";
  private static final Set<String> COMPONENT_SCAN_BASE_ARGUMENTS = SetUtils.immutableSetOf("basePackages", "basePackageClasses", "value");
  private static final Set<String> SCAN_BASE_ANNOTATIONS = SetUtils.immutableSetOf("scanBasePackages", "scanBasePackageClasses");

  /** Packages accumulated across all files in the current module. */
  private final Set<String> collectedPackages = new HashSet<>();

  /** Packages found in the file currently being scanned, used for per-file cache writes. */
  private final Set<String> packagesCollectedAtFileLevel = new HashSet<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS, Tree.Kind.INTERFACE);
  }

  @Override
  public boolean scanWithoutParsing(InputFileScannerContext inputFileScannerContext) {
    return readFromCache(inputFileScannerContext).map(packages -> {
      collectedPackages.addAll(packages);
      return true;
    }).orElse(false);
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
    if (context.getCacheContext().isCacheEnabled()) {
      writeToCache(context, packagesCollectedAtFileLevel);
    }
    packagesCollectedAtFileLevel.clear();
  }

  @Override
  public void gatherSpringContextData(ModuleScannerContext context, SpringContextModel springContextModel) {
    springContextModel.getProjectPackageScan().addPackages(context.getModuleKey(), collectedPackages);
  }

  private void collectFromComponentScan(SymbolMetadata metadata) {
    List<SymbolMetadata.AnnotationValue> componentScanAttributes = metadata.valuesForAnnotation(COMPONENT_SCAN_ANNOTATION);
    if (componentScanAttributes == null) {
      return;
    }
    componentScanAttributes.stream()
      .filter(v -> COMPONENT_SCAN_BASE_ARGUMENTS.contains(v.name()))
      .forEach(this::addAnnotationValueToCollectedPackages);
  }

  private void collectFromSpringBootApplication(Symbol classSymbol, SymbolMetadata metadata) {
    if (!metadata.isAnnotatedWith(SpringUtils.SPRING_BOOT_APP_ANNOTATION)) {
      return;
    }
    var packages = targetedPackages(PackageUtils.packageNameOf(classSymbol), metadata, packagesCollectedAtFileLevel.isEmpty());
    collectedPackages.addAll(packages);
    packagesCollectedAtFileLevel.addAll(packages);
  }

  private static List<String> targetedPackages(String classPackageName, SymbolMetadata metadata, boolean useOwnPackageAsFallback) {
    var scanBaseValues = Objects.requireNonNull(metadata.valuesForAnnotation(SpringUtils.SPRING_BOOT_APP_ANNOTATION)).stream()
      .filter(v -> SCAN_BASE_ANNOTATIONS.contains(v.name()) && v.value() instanceof Object[])
      .toList();

    List<String> packages = new ArrayList<>();
    for (SymbolMetadata.AnnotationValue value : scanBaseValues) {
      boolean isClassBased = "scanBasePackageClasses".equals(value.name());
      for (Object element : (Object[]) value.value()) {
        resolvePackage(element, isClassBased).ifPresent(packages::add);
      }
    }
    if (scanBaseValues.isEmpty()) {
      // Without explicit scan attributes, @SpringBootApplication scans its own package — but only if
      // no packages were already collected via @ComponentScan on the same class.
      return (useOwnPackageAsFallback && !classPackageName.isBlank()) ? Collections.singletonList(classPackageName) : List.of();
    }
    return packages;
  }

  private void addAnnotationValueToCollectedPackages(SymbolMetadata.AnnotationValue annotationValue) {
    if (annotationValue.value() instanceof Object[] objects) {
      for (Object o : objects) {
        if (o instanceof String oString && !oString.isBlank()) {
          collectedPackages.add(oString);
          packagesCollectedAtFileLevel.add(oString);
        } else if (o instanceof Symbol oSymbol) {
          var pkg = PackageUtils.packageNameOf(oSymbol);
          if (!pkg.isBlank()) {
            collectedPackages.add(pkg);
            packagesCollectedAtFileLevel.add(pkg);
          }
        }
      }
    }
  }

  private static Optional<String> resolvePackage(Object element, boolean isClassBased) {
    if (!isClassBased && element instanceof String s && !s.isBlank()) {
      return Optional.of(s);
    } else if (isClassBased && element instanceof Symbol s) {
      var pkg = PackageUtils.packageNameOf(s);
      return pkg.isBlank() ? Optional.empty() : Optional.of(pkg);
    }
    return Optional.empty();
  }

  private static String cacheKey(InputFile inputFile) {
    return CACHE_KEY_PREFIX + inputFile.key();
  }

  private static void writeToCache(InputFileScannerContext context, Set<String> packages) {
    var cacheKey = cacheKey(context.getInputFile());
    var data = String.join(";", packages).getBytes(StandardCharsets.UTF_8);
    try {
      context.getCacheContext().getWriteCache().write(cacheKey, data);
    } catch (IllegalArgumentException e) {
      LOG.trace("Tried to write multiple times to cache key '{}'. Ignoring writes after the first.", cacheKey);
    }
  }

  private static Optional<List<String>> readFromCache(InputFileScannerContext context) {
    var cacheKey = cacheKey(context.getInputFile());
    var bytes = context.getCacheContext().getReadCache().readBytes(cacheKey);
    if (bytes != null) {
      context.getCacheContext().getWriteCache().copyFromPrevious(cacheKey);
      String content = new String(bytes, StandardCharsets.UTF_8);
      if (content.isEmpty()) {
        return Optional.of(List.of());
      }
      return Optional.of(Arrays.asList(content.split(";")));
    }
    return Optional.empty();
  }
}
