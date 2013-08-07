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
import com.sonar.sslr.squid.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

@Rule(
  key = "S134",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class NestedIfStatementsCheck extends SquidCheck<LexerlessGrammar> {

  private static final int DEFAULT_MAX = 3;

  @RuleProperty(defaultValue = "" + DEFAULT_MAX)
  public int max = DEFAULT_MAX;

  private int nestingLevel;

  @Override
  public void init() {
    subscribeTo(JavaGrammar.IF_STATEMENT);
  }

  @Override
  public void visitFile(AstNode node) {
    nestingLevel = 0;
  }

  @Override
  public void visitNode(AstNode node) {
    nestingLevel++;

    if (nestingLevel == max + 1) {
      getContext().createLineViolation(this, "Refactor this code to not nest more than " + max + " if statements.", node);
    }
  }

  @Override
  public void leaveNode(AstNode node) {
    nestingLevel--;
  }

}
