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

import java.util.List;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6829")
public class AutowiredOnConstructorWhenMultipleConstructorsCheck extends IssuableSubscriptionVisitor {

  private final List<String> annotations = List.of(
    "org.springframework.context.annotation.Bean",
    "org.springframework.context.annotation.Configuration",
    "org.springframework.stereotype.Component",
    "org.springframework.stereotype.Controller",
    "org.springframework.stereotype.Repository",
    "org.springframework.stereotype.Service");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;

    boolean isSpringClass = classTree.modifiers().annotations().stream()
      .anyMatch(annotation -> annotations.contains(annotation.symbolType().fullyQualifiedName()));
    if (!isSpringClass) {
      return;
    }

    var constructors = classTree.members().stream()
      .filter(member -> member.is(Tree.Kind.CONSTRUCTOR))
      .map(MethodTree.class::cast)
      .collect(Collectors.toList());

    if (constructors.size() > 1) {
      boolean anyHasAutowired = constructors.stream()
        .anyMatch(constructor -> constructor.modifiers().annotations().stream()
          .anyMatch(annotation -> annotation.symbolType().is("org.springframework.beans.factory.annotation.Autowired")));

      if (!anyHasAutowired) {
        reportIssue(classTree.simpleName(), "Add @Autowired to one of the constructors.");
      }
    }
  }

}
