/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.quarkus;

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S9068")
public class SingletonInsteadOfApplicationScopedCheck extends IssuableSubscriptionVisitor {

  private static final String JAKARTA_SINGLETON = "jakarta.inject.Singleton";
  private static final String MESSAGE = "Replace \"@Singleton\" by \"@ApplicationScoped\" or add a comment indicating why \"@Singleton\" is necessary.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS, Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    ModifiersTree modifiers = tree instanceof ClassTree classTree
      ? classTree.modifiers()
      : ((MethodTree) tree).modifiers();

    modifiers.annotations().stream()
      .filter(annotation -> annotation.annotationType().symbolType().is(JAKARTA_SINGLETON))
      .filter(annotation -> !hasJustifyingComment(annotation))
      .forEach(annotation -> reportIssue(annotation, MESSAGE));
  }

  private static boolean hasJustifyingComment(AnnotationTree annotation) {
    return annotation.firstToken().trivias().stream()
      .anyMatch(trivia -> trivia.comment().toLowerCase(java.util.Locale.ROOT).contains("singleton"));
  }
}
