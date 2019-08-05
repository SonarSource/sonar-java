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
package org.sonar.java.ast.parser;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.sonar.sslr.api.typed.Optional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.api.JavaRestrictedKeyword;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.model.ArrayDimensionTreeImpl;
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
import org.sonar.java.model.declaration.ExportsDirectiveTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.ModifierKeywordTreeImpl;
import org.sonar.java.model.declaration.ModifiersTreeImpl;
import org.sonar.java.model.declaration.ModuleDeclarationTreeImpl;
import org.sonar.java.model.declaration.ModuleNameListTreeImpl;
import org.sonar.java.model.declaration.OpensDirectiveTreeImpl;
import org.sonar.java.model.declaration.ProvidesDirectiveTreeImpl;
import org.sonar.java.model.declaration.RequiresDirectiveTreeImpl;
import org.sonar.java.model.declaration.UsesDirectiveTreeImpl;
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
import org.sonar.java.model.expression.VarTypeTreeImpl;
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
import org.sonar.java.model.statement.SwitchExpressionTreeImpl;
import org.sonar.java.model.statement.SwitchStatementTreeImpl;
import org.sonar.java.model.statement.SynchronizedStatementTreeImpl;
import org.sonar.java.model.statement.ThrowStatementTreeImpl;
import org.sonar.java.model.statement.TryStatementTreeImpl;
import org.sonar.java.model.statement.WhileStatementTreeImpl;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ArrayDimensionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportClauseTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifierTree;
import org.sonar.plugins.java.api.tree.ModuleDeclarationTree;
import org.sonar.plugins.java.api.tree.ModuleDirectiveTree;
import org.sonar.plugins.java.api.tree.ModuleNameTree;
import org.sonar.plugins.java.api.tree.PackageDeclarationTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SwitchExpressionTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

public class TreeFactory {

  private final KindMaps kindMaps = new KindMaps();

  public ModifiersTreeImpl modifiers(Optional<List<ModifierTree>> modifierNodes) {
    if (!modifierNodes.isPresent()) {
      return ModifiersTreeImpl.emptyModifiers();
    }
    return new ModifiersTreeImpl(modifierNodes.get());
  }

  public ModifierKeywordTreeImpl modifierKeyword(InternalSyntaxToken token) {
    JavaKeyword keyword = (JavaKeyword) token.getGrammarRuleKey();
    return new ModifierKeywordTreeImpl(kindMaps.getModifier(keyword), token);
  }

  // Literals

  public ExpressionTree literal(InternalSyntaxToken token) {
    return new LiteralTreeImpl(kindMaps.getLiteral(token.getGrammarRuleKey()), token);
  }

  // End of literals

  // Compilation unit

  public CompilationUnitTreeImpl newCompilationUnit(
    JavaTree spacing,
    Optional<PackageDeclarationTree> packageDeclaration,
    Optional<List<ImportClauseTree>> importDeclarations,
    Optional<ModuleDeclarationTree> moduleDeclaration,
    Optional<List<Tree>> typeDeclarations,
    InternalSyntaxToken eof) {

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

    return new CompilationUnitTreeImpl(
      packageDeclaration.orNull(),
      imports.build(),
      types.build(),
      moduleDeclaration.orNull(),
      eof);
  }

  public PackageDeclarationTreeImpl newPackageDeclaration(Optional<List<AnnotationTreeImpl>> annotations, InternalSyntaxToken packageToken, ExpressionTree qualifiedIdentifier,
    InternalSyntaxToken semicolonToken) {
    List<AnnotationTree> annotationList = ImmutableList.copyOf(annotations.or(Collections.emptyList()));
    return new PackageDeclarationTreeImpl(annotationList, packageToken, qualifiedIdentifier, semicolonToken);
  }

  public ModuleDeclarationTree newModuleDeclaration(Optional<List<AnnotationTreeImpl>> annotations, Optional<InternalSyntaxToken> openToken, InternalSyntaxToken moduleToken,
    ModuleNameTree moduleName, InternalSyntaxToken openBraceToken, Optional<List<ModuleDirectiveTree>> moduleDirectives, InternalSyntaxToken closeBraceToken) {
    List<AnnotationTree> annotationList = ImmutableList.copyOf(annotations.or(Collections.emptyList()));
    List<ModuleDirectiveTree> moduleDirectiveList = ImmutableList.copyOf(moduleDirectives.or(Collections.emptyList()));
    return new ModuleDeclarationTreeImpl(annotationList, openToken.orNull(), moduleToken, moduleName, openBraceToken, moduleDirectiveList, closeBraceToken);
  }

  public ModuleNameTree newModuleName(InternalSyntaxToken firstIdentifier, Optional<List<Tuple<InternalSyntaxToken, InternalSyntaxToken>>> rest) {
    List<IdentifierTree> identifiers = new ArrayList<>();
    List<SyntaxToken> separators = new ArrayList<>();
    identifiers.add(new IdentifierTreeImpl(firstIdentifier));

    if (rest.isPresent()) {
      for (Tuple<InternalSyntaxToken, InternalSyntaxToken> modulePart : rest.get()) {
        separators.add(modulePart.first());
        identifiers.add(new IdentifierTreeImpl(modulePart.second()));
      }
    }
    return new ModuleNameTreeImpl(Collections.unmodifiableList(identifiers), Collections.unmodifiableList(separators));
  }

  public <T, U> Tuple<T, U> moduleNameRest(T dotToken, U identifier) {
    return newTuple(dotToken, identifier);
  }

  public ModuleDirectiveTree newRequiresModuleDirective(InternalSyntaxToken requiresToken, InternalSyntaxToken transitiveTokenAsModuleName, InternalSyntaxToken semicolonToken) {
    return new RequiresDirectiveTreeImpl(requiresToken, ModifiersTreeImpl.emptyModifiers(), transitiveModuleName(transitiveTokenAsModuleName), semicolonToken);
  }

  public ModuleDirectiveTree newRequiresModuleDirective(InternalSyntaxToken requiresToken, InternalSyntaxToken staticModifier, InternalSyntaxToken transitiveTokenAsModuleName,
    InternalSyntaxToken semicolonToken) {
    ModifierKeywordTreeImpl staticModifierTree = new ModifierKeywordTreeImpl(Modifier.STATIC, staticModifier);
    ModifiersTreeImpl modifiers = new ModifiersTreeImpl(Collections.singletonList(staticModifierTree));
    return new RequiresDirectiveTreeImpl(requiresToken, modifiers, transitiveModuleName(transitiveTokenAsModuleName), semicolonToken);
  }

  private static ModuleNameTree transitiveModuleName(InternalSyntaxToken transitiveTokenAsModuleName) {
    IdentifierTree transitiveModuleName = new IdentifierTreeImpl(transitiveTokenAsModuleName);
    return new ModuleNameTreeImpl(Collections.singletonList(transitiveModuleName), Collections.emptyList());
  }

  public ModuleDirectiveTree newRequiresModuleDirective(InternalSyntaxToken requiresToken, Optional<List<InternalSyntaxToken>> modifiers, ModuleNameTree moduleName,
    InternalSyntaxToken semicolonToken) {
    ModifiersTreeImpl newModifiers = ModifiersTreeImpl.emptyModifiers();
    if (modifiers.isPresent()) {
      List<ModifierTree> modifierKeywords = new ArrayList<>();
      // JLS9 - §7.7.1 'requires' only 'static' and 'transitive' modifiers are allowed
      for (InternalSyntaxToken modifierAsSyntaxToken : modifiers.get()) {
        if (JavaRestrictedKeyword.TRANSITIVE.getValue().equals(modifierAsSyntaxToken.text())) {
          modifierKeywords.add(new ModifierKeywordTreeImpl(Modifier.TRANSITIVE, modifierAsSyntaxToken));
        } else {
          modifierKeywords.add(new ModifierKeywordTreeImpl(Modifier.STATIC, modifierAsSyntaxToken));
        }
      }
      newModifiers = new ModifiersTreeImpl(modifierKeywords);
    }
    return new RequiresDirectiveTreeImpl(requiresToken, newModifiers, moduleName, semicolonToken);
  }

  public ModuleDirectiveTree newExportsModuleDirective(InternalSyntaxToken exportsKeyword, ExpressionTree packageName,
    Optional<Tuple<InternalSyntaxToken, ListTreeImpl<ModuleNameTree>>> moduleNames, InternalSyntaxToken semicolonToken) {
    InternalSyntaxToken toKeyword = null;
    ListTreeImpl<ModuleNameTree> otherModuleNames = ModuleNameListTreeImpl.emptyList();
    if (moduleNames.isPresent()) {
      Tuple<InternalSyntaxToken, ListTreeImpl<ModuleNameTree>> toModuleNames = moduleNames.get();
      toKeyword = toModuleNames.first();
      otherModuleNames = toModuleNames.second();
    }
    return new ExportsDirectiveTreeImpl(exportsKeyword, packageName, toKeyword, otherModuleNames, semicolonToken);
  }

  public <T, U> Tuple<T, U> toModuleNames(T toToken, U moduleNames) {
    return newTuple(toToken, moduleNames);
  }

  public ModuleNameListTreeImpl newModuleNameListTreeImpl(ModuleNameTree firstModuleName, Optional<List<Tuple<InternalSyntaxToken, ModuleNameTree>>> rest) {
    List<ModuleNameTree> moduleNames = new ArrayList<>();
    List<SyntaxToken> separators = new ArrayList<>();
    moduleNames.add(firstModuleName);
    if (rest.isPresent()) {
      for(Tuple<InternalSyntaxToken, ModuleNameTree> tuple : rest.get()) {
        separators.add(tuple.first());
        moduleNames.add(tuple.second());
      }
    }
    return new ModuleNameListTreeImpl(Collections.unmodifiableList(moduleNames), Collections.unmodifiableList(separators));
  }

  public <T, U> Tuple<T, U> moduleNamesRest(T toToken, U moduleNames) {
    return newTuple(toToken, moduleNames);
  }

  public ModuleDirectiveTree newOpensModuleDirective(InternalSyntaxToken opensKeyword, ExpressionTree packageName,
    Optional<Tuple<InternalSyntaxToken, ListTreeImpl<ModuleNameTree>>> moduleNames, InternalSyntaxToken semicolonToken) {
    InternalSyntaxToken toKeyword = null;
    ListTreeImpl<ModuleNameTree> otherModuleNames = ModuleNameListTreeImpl.emptyList();
    if (moduleNames.isPresent()) {
      Tuple<InternalSyntaxToken, ListTreeImpl<ModuleNameTree>> toModuleNames = moduleNames.get();
      toKeyword = toModuleNames.first();
      otherModuleNames = toModuleNames.second();
    }
    return new OpensDirectiveTreeImpl(opensKeyword, packageName, toKeyword, otherModuleNames, semicolonToken);
  }

  public <T, U> Tuple<T, U> toModuleNames2(T toToken, U moduleNames) {
    return newTuple(toToken, moduleNames);
  }

  public ModuleDirectiveTree newUsesModuleDirective(InternalSyntaxToken usesKeyword, TypeTree typeName, InternalSyntaxToken semicolonToken) {
    return new UsesDirectiveTreeImpl(usesKeyword, typeName, semicolonToken);
  }

  public ModuleDirectiveTree newProvidesModuleDirective(InternalSyntaxToken providesKeyword, TypeTree typeName,
    InternalSyntaxToken withKeyword, QualifiedIdentifierListTreeImpl typeNames, InternalSyntaxToken semicolonToken) {
    return new ProvidesDirectiveTreeImpl(providesKeyword, typeName, withKeyword, typeNames, semicolonToken);
  }

  public ImportClauseTree newEmptyImport(InternalSyntaxToken semicolonToken) {
    return new EmptyStatementTreeImpl(semicolonToken);
  }

  public ImportTreeImpl newImportDeclaration(InternalSyntaxToken importToken, Optional<InternalSyntaxToken> staticToken, ExpressionTree qualifiedIdentifier,
    Optional<Tuple<InternalSyntaxToken, InternalSyntaxToken>> dotStar,
    InternalSyntaxToken semicolonToken) {

    ExpressionTree target = qualifiedIdentifier;
    if (dotStar.isPresent()) {
      IdentifierTreeImpl identifier = new IdentifierTreeImpl(dotStar.get().second());
      InternalSyntaxToken dotToken = dotStar.get().first();
      target = new MemberSelectExpressionTreeImpl(qualifiedIdentifier, dotToken, identifier);
    }

    InternalSyntaxToken staticKeyword = staticToken.orNull();
    return new ImportTreeImpl(importToken, staticKeyword, target, semicolonToken);
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

  public VarTypeTreeImpl newVarType(InternalSyntaxToken varToken) {
    // JLS10 - $14.4
    return new VarTypeTreeImpl(varToken);
  }

  public TypeArgumentListTreeImpl newTypeArgumentList(InternalSyntaxToken openBracketToken,
    Tree typeArgument, Optional<List<Tuple<InternalSyntaxToken, Tree>>> rests, InternalSyntaxToken closeBracketToken) {
    ImmutableList.Builder<Tree> typeArguments = ImmutableList.builder();
    ImmutableList.Builder<SyntaxToken> separators = ImmutableList.builder();

    typeArguments.add(typeArgument);

    if (rests.isPresent()) {
      for (Tuple<InternalSyntaxToken, Tree> rest : rests.get()) {
        separators.add(rest.first());
        typeArguments.add(rest.second());
      }
    }
    return new TypeArgumentListTreeImpl(openBracketToken, typeArguments.build(), separators.build(), closeBracketToken);
  }

  public TypeArgumentListTreeImpl newDiamondTypeArgument(InternalSyntaxToken openBracketToken, InternalSyntaxToken closeBracketToken) {
    return new TypeArgumentListTreeImpl(openBracketToken, Collections.emptyList(), Collections.emptyList(), closeBracketToken);
  }

  public Tree completeTypeArgument(Optional<List<AnnotationTreeImpl>> annotations, TypeTree partial) {
    completeTypeTreeWithAnnotations(partial, annotations);
    return partial;
  }

  public TypeTree newBasicTypeArgument(TypeTree type) {
    return type;
  }

  public WildcardTreeImpl completeWildcardTypeArgument(Optional<List<AnnotationTree>> annotations, InternalSyntaxToken queryToken, Optional<WildcardTreeImpl> partial) {
    WildcardTreeImpl result = partial.isPresent() ?
      partial.get().complete(queryToken) :
      new WildcardTreeImpl(queryToken);
    if (annotations.isPresent()) {
      result.complete(annotations.get());
    }
    return result;
  }

  public WildcardTreeImpl newWildcardTypeArguments(InternalSyntaxToken extendsOrSuperToken, Optional<List<AnnotationTreeImpl>> annotations, TypeTree type) {

    completeTypeTreeWithAnnotations(type, annotations);

    return new WildcardTreeImpl(
      JavaKeyword.EXTENDS.getValue().equals(extendsOrSuperToken.text()) ? Kind.EXTENDS_WILDCARD : Kind.SUPER_WILDCARD,
      extendsOrSuperToken,
      type);
  }

  public TypeParameterListTreeImpl newTypeParameterList(InternalSyntaxToken openBracketToken, TypeParameterTreeImpl typeParameter, Optional<List<Tuple<InternalSyntaxToken,
    TypeParameterTreeImpl>>> rests, InternalSyntaxToken closeBracketToken) {
    ImmutableList.Builder<TypeParameterTree> typeParameters = ImmutableList.builder();
    typeParameters.add(typeParameter);

    ImmutableList.Builder<SyntaxToken> separators = ImmutableList.builder();
    if (rests.isPresent()) {
      for (Tuple<InternalSyntaxToken, TypeParameterTreeImpl> rest : rests.get()) {
        separators.add(rest.first());
        typeParameters.add(rest.second());
      }
    }

    return new TypeParameterListTreeImpl(openBracketToken, typeParameters.build(), separators.build(), closeBracketToken);
  }

  public TypeParameterTreeImpl completeTypeParameter(Optional<List<AnnotationTreeImpl>> annotations, InternalSyntaxToken identifierToken, Optional<TypeParameterTreeImpl> partial) {
    IdentifierTreeImpl identifier = new IdentifierTreeImpl(identifierToken);
    completeTypeTreeWithAnnotations(identifier, annotations);
    return partial.isPresent() ?
      partial.get().complete(identifier) :
      new TypeParameterTreeImpl(identifier);
  }

  public TypeParameterTreeImpl newTypeParameter(InternalSyntaxToken extendsToken, BoundListTreeImpl bounds) {
    return new TypeParameterTreeImpl(extendsToken, bounds);
  }

  public BoundListTreeImpl newBounds(TypeTree classType, Optional<List<Tuple<InternalSyntaxToken, Tree>>> rests) {
    ImmutableList.Builder<Tree> classTypes = ImmutableList.builder();
    ImmutableList.Builder<SyntaxToken> separators = ImmutableList.builder();

    classTypes.add(classType);
    if (rests.isPresent()) {
      for (Tuple<InternalSyntaxToken, Tree> rest : rests.get()) {
        separators.add(rest.first());
        classTypes.add(rest.second());
      }
    }
    return new BoundListTreeImpl(classTypes.build(), separators.build());
  }

  // End of types

  // Classes, enums and interfaces

  public ClassTreeImpl completeClassDeclaration(
    InternalSyntaxToken classSyntaxToken,
    InternalSyntaxToken identifierToken, Optional<TypeParameterListTreeImpl> typeParameters,
    Optional<Tuple<InternalSyntaxToken, TypeTree>> extendsClause,
    Optional<Tuple<InternalSyntaxToken, QualifiedIdentifierListTreeImpl>> implementsClause,
    ClassTreeImpl partial) {

    IdentifierTreeImpl identifier = new IdentifierTreeImpl(identifierToken);

    partial.completeDeclarationKeyword(classSyntaxToken);
    partial.completeIdentifier(identifier);
    if (typeParameters.isPresent()) {
      partial.completeTypeParameters(typeParameters.get());
    }
    if (extendsClause.isPresent()) {
      partial.completeSuperclass(extendsClause.get().first(), extendsClause.get().second());
    }
    if (implementsClause.isPresent()) {
      InternalSyntaxToken implementsKeyword = implementsClause.get().first();
      QualifiedIdentifierListTreeImpl interfaces = implementsClause.get().second();
      partial.completeInterfaces(implementsKeyword, interfaces);
    }

    return partial;
  }

  private static ClassTreeImpl newClassBody(Kind kind, InternalSyntaxToken openBraceSyntaxToken,
    Optional<List<JavaTree>> members, InternalSyntaxToken closeBraceTokenSyntaxToken) {
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
    InternalSyntaxToken enumToken,
    InternalSyntaxToken identifierToken,
    Optional<Tuple<InternalSyntaxToken, QualifiedIdentifierListTreeImpl>> implementsClause,
    InternalSyntaxToken openBraceToken,
    Optional<List<EnumConstantTreeImpl>> enumConstants,
    Optional<InternalSyntaxToken> semicolonToken,
    Optional<List<JavaTree>> enumDeclarations,
    InternalSyntaxToken closeBraceToken) {

    List<JavaTree> members = new LinkedList<>();
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

    if (enumDeclarations.isPresent() && !semicolonToken.isPresent()) {
      throw new IllegalStateException("missing semicolon after enum constants");
    }

    ClassTreeImpl result = newClassBody(Kind.ENUM, openBraceToken, Optional.of((List<JavaTree>) ImmutableList.<JavaTree>builder().addAll(members).build()), closeBraceToken);

    result.completeDeclarationKeyword(enumToken);

    IdentifierTreeImpl identifier = new IdentifierTreeImpl(identifierToken);
    result.completeIdentifier(identifier);

    if (implementsClause.isPresent()) {
      InternalSyntaxToken implementsKeyword = implementsClause.get().first();
      QualifiedIdentifierListTreeImpl interfaces = implementsClause.get().second();
      result.completeInterfaces(implementsKeyword, interfaces);
    }

    return result;
  }

  public EnumConstantTreeImpl newEnumConstant(
    Optional<List<AnnotationTreeImpl>> annotations, InternalSyntaxToken identifierToken,
    Optional<ArgumentListTreeImpl> arguments,
    Optional<ClassTreeImpl> classBody,
    Optional<InternalSyntaxToken> commaToken) {

    IdentifierTreeImpl identifier = new IdentifierTreeImpl(identifierToken);

    ArgumentListTreeImpl defaultArguments = new ArgumentListTreeImpl(Collections.emptyList(), Collections.emptyList());
    NewClassTreeImpl newClass = new NewClassTreeImpl(arguments.or(defaultArguments), classBody.orNull());
    newClass.completeWithIdentifier(identifier);

    return new EnumConstantTreeImpl(modifiers((Optional<List<ModifierTree>>) (Optional<?>) annotations), identifier, newClass, commaToken.orNull());
  }

  public ClassTreeImpl completeInterfaceDeclaration(
    InternalSyntaxToken interfaceToken,
    InternalSyntaxToken identifierToken, Optional<TypeParameterListTreeImpl> typeParameters,
    Optional<Tuple<InternalSyntaxToken, QualifiedIdentifierListTreeImpl>> extendsClause,
    ClassTreeImpl partial) {

    IdentifierTreeImpl identifier = new IdentifierTreeImpl(identifierToken);

    partial.completeDeclarationKeyword(interfaceToken);

    partial.completeIdentifier(identifier);
    if (typeParameters.isPresent()) {
      partial.completeTypeParameters(typeParameters.get());
    }
    if (extendsClause.isPresent()) {
      InternalSyntaxToken extendsKeyword = extendsClause.get().first();
      QualifiedIdentifierListTreeImpl interfaces = extendsClause.get().second();
      partial.completeInterfaces(extendsKeyword, interfaces);
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

  public BlockTreeImpl newInitializerMember(Optional<InternalSyntaxToken> staticToken, BlockTreeImpl block) {
    if (staticToken.isPresent()) {
      return new StaticInitializerTreeImpl(staticToken.get(), (InternalSyntaxToken) block.openBraceToken(), block.body(),
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

  private static MethodTreeImpl newMethodOrConstructor(
    Optional<TypeTree> type, InternalSyntaxToken identifierToken, FormalParametersListTreeImpl parameters,
    Optional<List<Tuple<Optional<List<AnnotationTreeImpl>>, Tuple<InternalSyntaxToken, InternalSyntaxToken>>>> annotatedDimensions,
    Optional<Tuple<InternalSyntaxToken, QualifiedIdentifierListTreeImpl>> throwsClause,
    JavaTree blockOrSemicolon) {

    IdentifierTreeImpl identifier = new IdentifierTreeImpl(identifierToken);

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
    ListTree<TypeTree> throwsClauses = QualifiedIdentifierListTreeImpl.emptyList();
    if (throwsClause.isPresent()) {
      throwsToken = throwsClause.get().first();
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
    TypeTree type, InternalSyntaxToken identifierToken, FormalParametersListTreeImpl parameters,
    Optional<List<Tuple<Optional<List<AnnotationTreeImpl>>, Tuple<InternalSyntaxToken, InternalSyntaxToken>>>> annotatedDimensions,
    Optional<Tuple<InternalSyntaxToken, QualifiedIdentifierListTreeImpl>> throwsClause,
    JavaTree blockOrSemicolon) {

    return newMethodOrConstructor(Optional.of(type), identifierToken, parameters, annotatedDimensions, throwsClause, blockOrSemicolon);
  }

  public MethodTreeImpl newConstructor(
    InternalSyntaxToken identifierToken, FormalParametersListTreeImpl parameters,
    Optional<List<Tuple<Optional<List<AnnotationTreeImpl>>, Tuple<InternalSyntaxToken, InternalSyntaxToken>>>> annotatedDimensions,
    Optional<Tuple<InternalSyntaxToken, QualifiedIdentifierListTreeImpl>> throwsClause,
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

  public ClassTreeImpl completeAnnotationType(InternalSyntaxToken atToken, InternalSyntaxToken interfaceToken, InternalSyntaxToken identifier, ClassTreeImpl partial) {
    return partial.complete(
      atToken,
      interfaceToken,
      new IdentifierTreeImpl(identifier));
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
    } else if (partial.is(Kind.CLASS, Kind.INTERFACE, Kind.ENUM, Kind.ANNOTATION_TYPE)) {
      ((ClassTreeImpl) partial).completeModifiers(modifiers);
    } else if (partial.is(Kind.METHOD)) {
      ((MethodTreeImpl) partial).completeWithModifiers(modifiers);
    } else {
      throw new IllegalArgumentException("Unsupported type: " + partial);
    }

    return partial;
  }

  public MethodTreeImpl completeAnnotationMethod(TypeTree type, InternalSyntaxToken identifierToken, MethodTreeImpl partial, InternalSyntaxToken semiToken) {
    partial.complete(type, new IdentifierTreeImpl(identifierToken), semiToken);
    return partial;
  }

  public MethodTreeImpl newAnnotationTypeMethod(InternalSyntaxToken openParenToken, InternalSyntaxToken closeParenToken,
    Optional<Tuple<InternalSyntaxToken, ExpressionTree>> defaultValue) {
    FormalParametersListTreeImpl parameters = new FormalParametersListTreeImpl(openParenToken, closeParenToken);
    InternalSyntaxToken defaultToken = null;
    ExpressionTree defaultExpression = null;
    if (defaultValue.isPresent()) {
      defaultToken = defaultValue.get().first();
      defaultExpression = defaultValue.get().second();
    }
    return new MethodTreeImpl(parameters, defaultToken, defaultExpression);
  }

  public Tuple<InternalSyntaxToken, ExpressionTree> newDefaultValue(InternalSyntaxToken defaultToken, ExpressionTree elementValue) {
    return new Tuple<>(defaultToken, elementValue);
  }

  public AnnotationTreeImpl newAnnotation(InternalSyntaxToken atToken, TypeTree qualifiedIdentifier, Optional<ArgumentListTreeImpl> arguments) {
    ArgumentListTreeImpl defaultValue = new ArgumentListTreeImpl(Collections.emptyList(), Collections.emptyList());
    return new AnnotationTreeImpl(atToken, qualifiedIdentifier, arguments.or(defaultValue));
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

    ImmutableList.Builder<SyntaxToken> separators = ImmutableList.builder();
    if (rests.isPresent()) {
      for (Tuple<InternalSyntaxToken, AssignmentExpressionTreeImpl> rest : rests.get()) {
        separators.add(rest.first());
        expressions.add(rest.second());
      }
    }

    return new ArgumentListTreeImpl(expressions.build(), separators.build());
  }

  public AssignmentExpressionTreeImpl newElementValuePair(InternalSyntaxToken identifierToken, InternalSyntaxToken operator, ExpressionTree elementValue) {
    return new AssignmentExpressionTreeImpl(
      kindMaps.getAssignmentOperator((JavaPunctuator) operator.getGrammarRuleKey()),
      new IdentifierTreeImpl(identifierToken),
      operator,
      elementValue);
  }

  public NewArrayTreeImpl completeElementValueArrayInitializer(
    InternalSyntaxToken openBraceToken, Optional<NewArrayTreeImpl> partial, InternalSyntaxToken closeBraceToken) {

    NewArrayTreeImpl elementValues = partial.or(new NewArrayTreeImpl(Collections.emptyList(), InitializerListTreeImpl.emptyList()));

    return elementValues.completeWithCurlyBraces(openBraceToken, closeBraceToken);
  }

  public NewArrayTreeImpl newElementValueArrayInitializer(List<Tuple<ExpressionTree, Optional<InternalSyntaxToken>>> rests) {
    ImmutableList.Builder<ExpressionTree> expressions = ImmutableList.builder();
    ImmutableList.Builder<SyntaxToken> separators = ImmutableList.builder();
    for (Tuple<ExpressionTree, Optional<InternalSyntaxToken>> tuple : rests) {
      expressions.add(tuple.first());
      if (tuple.second().isPresent()) {
        separators.add(tuple.second().get());
      }
    }
    return new NewArrayTreeImpl(Collections.emptyList(), new InitializerListTreeImpl(expressions.build(), separators.build()));
  }

  public ArgumentListTreeImpl newSingleElementAnnotation(InternalSyntaxToken openParenToken, ExpressionTree elementValue, InternalSyntaxToken closeParenToken) {
    return new ArgumentListTreeImpl(openParenToken, elementValue, closeParenToken);
  }

  // End of annotations

  // Formal parameters

  public FormalParametersListTreeImpl completeParenFormalParameters(InternalSyntaxToken openParenToken, Optional<FormalParametersListTreeImpl> partial,
    InternalSyntaxToken closeParenToken) {

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

  public FormalParametersListTreeImpl newVariableArgumentFormalParameter(Optional<List<AnnotationTreeImpl>> annotations,
    InternalSyntaxToken ellipsisToken, VariableTreeImpl variable) {
    variable.addEllipsisDimension(new ArrayTypeTreeImpl(null, annotations.or(Collections.emptyList()), ellipsisToken));

    return new FormalParametersListTreeImpl(
      annotations.or(Collections.emptyList()),
      ellipsisToken,
      variable);
  }

  public VariableTreeImpl newVariableDeclaratorId(InternalSyntaxToken identifierToken,
    Optional<List<Tuple<Optional<List<AnnotationTreeImpl>>, Tuple<InternalSyntaxToken, InternalSyntaxToken>>>> dims) {
    IdentifierTreeImpl identifier = new IdentifierTreeImpl(identifierToken);
    ArrayTypeTreeImpl nestedDimensions = newArrayTypeTreeWithAnnotations(dims);
    return new VariableTreeImpl(identifier, nestedDimensions);
  }

  public VariableTreeImpl newFormalParameter(ModifiersTreeImpl modifiers, TypeTree type, VariableTreeImpl variable) {
    VariableTreeImpl newVar = variable.completeModifiers(modifiers);
    return newVar.completeType(type);
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

  public VariableTreeImpl completeVariableDeclarator(InternalSyntaxToken identifierToken,
    Optional<List<Tuple<Optional<List<AnnotationTreeImpl>>, Tuple<InternalSyntaxToken, InternalSyntaxToken>>>> dimensions,
    Optional<VariableTreeImpl> partial) {
    IdentifierTreeImpl identifier = new IdentifierTreeImpl(identifierToken);

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
    InternalSyntaxToken assertToken, ExpressionTree expression, Optional<AssertStatementTreeImpl> detailExpression, InternalSyntaxToken semicolonSyntaxToken) {

    return detailExpression.isPresent() ?
      detailExpression.get().complete(assertToken, expression, semicolonSyntaxToken) :
      new AssertStatementTreeImpl(assertToken, expression, semicolonSyntaxToken);
  }

  public AssertStatementTreeImpl newAssertStatement(InternalSyntaxToken colonToken, ExpressionTree expression) {
    return new AssertStatementTreeImpl(colonToken, expression);
  }

  public IfStatementTreeImpl completeIf(InternalSyntaxToken ifToken, InternalSyntaxToken openParenToken, ExpressionTree condition, InternalSyntaxToken closeParenToken,
    StatementTree statement,
    Optional<IfStatementTreeImpl> elseClause) {
    if (elseClause.isPresent()) {
      return elseClause.get().complete(ifToken, openParenToken, condition, closeParenToken, statement);
    } else {
      return new IfStatementTreeImpl(ifToken, openParenToken, condition, closeParenToken, statement);
    }
  }

  public IfStatementTreeImpl newIfWithElse(InternalSyntaxToken elseToken, StatementTree elseStatement) {
    return new IfStatementTreeImpl(elseToken, elseStatement);
  }

  public ForStatementTreeImpl newStandardForStatement(
    InternalSyntaxToken forTokenKeyword,
    InternalSyntaxToken openParenToken,
    Optional<StatementExpressionListTreeImpl> forInit, InternalSyntaxToken forInitSemicolonToken,
    Optional<ExpressionTree> expression, InternalSyntaxToken expressionSemicolonToken,
    Optional<StatementExpressionListTreeImpl> forUpdate, InternalSyntaxToken closeParenToken,
    StatementTree statement) {

    StatementExpressionListTreeImpl forInitStatement = forInit.or(new StatementExpressionListTreeImpl(Collections.emptyList(), Collections.emptyList()));
    StatementExpressionListTreeImpl forUpdateStatement = forUpdate.or(new StatementExpressionListTreeImpl(Collections.emptyList(), Collections.emptyList()));

    return new ForStatementTreeImpl(
      forTokenKeyword,
      openParenToken,
      forInitStatement,
      forInitSemicolonToken,
      expression.orNull(),
      expressionSemicolonToken,
      forUpdateStatement,
      closeParenToken,
      statement);
  }

  public StatementExpressionListTreeImpl newForInitDeclaration(ModifiersTreeImpl modifiers, TypeTree type, VariableDeclaratorListTreeImpl variables) {
    for (VariableTreeImpl variable : variables) {
      variable.completeModifiersAndType(modifiers, type);
    }
    return new StatementExpressionListTreeImpl(variables, Collections.emptyList());
  }

  public StatementExpressionListTreeImpl newStatementExpressions(ExpressionTree expression, Optional<List<Tuple<InternalSyntaxToken, ExpressionTree>>> rests) {
    ImmutableList.Builder<StatementTree> statements = ImmutableList.builder();
    statements.add(new ExpressionStatementTreeImpl(expression, null));
    ImmutableList.Builder<SyntaxToken> separators = ImmutableList.builder();
    if (rests.isPresent()) {
      for (Tuple<InternalSyntaxToken, ExpressionTree> rest : rests.get()) {
        separators.add(rest.first());
        statements.add(new ExpressionStatementTreeImpl(rest.second(), null));
      }
    }
    return new StatementExpressionListTreeImpl(statements.build(), separators.build());
  }

  public ForEachStatementImpl newForeachStatement(
    InternalSyntaxToken forKeyword,
    InternalSyntaxToken openParenToken,
    VariableTreeImpl variable, InternalSyntaxToken colonToken, ExpressionTree expression,
    InternalSyntaxToken closeParenToken,
    StatementTree statement) {
    return new ForEachStatementImpl(forKeyword, openParenToken, variable, colonToken, expression, closeParenToken, statement);
  }

  public WhileStatementTreeImpl whileStatement(InternalSyntaxToken whileToken, InternalSyntaxToken openParen, ExpressionTree expression, InternalSyntaxToken closeParen,
    StatementTree statement) {
    return new WhileStatementTreeImpl(whileToken, openParen, expression, closeParen, statement);
  }

  public DoWhileStatementTreeImpl doWhileStatement(InternalSyntaxToken doToken, StatementTree statement,
    InternalSyntaxToken whileToken, InternalSyntaxToken openParen, ExpressionTree expression,
    InternalSyntaxToken closeParen, InternalSyntaxToken semicolon) {
    return new DoWhileStatementTreeImpl(doToken, statement, whileToken, openParen, expression, closeParen, semicolon);
  }

  public TryStatementTreeImpl completeStandardTryStatement(InternalSyntaxToken tryToken, BlockTreeImpl block, TryStatementTreeImpl partial) {
    return partial.completeStandardTry(tryToken, block);
  }

  public TryStatementTreeImpl newTryCatch(Optional<List<CatchTreeImpl>> catches, Optional<TryStatementTreeImpl> finallyBlock) {
    List<CatchTreeImpl> catchTrees = catches.or(Collections.emptyList());
    if (finallyBlock.isPresent()) {
      return finallyBlock.get().completeWithCatches(catchTrees);
    } else {
      return new TryStatementTreeImpl(catchTrees, null, null);
    }
  }

  public CatchTreeImpl newCatchClause(InternalSyntaxToken catchToken, InternalSyntaxToken openParenToken, VariableTreeImpl parameter,
    InternalSyntaxToken closeParenToken, BlockTreeImpl block) {
    return new CatchTreeImpl(catchToken, openParenToken, parameter, closeParenToken, block);
  }

  public VariableTreeImpl newCatchFormalParameter(ModifiersTreeImpl modifiers, TypeTree type, VariableTreeImpl parameter) {
    if (!modifiers.isEmpty()) {
      parameter.completeModifiers(modifiers);
    }
    return parameter.completeType(type);
  }

  public TypeTree newCatchType(TypeTree qualifiedIdentifier, Optional<List<Tuple<InternalSyntaxToken, TypeTree>>> rests) {
    if (!rests.isPresent()) {
      return qualifiedIdentifier;
    }
    ImmutableList.Builder<TypeTree> types = ImmutableList.builder();
    types.add(qualifiedIdentifier);
    ImmutableList.Builder<SyntaxToken> separators = ImmutableList.builder();
    for (Tuple<InternalSyntaxToken, TypeTree> rest : rests.get()) {
      separators.add(rest.first());
      types.add(rest.second());
    }
    return new UnionTypeTreeImpl(new TypeUnionListTreeImpl(types.build(), separators.build()));
  }

  public TryStatementTreeImpl newFinallyBlock(InternalSyntaxToken finallyToken, BlockTreeImpl block) {
    return new TryStatementTreeImpl(finallyToken, block);
  }

  public TryStatementTreeImpl newTryWithResourcesStatement(
    InternalSyntaxToken tryToken, InternalSyntaxToken openParenToken, ResourceListTreeImpl resources, InternalSyntaxToken closeParenToken,
    BlockTreeImpl block,
    Optional<List<CatchTreeImpl>> catches, Optional<TryStatementTreeImpl> finallyBlock) {

    List<CatchTreeImpl> catchTrees = catches.or(Collections.emptyList());
    if (finallyBlock.isPresent()) {
      return finallyBlock.get().completeTryWithResources(tryToken, openParenToken, resources, closeParenToken, block, catchTrees);
    } else {
      return new TryStatementTreeImpl(tryToken, openParenToken, resources, closeParenToken, block, catchTrees);
    }
  }

  public ResourceListTreeImpl newResources(List<Tuple<Tree, Optional<InternalSyntaxToken>>> rests) {
    ImmutableList.Builder<Tree> resources = ImmutableList.builder();
    ImmutableList.Builder<SyntaxToken> separators = ImmutableList.builder();

    for (Tuple<Tree, Optional<InternalSyntaxToken>> rest : rests) {
      if (rest.second().isPresent()) {
        separators.add(rest.second().get());
      }
      resources.add(rest.first());
    }

    return new ResourceListTreeImpl(resources.build(), separators.build());
  }

  public Tree newResource(ModifiersTreeImpl modifiers, TypeTree classType, VariableTreeImpl partial, InternalSyntaxToken equalToken, ExpressionTree expression) {
    if (!modifiers.isEmpty()) {
      partial.completeModifiers(modifiers);
    }
    return partial.completeTypeAndInitializer(classType, equalToken, expression);
  }

  public SwitchStatementTree switchStatement(SwitchExpressionTree switchExpression) {
    return new SwitchStatementTreeImpl(switchExpression);
  }

  public SwitchExpressionTree switchExpression(InternalSyntaxToken switchToken, InternalSyntaxToken openParenToken, ExpressionTree expression,
    InternalSyntaxToken closeParenToken,
    InternalSyntaxToken openBraceToken, Optional<List<CaseGroupTreeImpl>> optionalGroups, InternalSyntaxToken closeBraceToken) {

    List<CaseGroupTreeImpl> groups = optionalGroups.or(Collections.<CaseGroupTreeImpl>emptyList());

    return new SwitchExpressionTreeImpl(switchToken, openParenToken, expression, closeParenToken,
      openBraceToken, groups, closeBraceToken);
  }

  public CaseGroupTreeImpl switchGroup(List<CaseLabelTreeImpl> labels, BlockStatementListTreeImpl blockStatements) {
    return new CaseGroupTreeImpl(labels, blockStatements);
  }

  public CaseLabelTreeImpl newSwitchCase(InternalSyntaxToken caseSyntaxToken, ArgumentListTreeImpl argumentList, InternalSyntaxToken colonToken) {
    return new CaseLabelTreeImpl(caseSyntaxToken, argumentList, colonToken);
  }

  public CaseLabelTreeImpl newSwitchDefault(InternalSyntaxToken defaultToken, InternalSyntaxToken colonToken) {
    return new CaseLabelTreeImpl(defaultToken, Collections.emptyList(), colonToken);
  }

  public SynchronizedStatementTreeImpl synchronizedStatement(InternalSyntaxToken synchronizedToken, InternalSyntaxToken openParenToken, ExpressionTree expression,
    InternalSyntaxToken closeParenToken, BlockTreeImpl block) {
    return new SynchronizedStatementTreeImpl(synchronizedToken, openParenToken, expression, closeParenToken, block);
  }

  public BreakStatementTreeImpl breakStatement(InternalSyntaxToken breakToken, Optional<ExpressionTree> labelOrValue, InternalSyntaxToken semicolonSyntaxToken) {
    return new BreakStatementTreeImpl(breakToken, labelOrValue.orNull(), semicolonSyntaxToken);
  }

  public ContinueStatementTreeImpl continueStatement(InternalSyntaxToken continueToken, Optional<InternalSyntaxToken> identifierToken, InternalSyntaxToken semicolonToken) {
    IdentifierTreeImpl identifier = null;
    if (identifierToken.isPresent()) {
      identifier = new IdentifierTreeImpl(identifierToken.get());
    }
    return new ContinueStatementTreeImpl(continueToken, identifier, semicolonToken);
  }

  public ReturnStatementTreeImpl returnStatement(InternalSyntaxToken returnToken, Optional<ExpressionTree> expression, InternalSyntaxToken semicolonSyntaxToken) {
    return new ReturnStatementTreeImpl(returnToken, expression.orNull(), semicolonSyntaxToken);
  }

  public ThrowStatementTreeImpl throwStatement(InternalSyntaxToken throwToken, ExpressionTree expression, InternalSyntaxToken semicolonToken) {
    return new ThrowStatementTreeImpl(throwToken, expression, semicolonToken);
  }

  public LabeledStatementTreeImpl labeledStatement(InternalSyntaxToken identifierToken, InternalSyntaxToken colon, StatementTree statement) {
    IdentifierTreeImpl identifier = new IdentifierTreeImpl(identifierToken);
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
    return new BlockStatementListTreeImpl(Collections.singletonList(classTree));
  }

  public BlockStatementListTreeImpl wrapInBlockStatements(StatementTree statement) {
    return new BlockStatementListTreeImpl(Collections.singletonList(statement));
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

  public ConditionalExpressionTreeImpl newTernaryExpression(InternalSyntaxToken queryToken, ExpressionTree trueExpression, InternalSyntaxToken colonToken,
    ExpressionTree falseExpression) {
    return new ConditionalExpressionTreeImpl(queryToken, trueExpression, colonToken, falseExpression);
  }

  public ExpressionTree completeInstanceofExpression(ExpressionTree expression, Optional<InstanceOfTreeImpl> partial) {
    return partial.isPresent() ?
      partial.get().complete(expression) :
      expression;
  }

  public InstanceOfTreeImpl newInstanceofExpression(InternalSyntaxToken instanceofToken, TypeTree type) {
    return new InstanceOfTreeImpl(instanceofToken, type);
  }

  public VariableTreeImpl receiverParameterId(Optional<List<Tuple<InternalSyntaxToken, InternalSyntaxToken>>> optional, InternalSyntaxToken thisToken) {
    if(optional.isPresent()) {
      // FIXME qualified id of outer class for receiver type.
    }
    return new VariableTreeImpl(new IdentifierTreeImpl(thisToken), null);
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

  private static OperatorAndOperand newOperatorAndOperand(InternalSyntaxToken operator, ExpressionTree operand) {
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

  public TypeCastExpressionTreeImpl newClassCastExpression(TypeTree type, Optional<Tuple<InternalSyntaxToken, BoundListTreeImpl>> classTypes, InternalSyntaxToken closeParenToken,
    ExpressionTree expression) {
    BoundListTreeImpl bounds = BoundListTreeImpl.emptyList();
    InternalSyntaxToken andToken = null;
    if (classTypes.isPresent()) {
      andToken = classTypes.get().first();
      bounds = classTypes.get().second();
    }
    return new TypeCastExpressionTreeImpl(type, andToken, bounds, closeParenToken, expression);
  }

  public ExpressionTree completeMethodReference(MethodReferenceTreeImpl partial, Optional<TypeArgumentListTreeImpl> typeArguments, InternalSyntaxToken newOrIdentifierToken) {
    partial.complete(typeArguments.orNull(), new IdentifierTreeImpl(newOrIdentifierToken));
    return partial;
  }

  public MethodReferenceTreeImpl newSuperMethodReference(InternalSyntaxToken superToken, InternalSyntaxToken doubleColonToken) {
    IdentifierTree superIdentifier = new IdentifierTreeImpl(superToken);
    return new MethodReferenceTreeImpl(superIdentifier, doubleColonToken);
  }

  public MethodReferenceTreeImpl newTypeMethodReference(Tree type, InternalSyntaxToken doubleColonToken) {
    return new MethodReferenceTreeImpl(type, doubleColonToken);
  }

  public MethodReferenceTreeImpl newPrimaryMethodReference(ExpressionTree expression, InternalSyntaxToken doubleColonToken) {
    return new MethodReferenceTreeImpl(expression, doubleColonToken);
  }

  public ExpressionTree lambdaExpression(LambdaParameterListTreeImpl parameters, InternalSyntaxToken arrowToken, Tree body) {
    return new LambdaExpressionTreeImpl(
      parameters.openParenToken(),
      ImmutableList.<VariableTree>builder().addAll(parameters).build(),
      parameters.closeParenToken(),
      arrowToken,
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
    return new LambdaParameterListTreeImpl(null, Collections.singletonList(parameter), null);
  }

  public VariableTreeImpl newSimpleParameter(InternalSyntaxToken identifierToken) {
    IdentifierTreeImpl identifier = new IdentifierTreeImpl(identifierToken);
    return new VariableTreeImpl(identifier);
  }

  public ParenthesizedTreeImpl parenthesizedExpression(InternalSyntaxToken leftParenSyntaxToken, ExpressionTree expression, InternalSyntaxToken rightParenSyntaxToken) {
    return new ParenthesizedTreeImpl(leftParenSyntaxToken, expression, rightParenSyntaxToken);
  }

  public ExpressionTree newExpression(InternalSyntaxToken newToken, Optional<List<AnnotationTreeImpl>> annotations, ExpressionTree partial) {
    TypeTree typeTree;
    if (partial.is(Tree.Kind.NEW_CLASS)) {
      NewClassTreeImpl newClassTree = (NewClassTreeImpl) partial;
      newClassTree.completeWithNewKeyword(newToken);
      typeTree = newClassTree.identifier();
    } else {
      NewArrayTreeImpl newArrayTree = (NewArrayTreeImpl) partial;
      newArrayTree.completeWithNewKeyword(newToken);
      typeTree = newArrayTree.type();
    }
    completeTypeTreeWithAnnotations(typeTree, annotations);
    return partial;
  }

  public ExpressionTree newClassCreator(Optional<TypeArgumentListTreeImpl> typeArguments, TypeTree qualifiedIdentifier, NewClassTreeImpl classCreatorRest) {
    if (typeArguments.isPresent()) {
      classCreatorRest.completeWithTypeArguments(typeArguments.get());
    }
    return classCreatorRest.completeWithIdentifier(qualifiedIdentifier);
  }

  public ExpressionTree newArrayCreator(TypeTree type, NewArrayTreeImpl partial) {
    return partial.complete(type);
  }

  public NewArrayTreeImpl completeArrayCreator(Optional<List<AnnotationTreeImpl>> annotations, NewArrayTreeImpl partial) {
    if (annotations.isPresent()) {
      partial.completeFirstDimension(annotations.get());
    }
    return partial;
  }

  public NewArrayTreeImpl newArrayCreatorWithInitializer(
    InternalSyntaxToken openBracketToken, InternalSyntaxToken closeBracketToken,
    Optional<List<Tuple<Optional<List<AnnotationTreeImpl>>, Tuple<InternalSyntaxToken, InternalSyntaxToken>>>> dimensions,
    NewArrayTreeImpl partial) {

    ImmutableList.Builder<ArrayDimensionTree> dDimensionsBuilder = ImmutableList.builder();
    dDimensionsBuilder.add(new ArrayDimensionTreeImpl(openBracketToken, null, closeBracketToken));
    if (dimensions.isPresent()) {
      for (Tuple<Optional<List<AnnotationTreeImpl>>, Tuple<InternalSyntaxToken, InternalSyntaxToken>> dim : dimensions.get()) {
        List<AnnotationTreeImpl> annotations = dim.first().or(Collections.emptyList());
        Tuple<InternalSyntaxToken, InternalSyntaxToken> brackets = dim.second();
        dDimensionsBuilder.add(new ArrayDimensionTreeImpl(annotations, brackets.first(), null, brackets.second()));
      }
    }

    return partial.completeDimensions(dDimensionsBuilder.build());
  }

  public NewArrayTreeImpl newArrayCreatorWithDimension(InternalSyntaxToken openBracketToken, ExpressionTree expression, InternalSyntaxToken closeBracketToken,
    Optional<List<ArrayAccessExpressionTreeImpl>> arrayAccesses,
    Optional<List<Tuple<Optional<List<AnnotationTreeImpl>>, Tuple<InternalSyntaxToken, InternalSyntaxToken>>>> dims) {

    ImmutableList.Builder<ArrayDimensionTree> dimensions = ImmutableList.builder();

    dimensions.add(new ArrayDimensionTreeImpl(openBracketToken, expression, closeBracketToken));
    if (arrayAccesses.isPresent()) {
      for (ArrayAccessExpressionTreeImpl arrayAccess : arrayAccesses.get()) {
        dimensions.add(arrayAccess.dimension());
      }
    }
    if (dims.isPresent()) {
      for (Tuple<Optional<List<AnnotationTreeImpl>>, Tuple<InternalSyntaxToken, InternalSyntaxToken>> dim : dims.get()) {
        Tuple<InternalSyntaxToken, InternalSyntaxToken> brackets = dim.second();
        List<AnnotationTreeImpl> annotations = dim.first().or(Collections.emptyList());
        dimensions.add(new ArrayDimensionTreeImpl(annotations, brackets.first(), null, brackets.second()));
      }
    }
    return new NewArrayTreeImpl(dimensions.build(), InitializerListTreeImpl.emptyList());
  }

  public ExpressionTree basicClassExpression(PrimitiveTypeTreeImpl basicType, Optional<List<Tuple<InternalSyntaxToken, InternalSyntaxToken>>> dimensions,
    InternalSyntaxToken dotToken, InternalSyntaxToken classToken) {
    // 15.8.2. Class Literals
    // int.class
    // int[].class

    IdentifierTreeImpl classIdentifier = new IdentifierTreeImpl(classToken);
    ArrayTypeTreeImpl nestedDimensions = newArrayTypeTree(dimensions);
    TypeTree typeTree = applyDim(basicType, nestedDimensions);
    return new MemberSelectExpressionTreeImpl((ExpressionTree) typeTree, dotToken, classIdentifier);
  }

  public PrimitiveTypeTreeImpl newBasicType(Optional<List<AnnotationTreeImpl>> annotations, InternalSyntaxToken basicType) {
    JavaTree.PrimitiveTypeTreeImpl primitiveTypeTree = new JavaTree.PrimitiveTypeTreeImpl(basicType);
    completeTypeTreeWithAnnotations(primitiveTypeTree, annotations);
    return primitiveTypeTree;
  }

  public ArgumentListTreeImpl completeArguments(InternalSyntaxToken openParenthesisToken, Optional<ArgumentListTreeImpl> expressions, InternalSyntaxToken closeParenthesisToken) {
    return expressions.isPresent() ?
      expressions.get().complete(openParenthesisToken, closeParenthesisToken) :
      new ArgumentListTreeImpl(openParenthesisToken, closeParenthesisToken);
  }

  public ArgumentListTreeImpl newArguments(ExpressionTree expression, Optional<List<Tuple<InternalSyntaxToken, ExpressionTree>>> rests) {
    ImmutableList.Builder<ExpressionTree> expressions = ImmutableList.builder();
    expressions.add(expression);
    ImmutableList.Builder<SyntaxToken> separators = ImmutableList.builder();
    if (rests.isPresent()) {
      for (Tuple<InternalSyntaxToken, ExpressionTree> rest : rests.get()) {
        separators.add(rest.first());
        expressions.add(rest.second());
      }
    }

    return new ArgumentListTreeImpl(expressions.build(), separators.build());
  }

  public TypeTree annotationIdentifier(InternalSyntaxToken firstIdentifier, Optional<List<Tuple<InternalSyntaxToken, InternalSyntaxToken>>> rests) {
    List<InternalSyntaxToken> children = new ArrayList<>();
    children.add(firstIdentifier);
    if (rests.isPresent()) {
      for (Tuple<InternalSyntaxToken, InternalSyntaxToken> rest : rests.get()) {
        children.add(rest.first());
        children.add(rest.second());
      }
    }

    JavaTree result = null;

    InternalSyntaxToken dotToken = null;
    for (InternalSyntaxToken child : children) {
      if (!child.getGrammarRuleKey().equals(JavaTokenType.IDENTIFIER)) {
        dotToken = child;
      } else {
        InternalSyntaxToken identifierToken = child;

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
      moveAnnotations((TypeTree) result, (TypeTree) firstIdentifier);
    }

    return (T) result;
  }

  private static void moveAnnotations(TypeTree result, TypeTree firstIdentifier) {
    List<AnnotationTree> firstIdentifierAnnotations = firstIdentifier.annotations();
    // move the annotations from the first identifier to the member select or the parameterized type
    if (!firstIdentifierAnnotations.isEmpty()) {
      ((JavaTree.AnnotatedTypeTree) result).complete(firstIdentifierAnnotations);
      ((JavaTree.AnnotatedTypeTree) firstIdentifier).complete(Collections.emptyList());
    }
  }

  public ExpressionTree newAnnotatedParameterizedIdentifier(
    Optional<List<AnnotationTreeImpl>> annotations, InternalSyntaxToken identifierToken, Optional<TypeArgumentListTreeImpl> typeArguments) {

    List<AnnotationTree> annotationList = ImmutableList.copyOf(annotations.or(Collections.emptyList()));

    ExpressionTree result = new IdentifierTreeImpl(identifierToken);

    if (typeArguments.isPresent()) {
      result = new ParameterizedTypeTreeImpl((TypeTree) result, typeArguments.get());
    }

    ((JavaTree.AnnotatedTypeTree) result).complete(annotationList);

    return result;
  }

  public NewArrayTreeImpl newArrayInitializer(
    InternalSyntaxToken openBraceToken,
    Optional<InternalSyntaxToken> optionalComma,
    Optional<List<Tuple<ExpressionTree, Optional<InternalSyntaxToken>>>> rests,
    InternalSyntaxToken closeBraceToken) {
    ImmutableList.Builder<ExpressionTree> initializers = ImmutableList.builder();
    ImmutableList.Builder<SyntaxToken> separators = ImmutableList.builder();

    if (optionalComma.isPresent()) {
      separators.add(optionalComma.get());
    }
    if (rests.isPresent()) {
      for (Tuple<ExpressionTree, Optional<InternalSyntaxToken>> rest : rests.get()) {
        initializers.add(rest.first());
        if (rest.second().isPresent()) {
          separators.add(rest.second().get());
        }
      }
    }
    return new NewArrayTreeImpl(Collections.emptyList(),
      new InitializerListTreeImpl(initializers.build(), separators.build())).completeWithCurlyBraces(openBraceToken, closeBraceToken);
  }

  public QualifiedIdentifierListTreeImpl newQualifiedIdentifierList(TypeTree qualifiedIdentifier, Optional<List<Tuple<InternalSyntaxToken, TypeTree>>> rests) {
    ImmutableList.Builder<TypeTree> qualifiedIdentifiers = ImmutableList.builder();
    ImmutableList.Builder<SyntaxToken> separators = ImmutableList.builder();
    qualifiedIdentifiers.add(qualifiedIdentifier);
    if (rests.isPresent()) {
      for (Tuple<InternalSyntaxToken, TypeTree> rest : rests.get()) {
        separators.add(rest.first());
        qualifiedIdentifiers.add(rest.second());
      }
    }
    return new QualifiedIdentifierListTreeImpl(qualifiedIdentifiers.build(), separators.build());
  }

  public ArrayAccessExpressionTreeImpl newArrayAccessExpression(Optional<List<AnnotationTreeImpl>> annotations, InternalSyntaxToken openBracketToken, ExpressionTree index,
    InternalSyntaxToken closeBracketToken) {
    return new ArrayAccessExpressionTreeImpl(new ArrayDimensionTreeImpl(
      annotations.or(Collections.emptyList()),
      openBracketToken,
      index,
      closeBracketToken));
  }

  public NewClassTreeImpl newClassCreatorRest(ArgumentListTreeImpl arguments, Optional<ClassTreeImpl> classBody) {
    return new NewClassTreeImpl(arguments, classBody.orNull());
  }

  public ExpressionTree newIdentifierOrMethodInvocation(Optional<TypeArgumentListTreeImpl> typeArguments, InternalSyntaxToken identifierToken,
    Optional<ArgumentListTreeImpl> arguments) {
    IdentifierTreeImpl identifier = new IdentifierTreeImpl(identifierToken);
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

  public ExpressionTree newDotClassSelector(Optional<List<Tuple<InternalSyntaxToken, InternalSyntaxToken>>> dimensions,
    InternalSyntaxToken dotToken, InternalSyntaxToken classToken) {
    IdentifierTreeImpl identifier = new IdentifierTreeImpl(classToken);

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

  // End of expressions

  // Helpers

  public static class Tuple<T, U> {
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

  private static <T, U> Tuple<T, U> newTuple(T first, U second) {
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

  public <T, U> Tuple<T, U> newTuple9(T first, U second) {
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

  public <T, U> Tuple<T, U> newAnnotatedDimension(T first, U second) {
    return newTuple(first, second);
  }

  public <U> Tuple<Optional<InternalSyntaxToken>, U> newTupleAbsent1(U expression) {
    return newTuple(Optional.<InternalSyntaxToken>absent(), expression);
  }

  public <U> Tuple<Optional<InternalSyntaxToken>, U> newTupleAbsent2(U expression) {
    return newTuple(Optional.<InternalSyntaxToken>absent(), expression);
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
  private static ArrayTypeTreeImpl newArrayTypeTreeWithAnnotations(Optional<List<Tuple<Optional<List<AnnotationTreeImpl>>,
    Tuple<InternalSyntaxToken, InternalSyntaxToken>>>> dims) {
    ArrayTypeTreeImpl result = null;
    if (dims.isPresent()) {
      for (Tuple<Optional<List<AnnotationTreeImpl>>, Tuple<InternalSyntaxToken, InternalSyntaxToken>> dim : dims.get()) {
        result = newArrayTypeTreeWithAnnotations(result, dim);
      }
    }
    return result;
  }

  private static ArrayTypeTreeImpl newArrayTypeTreeWithAnnotations(TypeTree type, Tuple<Optional<List<AnnotationTreeImpl>>, Tuple<InternalSyntaxToken, InternalSyntaxToken>> dim) {
    List<AnnotationTreeImpl> annotations = dim.first().or(Collections.emptyList());
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
        result = new ArrayTypeTreeImpl(result, Collections.emptyList(), openBracketToken, closeBracketToken);
      }
    }
    return result;
  }

  private static void completeTypeTreeWithAnnotations(TypeTree type, Optional<List<AnnotationTreeImpl>> annotations) {
    List<AnnotationTree> annotationList = ImmutableList.copyOf(annotations.or(Collections.emptyList()));
    ((JavaTree.AnnotatedTypeTree) type).complete(annotationList);
  }

}
