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

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S8909")
public class CacheKeyGeneratorInstantiableCheck extends IssuableSubscriptionVisitor {

  private static final String CACHE_KEY_GENERATOR = "io.quarkus.cache.CacheKeyGenerator";

  private static final List<String> CDI_SCOPE_ANNOTATIONS = List.of(
    "jakarta.enterprise.context.ApplicationScoped",
    "jakarta.enterprise.context.Dependent",
    "jakarta.enterprise.context.RequestScoped",
    "jakarta.enterprise.context.SessionScoped",
    "jakarta.enterprise.context.ConversationScoped",
    "javax.enterprise.context.ApplicationScoped",
    "javax.enterprise.context.Dependent",
    "javax.enterprise.context.RequestScoped",
    "javax.enterprise.context.SessionScoped",
    "javax.enterprise.context.ConversationScoped"
  );

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (!isApplicableClass(classTree)) {
      return;
    }
    if (hasCdiScopeAnnotation(classTree) || hasPublicNoArgsConstructor(classTree)) {
      return;
    }
    reportIssue(Objects.requireNonNull(classTree.simpleName()),
      "Make this class a CDI bean by adding a scope annotation, or add a public no-args constructor.");
  }

  private static boolean isApplicableClass(ClassTree classTree) {
    return !isAnonymous(classTree)
      && !classTree.symbol().isAbstract()
      && !classTree.symbol().isInterface()
      && implementsCacheKeyGenerator(classTree);
  }

  private static boolean isAnonymous(ClassTree classTree) {
    return classTree.simpleName() == null;
  }

  private static boolean implementsCacheKeyGenerator(ClassTree classTree) {
    return classTree.symbol().type().isSubtypeOf(CACHE_KEY_GENERATOR);
  }

  private static boolean hasCdiScopeAnnotation(ClassTree classTree) {
    return CDI_SCOPE_ANNOTATIONS.stream()
      .anyMatch(annotation -> classTree.symbol().metadata().isAnnotatedWith(annotation));
  }

  private static boolean hasPublicNoArgsConstructor(ClassTree classTree) {
    Collection<Symbol> constructors = classTree.symbol().lookupSymbols("<init>");
    var noArgConstructor = constructors.stream()
      .filter(CacheKeyGeneratorInstantiableCheck::isNoArgConstructor)
      .findFirst();

    if (noArgConstructor.isEmpty()) {
      return false;
    }

    Symbol constructor = noArgConstructor.get();
    if (constructor.declaration() == null) {
      return classTree.symbol().isPublic();
    }
    return constructor.isPublic();
  }

  private static boolean isNoArgConstructor(Symbol constructor) {
    return ((Symbol.MethodSymbol) constructor).parameterTypes().isEmpty();
  }
}
