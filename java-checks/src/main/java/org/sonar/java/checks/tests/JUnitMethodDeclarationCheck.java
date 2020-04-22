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

import com.google.common.annotations.VisibleForTesting;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2391")
public class JUnitMethodDeclarationCheck extends IssuableSubscriptionVisitor {

  private static final String JUNIT_FRAMEWORK_TEST = "junit.framework.Test";
  private static final String JUNIT_SETUP = "setUp";
  private static final String JUNIT_TEARDOWN = "tearDown";
  private static final String JUNIT_SUITE = "suite";
  private static final int MAX_STRING_DISTANCE = 3;

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

    int jUnitVersion = getJUnitVersion(classTree, methods);
    if (jUnitVersion > 0) {
      methods.forEach(methodTree -> checkJUnitMethod(methodTree, jUnitVersion));
    }
  }

  private static int getJUnitVersion(ClassTree classTree, List<MethodTree> methods) {
    if (isJunit3Class(classTree)) {
      return 3;
    }
    boolean containsJUnit4Tests = false;
    for (MethodTree methodTree : methods) {
      SymbolMetadata metadata = methodTree.symbol().metadata();
      containsJUnit4Tests |= metadata.isAnnotatedWith("org.junit.Test");
      if (metadata.isAnnotatedWith("org.junit.jupiter.api.Test")) {
        // while migrating from JUnit4 to JUnit5, classes might end up in mixed state
        // of having tests using both versions - assuming 5
        return 5;
      }
    }
    return containsJUnit4Tests ? 4 : -1;
  }

  private void checkJUnitMethod(MethodTree methodTree, int jUnitVersion) {
    String name = methodTree.simpleName().name();
    if (JUNIT_SETUP.equals(name) || JUNIT_TEARDOWN.equals(name)) {
      checkSetupTearDownSignature(methodTree, jUnitVersion);
    } else if (JUNIT_SUITE.equals(name)) {
      checkSuiteSignature(methodTree, jUnitVersion);
    } else if (jUnitVersion == 3) {
      // only check for bad naming when targeting JUnit 3
      if (methodTree.symbol().returnType().type().isSubtypeOf(JUNIT_FRAMEWORK_TEST) || areVerySimilarStrings(JUNIT_SUITE, name)) {
        addIssueForMethodBadName(methodTree, JUNIT_SUITE, name);
      } else if (areVerySimilarStrings(JUNIT_SETUP, name)) {
        addIssueForMethodBadName(methodTree, JUNIT_SETUP, name);
      } else if (areVerySimilarStrings(JUNIT_TEARDOWN, name)) {
        addIssueForMethodBadName(methodTree, JUNIT_TEARDOWN, name);
      }
    }
  }

  @VisibleForTesting
  protected boolean areVerySimilarStrings(String expected, String actual) {
    // cut complexity when the strings length difference is bigger than the accepted threshold
    return (Math.abs(expected.length() - actual.length()) <= MAX_STRING_DISTANCE)
      && StringUtils.getLevenshteinDistance(expected, actual) < MAX_STRING_DISTANCE;
  }

  private void checkSuiteSignature(MethodTree methodTree, int jUnitVersion) {
    Symbol.MethodSymbol symbol = methodTree.symbol();
    if (jUnitVersion > 3) {
      if (symbol.returnType().type().isSubtypeOf(JUNIT_FRAMEWORK_TEST)) {
        // ignore modifiers and parameters, whatever they are, "suite():Test" should be dropped in a JUnit4/5 context
        reportIssue(methodTree.simpleName(), String.format("Remove this method, JUnit%d test suites are not relying on it anymore.", jUnitVersion));
      }
      return;
    }
    if (!symbol.isPublic()) {
      reportIssue(methodTree, "Make this method \"public\".");
    } else if (!symbol.isStatic()) {
      reportIssue(methodTree, "Make this method \"static\".");
    } else if (!methodTree.parameters().isEmpty()) {
      reportIssue(methodTree, "This method does not accept parameters.");
    } else if (!symbol.returnType().type().isSubtypeOf(JUNIT_FRAMEWORK_TEST)) {
      reportIssue(methodTree, "This method should return either a \"junit.framework.Test\" or a \"junit.framework.TestSuite\".");
    }
  }

  private void checkSetupTearDownSignature(MethodTree methodTree, int jUnitVersion) {
    if (!methodTree.parameters().isEmpty()) {
      reportIssue(methodTree, "This method does not accept parameters.");
    } else if (jUnitVersion > 3) {
      Symbol.MethodSymbol symbol = methodTree.symbol();
      expectedAnnotation(symbol, jUnitVersion)
        .filter(annotation -> !symbol.metadata().isAnnotatedWith(annotation))
        .ifPresent(annotation -> reportIssue(
          methodTree.simpleName(),
          String.format("Annotate this method with JUnit%d '@%s' or remove it.", jUnitVersion, annotation)));
    }
  }

  private static Optional<String> expectedAnnotation(Symbol.MethodSymbol symbol, int jUnitVersion) {
    if (JUNIT_SETUP.equals(symbol.name())) {
      return Optional.of(jUnitVersion == 4 ? "org.junit.Before" : "org.junit.jupiter.api.BeforeEach");
    }
    return Optional.of(jUnitVersion == 4 ? "org.junit.After" : "org.junit.jupiter.api.AfterEach");
  }

  private void addIssueForMethodBadName(MethodTree methodTree, String expected, String actual) {
    reportIssue(methodTree, "This method should be named \"" + expected + "\" not \"" + actual + "\".");
  }

  private void reportIssue(MethodTree methodTree, String message) {
    reportIssue(methodTree.simpleName(), message);
  }

  private static boolean isJunit3Class(ClassTree classTree) {
    return classTree.symbol().type().isSubtypeOf("junit.framework.TestCase");
  }

}
