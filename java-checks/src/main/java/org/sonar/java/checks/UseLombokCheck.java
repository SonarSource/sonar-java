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
package org.sonar.java.checks;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.DependencyVersionAware;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.classpath.DependencyVersion;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;


@Rule(key = "S9999")
public class UseLombokCheck extends IssuableSubscriptionVisitor implements DependencyVersionAware {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    String name = ((MethodTree) tree).simpleName().name();
    if (name.startsWith("get")) {
      context.reportIssue(this, tree, "Consider using @Getter and @Setter from Lombok to reduce boilerplate.");
    }
  }

  @Override
  public boolean isCompatibleWithDependencies(BiFunction<String, String, Optional<DependencyVersion>> dependencyFinder) {
    return dependencyFinder.apply("org.projectlombok", "lombok").isPresent();
  }
}
