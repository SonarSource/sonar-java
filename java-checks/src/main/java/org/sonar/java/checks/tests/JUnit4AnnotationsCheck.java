/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java.checks.tests;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MapBuilder;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5793")
public class JUnit4AnnotationsCheck extends IssuableSubscriptionVisitor {

  private static final Map<String, String> OLD_NEW_ANNOTATIONS_MAP = MapBuilder.<String, String>newMap()
    .add("org.junit.Test", "org.junit.jupiter.api.Test")
    .add("org.junit.Before", "org.junit.jupiter.api.BeforeEach")
    .add("org.junit.After", "org.junit.jupiter.api.AfterEach")
    .add("org.junit.BeforeClass", "org.junit.jupiter.api.BeforeAll")
    .add("org.junit.AfterClass", "org.junit.jupiter.api.AfterAll")
    .add("org.junit.Ignore", "org.junit.jupiter.api.Disabled")
    .add("org.junit.experimental.categories.Category", "org.junit.jupiter.api.Tag")
    .add("org.junit.Rule", "org.junit.jupiter.api.extension.ExtendWith")
    .add("org.junit.ClassRule", "org.junit.jupiter.api.extension.RegisterExtension")
    .add("org.junit.runner.RunWith", "org.junit.jupiter.api.extension.ExtendWith")
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.ANNOTATION);
  }

  @Override
  public void visitNode(Tree tree) {
    String qualifiedName = ((AnnotationTree) tree).annotationType().symbolType().fullyQualifiedName();
    if (OLD_NEW_ANNOTATIONS_MAP.containsKey(qualifiedName)) {
      reportIssue(tree, String.format("Change this JUnit4 %s to the equivalent JUnit5 %s annotation.",
        qualifiedName, OLD_NEW_ANNOTATIONS_MAP.get(qualifiedName)));
    }
  }
}
