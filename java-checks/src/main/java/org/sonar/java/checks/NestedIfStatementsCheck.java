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

import com.sonar.sslr.squid.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.model.*;
import org.sonar.sslr.parser.LexerlessGrammar;

@Rule(
  key = "S134",
  priority = Priority.MINOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MINOR)
public class NestedIfStatementsCheck extends SquidCheck<LexerlessGrammar> implements JavaTreeVisitorProvider {

  private static final int DEFAULT_MAX = 3;

  @RuleProperty(defaultValue = "" + DEFAULT_MAX)
  public int max = DEFAULT_MAX;

  @Override
  public JavaTreeVisitor createJavaTreeVisitor() {
    return new BaseTreeVisitor() {
      private int nestingLevel;

      @Override
      public void visitCompilationUnit(CompilationUnitTree tree) {
        nestingLevel = 0;
        super.visitCompilationUnit(tree);
      }

      @Override
      public void visitIfStatement(IfStatementTree tree) {
        nestingLevel++;
        if (nestingLevel == max + 1) {
          getContext().createLineViolation(NestedIfStatementsCheck.this, "Refactor this code to not nest more than " + max + " if statements.", ((JavaTree) tree).getLine());
        }
        visit(tree);
        nestingLevel--;
      }

      private void visit(IfStatementTree tree) {
        scan(tree.condition());
        scan(tree.thenStatement());

        StatementTree elseStatementTree = tree.elseStatement();
        if (elseStatementTree != null && elseStatementTree.is(Tree.Kind.IF_STATEMENT)) {
          visit((IfStatementTree) elseStatementTree);
        } else {
          scan(elseStatementTree);
        }
      }
    };
  }

}
