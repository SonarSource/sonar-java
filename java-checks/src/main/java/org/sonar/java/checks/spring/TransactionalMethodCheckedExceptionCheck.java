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
package org.sonar.java.checks.spring;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.SpringUtils;
import org.sonar.plugins.java.api.DependencyVersionAware;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.Version;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

@Rule(key = "S8989")
public class TransactionalMethodCheckedExceptionCheck extends IssuableSubscriptionVisitor implements DependencyVersionAware {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree method = (MethodTree) tree;

    SymbolMetadata effectiveAnnotation = getEffectiveTransactionalAnnotation(method);
    if (effectiveAnnotation == null) {
      return;
    }

    List<TypeTree> throwsClauses = method.throwsClauses();
    if (throwsClauses.isEmpty()) {
      return;
    }

    boolean hasCheckedExceptions = throwsClauses.stream()
      .map(TypeTree::symbolType)
      .anyMatch(this::isCheckedException);

    if (!hasCheckedExceptions) {
      return;
    }

    if (hasRollbackConfiguration(effectiveAnnotation)) {
      return;
    }

    reportIssue(
      method.simpleName(),
      "Specify rollback behavior for checked exceptions using \"rollbackFor\" or \"noRollbackFor\" attributes."
    );
  }

  private SymbolMetadata getEffectiveTransactionalAnnotation(MethodTree method) {
    Symbol.MethodSymbol methodSymbol = method.symbol();
    SymbolMetadata methodMetadata = methodSymbol.metadata();

    if (methodMetadata.isAnnotatedWith(SpringUtils.TRANSACTIONAL_ANNOTATION)) {
      return methodMetadata;
    }

    Symbol.TypeSymbol classSymbol = methodSymbol.enclosingClass();
    if (classSymbol != null) {
      SymbolMetadata classMetadata = classSymbol.metadata();
      if (classMetadata.isAnnotatedWith(SpringUtils.TRANSACTIONAL_ANNOTATION)) {
        return classMetadata;
      }
    }

    return null;
  }

  private boolean isCheckedException(Type type) {
    if (type.isUnknown()) {
      return false;
    }

    return type.isSubtypeOf("java.lang.Exception")
      && !type.isSubtypeOf("java.lang.RuntimeException")
      && !type.isSubtypeOf("java.lang.Error");
  }

  private boolean hasRollbackConfiguration(SymbolMetadata metadata) {
    List<SymbolMetadata.AnnotationValue> values = metadata.valuesForAnnotation(SpringUtils.TRANSACTIONAL_ANNOTATION);
    if (values == null) {
      return false;
    }

    return values.stream()
      .anyMatch(av -> "rollbackFor".equals(av.name())
        || "rollbackForClassName".equals(av.name())
        || "noRollbackFor".equals(av.name())
        || "noRollbackForClassName".equals(av.name()));
  }

  @Override
  public boolean isCompatibleWithDependencies(Function<String, Optional<Version>> dependencyFinder) {
    Optional<Version> springContextVersion = dependencyFinder.apply("spring-context");
    Optional<Version> springTxVersion = dependencyFinder.apply("spring-tx");
    return springTxVersion.isPresent() || springContextVersion.isPresent();
  }
}
