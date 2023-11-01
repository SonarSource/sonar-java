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
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6837")
public class SuperfluousResponseBodyAnnotationCheck extends IssuableSubscriptionVisitor {
  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    var ct = (ClassTree) tree;
    if (!ct.symbol().metadata().isAnnotatedWith("org.springframework.web.bind.annotation.RestController")) {
      return;
    }

    ct.members().stream().filter(member -> member.is(Tree.Kind.METHOD)).forEach(member -> {
      var mt = (MethodTree) member;
      mt.modifiers().annotations().stream()
        .filter(annotationTree -> annotationTree.symbolType().is("org.springframework.web.bind.annotation.ResponseBody"))
        .findFirst()
        .ifPresent(annotationTree -> reportIssue(annotationTree, "Remove this superfluous \"@ResponseBody\" annotation."));
    });
  }
}
