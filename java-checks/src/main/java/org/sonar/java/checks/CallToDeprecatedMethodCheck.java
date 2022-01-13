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
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;

@DeprecatedRuleKey(ruleKey = "CallToDeprecatedMethod", repositoryKey = "squid")
@Rule(key = "S1874")
public class CallToDeprecatedMethodCheck extends AbstractCallToDeprecatedCodeChecker {

  @Override
  void checkDeprecatedIdentifier(IdentifierTree identifierTree, Symbol deprecatedSymbol) {
    if (isFlaggedForRemoval(deprecatedSymbol)) {
      // do not overlap with S5738
      return;
    }
    String name = deprecatedSymbol.name();

    if (deprecatedSymbol.isMethodSymbol()) {
      Tree parent = identifierTree.parent();
      Arguments arguments = null;
      if (parent.is(Tree.Kind.METHOD_INVOCATION)) {
        arguments = ((MethodInvocationTree) parent).arguments();
      } else if (parent.is(Tree.Kind.NEW_CLASS)) {
        name = deprecatedSymbol.owner().name();
        arguments = ((NewClassTree) parent).arguments();
      }
      // If any of the arguments is of unknown type then we don't report any issue
      if (arguments != null && arguments.stream().anyMatch(arg -> arg.symbolType().isUnknown())) {
        return;
      }
    }
    reportIssue(identifierTree, String.format("Remove this use of \"%s\"; it is deprecated.", name));
  }

  @Override
  void checkOverridingMethod(MethodTree methodTree, List<Symbol.MethodSymbol> deprecatedSymbols) {
    if (deprecatedSymbols.stream().allMatch(this::nonAbstractOrFlaggedForRemoval)) {
      reportIssue(methodTree.simpleName(), "Don't override a deprecated method or explicitly mark it as \"@Deprecated\".");
    }
  }

  private boolean nonAbstractOrFlaggedForRemoval(Symbol.MethodSymbol method) {
    // if the method is abstract, you are forced to implement it
    return !(method.isAbstract()
      // if the method is flagged for removal, it will be handled by S5738
      || isFlaggedForRemoval(method));
  }
}
