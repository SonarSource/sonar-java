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
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;
import org.sonar.squidbridge.checks.SquidCheck;
import org.sonar.sslr.parser.LexerlessGrammar;

@Rule(
  key = "StringEqualityComparisonCheck",
  name = "Strings should be compared using \"equals()\"",
  tags = {"bug", "cwe"},
  status = "DEPRECATED",
  priority = Priority.CRITICAL)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.INSTRUCTION_RELIABILITY)
@SqaleConstantRemediation("5min")
public class StringEqualityComparisonCheck extends SquidCheck<LexerlessGrammar> implements JavaCheck {

  @Override
  public void init() {
    subscribeTo(Kind.EQUAL_TO);
    subscribeTo(Kind.NOT_EQUAL_TO);
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
      .children(Kind.STRING_LITERAL)
      .isNotEmpty();
  }

}
