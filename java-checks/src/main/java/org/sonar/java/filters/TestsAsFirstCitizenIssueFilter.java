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
package org.sonar.java.filters;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.sonar.java.checks.HardcodedURICheck;
import org.sonar.java.checks.RawExceptionCheck;
import org.sonar.java.checks.naming.BadMethodNameCheck;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

public class TestsAsFirstCitizenIssueFilter extends BaseTreeVisitorIssueFilter {

  @Override
  public Set<Class<? extends JavaCheck>> filteredRules() {
    return new HashSet<>(Arrays.asList(
      RawExceptionCheck.class,
      BadMethodNameCheck.class,
      HardcodedURICheck.class));
  }

  @Override
  public void visitMethod(MethodTree tree) {
    if (context().isTestFile()) {
      // exclude any issue from RawExceptionCheck for methods of test files
      excludeLines(tree, RawExceptionCheck.class);
    } else {
      acceptLines(tree, RawExceptionCheck.class);
    }

    if (isTestMethod(tree)) {
      excludeLines(tree, BadMethodNameCheck.class);
    } else {
      acceptLines(tree, BadMethodNameCheck.class);
    }

    super.visitMethod(tree);
  }

  private static boolean isTestMethod(MethodTree methodTree) {
    return methodTree.modifiers().annotations().stream()
      .map(AnnotationTree::annotationType)
      .map(Tree::lastToken)
      .map(SyntaxToken::text)
      .anyMatch("Test"::equals);
  }

  @Override
  public void visitLiteral(LiteralTree tree) {
    if (context().isTestFile() && tree.is(Tree.Kind.STRING_LITERAL)) {
      excludeLines(tree, HardcodedURICheck.class);
    } else {
      acceptLines(tree, HardcodedURICheck.class);
    }
    super.visitLiteral(tree);
  }

}
