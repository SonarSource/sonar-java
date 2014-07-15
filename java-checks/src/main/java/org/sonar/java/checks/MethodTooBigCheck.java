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
import org.sonar.squidbridge.checks.SquidCheck;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

@Rule(
  key = "S138",
  priority = Priority.MAJOR,
  tags={"brain-overload"})
public class MethodTooBigCheck extends SquidCheck<LexerlessGrammar> {

  private static final int DEFAULT_MAX = 100;

  @RuleProperty(defaultValue = "" + DEFAULT_MAX)
  public int max = DEFAULT_MAX;

  @Override
  public void init() {
    subscribeTo(JavaGrammar.METHOD_BODY);
  }

  @Override
  public void visitNode(AstNode node) {
    int lines = getLines(node);

    if (lines > max) {
      getContext().createLineViolation(this, "This method has " + lines + " lines, which is greater than the " + max + " lines authorized. Split it into smaller methods.", node);
    }
  }

  private static int getLines(AstNode node) {
    AstNode block = node.getFirstChild(JavaGrammar.BLOCK);

    AstNode leftCurlyBrace = block.getFirstChild(JavaPunctuator.LWING);
    AstNode rightCurlyBrace = block.getFirstChild(JavaPunctuator.RWING);

    return rightCurlyBrace.getTokenLine() - leftCurlyBrace.getTokenLine() + 1;
  }

}
