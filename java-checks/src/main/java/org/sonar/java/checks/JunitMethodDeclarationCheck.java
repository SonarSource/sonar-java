/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
  key = "S2391",
  name = "JUnit framework methods should be declared properly",
  tags = {"bug", "junit"},
  priority = Priority.CRITICAL)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNIT_TESTABILITY)
@SqaleConstantRemediation("5min")
public class JunitMethodDeclarationCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final String JUNIT_SETUP = "setUp";
  private static final String JUNIT_TEARDOWN = "tearDown";
  private static final String JUNIT_SUITE = "suite";
  private static final int MAX_STRING_DISTANCE = 3;

  private JavaFileScannerContext context;

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitClass(ClassTree tree) {
    if (isJunit3Class(tree)) {
      super.visitClass(tree);
    }
  }

  @Override
  public void visitMethod(MethodTree methodTree) {
    String name = methodTree.simpleName().name();
    TypeTree returnType = methodTree.returnType();
    if (JUNIT_SETUP.equals(name)) {
      checkSetupTearDownSignature(methodTree);
    } else if (JUNIT_TEARDOWN.equals(name)) {
      checkSetupTearDownSignature(methodTree);
    } else if (JUNIT_SUITE.equals(name)) {
      checkSuiteSignature(methodTree);
    } else if (returnType != null && returnType.symbolType().isSubtypeOf("junit.framework.Test")) {
      addIssueForMethodBadName(methodTree, JUNIT_SUITE, name);
    } else if (areVerySimilarStrings(JUNIT_SETUP, name)) {
      addIssueForMethodBadName(methodTree, JUNIT_SETUP, name);
    } else if (areVerySimilarStrings(JUNIT_TEARDOWN, name)) {
      addIssueForMethodBadName(methodTree, JUNIT_TEARDOWN, name);
    } else if (areVerySimilarStrings(JUNIT_SUITE, name)) {
      addIssueForMethodBadName(methodTree, JUNIT_SUITE, name);
    }
  }

  @VisibleForTesting
  protected boolean areVerySimilarStrings(String expected, String actual) {
    // cut complexity when the strings length difference is bigger than the accepted threshold
    return (Math.abs(expected.length() - actual.length()) <= MAX_STRING_DISTANCE) && StringUtils.getLevenshteinDistance(expected, actual) <= MAX_STRING_DISTANCE;
  }

  private void checkSuiteSignature(MethodTree methodTree) {
    Symbol.MethodSymbol symbol = methodTree.symbol();
    if (!symbol.isPublic()) {
      context.addIssue(methodTree, this, "Make this method \"public\".");
    } else if (!symbol.isStatic()) {
      context.addIssue(methodTree, this, "Make this method \"static\".");
    } else if (!methodTree.parameters().isEmpty()) {
      context.addIssue(methodTree, this, "This method does not accept parameters.");
    } else {
      TypeTree returnType = methodTree.returnType();
      if (returnType != null && !returnType.symbolType().isSubtypeOf("junit.framework.Test")) {
        context.addIssue(methodTree, this, "This method should return either a \"junit.framework.Test\" or a \"junit.framework.TestSuite\".");
      }
    }
  }

  private void checkSetupTearDownSignature(MethodTree methodTree) {
    Symbol.MethodSymbol symbol = methodTree.symbol();
    if (!symbol.isPublic()) {
      context.addIssue(methodTree, this, "Make this method \"public\".");
    } else if (!methodTree.parameters().isEmpty()) {
      context.addIssue(methodTree, this, "This method does not accept parameters.");
    } else {
      TypeTree returnType = methodTree.returnType();
      if (returnType != null && !returnType.symbolType().isVoid()) {
        context.addIssue(methodTree, this, "Make this method return \"void\".");
      }
    }
  }

  private void addIssueForMethodBadName(MethodTree methodTree, String expected, String actual) {
    context.addIssue(methodTree, this, "This method should be named \"" + expected + "\" not \"" + actual + "\".");
  }

  private static boolean isJunit3Class(ClassTree classTree) {
    return classTree.symbol().type().isSubtypeOf("junit.framework.TestCase");
  }

}
