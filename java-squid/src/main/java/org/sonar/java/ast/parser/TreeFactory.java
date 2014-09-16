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
import org.sonar.java.model.JavaTree.ArrayTypeTreeImpl;
import org.sonar.java.model.JavaTree.NotImplementedTreeImpl;
import org.sonar.java.model.JavaTree.ParameterizedTypeTreeImpl;
import org.sonar.java.model.JavaTree.PrimitiveTypeTreeImpl;
import org.sonar.java.model.JavaTree.UnionTypeTreeImpl;
import org.sonar.java.model.JavaTree.WildcardTreeImpl;
import org.sonar.java.model.JavaTreeMaker;
import org.sonar.java.model.KindMaps;
import org.sonar.java.model.TypeParameterTreeImpl;
import org.sonar.java.model.declaration.AnnotationTreeImpl;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.ModifiersTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.expression.ArrayAccessExpressionTreeImpl;
import org.sonar.java.model.expression.AssignmentExpressionTreeImpl;
import org.sonar.java.model.expression.BinaryExpressionTreeImpl;
import org.sonar.java.model.expression.ConditionalExpressionTreeImpl;
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
import org.sonar.java.model.statement.CatchTreeImpl;
import org.sonar.java.model.statement.ContinueStatementTreeImpl;
import org.sonar.java.model.statement.DoWhileStatementTreeImpl;
import org.sonar.java.model.statement.EmptyStatementTreeImpl;
import org.sonar.java.model.statement.ExpressionStatementTreeImpl;
import org.sonar.java.model.statement.ForEachStatementImpl;
import org.sonar.java.model.statement.ForStatementTreeImpl;
import org.sonar.java.model.statement.IfStatementTreeImpl;
import org.sonar.java.model.statement.LabeledStatementTreeImpl;
import org.sonar.java.model.statement.ReturnStatementTreeImpl;
import org.sonar.java.model.statement.SwitchStatementTreeImpl;
import org.sonar.java.model.statement.SynchronizedStatementTreeImpl;
import org.sonar.java.model.statement.ThrowStatementTreeImpl;
import org.sonar.java.model.statement.TryStatementTreeImpl;
import org.sonar.java.model.statement.WhileStatementTreeImpl;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Collections;
import java.util.List;

public class TreeFactory {

  private final KindMaps kindMaps = new KindMaps();

  private final JavaTreeMaker treeMaker = new JavaTreeMaker();

  public ModifiersTreeImpl modifiers(Optional<List<AstNode>> modifierNodes) {
    if (!modifierNodes.isPresent()) {
      return ModifiersTreeImpl.emptyModifiers();
    }

    ImmutableList.Builder<Modifier> modifiers = ImmutableList.builder();
    ImmutableList.Builder<AnnotationTree> annotations = ImmutableList.builder();
    for (AstNode astNode : modifierNodes.get()) {
      if (astNode.is(Kind.ANNOTATION)) {
        annotations.add((AnnotationTree) astNode);
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

  // End of literals

  // Types

  public ExpressionTree newType(ExpressionTree basicOrClassType, Optional<List<AstNode>> dims) {
    if (!dims.isPresent()) {
      return basicOrClassType;
    } else {
      ExpressionTree result = basicOrClassType;

      for (AstNode dim : dims.get()) {
        List<AstNode> children = Lists.newArrayList();
        children.add((AstNode) result);
        children.addAll(dim.getChildren());

        result = new ArrayTypeTreeImpl(result,
          children);
      }

      return result;
    }
  }

  public ExpressionTree newClassType(Optional<List<AnnotationTreeImpl>> annotations, AstNode identifierAstNode, Optional<TypeArgumentListTreeImpl> typeArguments,
    Optional<List<ClassTypeComplement>> classTypeComplements) {

    IdentifierTreeImpl identifier = new IdentifierTreeImpl(InternalSyntaxToken.create(identifierAstNode));
    if (annotations.isPresent()) {
      identifier.prependChildren(annotations.get());
    }

    ExpressionTree result = identifier;
    if (typeArguments.isPresent()) {
      result = new ParameterizedTypeTreeImpl(result, typeArguments.get());
    }

    if (classTypeComplements.isPresent()) {
      for (ClassTypeComplement classTypeComplement : classTypeComplements.get()) {
        result = new MemberSelectExpressionTreeImpl(result, classTypeComplement.identifier(),
          (AstNode) result, classTypeComplement.dotToken(), classTypeComplement.identifier());

        if (classTypeComplement.typeArguments().isPresent()) {
          result = new ParameterizedTypeTreeImpl(result, classTypeComplement.typeArguments().get());
        }
      }
    }

    return result;
  }

  private static class ClassTypeComplement extends AstNode {

    private final InternalSyntaxToken dotToken;
    private final IdentifierTreeImpl identifier;
    private final Optional<TypeArgumentListTreeImpl> typeArguments;

    public ClassTypeComplement(InternalSyntaxToken dotToken, IdentifierTreeImpl identifier, Optional<TypeArgumentListTreeImpl> typeArguments) {
      super(null, null, null);

      this.dotToken = dotToken;
      this.identifier = identifier;
      this.typeArguments = typeArguments;

      addChild(dotToken);
      addChild(identifier);
      if (typeArguments.isPresent()) {
        addChild(typeArguments.get());
      }
    }

    public InternalSyntaxToken dotToken() {
      return dotToken;
    }

    public IdentifierTreeImpl identifier() {
      return identifier;
    }

    public Optional<TypeArgumentListTreeImpl> typeArguments() {
      return typeArguments;
    }

  }

  public ClassTypeComplement newClassTypeComplement(AstNode dotTokenAstNode, Optional<List<AnnotationTreeImpl>> annotations, AstNode identifierAstNode,
    Optional<TypeArgumentListTreeImpl> typeArguments) {

    IdentifierTreeImpl identifier = new IdentifierTreeImpl(InternalSyntaxToken.create(identifierAstNode));
    if (annotations.isPresent()) {
      identifier.prependChildren(annotations.get());
    }

    return new ClassTypeComplement(InternalSyntaxToken.create(dotTokenAstNode), identifier, typeArguments);
  }

  public ClassTypeListTreeImpl newClassTypeList(ExpressionTree classType, Optional<List<AstNode>> rests) {
    ImmutableList.Builder<Tree> classTypes = ImmutableList.builder();
    List<AstNode> children = Lists.newArrayList();

    classTypes.add(classType);
    children.add((AstNode) classType);

    if (rests.isPresent()) {
      for (AstNode rest : rests.get()) {
        for (AstNode child : rest.getChildren()) {
          if (!child.is(JavaPunctuator.COMMA)) {
            classTypes.add((Tree) child);
          }

          children.add(child);
        }
      }
    }

    return new ClassTypeListTreeImpl(classTypes.build(), children);
  }

  public TypeArgumentListTreeImpl newTypeArgumentList(AstNode openBracketTokenAstNode, Tree typeArgument, Optional<List<AstNode>> rests, AstNode closeBracketTokenAstNode) {
    InternalSyntaxToken openBracketToken = InternalSyntaxToken.create(openBracketTokenAstNode);
    InternalSyntaxToken closeBracketToken = InternalSyntaxToken.create(closeBracketTokenAstNode);

    ImmutableList.Builder<Tree> typeArguments = ImmutableList.builder();
    List<AstNode> children = Lists.newArrayList();

    typeArguments.add(typeArgument);
    children.add((AstNode) typeArgument);

    if (rests.isPresent()) {
      for (AstNode rest : rests.get()) {
        for (AstNode child : rest.getChildren()) {
          if (!child.is(JavaPunctuator.COMMA)) {
            typeArguments.add((Tree) child);
          }

          children.add(child);
        }
      }
    }

    return new TypeArgumentListTreeImpl(openBracketToken, typeArguments.build(), children, closeBracketToken);
  }

  public Tree completeTypeArgument(Optional<List<AnnotationTreeImpl>> annotations, Tree partial) {
    if (annotations.isPresent()) {
      ((JavaTree) partial).prependChildren(annotations.get());
    }

    return partial;
  }

  public ExpressionTree newBasicTypeArgument(ExpressionTree type) {
    return type;
  }

  public WildcardTreeImpl completeWildcardTypeArgument(AstNode queryTokenAstNode, Optional<WildcardTreeImpl> partial) {
    InternalSyntaxToken queryToken = InternalSyntaxToken.create(queryTokenAstNode);

    return partial.isPresent() ?
      partial.get().complete(queryToken) :
      new WildcardTreeImpl(Kind.UNBOUNDED_WILDCARD, queryToken);
  }

  public WildcardTreeImpl newWildcardTypeArguments(AstNode extendsOrSuperTokenAstNode, Optional<List<AnnotationTreeImpl>> annotations, ExpressionTree type) {
    InternalSyntaxToken extendsOrSuperToken = InternalSyntaxToken.create(extendsOrSuperTokenAstNode);
    return new WildcardTreeImpl(
      JavaKeyword.EXTENDS.getValue().equals(extendsOrSuperToken.text()) ? Kind.EXTENDS_WILDCARD : Kind.SUPER_WILDCARD,
      extendsOrSuperToken,
      annotations.isPresent() ? annotations.get() : ImmutableList.<AnnotationTreeImpl>of(),
      type);
  }

  public TypeParameterListTreeImpl newTypeParameterList(AstNode openBracketTokenAstNode, TypeParameterTreeImpl typeParameter, Optional<List<AstNode>> rests,
    AstNode closeBracketTokenAstNode) {
    InternalSyntaxToken openBracketToken = InternalSyntaxToken.create(openBracketTokenAstNode);
    InternalSyntaxToken closeBracketToken = InternalSyntaxToken.create(closeBracketTokenAstNode);

    ImmutableList.Builder<TypeParameterTreeImpl> typeParameters = ImmutableList.builder();
    List<AstNode> children = Lists.newArrayList();

    typeParameters.add(typeParameter);
    children.add(typeParameter);

    if (rests.isPresent()) {
      for (AstNode rest : rests.get()) {
        for (AstNode child : rest.getChildren()) {
          if (!child.is(JavaPunctuator.COMMA)) {
            typeParameters.add((TypeParameterTreeImpl) child);
          }

          children.add(child);
        }
      }
    }

    return new TypeParameterListTreeImpl(openBracketToken, typeParameters.build(), children, closeBracketToken);
  }

  public TypeParameterTreeImpl completeTypeParameter(Optional<List<AnnotationTreeImpl>> annotations, AstNode identifierAstNode, Optional<TypeParameterTreeImpl> partial) {
    IdentifierTreeImpl identifier = new IdentifierTreeImpl(InternalSyntaxToken.create(identifierAstNode));
    if (annotations.isPresent()) {
      identifier.prependChildren(annotations.get());
    }

    return partial.isPresent() ?
      partial.get().complete(identifier) :
      new TypeParameterTreeImpl(identifier);
  }

  public TypeParameterTreeImpl newTypeParameter(AstNode extendsTokenAstNode, BoundListTreeImpl bounds) {
    return new TypeParameterTreeImpl(InternalSyntaxToken.create(extendsTokenAstNode), bounds);
  }

  public BoundListTreeImpl newBounds(ExpressionTree classType, Optional<List<AstNode>> rests) {
    ImmutableList.Builder<Tree> classTypes = ImmutableList.builder();
    List<AstNode> children = Lists.newArrayList();

    classTypes.add(classType);
    children.add((AstNode) classType);

    if (rests.isPresent()) {
      for (AstNode rest : rests.get()) {
        for (AstNode child : rest.getChildren()) {
          if (!child.is(JavaPunctuator.AND)) {
            classTypes.add((Tree) child);
          }

          children.add(child);
        }
      }
    }

    return new BoundListTreeImpl(classTypes.build(), children);
  }

  // End of types

  // Annotations

  public ClassTreeImpl completeAnnotationType(AstNode atTokenAstNode, AstNode interfaceTokenAstNode, AstNode identifier, ClassTreeImpl partial) {
    return partial.complete(
      InternalSyntaxToken.create(atTokenAstNode),
      InternalSyntaxToken.create(interfaceTokenAstNode),
      new IdentifierTreeImpl(InternalSyntaxToken.create(identifier)));
  }

  public ClassTreeImpl newAnnotationType(AstNode openBraceTokenAstNode, Optional<List<AstNode>> annotationTypeElementDeclarations, AstNode closeBraceTokenAstNode) {
    InternalSyntaxToken openBraceToken = InternalSyntaxToken.create(openBraceTokenAstNode);
    InternalSyntaxToken closeBraceToken = InternalSyntaxToken.create(closeBraceTokenAstNode);

    // TODO
    ModifiersTreeImpl emptyModifiers = ModifiersTreeImpl.emptyModifiers();

    ImmutableList.Builder<Tree> members = ImmutableList.builder();

    List<AstNode> children = Lists.newArrayList();
    children.add(emptyModifiers);
    children.add(openBraceToken);

    if (annotationTypeElementDeclarations.isPresent()) {
      for (AstNode annotationTypeElementDeclaration : annotationTypeElementDeclarations.get()) {
        // FIXME
        if (annotationTypeElementDeclaration.is(JavaGrammar.ANNOTATION_TYPE_ELEMENT_DECLARATION)) {
          Preconditions.checkArgument(annotationTypeElementDeclaration.getNumberOfChildren() == 2);

          ModifiersTreeImpl modifiers = (ModifiersTreeImpl) annotationTypeElementDeclaration.getFirstChild(JavaGrammar.MODIFIERS);
          AstNode declaration = annotationTypeElementDeclaration.getLastChild();

          if (declaration.is(Kind.METHOD)) {
            // method
            members.add(((MethodTreeImpl) declaration).complete(modifiers));
            children.add(declaration);
          } else if (declaration.is(JavaGrammar.ANNOTATION_TYPE_ELEMENT_REST)) {
            // constant
            treeMaker.appendConstantDeclarations(ModifiersTreeImpl.EMPTY, members, declaration);
            children.add(annotationTypeElementDeclaration);
          } else if (declaration.is(Kind.ANNOTATION_TYPE)) {
            // TODO Complete with modifiers
            members.add((Tree) declaration);
            children.add(annotationTypeElementDeclaration);
          } else {
            // interface, class, enum
            members.add(modifiers, treeMaker.typeDeclaration(modifiers, declaration));
            children.add(annotationTypeElementDeclaration);
          }
        } else {
          // semi
          children.add(annotationTypeElementDeclaration);
        }
      }
    }

    children.add(closeBraceToken);

    return new ClassTreeImpl(emptyModifiers, members.build(), children);
  }

  public AstNode completeAnnotationTypeMember(ModifiersTreeImpl modifiers, AstNode annotationTypeElementRest) {
    AstNodeType type = JavaGrammar.ANNOTATION_TYPE_ELEMENT_DECLARATION;
    AstNode result = new AstNode(type, type.toString(), null);
    result.addChild(modifiers);
    result.addChild(annotationTypeElementRest);
    return result;
  }

  public AstNode newAnnotationTypeMember(ExpressionTree type, AstNode identifierAstNode, AstNode annotationMethodOrConstantRest, AstNode semiTokenAstNode) {
    if (annotationMethodOrConstantRest.is(Kind.METHOD)) {
      MethodTreeImpl partial = (MethodTreeImpl) annotationMethodOrConstantRest;
      partial.complete(type, new IdentifierTreeImpl(InternalSyntaxToken.create(identifierAstNode)));
      partial.addChild(semiTokenAstNode);

      return partial;
    }

    AstNodeType type2 = JavaGrammar.ANNOTATION_TYPE_ELEMENT_REST;
    AstNode result = new AstNode(type2, type2.toString(), null);

    result.addChild((AstNode) type);
    result.addChild(identifierAstNode);
    result.addChild(annotationMethodOrConstantRest);
    result.addChild(semiTokenAstNode);

    return result;
  }

  public MethodTreeImpl newAnnotationTypeMethod(AstNode openParenTokenAstNode, AstNode closeParenTokenAstNode, Optional<ExpressionTree> defaultValue) {
    InternalSyntaxToken openParenToken = InternalSyntaxToken.create(openParenTokenAstNode);
    InternalSyntaxToken closeParenToken = InternalSyntaxToken.create(closeParenTokenAstNode);

    MethodTreeImpl tree = new MethodTreeImpl(defaultValue.isPresent() ? defaultValue.get() : null);

    tree.prependChildren(openParenToken, closeParenToken);

    return tree;
  }

  public ExpressionTree newDefaultValue(AstNode defaultTokenAstNode, ExpressionTree elementValue) {
    InternalSyntaxToken defaultToken = InternalSyntaxToken.create(defaultTokenAstNode);

    ((JavaTree) elementValue).prependChildren(defaultToken);
    return elementValue;
  }

  public AnnotationTreeImpl newAnnotation(AstNode atTokenAstNode, ExpressionTree qualifiedIdentifier, Optional<ArgumentListTreeImpl> arguments) {
    InternalSyntaxToken atToken = InternalSyntaxToken.create(atTokenAstNode);

    return new AnnotationTreeImpl(
      atToken,
      qualifiedIdentifier,
      arguments.isPresent() ?
        arguments.get() :
        null);
  }

  public ArgumentListTreeImpl completeNormalAnnotation(AstNode openParenTokenAstNode, Optional<ArgumentListTreeImpl> partial, AstNode closeParenTokenAstNode) {
    InternalSyntaxToken openParenToken = InternalSyntaxToken.create(openParenTokenAstNode);
    InternalSyntaxToken closeParenToken = InternalSyntaxToken.create(closeParenTokenAstNode);

    if (!partial.isPresent()) {
      return new ArgumentListTreeImpl(openParenToken, closeParenToken);
    }

    ArgumentListTreeImpl elementValuePairs = partial.get();
    elementValuePairs.prependChildren(openParenToken);
    elementValuePairs.addChild(closeParenToken);

    return elementValuePairs;
  }

  public ArgumentListTreeImpl newNormalAnnotation(AssignmentExpressionTreeImpl elementValuePair, Optional<List<AstNode>> rests) {
    ImmutableList.Builder<ExpressionTree> expressions = ImmutableList.builder();
    List<AstNode> children = Lists.newArrayList();

    expressions.add(elementValuePair);
    children.add(elementValuePair);

    if (rests.isPresent()) {
      for (AstNode rest : rests.get()) {
        for (AstNode child : rest.getChildren()) {
          if (!child.is(JavaPunctuator.COMMA)) {
            expressions.add((ExpressionTree) child);
          }
          children.add(child);
        }
      }
    }

    return new ArgumentListTreeImpl(expressions.build(), children);
  }

  public AssignmentExpressionTreeImpl newElementValuePair(AstNode identifierAstNode, AstNode equalTokenAstNode, ExpressionTree elementValue) {
    InternalSyntaxToken operator = InternalSyntaxToken.create(equalTokenAstNode);

    return new AssignmentExpressionTreeImpl(
      kindMaps.getAssignmentOperator((JavaPunctuator) operator.getType()),
      new IdentifierTreeImpl(InternalSyntaxToken.create(identifierAstNode)),
      operator,
      elementValue);
  }

  public NewArrayTreeImpl completeElementValueArrayInitializer(
    AstNode openBraceTokenAstNode, Optional<NewArrayTreeImpl> partial, Optional<AstNode> commaTokenAstNode, AstNode closeBraceTokenAstNode) {

    InternalSyntaxToken openBraceToken = InternalSyntaxToken.create(openBraceTokenAstNode);
    InternalSyntaxToken commaToken = commaTokenAstNode.isPresent() ? InternalSyntaxToken.create(commaTokenAstNode.get()) : null;
    InternalSyntaxToken closeBraceToken = InternalSyntaxToken.create(closeBraceTokenAstNode);

    NewArrayTreeImpl elementValues = partial.isPresent() ?
      partial.get() :
      new NewArrayTreeImpl(ImmutableList.<ExpressionTree>of(), ImmutableList.<ExpressionTree>of(), ImmutableList.<AstNode>of());

    elementValues.prependChildren(openBraceToken);
    if (commaToken != null) {
      elementValues.addChild(commaToken);
    }
    elementValues.addChild(closeBraceToken);

    return elementValues;
  }

  public NewArrayTreeImpl newElementValueArrayInitializer(ExpressionTree elementValue, Optional<List<AstNode>> rests) {
    ImmutableList.Builder<ExpressionTree> expressions = ImmutableList.builder();
    List<AstNode> children = Lists.newArrayList();

    expressions.add(elementValue);
    children.add((AstNode) elementValue);

    if (rests.isPresent()) {
      for (AstNode rest : rests.get()) {
        for (AstNode child : rest.getChildren()) {
          if (!child.is(JavaPunctuator.COMMA)) {
            expressions.add((ExpressionTree) child);
          }
          children.add(child);
        }
      }
    }

    return new NewArrayTreeImpl(ImmutableList.<ExpressionTree>of(), expressions.build(), children);
  }

  public ArgumentListTreeImpl newSingleElementAnnotation(AstNode openParenTokenAstNode, ExpressionTree elementValue, AstNode closeParenTokenAstNode) {
    InternalSyntaxToken openParenToken = InternalSyntaxToken.create(openParenTokenAstNode);
    InternalSyntaxToken closeParenToken = InternalSyntaxToken.create(closeParenTokenAstNode);

    return new ArgumentListTreeImpl(openParenToken, elementValue, closeParenToken);
  }

  // End of annotations

  // Formal parameters

  public FormalParametersListTreeImpl completeParenFormalParameters(AstNode openParenTokenAstNode, Optional<FormalParametersListTreeImpl> partial, AstNode closeParenTokenAstNode) {
    InternalSyntaxToken openParenToken = InternalSyntaxToken.create(openParenTokenAstNode);
    InternalSyntaxToken closeParenToken = InternalSyntaxToken.create(closeParenTokenAstNode);

    return partial.isPresent() ?
      partial.get().complete(openParenToken, closeParenToken) :
      new FormalParametersListTreeImpl(openParenToken, closeParenToken);
  }

  public FormalParametersListTreeImpl completeTypeFormalParameters(ModifiersTreeImpl modifiers, ExpressionTree type, FormalParametersListTreeImpl partial) {
    VariableTreeImpl variable = partial.get(0);

    variable.completeType(type);
    partial.prependChildren(modifiers, (AstNode) type);

    return partial;
  }

  public FormalParametersListTreeImpl prependNewFormalParameter(VariableTreeImpl variable, Optional<AstNode> rest) {
    if (rest.isPresent()) {
      AstNode comma = rest.get().getFirstChild(JavaPunctuator.COMMA);
      FormalParametersListTreeImpl partial = (FormalParametersListTreeImpl) rest.get().getLastChild();

      partial.add(0, variable);
      partial.prependChildren(variable, comma);

      return partial;
    } else {
      return new FormalParametersListTreeImpl(variable);
    }
  }

  public FormalParametersListTreeImpl newVariableArgumentFormalParameter(Optional<List<AnnotationTreeImpl>> annotations, AstNode ellipsisTokenAstNode, VariableTreeImpl variable) {
    InternalSyntaxToken ellipsisToken = InternalSyntaxToken.create(ellipsisTokenAstNode);

    variable.setVararg(true);

    return new FormalParametersListTreeImpl(
      annotations.isPresent() ? annotations.get() : ImmutableList.<AnnotationTreeImpl>of(),
      ellipsisToken,
      variable);
  }

  public VariableTreeImpl newVariableDeclaratorId(AstNode identifierAstNode, Optional<List<AstNode>> dims) {
    IdentifierTreeImpl identifier = new IdentifierTreeImpl(InternalSyntaxToken.create(identifierAstNode));
    return new VariableTreeImpl(
      identifier,
      dims.isPresent() ? dims.get().size() : 0,
      dims.isPresent() ? dims.get() : ImmutableList.<AstNode>of());
  }

  public VariableTreeImpl newFormalParameter(ModifiersTreeImpl modifiers, ExpressionTree type, VariableTreeImpl variable) {
    variable.prependChildren(modifiers, (AstNode) type);
    return variable.completeType(type);
  }

  // End of formal parameters

  // Statements

  public VariableDeclaratorListTreeImpl completeLocalVariableDeclaration(
    ModifiersTreeImpl modifiers,
    ExpressionTree type,
    VariableDeclaratorListTreeImpl variables,
    AstNode semicolonTokenAstNode) {

    variables.prependChildren(modifiers, (AstNode) type);
    variables.addChild(semicolonTokenAstNode);

    for (VariableTreeImpl variable : variables) {
      variable.completeModifiersAndType(modifiers, type);
    }

    return variables;
  }

  public VariableDeclaratorListTreeImpl newVariableDeclarators(VariableTreeImpl variable, Optional<List<Tuple<AstNode, VariableTreeImpl>>> rests) {
    ImmutableList.Builder<VariableTreeImpl> variables = ImmutableList.builder();

    variables.add(variable);
    List<AstNode> children = Lists.newArrayList();
    children.add(variable);

    if (rests.isPresent()) {
      for (Tuple<AstNode, VariableTreeImpl> rest : rests.get()) {
        variables.add(rest.second());
        children.add(rest.first());
        children.add(rest.second());
      }
    }

    return new VariableDeclaratorListTreeImpl(variables.build(), children);
  }

  public VariableTreeImpl completeVariableDeclarator(AstNode identifierAstNode, Optional<List<AstNode>> dims, Optional<VariableTreeImpl> partial) {
    IdentifierTreeImpl identifier = new IdentifierTreeImpl(InternalSyntaxToken.create(identifierAstNode));

    List<AstNode> children = Lists.newArrayList();
    if (dims.isPresent()) {
      for (AstNode dim : dims.get()) {
        children.add(dim);
      }
    }

    if (partial.isPresent()) {
      children.add(0, identifier);
      partial.get().prependChildren(children);

      return partial.get().completeIdentifierAndDims(identifier, dims.isPresent() ? dims.get().size() : 0);
    } else {
      return new VariableTreeImpl(
        identifier, dims.isPresent() ? dims.get().size() : 0,
        children);
    }
  }

  public VariableTreeImpl newVariableDeclarator(AstNode equalTokenAstNode, ExpressionTree initializer) {
    InternalSyntaxToken equalToken = InternalSyntaxToken.create(equalTokenAstNode);

    return new VariableTreeImpl(equalToken, initializer,
      equalToken, (AstNode) initializer);
  }

  public BlockTreeImpl block(AstNode openBraceTokenAstNode, AstNode statements, AstNode closeBraceTokenAstNode) {
    InternalSyntaxToken openBraceToken = InternalSyntaxToken.create(openBraceTokenAstNode);
    InternalSyntaxToken closeBraceToken = InternalSyntaxToken.create(closeBraceTokenAstNode);

    return new BlockTreeImpl(openBraceToken, treeMaker.blockStatements(statements), closeBraceToken,
      openBraceToken, statements, closeBraceToken);
  }

  public AssertStatementTreeImpl completeAssertStatement(AstNode assertToken, ExpressionTree expression, Optional<AssertStatementTreeImpl> detailExpression, AstNode semicolonToken) {
    return detailExpression.isPresent() ?
      detailExpression.get().complete(expression,
        assertToken, (AstNode) expression, semicolonToken) :
      new AssertStatementTreeImpl(expression,
        assertToken, (AstNode) expression, semicolonToken);
  }

  public AssertStatementTreeImpl newAssertStatement(AstNode colonToken, ExpressionTree expression) {
    return new AssertStatementTreeImpl(expression,
      colonToken, (AstNode) expression);
  }

  public IfStatementTreeImpl completeIf(AstNode ifToken, AstNode openParen, ExpressionTree condition, AstNode closeParen, StatementTree statement,
    Optional<IfStatementTreeImpl> elseClause) {
    InternalSyntaxToken ifKeyword = InternalSyntaxToken.create(ifToken);
    InternalSyntaxToken openParenToken = InternalSyntaxToken.create(openParen);
    InternalSyntaxToken closeParenToken = InternalSyntaxToken.create(closeParen);
    if (elseClause.isPresent()) {
      return elseClause.get().complete(ifKeyword, openParenToken, condition, closeParenToken, statement,
        ifKeyword, openParenToken, (AstNode) condition, closeParenToken, (AstNode) statement);
    } else {
      return new IfStatementTreeImpl(ifKeyword, openParenToken, condition, closeParenToken, statement,
        ifKeyword, openParenToken, (AstNode) condition, closeParenToken, (AstNode) statement);
    }
  }

  public IfStatementTreeImpl newIfWithElse(AstNode elseToken, StatementTree elseStatement) {
    InternalSyntaxToken elseKeyword = InternalSyntaxToken.create(elseToken);
    return new IfStatementTreeImpl(elseKeyword, elseStatement,
      elseKeyword, (AstNode) elseStatement);
  }

  public ForStatementTreeImpl newStandardForStatement(
    AstNode forTokenAstNode,
    AstNode openParenTokenAstNode,
    Optional<StatementExpressionListTreeImpl> forInit, AstNode forInitSemicolonTokenAstNode,
    Optional<ExpressionTree> expression, AstNode expressionSemicolonTokenAstNode,
    Optional<StatementExpressionListTreeImpl> forUpdate, AstNode forUpdateSemicolonTokenAstNode,
    StatementTree statement) {

    StatementExpressionListTreeImpl forInit2 = forInit.isPresent() ? forInit.get() : new StatementExpressionListTreeImpl(ImmutableList.<StatementTree>of());
    StatementExpressionListTreeImpl forUpdate2 = forUpdate.isPresent() ? forUpdate.get() : new StatementExpressionListTreeImpl(ImmutableList.<StatementTree>of());

    ForStatementTreeImpl result = new ForStatementTreeImpl(
      forInit2,
      expression.isPresent() ? expression.get() : null,
      forUpdate2,
      statement);

    List<AstNode> children = Lists.newArrayList();
    children.add(forTokenAstNode);
    children.add(openParenTokenAstNode);
    children.add(forInit2);
    children.add(forInitSemicolonTokenAstNode);
    if (expression.isPresent()) {
      children.add((AstNode) expression.get());
    }
    children.add(expressionSemicolonTokenAstNode);
    children.add(forUpdate2);
    children.add(forUpdateSemicolonTokenAstNode);
    children.add((AstNode) statement);

    result.prependChildren(children);

    return result;
  }

  public StatementExpressionListTreeImpl newForInitDeclaration(ModifiersTreeImpl modifiers, ExpressionTree type, VariableDeclaratorListTreeImpl variables) {
    for (VariableTreeImpl variable : variables) {
      variable.completeModifiersAndType(modifiers, type);
    }

    StatementExpressionListTreeImpl result = new StatementExpressionListTreeImpl(variables);
    result.prependChildren(modifiers, (AstNode) type, variables);

    return result;
  }

  public StatementExpressionListTreeImpl newStatementExpressions(ExpressionTree expression, Optional<List<AstNode>> rests) {
    List<AstNode> children = Lists.newArrayList();
    ImmutableList.Builder<StatementTree> statements = ImmutableList.builder();

    ExpressionStatementTreeImpl statement = new ExpressionStatementTreeImpl(
      expression, null,
      (AstNode) expression);
    statements.add(statement);
    children.add(statement);

    if (rests.isPresent()) {
      for (AstNode rest : rests.get()) {
        children.add(rest.getFirstChild());

        statement = new ExpressionStatementTreeImpl(
          (ExpressionTree) rest.getLastChild(), null,
          rest.getLastChild());
        statements.add(statement);
        children.add(statement);
      }
    }

    StatementExpressionListTreeImpl result = new StatementExpressionListTreeImpl(statements.build());
    result.prependChildren(children);

    return result;
  }

  public ForEachStatementImpl newForeachStatement(
    AstNode forTokenAstNode,
    AstNode openParenTokenAstNode,
    VariableTreeImpl variable, AstNode colonTokenAstNode, ExpressionTree expression,
    AstNode closeParenTokenAstNode,
    StatementTree statement) {

    return new ForEachStatementImpl(
      variable, expression, statement,
      forTokenAstNode, openParenTokenAstNode, variable, colonTokenAstNode, (AstNode) expression, closeParenTokenAstNode, (AstNode) statement);
  }

  public WhileStatementTreeImpl whileStatement(AstNode whileToken, AstNode openParen, ExpressionTree expression, AstNode closeParen, StatementTree statement) {
    InternalSyntaxToken whileKeyword = InternalSyntaxToken.create(whileToken);
    InternalSyntaxToken openParenToken = InternalSyntaxToken.create(openParen);
    InternalSyntaxToken closeParenToken = InternalSyntaxToken.create(closeParen);
    return new WhileStatementTreeImpl(whileKeyword, openParenToken, expression, closeParenToken, statement,
      whileKeyword, openParenToken, (AstNode) expression, closeParenToken, (AstNode) statement);
  }

  public DoWhileStatementTreeImpl doWhileStatement(AstNode doToken, StatementTree statement, AstNode whileToken, AstNode openParen, ExpressionTree expression, AstNode closeParen,
    AstNode semicolon) {
    InternalSyntaxToken doKeyword = InternalSyntaxToken.create(doToken);
    InternalSyntaxToken whileKeyword = InternalSyntaxToken.create(whileToken);
    InternalSyntaxToken openParenToken = InternalSyntaxToken.create(openParen);
    InternalSyntaxToken closeParenToken = InternalSyntaxToken.create(closeParen);
    InternalSyntaxToken semiColonToken = InternalSyntaxToken.create(semicolon);
    return new DoWhileStatementTreeImpl(doKeyword, statement, whileKeyword, openParenToken, expression, closeParenToken, semiColonToken,
      doKeyword, (AstNode) statement, whileKeyword, openParenToken, (AstNode) expression, closeParenToken, semiColonToken);
  }

  public TryStatementTreeImpl completeStandardTryStatement(AstNode tryTokenAstNode, BlockTreeImpl block, TryStatementTreeImpl partial) {
    InternalSyntaxToken tryToken = InternalSyntaxToken.create(tryTokenAstNode);

    return partial.completeStandardTry(tryToken, block);
  }

  public TryStatementTreeImpl newTryCatch(Optional<List<CatchTreeImpl>> catches, Optional<BlockTreeImpl> finallyBlock) {
    return new TryStatementTreeImpl(catches.isPresent() ? catches.get() : ImmutableList.<CatchTreeImpl>of(), finallyBlock.isPresent() ? finallyBlock.get() : null);
  }

  public TryStatementTreeImpl newTryFinally(BlockTreeImpl finallyBlock) {
    return new TryStatementTreeImpl(finallyBlock);
  }

  public CatchTreeImpl newCatchClause(AstNode catchTokenAstNode, AstNode openParenTokenAstNode, VariableTreeImpl parameter, AstNode closeParenTokenAstNode, BlockTreeImpl block) {
    InternalSyntaxToken catchToken = InternalSyntaxToken.create(catchTokenAstNode);
    InternalSyntaxToken openParenToken = InternalSyntaxToken.create(openParenTokenAstNode);
    InternalSyntaxToken closeParenToken = InternalSyntaxToken.create(closeParenTokenAstNode);

    return new CatchTreeImpl(catchToken, openParenToken, parameter, closeParenToken, block);
  }

  public VariableTreeImpl newCatchFormalParameter(Optional<ModifiersTreeImpl> modifiers, Tree type, VariableTreeImpl parameter) {
    // TODO modifiers

    if (modifiers.isPresent()) {
      parameter.prependChildren(modifiers.get(), (AstNode) type);
    } else {
      parameter.prependChildren((AstNode) type);
    }

    return parameter.completeType(type);
  }

  public Tree newCatchType(ExpressionTree qualifiedIdentifier, Optional<List<AstNode>> rests) {
    if (!rests.isPresent()) {
      return qualifiedIdentifier;
    }

    List<AstNode> children = Lists.newArrayList();
    ImmutableList.Builder<Tree> types = ImmutableList.builder();

    children.add((AstNode) qualifiedIdentifier);
    types.add(qualifiedIdentifier);

    for (AstNode rest : rests.get()) {
      children.add(rest.getFirstChild());

      ExpressionTree qualifiedIdentifier2 = (ExpressionTree) rest.getLastChild();
      types.add(qualifiedIdentifier2);

      children.add((AstNode) qualifiedIdentifier2);
    }

    return new UnionTypeTreeImpl(new TypeUnionListTreeImpl(types.build(), children));
  }

  public BlockTreeImpl newFinallyBlock(AstNode finallyTokenAstNode, BlockTreeImpl block) {
    InternalSyntaxToken finallyToken = InternalSyntaxToken.create(finallyTokenAstNode);
    block.prependChildren(finallyToken);

    return block;
  }

  public TryStatementTreeImpl newTryWithResourcesStatement(
    AstNode tryTokenAstNode, AstNode openParenTokenAstNode, ResourceListTreeImpl resources, AstNode closeParenTokenAstNode,
    BlockTreeImpl block,
    Optional<List<CatchTreeImpl>> catches, Optional<BlockTreeImpl> finallyBlock) {

    InternalSyntaxToken tryToken = InternalSyntaxToken.create(tryTokenAstNode);
    InternalSyntaxToken openParenToken = InternalSyntaxToken.create(openParenTokenAstNode);
    InternalSyntaxToken closeParenToken = InternalSyntaxToken.create(closeParenTokenAstNode);

    return new TryStatementTreeImpl(
      tryToken,
      openParenToken, resources, closeParenToken,
      block,
      catches.isPresent() ? catches.get() : ImmutableList.<CatchTreeImpl>of(),
      finallyBlock.isPresent() ? finallyBlock.get() : null);
  }

  public ResourceListTreeImpl newResources(List<AstNode> rests) {
    List<AstNode> children = Lists.newArrayList();
    ImmutableList.Builder<VariableTreeImpl> resources = ImmutableList.builder();

    for (AstNode rest : rests) {
      VariableTreeImpl resource = (VariableTreeImpl) rest.getFirstChild();
      children.add(resource);
      resources.add(resource);

      if (rest.getNumberOfChildren() == 2) {
        children.add(rest.getLastChild());
      }
    }

    return new ResourceListTreeImpl(resources.build(), children);
  }

  public VariableTreeImpl newResource(ModifiersTreeImpl modifiers, ExpressionTree classType, VariableTreeImpl partial, AstNode equalTokenAstNode, ExpressionTree expression) {
    // TODO modifiers
    partial.prependChildren(modifiers, (AstNode) classType);
    partial.addChild(equalTokenAstNode);
    partial.addChild((AstNode) expression);

    return partial.completeTypeAndInitializer(classType, expression);
  }

  public SwitchStatementTreeImpl switchStatement(AstNode switchToken, AstNode openParen, ExpressionTree expression, AstNode closeParen,
    AstNode leftCurlyBraceToken, Optional<List<CaseGroupTreeImpl>> optionalGroups, AstNode rightCurlyBraceToken) {

    InternalSyntaxToken switchKeyword = InternalSyntaxToken.create(switchToken);
    InternalSyntaxToken openParenToken = InternalSyntaxToken.create(openParen);
    InternalSyntaxToken closeParenToken = InternalSyntaxToken.create(closeParen);
    InternalSyntaxToken openBraceToken = InternalSyntaxToken.create(leftCurlyBraceToken);
    InternalSyntaxToken closeBraceToken = InternalSyntaxToken.create(rightCurlyBraceToken);

    List<CaseGroupTreeImpl> groups = optionalGroups.isPresent() ? optionalGroups.get() : Collections.<CaseGroupTreeImpl>emptyList();

    ImmutableList.Builder<AstNode> children = ImmutableList.builder();
    children.add(switchKeyword, openParenToken, (AstNode) expression, closeParenToken, openBraceToken);
    children.addAll(groups);
    children.add(closeBraceToken);

    return new SwitchStatementTreeImpl(switchKeyword, openParenToken, expression, closeParenToken,
      openBraceToken, groups, closeBraceToken,
      children.build());
  }

  public CaseGroupTreeImpl switchGroup(List<CaseLabelTreeImpl> labels, AstNode blockStatementsAstNode) {
    List<AstNode> blockStatements = blockStatementsAstNode.getChildren();

    ImmutableList.Builder<StatementTree> builder = ImmutableList.builder();
    for (AstNode blockStatement : blockStatements) {
      builder.addAll(treeMaker.blockStatement(blockStatement));
    }

    return new CaseGroupTreeImpl(labels, builder.build(), blockStatementsAstNode);
  }

  public CaseLabelTreeImpl newCaseSwitchLabel(AstNode caseToken, ExpressionTree expression, AstNode colonToken) {
    return new CaseLabelTreeImpl(expression,
      caseToken, (AstNode) expression, colonToken);
  }

  public CaseLabelTreeImpl newDefaultSwitchLabel(AstNode defaultToken, AstNode colonToken) {
    return new CaseLabelTreeImpl(null,
      defaultToken, colonToken);
  }

  public SynchronizedStatementTreeImpl synchronizedStatement(AstNode synchronizedToken, AstNode openParen, ExpressionTree expression, AstNode closeParen, BlockTreeImpl block) {
    InternalSyntaxToken synchronizedKeyword = InternalSyntaxToken.create(synchronizedToken);
    InternalSyntaxToken openParenToken = InternalSyntaxToken.create(openParen);
    InternalSyntaxToken closeParenToken = InternalSyntaxToken.create(closeParen);
    return new SynchronizedStatementTreeImpl(synchronizedKeyword, openParenToken, expression, closeParenToken, block,
      synchronizedKeyword, openParenToken, (AstNode) expression, closeParenToken, block);
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

  public ReturnStatementTreeImpl returnStatement(AstNode returnToken, Optional<ExpressionTree> expression, AstNode semicolonToken) {
    return expression.isPresent() ?
      new ReturnStatementTreeImpl(expression.get(),
        returnToken, (AstNode) expression.get(), semicolonToken) :
      new ReturnStatementTreeImpl(null,
        returnToken, semicolonToken);
  }

  public ThrowStatementTreeImpl throwStatement(AstNode throwToken, ExpressionTree expression, AstNode semicolonToken) {
    return new ThrowStatementTreeImpl(expression,
      throwToken, (AstNode) expression, semicolonToken);
  }

  public LabeledStatementTreeImpl labeledStatement(AstNode identifier, AstNode colon, StatementTree statement) {
    return new LabeledStatementTreeImpl(treeMaker.identifier(identifier), statement,
      identifier, colon, (AstNode) statement);
  }

  public ExpressionStatementTreeImpl expressionStatement(ExpressionTree expression, AstNode semicolonTokenAstNode) {
    InternalSyntaxToken semicolonToken = InternalSyntaxToken.create(semicolonTokenAstNode);

    return new ExpressionStatementTreeImpl(expression, semicolonToken,
      (AstNode) expression, semicolonToken);
  }

  public EmptyStatementTreeImpl emptyStatement(AstNode semicolon) {
    return new EmptyStatementTreeImpl(semicolon);
  }

  // End of statements

  // Expressions

  public ExpressionTree assignmentExpression(ExpressionTree expression, Optional<List<OperatorAndOperand>> operatorAndOperands) {
    if (!operatorAndOperands.isPresent()) {
      return expression;
    }

    ExpressionTree result = null;
    InternalSyntaxToken lastOperator = null;
    for (OperatorAndOperand operatorAndOperand : Lists.reverse(operatorAndOperands.get())) {
      if (lastOperator == null) {
        result = operatorAndOperand.operand();
      } else {
        result = new AssignmentExpressionTreeImpl(
          kindMaps.getAssignmentOperator((JavaPunctuator) lastOperator.getType()),
          operatorAndOperand.operand(),
          lastOperator,
          result);
      }

      lastOperator = operatorAndOperand.operator();
    }

    result = new AssignmentExpressionTreeImpl(
      kindMaps.getAssignmentOperator((JavaPunctuator) lastOperator.getType()),
      expression,
      lastOperator,
      result);

    return result;
  }

  public ExpressionTree completeTernaryExpression(ExpressionTree expression, Optional<ConditionalExpressionTreeImpl> partial) {
    return partial.isPresent() ?
      partial.get().complete(expression) :
      expression;
  }

  public ConditionalExpressionTreeImpl newTernaryExpression(AstNode queryTokenAstNode, ExpressionTree trueExpression, AstNode colonTokenAstNode, ExpressionTree falseExpression) {
    InternalSyntaxToken queryToken = InternalSyntaxToken.create(queryTokenAstNode);
    InternalSyntaxToken colonToken = InternalSyntaxToken.create(colonTokenAstNode);

    return new ConditionalExpressionTreeImpl(queryToken, trueExpression, colonToken, falseExpression);
  }

  public ExpressionTree completeInstanceofExpression(ExpressionTree expression, Optional<InstanceOfTreeImpl> partial) {
    return partial.isPresent() ?
      partial.get().complete(expression) :
      expression;
  }

  public InstanceOfTreeImpl newInstanceofExpression(AstNode instanceofTokenAstNode, Tree type) {
    InternalSyntaxToken instanceofToken = InternalSyntaxToken.create(instanceofTokenAstNode);
    return new InstanceOfTreeImpl(instanceofToken, type,
      (AstNode) type);
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

  // TODO Allow to use the same method several times

  public OperatorAndOperand newOperatorAndOperand11(AstNode operator, ExpressionTree operand) {
    return newOperatorAndOperand(operator, operand);
  }

  public ExpressionTree binaryExpression10(ExpressionTree expression, Optional<List<OperatorAndOperand>> operatorAndOperands) {
    return binaryExpression(expression, operatorAndOperands);
  }

  public OperatorAndOperand newOperatorAndOperand10(AstNode operator, ExpressionTree operand) {
    return newOperatorAndOperand(operator, operand);
  }

  public ExpressionTree binaryExpression9(ExpressionTree expression, Optional<List<OperatorAndOperand>> operatorAndOperands) {
    return binaryExpression(expression, operatorAndOperands);
  }

  public OperatorAndOperand newOperatorAndOperand9(AstNode operator, ExpressionTree operand) {
    return newOperatorAndOperand(operator, operand);
  }

  public ExpressionTree binaryExpression8(ExpressionTree expression, Optional<List<OperatorAndOperand>> operatorAndOperands) {
    return binaryExpression(expression, operatorAndOperands);
  }

  public OperatorAndOperand newOperatorAndOperand8(AstNode operator, ExpressionTree operand) {
    return newOperatorAndOperand(operator, operand);
  }

  public ExpressionTree binaryExpression7(ExpressionTree expression, Optional<List<OperatorAndOperand>> operatorAndOperands) {
    return binaryExpression(expression, operatorAndOperands);
  }

  public OperatorAndOperand newOperatorAndOperand7(AstNode operator, ExpressionTree operand) {
    return newOperatorAndOperand(operator, operand);
  }

  public ExpressionTree binaryExpression6(ExpressionTree expression, Optional<List<OperatorAndOperand>> operatorAndOperands) {
    return binaryExpression(expression, operatorAndOperands);
  }

  public OperatorAndOperand newOperatorAndOperand6(AstNode operator, ExpressionTree operand) {
    return newOperatorAndOperand(operator, operand);
  }

  public ExpressionTree binaryExpression5(ExpressionTree expression, Optional<List<OperatorAndOperand>> operatorAndOperands) {
    return binaryExpression(expression, operatorAndOperands);
  }

  public OperatorAndOperand newOperatorAndOperand5(AstNode operator, ExpressionTree operand) {
    return newOperatorAndOperand(operator, operand);
  }

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

  public TypeCastExpressionTreeImpl newClassCastExpression(Tree type, Optional<List<AstNode>> classTypes, AstNode closeParenTokenAstNode, ExpressionTree expression) {
    InternalSyntaxToken closeParenToken = InternalSyntaxToken.create(closeParenTokenAstNode);

    List<AstNode> children = Lists.newArrayList();
    children.add((AstNode) type);
    if (classTypes.isPresent()) {
      for (AstNode classType : classTypes.get()) {
        children.addAll(classType.getChildren());
      }
    }
    children.add(closeParenToken);
    children.add((AstNode) expression);

    return new TypeCastExpressionTreeImpl(type, expression, closeParenToken,
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

  public NotImplementedTreeImpl newTypeMethodReference(Tree type, AstNode doubleColonToken) {
    // TODO SONARJAVA-613
    return new NotImplementedTreeImpl((AstNode) type, doubleColonToken);
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

  public ExpressionTree lambdaExpression(LambdaParameterListTreeImpl parameters, AstNode arrowToken, Tree body) {
    return new LambdaExpressionTreeImpl(parameters.openParenToken(), ImmutableList.<VariableTree>builder().addAll(parameters).build(), parameters.closeParenToken(), body,
      parameters, arrowToken, (AstNode) body);
  }

  public LambdaParameterListTreeImpl newInferedParameters(
    AstNode openParenTokenAstNode,
    Optional<Tuple<VariableTreeImpl, Optional<List<Tuple<AstNode, VariableTreeImpl>>>>> identifiersOpt,
    AstNode closeParenTokenAstNode) {

    InternalSyntaxToken openParenToken = InternalSyntaxToken.create(openParenTokenAstNode);
    InternalSyntaxToken closeParenToken = InternalSyntaxToken.create(closeParenTokenAstNode);

    ImmutableList.Builder<VariableTreeImpl> params = ImmutableList.builder();

    List<AstNode> children = Lists.newArrayList();
    children.add(openParenToken);

    if (identifiersOpt.isPresent()) {
      Tuple<VariableTreeImpl, Optional<List<Tuple<AstNode, VariableTreeImpl>>>> identifiers = identifiersOpt.get();

      params.add(identifiers.first());
      children.add(identifiers.first());

      if (identifiers.second().isPresent()) {
        for (Tuple<AstNode, VariableTreeImpl> identifier : identifiers.second().get()) {
          params.add(identifier.second());

          children.add(identifier.first());
          children.add(identifier.second());
        }
      }
    }

    children.add(closeParenToken);

    return new LambdaParameterListTreeImpl(openParenToken, params.build(), closeParenToken, children);
  }

  public LambdaParameterListTreeImpl formalLambdaParameters(FormalParametersListTreeImpl formalParameters) {
    return new LambdaParameterListTreeImpl(formalParameters.openParenToken(), formalParameters, formalParameters.closeParenToken(), formalParameters.getChildren());
  }

  public LambdaParameterListTreeImpl singleInferedParameter(VariableTreeImpl parameter) {
    return new LambdaParameterListTreeImpl(null, ImmutableList.of(parameter), null, ImmutableList.<AstNode>of(parameter));
  }

  public VariableTreeImpl newSimpleParameter(AstNode identifierAstNode) {
    IdentifierTreeImpl identifier = new IdentifierTreeImpl(InternalSyntaxToken.create(identifierAstNode));
    return new VariableTreeImpl(identifier);
  }

  public ParenthesizedTreeImpl parenthesizedExpression(AstNode leftParenthesisToken, ExpressionTree expression, AstNode rightParenthesisToken) {
    return new ParenthesizedTreeImpl(expression,
      leftParenthesisToken, (AstNode) expression, rightParenthesisToken);
  }

  public ExpressionTree completeExplicityGenericInvocation(AstNode nonWildcardTypeArguments, ExpressionTree partial) {
    ((JavaTree) partial).prependChildren(nonWildcardTypeArguments);
    return partial;
  }

  public ExpressionTree newExplicitGenericInvokation(AstNode explicitGenericInvocationSuffix) {
    if (explicitGenericInvocationSuffix.hasDirectChildren(JavaGrammar.SUPER_SUFFIX)) {
      // <T>super...
      AstNode superSuffix = explicitGenericInvocationSuffix.getFirstChild(JavaGrammar.SUPER_SUFFIX);
      return applySuperSuffix(
        new IdentifierTreeImpl(InternalSyntaxToken.create(superSuffix.getFirstChild(JavaKeyword.SUPER))),
        superSuffix);
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

  public ExpressionTree superExpression(AstNode superSuffix) {
    return applySuperSuffix(new IdentifierTreeImpl(InternalSyntaxToken.create(superSuffix.getFirstChild(JavaKeyword.SUPER))), superSuffix);
  }

  public ExpressionTree newExpression(AstNode newToken, Optional<List<AnnotationTreeImpl>> annotations, ExpressionTree partial) {
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
      classBody = new ClassTreeImpl(classCreatorRest, Tree.Kind.CLASS, ModifiersTreeImpl.EMPTY, null, ImmutableList.<TypeParameterTree>of(), null, ImmutableList.<Tree>of(), body);
    }
    return new NewClassTreeImpl(null, treeMaker.classType(createdName), (ArgumentListTreeImpl) classCreatorRest.getFirstChild(JavaGrammar.ARGUMENTS), classBody,
      createdName, classCreatorRest);
  }

  public ExpressionTree newArrayCreator(Tree type, NewArrayTreeImpl partial) {
    return partial.complete(type,
      (AstNode) type);
  }

  public NewArrayTreeImpl completeArrayCreator(Optional<List<AnnotationTreeImpl>> annotations, NewArrayTreeImpl partial) {
    if (annotations.isPresent()) {
      partial.prependChildren(annotations.get());
    }
    return partial;
  }

  public NewArrayTreeImpl newArrayCreatorWithInitializer(AstNode openBracketToken, AstNode closeBracketToken, Optional<List<AstNode>> dims, NewArrayTreeImpl partial) {
    List<AstNode> children = Lists.newArrayList();
    children.add(openBracketToken);
    children.add(closeBracketToken);
    if (dims.isPresent()) {
      children.addAll(dims.get());
    }

    partial.prependChildren(children);

    return partial;
  }

  public NewArrayTreeImpl newArrayCreatorWithDimension(AstNode openBracketToken, ExpressionTree expression, AstNode closeBracketToken,
    Optional<List<ArrayAccessExpressionTreeImpl>> arrayAccesses,
    Optional<List<AstNode>> dims) {

    ImmutableList.Builder<ExpressionTree> dimensions = ImmutableList.builder();
    dimensions.add(expression);
    if (arrayAccesses.isPresent()) {
      for (ArrayAccessExpressionTreeImpl arrayAccess : arrayAccesses.get()) {
        dimensions.add(arrayAccess.index());
      }
    }

    List<AstNode> children = Lists.newArrayList();
    children.add(openBracketToken);
    children.add((AstNode) expression);
    children.add(closeBracketToken);
    if (arrayAccesses.isPresent()) {
      children.addAll(arrayAccesses.get());
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
          // TODO Replace by DIM_EXPR aka ARRAY_ACCESS_EXPRESSION()
          InternalSyntaxToken openBracketToken = InternalSyntaxToken.create(identifierSuffixNode.getFirstChild(JavaPunctuator.LBRK));
          InternalSyntaxToken closeBracketToken = InternalSyntaxToken.create(identifierSuffixNode.getFirstChild(JavaPunctuator.RBRK));

          return new ArrayAccessExpressionTreeImpl(
            qualifiedIdentifier,
            openBracketToken, (ExpressionTree) identifierSuffixNode.getChild(identifierSuffixNode.getNumberOfChildren() - 2), closeBracketToken);
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

  public PrimitiveTypeTreeImpl newBasicType(Optional<List<AnnotationTreeImpl>> annotations, AstNode basicType) {
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

  public ArgumentListTreeImpl newArguments(ExpressionTree expression, Optional<List<AstNode>> rests) {
    List<AstNode> children = Lists.newArrayList();
    ImmutableList.Builder<ExpressionTree> expressions = ImmutableList.builder();

    children.add((AstNode) expression);
    expressions.add(expression);

    if (rests.isPresent()) {
      for (AstNode rest : rests.get()) {
        children.addAll(rest.getChildren());
        expressions.add((ExpressionTree) rest.getLastChild());
      }
    }

    return new ArgumentListTreeImpl(expressions.build(), children);
  }

  public ExpressionTree qualifiedIdentifier(Optional<List<AnnotationTreeImpl>> annotations, AstNode firstIdentifier, Optional<List<AstNode>> rests) {
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

  public NewArrayTreeImpl newArrayInitializer(AstNode openBraceTokenAstNode, Optional<List<AstNode>> rests, AstNode closeBraceTokenAstNode) {
    ImmutableList.Builder<ExpressionTree> initializers = ImmutableList.builder();
    List<AstNode> children = Lists.newArrayList();

    children.add(openBraceTokenAstNode);
    if (rests.isPresent()) {
      for (AstNode rest : rests.get()) {
        initializers.add((ExpressionTree) rest.getFirstChild());
        children.add(rest.getFirstChild());

        if (rest.getNumberOfChildren() == 2) {
          children.add(rest.getLastChild());
        }
      }
    }
    children.add(closeBraceTokenAstNode);

    return new NewArrayTreeImpl(ImmutableList.<ExpressionTree>of(), initializers.build(), children);
  }

  public ArrayAccessExpressionTreeImpl newArrayAccessExpression(Optional<List<AnnotationTreeImpl>> annotations, AstNode openBracketTokenAstNode, ExpressionTree index,
    AstNode closeBracketTokenAstNode) {
    InternalSyntaxToken openBracketToken = InternalSyntaxToken.create(openBracketTokenAstNode);
    InternalSyntaxToken closeBracketToken = InternalSyntaxToken.create(closeBracketTokenAstNode);

    ArrayAccessExpressionTreeImpl result = new ArrayAccessExpressionTreeImpl(openBracketToken, index, closeBracketToken);
    if (annotations.isPresent()) {
      result.prependChildren(annotations.get());
    }

    return result;
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

  public AstNode newWrapperAstNode(AstNode e1, Optional<AstNode> e2) {
    AstNode astNode = new AstNode(WRAPPER_AST_NODE, WRAPPER_AST_NODE.toString(), null);
    astNode.addChild(e1);
    if (e2.isPresent()) {
      astNode.addChild(e2.get());
    }
    return astNode;
  }

  // TODO Enable the same method call multiple times

  public AstNode newWrapperAstNode2(AstNode e1, AstNode e2) {
    return newWrapperAstNode(e1, e2);
  }

  public AstNode newWrapperAstNode3(AstNode e1, AstNode e2) {
    return newWrapperAstNode(e1, e2);
  }

  public AstNode newWrapperAstNode4(AstNode e1, AstNode e2) {
    return newWrapperAstNode(e1, e2);
  }

  public AstNode newWrapperAstNode5(Optional<List<AstNode>> e1, AstNode e2) {
    return newWrapperAstNode(e1, e2);
  }

  public AstNode newWrapperAstNode6(AstNode e1, AstNode e2) {
    return newWrapperAstNode(e1, e2);
  }

  public AstNode newWrapperAstNode7(AstNode e1, AstNode e2) {
    return newWrapperAstNode(e1, e2);
  }

  public AstNode newWrapperAstNode8(AstNode e1, AstNode e2) {
    return newWrapperAstNode(e1, e2);
  }

  public AstNode newWrapperAstNode9(AstNode e1, AstNode e2) {
    return newWrapperAstNode(e1, e2);
  }

  public AstNode newWrapperAstNode10(AstNode e1, AstNode e2) {
    return newWrapperAstNode(e1, e2);
  }

  public AstNode newWrapperAstNode11(Optional<List<AstNode>> e1, AstNode e2) {
    return newWrapperAstNode(e1, e2);
  }

  public AstNode newWrapperAstNode12(AstNode e1, AstNode e2) {
    return newWrapperAstNode(e1, e2);
  }

  public AstNode newWrapperAstNode13(AstNode e1, AstNode e2) {
    return newWrapperAstNode(e1, e2);
  }

  public AstNode newWrapperAstNode14(AstNode e1, Optional<AstNode> e2) {
    return newWrapperAstNode(e1, e2);
  }

  public AstNode newWrapperAstNode15(AstNode e1, Optional<AstNode> e2) {
    return newWrapperAstNode(e1, e2);
  }

  public static class Tuple<T, U> extends AstNode {

    private final T first;
    private final U second;

    public Tuple(T first, U second) {
      super(WRAPPER_AST_NODE, WRAPPER_AST_NODE.toString(), null);

      this.first = first;
      this.second = second;

      add(first);
      add(second);
    }

    public T first() {
      return first;
    }

    public U second() {
      return second;
    }

    private void add(Object o) {
      if (o instanceof AstNode) {
        addChild((AstNode) o);
      } else if (o instanceof Optional) {
        Optional opt = (Optional) o;
        if (opt.isPresent()) {
          Object o2 = opt.get();
          Preconditions.checkArgument(o2 instanceof AstNode, "Unsupported optional type: " + o2.getClass().getSimpleName());
          addChild((AstNode) o2);
        }
      } else {
        throw new IllegalStateException("Unsupported argument type: " + o.getClass().getSimpleName());
      }
    }

  }

  private <T, U> Tuple<T, U> newTuple(T first, U second) {
    return new Tuple<T, U>(first, second);
  }

  public <T, U> Tuple<T, U> newTuple1(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple2(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple3(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple4(T first, U second) {
    return newTuple(first, second);
  }

  // Crappy methods which must go away

  private ExpressionTree applySuperSuffix(ExpressionTree expression, AstNode superSuffixNode) {
    Preconditions.checkArgument(!((JavaTree) expression).isLegacy());
    JavaTreeMaker.checkType(superSuffixNode, JavaGrammar.SUPER_SUFFIX);

    List<AstNode> children = Lists.newArrayList();
    boolean first = true;
    for (AstNode child : superSuffixNode.getChildren()) {
      if (!first) {
        children.add(child);
      }
      first = false;
    }

    if (superSuffixNode.hasDirectChildren(JavaGrammar.ARGUMENTS)) {
      // super(arguments)
      // super.method(arguments)
      // super.<T>method(arguments)
      // TODO typeArguments
      if (superSuffixNode.hasDirectChildren(JavaTokenType.IDENTIFIER)) {
        MemberSelectExpressionTreeImpl memberSelect = new MemberSelectExpressionTreeImpl(
          expression, treeMaker.identifier(superSuffixNode.getFirstChild(JavaTokenType.IDENTIFIER)),
          (AstNode) expression);

        children.add(0, memberSelect);

        return new MethodInvocationTreeImpl(memberSelect, (ArgumentListTreeImpl) superSuffixNode.getFirstChild(JavaGrammar.ARGUMENTS),
          children.toArray(new AstNode[0]));
      } else {
        children.add(0, (AstNode) expression);
        return new MethodInvocationTreeImpl(expression, (ArgumentListTreeImpl) superSuffixNode.getFirstChild(JavaGrammar.ARGUMENTS),
          children.toArray(new AstNode[0]));
      }
    } else {
      // super.field
      children.add(0, (AstNode) expression);
      return new MemberSelectExpressionTreeImpl(expression, treeMaker.identifier(superSuffixNode.getFirstChild(JavaTokenType.IDENTIFIER)),
        children.toArray(new AstNode[0]));
    }
  }

  public ExpressionTree applyExplicitGenericInvocation(ExpressionTree expression, AstNode dotToken, AstNode astNode) {
    Preconditions.checkArgument(!((JavaTree) expression).isLegacy());
    JavaTreeMaker.checkType(astNode, JavaGrammar.EXPLICIT_GENERIC_INVOCATION);
    // TODO NON_WILDCARD_TYPE_ARGUMENTS
    AstNode nonWildcardTypeArguments = astNode.getFirstChild(JavaGrammar.NON_WILDCARD_TYPE_ARGUMENTS);

    AstNode explicitGenericInvocationSuffixNode = astNode.getFirstChild(JavaGrammar.EXPLICIT_GENERIC_INVOCATION_SUFFIX);
    if (explicitGenericInvocationSuffixNode.hasDirectChildren(JavaGrammar.SUPER_SUFFIX)) {
      AstNode superSuffix = explicitGenericInvocationSuffixNode.getFirstChild(JavaGrammar.SUPER_SUFFIX);
      IdentifierTreeImpl superIdentifier = new IdentifierTreeImpl(InternalSyntaxToken.create(superSuffix.getFirstChild(JavaKeyword.SUPER)));
      expression = new MemberSelectExpressionTreeImpl(expression, superIdentifier,
        (AstNode) expression, dotToken, nonWildcardTypeArguments, superIdentifier);
      return applySuperSuffix(expression, superSuffix);
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

    AstNodeType[] identifierTypes = new AstNodeType[] {JavaTokenType.IDENTIFIER, JavaKeyword.THIS};
    AstNode identifierAstNode = selectorNode.getFirstChild(identifierTypes);
    if (identifierAstNode == null && selectorNode.hasDirectChildren(JavaGrammar.SUPER_SUFFIX)) {
      identifierAstNode = selectorNode.getFirstChild(JavaGrammar.SUPER_SUFFIX).getFirstChild(JavaKeyword.SUPER);
    }

    if (identifierAstNode != null) {
      InternalSyntaxToken identifierToken = InternalSyntaxToken.create(identifierAstNode);
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
    } else if (selectorNode.hasDirectChildren(Kind.ARRAY_ACCESS_EXPRESSION)) {
      return ((ArrayAccessExpressionTreeImpl) selectorNode.getFirstChild(Kind.ARRAY_ACCESS_EXPRESSION)).complete(expression);
    } else {
      throw new IllegalStateException(AstXmlPrinter.print(selectorNode));
    }
  }

  public QualifiedIdentifierListTreeImpl newQualifiedIdentifierList(ExpressionTree qualifiedIdentifier, Optional<List<Tuple<AstNode, ExpressionTree>>> rests) {
    ImmutableList.Builder<ExpressionTree> qualifiedIdentifiers = ImmutableList.builder();
    List<AstNode> children = Lists.newArrayList();

    qualifiedIdentifiers.add(qualifiedIdentifier);
    children.add((AstNode) qualifiedIdentifier);

    if (rests.isPresent()) {
      for (Tuple<AstNode, ExpressionTree> rest : rests.get()) {
        qualifiedIdentifiers.add(rest.second());
        children.add(rest.first());
        children.add((AstNode) rest.second());
      }
    }

    return new QualifiedIdentifierListTreeImpl(qualifiedIdentifiers.build(), children);
  }

}
