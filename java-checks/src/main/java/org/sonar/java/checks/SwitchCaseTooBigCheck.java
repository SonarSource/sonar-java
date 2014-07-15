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
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

@Rule(
  key = "S1151",
  priority = Priority.MAJOR,
  tags={"brain-overload"})
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class SwitchCaseTooBigCheck extends SquidCheck<LexerlessGrammar> {

  private static final int DEFAULT_MAX = 5;

  @RuleProperty(defaultValue = "" + DEFAULT_MAX)
  public int max = DEFAULT_MAX;

  @Override
  public void init() {
    subscribeTo(JavaGrammar.SWITCH_BLOCK_STATEMENT_GROUP);
  }

  @Override
  public void visitNode(AstNode node) {
    int lines = getNumberOfLines(node);
    if (lines > max) {
      getContext().createLineViolation(this, "Reduce this switch case number of lines from " + lines + " to at most " + max + ", for example by extracting code into methods.",
          node);
    }
  }

  private static int getNumberOfLines(AstNode node) {
    return Math.max(node.getNextAstNode().getTokenLine() - node.getTokenLine(), 1);
  }

}
