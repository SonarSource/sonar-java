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
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

@Rule(
  key = "S1067",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class ExpressionComplexityCheck extends SquidCheck<LexerlessGrammar> {

  private static final int DEFAULT_MAX = 3;

  @RuleProperty(defaultValue = "" + DEFAULT_MAX)
  public int max = DEFAULT_MAX;

  private int nestingLevel;
  private int operatorCounter;

  @Override
  public void init() {
    subscribeTo(JavaGrammar.EXPRESSION);
    subscribeTo(JavaGrammar.CONDITIONAL_EXPRESSION, JavaGrammar.CONDITIONAL_OR_EXPRESSION, JavaGrammar.CONDITIONAL_AND_EXPRESSION);
  }

  @Override
  public void visitFile(AstNode node) {
    nestingLevel = 0;
  }

  @Override
  public void visitNode(AstNode node) {
    if (node.is(JavaGrammar.EXPRESSION)) {
      nestingLevel++;
      if (nestingLevel == 1) {
        operatorCounter = 0;
      }
    } else {
      operatorCounter += node.getChildren(JavaPunctuator.QUERY, JavaPunctuator.OROR, JavaPunctuator.ANDAND).size();
    }
  }

  @Override
  public void leaveNode(AstNode node) {
    if (node.is(JavaGrammar.EXPRESSION)) {
      nestingLevel--;
      if (nestingLevel == 0 && operatorCounter > max) {
        getContext().createLineViolation(
            this,
            "Reduce the number of conditional operators (" + operatorCounter + ") used in the expression (maximum allowed " + max + ").",
            node);
      }
    }
  }

}
