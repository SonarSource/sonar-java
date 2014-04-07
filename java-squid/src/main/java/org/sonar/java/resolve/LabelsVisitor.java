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

import com.google.common.collect.Maps;
import com.sonar.sslr.api.AstNode;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BreakStatementTree;
import org.sonar.plugins.java.api.tree.ContinueStatementTree;
import org.sonar.plugins.java.api.tree.LabeledStatementTree;

import java.util.Map;

public class LabelsVisitor extends BaseTreeVisitor {

  private final SemanticModel semanticModel;
  private final Map<String, LabeledStatementTree> labelTrees;


  public LabelsVisitor(SemanticModel semanticModel) {
    this.semanticModel = semanticModel;
    this.labelTrees = Maps.newHashMap();
  }

  @Override
  public void visitLabeledStatement(LabeledStatementTree tree) {
    AstNode identifierNode = ((JavaTree.LabeledStatementTreeImpl) tree).getAstNode().getFirstChild(JavaTokenType.IDENTIFIER);
    Symbol symbol = new Symbol(0, 0, identifierNode.getTokenValue(), null);
    semanticModel.associateSymbol(identifierNode, symbol);
    semanticModel.associateSymbol(tree, symbol);
    labelTrees.put(tree.label(), tree);
    super.visitLabeledStatement(tree);
  }

  @Override
  public void visitBreakStatement(BreakStatementTree tree) {
    String label = tree.label();
    AstNode identifier = ((JavaTree.BreakStatementTreeImpl) tree).getAstNode().getFirstChild(JavaTokenType.IDENTIFIER);
    if (label != null) {
      LabeledStatementTree labelTree = labelTrees.get(label);
      if (labelTree != null) {
        semanticModel.associateReference(identifier, semanticModel.getSymbol(labelTree));
      }
    }
    super.visitBreakStatement(tree);
  }

  @Override
  public void visitContinueStatement(ContinueStatementTree tree) {
    String label = tree.label();
    AstNode identifier = ((JavaTree.ContinueStatementTreeImpl) tree).getAstNode().getFirstChild(JavaTokenType.IDENTIFIER);
    if (label != null) {
      LabeledStatementTree labelTree = labelTrees.get(label);
      if (labelTree != null) {
        semanticModel.associateReference(identifier, semanticModel.getSymbol(labelTree));
      }
    }
    super.visitContinueStatement(tree);
  }
}
