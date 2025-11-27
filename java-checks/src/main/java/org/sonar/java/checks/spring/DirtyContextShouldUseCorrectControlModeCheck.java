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
package org.sonar.java.checks.spring;

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S7177")
public class DirtyContextShouldUseCorrectControlModeCheck extends IssuableSubscriptionVisitor {

  private static final String DIRTY_CONTEXT = "org.springframework.test.annotation.DirtiesContext";
  private static final String REPLACE_CLASS_MODE = "Replace classMode with methodMode.";
  private static final String REPLACE_METHOD_MODE = "Replace methodMode with classMode.";
  private static final String CLASS_MODE = "classMode";
  private static final String METHOD_MODE = "methodMode";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD, Tree.Kind.CLASS, Tree.Kind.RECORD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree instanceof MethodTree method) {
      forEachDirtyContextArguments(method.modifiers(), CLASS_MODE, REPLACE_CLASS_MODE);
    } else {
      ClassTree clazz = (ClassTree) tree;
      forEachDirtyContextArguments(clazz.modifiers(), METHOD_MODE, REPLACE_METHOD_MODE);
    }
  }

  private void forEachDirtyContextArguments(ModifiersTree modifiers, String targetedArgument, String issueMessage) {
    for (AnnotationTree ann : modifiers.annotations()) {
      if (ann.symbolType().is(DIRTY_CONTEXT)) {
        for (ExpressionTree expr : ann.arguments()) {
          var assign = (AssignmentExpressionTree) expr;
          var ident = (IdentifierTree) assign.variable();
          if (ident.name().equals(targetedArgument)) {
            reportIssue(ident, issueMessage);
          }
        }
      }
    }
  }
}
