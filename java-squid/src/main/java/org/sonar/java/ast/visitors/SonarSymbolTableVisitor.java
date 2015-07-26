/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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

import com.google.common.collect.Lists;
import org.sonar.api.source.Symbol;
import org.sonar.api.source.Symbolizable;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.resolve.Symbols;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.LabeledStatementTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.List;

public class SonarSymbolTableVisitor extends BaseTreeVisitor {

  private final SemanticModel semanticModel;
  private final Symbolizable symbolizable;
  private final Symbolizable.SymbolTableBuilder symbolTableBuilder;
  private CompilationUnitTree outerClass;

  public SonarSymbolTableVisitor(Symbolizable symbolizable, SemanticModel semanticModel) {
    this.symbolizable = symbolizable;
    this.semanticModel = semanticModel;
    this.symbolTableBuilder = symbolizable.newSymbolTableBuilder();
  }

  @Override
  public void visitCompilationUnit(CompilationUnitTree tree) {
    if (outerClass == null) {
      outerClass = tree;
    }
    super.visitCompilationUnit(tree);

    if (tree.equals(outerClass)) {
      symbolizable.setSymbolTable(symbolTableBuilder.build());
    }
  }

  @Override
  public void visitClass(ClassTree tree) {
    IdentifierTree simpleName = tree.simpleName();
    if (simpleName != null) {
      createSymbol(simpleName, tree.symbol().usages());
    }
    for (TypeParameterTree typeParameterTree : tree.typeParameters()) {
      createSymbol(typeParameterTree.identifier(), typeParameterTree);
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
    createSymbol(tree.simpleName(), tree);
    super.visitEnumConstant(tree);
  }

  @Override
  public void visitMethod(MethodTree tree) {
    //as long as SONAR-5894 is not fixed, do not provide references to enum constructors
    if(tree.symbol().returnType() == null && tree.symbol().owner().isEnum()) {
      createSymbol(tree.simpleName(), Lists.<IdentifierTree>newArrayList());
    } else {
      createSymbol(tree.simpleName(), tree.symbol().usages());
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
      createSymbol(identifierTree, tree);
    }
    super.visitImport(tree);
  }

  private void createSymbol(IdentifierTree declaration, Tree tree) {
    org.sonar.plugins.java.api.semantic.Symbol semanticSymbol = semanticModel.getSymbol(tree);
    if (semanticSymbol == null) {
      semanticSymbol = Symbols.unknownSymbol;
    }
    createSymbol(declaration, semanticSymbol.usages());
  }

  private void createSymbol(IdentifierTree declaration, List<IdentifierTree> usages) {
    Symbol symbol = symbolTableBuilder.newSymbol(startOffsetFor(declaration), endOffsetFor(declaration));
    for (IdentifierTree usage : usages) {
      symbolTableBuilder.newReference(symbol, startOffsetFor(usage));
    }
  }

  private static int startOffsetFor(IdentifierTree tree) {
    return ((InternalSyntaxToken) tree.identifierToken()).fromIndex();
  }

  private static int endOffsetFor(IdentifierTree tree) {
    return ((InternalSyntaxToken) tree.identifierToken()).fromIndex() + tree.identifierToken().text().length();
  }

}
