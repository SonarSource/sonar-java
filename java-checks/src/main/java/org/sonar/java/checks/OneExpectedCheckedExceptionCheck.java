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

import java.util.List;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5783")
public class OneExpectedCheckedExceptionCheck extends AbstractOneExpectedExceptionRule {

  @Override
  void reportMultipleCallInTree(List<Type> expectedExceptions, Tree treeToVisit, Tree reportLocation, String placeToRefactor) {
    List<Type> checkedTypes = expectedExceptions.stream()
      .filter(AbstractOneExpectedExceptionRule::isChecked)
      .collect(Collectors.toList());

    if (checkedTypes.isEmpty()) {
      return;
    }

    MethodInvocationCollector visitor = new MethodInvocationCollector(symbol -> throwExpectedException(symbol, checkedTypes));
    treeToVisit.accept(visitor);
    List<Tree> invocationTree = visitor.invocationTree;
    if (invocationTree.size() > 1) {
      reportIssue(reportLocation,
        String.format("Refactor the %s to have only one invocation throwing an exception.", placeToRefactor),
        secondaryLocations(invocationTree),
        null);
    }
  }

  private static boolean throwExpectedException(Symbol symbol, List<Type> checkedTypes) {
    return symbol.isMethodSymbol()
      && ((Symbol.MethodSymbol) symbol).thrownTypes().stream()
      .anyMatch(t -> checkedTypes.stream().anyMatch(t::isSubtypeOf));
  }

}
