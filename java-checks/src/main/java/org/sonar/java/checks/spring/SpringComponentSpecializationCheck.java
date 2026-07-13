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
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.SpringUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5673")
public class SpringComponentSpecializationCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS);
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

    String className = classTree.simpleName().name();
    String suggestedAnnotation = getSuggestedAnnotation(className);

    if (suggestedAnnotation != null) {
      reportIssue(componentAnnotation.get(), String.format("Use @%s instead of @Component", suggestedAnnotation));
    }
  }

  @CheckForNull
  private static String getSuggestedAnnotation(String className) {
    // Check RestController first to avoid false matches with Controller
    if (endsWithIgnoreCase(className, "RestController") || endsWithIgnoreCase(className, "RestControllerImpl")) {
      return "RestController";
    }

    if (endsWithIgnoreCase(className, "Controller") || endsWithIgnoreCase(className, "ControllerImpl")) {
      return "Controller";
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
