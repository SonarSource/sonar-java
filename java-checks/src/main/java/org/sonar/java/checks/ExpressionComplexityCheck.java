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

import com.sonar.sslr.api.AstNode;
import org.sonar.squidbridge.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.Stack;

@Rule(
  key = "S1067",
  priority = Priority.MAJOR,
  tags={"brain-overload"})
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class ExpressionComplexityCheck extends SquidCheck<LexerlessGrammar> {

  private static final GrammarRuleKey[] OPERATORS = new GrammarRuleKey[] {
    JavaGrammar.CONDITIONAL_EXPRESSION,
    JavaGrammar.CONDITIONAL_OR_EXPRESSION,
    JavaGrammar.CONDITIONAL_AND_EXPRESSION
  };

  private static final GrammarRuleKey[] EXCLUSIONS = new GrammarRuleKey[] {
    JavaGrammar.CLASS_BODY,
    JavaGrammar.ARRAY_INITIALIZER
  };

  private static final int DEFAULT_MAX = 3;

  @RuleProperty(defaultValue = "" + DEFAULT_MAX)
  public int max = DEFAULT_MAX;

  private final Stack<Integer> expressionNestingLevel = new Stack<Integer>();
  private final Stack<Integer> operatorCounter = new Stack<Integer>();

  @Override
  public void init() {
    subscribeTo(JavaGrammar.EXPRESSION);
    subscribeTo(OPERATORS);
    subscribeTo(EXCLUSIONS);
  }

  @Override
  public void visitFile(AstNode node) {
    expressionNestingLevel.clear();
    operatorCounter.clear();

    pushExclusionLevel();
  }

  @Override
  public void visitNode(AstNode node) {
    if (node.is(JavaGrammar.EXPRESSION)) {
      int level = getExpressionNestingLevel();
      level++;
      setExpressionNestingLevel(level);

      if (level == 1) {
        setOperatorCounter(0);
      }
    } else if (node.is(EXCLUSIONS)) {
      pushExclusionLevel();
    } else {
      setOperatorCounter(getOperatorCounter() + node.getChildren(JavaPunctuator.QUERY, JavaPunctuator.OROR, JavaPunctuator.ANDAND).size());
    }
  }

  @Override
  public void leaveNode(AstNode node) {
    if (node.is(JavaGrammar.EXPRESSION)) {
      int level = getExpressionNestingLevel();
      level--;
      setExpressionNestingLevel(level);

      if (level == 0 && getOperatorCounter() > max) {
        getContext().createLineViolation(
            this,
            "Reduce the number of conditional operators (" + getOperatorCounter() + ") used in the expression (maximum allowed " + max + ").",
            node);
      }
    } else if (node.is(EXCLUSIONS)) {
      popExclusionLevel();
    }
  }

  private void pushExclusionLevel() {
    expressionNestingLevel.push(0);
    operatorCounter.push(0);
  }

  private void popExclusionLevel() {
    expressionNestingLevel.pop();
    operatorCounter.pop();
  }

  private int getExpressionNestingLevel() {
    return expressionNestingLevel.peek();
  }

  private void setExpressionNestingLevel(int level) {
    expressionNestingLevel.pop();
    expressionNestingLevel.push(level);
  }

  private int getOperatorCounter() {
    return operatorCounter.peek();
  }

  private void setOperatorCounter(int count) {
    operatorCounter.pop();
    operatorCounter.push(count);
  }

}
