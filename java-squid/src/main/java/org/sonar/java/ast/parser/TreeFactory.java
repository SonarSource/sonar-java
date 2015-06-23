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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
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

  public ModifierKeywordTreeImpl modifierKeyword(JavaTree javaTree) {
    JavaKeyword keyword = (JavaKeyword) javaTree.getGrammarRuleKey();
    return new ModifierKeywordTreeImpl(kindMaps.getModifier(keyword), ((InternalSyntaxToken) javaTree));
  }

  // Literals

  public ExpressionTree literal(JavaTree tree) {
    return new LiteralTreeImpl(kindMaps.getLiteral(tree.getGrammarRuleKey()), (InternalSyntaxToken) tree);
  }

  // End of literals

  // Compilation unit

  public CompilationUnitTreeImpl newCompilationUnit(
    JavaTree spacing,
    Optional<PackageDeclarationTree> packageDeclaration,
    Optional<List<ImportClauseTree>> importDeclarations,
    Optional<List<Tree>> typeDeclarations,
    JavaTree eof) {

    ImmutableList.Builder<ImportClauseTree> imports = ImmutableList.builder();
    if (importDeclarations.isPresent()) {
      for (ImportClauseTree child : importDeclarations.get()) {
        imports.add(child);
      }
    }

    ImmutableList.Builder<Tree> types = ImmutableList.builder();
    if (typeDeclarations.isPresent()) {
      for (Tree child : typeDeclarations.get()) {
        types.add(child);
      }
    }

    InternalSyntaxToken eofToken = (InternalSyntaxToken) eof;

    return new CompilationUnitTreeImpl(
      packageDeclaration.orNull(),
      imports.build(),
      types.build(),
      eofToken);
  }

  public PackageDeclarationTreeImpl newPackageDeclaration(Optional<List<AnnotationTreeImpl>> annotations, JavaTree packageToken, ExpressionTree qualifiedIdentifier,
                                                          InternalSyntaxToken semicolonToken) {
    List<AnnotationTree> annotationList = Collections.emptyList();
    if (annotations.isPresent()) {
      annotationList = ImmutableList.<AnnotationTree>builder().addAll(annotations.get()).build();
    }
    return new PackageDeclarationTreeImpl(annotationList, (SyntaxToken) packageToken, qualifiedIdentifier, semicolonToken);
  }

  public ImportClauseTree newEmptyImport(InternalSyntaxToken semicolonToken) {
    return new EmptyStatementTreeImpl(semicolonToken);
  }

  public ImportTreeImpl newImportDeclaration(JavaTree importToken, Optional<JavaTree> staticToken, ExpressionTree qualifiedIdentifier,
    Optional<Tuple<InternalSyntaxToken, InternalSyntaxToken>> dotStar,
    InternalSyntaxToken semicolonToken) {

    ExpressionTree target = qualifiedIdentifier;
    if (dotStar.isPresent()) {
      IdentifierTreeImpl identifier = new IdentifierTreeImpl(dotStar.get().second());
      InternalSyntaxToken dotToken = dotStar.get().first();
      target = new MemberSelectExpressionTreeImpl(qualifiedIdentifier, dotToken, identifier);
    }

    InternalSyntaxToken staticKeyword = (InternalSyntaxToken) staticToken.orNull();
    return new ImportTreeImpl((InternalSyntaxToken) importToken, staticKeyword, target, semicolonToken);
  }

  public ClassTreeImpl newTypeDeclaration(ModifiersTreeImpl modifiers, ClassTreeImpl partial) {
    return partial.completeModifiers(modifiers);
  }

  public Tree newEmptyType(InternalSyntaxToken semicolonToken) {
    return new EmptyStatementTreeImpl(semicolonToken);
  }

  // End of compilation unit

  // Types

  public TypeTree newType(TypeTree basicOrClassType,
    Optional<List<Tuple<Optional<List<AnnotationTreeImpl>>, Tuple<InternalSyntaxToken, InternalSyntaxToken>>>> dims) {
    if (!dims.isPresent()) {
      return basicOrClassType;
    } else {
      TypeTree result = basicOrClassType;

      for (Tuple<Optional<List<AnnotationTreeImpl>>, Tuple<InternalSyntaxToken, InternalSyntaxToken>> dim : dims.get()) {
        result = newArrayTypeTreeWithAnnotations(result, dim);
      }

      return result;
    }
  }

  public TypeArgumentListTreeImpl newTypeArgumentList(InternalSyntaxToken openBracketToken, Tree typeArgument, Optional<List<Tuple<InternalSyntaxToken, Tree>>> rests, InternalSyntaxToken closeBracketToken) {
    ImmutableList.Builder<Tree> typeArguments = ImmutableList.builder();
    typeArguments.add(typeArgument);

    if (rests.isPresent()) {
      for (Tuple<InternalSyntaxToken, Tree> rest : rests.get()) {
        // FIXME SONARJAVA-547 comma (rest.first()) should be part of the ArgumentList as token
        typeArguments.add(rest.second());
      }
    }
    return new TypeArgumentListTreeImpl(openBracketToken, typeArguments.build(), closeBracketToken);
  }

  public TypeArgumentListTreeImpl newDiamondTypeArgument(InternalSyntaxToken openBracketToken, InternalSyntaxToken closeBracketToken) {
    return new TypeArgumentListTreeImpl(openBracketToken, ImmutableList.<Tree>of(), closeBracketToken);
  }

  public Tree completeTypeArgument(Optional<List<AnnotationTreeImpl>> annotations, Tree partial) {
    // FIXME SONARJAVA-547 Handle annotations
    return partial;
  }

  public TypeTree newBasicTypeArgument(TypeTree type) {
    return type;
  }

  public WildcardTreeImpl completeWildcardTypeArgument(InternalSyntaxToken queryToken, Optional<WildcardTreeImpl> partial) {
    return partial.isPresent() ?
      partial.get().complete(queryToken) :
      new WildcardTreeImpl(Kind.UNBOUNDED_WILDCARD, queryToken);
  }

  public WildcardTreeImpl newWildcardTypeArguments(JavaTree extendsOrSuperToken, Optional<List<AnnotationTreeImpl>> annotations, TypeTree type) {
    InternalSyntaxToken extendsOrSuperKeyword = (InternalSyntaxToken) extendsOrSuperToken;
    return new WildcardTreeImpl(
      JavaKeyword.EXTENDS.getValue().equals(extendsOrSuperKeyword.text()) ? Kind.EXTENDS_WILDCARD : Kind.SUPER_WILDCARD,
      extendsOrSuperKeyword,
      annotations.isPresent() ? annotations.get() : ImmutableList.<AnnotationTreeImpl>of(),
      type);
  }

  public TypeParameterListTreeImpl newTypeParameterList(InternalSyntaxToken openBracketToken, TypeParameterTreeImpl typeParameter, Optional<List<Tuple<InternalSyntaxToken,
      TypeParameterTreeImpl>>> rests, InternalSyntaxToken closeBracketToken) {
    ImmutableList.Builder<TypeParameterTree> typeParameters = ImmutableList.builder();
    typeParameters.add(typeParameter);

    if (rests.isPresent()) {
      for (Tuple<InternalSyntaxToken, TypeParameterTreeImpl> rest : rests.get()) {
        //FIXME SONARJAVA-547 commas should be handled rest.first()
        typeParameters.add(rest.second());
      }
    }

    return new TypeParameterListTreeImpl(openBracketToken, typeParameters.build(), closeBracketToken);
  }

  public TypeParameterTreeImpl completeTypeParameter(Optional<List<AnnotationTreeImpl>> annotations, JavaTree identifierToken, Optional<TypeParameterTreeImpl> partial) {
    // FIXME SONARJAVA-547 Handle annotations
    IdentifierTreeImpl identifier = new IdentifierTreeImpl((InternalSyntaxToken) identifierToken);
    return partial.isPresent() ?
      partial.get().complete(identifier) :
      new TypeParameterTreeImpl(identifier);
  }

  public TypeParameterTreeImpl newTypeParameter(JavaTree extendsToken, BoundListTreeImpl bounds) {
    return new TypeParameterTreeImpl((InternalSyntaxToken) extendsToken, bounds);
  }

  public BoundListTreeImpl newBounds(TypeTree classType, Optional<List<Tuple<InternalSyntaxToken, Tree>>> rests) {
    ImmutableList.Builder<Tree> classTypes = ImmutableList.builder();

    classTypes.add(classType);
    if (rests.isPresent()) {
      for (Tuple<InternalSyntaxToken, Tree> rest : rests.get()) {
        //FIXME SONARJAVA-547 comma should be handled (rest.first())
          classTypes.add(rest.second());
      }
    }

    return new BoundListTreeImpl(classTypes.build());
  }

  // End of types

  // Classes, enums and interfaces

  public ClassTreeImpl completeClassDeclaration(
    JavaTree classSyntaxToken,
    JavaTree identifierToken, Optional<TypeParameterListTreeImpl> typeParameters,
    Optional<Tuple<JavaTree, TypeTree>> extendsClause,
    Optional<Tuple<JavaTree, QualifiedIdentifierListTreeImpl>> implementsClause,
    ClassTreeImpl partial) {

    IdentifierTreeImpl identifier = new IdentifierTreeImpl((InternalSyntaxToken) identifierToken);

    partial.completeDeclarationKeyword((SyntaxToken) classSyntaxToken);
    partial.completeIdentifier(identifier);
    if (typeParameters.isPresent()) {
      partial.completeTypeParameters(typeParameters.get());
    }
    if (extendsClause.isPresent()) {
      partial.completeSuperclass((SyntaxToken) extendsClause.get().first(), extendsClause.get().second());
    }
    if (implementsClause.isPresent()) {
      InternalSyntaxToken implementsKeyword = ((InternalSyntaxToken) implementsClause.get().first());
      QualifiedIdentifierListTreeImpl interfaces = implementsClause.get().second();
      partial.completeInterfaces(implementsKeyword, interfaces);
    }

    return partial;
  }

  private static ClassTreeImpl newClassBody(Kind kind, InternalSyntaxToken openBraceSyntaxToken, Optional<List<JavaTree>> members, InternalSyntaxToken closeBraceTokenSyntaxToken) {
    ImmutableList.Builder<Tree> builder = ImmutableList.builder();

    if (members.isPresent()) {
      for (JavaTree member : members.get()) {
        if (member instanceof VariableDeclaratorListTreeImpl) {
          for (VariableTreeImpl variable : (VariableDeclaratorListTreeImpl) member) {
            builder.add(variable);
          }
        } else {
          builder.add(member);
        }
      }
    }

    return new ClassTreeImpl(kind, openBraceSyntaxToken, builder.build(), closeBraceTokenSyntaxToken);
  }

  public ClassTreeImpl newClassBody(InternalSyntaxToken openBraceToken, Optional<List<JavaTree>> members, InternalSyntaxToken closeBraceToken) {
    return newClassBody(Kind.CLASS, openBraceToken, members, closeBraceToken);
  }

  public ClassTreeImpl newEnumDeclaration(
    JavaTree enumToken,
    JavaTree identifierToken,
    Optional<Tuple<JavaTree, QualifiedIdentifierListTreeImpl>> implementsClause,
    InternalSyntaxToken openBraceToken,
    Optional<List<EnumConstantTreeImpl>> enumConstants,
    Optional<InternalSyntaxToken> semicolonToken,
    Optional<List<JavaTree>> enumDeclarations,
    InternalSyntaxToken closeBraceToken) {
    
    List<JavaTree> members = Lists.newLinkedList();
    EnumConstantTreeImpl lastEnumConstant = null;
    if (enumConstants.isPresent()) {
      for (EnumConstantTreeImpl enumConstant : enumConstants.get()) {
        members.add(enumConstant);
        lastEnumConstant = enumConstant;
      }
    }
    if (semicolonToken.isPresent()) {
      InternalSyntaxToken semicolon = semicolonToken.get();
      // add the semicolon as endToken of the last enumConstant, or as empty statement in the enum members
      if (lastEnumConstant != null) {
        lastEnumConstant.setEndToken(semicolon);
      } else {
        members.add(newEmptyMember(semicolon));
      }
    }
    if (enumDeclarations.isPresent()) {
      for (JavaTree enumDeclaration : enumDeclarations.get()) {
        members.add(enumDeclaration);
      }
    }

    ClassTreeImpl result = newClassBody(Kind.ENUM, openBraceToken, Optional.of((List<JavaTree>) ImmutableList.<JavaTree>builder().addAll(members).build()), closeBraceToken);

    result.completeDeclarationKeyword((SyntaxToken) enumToken);

    IdentifierTreeImpl identifier = new IdentifierTreeImpl((InternalSyntaxToken) identifierToken);
    result.completeIdentifier(identifier);

    if (implementsClause.isPresent()) {
      InternalSyntaxToken implementsKeyword = (InternalSyntaxToken) implementsClause.get().first();
      QualifiedIdentifierListTreeImpl interfaces = implementsClause.get().second();
      result.completeInterfaces(implementsKeyword, interfaces);
    }

    return result;
  }

  public EnumConstantTreeImpl newEnumConstant(
    Optional<List<AnnotationTreeImpl>> annotations, JavaTree identifierToken,
    Optional<ArgumentListTreeImpl> arguments,
    Optional<ClassTreeImpl> classBody,
    Optional<InternalSyntaxToken> commaToken) {

    // FIXME SONARJAVA-547 Handle annotations
    IdentifierTreeImpl identifier = new IdentifierTreeImpl((InternalSyntaxToken) identifierToken);

    SyntaxToken openParenToken = null;
    SyntaxToken closeParenToken = null;
    List<ExpressionTree> argumentsList = Collections.emptyList();
    if (arguments.isPresent()) {
      ArgumentListTreeImpl argumentsListTreeImpl = arguments.get();
      argumentsList = argumentsListTreeImpl;
      openParenToken = argumentsListTreeImpl.openParenToken();
      closeParenToken = argumentsListTreeImpl.closeParenToken();
    }

    NewClassTreeImpl newClass = new NewClassTreeImpl(
      openParenToken,
      argumentsList,
      closeParenToken,
      classBody.isPresent() ? classBody.get() : null
      );
    newClass.completeWithIdentifier(identifier);

    return new EnumConstantTreeImpl(modifiers((Optional<List<ModifierTree>>) (Optional<?>) annotations), identifier, newClass, commaToken.orNull());
  }

  public ClassTreeImpl completeInterfaceDeclaration(
    JavaTree interfaceToken,
    JavaTree identifierToken, Optional<TypeParameterListTreeImpl> typeParameters,
    Optional<Tuple<JavaTree, QualifiedIdentifierListTreeImpl>> extendsClause,
    ClassTreeImpl partial) {

    IdentifierTreeImpl identifier = new IdentifierTreeImpl((InternalSyntaxToken) identifierToken);

    InternalSyntaxToken interfaceSyntaxToken = (InternalSyntaxToken) interfaceToken;
    partial.completeDeclarationKeyword(interfaceSyntaxToken);

    partial.completeIdentifier(identifier);
    if (typeParameters.isPresent()) {
      partial.completeTypeParameters(typeParameters.get());
    }
    if (extendsClause.isPresent()) {
      InternalSyntaxToken extendsKeyword = (InternalSyntaxToken) extendsClause.get().first();
      QualifiedIdentifierListTreeImpl interfaces = extendsClause.get().second();
      partial.compleInterfacesForInterface(extendsKeyword, interfaces);
    }

    return partial;
  }

  public ClassTreeImpl newInterfaceBody(InternalSyntaxToken openBraceToken, Optional<List<JavaTree>> members, InternalSyntaxToken closeBraceToken) {
    return newClassBody(Kind.INTERFACE, openBraceToken, members, closeBraceToken);
  }

  // TODO Create an intermediate implementation interface for completing modifiers
  public JavaTree completeMember(ModifiersTreeImpl modifiers, JavaTree partial) {

    if (partial instanceof ClassTreeImpl) {
      ((ClassTreeImpl) partial).completeModifiers(modifiers);
    } else if (partial instanceof VariableDeclaratorListTreeImpl) {
      for (VariableTreeImpl variable : (VariableDeclaratorListTreeImpl) partial) {
        variable.completeModifiers(modifiers);
      }
    } else if (partial instanceof MethodTreeImpl) {
      ((MethodTreeImpl) partial).completeWithModifiers(modifiers);
    } else {
      throw new IllegalArgumentException();
    }

    return partial;
  }

  public BlockTreeImpl newInitializerMember(Optional<JavaTree> staticToken, BlockTreeImpl block) {
    if (staticToken.isPresent()) {
      return new StaticInitializerTreeImpl((InternalSyntaxToken) staticToken.get(), (InternalSyntaxToken) block.openBraceToken(), block.body(),
        (InternalSyntaxToken) block.closeBraceToken());
    } else {
      return new BlockTreeImpl(Kind.INITIALIZER, (InternalSyntaxToken) block.openBraceToken(), block.body(), (InternalSyntaxToken) block.closeBraceToken());
    }
  }

  public EmptyStatementTreeImpl newEmptyMember(InternalSyntaxToken semicolonToken) {
    return new EmptyStatementTreeImpl(semicolonToken);
  }

  public MethodTreeImpl completeGenericMethodOrConstructorDeclaration(TypeParameterListTreeImpl typeParameters, MethodTreeImpl partial) {
    return partial.completeWithTypeParameters(typeParameters);
  }

  private MethodTreeImpl newMethodOrConstructor(
    Optional<TypeTree> type, JavaTree identifierToken, FormalParametersListTreeImpl parameters,
    Optional<List<Tuple<Optional<List<AnnotationTreeImpl>>, Tuple<InternalSyntaxToken, InternalSyntaxToken>>>> annotatedDimensions,
    Optional<Tuple<JavaTree, QualifiedIdentifierListTreeImpl>> throwsClause,
    JavaTree blockOrSemicolon) {

    IdentifierTreeImpl identifier = new IdentifierTreeImpl((InternalSyntaxToken) identifierToken);

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
      semicolonToken = (InternalSyntaxToken) blockOrSemicolon;
    }

    InternalSyntaxToken throwsToken = null;
    List<TypeTree> throwsClauses = ImmutableList.of();
    if (throwsClause.isPresent()) {
      throwsToken = (InternalSyntaxToken) throwsClause.get().first();
      throwsClauses = throwsClause.get().second();
    }

    return new MethodTreeImpl(
      actualType,
      identifier,
      parameters,
      throwsToken,
      throwsClauses,
      block,
      semicolonToken);
  }

  public MethodTreeImpl newMethod(
    TypeTree type, JavaTree identifierToken, FormalParametersListTreeImpl parameters,
    Optional<List<Tuple<Optional<List<AnnotationTreeImpl>>, Tuple<InternalSyntaxToken, InternalSyntaxToken>>>> annotatedDimensions,
    Optional<Tuple<JavaTree, QualifiedIdentifierListTreeImpl>> throwsClause,
    JavaTree blockOrSemicolon) {

    return newMethodOrConstructor(Optional.of(type), identifierToken, parameters, annotatedDimensions, throwsClause, blockOrSemicolon);
  }

  public MethodTreeImpl newConstructor(
    JavaTree identifierToken, FormalParametersListTreeImpl parameters,
    Optional<List<Tuple<Optional<List<AnnotationTreeImpl>>, Tuple<InternalSyntaxToken, InternalSyntaxToken>>>> annotatedDimensions,
    Optional<Tuple<JavaTree, QualifiedIdentifierListTreeImpl>> throwsClause,
    JavaTree blockOrSemicolon) {

    return newMethodOrConstructor(Optional.<TypeTree>absent(), identifierToken, parameters, annotatedDimensions, throwsClause, blockOrSemicolon);
  }

  public VariableDeclaratorListTreeImpl completeFieldDeclaration(TypeTree type, VariableDeclaratorListTreeImpl partial, InternalSyntaxToken semicolonToken) {
    for (VariableTreeImpl variable : partial) {
      variable.completeType(type);
    }

    // store the semicolon as endToken for the last variable
    partial.get(partial.size() - 1).setEndToken(semicolonToken);

    return partial;
  }

  // End of classes, enums and interfaces

  // Annotations

  public ClassTreeImpl completeAnnotationType(InternalSyntaxToken atToken, JavaTree interfaceToken, JavaTree identifier, ClassTreeImpl partial) {
    return partial.complete(
      atToken,
      (InternalSyntaxToken) interfaceToken,
      new IdentifierTreeImpl((InternalSyntaxToken) identifier));
  }

  public ClassTreeImpl newAnnotationType(InternalSyntaxToken openBraceToken, Optional<List<JavaTree>> annotationTypeElementDeclarations, InternalSyntaxToken closeBraceToken) {
    // TODO
    ModifiersTreeImpl emptyModifiers = ModifiersTreeImpl.emptyModifiers();

    ImmutableList.Builder<Tree> members = ImmutableList.builder();

    if (annotationTypeElementDeclarations.isPresent()) {
      for (JavaTree annotationTypeElementDeclaration : annotationTypeElementDeclarations.get()) {
        if (annotationTypeElementDeclaration.getGrammarRuleKey().equals(JavaLexer.VARIABLE_DECLARATORS)) {
          for (VariableTreeImpl variable : (VariableDeclaratorListTreeImpl) annotationTypeElementDeclaration) {
            members.add(variable);
          }
        } else if (!annotationTypeElementDeclaration.is(Kind.TOKEN)) {
          members.add(annotationTypeElementDeclaration);
        }
      }
    }

    return new ClassTreeImpl(emptyModifiers, openBraceToken, members.build(), closeBraceToken);
  }

  public JavaTree completeAnnotationTypeMember(ModifiersTreeImpl modifiers, JavaTree partial) {

    if (partial.getGrammarRuleKey().equals(JavaLexer.VARIABLE_DECLARATORS)) {
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

  public MethodTreeImpl completeAnnotationMethod(TypeTree type, JavaTree identifierToken, MethodTreeImpl partial, InternalSyntaxToken semiToken) {
    partial.complete(type, new IdentifierTreeImpl(((InternalSyntaxToken) identifierToken)), semiToken);
    return partial;
  }

  public MethodTreeImpl newAnnotationTypeMethod(InternalSyntaxToken openParenToken, InternalSyntaxToken closeParenToken, Optional<Tuple<InternalSyntaxToken, ExpressionTree>> defaultValue) {
    FormalParametersListTreeImpl parameters = new FormalParametersListTreeImpl(openParenToken, closeParenToken);
    InternalSyntaxToken defaultToken = null;
    ExpressionTree defaultExpression = null;
    if (defaultValue.isPresent()) {
      defaultToken = defaultValue.get().first();
      defaultExpression = defaultValue.get().second();
    }
    return new MethodTreeImpl(parameters, defaultToken, defaultExpression);
  }

  public Tuple<InternalSyntaxToken, ExpressionTree> newDefaultValue(JavaTree defaultToken, ExpressionTree elementValue) {
    return new Tuple<>((InternalSyntaxToken)defaultToken, elementValue);
  }

  public AnnotationTreeImpl newAnnotation(InternalSyntaxToken atToken, TypeTree qualifiedIdentifier, Optional<ArgumentListTreeImpl> arguments) {
    return new AnnotationTreeImpl(
      atToken,
      qualifiedIdentifier,
      arguments.isPresent() ?
        arguments.get() :
        null);
  }

  public ArgumentListTreeImpl completeNormalAnnotation(InternalSyntaxToken openParenToken, Optional<ArgumentListTreeImpl> partial, InternalSyntaxToken closeParenToken) {
    if (!partial.isPresent()) {
      return new ArgumentListTreeImpl(openParenToken, closeParenToken);
    }

    ArgumentListTreeImpl elementValuePairs = partial.get();
    elementValuePairs.complete(openParenToken, closeParenToken);

    return elementValuePairs;
  }

  public ArgumentListTreeImpl newNormalAnnotation(AssignmentExpressionTreeImpl elementValuePair, Optional<List<Tuple<InternalSyntaxToken, AssignmentExpressionTreeImpl>>> rests) {
    ImmutableList.Builder<ExpressionTree> expressions = ImmutableList.builder();
    expressions.add(elementValuePair);

    if (rests.isPresent()) {
      for (Tuple<InternalSyntaxToken, AssignmentExpressionTreeImpl> rest : rests.get()) {
        expressions.add(rest.second());
      }
    }

    return new ArgumentListTreeImpl(expressions.build());
  }

  public AssignmentExpressionTreeImpl newElementValuePair(JavaTree identifierToken, InternalSyntaxToken operator, ExpressionTree elementValue) {
    return new AssignmentExpressionTreeImpl(
      kindMaps.getAssignmentOperator((JavaPunctuator) operator.getGrammarRuleKey()),
      new IdentifierTreeImpl((InternalSyntaxToken) identifierToken),
      operator,
      elementValue);
  }

  public NewArrayTreeImpl completeElementValueArrayInitializer(
      InternalSyntaxToken openBraceToken, Optional<NewArrayTreeImpl> partial, Optional<InternalSyntaxToken> commaTokenOptional, InternalSyntaxToken closeBraceToken) {

    InternalSyntaxToken commaToken = commaTokenOptional.orNull();

    NewArrayTreeImpl elementValues = partial.isPresent() ?
      partial.get() :
      new NewArrayTreeImpl(ImmutableList.<ExpressionTree>of(), ImmutableList.<ExpressionTree>of());

    return elementValues.completeWithCurlyBraces(openBraceToken, closeBraceToken);
  }

  public NewArrayTreeImpl newElementValueArrayInitializer(ExpressionTree elementValue, Optional<List<Tuple<InternalSyntaxToken, ExpressionTree>>> rests) {
    ImmutableList.Builder<ExpressionTree> expressions = ImmutableList.builder();
    expressions.add(elementValue);
    if (rests.isPresent()) {
      for (Tuple<InternalSyntaxToken, ExpressionTree> rest : rests.get()) {
        expressions.add(rest.second());
      }
    }
    return new NewArrayTreeImpl(ImmutableList.<ExpressionTree>of(), expressions.build());
  }

  public ArgumentListTreeImpl newSingleElementAnnotation(InternalSyntaxToken openParenToken, ExpressionTree elementValue, InternalSyntaxToken closeParenToken) {
    return new ArgumentListTreeImpl(openParenToken, elementValue, closeParenToken);
  }

  // End of annotations

  // Formal parameters

  public FormalParametersListTreeImpl completeParenFormalParameters(InternalSyntaxToken openParenToken, Optional<FormalParametersListTreeImpl> partial, InternalSyntaxToken closeParenToken) {

    return partial.isPresent() ?
      partial.get().complete(openParenToken, closeParenToken) :
      new FormalParametersListTreeImpl(openParenToken, closeParenToken);
  }

  public FormalParametersListTreeImpl completeTypeFormalParameters(ModifiersTreeImpl modifiers, TypeTree type, FormalParametersListTreeImpl partial) {
    VariableTreeImpl variable = partial.get(0);

    variable.completeModifiersAndType(modifiers, type);

    return partial;
  }

  public FormalParametersListTreeImpl prependNewFormalParameter(VariableTreeImpl variable, Optional<Tuple<InternalSyntaxToken, FormalParametersListTreeImpl>> rest) {
    if (rest.isPresent()) {
      InternalSyntaxToken comma = rest.get().first();
      FormalParametersListTreeImpl partial = rest.get().second();

      partial.add(0, variable);

      // store the comma as endToken for the variable
      variable.setEndToken(comma);

      return partial;
    } else {
      return new FormalParametersListTreeImpl(variable);
    }
  }

  public FormalParametersListTreeImpl newVariableArgumentFormalParameter(Optional<List<AnnotationTreeImpl>> annotations, InternalSyntaxToken ellipsisToken, VariableTreeImpl variable) {
    variable.addEllipsisDimension(new ArrayTypeTreeImpl(null, annotations.isPresent() ? annotations.get() : ImmutableList.<AnnotationTreeImpl>of(), ellipsisToken));

    return new FormalParametersListTreeImpl(
      annotations.isPresent() ? annotations.get() : ImmutableList.<AnnotationTreeImpl>of(),
      ellipsisToken,
      variable);
  }

  public VariableTreeImpl newVariableDeclaratorId(JavaTree identifierToken, Optional<List<Tuple<Optional<List<AnnotationTreeImpl>>, Tuple<InternalSyntaxToken, InternalSyntaxToken>>>> dims) {
    IdentifierTreeImpl identifier = new IdentifierTreeImpl((InternalSyntaxToken) identifierToken);
    ArrayTypeTreeImpl nestedDimensions = newArrayTypeTreeWithAnnotations(dims);
    return new VariableTreeImpl(identifier, nestedDimensions);
  }

  public VariableTreeImpl newFormalParameter(ModifiersTreeImpl modifiers, TypeTree type, VariableTreeImpl variable) {
    return variable.completeType(type);
  }

  // End of formal parameters

  // Statements

  public VariableDeclaratorListTreeImpl completeLocalVariableDeclaration(
    ModifiersTreeImpl modifiers,
    TypeTree type,
    VariableDeclaratorListTreeImpl variables,
    InternalSyntaxToken semicolonSyntaxToken) {

    for (VariableTreeImpl variable : variables) {
      variable.completeModifiersAndType(modifiers, type);
    }

    // store the semicolon as endToken for the last variable
    variables.get(variables.size() - 1).setEndToken(semicolonSyntaxToken);

    return variables;
  }

  public VariableDeclaratorListTreeImpl newVariableDeclarators(VariableTreeImpl variable, Optional<List<Tuple<InternalSyntaxToken, VariableTreeImpl>>> rests) {
    ImmutableList.Builder<VariableTreeImpl> variables = ImmutableList.builder();

    variables.add(variable);
    if (rests.isPresent()) {
      VariableTreeImpl previousVariable = variable;
      for (Tuple<InternalSyntaxToken, VariableTreeImpl> rest : rests.get()) {
        VariableTreeImpl newVariable = rest.second();
        InternalSyntaxToken separator = rest.first();

        variables.add(newVariable);

        // store the separator
        previousVariable.setEndToken(separator);
        previousVariable = newVariable;
      }
    }

    return new VariableDeclaratorListTreeImpl(variables.build());
  }

  public VariableTreeImpl completeVariableDeclarator(JavaTree identifierToken, Optional<List<Tuple<Optional<List<AnnotationTreeImpl>>, Tuple<InternalSyntaxToken, InternalSyntaxToken>>>> dimensions,
    Optional<VariableTreeImpl> partial) {
    IdentifierTreeImpl identifier = new IdentifierTreeImpl((InternalSyntaxToken) identifierToken);

    ArrayTypeTreeImpl nestedDimensions = newArrayTypeTreeWithAnnotations(dimensions);

    if (partial.isPresent()) {
      return partial.get().completeIdentifierAndDims(identifier, nestedDimensions);
    } else {
      return new VariableTreeImpl(identifier, nestedDimensions);
    }
  }

  public VariableTreeImpl newVariableDeclarator(InternalSyntaxToken equalToken, ExpressionTree initializer) {
    return new VariableTreeImpl(equalToken, initializer);
  }

  public BlockTreeImpl block(InternalSyntaxToken openBraceToken, BlockStatementListTreeImpl blockStatements, InternalSyntaxToken closeBraceToken) {
    return new BlockTreeImpl(openBraceToken, blockStatements, closeBraceToken);
  }

  public AssertStatementTreeImpl completeAssertStatement(
    JavaTree assertToken, ExpressionTree expression, Optional<AssertStatementTreeImpl> detailExpression, InternalSyntaxToken semicolonSyntaxToken) {

    InternalSyntaxToken assertSyntaxToken = (InternalSyntaxToken) assertToken;
    return detailExpression.isPresent() ?
      detailExpression.get().complete(assertSyntaxToken, expression, semicolonSyntaxToken) :
      new AssertStatementTreeImpl(assertSyntaxToken, expression, semicolonSyntaxToken);
  }

  public AssertStatementTreeImpl newAssertStatement(InternalSyntaxToken colonToken, ExpressionTree expression) {
    return new AssertStatementTreeImpl(colonToken, expression);
  }

  public IfStatementTreeImpl completeIf(JavaTree ifToken, InternalSyntaxToken openParenToken, ExpressionTree condition, InternalSyntaxToken closeParenToken, StatementTree statement,
    Optional<IfStatementTreeImpl> elseClause) {
    InternalSyntaxToken ifKeyword = (InternalSyntaxToken) ifToken;
    if (elseClause.isPresent()) {
      return elseClause.get().complete(ifKeyword, openParenToken, condition, closeParenToken, statement);
    } else {
      return new IfStatementTreeImpl(ifKeyword, openParenToken, condition, closeParenToken, statement);
    }
  }

  public IfStatementTreeImpl newIfWithElse(JavaTree elseToken, StatementTree elseStatement) {
    return new IfStatementTreeImpl((InternalSyntaxToken) elseToken, elseStatement);
  }

  public ForStatementTreeImpl newStandardForStatement(
    JavaTree forTokenKeyword,
    InternalSyntaxToken openParenToken,
    Optional<StatementExpressionListTreeImpl> forInit, InternalSyntaxToken forInitSemicolonToken,
    Optional<ExpressionTree> expression, InternalSyntaxToken expressionSemicolonToken,
    Optional<StatementExpressionListTreeImpl> forUpdate, InternalSyntaxToken closeParenToken,
    StatementTree statement) {

    StatementExpressionListTreeImpl forInit2 = forInit.isPresent() ? forInit.get() : new StatementExpressionListTreeImpl(ImmutableList.<StatementTree>of());
    StatementExpressionListTreeImpl forUpdate2 = forUpdate.isPresent() ? forUpdate.get() : new StatementExpressionListTreeImpl(ImmutableList.<StatementTree>of());

    InternalSyntaxToken forKeyword = (InternalSyntaxToken) forTokenKeyword;

    return new ForStatementTreeImpl(
      forKeyword,
      openParenToken,
      forInit2,
      forInitSemicolonToken,
      expression.isPresent() ? expression.get() : null,
      expressionSemicolonToken,
      forUpdate2,
      closeParenToken,
      statement);
  }

  public StatementExpressionListTreeImpl newForInitDeclaration(ModifiersTreeImpl modifiers, TypeTree type, VariableDeclaratorListTreeImpl variables) {
    for (VariableTreeImpl variable : variables) {
      variable.completeModifiersAndType(modifiers, type);
    }

    StatementExpressionListTreeImpl result = new StatementExpressionListTreeImpl(variables);

    return result;
  }

  public StatementExpressionListTreeImpl newStatementExpressions(ExpressionTree expression, Optional<List<Tuple<InternalSyntaxToken, ExpressionTree>>> rests) {
    ImmutableList.Builder<StatementTree> statements = ImmutableList.builder();

    ExpressionStatementTreeImpl statement = new ExpressionStatementTreeImpl(expression, null);
    statements.add(statement);

    if (rests.isPresent()) {
      for (Tuple<InternalSyntaxToken, ExpressionTree> rest : rests.get()) {
        statement = new ExpressionStatementTreeImpl(rest.second(), null);
        statements.add(statement);
      }
    }

    StatementExpressionListTreeImpl result = new StatementExpressionListTreeImpl(statements.build());

    return result;
  }

  public ForEachStatementImpl newForeachStatement(
    JavaTree forKeyword,
    InternalSyntaxToken openParenToken,
    VariableTreeImpl variable, InternalSyntaxToken colonToken, ExpressionTree expression,
    InternalSyntaxToken closeParenToken,
    StatementTree statement) {
    return new ForEachStatementImpl((InternalSyntaxToken) forKeyword, openParenToken, variable, colonToken, expression, closeParenToken, statement);
  }

  public WhileStatementTreeImpl whileStatement(JavaTree whileToken, InternalSyntaxToken openParen, ExpressionTree expression, InternalSyntaxToken closeParen, StatementTree statement) {
    InternalSyntaxToken whileKeyword = (InternalSyntaxToken) whileToken;
    return new WhileStatementTreeImpl(whileKeyword, openParen, expression, closeParen, statement);
  }

  public DoWhileStatementTreeImpl doWhileStatement(JavaTree doToken, StatementTree statement, JavaTree whileToken, InternalSyntaxToken openParen, ExpressionTree expression,
                                                   InternalSyntaxToken closeParen, InternalSyntaxToken semicolon) {
    InternalSyntaxToken doKeyword = (InternalSyntaxToken) doToken;
    InternalSyntaxToken whileKeyword = (InternalSyntaxToken) whileToken;
    return new DoWhileStatementTreeImpl(doKeyword, statement, whileKeyword, openParen, expression, closeParen, semicolon);
  }

  public TryStatementTreeImpl completeStandardTryStatement(JavaTree tryToken, BlockTreeImpl block, TryStatementTreeImpl partial) {
    return partial.completeStandardTry((InternalSyntaxToken) tryToken, block);
  }

  public TryStatementTreeImpl newTryCatch(Optional<List<CatchTreeImpl>> catches, Optional<TryStatementTreeImpl> finallyBlock) {
    List<CatchTreeImpl> catchTrees = catches.isPresent() ? catches.get() : ImmutableList.<CatchTreeImpl>of();
    if (finallyBlock.isPresent()) {
      return finallyBlock.get().completeWithCatches(catchTrees);
    } else {
      return new TryStatementTreeImpl(catchTrees, null, null);
    }
  }

  public CatchTreeImpl newCatchClause(JavaTree catchToken, InternalSyntaxToken openParenToken, VariableTreeImpl parameter, InternalSyntaxToken closeParenToken, BlockTreeImpl block) {
    InternalSyntaxToken catchKeyword = (InternalSyntaxToken) catchToken;
    return new CatchTreeImpl(catchKeyword, openParenToken, parameter, closeParenToken, block);
  }

  public VariableTreeImpl newCatchFormalParameter(Optional<ModifiersTreeImpl> modifiers, TypeTree type, VariableTreeImpl parameter) {
    // FIXME SONARJAVA-547 keep annotations from modifiers
    return parameter.completeType(type);
  }

  public TypeTree newCatchType(TypeTree qualifiedIdentifier, Optional<List<Tuple<InternalSyntaxToken, TypeTree>>> rests) {
    if (!rests.isPresent()) {
      return qualifiedIdentifier;
    }
    ImmutableList.Builder<TypeTree> types = ImmutableList.builder();
    types.add(qualifiedIdentifier);
    for (Tuple<InternalSyntaxToken, TypeTree> rest : rests.get()) {
      types.add(rest.second());
    }

    return new UnionTypeTreeImpl(new TypeUnionListTreeImpl(types.build()));
  }

  public TryStatementTreeImpl newFinallyBlock(JavaTree finallyToken, BlockTreeImpl block) {
    return new TryStatementTreeImpl((InternalSyntaxToken) finallyToken, block);
  }

  public TryStatementTreeImpl newTryWithResourcesStatement(
    JavaTree tryToken, InternalSyntaxToken openParenToken, ResourceListTreeImpl resources, InternalSyntaxToken closeParenToken,
    BlockTreeImpl block,
    Optional<List<CatchTreeImpl>> catches, Optional<TryStatementTreeImpl> finallyBlock) {

    InternalSyntaxToken tryKeyword = (InternalSyntaxToken) tryToken;
    List<CatchTreeImpl> catchTrees = catches.isPresent() ? catches.get() : ImmutableList.<CatchTreeImpl>of();
    if (finallyBlock.isPresent()) {
      return finallyBlock.get().completeTryWithResources(tryKeyword, openParenToken, resources, closeParenToken, block, catchTrees);
    } else {
      return new TryStatementTreeImpl(tryKeyword, openParenToken, resources, closeParenToken, block, catchTrees);
    }
  }

  public ResourceListTreeImpl newResources(List<Tuple<VariableTreeImpl, Optional<InternalSyntaxToken>>> rests) {
    ImmutableList.Builder<VariableTreeImpl> resources = ImmutableList.builder();

    for (Tuple<VariableTreeImpl, Optional<InternalSyntaxToken>> rest : rests) {
      VariableTreeImpl resource = rest.first();
      resources.add(resource);
      //FIXME SONARJAVA-547 handle semi colon separator
    }

    return new ResourceListTreeImpl(resources.build());
  }

  public VariableTreeImpl newResource(ModifiersTreeImpl modifiers, TypeTree classType, VariableTreeImpl partial, InternalSyntaxToken equalToken, ExpressionTree expression) {
    // FIXME SONARJAVA-547 handle modifiers
    return partial.completeTypeAndInitializer(classType, equalToken, expression);
  }

  public SwitchStatementTreeImpl switchStatement(JavaTree switchToken, InternalSyntaxToken openParenToken, ExpressionTree expression, InternalSyntaxToken closeParenToken,
                                                 InternalSyntaxToken openBraceToken, Optional<List<CaseGroupTreeImpl>> optionalGroups, InternalSyntaxToken closeBraceToken) {

    InternalSyntaxToken switchKeyword = (InternalSyntaxToken) switchToken;

    List<CaseGroupTreeImpl> groups = optionalGroups.isPresent() ? optionalGroups.get() : Collections.<CaseGroupTreeImpl>emptyList();

    return new SwitchStatementTreeImpl(switchKeyword, openParenToken, expression, closeParenToken,
      openBraceToken, groups, closeBraceToken);
  }

  public CaseGroupTreeImpl switchGroup(List<CaseLabelTreeImpl> labels, BlockStatementListTreeImpl blockStatements) {
    return new CaseGroupTreeImpl(labels, blockStatements);
  }

  public CaseLabelTreeImpl newCaseSwitchLabel(JavaTree caseSyntaxToken, ExpressionTree expression, InternalSyntaxToken colonSyntaxToken) {
    return new CaseLabelTreeImpl((InternalSyntaxToken) caseSyntaxToken, expression, colonSyntaxToken);
  }

  public CaseLabelTreeImpl newDefaultSwitchLabel(JavaTree defaultToken, InternalSyntaxToken colonToken) {
    InternalSyntaxToken defaultSyntaxToken = (InternalSyntaxToken) defaultToken;
    return new CaseLabelTreeImpl(defaultSyntaxToken, null, colonToken);
  }

  public SynchronizedStatementTreeImpl synchronizedStatement(JavaTree synchronizedToken, InternalSyntaxToken openParenToken, ExpressionTree expression, InternalSyntaxToken closeParenToken, BlockTreeImpl block) {
    InternalSyntaxToken synchronizedKeyword = (InternalSyntaxToken) synchronizedToken;
    return new SynchronizedStatementTreeImpl(synchronizedKeyword, openParenToken, expression, closeParenToken, block);
  }

  public BreakStatementTreeImpl breakStatement(JavaTree breakToken, Optional<JavaTree> identifierToken, InternalSyntaxToken semicolonSyntaxToken) {
    InternalSyntaxToken breakSyntaxToken = (InternalSyntaxToken) breakToken;
    IdentifierTreeImpl identifier = null;
    if (identifierToken.isPresent()) {
      identifier = new IdentifierTreeImpl((InternalSyntaxToken) identifierToken.get());
    }
    return new BreakStatementTreeImpl(breakSyntaxToken, identifier, semicolonSyntaxToken);
  }

  public ContinueStatementTreeImpl continueStatement(JavaTree continueToken, Optional<JavaTree> identifierToken, InternalSyntaxToken semicolonToken) {
    InternalSyntaxToken continueKeywordSyntaxToken = (InternalSyntaxToken) continueToken;
    IdentifierTreeImpl identifier = null;
    if (identifierToken.isPresent()) {
      identifier = new IdentifierTreeImpl((InternalSyntaxToken) identifierToken.get());
    }
    return new ContinueStatementTreeImpl(continueKeywordSyntaxToken, identifier, semicolonToken);
  }

  public ReturnStatementTreeImpl returnStatement(JavaTree returnToken, Optional<ExpressionTree> expression, InternalSyntaxToken semicolonSyntaxToken) {
    InternalSyntaxToken returnKeywordSyntaxToken = (InternalSyntaxToken) returnToken;
    ExpressionTree expressionTree = expression.isPresent() ? expression.get() : null;
    return new ReturnStatementTreeImpl(returnKeywordSyntaxToken, expressionTree, semicolonSyntaxToken);
  }

  public ThrowStatementTreeImpl throwStatement(JavaTree throwToken, ExpressionTree expression, InternalSyntaxToken semicolonToken) {
    return new ThrowStatementTreeImpl((InternalSyntaxToken) throwToken, expression, semicolonToken);
  }

  public LabeledStatementTreeImpl labeledStatement(JavaTree identifierToken, InternalSyntaxToken colon, StatementTree statement) {
    IdentifierTreeImpl identifier = new IdentifierTreeImpl((InternalSyntaxToken) identifierToken);
    return new LabeledStatementTreeImpl(identifier, colon, statement);
  }

  public ExpressionStatementTreeImpl expressionStatement(ExpressionTree expression, InternalSyntaxToken semicolonToken) {
    return new ExpressionStatementTreeImpl(expression, semicolonToken);
  }

  public EmptyStatementTreeImpl emptyStatement(InternalSyntaxToken semicolon) {
    return new EmptyStatementTreeImpl(semicolon);
  }

  public BlockStatementListTreeImpl blockStatements(Optional<List<BlockStatementListTreeImpl>> blockStatements) {
    ImmutableList.Builder<StatementTree> builder = ImmutableList.builder();

    if (blockStatements.isPresent()) {
      for (BlockStatementListTreeImpl blockStatement : blockStatements.get()) {
        builder.addAll(blockStatement);
      }
    }

    return new BlockStatementListTreeImpl(builder.build());
  }

  public BlockStatementListTreeImpl wrapInBlockStatements(VariableDeclaratorListTreeImpl variables) {
    return new BlockStatementListTreeImpl(variables);
  }

  public BlockStatementListTreeImpl newInnerClassOrEnum(ModifiersTreeImpl modifiers, ClassTreeImpl classTree) {
    classTree.completeModifiers(modifiers);
    return new BlockStatementListTreeImpl(ImmutableList.<StatementTree>of(classTree));
  }

  public BlockStatementListTreeImpl wrapInBlockStatements(StatementTree statement) {
    return new BlockStatementListTreeImpl(ImmutableList.of(statement));
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
          kindMaps.getAssignmentOperator((JavaPunctuator) lastOperator.getGrammarRuleKey()),
          operatorAndOperand.operand(),
          lastOperator,
          result);
      }

      lastOperator = operatorAndOperand.operator();
    }

    result = new AssignmentExpressionTreeImpl(
      kindMaps.getAssignmentOperator((JavaPunctuator) lastOperator.getGrammarRuleKey()),
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

  public ConditionalExpressionTreeImpl newTernaryExpression(InternalSyntaxToken queryToken, ExpressionTree trueExpression, InternalSyntaxToken colonToken, ExpressionTree falseExpression) {
    return new ConditionalExpressionTreeImpl(queryToken, trueExpression, colonToken, falseExpression);
  }

  public ExpressionTree completeInstanceofExpression(ExpressionTree expression, Optional<InstanceOfTreeImpl> partial) {
    return partial.isPresent() ?
      partial.get().complete(expression) :
      expression;
  }

  public InstanceOfTreeImpl newInstanceofExpression(JavaTree instanceofToken, TypeTree type) {
    return new InstanceOfTreeImpl((InternalSyntaxToken) instanceofToken, type);
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
        kindMaps.getBinaryOperator((JavaPunctuator) operatorAndOperand.operator().getGrammarRuleKey()),
        result,
        operatorAndOperand.operator(),
        operatorAndOperand.operand());
    }
    return result;
  }

  private OperatorAndOperand newOperatorAndOperand(InternalSyntaxToken operator, ExpressionTree operand) {
    return new OperatorAndOperand(operator, operand);
  }

  // TODO Allow to use the same method several times

  public OperatorAndOperand newOperatorAndOperand11(InternalSyntaxToken operator, ExpressionTree operand) {
    return newOperatorAndOperand(operator, operand);
  }

  public ExpressionTree binaryExpression10(ExpressionTree expression, Optional<List<OperatorAndOperand>> operatorAndOperands) {
    return binaryExpression(expression, operatorAndOperands);
  }

  public OperatorAndOperand newOperatorAndOperand10(InternalSyntaxToken operator, ExpressionTree operand) {
    return newOperatorAndOperand(operator, operand);
  }

  public ExpressionTree binaryExpression9(ExpressionTree expression, Optional<List<OperatorAndOperand>> operatorAndOperands) {
    return binaryExpression(expression, operatorAndOperands);
  }

  public OperatorAndOperand newOperatorAndOperand9(InternalSyntaxToken operator, ExpressionTree operand) {
    return newOperatorAndOperand(operator, operand);
  }

  public ExpressionTree binaryExpression8(ExpressionTree expression, Optional<List<OperatorAndOperand>> operatorAndOperands) {
    return binaryExpression(expression, operatorAndOperands);
  }

  public OperatorAndOperand newOperatorAndOperand8(InternalSyntaxToken operator, ExpressionTree operand) {
    return newOperatorAndOperand(operator, operand);
  }

  public ExpressionTree binaryExpression7(ExpressionTree expression, Optional<List<OperatorAndOperand>> operatorAndOperands) {
    return binaryExpression(expression, operatorAndOperands);
  }

  public OperatorAndOperand newOperatorAndOperand7(InternalSyntaxToken operator, ExpressionTree operand) {
    return newOperatorAndOperand(operator, operand);
  }

  public ExpressionTree binaryExpression6(ExpressionTree expression, Optional<List<OperatorAndOperand>> operatorAndOperands) {
    return binaryExpression(expression, operatorAndOperands);
  }

  public OperatorAndOperand newOperatorAndOperand6(InternalSyntaxToken operator, ExpressionTree operand) {
    return newOperatorAndOperand(operator, operand);
  }

  public ExpressionTree binaryExpression5(ExpressionTree expression, Optional<List<OperatorAndOperand>> operatorAndOperands) {
    return binaryExpression(expression, operatorAndOperands);
  }

  public OperatorAndOperand newOperatorAndOperand5(InternalSyntaxToken operator, ExpressionTree operand) {
    return newOperatorAndOperand(operator, operand);
  }

  public ExpressionTree binaryExpression4(ExpressionTree expression, Optional<List<OperatorAndOperand>> operatorAndOperands) {
    return binaryExpression(expression, operatorAndOperands);
  }

  public OperatorAndOperand newOperatorAndOperand4(InternalSyntaxToken operator, ExpressionTree operand) {
    return newOperatorAndOperand(operator, operand);
  }

  public ExpressionTree binaryExpression3(ExpressionTree expression, Optional<List<OperatorAndOperand>> operatorAndOperands) {
    return binaryExpression(expression, operatorAndOperands);
  }

  public OperatorAndOperand newOperatorAndOperand3(InternalSyntaxToken operator, ExpressionTree operand) {
    return newOperatorAndOperand(operator, operand);
  }

  public ExpressionTree binaryExpression2(ExpressionTree expression, Optional<List<OperatorAndOperand>> operatorAndOperands) {
    return binaryExpression(expression, operatorAndOperands);
  }

  public OperatorAndOperand newOperatorAndOperand2(InternalSyntaxToken operator, ExpressionTree operand) {
    return newOperatorAndOperand(operator, operand);
  }

  public ExpressionTree binaryExpression1(ExpressionTree expression, Optional<List<OperatorAndOperand>> operatorAndOperands) {
    return binaryExpression(expression, operatorAndOperands);
  }

  public OperatorAndOperand newOperatorAndOperand1(InternalSyntaxToken operator, ExpressionTree operand) {
    return newOperatorAndOperand(operator, operand);
  }

  public ExpressionTree newPrefixedExpression(InternalSyntaxToken operatorToken, ExpressionTree expression) {
    return new InternalPrefixUnaryExpression(kindMaps.getPrefixOperator((JavaPunctuator) operatorToken.getGrammarRuleKey()), operatorToken, expression);
  }

  public ExpressionTree newPostfixExpression(ExpressionTree expression, Optional<InternalSyntaxToken> postfixOperator) {
    ExpressionTree result = expression;

    if (postfixOperator.isPresent()) {
      InternalSyntaxToken postfixOperatorToken = postfixOperator.get();
      result = new InternalPostfixUnaryExpression(kindMaps.getPostfixOperator((JavaPunctuator) postfixOperator.get().getGrammarRuleKey()), result, postfixOperatorToken);
    }

    return result;
  }

  public ExpressionTree newTildaExpression(InternalSyntaxToken tildaToken, ExpressionTree expression) {
    return new InternalPrefixUnaryExpression(Kind.BITWISE_COMPLEMENT, tildaToken, expression);
  }

  public ExpressionTree newBangExpression(InternalSyntaxToken bangToken, ExpressionTree expression) {
    return new InternalPrefixUnaryExpression(Kind.LOGICAL_COMPLEMENT, bangToken, expression);
  }

  public ExpressionTree completeCastExpression(InternalSyntaxToken openParenToken, TypeCastExpressionTreeImpl partial) {
    return partial.complete(openParenToken);
  }

  public TypeCastExpressionTreeImpl newBasicTypeCastExpression(PrimitiveTypeTreeImpl basicType, InternalSyntaxToken closeParenToken, ExpressionTree expression) {
    return new TypeCastExpressionTreeImpl(basicType, closeParenToken, expression);
  }


  public TypeCastExpressionTreeImpl newClassCastExpression(TypeTree type, Optional<List<Tuple<InternalSyntaxToken, Tree>>> classTypes, InternalSyntaxToken closeParenToken, ExpressionTree expression) {
    ImmutableList.Builder<Tree> boundsBuilder = ImmutableList.builder();
    if (classTypes.isPresent()) {
      for (Tuple<InternalSyntaxToken, Tree> tuple : classTypes.get()) {
        // TODO SONARJAVA-547 andOperator should be present in the tree
        InternalSyntaxToken andOperator = tuple.first();
        boundsBuilder.add(tuple.second());
      }
    }

    return new TypeCastExpressionTreeImpl(type, boundsBuilder.build(), closeParenToken, expression);
  }

  public ExpressionTree completeMethodReference(MethodReferenceTreeImpl partial, Optional<TypeArgumentListTreeImpl> typeArguments, JavaTree newOrIdentifierToken) {
    TypeArguments typeArgs = null;
    if (typeArguments.isPresent()) {
      typeArgs = typeArguments.get();
    }
    InternalSyntaxToken newOrIdentifierSyntaxToken = (InternalSyntaxToken) newOrIdentifierToken;
    partial.complete(typeArgs, new IdentifierTreeImpl(newOrIdentifierSyntaxToken));
    return partial;
  }

  public MethodReferenceTreeImpl newSuperMethodReference(JavaTree superToken, InternalSyntaxToken doubleColonToken) {
    IdentifierTree superIdentifier = new IdentifierTreeImpl((InternalSyntaxToken) superToken);
    return new MethodReferenceTreeImpl(superIdentifier, doubleColonToken);
  }

  public MethodReferenceTreeImpl newTypeMethodReference(Tree type, InternalSyntaxToken doubleColonToken) {
    return new MethodReferenceTreeImpl(type, doubleColonToken);
  }

  public MethodReferenceTreeImpl newPrimaryMethodReference(ExpressionTree expression, InternalSyntaxToken doubleColonToken) {
    return new MethodReferenceTreeImpl(expression, doubleColonToken);
  }

  public ExpressionTree lambdaExpression(LambdaParameterListTreeImpl parameters, JavaTree arrowToken, Tree body) {
    return new LambdaExpressionTreeImpl(
      parameters.openParenToken(),
      ImmutableList.<VariableTree>builder().addAll(parameters).build(),
      parameters.closeParenToken(),
      (InternalSyntaxToken) arrowToken,
      body);
  }

  public LambdaParameterListTreeImpl newInferedParameters(
    InternalSyntaxToken openParenToken,
    Optional<Tuple<VariableTreeImpl, Optional<List<Tuple<InternalSyntaxToken, VariableTreeImpl>>>>> identifiersOpt,
    InternalSyntaxToken closeParenToken) {

    ImmutableList.Builder<VariableTreeImpl> params = ImmutableList.builder();

    if (identifiersOpt.isPresent()) {
      Tuple<VariableTreeImpl, Optional<List<Tuple<InternalSyntaxToken, VariableTreeImpl>>>> identifiers = identifiersOpt.get();

      VariableTreeImpl variable = identifiers.first();
      params.add(variable);

      VariableTreeImpl previousVariable = variable;
      if (identifiers.second().isPresent()) {
        for (Tuple<InternalSyntaxToken, VariableTreeImpl> identifier : identifiers.second().get()) {
          variable = identifier.second();
          params.add(variable);

          InternalSyntaxToken comma = identifier.first();
          previousVariable.setEndToken(comma);
          previousVariable = variable;
        }
      }
    }

    return new LambdaParameterListTreeImpl(openParenToken, params.build(), closeParenToken);
  }

  public LambdaParameterListTreeImpl formalLambdaParameters(FormalParametersListTreeImpl formalParameters) {
    return new LambdaParameterListTreeImpl(formalParameters.openParenToken(), formalParameters, formalParameters.closeParenToken());
  }

  public LambdaParameterListTreeImpl singleInferedParameter(VariableTreeImpl parameter) {
    return new LambdaParameterListTreeImpl(null, ImmutableList.of(parameter), null);
  }

  public VariableTreeImpl newSimpleParameter(JavaTree identifierToken) {
    IdentifierTreeImpl identifier = new IdentifierTreeImpl((InternalSyntaxToken) identifierToken);
    return new VariableTreeImpl(identifier);
  }

  public ParenthesizedTreeImpl parenthesizedExpression(InternalSyntaxToken leftParenSyntaxToken, ExpressionTree expression, InternalSyntaxToken rightParenSyntaxToken) {
    return new ParenthesizedTreeImpl(leftParenSyntaxToken, expression, rightParenSyntaxToken);
  }

  public ExpressionTree newExpression(JavaTree newToken, Optional<List<AnnotationTreeImpl>> annotations, ExpressionTree partial) {
    // FIXME SONARJAVA-547 Handle annotations
    InternalSyntaxToken newSyntaxToken = (InternalSyntaxToken) newToken;
    if (partial.is(Tree.Kind.NEW_CLASS)) {
      ((NewClassTreeImpl) partial).completeWithNewKeyword(newSyntaxToken);
    } else {
      ((NewArrayTreeImpl) partial).completeWithNewKeyword(newSyntaxToken);
    }
    return partial;
  }

  public ExpressionTree completeCreator(Optional<TypeArgumentListTreeImpl> typeArguments, ExpressionTree partial) {
    // TODO typeArguments is a parameterized expression used to chose which constructor to call
    // FIXME SONARJAVA-547 Handle type arguments
    return partial;
  }

  public ExpressionTree newClassCreator(TypeTree qualifiedIdentifier, NewClassTreeImpl classCreatorRest) {
    return classCreatorRest.completeWithIdentifier(qualifiedIdentifier);
  }

  public ExpressionTree newArrayCreator(TypeTree type, NewArrayTreeImpl partial) {
    return partial.complete(type);
  }

  public NewArrayTreeImpl completeArrayCreator(Optional<List<AnnotationTreeImpl>> annotations, NewArrayTreeImpl partial) {
    // FIXME SONARJAVA-547 Handle annotations
    return partial;
  }

  public NewArrayTreeImpl newArrayCreatorWithInitializer(
    InternalSyntaxToken openBracketToken, InternalSyntaxToken closeBracketToken,
    Optional<List<Tuple<InternalSyntaxToken, InternalSyntaxToken>>> dimensions,
    NewArrayTreeImpl partial) {

    // FIXME SONARJAVA-547 Handle brackets for dimensions
    return partial;
  }

  public NewArrayTreeImpl newArrayCreatorWithDimension(InternalSyntaxToken openBracketToken, ExpressionTree expression, InternalSyntaxToken closeBracketToken,
    Optional<List<ArrayAccessExpressionTreeImpl>> arrayAccesses,
    Optional<List<Tuple<Optional<List<AnnotationTreeImpl>>, Tuple<InternalSyntaxToken, InternalSyntaxToken>>>> dims) {

    ImmutableList.Builder<ExpressionTree> dimensions = ImmutableList.builder();
    dimensions.add(expression);
    if (arrayAccesses.isPresent()) {
      for (ArrayAccessExpressionTreeImpl arrayAccess : arrayAccesses.get()) {
        dimensions.add(arrayAccess.index());
      }
    }
    // TODO SONARJAVA-547 brackets should be stored (dims parameter should be used).
    return new NewArrayTreeImpl(dimensions.build(), ImmutableList.<ExpressionTree>of());
  }

  public ExpressionTree basicClassExpression(PrimitiveTypeTreeImpl basicType, Optional<List<Tuple<InternalSyntaxToken, InternalSyntaxToken>>> dimensions,
                                             InternalSyntaxToken dotToken, JavaTree classToken) {
    // 15.8.2. Class Literals
    // int.class
    // int[].class

    IdentifierTreeImpl classIdentifier = new IdentifierTreeImpl((InternalSyntaxToken) classToken);
    ArrayTypeTreeImpl nestedDimensions = newArrayTypeTree(dimensions);
    TypeTree typeTree = applyDim(basicType, nestedDimensions);
    return new MemberSelectExpressionTreeImpl((ExpressionTree) typeTree, dotToken, classIdentifier);
  }

  public PrimitiveTypeTreeImpl newBasicType(Optional<List<AnnotationTreeImpl>> annotations, JavaTree basicType) {
    // TODO SONARJAVA-1167 annotations on primitiveTypeTree are lost
    return new JavaTree.PrimitiveTypeTreeImpl((InternalSyntaxToken) basicType);
  }

  public ArgumentListTreeImpl completeArguments(InternalSyntaxToken openParenthesisToken, Optional<ArgumentListTreeImpl> expressions, InternalSyntaxToken closeParenthesisToken) {
    return expressions.isPresent() ?
      expressions.get().complete(openParenthesisToken, closeParenthesisToken) :
      new ArgumentListTreeImpl(openParenthesisToken, closeParenthesisToken);
  }

  public ArgumentListTreeImpl newArguments(ExpressionTree expression, Optional<List<Tuple<InternalSyntaxToken, ExpressionTree>>> rests) {
    ImmutableList.Builder<ExpressionTree> expressions = ImmutableList.builder();

    expressions.add(expression);
    if (rests.isPresent()) {
      for (Tuple<InternalSyntaxToken, ExpressionTree> rest : rests.get()) {
        //FIXME : SONARJAVA-547 Comma should be handled : rest.first()
        expressions.add(rest.second());
      }
    }

    return new ArgumentListTreeImpl(expressions.build());
  }

  public TypeTree annotationIdentifier(JavaTree firstIdentifier, Optional<List<Tuple<InternalSyntaxToken, JavaTree>>> rests) {
    List<JavaTree> children = Lists.newArrayList();
    children.add(firstIdentifier);
    if (rests.isPresent()) {
      for (Tuple<InternalSyntaxToken, JavaTree> rest : rests.get()) {
        children.add(rest.first());
        children.add(rest.second());
      }
    }

    JavaTree result = null;

    InternalSyntaxToken dotToken = null;
    for (JavaTree child : children) {
      if (!child.getGrammarRuleKey().equals(JavaTokenType.IDENTIFIER)) {
        dotToken = (InternalSyntaxToken) child;
      } else {
        InternalSyntaxToken identifierToken = (InternalSyntaxToken) child;

        if (result == null) {
          result = new IdentifierTreeImpl(identifierToken);
        } else {
          IdentifierTreeImpl identifier = new IdentifierTreeImpl(identifierToken);
          result = new MemberSelectExpressionTreeImpl((ExpressionTree) result, dotToken, identifier);
        }
      }
    }

    return (TypeTree) result;
  }

  public <T extends Tree> T newQualifiedIdentifier(ExpressionTree firstIdentifier, Optional<List<Tuple<InternalSyntaxToken, ExpressionTree>>> rests) {
    ExpressionTree result = firstIdentifier;

    if (rests.isPresent()) {
      for (Tuple<InternalSyntaxToken, ExpressionTree> rest : rests.get()) {
        InternalSyntaxToken dotToken = rest.first();
        if (rest.second().is(Kind.IDENTIFIER)) {
          result = new MemberSelectExpressionTreeImpl(result, dotToken, (IdentifierTreeImpl) rest.second());
        } else if (rest.second().is(Kind.PARAMETERIZED_TYPE)) {
          ParameterizedTypeTreeImpl parameterizedType = (ParameterizedTypeTreeImpl) rest.second();
          IdentifierTreeImpl identifier = (IdentifierTreeImpl) parameterizedType.type();

          result = new MemberSelectExpressionTreeImpl(result, dotToken, identifier);
          result = new ParameterizedTypeTreeImpl((TypeTree) result, (TypeArgumentListTreeImpl) parameterizedType.typeArguments());
        } else {
          throw new IllegalArgumentException();
        }
      }
    }

    return (T) result;
  }

  public ExpressionTree newAnnotatedParameterizedIdentifier(
    Optional<List<AnnotationTreeImpl>> annotations, JavaTree identifierToken, Optional<TypeArgumentListTreeImpl> typeArguments) {

    ExpressionTree result = new IdentifierTreeImpl((InternalSyntaxToken) identifierToken);

    // FIXME SONARJAVA-547 Handle dimensions

    if (typeArguments.isPresent()) {
      result = new ParameterizedTypeTreeImpl((TypeTree) result, typeArguments.get());
    }

    return result;
  }

  public NewArrayTreeImpl newArrayInitializer(InternalSyntaxToken openBraceToken, Optional<List<Tuple<ExpressionTree, Optional<InternalSyntaxToken>>>> rests, InternalSyntaxToken closeBraceToken) {
    ImmutableList.Builder<ExpressionTree> initializers = ImmutableList.builder();
    if (rests.isPresent()) {
      for (Tuple<ExpressionTree, Optional<InternalSyntaxToken>> rest : rests.get()) {
        //FIXME SONARJAVA-547 commas should be handled.
        initializers.add(rest.first());
      }
    }
    return new NewArrayTreeImpl(ImmutableList.<ExpressionTree>of(), initializers.build()).completeWithCurlyBraces(openBraceToken, closeBraceToken);
  }

  public QualifiedIdentifierListTreeImpl newQualifiedIdentifierList(TypeTree qualifiedIdentifier, Optional<List<Tuple<InternalSyntaxToken, TypeTree>>> rests) {
    ImmutableList.Builder<TypeTree> qualifiedIdentifiers = ImmutableList.builder();

    qualifiedIdentifiers.add(qualifiedIdentifier);

    if (rests.isPresent()) {
      for (Tuple<InternalSyntaxToken, TypeTree> rest : rests.get()) {
        qualifiedIdentifiers.add(rest.second());
      }
    }

    return new QualifiedIdentifierListTreeImpl(qualifiedIdentifiers.build());
  }

  public ArrayAccessExpressionTreeImpl newArrayAccessExpression(Optional<List<AnnotationTreeImpl>> annotations, InternalSyntaxToken openBracketToken, ExpressionTree index,
                                                                InternalSyntaxToken closeBracketToken) {
    // FIXME SONARJAVA-547 Handle annotations
    return new ArrayAccessExpressionTreeImpl(openBracketToken, index, closeBracketToken);
  }

  public NewClassTreeImpl newClassCreatorRest(ArgumentListTreeImpl arguments, Optional<ClassTreeImpl> classBody) {
    return new NewClassTreeImpl(arguments.openParenToken(), arguments, arguments.closeParenToken(), classBody.isPresent() ? classBody.get() : null);
  }

  public ExpressionTree newIdentifierOrMethodInvocation(Optional<TypeArgumentListTreeImpl> typeArguments, JavaTree identifierToken, Optional<ArgumentListTreeImpl> arguments) {
    // FIXME SONARJAVA-547 Handle type arguments
    IdentifierTreeImpl identifier = new IdentifierTreeImpl((InternalSyntaxToken) identifierToken);
    ExpressionTree result = identifier;
    if (arguments.isPresent()) {
      result = new MethodInvocationTreeImpl(identifier, typeArguments.orNull(), arguments.get());
    }
    return result;
  }

  public Tuple<Optional<InternalSyntaxToken>, ExpressionTree> completeMemberSelectOrMethodSelector(InternalSyntaxToken dotToken, ExpressionTree partial) {
    return newTuple(Optional.of(dotToken), partial);
  }

  public Tuple<Optional<InternalSyntaxToken>, ExpressionTree> completeCreatorSelector(InternalSyntaxToken dotToken, ExpressionTree partial) {
    ((NewClassTreeImpl) partial).completeWithDotToken(dotToken);
    return newTuple(Optional.<InternalSyntaxToken>absent(), partial);
  }

  public ExpressionTree newDotClassSelector(Optional<List<Tuple<InternalSyntaxToken, InternalSyntaxToken>>> dimensions, InternalSyntaxToken dotToken, JavaTree classToken) {
    IdentifierTreeImpl identifier = new IdentifierTreeImpl((InternalSyntaxToken) classToken);

    ArrayTypeTreeImpl nestedDimensions = newArrayTypeTree(dimensions);
    return new MemberSelectExpressionTreeImpl(nestedDimensions, dotToken, identifier);
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
            result = new MemberSelectExpressionTreeImpl(result, dotToken, identifier);
          } else {
            MethodInvocationTreeImpl methodInvocation = (MethodInvocationTreeImpl) selector;
            IdentifierTreeImpl identifier = (IdentifierTreeImpl) methodInvocation.methodSelect();
            MemberSelectExpressionTreeImpl memberSelect = new MemberSelectExpressionTreeImpl(result, dotToken, identifier);

            result = new MethodInvocationTreeImpl(memberSelect, methodInvocation.typeArguments(), (ArgumentListTreeImpl) methodInvocation.arguments());
          }
        } else if (selector.is(Kind.NEW_CLASS)) {
          NewClassTreeImpl newClass = (NewClassTreeImpl) selector;
          result = newClass.completeWithEnclosingExpression(result);
        } else if (selector.is(Kind.ARRAY_ACCESS_EXPRESSION)) {
          ArrayAccessExpressionTreeImpl arrayAccess = (ArrayAccessExpressionTreeImpl) selector;
          result = arrayAccess.complete(result);
        } else if (selector.is(Kind.MEMBER_SELECT)) {
          MemberSelectExpressionTreeImpl memberSelect = (MemberSelectExpressionTreeImpl) selector;
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

  public static class Tuple<T, U>{
    private final T first;
    private final U second;

    public Tuple(T first, U second) {
      this.first = first;
      this.second = second;
    }

    public T first() {
      return first;
    }

    public U second() {
      return second;
    }
  }

  private <T, U> Tuple<T, U> newTuple(T first, U second) {
    return new Tuple<>(first, second);
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

  public <T, U> Tuple<T, U> newTuple18(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple19(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple20(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple21(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple22(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple23(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple24(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple25(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple26(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple27(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple28(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple29(T first, U second) {
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

  public Tuple<InternalSyntaxToken, Tree> newAdditionalBound(InternalSyntaxToken andSyntaxToken, Tree type) {
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
  private static ArrayTypeTreeImpl newArrayTypeTreeWithAnnotations(Optional<List<Tuple<Optional<List<AnnotationTreeImpl>>, Tuple<InternalSyntaxToken, InternalSyntaxToken>>>> dims) {
    ArrayTypeTreeImpl result = null;
    if (dims.isPresent()) {
      for (Tuple<Optional<List<AnnotationTreeImpl>>, Tuple<InternalSyntaxToken, InternalSyntaxToken>> dim : dims.get()) {
        result = newArrayTypeTreeWithAnnotations(result, dim);
      }
    }
    return result;
  }

  private static ArrayTypeTreeImpl newArrayTypeTreeWithAnnotations(TypeTree type, Tuple<Optional<List<AnnotationTreeImpl>>, Tuple<InternalSyntaxToken, InternalSyntaxToken>> dim) {
    List<AnnotationTreeImpl> annotations = dim.first().isPresent() ? dim.first().get() : ImmutableList.<AnnotationTreeImpl>of();
    InternalSyntaxToken openBracketToken = dim.second().first();
    InternalSyntaxToken closeBracketToken = dim.second().second();
    return new ArrayTypeTreeImpl(type, annotations, openBracketToken, closeBracketToken);
  }

  @CheckForNull
  private static ArrayTypeTreeImpl newArrayTypeTree(Optional<List<Tuple<InternalSyntaxToken, InternalSyntaxToken>>> dims) {
    ArrayTypeTreeImpl result = null;
    if (dims.isPresent()) {
      for (Tuple<InternalSyntaxToken, InternalSyntaxToken> dim : dims.get()) {
        InternalSyntaxToken openBracketToken = dim.first();
        InternalSyntaxToken closeBracketToken = dim.second();
        result = new ArrayTypeTreeImpl(result, ImmutableList.<AnnotationTreeImpl>of(), openBracketToken, closeBracketToken);
      }
    }
    return result;
  }
}
