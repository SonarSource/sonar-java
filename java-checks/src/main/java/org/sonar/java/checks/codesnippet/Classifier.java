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
import com.sonar.sslr.api.RecognitionException;
import com.sonar.sslr.api.Rule;
import com.sonar.sslr.impl.Parser;

import java.util.Collection;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class Classifier {

  private final Parser<? extends Grammar> parser;
  private final Set<Rule> rules;

  public Classifier(Parser<? extends Grammar> parser, Set<Rule> rules) {
    this.parser = parser;
    this.rules = rules;
  }

  public Set<Rule> getMatchingRules(Collection<String> inputs) {
    checkNotNull(inputs);
    checkArgument(!inputs.isEmpty(), "inputs cannot be empty");

    Set<Rule> matchingRules = Sets.newHashSet();

    for (Rule rule : rules) {
      parser.setRootRule(rule);

      try {
        for (String input : inputs) {
          parser.parse(input);
        }
        matchingRules.add(rule);
      } catch (RecognitionException re) {
        /* At least one of the inputs did not match */
      }
    }

    return matchingRules;
  }

}
