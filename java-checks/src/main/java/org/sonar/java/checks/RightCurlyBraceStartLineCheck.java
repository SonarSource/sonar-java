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

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;
import org.sonar.squidbridge.checks.SquidCheck;
import org.sonar.sslr.parser.LexerlessGrammar;

@Rule(
  key = "RightCurlyBraceStartLineCheck",
  name = "A close curly brace should be located at the beginning of a line",
  tags = {"convention"},
  priority = Priority.MINOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("1min")
public class RightCurlyBraceStartLineCheck extends SquidCheck<LexerlessGrammar> implements JavaCheck {

  @Override
  public void init() {
    subscribeTo(JavaPunctuator.RWING);
  }

  @Override
  public void visitNode(AstNode node) {
    if (!isExcluded(node) && hasSomeCodeBefore(node)) {
      getContext().createLineViolation(this, "Move this closing curly brace to the next line.", node);
    }
  }

  private static boolean hasSomeCodeBefore(AstNode node) {
    Token previousToken = getPreviousToken(node);
    return previousToken != null && previousToken.getLine() == node.getTokenLine();
  }

  private static boolean isExcluded(AstNode node) {
    return node.getParent().is(Kind.NEW_ARRAY);
  }

  private static Token getPreviousToken(AstNode node) {
    AstNode result = node.getPreviousAstNode();

    while (result != null && !result.hasToken()) {
      while (result != null && !result.hasToken()) {
        result = result.getPreviousAstNode();
      }

      while (result != null && result.getLastChild() != null) {
        result = result.getLastChild();
      }
    }

    return result == null ? null : result.getToken();
  }

}
