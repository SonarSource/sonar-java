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
import com.sonar.sslr.api.AstAndTokenVisitor;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;
import org.sonar.squidbridge.checks.SquidCheck;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.Set;

@Rule(
  key = "RightCurlyBraceSameLineAsNextBlockCheck",
  name = "Close curly brace and the next \"else\", \"catch\" and \"finally\" keywords should be located on the same line",
  tags = {"convention"},
  priority = Priority.MINOR)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("1min")
public class RightCurlyBraceSameLineAsNextBlockCheck extends SquidCheck<LexerlessGrammar> implements AstAndTokenVisitor, JavaCheck {

  private static final Set<String> NEXT_BLOCKS = ImmutableSet.of(
      "else",
      "catch",
      "finally");

  private boolean lastTokenIsRightCurlyBrace;
  private int lastTokenLine;

  @Override
  public void visitFile(AstNode astNode) {
    lastTokenIsRightCurlyBrace = false;
    lastTokenLine = -1;
  }

  @Override
  public void visitToken(Token token) {
    if (lastTokenIsRightCurlyBrace && lastTokenLine != token.getLine() && NEXT_BLOCKS.contains(token.getValue())) {
      getContext().createLineViolation(this, "Move this \"" + token.getValue() + "\" on the same line that the previous closing curly brace.", token);
    }

    lastTokenIsRightCurlyBrace = "}".equals(token.getValue());
    lastTokenLine = token.getLine();
  }

}
