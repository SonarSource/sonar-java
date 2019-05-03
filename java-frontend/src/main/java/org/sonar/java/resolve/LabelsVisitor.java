/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.resolve;

import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.statement.LabeledStatementTreeImpl;
import org.sonar.java.resolve.JavaSymbol.JavaLabelSymbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BreakStatementTree;
import org.sonar.plugins.java.api.tree.ContinueStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LabeledStatementTree;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class LabelsVisitor extends BaseTreeVisitor {

  private final Map<String, LabeledStatementTree> labelTrees;
  //FIXME (benzonico) The dependency of this class upon SemanticModel should be removed. This holds as long as Result relies on SemanticModel.
  //As a result of this removal, this visitor should always be executed, regardless semantic analysis is activated or not.
  private final SemanticModel semanticModel;


  public LabelsVisitor(SemanticModel semanticModel) {
    this.semanticModel = semanticModel;
    this.labelTrees = new HashMap<>();
  }

  @Override
  public void visitLabeledStatement(LabeledStatementTree tree) {
    JavaLabelSymbol symbol = new JavaLabelSymbol(tree);
    ((LabeledStatementTreeImpl) tree).setSymbol(symbol);
    semanticModel.associateSymbol(tree, symbol);
    labelTrees.put(tree.label().name(), tree);
    super.visitLabeledStatement(tree);
  }

  @Override
  public void visitBreakStatement(BreakStatementTree tree) {
    associateLabel(tree.label());
    super.visitBreakStatement(tree);
  }

  @Override
  public void visitContinueStatement(ContinueStatementTree tree) {
    associateLabel(tree.label());
    super.visitContinueStatement(tree);
  }

  private void associateLabel(@Nullable IdentifierTree label) {
    if (label == null) {
      return;
    }
    LabeledStatementTree labelTree = labelTrees.get(label.name());
    if (labelTree != null) {
      JavaSymbol symbol = (JavaSymbol) labelTree.symbol();
      ((IdentifierTreeImpl) label).setSymbol(symbol);
      symbol.addUsage(label);
    }
  }
}
