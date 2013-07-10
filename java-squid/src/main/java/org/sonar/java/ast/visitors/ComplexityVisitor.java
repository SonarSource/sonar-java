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
package org.sonar.java.ast.visitors;

import com.sonar.sslr.api.AstNode;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.api.JavaMetric;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.parser.JavaGrammar;

public class ComplexityVisitor extends JavaAstVisitor {

  @Override
  public void init() {
    subscribeTo(
        // Entry points
        JavaGrammar.METHOD_BODY,
        // Branching nodes
        JavaGrammar.IF_STATEMENT,
        JavaGrammar.FOR_STATEMENT,
        JavaGrammar.WHILE_STATEMENT,
        JavaGrammar.DO_STATEMENT,
        JavaKeyword.CASE,
        JavaGrammar.RETURN_STATEMENT,
        JavaGrammar.THROW_STATEMENT,
        JavaGrammar.CATCH_CLAUSE,
        // Expressions
        JavaGrammar.CONDITIONAL_EXPRESSION,
        JavaPunctuator.ANDAND,
        JavaPunctuator.OROR);
  }

  @Override
  public void visitNode(AstNode astNode) {
    if (astNode.is(JavaGrammar.RETURN_STATEMENT) && isLastReturnStatement(astNode)) {
      return;
    }
    getContext().peekSourceCode().add(JavaMetric.COMPLEXITY, 1);
  }

  private boolean isLastReturnStatement(AstNode astNode) {
    AstNode parent = astNode.getParent().getParent().getParent();
    AstNode block = astNode.getFirstAncestor(JavaGrammar.BLOCK_STATEMENTS);
    return block.getParent().getParent().is(JavaGrammar.METHOD_BODY) && parent == block;
  }

}
