/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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

import java.util.Arrays;
import java.util.List;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;

public class VisibleForTestingUsageCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.IDENTIFIER);
  }

  @Override
  public void visitNode(Tree tree) {
    IdentifierTree identifier = (IdentifierTree) tree;

    Symbol symbol = identifier.symbol();
    Symbol owner = symbol.owner();
    SymbolMetadata metadata = owner.metadata();
    if (metadata.isAnnotatedWith("com.google.common.annotations.VisibleForTesting")) {
      reportIssue(identifier, "Remove this usage of \"" + identifier.name() + "\", it is annotated with @VisibleForTesting and should not be accessed from production code.");
    }
//
//    boolean variableSymbol = identifier.symbol().isVariableSymbol();
//    System.out.println(name + ": " + variableSymbol);
  }
}
