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
package org.sonar.sslr.tests;

import com.google.common.base.Preconditions;
import com.sonar.sslr.api.RecognitionException;
import com.sonar.sslr.api.Rule;
import com.sonar.sslr.api.typed.ActionParser;
import org.assertj.core.api.AbstractAssert;
import org.sonar.java.ast.parser.FormalParametersListTreeImpl;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.java.ast.parser.JavaLexer;
import org.sonar.java.ast.parser.JavaNodeBuilder;
import org.sonar.java.ast.parser.TreeFactory;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerlessGrammarBuilder;

import java.nio.charset.StandardCharsets;

public class Assertions {

  public static RuleAssert assertThat(Rule actual) {
    return new RuleAssert(actual);
  }

  public static ParserAssert assertThat(GrammarRuleKey rule) {
    return assertThat(JavaLexer.createGrammarBuilder(), rule);
  }

  public static ParserAssert assertThat(LexerlessGrammarBuilder b, GrammarRuleKey rule) {
    return new ParserAssert(new ActionParser<>(
      StandardCharsets.UTF_8,
      b,
      JavaGrammar.class,
      new TreeFactory(),
      new JavaNodeBuilder(),
      rule));
  }

  public static class ParserAssert extends AbstractAssert<ParserAssert, ActionParser<Tree>> {

    public ParserAssert(ActionParser<Tree> actual) {
      super(actual, ParserAssert.class);
    }

    private void parseTillEof(String input) {
      JavaTree tree = (JavaTree) actual.parse(input);
      InternalSyntaxToken syntaxToken = (InternalSyntaxToken) tree.lastToken();
      //FIXME ugly hack to get closing parenthesis of formal parameter list
      if(tree instanceof FormalParametersListTreeImpl && ((FormalParametersListTreeImpl) tree).closeParenToken() != null) {
        syntaxToken = ((FormalParametersListTreeImpl) tree).closeParenToken();
      }
      if (syntaxToken == null || (!syntaxToken.isEOF() && (syntaxToken.column()+syntaxToken.text().length() != input.length()))) {
        if (syntaxToken == null)  {
          throw new RecognitionException(0, "Did not match till EOF : Last syntax token cannot be found");
        }
        throw new RecognitionException(
          0, "Did not match till EOF, but till line " + syntaxToken.line() + ": token \"" + syntaxToken.text() + "\"");
      }
    }

    public ParserAssert matches(String input) {
      isNotNull();
      Preconditions.checkArgument(!hasTrailingWhitespaces(input), "Trailing whitespaces in input are not supported");
      String expected = "Rule '" + getRuleName() + "' should match:\n" + input;
      try {
        parseTillEof(input);
      } catch (RecognitionException e) {
        String actual = e.getMessage();
        throw new ParsingResultComparisonFailure(expected, actual);
      }
      return this;
    }

    private static boolean hasTrailingWhitespaces(String input) {
      return input.endsWith(" ") || input.endsWith("\n") || input.endsWith("\r") || input.endsWith("\t");
    }

    public ParserAssert notMatches(String input) {
      isNotNull();
      try {
        parseTillEof(input);
      } catch (RecognitionException e) {
        // expected
        return this;
      }
      throw new AssertionError("Rule '" + getRuleName() + "' should not match:\n" + input);
    }

    private String getRuleName() {
      return actual.rootRule().toString();
    }

  }

}
