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
package org.sonar.java.checks.spring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.check.Rule;
import org.sonar.java.caching.FileCachingCheck;
import org.sonar.java.caching.JsonCacheFormat;
import org.sonar.java.utils.PackageUtils;
import org.sonar.java.utils.SpringUtils;
import org.sonar.java.model.DefaultJavaFileScannerContext;
import org.sonar.java.model.DefaultModuleScannerContext;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.plugins.java.api.InputFileScannerContext;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.ModuleScannerContext;
import org.sonar.plugins.java.api.internal.EndOfAnalysis;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4605")
public class SpringBeansShouldBeAccessibleCheck extends IssuableSubscriptionVisitor implements EndOfAnalysis, FileCachingCheck<Set<String>> {

  private static final String MESSAGE_FORMAT = "'%s' is not reachable by @ComponentScan or @SpringBootApplication. "
    + "Either move it to a package configured in @ComponentScan or update your @ComponentScan configuration.";

  private static final String[] SPRING_BEAN_ANNOTATIONS = {
    SpringUtils.COMPONENT_ANNOTATION,
    SpringUtils.SERVICE_ANNOTATION,
    SpringUtils.REPOSITORY_ANNOTATION,
    SpringUtils.CONTROLLER_ANNOTATION,
    SpringUtils.REST_CONTROLLER_ANNOTATION
  };

  private static final String CACHE_KEY_PREFIX = "java:S4605:targeted:";
  private static final int CACHE_FORMAT_VERSION = 1;
  private static final String PACKAGES = "packages";

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
  public byte[] serialize(Set<String> targetedPackages) {
    var document = JsonCacheFormat.newDocument(CACHE_FORMAT_VERSION);
    document.add(PACKAGES, JsonCacheFormat.strings(targetedPackages));
    return JsonCacheFormat.toBytes(document);
  }

  @Override
  public Set<String> deserialize(InputFile inputFile, byte[] data) {
    var document = JsonCacheFormat.parseDocument(data, CACHE_FORMAT_VERSION);
    return JsonCacheFormat.stringSet(JsonCacheFormat.requiredArray(document, PACKAGES));
  }

  @Override
  public void restore(InputFileScannerContext context, Set<String> targetedPackages) {
    packagesScannedBySpringAtProjectLevel.addAll(targetedPackages);
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

    String classPackageName = PackageUtils.packageNameOf(classTree.symbol());
    SymbolMetadata classSymbolMetadata = classTree.symbol().metadata();

    // try to apply "direct" annotation first
    if (!handledByComponentScan(classSymbolMetadata)) {
      if (hasAnnotation(classSymbolMetadata, SpringUtils.SPRING_BOOT_APP_ANNOTATION)) {
        // apply scan setting from @SpringBootApplication annotation
        var targetedPackages = SpringUtils.springBootApplicationScanPackages(classPackageName, classSymbolMetadata, true);
        packagesScannedBySpringAtProjectLevel.addAll(targetedPackages);
        packagesScannedBySpringAtFileLevel.addAll(targetedPackages);
      } else if (hasAnnotation(classSymbolMetadata, SPRING_BEAN_ANNOTATIONS)) {
        // include this class as a candidate for issue reporting
        addMessageToMap(classPackageName, classTree.simpleName());
      }
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
    writeToCache(context, packagesScannedBySpringAtFileLevel);
    packagesScannedBySpringAtFileLevel.clear();
  }

  private boolean handledByComponentScan(SymbolMetadata classSymbolMetadata) {
    List<SymbolMetadata.AnnotationValue> componentScanBaseAttributes = SpringUtils.componentScanBaseAttributes(classSymbolMetadata);
    componentScanBaseAttributes.stream()
      .map(SpringUtils::packagesFromAnnotationValue)
      .forEach(packagesScannedBySpringAtProjectLevel::addAll);
    return !componentScanBaseAttributes.isEmpty();
  }

  private void addMessageToMap(String classPackageName, IdentifierTree classNameTree) {
    DefaultJavaFileScannerContext defaultContext = (DefaultJavaFileScannerContext) context;
    AnalyzerMessage analyzerMessage = defaultContext.createAnalyzerMessage(this, classNameTree, String.format(MESSAGE_FORMAT, classNameTree.name()));
    messagesPerPackage.computeIfAbsent(classPackageName, k -> new ArrayList<>()).add(analyzerMessage);
  }

  private static boolean hasAnnotation(SymbolMetadata classSymbolMetadata, String... annotationName) {
    return Arrays.stream(annotationName).anyMatch(classSymbolMetadata::isAnnotatedWith);
  }
}
