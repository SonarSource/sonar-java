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

import com.google.common.collect.Sets;
import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.api.Rule;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.impl.Parser;
import org.sonar.java.checks.codesnippet.PrefixParser.PrefixParseResult;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class Classifier {

  private final PrefixParser prefixParser;
  private final Parser<? extends Grammar> parser;
  private final Set<Rule> rules;

  public Classifier(Parser<? extends Grammar> parser, Set<Rule> rules) {
    this.prefixParser = new PrefixParser(parser);
    this.parser = parser;
    this.rules = rules;
  }

  public Set<Rule> getMatchingRules(Collection<List<Token>> inputsTokens) {
    checkNotNull(inputsTokens);
    checkArgument(!inputsTokens.isEmpty(), "inputsTokens cannot be empty");

    Set<Rule> matchingRules = Sets.newHashSet();

    for (Rule rule : rules) {
      parser.setRootRule(rule);

      boolean allInputsMatched = true;
      for (List<Token> inputTokens : inputsTokens) {
        if (prefixParser.parse(inputTokens) != PrefixParseResult.FULL_MATCH) {
          allInputsMatched = false;
          break;
        }
      }
      if (allInputsMatched) {
        matchingRules.add(rule);
      }
    }

    return matchingRules;
  }

}
