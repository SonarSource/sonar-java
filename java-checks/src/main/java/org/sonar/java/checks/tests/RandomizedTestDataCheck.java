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
package org.sonar.java.checks.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5977")
public class RandomizedTestDataCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatchers RANDOM_CONSTRUCTOR_METHOD_MATCHER = MethodMatchers.create()
    .ofTypes("java.util.Random")
    .constructor()
    .withAnyParameters()
    .build();

  private static final MethodMatchers RANDOM_UUID_MATCHER = MethodMatchers.create()
    .ofTypes("java.util.UUID")
    .names("randomUUID")
    .addWithoutParametersMatcher()
    .build();

  private static final String LOCATIONS_TEXT = "usage of random data in test";
  private static final String MESSAGE = "Replace randomly generated values with fixed ones.";

  private boolean reportedUUIDRandom = false;
  private final List<Tree> randomSecondaryLocations = new ArrayList<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.NEW_CLASS)) {
      NewClassTree newClassTree = (NewClassTree) tree;
      checkForRandomConstructorUsage(newClassTree);
    }
    if (!reportedUUIDRandom && tree.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree methodInvocationTree = (MethodInvocationTree) tree;
      checkAndReportUUIDRandomUsage(methodInvocationTree);
    }
  }

  private void checkForRandomConstructorUsage(NewClassTree newClassTree) {
    if (RANDOM_CONSTRUCTOR_METHOD_MATCHER.matches(newClassTree)) {
      randomSecondaryLocations.add(newClassTree);
    }
  }

  private void checkAndReportUUIDRandomUsage(MethodInvocationTree methodInvocationTree) {
    Symbol symbol = methodInvocationTree.symbol();
    if (RANDOM_UUID_MATCHER.matches(methodInvocationTree)) {
      reportedUUIDRandom = true;
      List<JavaFileScannerContext.Location> locations = symbol.usages().stream()
        .map(identifierTree -> new JavaFileScannerContext.Location(LOCATIONS_TEXT, identifierTree))
        .collect(Collectors.toList());
      reportIssue(methodInvocationTree, MESSAGE, locations, null);
    }
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    if (!randomSecondaryLocations.isEmpty()) {
      List<JavaFileScannerContext.Location> locations = randomSecondaryLocations.stream()
        .map(tree -> new JavaFileScannerContext.Location(LOCATIONS_TEXT, tree))
        .collect(Collectors.toList());
      reportIssue(randomSecondaryLocations.get(0), MESSAGE, locations, null);
    }
    reportedUUIDRandom = false;
    randomSecondaryLocations.clear();
    super.leaveFile(context);
  }
}
