/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonarsource.analyzer.commons.collections.MapBuilder;
import org.sonar.java.checks.helpers.UnitTestUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5826")
public class JUnit45MethodAnnotationCheck extends IssuableSubscriptionVisitor {

  private static final String JUNIT_SETUP = "setUp";
  private static final String JUNIT_TEARDOWN = "tearDown";
  private static final String ORG_JUNIT_AFTER = "org.junit.After";
  private static final String ORG_JUNIT_BEFORE = "org.junit.Before";

  private static final Map<String, String> JUNIT4_TO_JUNIT5 = MapBuilder.<String, String>newMap()
    .put(ORG_JUNIT_BEFORE, "org.junit.jupiter.api.BeforeEach")
    .put("org.junit.BeforeClass", "org.junit.jupiter.api.BeforeAll")
    .put(ORG_JUNIT_AFTER, "org.junit.jupiter.api.AfterEach")
    .put("org.junit.AfterClass", "org.junit.jupiter.api.AfterAll")
    .build();

  private static final Set<String> JUNIT4_ANNOTATIONS = JUNIT4_TO_JUNIT5.keySet();
  private static final Set<String> JUNIT5_ANNOTATIONS = new HashSet<>(JUNIT4_TO_JUNIT5.values());

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;

    List<MethodTree> methods = classTree.members().stream()
      .filter(member -> member.is(Tree.Kind.METHOD))
      .map(MethodTree.class::cast)
      .collect(Collectors.toList());

    int jUnitVersion = getJUnitVersion(methods);
    if (jUnitVersion > 0) {
      methods.forEach(methodTree -> checkJUnitMethod(methodTree, jUnitVersion));
    }
  }

  private static int getJUnitVersion(List<MethodTree> methods) {
    boolean containsJUnit4Tests = false;
    for (MethodTree methodTree : methods) {
      SymbolMetadata metadata = methodTree.symbol().metadata();
      containsJUnit4Tests |= metadata.isAnnotatedWith("org.junit.Test");
      if (UnitTestUtils.hasJUnit5TestAnnotation(methodTree)) {
        // While migrating from JUnit4 to JUnit5, classes might end up in mixed state of having tests using both versions.
        // If it's the case, we consider the test classes as ultimately targeting 5
        return 5;
      }
    }
    return containsJUnit4Tests ? 4 : -1;
  }

  private void checkJUnitMethod(MethodTree methodTree, int jUnitVersion) {
    if (isSetupTearDownSignature(methodTree) || (jUnitVersion == 5 && isAnnotatedWith(methodTree, ORG_JUNIT_BEFORE, ORG_JUNIT_AFTER))) {
      checkSetupTearDownSignature(methodTree, jUnitVersion);
    }
  }

  private void checkSetupTearDownSignature(MethodTree methodTree, int jUnitVersion) {
    Symbol.MethodSymbol symbol = methodTree.symbol();
    if (!Boolean.FALSE.equals(methodTree.isOverriding())) {
      // Annotation can be in a parent. If unknown (null), consider has override to avoid FP.
      return;
    }

    SymbolMetadata metadata = symbol.metadata();
    Optional<String> junit4Annotation = JUNIT4_ANNOTATIONS.stream().filter(metadata::isAnnotatedWith).findFirst();
    boolean isAnnotatedWithJUnit4 = junit4Annotation.isPresent();
    boolean isAnnotatedWithJUnit5 = JUNIT5_ANNOTATIONS.stream().anyMatch(metadata::isAnnotatedWith);

    if (jUnitVersion == 5 && isAnnotatedWithJUnit4 && !isAnnotatedWithJUnit5) {
      String jUnit4Annotation = junit4Annotation.get();
      reportIssue(methodTree.simpleName(), String.format("Annotate this method with JUnit5 '@%s' instead of JUnit4 '@%s'.",
        JUNIT4_TO_JUNIT5.get(jUnit4Annotation),
        jUnit4Annotation.substring(jUnit4Annotation.lastIndexOf('.') + 1)));
    } else if (!isAnnotatedWithJUnit4 && !isAnnotatedWithJUnit5) {
      reportIssue(methodTree.simpleName(), String.format("Annotate this method with JUnit%d '@%s' or rename it to avoid confusion.",
        jUnitVersion,
        expectedAnnotation(symbol, jUnitVersion)));
    }
  }

  private static boolean isAnnotatedWith(MethodTree methodTree, String... annotations) {
    SymbolMetadata methodMetadata = methodTree.symbol().metadata();
    return Arrays.stream(annotations).anyMatch(methodMetadata::isAnnotatedWith);
  }

  private static boolean isSetupTearDownSignature(MethodTree methodTree) {
    String name = methodTree.simpleName().name();
    return (JUNIT_SETUP.equals(name) || JUNIT_TEARDOWN.equals(name))
      && methodTree.parameters().isEmpty()
      && !methodTree.symbol().isPrivate();
  }

  private static String expectedAnnotation(Symbol.MethodSymbol symbol, int jUnitVersion) {
    String expected;
    if (JUNIT_SETUP.equals(symbol.name())) {
      expected = ORG_JUNIT_BEFORE;
    } else {
      expected = ORG_JUNIT_AFTER;
    }
    return jUnitVersion == 4 ? expected : JUNIT4_TO_JUNIT5.get(expected);
  }

}
