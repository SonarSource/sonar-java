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
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

@Rule(
  key = "LeftCurlyBraceStartLineCheck",
  priority = Priority.MAJOR,
  tags={"convention"})
public class LeftCurlyBraceStartLineCheck extends SquidCheck<LexerlessGrammar> {

  @Override
  public void init() {
    subscribeTo(JavaPunctuator.LWING);
  }

  @Override
  public void visitNode(AstNode node) {
    if (!isExcluded(node) && node.getPreviousAstNode().getLastToken().getLine() == node.getTokenLine()) {
      getContext().createLineViolation(this, "Move this left curly brace to the beginning of next line of code.", node);
    }
  }

  private static boolean isExcluded(AstNode node) {
    return node.getParent().is(
        JavaGrammar.ELEMENT_VALUE_ARRAY_INITIALIZER,
        JavaGrammar.ARRAY_INITIALIZER);
  }

}
