/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.checks.spring;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.check.Rule;
import org.sonar.java.model.DefaultJavaFileScannerContext;
import org.sonar.java.model.DefaultModuleScannerContext;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.plugins.java.api.InputFileScannerContext;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.ModuleScannerContext;
import org.sonar.plugins.java.api.internal.EndOfAnalysis;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.analyzer.commons.collections.SetUtils;

@Rule(key = "S4605")
public class SpringBeansShouldBeAccessibleCheck extends IssuableSubscriptionVisitor implements EndOfAnalysis {

  private static final Logger LOG = LoggerFactory.getLogger(SpringBeansShouldBeAccessibleCheck.class);

  private static final String MESSAGE_FORMAT = "'%s' is not reachable by @ComponentScan or @SpringBootApplication. "
    + "Either move it to a package configured in @ComponentScan or update your @ComponentScan configuration.";

  private static final String[] SPRING_BEAN_ANNOTATIONS = {
    "org.springframework.stereotype.Component",
    "org.springframework.stereotype.Service",
    "org.springframework.stereotype.Repository",
    "org.springframework.stereotype.Controller",
    "org.springframework.web.bind.annotation.RestController"
  };

  private static final String COMPONENT_SCAN_ANNOTATION = "org.springframework.context.annotation.ComponentScan";
  private static final Set<String> COMPONENT_SCAN_ARGUMENTS = SetUtils.immutableSetOf("basePackages", "value");

  private static final String SPRING_BOOT_APP_ANNOTATION = "org.springframework.boot.autoconfigure.SpringBootApplication";
  private static final String CACHE_KEY_PREFIX = "java:S4605:targeted:";

  /**
   * The key is the package name.
   * The value is a list of messages which are independent of Syntax Trees (to avoid memory leaks).
   */
  private final Map<String, List<AnalyzerMessage>> messagesPerPackage = new HashMap<>();
  /**
   * These are the packages that will be scanned by Spring in search of components
   */
  private final Set<String> packagesScannedBySpringAtProjectLevel = new HashSet<>();

  /**
   * Used to track the set of packages scanned by this file to cache when exiting the file.
   */
  private final Set<String> packagesScannedBySpringAtFileLevel = new HashSet<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public boolean scanWithoutParsing(InputFileScannerContext inputFileScannerContext) {
    return readFromCache(inputFileScannerContext).map(targetedPackages -> {
      packagesScannedBySpringAtProjectLevel.addAll(targetedPackages);
      return true;
    }).orElse(false);
  }

  @Override
  public void endOfAnalysis(ModuleScannerContext context) {
    var defaultContext = (DefaultModuleScannerContext) context;
    messagesPerPackage.entrySet().stream()
      // support sub-packages
      .filter(entry -> packagesScannedBySpringAtProjectLevel.stream().noneMatch(entry.getKey()::contains))
      .forEach(entry -> entry.getValue().forEach(defaultContext::reportIssue));
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;

    if (classTree.simpleName() == null) {
      return;
    }

    String classPackageName = packageNameOf(classTree.symbol());
    SymbolMetadata classSymbolMetadata = classTree.symbol().metadata();


    List<SymbolMetadata.AnnotationValue> componentScanValues = classSymbolMetadata.valuesForAnnotation(COMPONENT_SCAN_ANNOTATION);
    if (componentScanValues != null) {
      componentScanValues.forEach(this::addToScannedPackages);
    } else if (hasAnnotation(classSymbolMetadata, SPRING_BOOT_APP_ANNOTATION)) {
      var targetedPackages = targetedPackages(classPackageName, classSymbolMetadata);
      packagesScannedBySpringAtProjectLevel.addAll(targetedPackages);
      packagesScannedBySpringAtFileLevel.addAll(targetedPackages);
    } else if (hasAnnotation(classSymbolMetadata, SPRING_BEAN_ANNOTATIONS)) {
      addMessageToMap(classPackageName, classTree.simpleName());
    }
  }

  @Override
  public void setContext(JavaFileScannerContext context) {
    packagesScannedBySpringAtFileLevel.clear();
    super.setContext(context);
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    super.leaveFile(context);
    if (context.getCacheContext().isCacheEnabled()) {
      writeToCache(context, packagesScannedBySpringAtFileLevel);
    }
    packagesScannedBySpringAtFileLevel.clear();
  }

  private static String cacheKey(InputFile inputFile) {
    return CACHE_KEY_PREFIX + inputFile.key();
  }

  private static void writeToCache(InputFileScannerContext context, Collection<String> targetedPackages) {
    var cacheKey = cacheKey(context.getInputFile());
    var data = String.join(";", targetedPackages).getBytes(StandardCharsets.UTF_8);
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
      return Optional.of(Arrays.asList(new String(bytes, StandardCharsets.UTF_8).split(";")));
    } else {
      return Optional.empty();
    }
  }

  private static List<String> targetedPackages(String classPackageName, SymbolMetadata classSymbolMetadata) {
    // annotation is necessarily there already
    return Objects.requireNonNull(classSymbolMetadata.valuesForAnnotation(SPRING_BOOT_APP_ANNOTATION))
      .stream()
      .filter(v -> "scanBasePackages".equals(v.name()))
      .map(SymbolMetadata.AnnotationValue::value)
      .findFirst()
      // list of packages to scan
      .filter(Object[].class::isInstance)
      .map(Object[].class::cast)
      .map(SpringBeansShouldBeAccessibleCheck::asStringList)
      // Using this annotation without arguments tells Spring to scan the current package and all of its sub-packages.
      .orElse(Collections.singletonList(classPackageName));
  }

  private static List<String> asStringList(Object[] array) {
    return Arrays.asList(array)
      .stream()
      .filter(String.class::isInstance)
      .map(String.class::cast)
      .toList();
  }

  private void addMessageToMap(String classPackageName, IdentifierTree classNameTree) {
    DefaultJavaFileScannerContext defaultContext = (DefaultJavaFileScannerContext) context;
    AnalyzerMessage analyzerMessage = defaultContext.createAnalyzerMessage(this, classNameTree, String.format(MESSAGE_FORMAT, classNameTree.name()));
    messagesPerPackage.computeIfAbsent(classPackageName, k -> new ArrayList<>()).add(analyzerMessage);
  }

  private void addToScannedPackages(SymbolMetadata.AnnotationValue annotationValue) {
    if (!COMPONENT_SCAN_ARGUMENTS.contains(annotationValue.name())) {
      return;
    }
    if (annotationValue.value() instanceof Object[]) {
      for (Object o : (Object[]) annotationValue.value()) {
        if (o instanceof String) {
          packagesScannedBySpringAtProjectLevel.add((String) o);
        }
      }
    }
  }

  private static String packageNameOf(Symbol symbol) {
    Symbol owner = symbol.owner();
    while (!owner.isPackageSymbol()) {
      owner = owner.owner();
    }
    return owner.name();
  }

  private static boolean hasAnnotation(SymbolMetadata classSymbolMetadata, String... annotationName) {
    return Arrays.stream(annotationName).anyMatch(classSymbolMetadata::isAnnotatedWith);
  }
}
