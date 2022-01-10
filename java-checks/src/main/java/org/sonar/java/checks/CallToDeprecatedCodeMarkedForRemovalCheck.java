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
package org.sonar.java.checks;

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;

@Rule(key = "S5738")
public class CallToDeprecatedCodeMarkedForRemovalCheck extends AbstractCallToDeprecatedCodeChecker {

  private static final String MESSAGE = "Remove this call to a deprecated %s, it has been marked for removal.";

  @Override
  void checkDeprecatedIdentifier(IdentifierTree identifierTree, Symbol deprecatedSymbol) {
    if (!isFlaggedForRemoval(deprecatedSymbol)) {
      // do not overlap with S1874
      return;
    }
    String deprecatedCode = "code";
    if (deprecatedSymbol.isMethodSymbol()) {
      deprecatedCode = "method";
    } else if (deprecatedSymbol.isTypeSymbol()) {
      deprecatedCode = "class";
    } else if (deprecatedSymbol.isVariableSymbol()) {
      deprecatedCode = "field";
    }
    reportIssue(identifierTree, String.format(MESSAGE, deprecatedCode));
  }

  @Override
  void checkOverridingMethod(MethodTree methodTree, List<Symbol.MethodSymbol> deprecatedSymbols) {
    if (deprecatedSymbols.stream().anyMatch(this::isFlaggedForRemoval)) {
      reportIssue(methodTree.simpleName(), "Don't override this deprecated method, it has been marked for removal.");
    }
  }
}
