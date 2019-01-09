/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.EndOfAnalysisCheck;
import org.sonar.java.checks.helpers.ConstantUtils;
import org.sonar.java.model.DefaultJavaFileScannerContext;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4605")
public class SpringBeansShouldBeAccessibleCheck extends IssuableSubscriptionVisitor implements EndOfAnalysisCheck {

  private static final String MESSAGE_FORMAT = "'%s' is not reachable by @ComponentsScan or @SpringBootApplication. "
    + "Either move it to a package configured in @ComponentsScan or update your @ComponentsScan configuration.";

  private static final String[] SPRING_BEAN_ANNOTATIONS = {
    "org.springframework.stereotype.Component",
    "org.springframework.stereotype.Service",
    "org.springframework.stereotype.Repository",
    "org.springframework.stereotype.Controller",
    "org.springframework.web.bind.annotation.RestController"
  };

  private static final String COMPONENT_SCAN_ANNOTATION = "org.springframework.context.annotation.ComponentScan";
  private static final Set<String> COMPONENT_SCAN_ARGUMENTS = new HashSet<>(Arrays.asList("basePackages", "value"));

  private static final String SPRING_BOOT_APP_ANNOTATION = "org.springframework.boot.autoconfigure.SpringBootApplication";

  /**
   * The key is the package name.
   * The value is a list of messages which are independent of Syntax Trees (to avoid memory leaks).
   */
  private final Map<String, List<AnalyzerMessage>> messagesPerPackage = new HashMap<>();
  /**
   * These are the packages that will be scanned by Spring in search of components
   */
  private final Set<String> packagesScannedBySpring = new HashSet<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void endOfAnalysis() {
    DefaultJavaFileScannerContext defaultContext = (DefaultJavaFileScannerContext) context;
    messagesPerPackage.entrySet().stream()
      // support sub-packages
      .filter(entry -> packagesScannedBySpring.stream().noneMatch(entry.getKey()::contains))
      .forEach(entry -> entry.getValue().forEach(defaultContext::reportIssue));
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;

    if (!hasSemantic() || classTree.simpleName() == null) {
      return;
    }

    String classPackageName = packageNameOf(classTree.symbol());
    SymbolMetadata classSymbolMetadata = classTree.symbol().metadata();

    List<SymbolMetadata.AnnotationValue> componentScanValues = classSymbolMetadata.valuesForAnnotation(COMPONENT_SCAN_ANNOTATION);
    if (componentScanValues != null) {
      componentScanValues.forEach(this::addToScannedPackages);
    } else if (hasAnnotation(classSymbolMetadata, SPRING_BOOT_APP_ANNOTATION)) {
      packagesScannedBySpring.add(classPackageName);
    } else if (hasAnnotation(classSymbolMetadata, SPRING_BEAN_ANNOTATIONS)) {
      addMessageToMap(classPackageName, classTree.simpleName());
    }
  }

  private void addMessageToMap(String classPackageName, IdentifierTree classNameTree) {
    DefaultJavaFileScannerContext defaultContext = (DefaultJavaFileScannerContext) context;
    AnalyzerMessage analyzerMessage = defaultContext.createAnalyzerMessage(this, classNameTree, String.format(MESSAGE_FORMAT, classNameTree.name()));
    messagesPerPackage.computeIfAbsent(classPackageName, k -> new ArrayList<>()).add(analyzerMessage);
  }

  private void addToScannedPackages(SymbolMetadata.AnnotationValue annotationValue) {
    if (COMPONENT_SCAN_ARGUMENTS.contains(annotationValue.name()) && annotationValue.value() instanceof ExpressionTree) {
      ExpressionTree values = (ExpressionTree) annotationValue.value();
      if (values.is(Tree.Kind.STRING_LITERAL)) {
        String packageName = ConstantUtils.resolveAsStringConstant(values);
        packagesScannedBySpring.add(packageName);
      } else if (values.is(Tree.Kind.NEW_ARRAY)) {
        for (ExpressionTree p : ((NewArrayTree) values).initializers()) {
          String packageName = ConstantUtils.resolveAsStringConstant(p);
          packagesScannedBySpring.add(packageName);
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
