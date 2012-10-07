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
    defaultValue = "" + DEFAULT_DONT_EXAMPLE)
  public String dontExample = DEFAULT_DONT_EXAMPLE;

  @RuleProperty(
    key = "doExample",
    defaultValue = "" + DEFAULT_DO_EXAMPLE)
  public String doExample = DEFAULT_DO_EXAMPLE;

  private List<Token> tokensToBeMatched;
  private int tokenIndexToBeMatched;

  @Override
  public void init() {
    if (!StringUtils.isEmpty(dontExample)) {
      tokensToBeMatched = JavaLexer.create(Charset.forName("UTF-8")).lex(dontExample);

      // Exclude the EOF token
      tokensToBeMatched = tokensToBeMatched.subList(0, tokensToBeMatched.size() - 1);
    } else {
      tokensToBeMatched = Lists.newArrayList();
    }
  }

  @Override
  public void visitFile(AstNode node) {
    tokenIndexToBeMatched = 0;
  }

  public void visitToken(Token token) {
    if (!tokensToBeMatched.isEmpty()) {
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
