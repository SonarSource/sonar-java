/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.java.resolve.SemanticModel;
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
import java.util.stream.Collectors;

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
    if (!hasSemantic()) {
      return;
    }
    CompilationUnitTree cut = (CompilationUnitTree) tree;
    cut.types().stream().filter(NOT_EMPTY_STATEMENT).map(t -> ((ClassTree) t).symbol()).forEach(this::checkSymbol);
    List<ImportTree> imports = cut.imports().stream().filter(NOT_EMPTY_STATEMENT).map(t -> (ImportTree) t).collect(Collectors.toList());
    boolean fileContainsStarImport = imports.stream()
      .filter(it -> it.qualifiedIdentifier().is(Kind.MEMBER_SELECT))
      .map(it -> ((MemberSelectExpressionTree) it.qualifiedIdentifier()).identifier())
      .anyMatch(i -> "*".equals(i.name()));
    if(!fileContainsStarImport) {
      checkImports(imports);
    }
  }

  private void checkImports(List<ImportTree> imports) {
    SemanticModel semanticModel = (SemanticModel) context.getSemanticModel();
    imports.stream()
      .map(semanticModel::getSymbol)
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
