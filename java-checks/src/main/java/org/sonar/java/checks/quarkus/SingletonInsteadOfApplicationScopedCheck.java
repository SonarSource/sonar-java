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
import java.util.Locale;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ModifierKeywordTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
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
    ModifiersTree modifiers;
    SyntaxToken declarationToken;
    if (tree instanceof ClassTree classTree) {
      modifiers = classTree.modifiers();
      declarationToken = classTree.declarationKeyword();
    } else {
      MethodTree methodTree = (MethodTree) tree;
      modifiers = methodTree.modifiers();
      declarationToken = methodTree.returnType().firstToken();
    }

    modifiers.annotations().stream()
      .filter(annotation -> annotation.annotationType().symbolType().is(JAKARTA_SINGLETON))
      .filter(annotation -> !hasJustifyingComment(annotation, modifiers, declarationToken))
      .forEach(annotation -> reportIssue(annotation, MESSAGE));
  }

  private static boolean hasJustifyingComment(AnnotationTree annotation, ModifiersTree modifiers, SyntaxToken declarationToken) {
    // Comment appearing before the annotation (trivia on its @ token)
    if (containsSingletonComment(annotation.firstToken().trivias())) {
      return true;
    }
    // Comment after the annotation (inline or on the following line): it attaches as trivia to the
    // next token in the stream, which may be another annotation's @, a modifier keyword, or the
    // declaration keyword / return type.
    Stream<SyntaxToken> candidateTokens = Stream.concat(
      Stream.concat(
        modifiers.annotations().stream()
          .map(Tree::firstToken)
          .filter(t -> !t.equals(annotation.firstToken())),
        modifiers.modifiers().stream()
          .map(ModifierKeywordTree::keyword)
      ),
      Stream.of(declarationToken)
    );
    return candidateTokens
      .flatMap(token -> token.trivias().stream())
      .anyMatch(SingletonInsteadOfApplicationScopedCheck::isSingletonComment);
  }

  private static boolean containsSingletonComment(List<SyntaxTrivia> trivias) {
    return trivias.stream().anyMatch(SingletonInsteadOfApplicationScopedCheck::isSingletonComment);
  }

  private static boolean isSingletonComment(SyntaxTrivia trivia) {
    return trivia.comment().toLowerCase(Locale.ROOT).contains("singleton");
  }
}
