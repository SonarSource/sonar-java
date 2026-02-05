/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;


/**
 * Checks that the "main" method has the correct signature for a program entry point.
 *
 * <p>Note, that even with a correct signature, the "main" method may not be valid entry point.
 * For example, it may be declared in an abstract class or an interface.
 */
@Rule(key = "S3051")
public class MainMethodSignatureCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "\"main\" method should only be used for the program entry point and should have appropriate signature.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    if ("main".equals(methodTree.simpleName().name())
      && !MethodTreeUtils.isMainMethod(methodTree, context.getJavaVersion())) {
      reportIssue(methodTree.simpleName(), MESSAGE);
    }
  }
}
