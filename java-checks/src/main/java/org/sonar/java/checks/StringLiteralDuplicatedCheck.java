/*
 * SonarQube Java
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

import com.google.common.collect.Maps;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.squid.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.Map;

@Rule(
  key = "S1192",
  priority = Priority.MINOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MINOR)
public class StringLiteralDuplicatedCheck extends SquidCheck<LexerlessGrammar> {

  private static final Integer MINIMAL_LITERAL_LENGTH = 7;

  private final Map<String, Integer> firstOccurrence = Maps.newHashMap();
  private final Map<String, Integer> literalsOccurrences = Maps.newHashMap();

  private boolean inAnnotation;

  @Override
  public void init() {
    subscribeTo(JavaGrammar.ANNOTATION_REST);
    subscribeTo(JavaTokenType.LITERAL);
  }

  @Override
  public void visitFile(AstNode node) {
    inAnnotation = false;
    firstOccurrence.clear();
    literalsOccurrences.clear();
  }

  @Override
  public void visitNode(AstNode node) {
    if (node.is(JavaGrammar.ANNOTATION_REST)) {
      inAnnotation = true;
    } else if (!inAnnotation) {
      visitOccurence(node.getTokenOriginalValue(), node.getTokenLine());
    }
  }

  @Override
  public void leaveNode(AstNode node) {
    if (node.is(JavaGrammar.ANNOTATION_REST)) {
      inAnnotation = false;
    }
  }

  @Override
  public void leaveFile(AstNode node) {
    for (Map.Entry<String, Integer> literalOccurences : literalsOccurrences.entrySet()) {
      Integer occurences = literalOccurences.getValue();

      if (occurences > 1) {
        String literal = literalOccurences.getKey();

        getContext().createLineViolation(this, "Define a constant instead of duplicating this literal " + literal + " " + occurences + " times.", firstOccurrence.get(literal));
      }
    }
  }

  private void visitOccurence(String literal, int line) {
    if (literal.length() >= MINIMAL_LITERAL_LENGTH) {
      if (!firstOccurrence.containsKey(literal)) {
        firstOccurrence.put(literal, line);
        literalsOccurrences.put(literal, 1);
      } else {
        int occurences = literalsOccurrences.get(literal);
        literalsOccurrences.put(literal, occurences + 1);
      }
    }
  }

}
