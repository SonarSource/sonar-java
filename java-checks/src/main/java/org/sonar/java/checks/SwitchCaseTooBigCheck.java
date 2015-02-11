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

import com.google.common.collect.Iterables;
import com.sonar.sslr.api.AstNode;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.ast.parser.JavaLexer;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;
import org.sonar.squidbridge.checks.SquidCheck;
import org.sonar.sslr.parser.LexerlessGrammar;

@Rule(
  key = "S1151",
  name = "\"switch case\" clauses should not have too many lines",
  tags = {"brain-overload"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("5min")
public class SwitchCaseTooBigCheck extends SquidCheck<LexerlessGrammar> {

  private static final int DEFAULT_MAX = 5;

  @RuleProperty(defaultValue = "" + DEFAULT_MAX,
  description = "Maximum number of lines")
  public int max = DEFAULT_MAX;

  @Override
  public void init() {
    subscribeTo(JavaLexer.SWITCH_BLOCK_STATEMENT_GROUP);
  }

  @Override
  public void visitNode(AstNode node) {
    CaseGroupTree tree = (CaseGroupTree) node;

    for (int i = 0; i < tree.labels().size() - 1; i++) {
      int caseStartLine = ((JavaTree) tree.labels().get(i)).getLine();
      int nextCaseStartLine = ((JavaTree) tree.labels().get(i + 1)).getLine();

      check(caseStartLine, nextCaseStartLine);
    }

    check(((JavaTree) Iterables.getLast(tree.labels())).getLine(), node.getNextAstNode().getTokenLine());
  }

  private void check(int caseStartLine, int nextCaseStartLine) {
    int lines = Math.max(nextCaseStartLine - caseStartLine, 1);

    if (lines > max) {
      getContext().createLineViolation(
        this,
        "Reduce this switch case number of lines from " + lines + " to at most " + max + ", for example by extracting code into methods.",
        caseStartLine);
    }
  }

}
