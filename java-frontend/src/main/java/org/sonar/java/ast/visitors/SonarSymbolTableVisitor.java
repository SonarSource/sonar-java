/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.java.ast.visitors;

import java.util.List;
import org.sonar.api.batch.sensor.symbol.NewSymbol;
import org.sonar.api.batch.sensor.symbol.NewSymbolTable;
import org.sonar.java.model.JUtils;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.LabeledStatementTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.location.Range;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.VariableTree;

public class SonarSymbolTableVisitor extends BaseTreeVisitor {

  private final NewSymbolTable newSymbolTable;
  private CompilationUnitTree outerClass;

  public SonarSymbolTableVisitor(NewSymbolTable newSymbolTable) {
    this.newSymbolTable = newSymbolTable;
  }

  @Override
  public void visitCompilationUnit(CompilationUnitTree tree) {
    if (outerClass == null) {
      outerClass = tree;
    }
    super.visitCompilationUnit(tree);

    if (tree.equals(outerClass)) {
      newSymbolTable.save();
    }
  }

  @Override
  public void visitClass(ClassTree tree) {
    IdentifierTree simpleName = tree.simpleName();
    if (simpleName != null) {
      createSymbol(simpleName, tree.symbol().usages());
    }
    for (TypeParameterTree typeParameterTree : tree.typeParameters()) {
      createSymbol(typeParameterTree.identifier(), JUtils.typeParameterTreeSymbol(typeParameterTree).usages());
    }
    super.visitClass(tree);
  }

  @Override
  public void visitVariable(VariableTree tree) {
    createSymbol(tree.simpleName(), tree.symbol().usages());
    super.visitVariable(tree);
  }

  @Override
  public void visitEnumConstant(EnumConstantTree tree) {
    createSymbol(tree.simpleName(), ((VariableTreeImpl) tree).symbol().usages());
    super.visitEnumConstant(tree);
  }

  @Override
  public void visitMethod(MethodTree tree) {
    List<IdentifierTree> usages = tree.symbol().usages();
    createSymbol(tree.simpleName(), usages);
    for (TypeParameterTree typeParameterTree : tree.typeParameters()) {
      createSymbol(typeParameterTree.identifier(), JUtils.typeParameterTreeSymbol(typeParameterTree).usages());
    }
    super.visitMethod(tree);
  }

  @Override
  public void visitLabeledStatement(LabeledStatementTree tree) {
    createSymbol(tree.label(), tree.symbol().usages());
    super.visitLabeledStatement(tree);
  }

  @Override
  public void visitImport(ImportTree tree) {
    IdentifierTree identifierTree;
    if (tree.qualifiedIdentifier().is(Tree.Kind.IDENTIFIER)) {
      identifierTree = (IdentifierTree) tree.qualifiedIdentifier();
    } else {
      identifierTree = ((MemberSelectExpressionTree) tree.qualifiedIdentifier()).identifier();
    }
    // Exclude on demands imports
    if (!"*".equals(identifierTree.name())) {
      Symbol symbol = JUtils.importTreeSymbol(tree);
      if (symbol != null) {
        createSymbol(identifierTree, symbol.usages());
      }
    }
    super.visitImport(tree);
  }

  private void createSymbol(IdentifierTree declaration, List<IdentifierTree> usages) {
    Range range = declaration.identifierToken().range();
    NewSymbol newSymbol = newSymbolTable.newSymbol(
      range.start().line(), range.start().columnOffset(),
      range.end().line(), range.end().columnOffset());
    for (IdentifierTree usage : usages) {
      range = usage.identifierToken().range();
      newSymbol.newReference(
        range.start().line(), range.start().columnOffset(),
        range.end().line(), range.end().columnOffset());
    }
  }

}
