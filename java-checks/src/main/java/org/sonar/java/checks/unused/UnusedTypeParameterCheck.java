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
package org.sonar.java.checks.unused;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.TypeParameters;

@Rule(key = "S2326")
public class UnusedTypeParameterCheck extends IssuableSubscriptionVisitor {

  private static final String ISSUE_MESSAGE = "%s is not used in the %s.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.CLASS, Tree.Kind.INTERFACE, Tree.Kind.RECORD, Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    TypeParameters typeParameters = tree.is(Tree.Kind.METHOD) ? ((MethodTree) tree).typeParameters() : ((ClassTree) tree).typeParameters();
    for (TypeParameterTree typeParameter : typeParameters) {
      Symbol symbol = typeParameter.symbol();
      if (!symbol.isUnknown() && symbol.usages().isEmpty()) {
        String message = String.format(ISSUE_MESSAGE, symbol.name(), tree.kind().name().toLowerCase(Locale.ROOT));
        reportIssue(typeParameter.identifier(), message);
      }
    }
  }
}
