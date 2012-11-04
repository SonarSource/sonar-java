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

import com.google.common.collect.Lists;
import com.sonar.sslr.api.Rule;
import com.sonar.sslr.api.Token;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class PatternMatcherBuilder {

  private final Extractor<Token> extractorI;
  private final Extractor<Token> extractorJ;
  private final Comparator<Token> comparator;
  private final PrefixParser prefixParser;
  private final Classifier classifier;

  public PatternMatcherBuilder(ElementSequence<Token> inputI, ElementSequence<Token> inputJ, Comparator<Token> comparator, PrefixParser prefixParser, Set<Rule> rules) {
    checkNotNull(inputI);
    checkNotNull(inputJ);
    checkNotNull(comparator);
    checkNotNull(prefixParser);
    checkNotNull(rules);
    checkArgument(rules.size() >= 1, "rules must contain at least one element");

    this.extractorI = new Extractor<Token>(inputI);
    this.extractorJ = new Extractor<Token>(inputJ);
    this.comparator = comparator;
    this.prefixParser = prefixParser;
    this.classifier = new Classifier(prefixParser, rules);
  }

  private CommonPatternMatcher getPatternMatcher(CommonGroup commonGroup, PatternMatcher nextPatternMatcher) {
    List<Token> tokensToMatch = extractorI.getExtraction(commonGroup.getIndexesI());

    return new CommonPatternMatcher(tokensToMatch, comparator, nextPatternMatcher);
  }

  private VaryingPatternMatcher getPatternMatcher(VaryingGroup varyingGroup, CommonPatternMatcher nextCommonPatternMatcher) {
    List<List<Token>> inputsTokens = Lists.newArrayList(
        extractorI.getExtraction(varyingGroup.getIndexesI()),
        extractorJ.getExtraction(varyingGroup.getIndexesJ()));

    Set<Rule> rules = classifier.getMatchingRules(inputsTokens);

    return new VaryingPatternMatcher(prefixParser, rules, nextCommonPatternMatcher);
  }

  private PatternMatcher getPatternMatcher(Group group, PatternMatcher nextPatternMatcher) {
    if (group instanceof CommonGroup) {
      return getPatternMatcher((CommonGroup) group, nextPatternMatcher);
    } else {
      return getPatternMatcher((VaryingGroup) group, (CommonPatternMatcher) nextPatternMatcher);
    }
  }

  public PatternMatcher getPatternMatcher(List<Group> groups) {
    PatternMatcher currentPatternMatcher = null;

    for (int i = groups.size() - 1; i >= 0; i--) {
      currentPatternMatcher = getPatternMatcher(groups.get(i), currentPatternMatcher);
    }

    return currentPatternMatcher;
  }

}
