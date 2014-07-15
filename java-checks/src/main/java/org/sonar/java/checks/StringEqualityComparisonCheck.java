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
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

@Rule(
  key = "StringEqualityComparisonCheck",
  priority = Priority.CRITICAL,
  status = "DEPRECATED",
  tags={"bug"})
public class StringEqualityComparisonCheck extends SquidCheck<LexerlessGrammar> {

  @Override
  public void init() {
    subscribeTo(JavaGrammar.EQUALITY_EXPRESSION);
  }

  @Override
  public void visitNode(AstNode node) {
    if (hasStringLiteralOperand(node)) {
      getContext().createLineViolation(
          this,
          "Replace \"==\" and \"!=\" by \"equals()\" and \"!equals()\" respectively to compare these strings.",
          node);
    }
  }

  private static boolean hasStringLiteralOperand(AstNode node) {
    return node.select()
        .children(JavaGrammar.PRIMARY)
        .children(JavaGrammar.LITERAL)
        .descendants(JavaTokenType.LITERAL)
        .isNotEmpty();
  }

}
