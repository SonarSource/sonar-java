/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.collect.ImmutableList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.model.SyntacticEquivalence;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S1942")
public class SimpleClassNameCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Replace this fully qualified name with \"%s\"";
  private Set<Tree> hasIssue = new HashSet<>();
  private Set<Tree> importedWithWildcardNames = new HashSet<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Kind.IMPORT, Kind.MEMBER_SELECT);
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    super.scanFile(context);
    hasIssue.clear();
    importedWithWildcardNames.clear();
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }

    if (tree.is(Kind.IMPORT)) {
      visitImport((ImportTree)tree);

    } else {
      visitMemberSelect((MemberSelectExpressionTree)tree);
    }
  }

  private void visitMemberSelect(MemberSelectExpressionTree tree) {
    if (!isInImport(tree) && !hasIssue.contains(tree) && !tree.identifier().symbol().isPackageSymbol()) {

      for (Tree importedWildcard : importedWithWildcardNames) {
        if (SyntacticEquivalence.areEquivalent(importedWildcard, tree.expression())) {
          reportIssue(tree, String.format(MESSAGE, tree.identifier().name()));
          return;
        }

      }
    }

  }

  private static boolean isInImport(Tree tree) {
    Tree parent = tree.parent();

    while (parent != null) {
      if (parent.is(Kind.IMPORT)) {
        return true;
      }
      parent = parent.parent();
    }

    return false;
  }

  private void visitImport(ImportTree tree) {
    SemanticModel semanticModel = (SemanticModel) context.getSemanticModel();
    Symbol symbol = semanticModel.getSymbol(tree);

    if (symbol != null) {
      checkImportedSymbol(symbol);

    } else {
      importedWithWildcardNames.add(((MemberSelectExpressionTree)tree.qualifiedIdentifier()).expression());
    }
  }

  private void checkImportedSymbol(Symbol symbol) {
    for (IdentifierTree usageIdentifier : symbol.usages()) {
      Tree parent = usageIdentifier.parent();

      if (parent.is(Kind.MEMBER_SELECT) && ((MemberSelectExpressionTree) parent).expression().is(Kind.MEMBER_SELECT)) {
        reportIssue(parent, String.format(MESSAGE, symbol.name()));
        hasIssue.add(parent);
      }
    }
  }

}
