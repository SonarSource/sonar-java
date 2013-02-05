/*
 * Sonar Java
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
package org.sonar.java.ast.visitors;

import com.sonar.sslr.api.AstNode;
import org.sonar.java.ast.api.JavaGrammar;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.api.JavaMetric;
import org.sonar.java.ast.api.JavaPunctuator;

public class ComplexityVisitor extends JavaAstVisitor {

  @Override
  public void init() {
    JavaGrammar grammar = getContext().getGrammar();
    subscribeTo(
        // Entry points
        grammar.methodBody,
        // Branching nodes
        grammar.ifStatement,
        grammar.forStatement,
        grammar.whileStatement,
        grammar.doStatement,
        JavaKeyword.CASE,
        grammar.returnStatement,
        grammar.throwStatement,
        grammar.catchClause,
        // Expressions
        grammar.conditionalExpression,
        JavaPunctuator.ANDAND,
        JavaPunctuator.OROR);
  }

  @Override
  public void visitNode(AstNode astNode) {
    if (astNode.is(getContext().getGrammar().returnStatement) && isLastReturnStatement(astNode)) {
      return;
    }
    getContext().peekSourceCode().add(JavaMetric.COMPLEXITY, 1);
  }

  private boolean isLastReturnStatement(AstNode astNode) {
    AstNode parent = astNode.getParent().getParent().getParent();
    AstNode block = astNode.getFirstAncestor(getContext().getGrammar().blockStatements);
    return block.getParent().getParent().is(getContext().getGrammar().methodBody) && parent == block;
  }

}
