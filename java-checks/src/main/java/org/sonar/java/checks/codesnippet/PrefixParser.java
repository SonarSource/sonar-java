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

import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.api.RecognitionException;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.impl.Parser;

import java.util.List;

public class PrefixParser {

  public final Parser<? extends Grammar> parser;

  public PrefixParser(Parser<? extends Grammar> parser) {
    this.parser = parser;
  }

  public PrefixParseResult parse(List<Token> tokens) {

    try {
      parser.parse(tokens);

      int lastAttemptedTokenIndex = parser.getParsingState().getOutpostMatcherTokenIndex();

      return lastAttemptedTokenIndex == tokens.size() - 1 ?
          PrefixParseResult.FULL_MATCH :
          PrefixParseResult.MISMATCH;
    } catch (RecognitionException re) {
      int lastAttemptedTokenIndex = parser.getParsingState().getOutpostMatcherTokenIndex();

      return lastAttemptedTokenIndex == tokens.size() ?
          PrefixParseResult.PARTIAL_MATCH :
          PrefixParseResult.MISMATCH;
    }
  }

  enum PrefixParseResult {
    MISMATCH,
    FULL_MATCH,
    PARTIAL_MATCH
  }

}
