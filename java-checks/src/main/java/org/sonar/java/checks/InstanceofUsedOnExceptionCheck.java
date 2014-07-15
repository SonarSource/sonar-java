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
import org.sonar.squidbridge.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.Set;

@Rule(
  key = "S1193",
  priority = Priority.MAJOR,
  tags={"error-handling"})
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class InstanceofUsedOnExceptionCheck extends SquidCheck<LexerlessGrammar> {

  private final Set<String> caughtVariables = Sets.newHashSet();

  @Override
  public void init() {
    subscribeTo(JavaGrammar.CATCH_CLAUSE);
    subscribeTo(JavaKeyword.INSTANCEOF);
  }

  @Override
  public void visitFile(AstNode node) {
    caughtVariables.clear();
  }

  @Override
  public void visitNode(AstNode node) {
    if (node.is(JavaGrammar.CATCH_CLAUSE)) {
      caughtVariables.add(getCaughtVariable(node));
    } else if (isLeftOperandAnException(node)) {
      getContext().createLineViolation(this, "Replace the usage of the \"instanceof\" operator by a catch block.", node);
    }
  }

  @Override
  public void leaveNode(AstNode node) {
    if (node.is(JavaGrammar.CATCH_CLAUSE)) {
      caughtVariables.remove(getCaughtVariable(node));
    }
  }

  private static String getCaughtVariable(AstNode catchClause) {
    return catchClause.getFirstChild(JavaGrammar.CATCH_FORMAL_PARAMETER)
        .getFirstChild(JavaGrammar.VARIABLE_DECLARATOR_ID)
        .getTokenOriginalValue();
  }

  private boolean isLeftOperandAnException(AstNode node) {
    AstNode leftOperand = node.getPreviousSibling();

    return leftOperand.getToken().equals(leftOperand.getLastToken()) &&
      caughtVariables.contains(leftOperand.getTokenOriginalValue());
  }

}
