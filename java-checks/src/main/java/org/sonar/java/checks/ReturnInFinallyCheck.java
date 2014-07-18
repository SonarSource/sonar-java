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
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.squidbridge.checks.SquidCheck;
import org.sonar.sslr.parser.LexerlessGrammar;

@Rule(
    key = "S1143",
    priority = Priority.BLOCKER,
    tags = {"bug"})
@BelongsToProfile(title = "Sonar way", priority = Priority.BLOCKER)
public class ReturnInFinallyCheck extends SquidCheck<LexerlessGrammar> {

  @Override
  public void init() {
    subscribeTo(JavaGrammar.RETURN_STATEMENT);
  }

  @Override
  public void visitNode(AstNode node) {
    if (isInFinally(node)) {
      getContext().createLineViolation(this, "Remove this return statement from this finally block.", node);
    }
  }

  private static boolean isInFinally(AstNode node) {
    AstNode ancestor = node.getFirstAncestor(JavaGrammar.FINALLY_, JavaGrammar.CLASS_BODY);
    return ancestor != null && ancestor.is(JavaGrammar.FINALLY_);
  }

}
