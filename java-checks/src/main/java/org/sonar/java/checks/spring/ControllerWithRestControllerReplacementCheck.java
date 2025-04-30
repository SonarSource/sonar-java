/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    var classTree = (ClassTree) tree;

    var annotation = classTree.modifiers().annotations().stream()
      .filter(a -> "org.springframework.stereotype.Controller".equals(a.annotationType().symbolType().fullyQualifiedName()))
      .findFirst();

    if (annotation.isEmpty()) {
      return;
    }

    List<AnnotationTree> responseBodyOnMethods = new ArrayList<>();

    for(Tree member : classTree.members()) {
      if (member instanceof MethodTree method) {

        var response = getAnnotation(method, RESPONSE_BODY);
        response.ifPresent(responseBodyOnMethods::add);

        var mapping = getAnnotation(method, "org.springframework.web.bind.annotation.RequestMapping")
          .or(() -> getAnnotation(method, "org.springframework.web.bind.annotation.GetMapping"))
          .or(() -> getAnnotation(method, "org.springframework.web.bind.annotation.PostMapping"))
          .or(() -> getAnnotation(method, "org.springframework.web.bind.annotation.PutMapping"))
          .or(() -> getAnnotation(method, "org.springframework.web.bind.annotation.PatchMapping"))
          .or(() -> getAnnotation(method, "org.springframework.web.bind.annotation.DeleteMapping"));

        if(mapping.isPresent() && response.isEmpty()){
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
      .filter(a -> RESPONSE_BODY.equals(a.annotationType().symbolType().fullyQualifiedName()))
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

  private static Optional<AnnotationTree> getAnnotation(MethodTree method, String fullyQualifiedName){
    return method.modifiers().annotations().stream()
      .filter(a -> fullyQualifiedName.equals(a.annotationType().symbolType().fullyQualifiedName()))
      .findFirst();
  }

}
