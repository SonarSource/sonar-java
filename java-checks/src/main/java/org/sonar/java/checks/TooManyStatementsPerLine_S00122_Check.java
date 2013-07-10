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
import com.sonar.sslr.squid.checks.AbstractOneStatementPerLineCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

@Rule(
  key = "S00122",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class TooManyStatementsPerLine_S00122_Check extends AbstractOneStatementPerLineCheck<LexerlessGrammar> {

  @Override
  public com.sonar.sslr.api.Rule getStatementRule() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void init() {
    subscribeTo(JavaGrammar.STATEMENT, JavaGrammar.LOCAL_VARIABLE_DECLARATION_STATEMENT);
  }

  @Override
  public boolean isExcluded(AstNode astNode) {
    AstNode statementNode = astNode.getChild(0);
    return statementNode.is(JavaGrammar.BLOCK)
      || statementNode.is(JavaGrammar.EMPTY_STATEMENT)
      || statementNode.is(JavaGrammar.LABELED_STATEMENT);
  }

}
