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
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S8954")
public class BeanValidationConstraintOnStaticFieldCheck extends IssuableSubscriptionVisitor {

  private static final Set<String> CONSTRAINT_META_ANNOTATIONS = Set.of(
    "javax.validation.Constraint",
    "jakarta.validation.Constraint");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.VARIABLE);
  }

  @Override
  public void visitNode(Tree tree) {
    VariableTree variable = (VariableTree) tree;
    if (!variable.symbol().isStatic()) {
      return;
    }
    boolean hasConstraint = variable.modifiers().annotations().stream()
      .anyMatch(BeanValidationConstraintOnStaticFieldCheck::isBeanValidationConstraint);
    if (hasConstraint) {
      ModifiersUtils.findModifier(variable.modifiers(), Modifier.STATIC)
        .ifPresent(staticModifier -> reportIssue(staticModifier, "Remove the \"static\" modifier from this field."));
    }
  }

  private static boolean isBeanValidationConstraint(AnnotationTree annotation) {
    var annotationType = annotation.annotationType().symbolType();
    if (annotationType.isUnknown()) {
      return false;
    }
    SymbolMetadata metadata = annotationType.symbol().metadata();
    return CONSTRAINT_META_ANNOTATIONS.stream().anyMatch(metadata::isAnnotatedWith);
  }
}
