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
package org.sonar.java.checks.codesnippet;

import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Rule;
import com.sonar.sslr.impl.Lexer;
import com.sonar.sslr.impl.Parser;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.api.JavaTokenType;

import static com.sonar.sslr.impl.matcher.GrammarFunctions.Advanced.bridge;
import static com.sonar.sslr.impl.matcher.GrammarFunctions.Standard.and;
import static com.sonar.sslr.impl.matcher.GrammarFunctions.Standard.o2n;
import static com.sonar.sslr.impl.matcher.GrammarFunctions.Standard.one2n;
import static com.sonar.sslr.impl.matcher.GrammarFunctions.Standard.or;

public class JavaPatternGrammarImpl extends JavaPatternGrammar {

  public JavaPatternGrammarImpl() {
    identifier.is(or(
        GenericTokenType.IDENTIFIER,
        JavaKeyword.THIS,
        JavaKeyword.SUPER));
    characterLiteral.is(JavaTokenType.CHARACTER_LITERAL);
    stringLiteral.is(GenericTokenType.LITERAL);
    nullLiteral.is(JavaKeyword.NULL);
    booleanLiteral.is(or(
        JavaKeyword.TRUE,
        JavaKeyword.FALSE));
    integerLiteral.is(JavaTokenType.INTEGER_LITERAL);
    floatingLiteral.is(JavaTokenType.FLOATING_LITERAL);
    qualifiedIdentifier.is(identifier,
        o2n(or(
            and(JavaPunctuator.DOT, identifier),
            and(bridge(JavaPunctuator.LPAR, JavaPunctuator.RPAR), JavaPunctuator.DOT, identifier))));
    methodCall.is(identifier,
        one2n(or(
            bridge(JavaPunctuator.LPAR, JavaPunctuator.RPAR),
            and(one2n(JavaPunctuator.DOT, identifier), bridge(JavaPunctuator.LPAR, JavaPunctuator.RPAR)))));
  }

  @Override
  public Rule getRootRule() {
    return null;
  }

  public Parser<JavaPatternGrammar> getParser(Lexer lexer) {
    return Parser.builder((JavaPatternGrammar) this)
        .withLexer(lexer)
        .build();
  }

}
