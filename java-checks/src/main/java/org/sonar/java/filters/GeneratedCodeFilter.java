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
package org.sonar.java.filters;

import java.util.Set;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.AnnotationInstance;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;

public class GeneratedCodeFilter extends AnyRuleIssueFilter {

  private static final Set<String> GENERATED_ANNOTATIONS = Set.of(
    "javax.annotation.Generated",
    "javax.annotation.processing.Generated",
    "jakarta.annotation.Generated",
    "jakarta.annotation.processing.Generated"
  );

  @Override
  public void scanFile(JavaFileScannerContext context) {
    if (context.getSemanticModel() == null) {
      // Filter requires semantic
      return;
    }
    super.scanFile(context);
  }

  @Override
  public void visitClass(ClassTree tree) {
    if (isGenerated(tree.symbol())) {
      excludeLines(tree);
    }
    super.visitClass(tree);
  }

  @Override
  public void visitMethod(MethodTree tree) {
    if (isGenerated(tree.symbol())) {
      excludeLines(tree);
    }
    super.visitMethod(tree);
  }

  private static boolean isGenerated(Symbol symbol) {
    return symbol.metadata().annotations().stream()
      .map(AnnotationInstance::symbol)
      .map(Symbol::type)
      .map(Type::fullyQualifiedName)
      .anyMatch(GENERATED_ANNOTATIONS::contains);
  }
}
