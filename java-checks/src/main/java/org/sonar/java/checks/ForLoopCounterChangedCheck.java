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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.squid.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.ast.AstSelect;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.Collections;
import java.util.Set;

@Rule(
  key = "ForLoopCounterChangedCheck",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class ForLoopCounterChangedCheck extends SquidCheck<LexerlessGrammar> {

  private final Set<String> loopCounters = Sets.newHashSet();

  @Override
  public void init() {
    subscribeTo(JavaGrammar.FOR_STATEMENT);
    subscribeTo(JavaGrammar.ASSIGNMENT_EXPRESSION);
  }

  @Override
  public void visitFile(AstNode astNode) {
    loopCounters.clear();
  }

  @Override
  public void visitNode(AstNode node) {
    if (node.is(JavaGrammar.FOR_STATEMENT)) {
      loopCounters.addAll(getLoopCounters(node));
    } else if (!loopCounters.isEmpty()) {
      for (int i = 0; i < node.getNumberOfChildren() - 1; i++) {
        if (loopCounters.contains(merge(node.getChild(i)))) {
          getContext().createLineViolation(this, "Refactor the code in order to not assign to this loop counter from within the loop body.", node.getChild(i));
        }
      }
    }
  }

  @Override
  public void leaveNode(AstNode node) {
    if (node.is(JavaGrammar.FOR_STATEMENT)) {
      loopCounters.removeAll(getLoopCounters(node));
    }
  }

  private Set<String> getLoopCounters(AstNode node) {
    Set<String> result;

    AstSelect identifiers = node.select()
        .children(JavaGrammar.FOR_INIT)
        .children(JavaGrammar.VARIABLE_DECLARATORS)
        .children(JavaGrammar.VARIABLE_DECLARATOR)
        .children(JavaTokenType.IDENTIFIER);

    if (identifiers.isNotEmpty()) {
      ImmutableSet.Builder<String> builder = ImmutableSet.builder();

      for (AstNode identifier : identifiers) {
        builder.add(identifier.getTokenOriginalValue());
      }

      result = builder.build();
    }
    else {
      result = Collections.EMPTY_SET;
    }

    return result;
  }

  private static String merge(AstNode node) {
    StringBuilder sb = new StringBuilder();

    for (Token token : node.getTokens()) {
      sb.append(token.getOriginalValue());
    }

    return sb.toString();
  }

}
