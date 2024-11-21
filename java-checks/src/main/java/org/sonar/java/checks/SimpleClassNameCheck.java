/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

@Rule(key = "S1942")
public class SimpleClassNameCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Replace this fully qualified name with \"%s\"";
  private static final Predicate<Tree> NOT_EMPTY_STATEMENT = t -> !t.is(Kind.EMPTY_STATEMENT);

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Kind.COMPILATION_UNIT);
  }

  @Override
  public void visitNode(Tree tree) {
    CompilationUnitTree cut = (CompilationUnitTree) tree;
    cut.types().stream().filter(NOT_EMPTY_STATEMENT).map(t -> ((ClassTree) t).symbol()).forEach(this::checkSymbol);
    List<ImportTree> imports = cut.imports().stream().filter(NOT_EMPTY_STATEMENT).map(ImportTree.class::cast).toList();
    boolean fileContainsStarImport = imports.stream()
      .filter(it -> it.qualifiedIdentifier().is(Kind.MEMBER_SELECT))
      .map(it -> ((MemberSelectExpressionTree) it.qualifiedIdentifier()).identifier())
      .anyMatch(i -> "*".equals(i.name()));
    if(!fileContainsStarImport) {
      checkImports(imports);
    }
  }

  private void checkImports(List<ImportTree> imports) {
    imports.stream()
      .map(ImportTree::symbol)
      .filter(Objects::nonNull)
      .forEach(this::checkSymbol);
  }

  private void checkSymbol(Symbol symbol) {
    for (IdentifierTree usageIdentifier : symbol.usages()) {
      Tree parent = usageIdentifier.parent();

      if (parent.is(Kind.MEMBER_SELECT) && ((MemberSelectExpressionTree) parent).expression().is(Kind.MEMBER_SELECT)) {
        reportIssue(parent, String.format(MESSAGE, symbol.name()));
      }
    }
  }

}
