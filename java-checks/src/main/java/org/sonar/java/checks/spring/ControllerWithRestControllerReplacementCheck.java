/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.checks.helpers.SpringUtils;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6833")
public class ControllerWithRestControllerReplacementCheck extends IssuableSubscriptionVisitor {
  private static final String RESPONSE_BODY = "org.springframework.web.bind.annotation.ResponseBody";
  private static final List<String> MAPPING_ANNOTATIONS = List.of(
    "org.springframework.web.bind.annotation.RequestMapping",
    "org.springframework.web.bind.annotation.GetMapping",
    "org.springframework.web.bind.annotation.PostMapping",
    "org.springframework.web.bind.annotation.PutMapping",
    "org.springframework.web.bind.annotation.PatchMapping",
    "org.springframework.web.bind.annotation.DeleteMapping");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    var classTree = (ClassTree) tree;

    var annotation = classTree.modifiers().annotations().stream()
      .filter(a -> SpringUtils.CONTROLLER_ANNOTATION.equals(a.annotationType().symbolType().fullyQualifiedName()))
      .findFirst();

    if (annotation.isEmpty()) {
      return;
    }

    List<AnnotationTree> responseBodyOnMethods = new ArrayList<>();

    for (Tree member : classTree.members()) {
      if (member instanceof MethodTree method) {

        var response = firstAnnotation(method, List.of(RESPONSE_BODY));
        if (response.isPresent()) {
          responseBodyOnMethods.add(response.get());
        } else if (firstAnnotation(method, MAPPING_ANNOTATIONS).isPresent()) {
          return;
        }
      }
    }

    var secondaryLocations = new ArrayList<JavaFileScannerContext.Location>();
    List<JavaTextEdit> edits = new ArrayList<>();

    responseBodyOnMethods
      .forEach(ann -> {
        secondaryLocations.add(new JavaFileScannerContext.Location("Remove this \"@ResponseBody\" annotation.", ann));
        edits.add(JavaTextEdit.removeTree(ann));
      });

    classTree.modifiers().annotations().stream()
      .filter(ControllerWithRestControllerReplacementCheck::isResponseBody)
      .forEach(annotationTree -> secondaryLocations.add(new JavaFileScannerContext.Location("Remove this \"@ResponseBody\" annotation.", annotationTree)));

    if (secondaryLocations.isEmpty()) {
      return;
    }

    QuickFixHelper.newIssue(context)
      .forRule(this)
      .onTree(annotation.get())
      .withMessage("Replace the \"@Controller\" annotation by \"@RestController\" and remove all \"@ResponseBody\" annotations.")
      .withSecondaries(secondaryLocations)
      .withQuickFixes(() -> List.of(JavaQuickFix.newQuickFix("Remove \"@ResponseBody\" annotations.").addTextEdits(edits).build(),
        JavaQuickFix.newQuickFix("Replace \"@Controller\" by \"@RestController\".").addTextEdit(JavaTextEdit.replaceTree(annotation.get(), "@RestController")).build()))
      .report();

  }

  private static boolean isResponseBody(AnnotationTree a) {
    return RESPONSE_BODY.equals(a.symbolType().fullyQualifiedName());
  }

  private static Optional<AnnotationTree> firstAnnotation(MethodTree method, List<String> annFullyQualifiedNames) {
    for (AnnotationTree annotation : method.modifiers().annotations()) {
      String fqn = annotation.symbolType().fullyQualifiedName();
      if (annFullyQualifiedNames.contains(fqn)) {
        return Optional.of(annotation);
      }
    }
    return Optional.empty();
  }

}
