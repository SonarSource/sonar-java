/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6833")
public class ControllerWithRestControllerReplacementCheck extends IssuableSubscriptionVisitor {

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

    var secondaryLocations = new ArrayList<JavaFileScannerContext.Location>();
    List<JavaTextEdit> edits = new ArrayList<>();

    classTree.members().stream()
      .filter(member -> member.is(Tree.Kind.METHOD))
      .map(MethodTree.class::cast)
      .forEach(method -> {
        var methodAnnotation = method.modifiers().annotations().stream()
          .filter(a -> "org.springframework.web.bind.annotation.ResponseBody".equals(a.annotationType().symbolType().fullyQualifiedName()))
          .findFirst();
        methodAnnotation.ifPresent(annotationTree -> secondaryLocations.add(new JavaFileScannerContext.Location("Remove this \"@ResponseBody\" annotation.", annotationTree)));
        methodAnnotation.ifPresent(annotationTree -> edits.add(JavaTextEdit.removeTree(annotationTree)));
      });

    classTree.modifiers().annotations().stream()
      .filter(a -> "org.springframework.web.bind.annotation.ResponseBody".equals(a.annotationType().symbolType().fullyQualifiedName()))
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

}
