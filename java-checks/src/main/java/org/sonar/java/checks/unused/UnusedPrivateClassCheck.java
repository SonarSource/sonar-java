/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.java.checks.unused;

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.UnresolvedIdentifiersVisitor;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S3985")
public class UnusedPrivateClassCheck extends IssuableSubscriptionVisitor {

  private static final UnresolvedIdentifiersVisitor UNRESOLVED_IDENTIFIERS_VISITOR = new UnresolvedIdentifiersVisitor();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.COMPILATION_UNIT, Tree.Kind.CLASS, Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE, Tree.Kind.ENUM);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.COMPILATION_UNIT)) {
      UNRESOLVED_IDENTIFIERS_VISITOR.check(tree);
      return;
    }
    ClassTree classTree = (ClassTree) tree;
    Symbol.TypeSymbol classSymbol = classTree.symbol();
    IdentifierTree identifierTree = classTree.simpleName();
    if(identifierTree != null && classSymbol.isPrivate()
      && classSymbol.usages().isEmpty() && !UNRESOLVED_IDENTIFIERS_VISITOR.isUnresolved(identifierTree.name())) {
      reportIssue(identifierTree, "Remove this unused private \""+ classSymbol.name()+"\" class.");
    }
  }
}
