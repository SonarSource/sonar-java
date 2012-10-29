/*
 * Sonar Java
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

import com.google.common.collect.Lists;
import com.sonar.sslr.api.AstAndTokenVisitor;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import org.apache.commons.lang.StringUtils;
import org.sonar.check.Cardinality;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.ast.lexer.JavaLexer;
import org.sonar.java.ast.visitors.JavaAstCheck;

import java.nio.charset.Charset;
import java.util.List;

@Rule(
  key = "Snippet",
  priority = Priority.MAJOR,
  cardinality = Cardinality.MULTIPLE)
public final class SnippetCheck extends JavaAstCheck implements AstAndTokenVisitor {

  private static final String DEFAULT_DONT_EXAMPLE = "";
  private static final String DEFAULT_DO_EXAMPLE = "";

  @RuleProperty(
    key = "dontExample",
    defaultValue = "" + DEFAULT_DONT_EXAMPLE,
    type = "TEXT")
  public String dontExample = DEFAULT_DONT_EXAMPLE;

  @RuleProperty(
    key = "doExample",
    defaultValue = "" + DEFAULT_DO_EXAMPLE,
    type = "TEXT")
  public String doExample = DEFAULT_DO_EXAMPLE;

  private List<Token> tokensToBeMatched;
  private int tokenIndexToBeMatched;
  private PlaceholderState placeholderState;
  private int placeholderParenthesesBalancedLevel;

  private enum PlaceholderState {
    IN,
    OUT,
    LEAVING
  }

  @Override
  public void init() {
    if (!StringUtils.isEmpty(dontExample)) {
      tokensToBeMatched = JavaLexer.create(Charset.forName("UTF-8")).lex(dontExample);

      // Exclude the EOF token
      tokensToBeMatched = tokensToBeMatched.subList(0, tokensToBeMatched.size() - 1);

      // "value" cannot be last
      // "value(" is not allowed
      // "value.value" is not allowed
    } else {
      tokensToBeMatched = Lists.newArrayList();
    }
  }

  @Override
  public void visitFile(AstNode node) {
    tokenIndexToBeMatched = 0;
    placeholderState = PlaceholderState.OUT;
  }

  public void visitToken(Token token) {
    if (!tokensToBeMatched.isEmpty()) {
      updatePlaceholderState(token);

      if (placeholderState == PlaceholderState.OUT) {
        String expectedValue = tokensToBeMatched.get(tokenIndexToBeMatched).getOriginalValue();
        String actualValue = token.getOriginalValue();

        if (actualValue.equals(expectedValue)) {
          tokenIndexToBeMatched++;
          if (tokenIndexToBeMatched == tokensToBeMatched.size()) {
            getContext().createLineViolation(this, "This should be rewritten as: " + doExample, token);
            tokenIndexToBeMatched = 0;
          }
        } else {
          tokenIndexToBeMatched = 0;
        }
      }
    }
  }

  private void updatePlaceholderState(Token token) {
    String expectedValue = tokensToBeMatched.get(tokenIndexToBeMatched).getOriginalValue();
    String actualValue = token.getOriginalValue();

    if (placeholderState == PlaceholderState.LEAVING) {

      if (actualValue.equals(expectedValue)) {
        placeholderState = PlaceholderState.OUT;
      } else {
        placeholderState = PlaceholderState.IN;
      }
    }

    if (placeholderState == PlaceholderState.IN) {
      if ("(".equals(actualValue)) {
        placeholderParenthesesBalancedLevel++;
      } else if (")".equals(actualValue) && placeholderParenthesesBalancedLevel > 0) {
        placeholderParenthesesBalancedLevel--;
      } else if (placeholderParenthesesBalancedLevel == 0) {
        if (",".equals(actualValue)) {
          placeholderState = PlaceholderState.OUT;
        } else if (".".equals(actualValue)) {
          placeholderState = PlaceholderState.LEAVING;
        } else if (actualValue.equals(expectedValue)) {
          placeholderState = PlaceholderState.OUT;
        }
      }
    } else if (isPlaceholder(tokensToBeMatched.get(tokenIndexToBeMatched))) {
      placeholderState = PlaceholderState.IN;
      tokenIndexToBeMatched++;
    }
  }

  private boolean isPlaceholder(Token token) {
    return "value".equals(token.getOriginalValue());
  }

}
