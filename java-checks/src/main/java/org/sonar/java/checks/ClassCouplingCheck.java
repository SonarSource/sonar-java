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

import com.google.common.collect.Sets;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.squid.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.List;
import java.util.Set;
import java.util.Stack;

@Rule(
  key = "S1200",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class ClassCouplingCheck extends SquidCheck<LexerlessGrammar> {

  private static final int DEFAULT_MAX = 20;

  @RuleProperty(
    key = "max",
    defaultValue = "" + DEFAULT_MAX)
  public int max = DEFAULT_MAX;

  private final Stack<Set<String>> nesting = new Stack<Set<String>>();
  private Set<String> types;

  @Override
  public void init() {
    subscribeTo(
      JavaGrammar.CLASS_DECLARATION,
      JavaGrammar.TYPE);
  }

  @Override
  public void visitNode(AstNode node) {
    if (node.is(JavaGrammar.CLASS_DECLARATION)) {
      nesting.push(types);
      types = Sets.newHashSet();
    } else if (types != null) {
      types.add(joinTokens(node.getTokens()));
    }
  }

  @Override
  public void leaveNode(AstNode node) {
    if (node.is(JavaGrammar.CLASS_DECLARATION)) {
      if (types.size() > max) {
        getContext().createLineViolation(
          this,
          "Split this class into smaller and more specialized ones to reduce its dependencies on other classes from " +
            types.size() + " to the maximum authorized " + max + " or less.",
          node);
      }

      types = nesting.pop();
    }
  }

  private String joinTokens(List<Token> tokens) {
    StringBuilder sb = new StringBuilder();
    for (Token token : tokens) {
      sb.append(token.getOriginalValue());
    }
    return sb.toString();
  }

}
