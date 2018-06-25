/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
import java.util.StringJoiner;
import org.sonar.check.Rule;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.AnalyzerMessageReporter;
import org.sonar.java.EndOfAnalysisCheck;
import org.sonar.java.checks.helpers.ConstantUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
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
   * The key is the class fully qualified name prefix, which includes the name of the package
   * The value is a list of messages which are independent of Syntax Trees (to avoid keeping references to all ASTs in all files)
   */
  private final Map<String, List<AnalyzerMessage>> messagesPerClassPrefix = new HashMap<>();
  private final Set<String> scannedPackages = new HashSet<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void endOfAnalysis() {
    AnalyzerMessageReporter reporter = (AnalyzerMessageReporter) context;
    messagesPerClassPrefix.entrySet().stream()
      // support sub-packages and inner classes
      .filter(entry -> scannedPackages.stream().noneMatch(entry.getKey()::contains))
      .forEach(entry -> entry.getValue().forEach(reporter::reportIssue));
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;

    if (!hasSemantic() || classTree.simpleName() == null) {
      return;
    }

    String classPackageName = extractClassPrefix(classTree.symbol().type().fullyQualifiedName());
    SymbolMetadata classSymbolMetadata = classTree.symbol().metadata();

    List<SymbolMetadata.AnnotationValue> componentScanValues = classSymbolMetadata.valuesForAnnotation(COMPONENT_SCAN_ANNOTATION);
    if (componentScanValues != null) {
      componentScanValues.forEach(this::addToScannedPackages);
    } else if (hasAnnotation(classSymbolMetadata, SPRING_BOOT_APP_ANNOTATION)) {
      scannedPackages.add(classPackageName);
    } else if (hasAnnotation(classSymbolMetadata, SPRING_BEAN_ANNOTATIONS)) {
      addMessageToMap(classPackageName, classTree.simpleName());
    }
  }

  private void addMessageToMap(String classPackageName, IdentifierTree classNameTree) {
    AnalyzerMessageReporter reporter = (AnalyzerMessageReporter) context;
    AnalyzerMessage analyzerMessage = reporter.createAnalyzerMessage(this, classNameTree, String.format(MESSAGE_FORMAT, classNameTree));
    messagesPerClassPrefix.computeIfAbsent(classPackageName, k -> new ArrayList<>()).add(analyzerMessage);
  }

  private void addToScannedPackages(SymbolMetadata.AnnotationValue annotationValue) {
    if (COMPONENT_SCAN_ARGUMENTS.contains(annotationValue.name()) && annotationValue.value() instanceof ExpressionTree) {
      ExpressionTree values = (ExpressionTree) annotationValue.value();
      if (values.is(Tree.Kind.STRING_LITERAL)) {
        String packageName = ConstantUtils.resolveAsStringConstant(values);
        scannedPackages.add(packageName);
      } else if (values.is(Tree.Kind.NEW_ARRAY)) {
        for (ExpressionTree p : ((NewArrayTree) values).initializers()) {
          String packageName = ConstantUtils.resolveAsStringConstant(p);
          scannedPackages.add(packageName);
        }
      }
    }
  }

  /**
   * Returns the prefix of the class name, which:
   * - in general, is the package name (e.g. 'foo.bar')
   * - for inner classes, is the package name + outer class name (e.g. 'foo.bar.Outer')
   * - for classes in the default package, is the empty string
   */
  private static String extractClassPrefix(String fullyQualifiedClassName) {
    // '$' sign is the delimiter for inner classes
    String[] nameGroup = fullyQualifiedClassName.split("\\$|\\.");
    StringJoiner stringJoiner = new StringJoiner(".");
    for (int i = 0; i < nameGroup.length - 1; i++) {
      stringJoiner.add(nameGroup[i]);
    }
    return stringJoiner.toString();
  }

  private static boolean hasAnnotation(SymbolMetadata classSymbolMetadata, String... annotationName) {
    return Arrays.stream(annotationName).anyMatch(classSymbolMetadata::isAnnotatedWith);
  }
}
