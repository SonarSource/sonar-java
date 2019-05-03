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
package org.sonar.java.ast.parser;

import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Rule;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.TokenType;
import com.sonar.sslr.api.Trivia;
import com.sonar.sslr.api.typed.Input;
import com.sonar.sslr.api.typed.NodeBuilder;
import org.sonar.java.model.InternalSyntaxSpacing;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.InternalSyntaxTrivia;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.sslr.grammar.GrammarRuleKey;

import java.util.ArrayList;
import java.util.List;

public class JavaNodeBuilder implements NodeBuilder {

  @Override
  public Object createNonTerminal(GrammarRuleKey ruleKey, Rule rule, List<Object> children, int startIndex, int endIndex) {
    for (Object child : children) {
      if (child instanceof InternalSyntaxToken) {
        InternalSyntaxToken syntaxToken = (InternalSyntaxToken) child;
        syntaxToken.setGrammarRuleKey(ruleKey);
        return child;
      }
    }
    return new InternalSyntaxSpacing(startIndex, endIndex);
  }

  @Override
  public Object createTerminal(Input input, int startIndex, int endIndex, List<Trivia> trivias, TokenType type) {
    boolean isEof = GenericTokenType.EOF.equals(type);
    LineColumnValue lineColumnValue = tokenPosition(input, startIndex, endIndex);
    return new InternalSyntaxToken(lineColumnValue.line, lineColumnValue.column, lineColumnValue.value,
      createTrivias(trivias), startIndex, endIndex, isEof);
  }

  private static List<SyntaxTrivia> createTrivias(List<Trivia> trivias) {
    List<SyntaxTrivia> result = new ArrayList<>();
    for (Trivia trivia : trivias) {
      Token trivialToken = trivia.getToken();
      result.add(InternalSyntaxTrivia.create(trivialToken.getValue(), trivialToken.getLine(), trivialToken.getColumn()));
    }
    return result;
  }

  private static LineColumnValue tokenPosition(Input input, int startIndex, int endIndex) {
    int[] lineAndColumn = input.lineAndColumnAt(startIndex);
    String value = input.substring(startIndex, endIndex);
    return new LineColumnValue(lineAndColumn[0], lineAndColumn[1] - 1, value);
  }

  private static class LineColumnValue {
    final int line;
    final int column;
    final String value;

    private LineColumnValue(int line, int column, String value) {
      this.line = line;
      this.column = column;
      this.value = value;
    }
  }

}
