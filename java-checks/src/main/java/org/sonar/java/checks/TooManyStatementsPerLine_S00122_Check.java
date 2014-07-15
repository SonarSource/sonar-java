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

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.sonar.sslr.api.AstNode;
import org.sonar.squidbridge.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.HashMap;
import java.util.Map;

@Rule(
    key = "S00122",
    priority = Priority.MAJOR,
    tags = {"convention"})
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class TooManyStatementsPerLine_S00122_Check extends SquidCheck<LexerlessGrammar> {

  private final Multiset<Integer> statementsPerLine = HashMultiset.create();
  private final Map<Integer, Integer> columnsByLine = new HashMap<Integer, Integer>();

  @Override
  public void init() {
    subscribeTo(JavaGrammar.STATEMENT, JavaGrammar.LOCAL_VARIABLE_DECLARATION_STATEMENT);
  }

  public boolean isExcluded(AstNode astNode) {
    AstNode statementNode = astNode.getFirstChild();
    return statementNode.is(JavaGrammar.BLOCK)
        || statementNode.is(JavaGrammar.EMPTY_STATEMENT)
        || statementNode.is(JavaGrammar.LABELED_STATEMENT);
  }

  @Override
  public void visitFile(AstNode astNode) {
    statementsPerLine.clear();
    columnsByLine.clear();
  }

  @Override
  public void visitNode(AstNode statementNode) {
    if (!isExcluded(statementNode)) {
      int lineStart = statementNode.getTokenLine();
      int lineEnd = statementNode.getLastToken().getLine();
      int columnStart = statementNode.getToken().getColumn();
      int columnEnd = statementNode.getLastToken().getColumn();

      if (!isNestedInStatement(lineStart, columnStart)) {
        statementsPerLine.add(lineStart);
      }
      if (lineStart != lineEnd) {
        if (!isNestedInStatement(lineEnd, columnEnd)) {
          statementsPerLine.add(lineEnd);
        }
        columnsByLine.put(lineEnd, columnEnd);
      }
    }
  }

  private boolean isNestedInStatement(int line, int column) {
    if (columnsByLine.get(line) != null && columnsByLine.get(line) >= column) {
      columnsByLine.remove(line);
      return true;
    }
    return false;
  }

  @Override
  public void leaveFile(AstNode astNode) {
    for (Multiset.Entry<Integer> statementsAtLine : statementsPerLine.entrySet()) {
      if (statementsAtLine.getCount() > 1) {
        getContext().createLineViolation(this, "At most one statement is allowed per line, but {0} statements were found on this line.", statementsAtLine.getElement(),
            statementsAtLine.getCount());
      }
    }
  }

}
