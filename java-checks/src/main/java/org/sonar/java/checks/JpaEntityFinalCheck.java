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
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifierKeywordTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S8947")
public class JpaEntityFinalCheck extends IssuableSubscriptionVisitor {

  private static final List<String> ENTITY_ANNOTATIONS = List.of(
    "javax.persistence.Entity",
    "jakarta.persistence.Entity",
    "javax.persistence.MappedSuperclass",
    "jakarta.persistence.MappedSuperclass"
  );

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS, Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.CLASS)) {
      visitClass((ClassTree) tree);
    } else {
      visitMethod((MethodTree) tree);
    }
  }

  private void visitClass(ClassTree classTree) {
    if (!isJpaEntity(classTree.symbol().metadata())) {
      return;
    }
    ModifierKeywordTree finalModifier = ModifiersUtils.getModifier(classTree.modifiers(), Modifier.FINAL);
    if (finalModifier != null) {
      reportIssue(finalModifier, "Remove this \"final\" modifier from this JPA entity class.");
    }
  }

  private void visitMethod(MethodTree methodTree) {
    ModifierKeywordTree finalModifier = ModifiersUtils.getModifier(methodTree.modifiers(), Modifier.FINAL);
    if (finalModifier == null) {
      return;
    }
    Symbol methodSymbol = methodTree.symbol();
    if (methodSymbol.isStatic() || methodSymbol.isPrivate()) {
      return;
    }
    Symbol.TypeSymbol enclosingClass = methodSymbol.enclosingClass();
    if (enclosingClass != null && isJpaEntity(enclosingClass.metadata())) {
      reportIssue(finalModifier, "Remove this \"final\" modifier from this JPA entity method.");
    }
  }

  private static boolean isJpaEntity(SymbolMetadata metadata) {
    return ENTITY_ANNOTATIONS.stream().anyMatch(metadata::isAnnotatedWith);
  }
}
