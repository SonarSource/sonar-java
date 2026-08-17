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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.SpringUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5673")
public class SpringComponentSpecializationCheck extends IssuableSubscriptionVisitor {

  private static final Set<String> SPECIALIZED_STEREOTYPE_ANNOTATIONS = Set.of(
    SpringUtils.CONTROLLER_ANNOTATION,
    SpringUtils.REST_CONTROLLER_ANNOTATION,
    SpringUtils.SERVICE_ANNOTATION,
    SpringUtils.REPOSITORY_ANNOTATION);

  private static final List<String> REQUEST_MAPPING_ANNOTATIONS = List.of(
    "org.springframework.web.bind.annotation.RequestMapping",
    "org.springframework.web.bind.annotation.GetMapping",
    "org.springframework.web.bind.annotation.PostMapping",
    "org.springframework.web.bind.annotation.PutMapping",
    "org.springframework.web.bind.annotation.DeleteMapping",
    "org.springframework.web.bind.annotation.PatchMapping");

  private static final List<String> NON_WEB_FRAMEWORK_INTERFACES = List.of(
    "org.springframework.boot.ApplicationRunner",
    "org.springframework.boot.CommandLineRunner",
    "org.springframework.boot.actuate.health.HealthIndicator",
    "org.springframework.boot.actuate.health.ReactiveHealthIndicator");

  private static final String CONTROLLER = "Controller";
  private static final String REST_CONTROLLER = "RestController";

  private static final List<String> NON_WEB_FRAMEWORK_ANNOTATIONS = List.of(
    "org.springframework.boot.actuate.endpoint.annotation.Endpoint",
    "org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint",
    "org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpoint");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS, Tree.Kind.INTERFACE);
  }

  @Override
  public void visitNode(Tree tree) {
    var classTree = (ClassTree) tree;

    Optional<AnnotationTree> componentAnnotation = classTree.modifiers().annotations().stream()
      .filter(a -> SpringUtils.COMPONENT_ANNOTATION.equals(a.annotationType().symbolType().fullyQualifiedName()))
      .findFirst();

    if (componentAnnotation.isEmpty()) {
      return;
    }

    if (hasSpecializedStereotypeAnnotation(classTree)) {
      return;
    }

    String className = classTree.simpleName().name();
    String suggestedAnnotation = getSuggestedAnnotation(className);

    if (suggestedAnnotation != null && shouldRaise(suggestedAnnotation, classTree)) {
      reportIssue(componentAnnotation.get(), String.format("Use @%s instead of @Component, or rename this type if the @Component annotation is intentional", suggestedAnnotation));
    }
  }

  private static boolean hasSpecializedStereotypeAnnotation(ClassTree classTree) {
    return classTree.modifiers().annotations().stream()
      .anyMatch(a -> SPECIALIZED_STEREOTYPE_ANNOTATIONS.contains(a.annotationType().symbolType().fullyQualifiedName()));
  }

  private static boolean shouldRaise(String suggestedAnnotation, ClassTree classTree) {
    if (CONTROLLER.equals(suggestedAnnotation) || REST_CONTROLLER.equals(suggestedAnnotation)) {
      return hasRequestMappingMethod(classTree) && !implementsNonWebFrameworkInterface(classTree) && !hasNonWebFrameworkAnnotation(classTree);
    }
    return true;
  }

  private static boolean hasRequestMappingMethod(ClassTree classTree) {
    for (Tree member : classTree.members()) {
      if (member instanceof MethodTree method) {
        for (AnnotationTree annotation : method.modifiers().annotations()) {
          if (REQUEST_MAPPING_ANNOTATIONS.contains(annotation.annotationType().symbolType().fullyQualifiedName())) {
            return true;
          }
        }
      }
    }
    for (Type superType : classTree.symbol().superTypes()) {
      if (hasRequestMappingMethodInSymbol(superType.symbol())) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasRequestMappingMethodInSymbol(Symbol.TypeSymbol typeSymbol) {
    return typeSymbol.memberSymbols().stream()
      .filter(Symbol::isMethodSymbol)
      .anyMatch(method -> REQUEST_MAPPING_ANNOTATIONS.stream().anyMatch(method.metadata()::isAnnotatedWith));
  }

  private static boolean implementsNonWebFrameworkInterface(ClassTree classTree) {
    Type classType = classTree.symbol().type();
    if (classType == null) {
      return false;
    }
    for (String interfaceFqn : NON_WEB_FRAMEWORK_INTERFACES) {
      if (classType.isSubtypeOf(interfaceFqn)) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasNonWebFrameworkAnnotation(ClassTree classTree) {
    return classTree.modifiers().annotations().stream()
      .anyMatch(a -> NON_WEB_FRAMEWORK_ANNOTATIONS.contains(a.annotationType().symbolType().fullyQualifiedName()));
  }

  @CheckForNull
  private static String getSuggestedAnnotation(String className) {
    // Check RestController first to avoid false matches with Controller
    if (endsWithIgnoreCase(className, REST_CONTROLLER) || endsWithIgnoreCase(className, REST_CONTROLLER + "Impl")) {
      return REST_CONTROLLER;
    }

    if (endsWithIgnoreCase(className, CONTROLLER) || endsWithIgnoreCase(className, CONTROLLER + "Impl")) {
      return CONTROLLER;
    }

    if (endsWithIgnoreCase(className, "Service") ||
        endsWithIgnoreCase(className, "ServiceImpl") ||
        endsWithIgnoreCase(className, "ServiceFacade")) {
      return "Service";
    }

    if (endsWithIgnoreCase(className, "Repository") ||
        endsWithIgnoreCase(className, "RepositoryImpl") ||
        endsWithIgnoreCase(className, "Dao")) {
      return "Repository";
    }

    return null;
  }

  private static boolean endsWithIgnoreCase(String str, String suffix) {
    if (str.length() < suffix.length()) {
      return false;
    }
    return str.substring(str.length() - suffix.length()).equalsIgnoreCase(suffix);
  }

}
