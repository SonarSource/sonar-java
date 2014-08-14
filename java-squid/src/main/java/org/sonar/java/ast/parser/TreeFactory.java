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
package org.sonar.java.ast.parser;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.sonar.sslr.api.AstNode;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.JavaTreeMaker;
import org.sonar.java.model.KindMaps;
import org.sonar.java.model.declaration.ModifiersTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.LambdaExpressionTreeImpl;
import org.sonar.java.model.expression.LiteralTreeImpl;
import org.sonar.java.model.expression.MemberSelectExpressionTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.model.expression.ParenthesizedTreeImpl;
import org.sonar.java.model.statement.AssertStatementTreeImpl;
import org.sonar.java.model.statement.BlockTreeImpl;
import org.sonar.java.model.statement.BreakStatementTreeImpl;
import org.sonar.java.model.statement.CaseGroupTreeImpl;
import org.sonar.java.model.statement.CaseLabelTreeImpl;
import org.sonar.java.model.statement.ContinueStatementTreeImpl;
import org.sonar.java.model.statement.DoWhileStatementTreeImpl;
import org.sonar.java.model.statement.EmptyStatementTreeImpl;
import org.sonar.java.model.statement.ExpressionStatementTreeImpl;
import org.sonar.java.model.statement.IfStatementTreeImpl;
import org.sonar.java.model.statement.LabeledStatementTreeImpl;
import org.sonar.java.model.statement.ReturnStatementTreeImpl;
import org.sonar.java.model.statement.SwitchStatementTreeImpl;
import org.sonar.java.model.statement.SynchronizedStatementTreeImpl;
import org.sonar.java.model.statement.ThrowStatementTreeImpl;
import org.sonar.java.model.statement.WhileStatementTreeImpl;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Collections;
import java.util.List;

public class TreeFactory {

  private final KindMaps kindMaps = new KindMaps();

  private final JavaTreeMaker treeMaker = new JavaTreeMaker();

  public ModifiersTreeImpl modifiers(Optional<List<AstNode>> modifierNodes) {
    if (!modifierNodes.isPresent()) {
      return ModifiersTreeImpl.EMPTY_MODIFIERS;
    }

    ImmutableList.Builder<Modifier> modifiers = ImmutableList.builder();
    ImmutableList.Builder<AnnotationTree> annotations = ImmutableList.builder();
    for (AstNode astNode : modifierNodes.get()) {
      Preconditions.checkArgument(astNode.is(JavaGrammar.MODIFIER), "Unexpected AstNodeType: %s", astNode.getType().toString());
      astNode = astNode.getFirstChild();
      if (astNode.is(JavaGrammar.ANNOTATION)) {
        annotations.add(treeMaker.annotation(astNode));
      } else {
        JavaKeyword keyword = (JavaKeyword) astNode.getType();
        modifiers.add(kindMaps.getModifier(keyword));
      }
    }

    return new ModifiersTreeImpl(modifierNodes.get(), modifiers.build(), annotations.build());
  }

  // Literals

  public ExpressionTree literal(InternalSyntaxToken token) {
    return new LiteralTreeImpl(kindMaps.getLiteral(token.type()),
      token);
  }

  public InternalSyntaxToken literalToken(AstNode astNode) {
    return new InternalSyntaxToken(astNode.getToken(), astNode.getType());
  }

  // Statements

  public BlockTreeImpl block(AstNode leftCurlyBraceToken, AstNode statements, AstNode rightCurlyBraceToken) {
    return new BlockTreeImpl(Tree.Kind.BLOCK, treeMaker.blockStatements(statements), leftCurlyBraceToken, statements, rightCurlyBraceToken);
  }

  public AssertStatementTreeImpl completeAssertStatement(AstNode assertToken, AstNode expression, Optional<AssertStatementTreeImpl> detailExpression, AstNode semicolonToken) {
    return detailExpression.isPresent() ?
      detailExpression.get().complete(treeMaker.expression(expression),
        assertToken, expression, semicolonToken) :
      new AssertStatementTreeImpl(treeMaker.expression(expression),
        assertToken, expression, semicolonToken);
  }

  public AssertStatementTreeImpl newAssertStatement(AstNode colonToken, AstNode expression) {
    return new AssertStatementTreeImpl(treeMaker.expression(expression),
      colonToken, expression);
  }

  public IfStatementTreeImpl completeIf(AstNode ifToken, AstNode condition, AstNode statement, Optional<IfStatementTreeImpl> elseClause) {
    if (elseClause.isPresent()) {
      return elseClause.get().complete(treeMaker.expression(condition), treeMaker.statement(statement), ifToken, condition, statement);
    } else {
      return new IfStatementTreeImpl(treeMaker.expression(condition), treeMaker.statement(statement), ifToken, condition, statement);
    }
  }

  public IfStatementTreeImpl newIfWithElse(AstNode elseToken, AstNode elseStatement) {
    return new IfStatementTreeImpl(treeMaker.statement(elseStatement), elseToken, elseStatement);
  }

  public WhileStatementTreeImpl whileStatement(AstNode whileToken, AstNode expression, AstNode statement) {
    return new WhileStatementTreeImpl(treeMaker.expression(expression), treeMaker.statement(statement),
      whileToken, expression, statement);
  }

  public DoWhileStatementTreeImpl doWhileStatement(AstNode doToken, AstNode statement, AstNode whileToken, AstNode expression, AstNode semicolonToken) {
    return new DoWhileStatementTreeImpl(treeMaker.statement(statement), treeMaker.expression(expression),
      doToken, statement, whileToken, expression, semicolonToken);
  }

  public SwitchStatementTreeImpl switchStatement(
    AstNode switchToken, AstNode expression, AstNode leftCurlyBraceToken, Optional<List<CaseGroupTreeImpl>> optionalGroups, AstNode rightCurlyBraceToken) {

    List<CaseGroupTreeImpl> groups = optionalGroups.isPresent() ? optionalGroups.get() : Collections.<CaseGroupTreeImpl>emptyList();

    ImmutableList.Builder<AstNode> children = ImmutableList.builder();
    children.add(switchToken, expression, leftCurlyBraceToken);
    children.addAll(groups);
    children.add(rightCurlyBraceToken);

    return new SwitchStatementTreeImpl(treeMaker.expression(expression), groups,
      children.build());
  }

  public CaseGroupTreeImpl switchGroup(List<CaseLabelTreeImpl> labels, Optional<List<AstNode>> optionalBlockStatements) {
    List<AstNode> blockStatements = optionalBlockStatements.isPresent() ? optionalBlockStatements.get() : Collections.<AstNode>emptyList();

    ImmutableList.Builder<StatementTree> builder = ImmutableList.builder();
    for (AstNode blockStatement : blockStatements) {
      builder.addAll(treeMaker.blockStatement(blockStatement));
    }

    return new CaseGroupTreeImpl(labels, builder.build(), blockStatements);
  }

  public CaseLabelTreeImpl newCaseSwitchLabel(AstNode caseToken, AstNode expression, AstNode colonToken) {
    return new CaseLabelTreeImpl(treeMaker.expression(expression),
      caseToken, expression, colonToken);
  }

  public CaseLabelTreeImpl newDefaultSwitchLabel(AstNode defaultToken, AstNode colonToken) {
    return new CaseLabelTreeImpl(null,
      defaultToken, colonToken);
  }

  public SynchronizedStatementTreeImpl synchronizedStatement(AstNode synchronizedToken, AstNode expression, BlockTreeImpl block) {
    return new SynchronizedStatementTreeImpl(treeMaker.expression(expression), block,
      synchronizedToken, expression, block);
  }

  public BreakStatementTreeImpl breakStatement(AstNode breakToken, Optional<AstNode> identifier, AstNode semicolonToken) {
    return identifier.isPresent() ?
      new BreakStatementTreeImpl(treeMaker.identifier(identifier.get()),
        breakToken, identifier.get(), semicolonToken) :
      new BreakStatementTreeImpl(null,
        breakToken, semicolonToken);
  }

  public ContinueStatementTreeImpl continueStatement(AstNode continueToken, Optional<AstNode> identifier, AstNode semicolonToken) {
    return identifier.isPresent() ?
      new ContinueStatementTreeImpl(treeMaker.identifier(identifier.get()),
        continueToken, identifier.get(), semicolonToken) :
      new ContinueStatementTreeImpl(null,
        continueToken, semicolonToken);
  }

  public ReturnStatementTreeImpl returnStatement(AstNode returnToken, Optional<AstNode> expression, AstNode semicolonToken) {
    return expression.isPresent() ?
      new ReturnStatementTreeImpl(treeMaker.expression(expression.get()),
        returnToken, expression.get(), semicolonToken) :
      new ReturnStatementTreeImpl(null,
        returnToken, semicolonToken);
  }

  public ThrowStatementTreeImpl throwStatement(AstNode throwToken, AstNode expression, AstNode semicolonToken) {
    return new ThrowStatementTreeImpl(treeMaker.expression(expression),
      throwToken, expression, semicolonToken);
  }

  public LabeledStatementTreeImpl labeledStatement(AstNode identifier, AstNode colon, AstNode statement) {
    return new LabeledStatementTreeImpl(treeMaker.identifier(identifier), treeMaker.statement(statement),
      identifier, colon, statement);
  }

  public ExpressionStatementTreeImpl expressionStatement(AstNode expression, AstNode semicolonToken) {
    return new ExpressionStatementTreeImpl(treeMaker.expression(expression),
      expression, semicolonToken);
  }

  public EmptyStatementTreeImpl emptyStatement(AstNode semicolon) {
    return new EmptyStatementTreeImpl(semicolon);
  }

  // End of statements

  // Expressions

  public ExpressionTree lambdaExpression(AstNode parameters, AstNode arrowToken, AstNode body) {
    Tree bodyTree;
    if (body.hasDirectChildren(JavaGrammar.BLOCK)) {
      bodyTree = (BlockTree) body.getFirstChild(JavaGrammar.BLOCK);
    } else {
      bodyTree = treeMaker.expression(body.getFirstChild());
    }
    List<VariableTree> params = Lists.newArrayList();
    // FIXME(Godin): params always empty

    return new LambdaExpressionTreeImpl(params, bodyTree,
      parameters, arrowToken, body);
  }

  public ExpressionTree parenthesizedExpression(AstNode leftParenthesisToken, AstNode expression, AstNode rightParenthesisToken) {
    return new ParenthesizedTreeImpl(treeMaker.expression(expression),
      leftParenthesisToken, expression, rightParenthesisToken);
  }

  public ExpressionTree completeExplicityGenericInvocation(AstNode nonWildcardTypeArguments, ExpressionTree partial) {
    // TODO do not lose nonWildcardTypeArguments
    return partial;
  }

  public ExpressionTree newExplicitGenericInvokation(AstNode explicitGenericInvocationSuffix) {
    if (explicitGenericInvocationSuffix.hasDirectChildren(JavaKeyword.SUPER)) {
      // <T>super...
      return applySuperSuffix(explicitGenericInvocationSuffix.getFirstChild(JavaKeyword.SUPER), explicitGenericInvocationSuffix.getFirstChild(JavaGrammar.SUPER_SUFFIX));
    } else {
      // <T>id(arguments)
      AstNode identifierNode = explicitGenericInvocationSuffix.getFirstChild(JavaTokenType.IDENTIFIER);

      // TODO Detect that no children are lost
      return new MethodInvocationTreeImpl(treeMaker.identifier(identifierNode), treeMaker.arguments(explicitGenericInvocationSuffix.getFirstChild(JavaGrammar.ARGUMENTS)),
        identifierNode, explicitGenericInvocationSuffix.getFirstChild(JavaGrammar.ARGUMENTS));
    }
  }

  public ExpressionTree newExplicitGenericInvokation(AstNode thisToken, AstNode arguments) {
    return new MethodInvocationTreeImpl(treeMaker.identifier(thisToken), treeMaker.arguments(arguments),
      thisToken, arguments);
  }

  public ExpressionTree thisExpression(AstNode thisToken, Optional<AstNode> arguments) {
    if (arguments.isPresent()) {
      // this(arguments)
      return new MethodInvocationTreeImpl(treeMaker.identifier(thisToken), treeMaker.arguments(arguments.get()),
        thisToken, arguments.get());
    } else {
      // this
      return new IdentifierTreeImpl(new InternalSyntaxToken(thisToken.getToken()), thisToken);
    }
  }

  public ExpressionTree superExpression(AstNode superToken, AstNode superSuffix) {
    return applySuperSuffix(superToken, superSuffix);
  }

  // End of expressions

  // Helpers

  private ExpressionTree applySuperSuffix(AstNode identifierNode, AstNode superSuffixNode) {
    Preconditions.checkArgument(!(identifierNode instanceof JavaTree));
    JavaTreeMaker.checkType(superSuffixNode, JavaGrammar.SUPER_SUFFIX);

    if (superSuffixNode.hasDirectChildren(JavaGrammar.ARGUMENTS)) {
      // super(arguments)
      // super.method(arguments)
      // super.<T>method(arguments)
      // TODO typeArguments
      if (superSuffixNode.hasDirectChildren(JavaTokenType.IDENTIFIER)) {
        AstNode superSuffixIdentifierNode = superSuffixNode.getFirstChild(JavaTokenType.IDENTIFIER);
        MemberSelectExpressionTreeImpl memberSelect = new MemberSelectExpressionTreeImpl(
          superSuffixNode,
          treeMaker.identifier(identifierNode),
          treeMaker.identifier(superSuffixIdentifierNode));

        AstNode superSuffixArgumentsNode = superSuffixNode.getFirstChild(JavaGrammar.ARGUMENTS);
        return new MethodInvocationTreeImpl(memberSelect, treeMaker.arguments(superSuffixArgumentsNode),
          identifierNode, superSuffixNode);
      } else {
        return new MethodInvocationTreeImpl(treeMaker.identifier(identifierNode), treeMaker.arguments(superSuffixNode.getFirstChild(JavaGrammar.ARGUMENTS)),
          identifierNode, superSuffixNode);
      }
    } else {
      // super.field
      return new MemberSelectExpressionTreeImpl(treeMaker.identifier(identifierNode), treeMaker.identifier(superSuffixNode.getFirstChild(JavaTokenType.IDENTIFIER)),
        identifierNode, superSuffixNode);
    }
  }

}
