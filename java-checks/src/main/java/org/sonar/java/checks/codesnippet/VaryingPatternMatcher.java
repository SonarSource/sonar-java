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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.sonar.sslr.api.Rule;
import com.sonar.sslr.api.Token;
import org.sonar.java.checks.codesnippet.PrefixParser.PrefixParseResult;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class VaryingPatternMatcher extends PatternMatcher {

  private final PrefixParser prefixParser;
  private final Set<Rule> rules;

  public VaryingPatternMatcher(PrefixParser prefixParser, Set<Rule> rules, CommonPatternMatcher nextCommonPatternMatcher) {
    super(nextCommonPatternMatcher);

    checkNotNull(prefixParser);
    checkNotNull(rules);
    checkArgument(rules.size() >= 1, "rules must contain at least one element");

    this.prefixParser = prefixParser;
    this.rules = rules;
  }

  @Override
  public boolean isMatching(List<Token> tokens) {
    Set<Rule> ruleCandidates = Sets.newHashSet(rules);
    Token nextCommonPatternMatcherTokenToMatch = getNextCommonPatternMatcherTokenToMatch();

    int prefixMatchTokens = 0;
    for (Token token : tokens) {
      if (getNextCommonPatternMatcherComparator().compare(token, nextCommonPatternMatcherTokenToMatch) == 0) {
        ImmutableSet.Builder<Rule> mismatchingRules = ImmutableSet.builder();

        for (Rule ruleCandidate : ruleCandidates) {
          PrefixParser.PrefixParseResult prefixParseResult = prefixParser.parse(ruleCandidate, tokens.subList(0, prefixMatchTokens));

          if (prefixParseResult == PrefixParseResult.MISMATCH) {
            mismatchingRules.add(ruleCandidate);
          } else if (prefixParseResult == PrefixParseResult.FULL_MATCH &&
            getNextPatternMatcher().isMatching(tokens.subList(prefixMatchTokens, tokens.size()))) {

            return true;
          }
        }

        ruleCandidates.removeAll(mismatchingRules.build());

        if (ruleCandidates.isEmpty()) {
          break;
        }
      }

      prefixMatchTokens++;
    }

    return false;
  }

  @VisibleForTesting
  Token getNextCommonPatternMatcherTokenToMatch() {
    CommonPatternMatcher commonPatternMatcher = (CommonPatternMatcher) getNextPatternMatcher();
    return commonPatternMatcher.getTokensToMatch().get(0);
  }

  @VisibleForTesting
  Comparator<Token> getNextCommonPatternMatcherComparator() {
    CommonPatternMatcher commonPatternMatcher = (CommonPatternMatcher) getNextPatternMatcher();
    return commonPatternMatcher.getComparator();
  }

}
