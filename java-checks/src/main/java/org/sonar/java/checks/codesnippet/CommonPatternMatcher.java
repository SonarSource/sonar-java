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

import com.sonar.sslr.api.Token;

import java.util.Comparator;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class CommonPatternMatcher extends PatternMatcher {

  private final List<Token> tokensToMatch;
  private final Comparator<Token> comparator;

  public CommonPatternMatcher(List<Token> tokensToMatch, Comparator<Token> comparator) {
    this(tokensToMatch, comparator, null);
  }

  public CommonPatternMatcher(List<Token> tokensToMatch, Comparator<Token> comparator, PatternMatcher nextPatternMatcher) {
    super(nextPatternMatcher);

    checkNotNull(tokensToMatch);
    checkArgument(tokensToMatch.size() >= 1, "tokensToMatch must contain at least one element");

    checkNotNull(comparator);

    this.tokensToMatch = tokensToMatch;
    this.comparator = comparator;
  }

  public List<Token> getTokensToMatch() {
    return tokensToMatch;
  }

  public Comparator<Token> getComparator() {
    return comparator;
  }

  @Override
  public boolean isMatching(List<Token> tokens) {
    if (tokens.size() < tokensToMatch.size()) {
      return false;
    }

    for (int i = 0; i < tokensToMatch.size(); i++) {
      Token tokenToMatch = tokensToMatch.get(i);
      Token token = tokens.get(i);

      if (comparator.compare(tokenToMatch, token) != 0) {
        return false;
      }
    }

    if (hasNextPatternMatcher()) {
      return getNextPatternMatcher().isMatching(tokens.subList(tokensToMatch.size(), tokens.size()));
    } else {
      return true;
    }
  }

}
