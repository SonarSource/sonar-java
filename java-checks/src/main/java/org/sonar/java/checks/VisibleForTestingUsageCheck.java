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
import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5803")
public class VisibleForTestingUsageCheck extends IssuableSubscriptionVisitor {

  private static final List<String> ANNOTATIONS = Arrays.asList(
    "com.google.common.annotations.VisibleForTesting",
    "org.assertj.core.util.VisibleForTesting",
    "androidx.annotation.VisibleForTesting",
    "org.apache.flink.annotation.VisibleForTesting"
  );

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.IDENTIFIER);
  }

  @Override
  public void visitNode(Tree tree) {
    IdentifierTree identifier = (IdentifierTree) tree;
    Symbol symbol = identifier.symbol();
    SymbolMetadata metadata = symbol.metadata();
    boolean inTheSameFile = symbol.declaration() != null;
    Symbol owner = symbol.owner();
    if (owner != null && owner.isTypeSymbol() && !inTheSameFile
      && (ANNOTATIONS.stream().anyMatch(metadata::isAnnotatedWith))) {
      reportIssue(identifier, String.format("Remove this usage of \"%s\", it is annotated with @VisibleForTesting and should not be accessed from production code.",
        identifier.name()));
    }
  }
}
