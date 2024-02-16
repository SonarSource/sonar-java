/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6216")
public class AccessibilityChangeOnRecordsCheck extends AbstractAccessibilityChangeChecker {
  private static final String MESSAGE = "Remove this private field update which will never succeed";
  private static final String SECONDARY_MESSAGE = "Remove this accessibility bypass which will never succeed";

  private Map<Symbol, MethodInvocationTree> primaryTargets = new HashMap<>();
  private Map<Symbol, List<MethodInvocationTree>> secondaryTargets = new HashMap<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.COMPILATION_UNIT, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.COMPILATION_UNIT)) {
      primaryTargets.clear();
      secondaryTargets.clear();
    }
    super.visitNode(tree);
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Tree.Kind.COMPILATION_UNIT)) {
      for (Map.Entry<Symbol, MethodInvocationTree> entry : primaryTargets.entrySet()) {
        Symbol symbol = entry.getKey();
        MethodInvocationTree setInvocation = entry.getValue();
        List<JavaFileScannerContext.Location> secondaries = secondaryTargets.getOrDefault(symbol, Collections.emptyList())
          .stream()
          .map(mit -> new JavaFileScannerContext.Location(SECONDARY_MESSAGE, mit))
          .toList();
        reportIssue(setInvocation, MESSAGE, secondaries, null);
      }
    }
    super.leaveNode(tree);
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (!isModifyingFieldFromRecord(mit)) {
      return;
    }
    if (SET_MATCHERS.matches(mit)) {
      Optional<Symbol> symbol = getIdentifierSymbol(mit);
      if (symbol.isPresent()) {
        primaryTargets.put(symbol.get(), mit);
      } else {
        reportIssue(mit, MESSAGE);
      }
    } else if (setsToPubliclyAccessible(mit)) {
      Optional<Symbol> symbol = getIdentifierSymbol(mit);
      symbol.ifPresent(key -> {
        List<MethodInvocationTree> secondaries = secondaryTargets.getOrDefault(key, new ArrayList<>());
        secondaries.add(mit);
        secondaryTargets.put(key, secondaries);
      });
    }
  }

  /**
   * Looks up the compoment the method is called on. If the component is an identifier, the method returns the symbol.
   */
  private static Optional<Symbol> getIdentifierSymbol(MethodInvocationTree mit) {
    ExpressionTree expression = mit.methodSelect();
    if (!expression.is(Tree.Kind.MEMBER_SELECT)) {
      return Optional.empty();
    }
    MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) expression;
    ExpressionTree callee = memberSelect.expression();
    if (!callee.is(Tree.Kind.IDENTIFIER)) {
      return Optional.empty();
    }
    IdentifierTree identifier = (IdentifierTree) callee;
    return Optional.of(identifier.symbol());
  }
}
