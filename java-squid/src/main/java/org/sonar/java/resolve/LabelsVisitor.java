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
package org.sonar.java.resolve;

import com.sonar.sslr.api.AstNode;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.java.ast.visitors.JavaAstVisitor;

public class LabelsVisitor extends JavaAstVisitor {

  private final SemanticModel semanticModel;

  public LabelsVisitor(SemanticModel semanticModel) {
    this.semanticModel = semanticModel;
  }

  @Override
  public void init() {
    subscribeTo(JavaGrammar.LABELED_STATEMENT, JavaGrammar.BREAK_STATEMENT, JavaGrammar.CONTINUE_STATEMENT);
  }

  @Override
  public void visitNode(AstNode astNode) {
    if (astNode.is(JavaGrammar.LABELED_STATEMENT)) {
      visitLabeledStatement(astNode);
    } else if (astNode.is(JavaGrammar.BREAK_STATEMENT, JavaGrammar.CONTINUE_STATEMENT)) {
      visitBreakOrContinueStatement(astNode);
    } else {
      throw new IllegalArgumentException("Unexpected AstNodeType: " + astNode.getType());
    }
  }

  private void visitLabeledStatement(AstNode astNode) {
    AstNode identifierNode = astNode.getFirstChild(JavaTokenType.IDENTIFIER);
    // JLS7 6.2: in fact labelled statement is not a symbol
    semanticModel.associateSymbol(identifierNode, new Symbol(0, 0, identifierNode.getTokenValue(), null));
  }

  private void visitBreakOrContinueStatement(AstNode astNode) {
    // idea: associate break and continue with jump target like in IntelliJ IDEA
    AstNode identifier = astNode.getFirstChild(JavaTokenType.IDENTIFIER);
    if (identifier != null) {
      String label = identifier.getTokenValue();
      AstNode labelledStatement = astNode.getFirstAncestor(JavaGrammar.LABELED_STATEMENT);
      while (labelledStatement != null && !label.equals(labelledStatement.getFirstChild(JavaTokenType.IDENTIFIER).getTokenValue())) {
        labelledStatement = labelledStatement.getFirstAncestor(JavaGrammar.LABELED_STATEMENT);
      }
      if (labelledStatement != null) {
        semanticModel.associateReference(identifier, semanticModel.getSymbol(labelledStatement.getFirstChild(JavaTokenType.IDENTIFIER)));
      }
    }
  }

}
