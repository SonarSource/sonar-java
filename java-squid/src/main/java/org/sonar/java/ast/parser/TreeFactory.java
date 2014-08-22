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
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.impl.ast.AstXmlPrinter;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.JavaTree.NotImplementedTreeImpl;
import org.sonar.java.model.JavaTree.PrimitiveTypeTreeImpl;
import org.sonar.java.model.JavaTreeMaker;
import org.sonar.java.model.KindMaps;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.ModifiersTreeImpl;
import org.sonar.java.model.expression.ArrayAccessExpressionTreeImpl;
import org.sonar.java.model.expression.BinaryExpressionTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.InstanceOfTreeImpl;
import org.sonar.java.model.expression.InternalPostfixUnaryExpression;
import org.sonar.java.model.expression.InternalPrefixUnaryExpression;
import org.sonar.java.model.expression.LambdaExpressionTreeImpl;
import org.sonar.java.model.expression.LiteralTreeImpl;
import org.sonar.java.model.expression.MemberSelectExpressionTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.model.expression.NewArrayTreeImpl;
import org.sonar.java.model.expression.NewClassTreeImpl;
import org.sonar.java.model.expression.ParenthesizedTreeImpl;
import org.sonar.java.model.expression.TypeCastExpressionTreeImpl;
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
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
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

    return new ModifiersTreeImpl(modifiers.build(), annotations.build(),
      modifierNodes.get());
  }

  // Literals

  public ExpressionTree literal(AstNode astNode) {
    InternalSyntaxToken token = InternalSyntaxToken.create(astNode);

    return new LiteralTreeImpl(kindMaps.getLiteral(astNode.getType()), token);
  }

  // Statements

  public BlockTreeImpl block(AstNode openBraceTokenAstNode, AstNode statements, AstNode closeBraceTokenAstNode) {
    InternalSyntaxToken openBraceToken = InternalSyntaxToken.create(openBraceTokenAstNode);
    InternalSyntaxToken closeBraceToken = InternalSyntaxToken.create(closeBraceTokenAstNode);

    return new BlockTreeImpl(openBraceToken, treeMaker.blockStatements(statements), closeBraceToken,
      openBraceToken, statements, closeBraceToken);
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

  public IfStatementTreeImpl completeIf(AstNode ifToken, ParenthesizedTreeImpl condition, AstNode statement, Optional<IfStatementTreeImpl> elseClause) {
    if (elseClause.isPresent()) {
      return elseClause.get().complete(condition, treeMaker.statement(statement),
        ifToken, condition, statement);
    } else {
      return new IfStatementTreeImpl(condition, treeMaker.statement(statement),
        ifToken, condition, statement);
    }
  }

  public IfStatementTreeImpl newIfWithElse(AstNode elseToken, AstNode elseStatement) {
    return new IfStatementTreeImpl(treeMaker.statement(elseStatement), elseToken, elseStatement);
  }

  public WhileStatementTreeImpl whileStatement(AstNode whileToken, ParenthesizedTreeImpl expression, AstNode statement) {
    return new WhileStatementTreeImpl(expression, treeMaker.statement(statement),
      whileToken, expression, statement);
  }

  public DoWhileStatementTreeImpl doWhileStatement(AstNode doToken, AstNode statement, AstNode whileToken, ParenthesizedTreeImpl expression, AstNode semicolonToken) {
    return new DoWhileStatementTreeImpl(treeMaker.statement(statement), expression,
      doToken, statement, whileToken, expression, semicolonToken);
  }

  public SwitchStatementTreeImpl switchStatement(
    AstNode switchToken, ParenthesizedTreeImpl expression, AstNode leftCurlyBraceToken, Optional<List<CaseGroupTreeImpl>> optionalGroups, AstNode rightCurlyBraceToken) {

    List<CaseGroupTreeImpl> groups = optionalGroups.isPresent() ? optionalGroups.get() : Collections.<CaseGroupTreeImpl>emptyList();

    ImmutableList.Builder<AstNode> children = ImmutableList.builder();
    children.add(switchToken, expression, leftCurlyBraceToken);
    children.addAll(groups);
    children.add(rightCurlyBraceToken);

    return new SwitchStatementTreeImpl(expression, groups,
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

  public SynchronizedStatementTreeImpl synchronizedStatement(AstNode synchronizedToken, ParenthesizedTreeImpl expression, BlockTreeImpl block) {
    return new SynchronizedStatementTreeImpl(expression, block,
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

  public ExpressionTree completeInstanceofExpression(ExpressionTree expression, Optional<InstanceOfTreeImpl> partial) {
    return partial.isPresent() ?
      partial.get().complete(expression) :
      expression;
  }

  public InstanceOfTreeImpl newInstanceofExpression(AstNode instanceofTokenAstNode, AstNode type) {
    InternalSyntaxToken instanceofToken = InternalSyntaxToken.create(instanceofTokenAstNode);
    return new InstanceOfTreeImpl(instanceofToken, treeMaker.referenceType(type),
      type);
  }

  private static class OperatorAndOperand extends AstNode {

    private final InternalSyntaxToken operator;
    private final ExpressionTree operand;

    public OperatorAndOperand(InternalSyntaxToken operator, ExpressionTree operand) {
      super(null, null, null);

      this.operator = operator;
      this.operand = operand;

      addChild(operator);
      addChild((AstNode) operand);
    }

    public InternalSyntaxToken operator() {
      return operator;
    }

    public ExpressionTree operand() {
      return operand;
    }

  }

  private ExpressionTree binaryExpression(ExpressionTree expression, Optional<List<OperatorAndOperand>> operatorAndOperands) {
    if (!operatorAndOperands.isPresent()) {
      return expression;
    }

    // TODO SONARJAVA-610
    ExpressionTree result = null;
    InternalSyntaxToken lastOperator = null;
    for (OperatorAndOperand operatorAndOperand : Lists.reverse(operatorAndOperands.get())) {
      if (lastOperator == null) {
        result = operatorAndOperand.operand();
      } else {
        result = new BinaryExpressionTreeImpl(
          kindMaps.getBinaryOperator((JavaPunctuator) lastOperator.getType()),
          operatorAndOperand.operand(),
          lastOperator,
          result);
      }

      lastOperator = operatorAndOperand.operator();
    }

    result = new BinaryExpressionTreeImpl(
      kindMaps.getBinaryOperator((JavaPunctuator) lastOperator.getType()),
      expression,
      lastOperator,
      result);

    return result;
  }

  private OperatorAndOperand newOperatorAndOperand(AstNode operator, ExpressionTree operand) {
    return new OperatorAndOperand(InternalSyntaxToken.create(operator), operand);
  }

  // TODO Use same method several times

  public ExpressionTree binaryExpression4(ExpressionTree expression, Optional<List<OperatorAndOperand>> operatorAndOperands) {
    return binaryExpression(expression, operatorAndOperands);
  }

  public OperatorAndOperand newOperatorAndOperand4(AstNode operator, ExpressionTree operand) {
    return newOperatorAndOperand(operator, operand);
  }

  public ExpressionTree binaryExpression3(ExpressionTree expression, Optional<List<OperatorAndOperand>> operatorAndOperands) {
    return binaryExpression(expression, operatorAndOperands);
  }

  public OperatorAndOperand newOperatorAndOperand3(AstNode operator, ExpressionTree operand) {
    return newOperatorAndOperand(operator, operand);
  }

  public ExpressionTree binaryExpression2(ExpressionTree expression, Optional<List<OperatorAndOperand>> operatorAndOperands) {
    return binaryExpression(expression, operatorAndOperands);
  }

  public OperatorAndOperand newOperatorAndOperand2(AstNode operator, ExpressionTree operand) {
    return newOperatorAndOperand(operator, operand);
  }

  public ExpressionTree binaryExpression1(ExpressionTree expression, Optional<List<OperatorAndOperand>> operatorAndOperands) {
    return binaryExpression(expression, operatorAndOperands);
  }

  public OperatorAndOperand newOperatorAndOperand1(AstNode operator, ExpressionTree operand) {
    return newOperatorAndOperand(operator, operand);
  }

  public ExpressionTree newPrefixedExpression(AstNode operatorTokenAstNode, ExpressionTree expression) {
    InternalSyntaxToken operatorToken = InternalSyntaxToken.create(operatorTokenAstNode);
    return new InternalPrefixUnaryExpression(kindMaps.getPrefixOperator((JavaPunctuator) operatorTokenAstNode.getType()), operatorToken, expression);
  }

  public ExpressionTree newPostfixExpression(ExpressionTree primary, Optional<List<AstNode>> selectors, Optional<List<AstNode>> postfixOperatorAstNodes) {
    ExpressionTree result = primary;

    if (selectors.isPresent()) {
      for (AstNode selector : selectors.get()) {
        result = applySelector(result, selector);
      }
    }

    if (postfixOperatorAstNodes.isPresent()) {
      for (AstNode postfixOperatorAstNode : postfixOperatorAstNodes.get()) {
        InternalSyntaxToken postfixOperatorToken = InternalSyntaxToken.create(postfixOperatorAstNode);
        result = new InternalPostfixUnaryExpression(kindMaps.getPostfixOperator((JavaPunctuator) postfixOperatorAstNode.getType()), result, postfixOperatorToken);
      }
    }

    return result;
  }

  public ExpressionTree newTildaExpression(AstNode tildaTokenAstNode, ExpressionTree expression) {
    InternalSyntaxToken operatorToken = InternalSyntaxToken.create(tildaTokenAstNode);
    return new InternalPrefixUnaryExpression(Kind.BITWISE_COMPLEMENT, operatorToken, expression);
  }

  public ExpressionTree newBangExpression(AstNode bangTokenAstNode, ExpressionTree expression) {
    InternalSyntaxToken operatorToken = InternalSyntaxToken.create(bangTokenAstNode);
    return new InternalPrefixUnaryExpression(Kind.LOGICAL_COMPLEMENT, operatorToken, expression);
  }

  public ExpressionTree completeCastExpression(AstNode openParenTokenAstNode, TypeCastExpressionTreeImpl partial) {
    return partial.complete(InternalSyntaxToken.create(openParenTokenAstNode));
  }

  public TypeCastExpressionTreeImpl newBasicTypeCastExpression(PrimitiveTypeTreeImpl basicType, AstNode closeParenTokenAstNode, ExpressionTree expression) {
    InternalSyntaxToken closeParenToken = InternalSyntaxToken.create(closeParenTokenAstNode);

    List<AstNode> children = Lists.newArrayList();
    children.add(basicType);
    children.add(closeParenToken);
    children.add((AstNode) expression);

    return new TypeCastExpressionTreeImpl(basicType, expression, closeParenToken,
      children);
  }

  public TypeCastExpressionTreeImpl newClassCastExpression(AstNode type, Optional<List<AstNode>> classTypes, AstNode closeParenTokenAstNode, ExpressionTree expression) {
    InternalSyntaxToken closeParenToken = InternalSyntaxToken.create(closeParenTokenAstNode);

    List<AstNode> children = Lists.newArrayList();
    children.add(type);
    if (classTypes.isPresent()) {
      for (AstNode classType : classTypes.get()) {
        children.addAll(classType.getChildren());
      }
    }
    children.add(closeParenToken);
    children.add((AstNode) expression);

    return new TypeCastExpressionTreeImpl(treeMaker.referenceType(type), expression, closeParenToken,
      children);
  }

  public ExpressionTree completeMethodReference(NotImplementedTreeImpl partial, Optional<AstNode> typeArguments, AstNode newOrIdentifierToken) {
    // TODO SONARJAVA-613
    if (typeArguments.isPresent()) {
      partial.addChild(typeArguments.get());
    }
    partial.addChild(newOrIdentifierToken);
    return partial;
  }

  public NotImplementedTreeImpl newSuperMethodReference(AstNode superToken, AstNode doubleColonToken) {
    // TODO SONARJAVA-613
    return new NotImplementedTreeImpl(superToken, doubleColonToken);
  }

  public NotImplementedTreeImpl newTypeMethodReference(AstNode type, AstNode doubleColonToken) {
    // TODO SONARJAVA-613
    return new NotImplementedTreeImpl(type, doubleColonToken);
  }

  public NotImplementedTreeImpl newPrimaryMethodReference(ExpressionTree primary, Optional<List<AstNode>> selectors, AstNode doubleColonToken) {
    // TODO SONARJAVA-613
    List<AstNode> children = Lists.newArrayList();
    children.add((AstNode) primary);
    if (selectors.isPresent()) {
      children.addAll(selectors.get());
    }
    children.add(doubleColonToken);

    return new NotImplementedTreeImpl(children.toArray(new AstNode[children.size()]));
  }

  public ExpressionTree lambdaExpression(AstNode parameters, AstNode arrowToken, AstNode body) {
    Tree bodyTree;
    if (body.hasDirectChildren(Kind.BLOCK)) {
      bodyTree = (BlockTree) body.getFirstChild(Kind.BLOCK);
    } else {
      bodyTree = treeMaker.expression(body.getFirstChild());
    }
    List<VariableTree> params = Lists.newArrayList();
    // FIXME(Godin): params always empty

    return new LambdaExpressionTreeImpl(params, bodyTree,
      parameters, arrowToken, body);
  }

  public ParenthesizedTreeImpl parenthesizedExpression(AstNode leftParenthesisToken, AstNode expression, AstNode rightParenthesisToken) {
    return new ParenthesizedTreeImpl(treeMaker.expression(expression),
      leftParenthesisToken, expression, rightParenthesisToken);
  }

  public ExpressionTree completeExplicityGenericInvocation(AstNode nonWildcardTypeArguments, ExpressionTree partial) {
    ((JavaTree) partial).prependChildren(nonWildcardTypeArguments);
    return partial;
  }

  public ExpressionTree newExplicitGenericInvokation(AstNode explicitGenericInvocationSuffix) {
    if (explicitGenericInvocationSuffix.hasDirectChildren(JavaKeyword.SUPER)) {
      // <T>super...
      return applySuperSuffix(
        new IdentifierTreeImpl(InternalSyntaxToken.create(explicitGenericInvocationSuffix.getFirstChild(JavaKeyword.SUPER))),
        explicitGenericInvocationSuffix.getFirstChild(JavaGrammar.SUPER_SUFFIX));
    } else {
      // <T>id(arguments)
      AstNode identifierNode = explicitGenericInvocationSuffix.getFirstChild(JavaTokenType.IDENTIFIER);

      return new MethodInvocationTreeImpl(treeMaker.identifier(identifierNode), (ArgumentListTreeImpl) explicitGenericInvocationSuffix.getFirstChild(JavaGrammar.ARGUMENTS),
        identifierNode, explicitGenericInvocationSuffix.getFirstChild(JavaGrammar.ARGUMENTS));
    }
  }

  public ExpressionTree newExplicitGenericInvokation(AstNode thisToken, ArgumentListTreeImpl arguments) {
    return new MethodInvocationTreeImpl(treeMaker.identifier(thisToken), arguments,
      thisToken, arguments);
  }

  public ExpressionTree thisExpression(AstNode thisToken, Optional<ArgumentListTreeImpl> arguments) {
    if (arguments.isPresent()) {
      // this(arguments)
      return new MethodInvocationTreeImpl(treeMaker.identifier(thisToken), arguments.get(),
        thisToken, arguments.get());
    } else {
      // this
      return new IdentifierTreeImpl(InternalSyntaxToken.create(thisToken));
    }
  }

  public ExpressionTree superExpression(AstNode superToken, AstNode superSuffix) {
    return applySuperSuffix(new IdentifierTreeImpl(InternalSyntaxToken.create(superToken)), superSuffix);
  }

  public ExpressionTree newExpression(AstNode newToken, Optional<List<AstNode>> annotations, ExpressionTree partial) {
    if (annotations.isPresent()) {
      ((JavaTree) partial).prependChildren(annotations.get());
    }
    ((JavaTree) partial).prependChildren(newToken);
    return partial;
  }

  public ExpressionTree completeCreator(Optional<AstNode> nonWildcardTypeArguments, ExpressionTree partial) {
    if (nonWildcardTypeArguments.isPresent()) {
      ((JavaTree) partial).prependChildren(nonWildcardTypeArguments.get());
    }
    return partial;
  }

  public ExpressionTree newClassCreator(AstNode createdName, AstNode classCreatorRest) {
    ClassTreeImpl classBody = null;
    if (classCreatorRest.hasDirectChildren(JavaGrammar.CLASS_BODY)) {
      List<Tree> body = treeMaker.classBody(classCreatorRest.getFirstChild(JavaGrammar.CLASS_BODY));
      classBody = new ClassTreeImpl(classCreatorRest, Tree.Kind.CLASS, ModifiersTreeImpl.EMPTY, null, null, ImmutableList.<Tree>of(), body);
    }
    return new NewClassTreeImpl(null, treeMaker.classType(createdName), (ArgumentListTreeImpl) classCreatorRest.getFirstChild(JavaGrammar.ARGUMENTS), classBody,
      createdName, classCreatorRest);
  }

  public ExpressionTree newArrayCreator(AstNode type, NewArrayTreeImpl partial) {
    JavaTree typeTree = (JavaTree) (type.is(Kind.PRIMITIVE_TYPE) ? (PrimitiveTypeTreeImpl) type : treeMaker.classType(type));

    return partial.complete(typeTree,
      type);
  }

  public NewArrayTreeImpl completeArrayCreator(Optional<List<AstNode>> annotations, NewArrayTreeImpl partial) {
    if (annotations.isPresent()) {
      partial.prependChildren(annotations.get());
    }
    return partial;
  }

  public NewArrayTreeImpl newArrayCreatorWithInitializer(AstNode openBracketToken, AstNode closeBracketToken, Optional<List<AstNode>> dims, AstNode arrayInitializer) {
    ImmutableList.Builder<ExpressionTree> elems = ImmutableList.builder();
    for (AstNode elem : arrayInitializer.getChildren(JavaGrammar.VARIABLE_INITIALIZER)) {
      elems.add(treeMaker.variableInitializer(elem));
    }

    List<AstNode> children = Lists.newArrayList();
    children.add(openBracketToken);
    children.add(closeBracketToken);
    if (dims.isPresent()) {
      children.addAll(dims.get());
    }
    children.add(arrayInitializer);

    return new NewArrayTreeImpl(ImmutableList.<ExpressionTree>of(), elems.build(),
      children);
  }

  public NewArrayTreeImpl newArrayCreatorWithDimension(AstNode openBracketToken, AstNode expression, AstNode closeBracketToken,
    Optional<List<AstNode>> dimExpressions,
    Optional<List<AstNode>> dims) {

    ImmutableList.Builder<ExpressionTree> dimensions = ImmutableList.builder();
    dimensions.add(treeMaker.expression(expression));
    if (dimExpressions.isPresent()) {
      for (AstNode dimExpr : dimExpressions.get()) {
        dimensions.add(treeMaker.expression(dimExpr.getFirstChild(JavaGrammar.EXPRESSION)));
      }
    }

    List<AstNode> children = Lists.newArrayList();
    children.add(openBracketToken);
    children.add(expression);
    children.add(closeBracketToken);
    if (dimExpressions.isPresent()) {
      children.addAll(dimExpressions.get());
    }
    if (dims.isPresent()) {
      children.addAll(dims.get());
    }

    return new NewArrayTreeImpl(dimensions.build(), ImmutableList.<ExpressionTree>of(),
      children);
  }

  public ExpressionTree newQualifiedIdentifierExpression(ExpressionTree qualifiedIdentifier, Optional<AstNode> identifierSuffix) {
    if (!identifierSuffix.isPresent()) {
      // id
      return qualifiedIdentifier;
    } else {
      AstNode identifierSuffixNode = identifierSuffix.get();
      if (identifierSuffixNode.getFirstChild().is(JavaPunctuator.LBRK)) {
        if (identifierSuffixNode.hasDirectChildren(JavaKeyword.CLASS)) {
          // 15.8.2. Class Literals
          // id[].class
          return new MemberSelectExpressionTreeImpl(
            treeMaker.applyDim(qualifiedIdentifier, identifierSuffixNode.getChildren(JavaGrammar.DIM).size() + 1),
            treeMaker.identifier(identifierSuffixNode.getFirstChild(JavaKeyword.CLASS)),
            (AstNode) qualifiedIdentifier, identifierSuffixNode);
        } else {
          // id[expression]
          return new ArrayAccessExpressionTreeImpl(
            qualifiedIdentifier, treeMaker.expression(identifierSuffixNode.getFirstChild(JavaGrammar.EXPRESSION)),
            (AstNode) qualifiedIdentifier, identifierSuffixNode);
        }
      } else if (identifierSuffixNode.getFirstChild().is(JavaGrammar.ARGUMENTS)) {
        // id(arguments)
        return new MethodInvocationTreeImpl(
          qualifiedIdentifier, (ArgumentListTreeImpl) identifierSuffixNode.getFirstChild(),
          (AstNode) qualifiedIdentifier, identifierSuffixNode);
      } else if (identifierSuffixNode.getFirstChild().is(JavaPunctuator.DOT)) {
        if (identifierSuffixNode.hasDirectChildren(JavaKeyword.CLASS)) {
          // 15.8.2. Class Literals
          // id.class
          return new MemberSelectExpressionTreeImpl(
            qualifiedIdentifier, treeMaker.identifier(identifierSuffixNode.getFirstChild(JavaKeyword.CLASS)),
            (AstNode) qualifiedIdentifier, identifierSuffixNode);
        } else if (identifierSuffixNode.hasDirectChildren(JavaGrammar.EXPLICIT_GENERIC_INVOCATION)) {
          // id.<...>...
          return applyExplicitGenericInvocation(
            qualifiedIdentifier, identifierSuffixNode.getFirstChild(JavaPunctuator.DOT), identifierSuffixNode.getFirstChild(JavaGrammar.EXPLICIT_GENERIC_INVOCATION));
        } else if (identifierSuffixNode.hasDirectChildren(JavaKeyword.THIS)) {
          // id.this
          return new MemberSelectExpressionTreeImpl(
            qualifiedIdentifier, treeMaker.identifier(identifierSuffixNode.getFirstChild(JavaKeyword.THIS)),
            (AstNode) qualifiedIdentifier, identifierSuffixNode);
        } else if (identifierSuffixNode.hasDirectChildren(JavaKeyword.SUPER)) {
          // id.super(arguments)
          IdentifierTreeImpl superIdentifier = new IdentifierTreeImpl(InternalSyntaxToken.create(identifierSuffixNode.getFirstChild(JavaKeyword.SUPER)));

          MemberSelectExpressionTreeImpl memberSelect = new MemberSelectExpressionTreeImpl(
            qualifiedIdentifier, superIdentifier,
            (AstNode) qualifiedIdentifier, identifierSuffixNode.getFirstChild(JavaPunctuator.DOT), superIdentifier);

          return new MethodInvocationTreeImpl(
            memberSelect, (ArgumentListTreeImpl) identifierSuffixNode.getFirstChild(JavaGrammar.ARGUMENTS),
            memberSelect, identifierSuffixNode.getFirstChild(JavaGrammar.ARGUMENTS));
        } else if (identifierSuffixNode.hasDirectChildren(JavaKeyword.NEW)) {
          // id.new...
          AstNode innerCreatorNode = identifierSuffixNode.getFirstChild(JavaGrammar.INNER_CREATOR);

          AstNode classCreatorRestNode = innerCreatorNode.getFirstChild(JavaGrammar.CLASS_CREATOR_REST);

          ClassTree classBody = null;
          if (classCreatorRestNode.hasDirectChildren(JavaGrammar.CLASS_BODY)) {
            classBody = new ClassTreeImpl(
              classCreatorRestNode,
              Tree.Kind.CLASS,
              ModifiersTreeImpl.EMPTY,
              treeMaker.classBody(classCreatorRestNode.getFirstChild(JavaGrammar.CLASS_BODY)));
          }
          return new NewClassTreeImpl(
            qualifiedIdentifier,
            treeMaker.identifier(innerCreatorNode.getFirstChild(JavaTokenType.IDENTIFIER)),
            (ArgumentListTreeImpl) classCreatorRestNode.getFirstChild(JavaGrammar.ARGUMENTS),
            classBody,
            (AstNode) qualifiedIdentifier, identifierSuffixNode);
        } else {
          throw new IllegalArgumentException("Unexpected AstNodeType: " + identifierSuffixNode.getChild(1));
        }
      } else {
        throw new IllegalArgumentException("Unexpected AstNodeType: " + identifierSuffixNode.getFirstChild());
      }
    }
  }

  public ExpressionTree basicClassExpression(PrimitiveTypeTreeImpl basicType, Optional<List<AstNode>> dims, AstNode dotToken, AstNode classToken) {
    // 15.8.2. Class Literals
    // int.class
    // int[].class

    List<AstNode> children = Lists.newArrayList();
    children.add(basicType);
    if (dims.isPresent()) {
      children.addAll(dims.get());
    }
    children.add(dotToken);
    children.add(classToken);

    return new MemberSelectExpressionTreeImpl(
      treeMaker.applyDim(basicType, dims.isPresent() ? dims.get().size() : 0), treeMaker.identifier(classToken),
      children.toArray(new AstNode[children.size()]));
  }

  public ExpressionTree voidClassExpression(AstNode voidToken, AstNode dotToken, AstNode classToken) {
    // void.class
    return new MemberSelectExpressionTreeImpl(treeMaker.basicType(voidToken), treeMaker.identifier(classToken),
      voidToken, dotToken, classToken);
  }

  public PrimitiveTypeTreeImpl newBasicType(Optional<List<AstNode>> annotations, AstNode basicType) {
    InternalSyntaxToken token = InternalSyntaxToken.create(basicType);

    List<AstNode> children = Lists.newArrayList();
    if (annotations.isPresent()) {
      children.addAll(annotations.get());
    }
    children.add(token);

    return new JavaTree.PrimitiveTypeTreeImpl(token, children);
  }

  public ArgumentListTreeImpl completeArguments(AstNode openParenthesisTokenAstNode, Optional<ArgumentListTreeImpl> expressions, AstNode closeParenthesisTokenAstNode) {
    InternalSyntaxToken openParenthesisToken = InternalSyntaxToken.create(openParenthesisTokenAstNode);
    InternalSyntaxToken closeParenthesisToken = InternalSyntaxToken.create(closeParenthesisTokenAstNode);

    return expressions.isPresent() ?
      expressions.get().complete(openParenthesisToken, closeParenthesisToken) :
      new ArgumentListTreeImpl(openParenthesisToken, closeParenthesisToken);
  }

  public ArgumentListTreeImpl newArguments(AstNode expression, Optional<List<AstNode>> rests) {
    List<AstNode> children = Lists.newArrayList();
    ImmutableList.Builder<ExpressionTree> expressions = ImmutableList.builder();

    children.add(expression);
    expressions.add(treeMaker.expression(expression));

    if (rests.isPresent()) {
      for (AstNode rest : rests.get()) {
        children.addAll(rest.getChildren());
        expressions.add(treeMaker.expression(rest.getFirstChild(JavaGrammar.EXPRESSION)));
      }
    }

    return new ArgumentListTreeImpl(expressions.build(), children);
  }

  public ExpressionTree qualifiedIdentifier(Optional<List<AstNode>> annotations, AstNode firstIdentifier, Optional<List<AstNode>> rests) {
    List<AstNode> children = Lists.newArrayList();
    if (annotations.isPresent()) {
      children.addAll(annotations.get());
    }
    children.add(firstIdentifier);
    if (rests.isPresent()) {
      for (AstNode rest : rests.get()) {
        children.addAll(rest.getChildren());
      }
    }

    JavaTree result = null;

    List<AstNode> pendingChildren = Lists.newArrayList();
    for (AstNode child : children) {
      if (!child.is(JavaTokenType.IDENTIFIER)) {
        pendingChildren.add(child);
      } else {
        InternalSyntaxToken identifierToken = InternalSyntaxToken.create(child);

        if (result == null) {
          pendingChildren.add(identifierToken);
          result = new IdentifierTreeImpl(identifierToken, pendingChildren);
        } else {
          IdentifierTreeImpl identifier = new IdentifierTreeImpl(identifierToken);

          pendingChildren.add(0, result);
          pendingChildren.add(identifier);

          result = new MemberSelectExpressionTreeImpl((ExpressionTree) result, identifier,
            pendingChildren.toArray(new AstNode[pendingChildren.size()]));
        }

        pendingChildren.clear();
      }
    }

    return (ExpressionTree) result;
  }

  // End of expressions

  // Helpers

  private static final AstNodeType WRAPPER_AST_NODE = new AstNodeType() {
    @Override
    public String toString() {
      return "WRAPPER_AST_NODE";
    }
  };

  public AstNode newWrapperAstNode(Optional<List<AstNode>> e1, AstNode e2) {
    if (e1.isPresent()) {
      AstNode astNode = new AstNode(WRAPPER_AST_NODE, WRAPPER_AST_NODE.toString(), null);
      for (AstNode child : e1.get()) {
        astNode.addChild(child);
      }
      astNode.addChild(e2);
      return astNode;
    } else {
      return e2;
    }
  }

  public AstNode newWrapperAstNode(AstNode e1, Optional<List<AstNode>> e2, AstNode e3) {
    AstNode astNode = new AstNode(WRAPPER_AST_NODE, WRAPPER_AST_NODE.toString(), null);
    astNode.addChild(e1);
    if (e2.isPresent()) {
      for (AstNode child : e2.get()) {
        astNode.addChild(child);
      }
    }
    astNode.addChild(e3);
    return astNode;
  }

  public AstNode newWrapperAstNode(AstNode e1, AstNode e2) {
    AstNode astNode = new AstNode(WRAPPER_AST_NODE, WRAPPER_AST_NODE.toString(), null);
    astNode.addChild(e1);
    astNode.addChild(e2);
    return astNode;
  }

  // TODO Enable the same method call multiple times
  public AstNode newWrapperAstNode2(AstNode e1, AstNode e2) {
    return newWrapperAstNode(e1, e2);
  }

  // Crappy methods which must go away

  private ExpressionTree applySuperSuffix(ExpressionTree expression, AstNode superSuffixNode) {
    Preconditions.checkArgument(!((JavaTree) expression).isLegacy());
    JavaTreeMaker.checkType(superSuffixNode, JavaGrammar.SUPER_SUFFIX);

    if (superSuffixNode.hasDirectChildren(JavaGrammar.ARGUMENTS)) {
      // super(arguments)
      // super.method(arguments)
      // super.<T>method(arguments)
      // TODO typeArguments
      if (superSuffixNode.hasDirectChildren(JavaTokenType.IDENTIFIER)) {
        MemberSelectExpressionTreeImpl memberSelect = new MemberSelectExpressionTreeImpl(
          expression, treeMaker.identifier(superSuffixNode.getFirstChild(JavaTokenType.IDENTIFIER)),
          (AstNode) expression);

        return new MethodInvocationTreeImpl(memberSelect, (ArgumentListTreeImpl) superSuffixNode.getFirstChild(JavaGrammar.ARGUMENTS),
          memberSelect, superSuffixNode);
      } else {
        return new MethodInvocationTreeImpl(expression, (ArgumentListTreeImpl) superSuffixNode.getFirstChild(JavaGrammar.ARGUMENTS),
          (AstNode) expression, superSuffixNode);
      }
    } else {
      // super.field
      return new MemberSelectExpressionTreeImpl(expression, treeMaker.identifier(superSuffixNode.getFirstChild(JavaTokenType.IDENTIFIER)),
        (AstNode) expression, superSuffixNode);
    }
  }

  public ExpressionTree applyExplicitGenericInvocation(ExpressionTree expression, AstNode dotToken, AstNode astNode) {
    Preconditions.checkArgument(!((JavaTree) expression).isLegacy());
    JavaTreeMaker.checkType(astNode, JavaGrammar.EXPLICIT_GENERIC_INVOCATION);
    // TODO NON_WILDCARD_TYPE_ARGUMENTS
    AstNode nonWildcardTypeArguments = astNode.getFirstChild(JavaGrammar.NON_WILDCARD_TYPE_ARGUMENTS);

    AstNode explicitGenericInvocationSuffixNode = astNode.getFirstChild(JavaGrammar.EXPLICIT_GENERIC_INVOCATION_SUFFIX);
    if (explicitGenericInvocationSuffixNode.hasDirectChildren(JavaGrammar.SUPER_SUFFIX)) {
      IdentifierTreeImpl superIdentifier = new IdentifierTreeImpl(InternalSyntaxToken.create(explicitGenericInvocationSuffixNode.getFirstChild(JavaKeyword.SUPER)));
      expression = new MemberSelectExpressionTreeImpl(expression, superIdentifier,
        (AstNode) expression, dotToken, nonWildcardTypeArguments, superIdentifier);
      return applySuperSuffix(expression, explicitGenericInvocationSuffixNode.getFirstChild(JavaGrammar.SUPER_SUFFIX));
    } else {
      IdentifierTreeImpl identifier = new IdentifierTreeImpl(InternalSyntaxToken.create(explicitGenericInvocationSuffixNode.getFirstChild(JavaTokenType.IDENTIFIER)));

      MemberSelectExpressionTreeImpl memberSelect = new MemberSelectExpressionTreeImpl(
        expression, identifier,
        (AstNode) expression, dotToken, nonWildcardTypeArguments, identifier);

      return new MethodInvocationTreeImpl(
        memberSelect, (ArgumentListTreeImpl) explicitGenericInvocationSuffixNode.getFirstChild(JavaGrammar.ARGUMENTS),
        memberSelect, explicitGenericInvocationSuffixNode.getFirstChild(JavaGrammar.ARGUMENTS));
    }
  }

  private ExpressionTree applySelector(ExpressionTree expression, AstNode selectorNode) {
    JavaTreeMaker.checkType(selectorNode, JavaGrammar.SELECTOR);

    AstNodeType[] identifierTypes = new AstNodeType[] {JavaTokenType.IDENTIFIER, JavaKeyword.THIS, JavaKeyword.SUPER};
    if (selectorNode.hasDirectChildren(identifierTypes)) {
      InternalSyntaxToken identifierToken = InternalSyntaxToken.create(selectorNode.getFirstChild(identifierTypes));

      IdentifierTreeImpl identifier = new IdentifierTreeImpl(identifierToken);

      ExpressionTree result = new MemberSelectExpressionTreeImpl(
        expression, identifier,
        (AstNode) expression, selectorNode.getFirstChild(JavaPunctuator.DOT), identifier);

      if (selectorNode.hasDirectChildren(JavaGrammar.ARGUMENTS)) {
        ArgumentListTreeImpl arguments = (ArgumentListTreeImpl) selectorNode.getFirstChild(JavaGrammar.ARGUMENTS);
        result = new MethodInvocationTreeImpl(result, arguments,
          (AstNode) result, arguments);
      } else if (selectorNode.hasDirectChildren(JavaGrammar.SUPER_SUFFIX)) {
        result = applySuperSuffix(result, selectorNode.getFirstChild(JavaGrammar.SUPER_SUFFIX));
      }

      return result;
    } else if (selectorNode.hasDirectChildren(JavaGrammar.EXPLICIT_GENERIC_INVOCATION)) {
      return applyExplicitGenericInvocation(expression, selectorNode.getFirstChild(JavaPunctuator.DOT), selectorNode.getFirstChild(JavaGrammar.EXPLICIT_GENERIC_INVOCATION));
    } else if (selectorNode.hasDirectChildren(JavaKeyword.NEW)) {
      AstNode innerCreatorNode = selectorNode.getFirstChild(JavaGrammar.INNER_CREATOR);
      IdentifierTreeImpl identifier = new IdentifierTreeImpl(InternalSyntaxToken.create(innerCreatorNode.getFirstChild(JavaTokenType.IDENTIFIER)));

      AstNode classCreatorRestNode = innerCreatorNode.getFirstChild(JavaGrammar.CLASS_CREATOR_REST);
      ArgumentListTreeImpl arguments = (ArgumentListTreeImpl) classCreatorRestNode.getFirstChild(JavaGrammar.ARGUMENTS);

      ClassTree classBody = null;
      if (classCreatorRestNode.hasDirectChildren(JavaGrammar.CLASS_BODY)) {
        classBody = new ClassTreeImpl(
          classCreatorRestNode,
          Tree.Kind.CLASS,
          ModifiersTreeImpl.EMPTY,
          treeMaker.classBody(classCreatorRestNode.getFirstChild(JavaGrammar.CLASS_BODY)));
      }

      List<AstNode> children = Lists.newArrayList();
      children.add((AstNode) expression);
      children.add(selectorNode.getFirstChild(JavaPunctuator.DOT));
      children.add(selectorNode.getFirstChild(JavaKeyword.NEW));
      if (selectorNode.hasDirectChildren(JavaGrammar.NON_WILDCARD_TYPE_ARGUMENTS)) {
        children.add(selectorNode.getFirstChild(JavaGrammar.NON_WILDCARD_TYPE_ARGUMENTS));
      }
      children.add(identifier);
      children.add(classCreatorRestNode);

      return new NewClassTreeImpl(
        expression, identifier, arguments, classBody,
        children.toArray(new AstNode[children.size()]));
    } else if (selectorNode.hasDirectChildren(JavaGrammar.DIM_EXPR)) {
      return new ArrayAccessExpressionTreeImpl(
        expression, treeMaker.expression(selectorNode.getFirstChild(JavaGrammar.DIM_EXPR).getFirstChild(JavaGrammar.EXPRESSION)),
        (AstNode) expression, selectorNode.getFirstChild(JavaGrammar.DIM_EXPR));
    } else {
      throw new IllegalStateException(AstXmlPrinter.print(selectorNode));
    }
  }

}
