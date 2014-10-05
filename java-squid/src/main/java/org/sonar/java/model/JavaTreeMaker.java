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
package org.sonar.java.model;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.parser.JavaLexer;
import org.sonar.java.ast.parser.TreeFactory;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.MemberSelectExpressionTreeImpl;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.List;

public class JavaTreeMaker {

  // TODO To be replaced by members such as "STATEMENTS"
  public static Kind[] getKindsAssociatedTo(Class<? extends Tree> associatedInterface) {
    List<Kind> result = Lists.newArrayList();
    for (Kind kind : Kind.values()) {
      if (associatedInterface.equals(kind.getAssociatedInterface())) {
        result.add(kind);
      }
    }
    return result.toArray(new Kind[result.size()]);
  }

  public static final Kind[] TYPE_KINDS = ImmutableList.<Kind>builder()
    .add(getKindsAssociatedTo(PrimitiveTypeTree.class))
    .add(Kind.IDENTIFIER, Kind.MEMBER_SELECT, Kind.PARAMETERIZED_TYPE)
    .add(Kind.ARRAY_TYPE)
    .build()
    .toArray(new Kind[0]);
  public static final Kind[] QUALIFIED_EXPRESSION_KINDS = new Kind[] {Kind.IDENTIFIER, Kind.MEMBER_SELECT};

  public static final AstNodeType[] STATEMENTS_KINDS = new ImmutableList.Builder<AstNodeType>()
    .add(
      Kind.EMPTY_STATEMENT,
      Kind.LABELED_STATEMENT,
      Kind.IF_STATEMENT,
      Kind.ASSERT_STATEMENT,
      Kind.SWITCH_STATEMENT,
      Kind.WHILE_STATEMENT,
      Kind.DO_STATEMENT,
      Kind.BREAK_STATEMENT,
      Kind.CONTINUE_STATEMENT,
      Kind.RETURN_STATEMENT,
      Kind.THROW_STATEMENT,
      Kind.SYNCHRONIZED_STATEMENT,
      Kind.EXPRESSION_STATEMENT,
      Kind.FOR_STATEMENT,
      Kind.FOR_EACH_STATEMENT,
      Kind.TRY_STATEMENT)
    .build()
    .toArray(new AstNodeType[0]);

  private final KindMaps kindMaps = new KindMaps();

  public static void checkType(AstNode astNode, AstNodeType... expected) {
    Preconditions.checkArgument(astNode.is(expected), "Unexpected AstNodeType: %s", astNode.getType().toString());
  }

  public IdentifierTree identifier(AstNode astNode) {
    checkType(astNode, JavaTokenType.IDENTIFIER, JavaKeyword.THIS, JavaKeyword.CLASS, JavaKeyword.SUPER);
    return new IdentifierTreeImpl(InternalSyntaxToken.createLegacy(astNode), astNode);
  }

  /*
   * 4. Types, Values and Variables
   */

  public PrimitiveTypeTree basicType(AstNode astNode) {
    checkType(astNode, JavaKeyword.VOID);
    return new JavaTree.PrimitiveTypeTreeImpl(astNode);
  }

  public ExpressionTree referenceType(AstNode astNode) {
    if (astNode instanceof ExpressionTree && ((JavaTree) astNode).isLegacy()) {
      return (ExpressionTree) astNode;
    }

    return referenceType(astNode, 0);
  }

  ExpressionTree referenceType(AstNode astNode, int dimSize) {
    ExpressionTree result = astNode.getFirstChild().is(Kind.PRIMITIVE_TYPE) ? (PrimitiveTypeTree) astNode.getFirstChild() : (ExpressionTree) astNode.getFirstChild();
    return applyDim(result, dimSize + astNode.getChildren(TreeFactory.WRAPPER_AST_NODE).size());
  }

  /*
   * 7.3. Compilation Units
   */

  public CompilationUnitTree compilationUnit(AstNode astNode) {
    checkType(astNode, JavaLexer.COMPILATION_UNIT);
    ImmutableList.Builder<ImportTree> imports = ImmutableList.builder();
    for (AstNode importNode : astNode.getChildren(JavaLexer.IMPORT_DECLARATION)) {
      ExpressionTree qualifiedIdentifier = (ExpressionTree) importNode.getFirstChild(QUALIFIED_EXPRESSION_KINDS);
      AstNode astNodeQualifiedIdentifier = (AstNode) qualifiedIdentifier;
      // star import : if there is a star then add it as an identifier.
      AstNode nextNextSibling = astNodeQualifiedIdentifier.getNextSibling().getNextSibling();
      if (astNodeQualifiedIdentifier.getNextSibling().is(JavaPunctuator.DOT) && nextNextSibling.is(JavaPunctuator.STAR)) {
        qualifiedIdentifier = new MemberSelectExpressionTreeImpl(
          astNodeQualifiedIdentifier.getNextSibling().getNextSibling(),
          qualifiedIdentifier,
          new IdentifierTreeImpl(InternalSyntaxToken.createLegacy(nextNextSibling), nextNextSibling));
      }

      imports.add(new JavaTree.ImportTreeImpl(
        importNode,
        importNode.hasDirectChildren(JavaKeyword.STATIC),
        qualifiedIdentifier));
    }
    ImmutableList.Builder<Tree> types = ImmutableList.builder();
    for (AstNode typeNode : astNode.getChildren(Kind.CLASS,
      Kind.ENUM,
      Kind.INTERFACE,
      Kind.ANNOTATION_TYPE)) {
      types.add((Tree) typeNode);
    }

    ExpressionTree packageDeclaration = null;
    ImmutableList.Builder<AnnotationTree> packageAnnotations = ImmutableList.builder();
    if (astNode.hasDirectChildren(JavaLexer.PACKAGE_DECLARATION)) {
      AstNode packageDeclarationNode = astNode.getFirstChild(JavaLexer.PACKAGE_DECLARATION);
      packageDeclaration = (ExpressionTree) packageDeclarationNode.getFirstChild(QUALIFIED_EXPRESSION_KINDS);
      for (AstNode annotationNode : packageDeclarationNode.getChildren(Kind.ANNOTATION)) {
        packageAnnotations.add((AnnotationTree) annotationNode);
      }
    }
    return new JavaTree.CompilationUnitTreeImpl(
      astNode,
      packageDeclaration,
      imports.build(),
      types.build(),
      packageAnnotations.build());
  }

  public ExpressionTree applyDim(ExpressionTree expression, int count) {
    ExpressionTree result = expression;
    for (int i = 0; i < count; i++) {
      result = new JavaTree.ArrayTypeTreeImpl(/* FIXME should not be null */null, result);
    }
    return result;
  }

}
