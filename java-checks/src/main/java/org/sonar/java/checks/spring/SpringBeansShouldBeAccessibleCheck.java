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
import java.util.Optional;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.AnalyzerMessageReporter;
import org.sonar.java.CrossFileScanner;
import org.sonar.java.checks.helpers.ConstantUtils;
import org.sonar.java.model.PackageUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4605")
public class SpringBeansShouldBeAccessibleCheck extends IssuableSubscriptionVisitor implements CrossFileScanner {

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

  private final Map<String, List<AnalyzerMessage>> messagesPerPackage = new HashMap<>();
  private final Set<String> scannedPackages = new HashSet<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void endOfAnalysis() {
    if (!(context instanceof AnalyzerMessageReporter)) {
      return;
    }

    AnalyzerMessageReporter reporter = (AnalyzerMessageReporter) context;
    messagesPerPackage.entrySet().stream()
      // also consider sub-packages
      .filter(entry -> scannedPackages.stream().noneMatch(entry.getKey()::contains))
      .forEach(entry -> entry.getValue().forEach(reporter::reportIssue));
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic() || !(context instanceof AnalyzerMessageReporter)) {
      return;
    }

    ClassTree classTree = (ClassTree) tree;
    String classPackageName = packageNameOf(classTree);

    Optional<AnnotationTree> componentScanAnnotation = classTree.modifiers().annotations().stream()
      .filter(SpringBeansShouldBeAccessibleCheck::isComponentScan).findFirst();
    if (componentScanAnnotation.isPresent()) {
      componentScanAnnotation.get().arguments().forEach(this::addToScannedPackages);
    } else if (hasAnnotation(classTree, SPRING_BOOT_APP_ANNOTATION)) {
      scannedPackages.add(classPackageName);
    } else if (hasAnnotation(classTree, SPRING_BEAN_ANNOTATIONS)) {
      addMessageToMap(classPackageName, classTree);
    }
  }

  private void addMessageToMap(String classPackageName, ClassTree classTree) {
    AnalyzerMessageReporter reporter = (AnalyzerMessageReporter) context;
    IdentifierTree className = classTree.simpleName();
    if (className != null) {
      AnalyzerMessage analyzerMessage = reporter.createAnalyzerMessage(this, className, String.format(MESSAGE_FORMAT, className));
      messagesPerPackage.computeIfAbsent(classPackageName, k -> new ArrayList<>());
      messagesPerPackage.get(classPackageName).add(analyzerMessage);
    }
  }

  private void addToScannedPackages(ExpressionTree annotationArgument) {
    if (annotationArgument.is(Tree.Kind.ASSIGNMENT)) {
      AssignmentExpressionTree argumentAssignment = (AssignmentExpressionTree) annotationArgument;
      ExpressionTree argumentAssignmentVar = argumentAssignment.variable();
      if (argumentAssignmentVar.is(Tree.Kind.IDENTIFIER) && COMPONENT_SCAN_ARGUMENTS.contains(((IdentifierTree) argumentAssignmentVar).name())) {
        addLiteralsToScannedPackages(argumentAssignment.expression());
      }
    } else {
      addLiteralsToScannedPackages(annotationArgument);
    }
  }

  private void addLiteralsToScannedPackages(ExpressionTree packageNames) {
    if (packageNames.is(Tree.Kind.STRING_LITERAL)) {
      String name = ConstantUtils.resolveAsStringConstant(packageNames);
      scannedPackages.add(name);
    } else if (packageNames.is(Tree.Kind.NEW_ARRAY)) {
      for (ExpressionTree p : ((NewArrayTree) packageNames).initializers()) {
        String name = ConstantUtils.resolveAsStringConstant(p);
        scannedPackages.add(name);
      }
    }
  }

  private static String packageNameOf(ClassTree classTree) {
    Tree classTreeParent = classTree.parent();
    if (classTreeParent != null && classTreeParent.is(Tree.Kind.COMPILATION_UNIT)) {
      return PackageUtils.packageName(((CompilationUnitTree) classTreeParent).packageDeclaration(), ".");
    }
    return "";
  }

  private static boolean hasAnnotation(ClassTree classTree, String... annotationName) {
    return Arrays.stream(annotationName).anyMatch(annotation -> classTree.symbol().metadata().isAnnotatedWith(annotation));
  }

  private static boolean isComponentScan(AnnotationTree annotation) {
    return annotation.annotationType().symbolType().fullyQualifiedName().equals(COMPONENT_SCAN_ANNOTATION);
  }

}
