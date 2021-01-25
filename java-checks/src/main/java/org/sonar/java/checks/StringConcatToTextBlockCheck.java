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
package org.sonar.java.checks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6126")
public class StringConcatToTextBlockCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  private static final String MESSAGE = "Replace this String concatenation with Text block.";
  public static final int MINIMAL_LINE_LENGTH = 7;
  public static final int MINIMAL_NUMBER_OF_LINES = 2;
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
    List<Tree> concatOperands = getConcatOperands(((BinaryExpressionTree) tree));
    
    // We cannot report issues if there are non literal operands in concatenation
    if (concatOperands.stream().anyMatch(operand -> !(operand instanceof LiteralTree))) {
      return;
    }
    
    long outputLines = concatOperands.stream()
      .filter(op -> op.is(Tree.Kind.STRING_LITERAL))
      .map(LiteralTree.class::cast)
      .map(LiteralTree::value)
      .filter(value -> value.contains("\\n"))
      .flatMap(value -> Stream.of(value.split("\\\\n")))
      .filter(str -> str.length() >= MINIMAL_LINE_LENGTH)
      .count();

    if (outputLines >= MINIMAL_NUMBER_OF_LINES) {
      reportIssue(tree, MESSAGE);
    }
  }
  
  private List<Tree> getConcatOperands(BinaryExpressionTree binaryExpressionTree) {
    List<Tree> operands = new ArrayList<>();
    ExpressionTree rightOperand = ExpressionUtils.skipParentheses(binaryExpressionTree.rightOperand());
    ExpressionTree leftOperand = ExpressionUtils.skipParentheses(binaryExpressionTree.leftOperand());

    operands.addAll(childrenOperands(leftOperand));
    operands.addAll(childrenOperands(rightOperand));
    
    visitedNodes.add(binaryExpressionTree);
    return operands;
  }

  private List<Tree> childrenOperands(ExpressionTree leftOperand) {
    if (leftOperand.is(Tree.Kind.PLUS)) {
      return getConcatOperands(((BinaryExpressionTree) leftOperand));
    } else {
      return Collections.singletonList(leftOperand);
    }
  }

  @Override
  public void setContext(JavaFileScannerContext context) {
    visitedNodes.clear();
    super.setContext(context);
  }

}
