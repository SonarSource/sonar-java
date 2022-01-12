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
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
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
      Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) deprecatedSymbol;
      Tree parent = identifierTree.parent();
      Arguments arguments = null;
      if (parent.is(Tree.Kind.METHOD_INVOCATION)) {
        arguments = ((MethodInvocationTree) parent).arguments();
      } else if (parent.is(Tree.Kind.NEW_CLASS)) {
        name = deprecatedSymbol.owner().name();
        arguments = ((NewClassTree) parent).arguments();
      }
      if (arguments != null && !argumentsMatchSignature(arguments, methodSymbol.parameterTypes())) {
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

  /**
   * Tests that the arguments types match the parameter types.
   * Please note that the method returns false when the signature contains variadic parameters.
   *
   * @param arguments Arguments in a method call
   * @param types Parameter types in a method signature
   * @return true if the arguments' types match types. false otherwise.
   */
  private static boolean argumentsMatchSignature(Arguments arguments, List<Type> types) {
    if (arguments.size() != types.size()) {
      return false;
    }
    for (int i = 0; i < arguments.size(); i++) {
      ExpressionTree argument = arguments.get(i);
      Type type = types.get(i);
      if (!argument.symbolType().equals(type)) {
        return false;
      }
    }
    return true;
  }
}
