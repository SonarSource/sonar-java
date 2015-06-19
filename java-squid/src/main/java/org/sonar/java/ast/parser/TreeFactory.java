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
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.JavaTree.ArrayTypeTreeImpl;
import org.sonar.java.model.JavaTree.CompilationUnitTreeImpl;
import org.sonar.java.model.JavaTree.ImportTreeImpl;
import org.sonar.java.model.JavaTree.PackageDeclarationTreeImpl;
import org.sonar.java.model.JavaTree.ParameterizedTypeTreeImpl;
import org.sonar.java.model.JavaTree.PrimitiveTypeTreeImpl;
import org.sonar.java.model.JavaTree.UnionTypeTreeImpl;
import org.sonar.java.model.JavaTree.WildcardTreeImpl;
import org.sonar.java.model.KindMaps;
import org.sonar.java.model.TypeParameterTreeImpl;
import org.sonar.java.model.declaration.AnnotationTreeImpl;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.EnumConstantTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.ModifierKeywordTreeImpl;
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
import org.sonar.java.model.expression.MethodReferenceTreeImpl;
import org.sonar.java.model.expression.NewArrayTreeImpl;
import org.sonar.java.model.expression.NewClassTreeImpl;
import org.sonar.java.model.expression.ParenthesizedTreeImpl;
import org.sonar.java.model.expression.TypeArgumentListTreeImpl;
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
import org.sonar.java.model.statement.StaticInitializerTreeImpl;
import org.sonar.java.model.statement.SwitchStatementTreeImpl;
import org.sonar.java.model.statement.SynchronizedStatementTreeImpl;
import org.sonar.java.model.statement.ThrowStatementTreeImpl;
import org.sonar.java.model.statement.TryStatementTreeImpl;
import org.sonar.java.model.statement.WhileStatementTreeImpl;
import org.sonar.java.parser.sslr.Optional;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportClauseTree;
import org.sonar.plugins.java.api.tree.ModifierTree;
import org.sonar.plugins.java.api.tree.PackageDeclarationTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.TypeArguments;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Collections;
import java.util.List;

public class TreeFactory {

  private final KindMaps kindMaps = new KindMaps();

  public ModifiersTreeImpl modifiers(Optional<List<ModifierTree>> modifierNodes) {
    if (!modifierNodes.isPresent()) {
      return ModifiersTreeImpl.emptyModifiers();
    }
    return new ModifiersTreeImpl(modifierNodes.get());
  }

  public ModifierKeywordTreeImpl modifierKeyword(AstNode astNode) {
    JavaKeyword keyword = (JavaKeyword) astNode.getType();
    return new ModifierKeywordTreeImpl(kindMaps.getModifier(keyword), astNode);
  }

  // Literals

  public ExpressionTree literal(AstNode astNode) {
    InternalSyntaxToken token = InternalSyntaxToken.create(astNode);
    return new LiteralTreeImpl(kindMaps.getLiteral(astNode.getType()), token);
  }

  // End of literals

  // Compilation unit

  public CompilationUnitTreeImpl newCompilationUnit(
    AstNode spacing,
    Optional<PackageDeclarationTree> packageDeclaration,
    Optional<List<ImportClauseTree>> importDeclarations,
    Optional<List<Tree>> typeDeclarations,
    AstNode eof) {

    List<AstNode> children = Lists.newArrayList();
    children.add(spacing);

    if (packageDeclaration.isPresent()) {
      children.add((AstNode) packageDeclaration.get());
    }

    ImmutableList.Builder<ImportClauseTree> imports = ImmutableList.builder();
    if (importDeclarations.isPresent()) {
      for (ImportClauseTree child : importDeclarations.get()) {
        children.add((AstNode) child);

        if (!child.is(Kind.EMPTY_STATEMENT)) {
          imports.add((ImportTreeImpl) child);
        } else {
          imports.add((EmptyStatementTreeImpl) child);
        }
      }
    }

    ImmutableList.Builder<Tree> types = ImmutableList.builder();
    if (typeDeclarations.isPresent()) {
      for (Tree child : typeDeclarations.get()) {
        children.add((AstNode) child);
        types.add(child);
      }
    }

    InternalSyntaxToken eofToken = InternalSyntaxToken.create(eof);
    children.add(eofToken);

    return new CompilationUnitTreeImpl(
      packageDeclaration.orNull(),
      imports.build(),
      types.build(),
      eofToken,
      children);
  }

  public PackageDeclarationTree newPackageDeclaration(Optional<List<AnnotationTreeImpl>> annotations, AstNode packageTokenAstNode, ExpressionTree qualifiedIdentifier,
    AstNode semicolonTokenAstNode) {

    List<AnnotationTree> annotationList = Collections.emptyList();
    if (annotations.isPresent()) {
      annotationList = ImmutableList.<AnnotationTree>builder().addAll(annotations.get()).build();
    }
    InternalSyntaxToken packageToken = InternalSyntaxToken.create(packageTokenAstNode);
    InternalSyntaxToken semicolonToken = InternalSyntaxToken.create(semicolonTokenAstNode);

    return new PackageDeclarationTreeImpl(annotationList, packageToken, qualifiedIdentifier, semicolonToken);
  }

  public ImportClauseTree newEmptyImport(AstNode semicolonTokenAstNode) {
    return new EmptyStatementTreeImpl(InternalSyntaxToken.create(semicolonTokenAstNode));
  }

  public ImportTreeImpl newImportDeclaration(AstNode importTokenAstNode, Optional<AstNode> staticTokenAstNode, ExpressionTree qualifiedIdentifier,
    Optional<Tuple<AstNode, AstNode>> dotStar,
    AstNode semicolonTokenAstNode) {

    ExpressionTree target = qualifiedIdentifier;
    if (dotStar.isPresent()) {
      IdentifierTreeImpl identifier = new IdentifierTreeImpl(InternalSyntaxToken.create(dotStar.get().second()));
      InternalSyntaxToken dotToken = InternalSyntaxToken.create(dotStar.get().first());
      target = new MemberSelectExpressionTreeImpl(qualifiedIdentifier, dotToken, identifier,
        (AstNode) qualifiedIdentifier, dotStar.get().first(), identifier);
    }

    InternalSyntaxToken importToken = InternalSyntaxToken.create(importTokenAstNode);
    InternalSyntaxToken staticToken = null;
    if (staticTokenAstNode.isPresent()) {
      staticToken = InternalSyntaxToken.create(staticTokenAstNode.get());
    }
    InternalSyntaxToken semiColonToken = InternalSyntaxToken.create(semicolonTokenAstNode);
    return new ImportTreeImpl(importToken, staticToken, target, semiColonToken);
  }

  public ClassTreeImpl newTypeDeclaration(ModifiersTreeImpl modifiers, ClassTreeImpl partial) {
    partial.prependChildren(modifiers);
    return partial.completeModifiers(modifiers);
  }

  public Tree newEmptyType(AstNode semicolonTokenAstNode) {
    return new EmptyStatementTreeImpl(InternalSyntaxToken.create(semicolonTokenAstNode));
  }

  // End of compilation unit

  // Types

  public TypeTree newType(TypeTree basicOrClassType,
    Optional<List<Tuple<Optional<List<AnnotationTreeImpl>>, Tuple<AstNode, AstNode>>>> dims) {
    if (!dims.isPresent()) {
      return basicOrClassType;
    } else {
      TypeTree result = basicOrClassType;

      for (Tuple<Optional<List<AnnotationTreeImpl>>, Tuple<AstNode, AstNode>> dim : dims.get()) {
        result = newArrayTypeTreeWithAnnotations(result, dim);
      }

      return result;
    }
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
          // FIXME SONARJAVA-547 comma should be part of the ArgumentList as token
          if (!child.is(JavaPunctuator.COMMA)) {
            typeArguments.add((Tree) child);
          }

          children.add(child);
        }
      }
    }

    return new TypeArgumentListTreeImpl(openBracketToken, typeArguments.build(), closeBracketToken, children);
  }

  public TypeArgumentListTreeImpl newDiamondTypeArgument(AstNode openBracketTokenAstNode, AstNode closeBracketTokenAstNode) {
    InternalSyntaxToken openBracketToken = InternalSyntaxToken.create(openBracketTokenAstNode);
    InternalSyntaxToken closeBracketToken = InternalSyntaxToken.create(closeBracketTokenAstNode);

    return new TypeArgumentListTreeImpl(openBracketToken, ImmutableList.<Tree>of(), closeBracketToken, ImmutableList.<AstNode>of());
  }

  public Tree completeTypeArgument(Optional<List<AnnotationTreeImpl>> annotations, Tree partial) {
    if (annotations.isPresent()) {
      ((JavaTree) partial).prependChildren(annotations.get());
    }

    return partial;
  }

  public TypeTree newBasicTypeArgument(TypeTree type) {
    return type;
  }

  public WildcardTreeImpl completeWildcardTypeArgument(AstNode queryTokenAstNode, Optional<WildcardTreeImpl> partial) {
    InternalSyntaxToken queryToken = InternalSyntaxToken.create(queryTokenAstNode);

    return partial.isPresent() ?
      partial.get().complete(queryToken) :
      new WildcardTreeImpl(Kind.UNBOUNDED_WILDCARD, queryToken);
  }

  public WildcardTreeImpl newWildcardTypeArguments(AstNode extendsOrSuperTokenAstNode, Optional<List<AnnotationTreeImpl>> annotations, TypeTree type) {
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

    ImmutableList.Builder<TypeParameterTree> typeParameters = ImmutableList.builder();
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

  public BoundListTreeImpl newBounds(TypeTree classType, Optional<List<AstNode>> rests) {
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

  // Classes, enums and interfaces

  public ClassTreeImpl completeClassDeclaration(
    AstNode classTokenAstNode,
    AstNode identifierAstNode, Optional<TypeParameterListTreeImpl> typeParameters,
    Optional<Tuple<AstNode, TypeTree>> extendsClause,
    Optional<Tuple<AstNode, QualifiedIdentifierListTreeImpl>> implementsClause,
    ClassTreeImpl partial) {

    IdentifierTreeImpl identifier = new IdentifierTreeImpl(InternalSyntaxToken.create(identifierAstNode));

    List<AstNode> children = Lists.newArrayList();
    InternalSyntaxToken classSyntaxToken = InternalSyntaxToken.create(classTokenAstNode);
    children.add(classSyntaxToken);
    partial.completeDeclarationKeyword(classSyntaxToken);
    children.add(identifier);
    partial.completeIdentifier(identifier);
    if (typeParameters.isPresent()) {
      children.add(typeParameters.get());
      partial.completeTypeParameters(typeParameters.get());
    }
    if (extendsClause.isPresent()) {
      InternalSyntaxToken extendsKeyword = InternalSyntaxToken.create(extendsClause.get().first());
      TypeTree superClass = extendsClause.get().second();
      children.add(extendsKeyword);
      children.add((AstNode) superClass);
      partial.completeSuperclass(extendsKeyword, superClass);
    }
    if (implementsClause.isPresent()) {
      InternalSyntaxToken implementsKeyword = InternalSyntaxToken.create(implementsClause.get().first());
      QualifiedIdentifierListTreeImpl interfaces = implementsClause.get().second();
      children.add(implementsKeyword);
      children.add(interfaces);
      partial.completeInterfaces(implementsKeyword, interfaces);
    }

    partial.prependChildren(children);

    return partial;
  }

  private static ClassTreeImpl newClassBody(Kind kind, AstNode openBraceTokenAstNode, Optional<List<AstNode>> members, AstNode closeBraceTokenAstNode) {
    List<AstNode> children = Lists.newArrayList();
    ImmutableList.Builder<Tree> builder = ImmutableList.builder();

    InternalSyntaxToken openBraceSyntaxToken = InternalSyntaxToken.create(openBraceTokenAstNode);
    children.add(openBraceSyntaxToken);
    if (members.isPresent()) {
      for (AstNode member : members.get()) {
        children.add(member);

        if (member instanceof VariableDeclaratorListTreeImpl) {
          for (VariableTreeImpl variable : (VariableDeclaratorListTreeImpl) member) {
            builder.add(variable);
          }
        } else if (member instanceof Tree) {
          builder.add((Tree) member);
        }
      }
    }
    InternalSyntaxToken closeBraceTokenSyntaxToken = InternalSyntaxToken.create(closeBraceTokenAstNode);
    children.add(closeBraceTokenSyntaxToken);

    return new ClassTreeImpl(kind, openBraceSyntaxToken, builder.build(), closeBraceTokenSyntaxToken, children);
  }

  public ClassTreeImpl newClassBody(AstNode openBraceTokenAstNode, Optional<List<AstNode>> members, AstNode closeBraceTokenAstNode) {
    return newClassBody(Kind.CLASS, openBraceTokenAstNode, members, closeBraceTokenAstNode);
  }

  public ClassTreeImpl newEnumDeclaration(
    AstNode enumTokenAstNode,
    AstNode identifierAstNode,
    Optional<Tuple<AstNode, QualifiedIdentifierListTreeImpl>> implementsClause,
    AstNode openBraceTokenAstNode,
    Optional<List<EnumConstantTreeImpl>> enumConstants,
    Optional<AstNode> semicolonTokenAstNode,
    Optional<List<AstNode>> enumDeclarations,
    AstNode closeBraceTokenAstNode) {

    ImmutableList.Builder<AstNode> members = ImmutableList.builder();
    if (enumConstants.isPresent()) {
      for (EnumConstantTreeImpl enumConstant : enumConstants.get()) {
        members.add(enumConstant);
      }
    }
    if (semicolonTokenAstNode.isPresent()) {
      // TODO This is a hack
      members.add(semicolonTokenAstNode.get());
    }
    if (enumDeclarations.isPresent()) {
      for (AstNode enumDeclaration : enumDeclarations.get()) {
        members.add(enumDeclaration);
      }
    }

    ClassTreeImpl result = newClassBody(Kind.ENUM, openBraceTokenAstNode, Optional.of((List<AstNode>) members.build()), closeBraceTokenAstNode);

    List<AstNode> children = Lists.newArrayList();
    children.add(enumTokenAstNode);
    result.completeDeclarationKeyword(InternalSyntaxToken.create(enumTokenAstNode));

    IdentifierTreeImpl identifier = new IdentifierTreeImpl(InternalSyntaxToken.create(identifierAstNode));
    result.completeIdentifier(identifier);
    children.add(identifier);

    if (implementsClause.isPresent()) {
      InternalSyntaxToken implementsKeyword = InternalSyntaxToken.create(implementsClause.get().first());
      QualifiedIdentifierListTreeImpl interfaces = implementsClause.get().second();
      children.add(implementsKeyword);
      children.add(interfaces);
      result.completeInterfaces(implementsKeyword, interfaces);
    }

    result.prependChildren(children);

    return result;
  }

  public EnumConstantTreeImpl newEnumConstant(
    Optional<List<AnnotationTreeImpl>> annotations, AstNode identifierAstNode,
    Optional<ArgumentListTreeImpl> arguments,
    Optional<ClassTreeImpl> classBody,
    Optional<AstNode> semicolonTokenAstNode) {

    IdentifierTreeImpl identifier = new IdentifierTreeImpl(InternalSyntaxToken.create(identifierAstNode));
    if (annotations.isPresent()) {
      identifier.prependChildren(annotations.get());
    }

    List<AstNode> children = Lists.newArrayList();
    SyntaxToken openParenToken = null;
    SyntaxToken closeParenToken = null;
    List argumentsList = Collections.emptyList();
    if (arguments.isPresent()) {
      ArgumentListTreeImpl argumentsListTreeImpl = arguments.get();
      argumentsList = argumentsListTreeImpl;
      openParenToken = argumentsListTreeImpl.openParenToken();
      closeParenToken = argumentsListTreeImpl.closeParenToken();
      children.add(argumentsListTreeImpl);
    }

    if (classBody.isPresent()) {
      children.add(classBody.get());
    }

    NewClassTreeImpl newClass = new NewClassTreeImpl(
      openParenToken,
      argumentsList,
      closeParenToken,
      classBody.isPresent() ? classBody.get() : null,
      children.toArray(new AstNode[0]));
    newClass.completeWithIdentifier(identifier);

    @SuppressWarnings("unchecked")
    EnumConstantTreeImpl result = new EnumConstantTreeImpl(modifiers((Optional<List<ModifierTree>>) (Optional<?>) annotations), identifier, newClass);

    result.addChild(identifier);
    result.addChild(newClass);
    if (semicolonTokenAstNode.isPresent()) {
      result.addChild(semicolonTokenAstNode.get());
    }

    return result;
  }

  public ClassTreeImpl completeInterfaceDeclaration(
    AstNode interfaceTokenAstNode,
    AstNode identifierAstNode, Optional<TypeParameterListTreeImpl> typeParameters,
    Optional<Tuple<AstNode, QualifiedIdentifierListTreeImpl>> extendsClause,
    ClassTreeImpl partial) {

    IdentifierTreeImpl identifier = new IdentifierTreeImpl(InternalSyntaxToken.create(identifierAstNode));

    List<AstNode> children = Lists.newArrayList();
    InternalSyntaxToken interfaceSyntaxToken = InternalSyntaxToken.create(interfaceTokenAstNode);
    children.add(interfaceSyntaxToken);
    partial.completeDeclarationKeyword(interfaceSyntaxToken);

    children.add(identifier);
    partial.completeIdentifier(identifier);
    if (typeParameters.isPresent()) {
      children.add(typeParameters.get());
      partial.completeTypeParameters(typeParameters.get());
    }
    if (extendsClause.isPresent()) {
      InternalSyntaxToken extendsKeyword = InternalSyntaxToken.create(extendsClause.get().first());
      QualifiedIdentifierListTreeImpl interfaces = extendsClause.get().second();
      children.add(extendsKeyword);
      children.add(interfaces);
      partial.compleInterfacesForInterface(extendsKeyword, interfaces);
    }

    partial.prependChildren(children);

    return partial;
  }

  public ClassTreeImpl newInterfaceBody(AstNode openBraceTokenAstNode, Optional<List<AstNode>> members, AstNode closeBraceTokenAstNode) {
    return newClassBody(Kind.INTERFACE, openBraceTokenAstNode, members, closeBraceTokenAstNode);
  }

  // TODO Create an intermediate implementation interface for completing modifiers
  public AstNode completeMember(ModifiersTreeImpl modifiers, JavaTree partial) {

    if (partial instanceof ClassTreeImpl) {
      ((ClassTreeImpl) partial).completeModifiers(modifiers);
      partial.prependChildren(modifiers);
    } else if (partial instanceof VariableDeclaratorListTreeImpl) {
      for (VariableTreeImpl variable : (VariableDeclaratorListTreeImpl) partial) {
        variable.completeModifiers(modifiers);
      }
      partial.prependChildren(modifiers);
    } else if (partial instanceof MethodTreeImpl) {
      ((MethodTreeImpl) partial).completeWithModifiers(modifiers);
    } else {
      throw new IllegalArgumentException();
    }

    return partial;
  }

  public BlockTreeImpl newInitializerMember(Optional<AstNode> staticTokenAstNode, BlockTreeImpl block) {
    BlockTreeImpl blockTree;
    List<AstNode> children = Lists.newArrayList();

    if (staticTokenAstNode.isPresent()) {
      InternalSyntaxToken staticKeyword = InternalSyntaxToken.create(staticTokenAstNode.get());
      children.add(staticKeyword);
      children.addAll(block.getChildren());
      blockTree = new StaticInitializerTreeImpl(staticKeyword, (InternalSyntaxToken) block.openBraceToken(), block.body(), (InternalSyntaxToken) block.closeBraceToken(),
        children.toArray(new AstNode[0]));
    } else {
      children.addAll(block.getChildren());
      blockTree = new BlockTreeImpl(Kind.INITIALIZER, (InternalSyntaxToken) block.openBraceToken(), block.body(), (InternalSyntaxToken) block.closeBraceToken(),
        children.toArray(new AstNode[0]));
    }

    return blockTree;

  }

  public AstNode newEmptyMember(AstNode semicolonTokenAstNode) {
    return new EmptyStatementTreeImpl(InternalSyntaxToken.create(semicolonTokenAstNode));
  }

  public MethodTreeImpl completeGenericMethodOrConstructorDeclaration(TypeParameterListTreeImpl typeParameters, MethodTreeImpl partial) {
    partial.prependChildren((AstNode) typeParameters);

    return partial.completeWithTypeParameters(typeParameters);
  }

  private MethodTreeImpl newMethodOrConstructor(
    Optional<TypeTree> type, AstNode identifierAstNode, FormalParametersListTreeImpl parameters,
    Optional<List<Tuple<Optional<List<AnnotationTreeImpl>>, Tuple<AstNode, AstNode>>>> annotatedDimensions,
    Optional<Tuple<AstNode, QualifiedIdentifierListTreeImpl>> throwsClause,
    AstNode blockOrSemicolon) {

    IdentifierTreeImpl identifier = new IdentifierTreeImpl(InternalSyntaxToken.create(identifierAstNode));

    ArrayTypeTreeImpl nestedDimensions = newArrayTypeTreeWithAnnotations(annotatedDimensions);
    TypeTree actualType;
    if (type.isPresent()) {
      actualType = applyDim(type.get(), nestedDimensions);
    } else {
      actualType = null;
    }
    BlockTreeImpl block = null;
    InternalSyntaxToken semicolonToken = null;
    if (blockOrSemicolon.is(Tree.Kind.BLOCK)) {
      block = (BlockTreeImpl) blockOrSemicolon;
    } else {
      semicolonToken = InternalSyntaxToken.create(blockOrSemicolon);
    }

    InternalSyntaxToken throwsToken = null;
    List<TypeTree> throwsClauses = ImmutableList.<TypeTree>of();
    if (throwsClause.isPresent()) {
      throwsToken = InternalSyntaxToken.create(throwsClause.get().first());
      throwsClauses = throwsClause.get().second();
    }

    MethodTreeImpl result = new MethodTreeImpl(
      actualType,
      identifier,
      parameters,
      throwsToken,
      throwsClauses,
      block,
      semicolonToken);

    List<AstNode> children = Lists.newArrayList();
    if (type.isPresent()) {
      children.add((AstNode) type.get());
    }
    children.add(identifier);
    children.add(parameters);
    if (nestedDimensions != null) {
      children.add(nestedDimensions);
    }
    if (throwsClause.isPresent()) {
      children.add(throwsClause.get().first());
      children.add(throwsClause.get().second());
    }
    if (block != null) {
      children.add(block);
    } else {
      children.add(semicolonToken);
    }

    result.prependChildren(children);

    return result;
  }

  public MethodTreeImpl newMethod(
    TypeTree type, AstNode identifierAstNode, FormalParametersListTreeImpl parameters,
    Optional<List<Tuple<Optional<List<AnnotationTreeImpl>>, Tuple<AstNode, AstNode>>>> annotatedDimensions,
    Optional<Tuple<AstNode, QualifiedIdentifierListTreeImpl>> throwsClause,
    AstNode blockOrSemicolon) {

    return newMethodOrConstructor(Optional.of(type), identifierAstNode, parameters, annotatedDimensions, throwsClause, blockOrSemicolon);
  }

  public MethodTreeImpl newConstructor(
    AstNode identifierAstNode, FormalParametersListTreeImpl parameters,
    Optional<List<Tuple<Optional<List<AnnotationTreeImpl>>, Tuple<AstNode, AstNode>>>> annotatedDimensions,
    Optional<Tuple<AstNode, QualifiedIdentifierListTreeImpl>> throwsClause,
    AstNode blockOrSemicolon) {

    return newMethodOrConstructor(Optional.<TypeTree>absent(), identifierAstNode, parameters, annotatedDimensions, throwsClause, blockOrSemicolon);
  }

  public VariableDeclaratorListTreeImpl completeFieldDeclaration(TypeTree type, VariableDeclaratorListTreeImpl partial, AstNode semicolonTokenAstNode) {
    partial.prependChildren((AstNode) type);
    for (VariableTreeImpl variable : partial) {
      variable.completeType(type);
    }

    // store the semicolon as endToken for the last variable
    InternalSyntaxToken semicolonToken = InternalSyntaxToken.create(semicolonTokenAstNode);
    partial.get(partial.size() - 1).setEndToken(semicolonToken);

    partial.addChild(semicolonToken);
    return partial;
  }

  // End of classes, enums and interfaces

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
    children.add(openBraceToken);

    if (annotationTypeElementDeclarations.isPresent()) {
      for (AstNode annotationTypeElementDeclaration : annotationTypeElementDeclarations.get()) {
        children.add(annotationTypeElementDeclaration);
        if (annotationTypeElementDeclaration.is(JavaLexer.VARIABLE_DECLARATORS)) {
          for (VariableTreeImpl variable : (VariableDeclaratorListTreeImpl) annotationTypeElementDeclaration) {
            members.add(variable);
          }
        } else if (!annotationTypeElementDeclaration.is(JavaPunctuator.SEMI)) {
          members.add((Tree) annotationTypeElementDeclaration);
        }
      }
    }

    children.add(closeBraceToken);

    return new ClassTreeImpl(emptyModifiers, openBraceToken, members.build(), closeBraceToken, children);
  }

  public AstNode completeAnnotationTypeMember(ModifiersTreeImpl modifiers, AstNode partialAstNode) {
    JavaTree partial = (JavaTree) partialAstNode;
    partial.prependChildren(modifiers);

    if (partial.is(JavaLexer.VARIABLE_DECLARATORS)) {
      for (VariableTreeImpl variable : (VariableDeclaratorListTreeImpl) partial) {
        variable.completeModifiers(modifiers);
      }
    } else if (partial.is(Kind.CLASS) || partial.is(Kind.INTERFACE) || partial.is(Kind.ENUM) || partial.is(Kind.ANNOTATION_TYPE)) {
      ((ClassTreeImpl) partial).completeModifiers(modifiers);
    } else if (partial.is(Kind.METHOD)) {
      ((MethodTreeImpl) partial).completeWithModifiers(modifiers);
    } else {
      throw new IllegalArgumentException("Unsupported type: " + partial);
    }

    return partial;
  }

  public AstNode completeAnnotationMethod(TypeTree type, AstNode identifierAstNode, MethodTreeImpl partial, AstNode semiTokenAstNode) {
    partial.complete(type, new IdentifierTreeImpl(InternalSyntaxToken.create(identifierAstNode)), InternalSyntaxToken.create(semiTokenAstNode));

    return partial;
  }

  public MethodTreeImpl newAnnotationTypeMethod(AstNode openParenTokenAstNode, AstNode closeParenTokenAstNode, Optional<Tuple<InternalSyntaxToken, ExpressionTree>> defaultValue) {
    InternalSyntaxToken openParenToken = InternalSyntaxToken.create(openParenTokenAstNode);
    InternalSyntaxToken closeParenToken = InternalSyntaxToken.create(closeParenTokenAstNode);

    FormalParametersListTreeImpl parameters = new FormalParametersListTreeImpl(openParenToken, closeParenToken);
    InternalSyntaxToken defaultToken = null;
    ExpressionTree defaultExpression = null;
    if (defaultValue.isPresent()) {
      defaultToken = defaultValue.get().first();
      defaultExpression = defaultValue.get().second();
    }
    return new MethodTreeImpl(parameters, defaultToken, defaultExpression);
  }

  public Tuple<InternalSyntaxToken, ExpressionTree> newDefaultValue(AstNode defaultTokenAstNode, ExpressionTree elementValue) {
    InternalSyntaxToken defaultToken = InternalSyntaxToken.create(defaultTokenAstNode);
    return new Tuple<InternalSyntaxToken, ExpressionTree>(defaultToken, elementValue);
  }

  public AnnotationTreeImpl newAnnotation(AstNode atTokenAstNode, TypeTree qualifiedIdentifier, Optional<ArgumentListTreeImpl> arguments) {
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
    elementValuePairs.complete(openParenToken, closeParenToken);
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

    return elementValues.completeWithCurlyBraces(openBraceToken, closeBraceToken);
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

  public FormalParametersListTreeImpl completeTypeFormalParameters(ModifiersTreeImpl modifiers, TypeTree type, FormalParametersListTreeImpl partial) {
    VariableTreeImpl variable = partial.get(0);

    variable.completeModifiersAndType(modifiers, type);
    partial.prependChildren(modifiers, (AstNode) type);

    return partial;
  }

  public FormalParametersListTreeImpl prependNewFormalParameter(VariableTreeImpl variable, Optional<AstNode> rest) {
    if (rest.isPresent()) {
      InternalSyntaxToken comma = InternalSyntaxToken.create(rest.get().getFirstChild(JavaPunctuator.COMMA));
      FormalParametersListTreeImpl partial = (FormalParametersListTreeImpl) rest.get().getLastChild();

      partial.add(0, variable);
      partial.prependChildren(variable, comma);

      // store the comma as endToken for the variable
      variable.setEndToken(comma);

      return partial;
    } else {
      return new FormalParametersListTreeImpl(variable);
    }
  }

  public FormalParametersListTreeImpl newVariableArgumentFormalParameter(Optional<List<AnnotationTreeImpl>> annotations, AstNode ellipsisTokenAstNode, VariableTreeImpl variable) {
    InternalSyntaxToken ellipsisToken = InternalSyntaxToken.create(ellipsisTokenAstNode);

    variable.addEllipsisDimension(new ArrayTypeTreeImpl(null, annotations.isPresent() ? annotations.get() : ImmutableList.<AnnotationTreeImpl>of(), ellipsisToken));

    return new FormalParametersListTreeImpl(
      annotations.isPresent() ? annotations.get() : ImmutableList.<AnnotationTreeImpl>of(),
      ellipsisToken,
      variable);
  }

  public VariableTreeImpl newVariableDeclaratorId(AstNode identifierAstNode, Optional<List<Tuple<Optional<List<AnnotationTreeImpl>>, Tuple<AstNode, AstNode>>>> dims) {
    IdentifierTreeImpl identifier = new IdentifierTreeImpl(InternalSyntaxToken.create(identifierAstNode));
    ArrayTypeTreeImpl nestedDimensions = newArrayTypeTreeWithAnnotations(dims);
    List<AstNode> children = Lists.newArrayList();
    children.add(nestedDimensions);
    return new VariableTreeImpl(identifier, nestedDimensions, children);
  }

  public VariableTreeImpl newFormalParameter(ModifiersTreeImpl modifiers, TypeTree type, VariableTreeImpl variable) {
    variable.prependChildren(modifiers, (AstNode) type);
    return variable.completeType(type);
  }

  // End of formal parameters

  // Statements

  public VariableDeclaratorListTreeImpl completeLocalVariableDeclaration(
    ModifiersTreeImpl modifiers,
    TypeTree type,
    VariableDeclaratorListTreeImpl variables,
    AstNode semicolonTokenAstNode) {
    InternalSyntaxToken semicolonSyntaxToken = InternalSyntaxToken.create(semicolonTokenAstNode);

    variables.prependChildren(modifiers, (AstNode) type);

    for (VariableTreeImpl variable : variables) {
      variable.completeModifiersAndType(modifiers, type);
    }

    // store the semicolon as endToken for the last variable
    variables.get(variables.size() - 1).setEndToken(semicolonSyntaxToken);

    return variables;
  }

  public VariableDeclaratorListTreeImpl newVariableDeclarators(VariableTreeImpl variable, Optional<List<Tuple<AstNode, VariableTreeImpl>>> rests) {
    ImmutableList.Builder<VariableTreeImpl> variables = ImmutableList.builder();

    variables.add(variable);
    List<AstNode> children = Lists.newArrayList();
    children.add(variable);

    if (rests.isPresent()) {
      VariableTreeImpl previousVariable = variable;
      for (Tuple<AstNode, VariableTreeImpl> rest : rests.get()) {
        VariableTreeImpl newVariable = rest.second();
        InternalSyntaxToken separator = InternalSyntaxToken.create(rest.first());

        variables.add(newVariable);
        children.add(newVariable);

        // store the separator
        previousVariable.setEndToken(separator);
        previousVariable = newVariable;
      }
    }

    return new VariableDeclaratorListTreeImpl(variables.build(), children);
  }

  public VariableTreeImpl completeVariableDeclarator(AstNode identifierAstNode, Optional<List<Tuple<Optional<List<AnnotationTreeImpl>>, Tuple<AstNode, AstNode>>>> dimensions,
    Optional<VariableTreeImpl> partial) {
    IdentifierTreeImpl identifier = new IdentifierTreeImpl(InternalSyntaxToken.create(identifierAstNode));

    ArrayTypeTreeImpl nestedDimensions = newArrayTypeTreeWithAnnotations(dimensions);

    List<AstNode> children = Lists.newArrayList();
    if (nestedDimensions != null) {
      children.add(nestedDimensions);
    }
    if (partial.isPresent()) {
      children.add(0, identifier);
      partial.get().prependChildren(children);

      return partial.get().completeIdentifierAndDims(identifier, nestedDimensions);
    } else {
      return new VariableTreeImpl(identifier, nestedDimensions, children);
    }
  }

  public VariableTreeImpl newVariableDeclarator(AstNode equalTokenAstNode, ExpressionTree initializer) {
    InternalSyntaxToken equalToken = InternalSyntaxToken.create(equalTokenAstNode);

    return new VariableTreeImpl(equalToken, initializer,
      equalToken, (AstNode) initializer);
  }

  public BlockTreeImpl block(AstNode openBraceTokenAstNode, BlockStatementListTreeImpl blockStatements, AstNode closeBraceTokenAstNode) {
    InternalSyntaxToken openBraceToken = InternalSyntaxToken.create(openBraceTokenAstNode);
    InternalSyntaxToken closeBraceToken = InternalSyntaxToken.create(closeBraceTokenAstNode);

    return new BlockTreeImpl(openBraceToken, blockStatements, closeBraceToken,
      openBraceToken, blockStatements, closeBraceToken);
  }

  public AssertStatementTreeImpl completeAssertStatement(
    AstNode assertToken, ExpressionTree expression, Optional<AssertStatementTreeImpl> detailExpression, AstNode semicolonToken) {

    InternalSyntaxToken assertSyntaxToken = InternalSyntaxToken.create(assertToken);
    InternalSyntaxToken semicolonSyntaxToken = InternalSyntaxToken.create(semicolonToken);
    return detailExpression.isPresent() ?
      detailExpression.get().complete(assertSyntaxToken, expression, semicolonSyntaxToken) :
      new AssertStatementTreeImpl(assertSyntaxToken, expression, semicolonSyntaxToken);
  }

  public AssertStatementTreeImpl newAssertStatement(AstNode colonToken, ExpressionTree expression) {
    return new AssertStatementTreeImpl(InternalSyntaxToken.create(colonToken), expression);
  }

  public IfStatementTreeImpl completeIf(AstNode ifToken, AstNode openParen, ExpressionTree condition, AstNode closeParen, StatementTree statement,
    Optional<IfStatementTreeImpl> elseClause) {
    InternalSyntaxToken ifKeyword = InternalSyntaxToken.create(ifToken);
    InternalSyntaxToken openParenToken = InternalSyntaxToken.create(openParen);
    InternalSyntaxToken closeParenToken = InternalSyntaxToken.create(closeParen);
    if (elseClause.isPresent()) {
      return elseClause.get().complete(ifKeyword, openParenToken, condition, closeParenToken, statement);
    } else {
      return new IfStatementTreeImpl(ifKeyword, openParenToken, condition, closeParenToken, statement);
    }
  }

  public IfStatementTreeImpl newIfWithElse(AstNode elseToken, StatementTree elseStatement) {
    InternalSyntaxToken elseKeyword = InternalSyntaxToken.create(elseToken);
    return new IfStatementTreeImpl(elseKeyword, elseStatement);
  }

  public ForStatementTreeImpl newStandardForStatement(
    AstNode forTokenAstNode,
    AstNode openParenTokenAstNode,
    Optional<StatementExpressionListTreeImpl> forInit, AstNode forInitSemicolonTokenAstNode,
    Optional<ExpressionTree> expression, AstNode expressionSemicolonTokenAstNode,
    Optional<StatementExpressionListTreeImpl> forUpdate, AstNode closeParenTokenAstNode,
    StatementTree statement) {

    StatementExpressionListTreeImpl forInit2 = forInit.isPresent() ? forInit.get() : new StatementExpressionListTreeImpl(ImmutableList.<StatementTree>of());
    StatementExpressionListTreeImpl forUpdate2 = forUpdate.isPresent() ? forUpdate.get() : new StatementExpressionListTreeImpl(ImmutableList.<StatementTree>of());

    InternalSyntaxToken forKeyword = InternalSyntaxToken.create(forTokenAstNode);
    InternalSyntaxToken openParenToken = InternalSyntaxToken.create(openParenTokenAstNode);
    InternalSyntaxToken firstSemicolonToken = InternalSyntaxToken.create(forInitSemicolonTokenAstNode);
    InternalSyntaxToken secondSemicolonToken = InternalSyntaxToken.create(expressionSemicolonTokenAstNode);
    InternalSyntaxToken closeParenToken = InternalSyntaxToken.create(closeParenTokenAstNode);
    
    ForStatementTreeImpl result = new ForStatementTreeImpl(
      forKeyword,
      openParenToken,
      forInit2,
      firstSemicolonToken,
      expression.isPresent() ? expression.get() : null,
      secondSemicolonToken,
      forUpdate2,
      closeParenToken,
      statement);

    List<AstNode> children = Lists.newArrayList();
    children.add(forKeyword);
    children.add(openParenToken);
    children.add(forInit2);
    children.add(firstSemicolonToken);
    if (expression.isPresent()) {
      children.add((AstNode) expression.get());
    }
    children.add(secondSemicolonToken);
    children.add(forUpdate2);
    children.add(closeParenToken);
    children.add((AstNode) statement);

    result.prependChildren(children);

    return result;
  }

  public StatementExpressionListTreeImpl newForInitDeclaration(ModifiersTreeImpl modifiers, TypeTree type, VariableDeclaratorListTreeImpl variables) {
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

    ExpressionStatementTreeImpl statement = new ExpressionStatementTreeImpl(expression, null);
    statements.add(statement);
    children.add(statement);

    if (rests.isPresent()) {
      for (AstNode rest : rests.get()) {
        children.add(rest.getFirstChild());

        statement = new ExpressionStatementTreeImpl((ExpressionTree) rest.getLastChild(), null);
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

    InternalSyntaxToken forKeyword = InternalSyntaxToken.create(forTokenAstNode);
    InternalSyntaxToken openParenToken = InternalSyntaxToken.create(openParenTokenAstNode);
    InternalSyntaxToken colonToken = InternalSyntaxToken.create(colonTokenAstNode);
    InternalSyntaxToken closeParenToken = InternalSyntaxToken.create(closeParenTokenAstNode);

    return new ForEachStatementImpl(forKeyword, openParenToken, variable, colonToken, expression, closeParenToken, statement);
  }

  public WhileStatementTreeImpl whileStatement(AstNode whileToken, AstNode openParen, ExpressionTree expression, AstNode closeParen, StatementTree statement) {
    InternalSyntaxToken whileKeyword = InternalSyntaxToken.create(whileToken);
    InternalSyntaxToken openParenToken = InternalSyntaxToken.create(openParen);
    InternalSyntaxToken closeParenToken = InternalSyntaxToken.create(closeParen);
    return new WhileStatementTreeImpl(whileKeyword, openParenToken, expression, closeParenToken, statement);
  }

  public DoWhileStatementTreeImpl doWhileStatement(AstNode doToken, StatementTree statement, AstNode whileToken, AstNode openParen, ExpressionTree expression, AstNode closeParen,
    AstNode semicolon) {
    InternalSyntaxToken doKeyword = InternalSyntaxToken.create(doToken);
    InternalSyntaxToken whileKeyword = InternalSyntaxToken.create(whileToken);
    InternalSyntaxToken openParenToken = InternalSyntaxToken.create(openParen);
    InternalSyntaxToken closeParenToken = InternalSyntaxToken.create(closeParen);
    InternalSyntaxToken semiColonToken = InternalSyntaxToken.create(semicolon);
    return new DoWhileStatementTreeImpl(doKeyword, statement, whileKeyword, openParenToken, expression, closeParenToken, semiColonToken);
  }

  public TryStatementTreeImpl completeStandardTryStatement(AstNode tryTokenAstNode, BlockTreeImpl block, TryStatementTreeImpl partial) {
    InternalSyntaxToken tryToken = InternalSyntaxToken.create(tryTokenAstNode);

    return partial.completeStandardTry(tryToken, block);
  }

  public TryStatementTreeImpl newTryCatch(Optional<List<CatchTreeImpl>> catches, Optional<TryStatementTreeImpl> finallyBlock) {
    List<CatchTreeImpl> catchTrees = catches.isPresent() ? catches.get() : ImmutableList.<CatchTreeImpl>of();
    if (finallyBlock.isPresent()) {
      return finallyBlock.get().completeWithCatches(catchTrees);
    } else {
      return new TryStatementTreeImpl(catchTrees, null, null);
    }
  }

  public CatchTreeImpl newCatchClause(AstNode catchTokenAstNode, AstNode openParenTokenAstNode, VariableTreeImpl parameter, AstNode closeParenTokenAstNode, BlockTreeImpl block) {
    InternalSyntaxToken catchToken = InternalSyntaxToken.create(catchTokenAstNode);
    InternalSyntaxToken openParenToken = InternalSyntaxToken.create(openParenTokenAstNode);
    InternalSyntaxToken closeParenToken = InternalSyntaxToken.create(closeParenTokenAstNode);

    return new CatchTreeImpl(catchToken, openParenToken, parameter, closeParenToken, block);
  }

  public VariableTreeImpl newCatchFormalParameter(Optional<ModifiersTreeImpl> modifiers, TypeTree type, VariableTreeImpl parameter) {
    // TODO modifiers

    if (modifiers.isPresent()) {
      parameter.prependChildren(modifiers.get(), (AstNode) type);
    } else {
      parameter.prependChildren((AstNode) type);
    }

    return parameter.completeType(type);
  }

  public TypeTree newCatchType(TypeTree qualifiedIdentifier, Optional<List<AstNode>> rests) {
    if (!rests.isPresent()) {
      return qualifiedIdentifier;
    }

    List<AstNode> children = Lists.newArrayList();
    ImmutableList.Builder<TypeTree> types = ImmutableList.builder();

    children.add((AstNode) qualifiedIdentifier);
    types.add(qualifiedIdentifier);

    for (AstNode rest : rests.get()) {
      children.add(rest.getFirstChild());

      TypeTree qualifiedIdentifier2 = (TypeTree) rest.getLastChild();
      types.add(qualifiedIdentifier2);

      children.add((AstNode) qualifiedIdentifier2);
    }

    return new UnionTypeTreeImpl(new TypeUnionListTreeImpl(types.build(), children));
  }

  public TryStatementTreeImpl newFinallyBlock(AstNode finallyTokenAstNode, BlockTreeImpl block) {
    return new TryStatementTreeImpl(InternalSyntaxToken.create(finallyTokenAstNode), block);
  }

  public TryStatementTreeImpl newTryWithResourcesStatement(
    AstNode tryTokenAstNode, AstNode openParenTokenAstNode, ResourceListTreeImpl resources, AstNode closeParenTokenAstNode,
    BlockTreeImpl block,
    Optional<List<CatchTreeImpl>> catches, Optional<TryStatementTreeImpl> finallyBlock) {

    InternalSyntaxToken tryToken = InternalSyntaxToken.create(tryTokenAstNode);
    InternalSyntaxToken openParenToken = InternalSyntaxToken.create(openParenTokenAstNode);
    InternalSyntaxToken closeParenToken = InternalSyntaxToken.create(closeParenTokenAstNode);
    
    List<CatchTreeImpl> catchTrees = catches.isPresent() ? catches.get() : ImmutableList.<CatchTreeImpl>of();
    if (finallyBlock.isPresent()) {
      return finallyBlock.get().completeTryWithResources(tryToken, openParenToken, resources, closeParenToken, block, catchTrees);
    } else {
      return new TryStatementTreeImpl(tryToken, openParenToken, resources, closeParenToken, block, catchTrees);
    }
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

  public VariableTreeImpl newResource(ModifiersTreeImpl modifiers, TypeTree classType, VariableTreeImpl partial, AstNode equalTokenAstNode, ExpressionTree expression) {
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

    return new SwitchStatementTreeImpl(switchKeyword, openParenToken, expression, closeParenToken,
      openBraceToken, groups, closeBraceToken);
  }

  public CaseGroupTreeImpl switchGroup(List<CaseLabelTreeImpl> labels, BlockStatementListTreeImpl blockStatements) {
    return new CaseGroupTreeImpl(labels, blockStatements);
  }

  public CaseLabelTreeImpl newCaseSwitchLabel(AstNode caseToken, ExpressionTree expression, AstNode colonToken) {
    InternalSyntaxToken caseSyntaxToken = InternalSyntaxToken.create(caseToken);
    InternalSyntaxToken colonSyntaxToken = InternalSyntaxToken.create(colonToken);
    return new CaseLabelTreeImpl(caseSyntaxToken, expression, colonSyntaxToken);
  }

  public CaseLabelTreeImpl newDefaultSwitchLabel(AstNode defaultToken, AstNode colonToken) {
    InternalSyntaxToken defaultSyntaxToken = InternalSyntaxToken.create(defaultToken);
    InternalSyntaxToken colonSyntaxToken = InternalSyntaxToken.create(colonToken);
    return new CaseLabelTreeImpl(defaultSyntaxToken, null, colonSyntaxToken);
  }

  public SynchronizedStatementTreeImpl synchronizedStatement(AstNode synchronizedToken, AstNode openParen, ExpressionTree expression, AstNode closeParen, BlockTreeImpl block) {
    InternalSyntaxToken synchronizedKeyword = InternalSyntaxToken.create(synchronizedToken);
    InternalSyntaxToken openParenToken = InternalSyntaxToken.create(openParen);
    InternalSyntaxToken closeParenToken = InternalSyntaxToken.create(closeParen);
    return new SynchronizedStatementTreeImpl(synchronizedKeyword, openParenToken, expression, closeParenToken, block);
  }

  public BreakStatementTreeImpl breakStatement(AstNode breakToken, Optional<AstNode> identifierAstNode, AstNode semicolonToken) {
    InternalSyntaxToken breakSyntaxToken = InternalSyntaxToken.create(breakToken);
    InternalSyntaxToken semicolonSyntaxToken = InternalSyntaxToken.create(semicolonToken);
    IdentifierTreeImpl identifier = null;
    if (identifierAstNode.isPresent()) {
      identifier = new IdentifierTreeImpl(InternalSyntaxToken.create(identifierAstNode.get()));
    }
    return new BreakStatementTreeImpl(breakSyntaxToken, identifier, semicolonSyntaxToken);
  }

  public ContinueStatementTreeImpl continueStatement(AstNode continueToken, Optional<AstNode> identifierAstNode, AstNode semicolonToken) {
    InternalSyntaxToken continueKeywordSyntaxToken = InternalSyntaxToken.create(continueToken);
    InternalSyntaxToken semicolonSyntaxToken = InternalSyntaxToken.create(semicolonToken);
    IdentifierTreeImpl identifier = null;
    if (identifierAstNode.isPresent()) {
      identifier = new IdentifierTreeImpl(InternalSyntaxToken.create(identifierAstNode.get()));
    }
    return new ContinueStatementTreeImpl(continueKeywordSyntaxToken, identifier, semicolonSyntaxToken);
  }

  public ReturnStatementTreeImpl returnStatement(AstNode returnToken, Optional<ExpressionTree> expression, AstNode semicolonToken) {
    InternalSyntaxToken returnKeywordSyntaxToken = InternalSyntaxToken.create(returnToken);
    InternalSyntaxToken semicolonSyntaxToken = InternalSyntaxToken.create(semicolonToken);
    ExpressionTree expressionTree = expression.isPresent() ? expression.get() : null;
    return new ReturnStatementTreeImpl(returnKeywordSyntaxToken, expressionTree, semicolonSyntaxToken);
  }

  public ThrowStatementTreeImpl throwStatement(AstNode throwToken, ExpressionTree expression, AstNode semicolonToken) {
    InternalSyntaxToken throwSyntaxToken = InternalSyntaxToken.create(throwToken);
    InternalSyntaxToken semicolonSyntaxToken = InternalSyntaxToken.create(semicolonToken);
    return new ThrowStatementTreeImpl(throwSyntaxToken, expression, semicolonSyntaxToken);
  }

  public LabeledStatementTreeImpl labeledStatement(AstNode identifierAstNode, AstNode colon, StatementTree statement) {
    IdentifierTreeImpl identifier = new IdentifierTreeImpl(InternalSyntaxToken.create(identifierAstNode));
    InternalSyntaxToken colonSyntaxToken = InternalSyntaxToken.create(colon);
    return new LabeledStatementTreeImpl(identifier, colonSyntaxToken, statement);
  }

  public ExpressionStatementTreeImpl expressionStatement(ExpressionTree expression, AstNode semicolonTokenAstNode) {
    InternalSyntaxToken semicolonToken = InternalSyntaxToken.create(semicolonTokenAstNode);

    return new ExpressionStatementTreeImpl(expression, semicolonToken);
  }

  public EmptyStatementTreeImpl emptyStatement(AstNode semicolon) {
    return new EmptyStatementTreeImpl(InternalSyntaxToken.create(semicolon));
  }

  public BlockStatementListTreeImpl blockStatements(Optional<List<BlockStatementListTreeImpl>> blockStatements) {
    List<AstNode> children = Lists.newArrayList();
    ImmutableList.Builder<StatementTree> builder = ImmutableList.builder();

    if (blockStatements.isPresent()) {
      for (BlockStatementListTreeImpl blockStatement : blockStatements.get()) {
        children.add(blockStatement);
        builder.addAll(blockStatement);
      }
    }

    return new BlockStatementListTreeImpl(builder.build(),
      children);
  }

  public BlockStatementListTreeImpl wrapInBlockStatements(VariableDeclaratorListTreeImpl variables) {
    return new BlockStatementListTreeImpl(variables,
      ImmutableList.<AstNode>of(variables));
  }

  public BlockStatementListTreeImpl newInnerClassOrEnum(ModifiersTreeImpl modifiers, ClassTreeImpl classTree) {
    classTree.prependChildren(modifiers);
    classTree.completeModifiers(modifiers);
    return new BlockStatementListTreeImpl(ImmutableList.<StatementTree>of(classTree),
      ImmutableList.<AstNode>of(classTree));
  }

  public BlockStatementListTreeImpl wrapInBlockStatements(StatementTree statement) {
    return new BlockStatementListTreeImpl(ImmutableList.of(statement),
      ImmutableList.of((AstNode) statement));
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

  public InstanceOfTreeImpl newInstanceofExpression(AstNode instanceofTokenAstNode, TypeTree type) {
    InternalSyntaxToken instanceofToken = InternalSyntaxToken.create(instanceofTokenAstNode);
    return new InstanceOfTreeImpl(instanceofToken, type);
  }

  private static class OperatorAndOperand {

    private final InternalSyntaxToken operator;
    private final ExpressionTree operand;

    public OperatorAndOperand(InternalSyntaxToken operator, ExpressionTree operand) {
      this.operator = operator;
      this.operand = operand;
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

    ExpressionTree result = expression;
    for (OperatorAndOperand operatorAndOperand : operatorAndOperands.get()) {
      result = new BinaryExpressionTreeImpl(
        kindMaps.getBinaryOperator((JavaPunctuator) operatorAndOperand.operator().getType()),
        result,
        operatorAndOperand.operator(),
        operatorAndOperand.operand());
    }
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

  public ExpressionTree newPostfixExpression(ExpressionTree expression, Optional<AstNode> postfixOperatorAstNode) {
    ExpressionTree result = expression;

    if (postfixOperatorAstNode.isPresent()) {
      InternalSyntaxToken postfixOperatorToken = InternalSyntaxToken.create(postfixOperatorAstNode.get());
      result = new InternalPostfixUnaryExpression(kindMaps.getPostfixOperator((JavaPunctuator) postfixOperatorAstNode.get().getType()), result, postfixOperatorToken);
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
    return new TypeCastExpressionTreeImpl(basicType, closeParenToken, expression);
  }

  public TypeCastExpressionTreeImpl newClassCastExpression(TypeTree type, Optional<List<Tuple<InternalSyntaxToken, Tree>>> classTypes, AstNode closeParenTokenAstNode,
    ExpressionTree expression) {
    InternalSyntaxToken closeParenToken = InternalSyntaxToken.create(closeParenTokenAstNode);

    ImmutableList.Builder<Tree> boundsBuilder = ImmutableList.<Tree>builder();
    List<AstNode> children = Lists.newArrayList();
    children.add((AstNode) type);
    if (classTypes.isPresent()) {
      for (Tuple<InternalSyntaxToken, Tree> tuple : classTypes.get()) {
        // TODO SONARJAVA-547 andOperator should be present in the tree
        InternalSyntaxToken andOperator = tuple.first();
        Tree classType = tuple.second();
        boundsBuilder.add(classType);

        children.add(andOperator);
        children.add((AstNode) classType);
      }
    }
    children.add(closeParenToken);
    children.add((AstNode) expression);

    return new TypeCastExpressionTreeImpl(type, boundsBuilder.build(), closeParenToken, expression, children);
  }

  public ExpressionTree completeMethodReference(MethodReferenceTreeImpl partial, Optional<TypeArgumentListTreeImpl> typeArguments, AstNode newOrIdentifierToken) {
    TypeArguments typeArgs = null;
    if (typeArguments.isPresent()) {
      typeArgs = typeArguments.get();
    }
    InternalSyntaxToken newOrIdentifierSyntaxToken = InternalSyntaxToken.create(newOrIdentifierToken);
    partial.complete(typeArgs, new IdentifierTreeImpl(newOrIdentifierSyntaxToken));
    return partial;
  }

  public MethodReferenceTreeImpl newSuperMethodReference(AstNode superToken, AstNode doubleColonToken) {
    InternalSyntaxToken superSyntaxToken = InternalSyntaxToken.create(superToken);
    IdentifierTree superIdentifier = new IdentifierTreeImpl(superSyntaxToken);
    InternalSyntaxToken doubleColonSyntaxToken = InternalSyntaxToken.create(doubleColonToken);
    return new MethodReferenceTreeImpl(superIdentifier, doubleColonSyntaxToken);
  }

  public MethodReferenceTreeImpl newTypeMethodReference(Tree type, AstNode doubleColonToken) {
    InternalSyntaxToken doubleColonSyntaxToken = InternalSyntaxToken.create(doubleColonToken);
    return new MethodReferenceTreeImpl(type, doubleColonSyntaxToken);
  }

  public MethodReferenceTreeImpl newPrimaryMethodReference(ExpressionTree expression, AstNode doubleColonToken) {
    InternalSyntaxToken doubleColonSyntaxToken = InternalSyntaxToken.create(doubleColonToken);
    return new MethodReferenceTreeImpl(expression, doubleColonSyntaxToken);
  }

  public ExpressionTree lambdaExpression(LambdaParameterListTreeImpl parameters, AstNode arrowToken, Tree body) {
    InternalSyntaxToken arrowSyntaxToken = InternalSyntaxToken.create(arrowToken);
    return new LambdaExpressionTreeImpl(
      parameters.openParenToken(),
      ImmutableList.<VariableTree>builder().addAll(parameters).build(),
      parameters.closeParenToken(),
      arrowSyntaxToken,
      body);
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

      VariableTreeImpl variable = identifiers.first();
      params.add(variable);
      children.add(variable);

      VariableTreeImpl previousVariable = variable;
      if (identifiers.second().isPresent()) {
        for (Tuple<AstNode, VariableTreeImpl> identifier : identifiers.second().get()) {
          variable = identifier.second();
          params.add(variable);
          children.add(variable);

          InternalSyntaxToken comma = InternalSyntaxToken.create(identifier.first());
          previousVariable.setEndToken(comma);
          previousVariable = variable;
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
    InternalSyntaxToken leftParenSyntaxToken = InternalSyntaxToken.create(leftParenthesisToken);
    InternalSyntaxToken rightParenSyntaxToken = InternalSyntaxToken.create(rightParenthesisToken);
    return new ParenthesizedTreeImpl(leftParenSyntaxToken, expression, rightParenSyntaxToken);
  }

  public ExpressionTree newExpression(AstNode newToken, Optional<List<AnnotationTreeImpl>> annotations, ExpressionTree partial) {
    if (annotations.isPresent()) {
      ((JavaTree) partial).prependChildren(annotations.get());
    }
    InternalSyntaxToken newSyntaxToken = InternalSyntaxToken.create(newToken);
    ((JavaTree) partial).prependChildren(newSyntaxToken);
    if (partial.is(Tree.Kind.NEW_CLASS)) {
      ((NewClassTreeImpl) partial).completeWithNewKeyword(newSyntaxToken);
    } else {
      ((NewArrayTreeImpl) partial).completeWithNewKeyword(newSyntaxToken);
    }
    return partial;
  }

  public ExpressionTree completeCreator(Optional<TypeArgumentListTreeImpl> typeArguments, ExpressionTree partial) {
    // TODO typeArguments is a parameterized expression used to chose which constructor to call
    if (typeArguments.isPresent()) {
      ((JavaTree) partial).prependChildren(typeArguments.get());
    }
    return partial;
  }

  public ExpressionTree newClassCreator(TypeTree qualifiedIdentifier, NewClassTreeImpl classCreatorRest) {
    classCreatorRest.prependChildren((AstNode) qualifiedIdentifier);
    return classCreatorRest.completeWithIdentifier(qualifiedIdentifier);
  }

  public ExpressionTree newArrayCreator(TypeTree type, NewArrayTreeImpl partial) {
    return partial.complete(type,
      (AstNode) type);
  }

  public NewArrayTreeImpl completeArrayCreator(Optional<List<AnnotationTreeImpl>> annotations, NewArrayTreeImpl partial) {
    if (annotations.isPresent()) {
      partial.prependChildren(annotations.get());
    }
    return partial;
  }

  public NewArrayTreeImpl newArrayCreatorWithInitializer(
    AstNode openBracketToken, AstNode closeBracketToken,
    Optional<List<Tuple<AstNode, AstNode>>> dimensions,
    NewArrayTreeImpl partial) {

    List<AstNode> children = Lists.newArrayList();
    children.add(openBracketToken);
    children.add(closeBracketToken);
    if (dimensions.isPresent()) {
      for (Tuple<AstNode, AstNode> dimension : dimensions.get()) {
        children.add(dimension.first());
        children.add(dimension.second());
      }
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
    // TODO SONARJAVA-547 brackets should be stored
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

  public ExpressionTree basicClassExpression(PrimitiveTypeTreeImpl basicType, Optional<List<Tuple<AstNode, AstNode>>> dimensions, AstNode dotToken, AstNode classTokenAstNode) {
    // 15.8.2. Class Literals
    // int.class
    // int[].class

    IdentifierTreeImpl classToken = new IdentifierTreeImpl(InternalSyntaxToken.create(classTokenAstNode));
    ArrayTypeTreeImpl nestedDimensions = newArrayTypeTree(dimensions);
    InternalSyntaxToken dotSyntaxToken = InternalSyntaxToken.create(dotToken);

    List<AstNode> children = Lists.newArrayList();
    children.add(basicType);
    if (nestedDimensions != null) {
      children.add(nestedDimensions);
    }
    children.add(dotSyntaxToken);
    children.add(classToken);

    TypeTree typeTree = applyDim(basicType, nestedDimensions);
    return new MemberSelectExpressionTreeImpl((ExpressionTree) typeTree, dotSyntaxToken, classToken, children.toArray(new AstNode[children.size()]));
  }

  public ExpressionTree voidClassExpression(AstNode voidTokenAstNode, AstNode dotToken, AstNode classTokenAstNode) {
    // void.class
    InternalSyntaxToken voidToken = InternalSyntaxToken.create(voidTokenAstNode);
    InternalSyntaxToken dotSyntaxToken = InternalSyntaxToken.create(dotToken);
    PrimitiveTypeTreeImpl voidType = new PrimitiveTypeTreeImpl(voidToken,
      ImmutableList.<AstNode>of(voidToken));

    IdentifierTreeImpl classToken = new IdentifierTreeImpl(InternalSyntaxToken.create(classTokenAstNode));

    return new MemberSelectExpressionTreeImpl(voidType, dotSyntaxToken, classToken,
      voidType, dotToken, classToken);
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

  public TypeTree annotationIdentifier(AstNode firstIdentifier, Optional<List<Tuple<AstNode, AstNode>>> rests) {
    List<AstNode> children = Lists.newArrayList();
    children.add(firstIdentifier);
    if (rests.isPresent()) {
      for (Tuple<AstNode, AstNode> rest : rests.get()) {
        children.add(rest.first());
        children.add(rest.second());
      }
    }

    JavaTree result = null;

    List<AstNode> pendingChildren = Lists.newArrayList();
    InternalSyntaxToken dotToken = null;
    for (AstNode child : children) {
      if (!child.is(JavaTokenType.IDENTIFIER)) {
        dotToken = InternalSyntaxToken.create(child);
        pendingChildren.add(child);
      } else {
        InternalSyntaxToken identifierToken = InternalSyntaxToken.create(child);

        if (result == null) {
          result = new IdentifierTreeImpl(identifierToken);
        } else {
          IdentifierTreeImpl identifier = new IdentifierTreeImpl(identifierToken);

          pendingChildren.add(0, result);
          pendingChildren.add(identifier);

          result = new MemberSelectExpressionTreeImpl((ExpressionTree) result, dotToken, identifier,
            pendingChildren.toArray(new AstNode[pendingChildren.size()]));
        }

        pendingChildren.clear();
      }
    }

    return (TypeTree) result;
  }

  public <T extends Tree> T newQualifiedIdentifier(ExpressionTree firstIdentifier, Optional<List<Tuple<AstNode, ExpressionTree>>> rests) {
    ExpressionTree result = firstIdentifier;

    if (rests.isPresent()) {
      for (Tuple<AstNode, ExpressionTree> rest : rests.get()) {
        InternalSyntaxToken dotToken = InternalSyntaxToken.create(rest.first());
        if (rest.second().is(Kind.IDENTIFIER)) {
          result = new MemberSelectExpressionTreeImpl(result, dotToken, (IdentifierTreeImpl) rest.second(),
            (AstNode) result, rest.first(), (AstNode) rest.second());
        } else if (rest.second().is(Kind.PARAMETERIZED_TYPE)) {
          ParameterizedTypeTreeImpl parameterizedType = (ParameterizedTypeTreeImpl) rest.second();
          IdentifierTreeImpl identifier = (IdentifierTreeImpl) parameterizedType.type();

          result = new MemberSelectExpressionTreeImpl(result, dotToken, identifier,
            (AstNode) result, rest.first(), identifier);

          result = new ParameterizedTypeTreeImpl((TypeTree) result, (TypeArgumentListTreeImpl) parameterizedType.typeArguments());
        } else {
          throw new IllegalArgumentException();
        }
      }
    }

    return (T) result;
  }

  public ExpressionTree newAnnotatedParameterizedIdentifier(
    Optional<List<AnnotationTreeImpl>> annotations, AstNode identifierAstNode, Optional<TypeArgumentListTreeImpl> typeArguments) {

    ExpressionTree result = new IdentifierTreeImpl(InternalSyntaxToken.create(identifierAstNode));

    if (annotations.isPresent()) {
      ((JavaTree) result).prependChildren(annotations.get());
    }

    if (typeArguments.isPresent()) {
      result = new ParameterizedTypeTreeImpl((TypeTree) result, typeArguments.get());
    }

    return result;
  }

  public NewArrayTreeImpl newArrayInitializer(AstNode openBraceTokenAstNode, Optional<List<AstNode>> rests, AstNode closeBraceTokenAstNode) {
    ImmutableList.Builder<ExpressionTree> initializers = ImmutableList.builder();
    List<AstNode> children = Lists.newArrayList();

    InternalSyntaxToken openBraceToken = InternalSyntaxToken.create(openBraceTokenAstNode);
    InternalSyntaxToken closeBraceToken = InternalSyntaxToken.create(closeBraceTokenAstNode);

    children.add(openBraceToken);
    if (rests.isPresent()) {
      for (AstNode rest : rests.get()) {
        initializers.add((ExpressionTree) rest.getFirstChild());
        children.add(rest.getFirstChild());

        if (rest.getNumberOfChildren() == 2) {
          children.add(rest.getLastChild());
        }
      }
    }
    children.add(closeBraceToken);

    return new NewArrayTreeImpl(ImmutableList.<ExpressionTree>of(), initializers.build(), children).completeWithCurlyBraces(openBraceToken, closeBraceToken);
  }

  public QualifiedIdentifierListTreeImpl newQualifiedIdentifierList(TypeTree qualifiedIdentifier, Optional<List<Tuple<AstNode, TypeTree>>> rests) {
    ImmutableList.Builder<TypeTree> qualifiedIdentifiers = ImmutableList.builder();
    List<AstNode> children = Lists.newArrayList();

    qualifiedIdentifiers.add(qualifiedIdentifier);
    children.add((AstNode) qualifiedIdentifier);

    if (rests.isPresent()) {
      for (Tuple<AstNode, TypeTree> rest : rests.get()) {
        qualifiedIdentifiers.add(rest.second());
        children.add(rest.first());
        children.add((AstNode) rest.second());
      }
    }

    return new QualifiedIdentifierListTreeImpl(qualifiedIdentifiers.build(), children);
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

  public NewClassTreeImpl newClassCreatorRest(ArgumentListTreeImpl arguments, Optional<ClassTreeImpl> classBody) {
    List<AstNode> children = Lists.newArrayList();
    children.add(arguments);

    if (classBody.isPresent()) {
      children.add(classBody.get());
    }

    return new NewClassTreeImpl(arguments.openParenToken(), arguments, arguments.closeParenToken(), classBody.isPresent() ? classBody.get() : null,
      children.toArray(new AstNode[0]));
  }

  public ExpressionTree newIdentifierOrMethodInvocation(Optional<TypeArgumentListTreeImpl> typeArguments, AstNode identifierAstNode, Optional<ArgumentListTreeImpl> arguments) {
    InternalSyntaxToken identifierToken = InternalSyntaxToken.create(identifierAstNode);
    IdentifierTreeImpl identifier = new IdentifierTreeImpl(identifierToken);

    if (typeArguments.isPresent()) {
      identifier.prependChildren(typeArguments.get());
    }

    ExpressionTree result = identifier;

    if (arguments.isPresent()) {
      result = new MethodInvocationTreeImpl(identifier, typeArguments.orNull(), arguments.get());
    }

    return result;
  }

  public Tuple<Optional<InternalSyntaxToken>, ExpressionTree> completeMemberSelectOrMethodSelector(AstNode dotTokenAstNode, ExpressionTree partial) {
    return newTuple(Optional.of(InternalSyntaxToken.create(dotTokenAstNode)), partial);
  }

  public Tuple<Optional<InternalSyntaxToken>, ExpressionTree> completeCreatorSelector(AstNode dotTokenAstNode, ExpressionTree partial) {
    ((NewClassTreeImpl) partial).completeWithDotToken(InternalSyntaxToken.create(dotTokenAstNode));
    return newTuple(Optional.<InternalSyntaxToken>absent(), partial);
  }

  public ExpressionTree newDotClassSelector(Optional<List<Tuple<AstNode, AstNode>>> dimensions, AstNode dotTokenAstNode, AstNode classTokenAstNode) {
    IdentifierTreeImpl identifier = new IdentifierTreeImpl(InternalSyntaxToken.create(classTokenAstNode));
    InternalSyntaxToken dotToken = InternalSyntaxToken.create(dotTokenAstNode);

    ArrayTypeTreeImpl nestedDimensions = newArrayTypeTree(dimensions);
    List<AstNode> children = Lists.newArrayList();
    if (nestedDimensions != null) {
      children.add(nestedDimensions);
    }
    children.add(dotTokenAstNode);
    children.add(identifier);

    return new MemberSelectExpressionTreeImpl(nestedDimensions, dotToken, identifier, children);
  }

  private static ExpressionTree applySelectors(ExpressionTree primary, Optional<List<Tuple<Optional<InternalSyntaxToken>, ExpressionTree>>> selectors) {
    ExpressionTree result = primary;

    if (selectors.isPresent()) {
      for (Tuple<Optional<InternalSyntaxToken>, ExpressionTree> tuple : selectors.get()) {
        Optional<InternalSyntaxToken> dotTokenOptional = tuple.first();
        ExpressionTree selector = tuple.second();

        if (dotTokenOptional.isPresent()) {
          InternalSyntaxToken dotToken = dotTokenOptional.get();

          if (selector.is(Kind.IDENTIFIER)) {
            IdentifierTreeImpl identifier = (IdentifierTreeImpl) selector;
            result = new MemberSelectExpressionTreeImpl(result, dotToken, identifier,
              (AstNode) result, dotToken, identifier);
          } else {
            MethodInvocationTreeImpl methodInvocation = (MethodInvocationTreeImpl) selector;
            IdentifierTreeImpl identifier = (IdentifierTreeImpl) methodInvocation.methodSelect();
            MemberSelectExpressionTreeImpl memberSelect = new MemberSelectExpressionTreeImpl(result, dotToken, identifier,
              (AstNode) result, dotToken, identifier);

            result = new MethodInvocationTreeImpl(memberSelect, methodInvocation.typeArguments(), (ArgumentListTreeImpl) methodInvocation.arguments());
          }
        } else if (selector.is(Kind.NEW_CLASS)) {
          NewClassTreeImpl newClass = (NewClassTreeImpl) selector;
          newClass.prependChildren((AstNode) result);
          result = newClass.completeWithEnclosingExpression(result);
        } else if (selector.is(Kind.ARRAY_ACCESS_EXPRESSION)) {
          ArrayAccessExpressionTreeImpl arrayAccess = (ArrayAccessExpressionTreeImpl) selector;
          result = arrayAccess.complete(result);
        } else if (selector.is(Kind.MEMBER_SELECT)) {
          MemberSelectExpressionTreeImpl memberSelect = (MemberSelectExpressionTreeImpl) selector;
          memberSelect.prependChildren((AstNode) result);
          result = memberSelect.completeWithExpression(result);
        } else {
          throw new IllegalStateException();
        }
      }
    }

    return result;
  }

  public ExpressionTree applySelectors1(ExpressionTree primary, Optional<List<Tuple<Optional<InternalSyntaxToken>, ExpressionTree>>> selectors) {
    return applySelectors(primary, selectors);
  }

  public ExpressionTree applySelectors2(ExpressionTree primary, Optional<List<Tuple<Optional<InternalSyntaxToken>, ExpressionTree>>> selectors) {
    return applySelectors(primary, selectors);
  }

  // End of expressions

  // Helpers

  public static final AstNodeType WRAPPER_AST_NODE = new AstNodeType() {
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

  public AstNode newWrapperAstNode4(AstNode e1, AstNode e2) {
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
          if (o2 instanceof AstNode) {
            addChild((AstNode) o2);
          } else if (o2 instanceof List) {
            for (Object o3 : (List) o2) {
              Preconditions.checkArgument(o3 instanceof AstNode, "Unsupported type: " + o3.getClass().getSimpleName());
              addChild((AstNode) o3);
            }
          } else {
            throw new IllegalArgumentException("Unsupported type: " + o2.getClass().getSimpleName());
          }
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

  public <T, U> Tuple<T, U> newTuple5(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple6(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple7(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple8(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple10(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple11(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple12(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple14(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple16(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple17(T first, U second) {
    return newTuple(first, second);
  }

  public <U> Tuple<Optional<InternalSyntaxToken>, U> newTupleAbsent1(U expression) {
    return newTuple(Optional.<InternalSyntaxToken>absent(), expression);
  }

  public <U> Tuple<Optional<InternalSyntaxToken>, U> newTupleAbsent2(U expression) {
    return newTuple(Optional.<InternalSyntaxToken>absent(), expression);
  }

  public <T, U> Tuple<T, U> newAnnotatedDimensionFromVariableDeclarator(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newAnnotatedDimensionFromVariableDeclaratorId(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newAnnotatedDimensionFromType(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newAnnotatedDimensionFromMethod(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newAnnotatedDimensionFromConstructor(T first, U second) {
    return newTuple(first, second);
  }

  public Tuple<InternalSyntaxToken, Tree> newAdditionalBound(AstNode andToken, Tree type) {
    InternalSyntaxToken andSyntaxToken = InternalSyntaxToken.create(andToken);
    return newTuple(andSyntaxToken, type);
  }

  // End

  private static TypeTree applyDim(TypeTree expression, @Nullable ArrayTypeTreeImpl dim) {
    if (dim != null) {
      dim.setLastChildType(expression);
      return dim;
    } else {
      return expression;
    }
  }

  @CheckForNull
  private static ArrayTypeTreeImpl newArrayTypeTreeWithAnnotations(Optional<List<Tuple<Optional<List<AnnotationTreeImpl>>, Tuple<AstNode, AstNode>>>> dims) {
    ArrayTypeTreeImpl result = null;
    if (dims.isPresent()) {
      for (Tuple<Optional<List<AnnotationTreeImpl>>, Tuple<AstNode, AstNode>> dim : dims.get()) {
        result = newArrayTypeTreeWithAnnotations(result, dim);
      }
    }
    return result;
  }

  private static ArrayTypeTreeImpl newArrayTypeTreeWithAnnotations(TypeTree type, Tuple<Optional<List<AnnotationTreeImpl>>, Tuple<AstNode, AstNode>> dim) {
    List<AnnotationTreeImpl> annotations = dim.first().isPresent() ? dim.first().get() : ImmutableList.<AnnotationTreeImpl>of();
    InternalSyntaxToken openBracketToken = InternalSyntaxToken.create(dim.second().first());
    InternalSyntaxToken closeBracketToken = InternalSyntaxToken.create(dim.second().second());
    return new ArrayTypeTreeImpl(type, annotations, openBracketToken, closeBracketToken);
  }

  @CheckForNull
  private static ArrayTypeTreeImpl newArrayTypeTree(Optional<List<Tuple<AstNode, AstNode>>> dims) {
    ArrayTypeTreeImpl result = null;
    if (dims.isPresent()) {
      for (Tuple<AstNode, AstNode> dim : dims.get()) {
        InternalSyntaxToken openBracketToken = InternalSyntaxToken.create(dim.first());
        InternalSyntaxToken closeBracketToken = InternalSyntaxToken.create(dim.second());
        result = new ArrayTypeTreeImpl(result, ImmutableList.<AnnotationTreeImpl>of(), openBracketToken, closeBracketToken);
      }
    }
    return result;
  }
}
