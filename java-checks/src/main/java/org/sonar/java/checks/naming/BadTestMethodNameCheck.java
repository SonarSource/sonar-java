/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.checks.naming;

import com.google.common.collect.ImmutableList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import static java.util.Arrays.asList;

@Rule(key = "S3578")
public class BadTestMethodNameCheck extends IssuableSubscriptionVisitor {

  private static final String DEFAULT_FORMAT = "^test[A-Z][a-zA-Z0-9]*$";
  private static final Set<String> TEST_ANNOTATIONS = new HashSet<>(asList(
    "org.junit.Test",
    "org.testng.annotations.Test",
    "org.junit.jupiter.api.Test",
    "org.junit.jupiter.api.RepeatedTest",
    "org.junit.jupiter.api.TestFactory",
    "org.junit.jupiter.api.TestTemplate",
    "org.junit.jupiter.params.ParameterizedTest"));

  @RuleProperty(
    key = "format",
    description = "Regular expression the test method names are checked against.",
    defaultValue = "" + DEFAULT_FORMAT)
  public String format = DEFAULT_FORMAT;

  private Pattern pattern = null;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD);
  }

  @Override
  public void setContext(JavaFileScannerContext context) {
    if (pattern == null) {
      pattern = Pattern.compile(format, Pattern.DOTALL);
    }
    super.setContext(context);
  }


  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    MethodTree methodTree = (MethodTree) tree;
    if (isNotOverriden(methodTree) && isTestMethod(methodTree) && !pattern.matcher(methodTree.simpleName().name()).matches()) {
      reportIssue(methodTree.simpleName(), "Rename this method name to match the regular expression: '" + format + "'");
    }
  }

  private static boolean isNotOverriden(MethodTree methodTree) {
    return Boolean.FALSE.equals(methodTree.isOverriding());
  }

  private static boolean isTestMethod(MethodTree methodTree) {
    SymbolMetadata metadata = methodTree.symbol().metadata();
    return TEST_ANNOTATIONS.stream().anyMatch(metadata::isAnnotatedWith);
  }
}
