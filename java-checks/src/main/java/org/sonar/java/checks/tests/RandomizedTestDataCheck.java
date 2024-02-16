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
package org.sonar.java.checks.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
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

  private final List<Tree> randomSecondaryLocations = new ArrayList<>();
  private final List<Tree> randomUUIDSecondaryLocations = new ArrayList<>();

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
    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree methodInvocationTree = (MethodInvocationTree) tree;
      checkUUIDRandomUsage(methodInvocationTree);
    }
  }

  private void checkForRandomConstructorUsage(NewClassTree newClassTree) {
    if (RANDOM_CONSTRUCTOR_METHOD_MATCHER.matches(newClassTree)) {
      randomSecondaryLocations.add(newClassTree);
    }
  }

  private void checkUUIDRandomUsage(MethodInvocationTree methodInvocationTree) {
    if (RANDOM_UUID_MATCHER.matches(methodInvocationTree)) {
      randomUUIDSecondaryLocations.add(methodInvocationTree);
    }
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    if (!randomSecondaryLocations.isEmpty()) {
      reportIssue(randomSecondaryLocations.get(0), MESSAGE, convertToLocations(randomSecondaryLocations.stream().skip(1)), null);
    }
    if (!randomUUIDSecondaryLocations.isEmpty()) {
      reportIssue(randomUUIDSecondaryLocations.get(0), MESSAGE, convertToLocations(randomUUIDSecondaryLocations.stream().skip(1)), null);
    }
    cleanup();
    super.leaveFile(context);
  }

  private void cleanup() {
    randomSecondaryLocations.clear();
    randomUUIDSecondaryLocations.clear();
  }

  private static List<JavaFileScannerContext.Location> convertToLocations(Stream<Tree> trees) {
    return trees
      .map(tree -> new JavaFileScannerContext.Location(LOCATIONS_TEXT, tree))
      .toList();
  }
}
