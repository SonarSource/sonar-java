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
package org.sonar.java.checks;

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S8912")
public class CredentialsProviderUnremovableCheck extends IssuableSubscriptionVisitor {
  private static final String CREDENTIALS_PROVIDER_FQN = "io.quarkus.credentials.CredentialsProvider";
  private static final String UNREMOVABLE_FQN = "io.quarkus.arc.Unremovable";

  private static final List<String> CDI_SCOPE_ANNOTATIONS = List.of(
    "jakarta.enterprise.context.ApplicationScoped",
    "jakarta.enterprise.context.RequestScoped",
    "jakarta.enterprise.context.SessionScoped",
    "jakarta.enterprise.context.Dependent",
    "jakarta.inject.Singleton",
    "javax.enterprise.context.ApplicationScoped",
    "javax.enterprise.context.RequestScoped",
    "javax.enterprise.context.SessionScoped",
    "javax.enterprise.context.Dependent",
    "javax.inject.Singleton"
  );

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;

    if (classTree.simpleName() == null) {
      return;
    }

    if (!implementsCredentialsProvider(classTree)) {
      return;
    }

    SymbolMetadata metadata = classTree.symbol().metadata();

    if (!hasCdiScopeAnnotation(metadata)) {
      return;
    }

    if (metadata.isAnnotatedWith(UNREMOVABLE_FQN)) {
      return;
    }

    reportIssue(
      classTree.simpleName(),
      "Add the @Unremovable annotation to this CredentialsProvider implementation."
    );
  }

  private static boolean implementsCredentialsProvider(ClassTree classTree) {
    return classTree.symbol().type().isSubtypeOf(CREDENTIALS_PROVIDER_FQN);
  }

  private static boolean hasCdiScopeAnnotation(SymbolMetadata metadata) {
    return CDI_SCOPE_ANNOTATIONS.stream()
      .anyMatch(metadata::isAnnotatedWith);
  }
}
