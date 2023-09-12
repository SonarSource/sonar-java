/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6126")
public class StringConcatToTextBlockCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  private static final String MESSAGE = "Replace this String concatenation with Text block.";
  public static final int MINIMAL_CONTENT_LENGTH = 19;
  public static final int MINIMAL_NUMBER_OF_LINES = 2;
  // matches '\n' characters, but skips '\\n'
  public static final Pattern EOL = Pattern.compile("(?<!\\\\)\\\\n");
  private final Set<Tree> visitedNodes = new HashSet<>();

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava15Compatible();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.PLUS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (visitedNodes.contains(tree)) {
      return;
    }
    StringBuilder builder = new StringBuilder();
    if (concatStringLiterals(builder, tree)) {
      String content = builder.toString();
      if (content.length() >= MINIMAL_CONTENT_LENGTH
        && isMultiline(content)) {
        reportIssue(tree, MESSAGE);
      }
    }
  }

  private static boolean isMultiline(String line) {
    Matcher matcher = EOL.matcher(line);
    int matches = 0;
    while (matcher.find() && matches < MINIMAL_NUMBER_OF_LINES) {
      matches++;
    }
    return matches == MINIMAL_NUMBER_OF_LINES;
  }

  private boolean concatStringLiterals(StringBuilder concatenatedContent, Tree tree) {
    if (tree.is(Tree.Kind.PLUS)) {
      BinaryExpressionTree binaryExpression = (BinaryExpressionTree) tree;
      visitedNodes.add(binaryExpression);
      return concatStringLiterals(concatenatedContent, ExpressionUtils.skipParentheses(binaryExpression.leftOperand())) &&
        concatStringLiterals(concatenatedContent, ExpressionUtils.skipParentheses(binaryExpression.rightOperand()));
    } else if (tree instanceof LiteralTree) {
      String treeValue = LiteralUtils.getAsStringValue(((LiteralTree) tree));
      concatenatedContent.append(treeValue);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void setContext(JavaFileScannerContext context) {
    visitedNodes.clear();
    super.setContext(context);
  }

}
