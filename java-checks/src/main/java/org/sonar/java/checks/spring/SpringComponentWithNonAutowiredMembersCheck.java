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

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S3749")
public class SpringComponentWithNonAutowiredMembersCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree clazzTree = (ClassTree) tree;
    SymbolMetadata clazzMeta = clazzTree.symbol().metadata();

    if (isSpringComponent(clazzMeta)) {
      clazzTree.members().stream().filter(v -> v.is(Kind.VARIABLE))
        .map(m -> (VariableTree) m)
        .filter(v -> !v.symbol().isStatic())
        .filter(v -> !isSpringInjectionAnnotated(v.symbol().metadata()))
        .forEach(v -> reportIssue(v.simpleName(), "Annotate this member with \"@Autowired\", \"@Resource\", \"@Inject\", or \"@Value\", or remove it."));
    }
  }

  private static boolean isSpringInjectionAnnotated(SymbolMetadata metadata) {
    return metadata.isAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
      || metadata.isAnnotatedWith("javax.inject.Inject")
      || metadata.isAnnotatedWith("javax.annotation.Resource")
      || metadata.isAnnotatedWith("org.springframework.beans.factory.annotation.Value");
  }

  private static boolean isSpringComponent(SymbolMetadata clazzMeta) {
    return clazzMeta.isAnnotatedWith("org.springframework.stereotype.Controller")
      || clazzMeta.isAnnotatedWith("org.springframework.stereotype.Service")
      || clazzMeta.isAnnotatedWith("org.springframework.stereotype.Repository");
  }
}
