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
package org.sonar.java.checks;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6862")
public class ConfigurationBeanNamesCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    var classTree = (ClassTree) tree;
    if (!isConfigurationClass(classTree)) {
      return;
    }

    var beanMethods = getBeanMethods(classTree);
    var foundNames = new HashSet<String>();
    for (MethodTree beanMethod : beanMethods) {
      if (!foundNames.add(beanMethod.simpleName().name())) {
        reportIssue(beanMethod.simpleName(), "Rename this bean method to prevent any conflict with other beans.");
      }
    }
  }

  private static boolean isConfigurationClass(ClassTree classTree) {
    return classTree.symbol().metadata().isAnnotatedWith("org.springframework.context.annotation.Configuration");
  }

  private static List<MethodTree> getBeanMethods(ClassTree classTree) {
    return classTree.members().stream()
      .filter(member -> member.is(Tree.Kind.METHOD))
      .map(MethodTree.class::cast)
      .filter(method -> method.symbol().metadata().isAnnotatedWith("org.springframework.context.annotation.Bean"))
      .collect(Collectors.toList());
  }

}
