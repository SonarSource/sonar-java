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

import com.sonar.sslr.api.typed.GrammarBuilder;
import com.sonar.sslr.api.typed.Optional;
import java.util.List;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.api.JavaRestrictedKeyword;
import org.sonar.java.ast.api.JavaSpecialIdentifier;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.parser.TreeFactory.Tuple;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.JavaTree.CompilationUnitTreeImpl;
import org.sonar.java.model.JavaTree.PrimitiveTypeTreeImpl;
import org.sonar.java.model.TypeParameterTreeImpl;
import org.sonar.java.model.declaration.AnnotationTreeImpl;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.EnumConstantTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.ModifierKeywordTreeImpl;
import org.sonar.java.model.declaration.ModifiersTreeImpl;
import org.sonar.java.model.declaration.ModuleNameListTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.expression.ArrayAccessExpressionTreeImpl;
import org.sonar.java.model.expression.AssignmentExpressionTreeImpl;
import org.sonar.java.model.expression.NewArrayTreeImpl;
import org.sonar.java.model.expression.NewClassTreeImpl;
import org.sonar.java.model.expression.ParenthesizedTreeImpl;
import org.sonar.java.model.expression.TypeArgumentListTreeImpl;
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
import org.sonar.java.model.statement.SynchronizedStatementTreeImpl;
import org.sonar.java.model.statement.ThrowStatementTreeImpl;
import org.sonar.java.model.statement.TryStatementTreeImpl;
import org.sonar.java.model.statement.WhileStatementTreeImpl;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ImportClauseTree;
import org.sonar.plugins.java.api.tree.ModifierTree;
import org.sonar.plugins.java.api.tree.ModuleDeclarationTree;
import org.sonar.plugins.java.api.tree.ModuleDirectiveTree;
import org.sonar.plugins.java.api.tree.ModuleNameTree;
import org.sonar.plugins.java.api.tree.PackageDeclarationTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SwitchExpressionTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

import static org.sonar.java.ast.api.JavaPunctuator.COLON;
import static org.sonar.java.ast.api.JavaTokenType.IDENTIFIER;

public class JavaGrammar {

  private final GrammarBuilder<InternalSyntaxToken> b;
  private final TreeFactory f;

  public JavaGrammar(GrammarBuilder<InternalSyntaxToken> b, TreeFactory f) {
    this.b = b;
    this.f = f;
  }

  public ModifiersTreeImpl MODIFIERS() {
    return b.<ModifiersTreeImpl>nonterminal(JavaLexer.MODIFIERS)
      .is(
        f.modifiers(
          b.zeroOrMore(
            b.<ModifierTree>firstOf(
              ANNOTATION(),
              MODIFIER_KEYWORD()))));
  }

  public ModifierKeywordTreeImpl MODIFIER_KEYWORD() {
    return b.<ModifierKeywordTreeImpl>nonterminal().is(
      f.modifierKeyword(
        b.firstOf(
          b.token(JavaKeyword.PUBLIC),
          b.token(JavaKeyword.PROTECTED),
          b.token(JavaKeyword.PRIVATE),
          b.token(JavaKeyword.ABSTRACT),
          b.token(JavaKeyword.STATIC),
          b.token(JavaKeyword.FINAL),
          b.token(JavaKeyword.TRANSIENT),
          b.token(JavaKeyword.VOLATILE),
          b.token(JavaKeyword.SYNCHRONIZED),
          b.token(JavaKeyword.NATIVE),
          b.token(JavaKeyword.DEFAULT),
          b.token(JavaKeyword.STRICTFP))));
  }

  // Literals

  public ExpressionTree LITERAL() {
    return b.<ExpressionTree>nonterminal(JavaLexer.LITERAL)
      .is(
        f.literal(
          b.firstOf(
            b.token(JavaKeyword.TRUE),
            b.token(JavaKeyword.FALSE),
            b.token(JavaKeyword.NULL),
            b.token(JavaTokenType.CHARACTER_LITERAL),
            b.token(JavaTokenType.STRING_LITERAL),
            b.token(JavaTokenType.FLOAT_LITERAL),
            b.token(JavaTokenType.DOUBLE_LITERAL),
            b.token(JavaTokenType.LONG_LITERAL),
            b.token(JavaTokenType.INTEGER_LITERAL))));
  }

  // End of literals

  // Compilation unit

  public CompilationUnitTreeImpl COMPILATION_UNIT() {
    return b.<CompilationUnitTreeImpl>nonterminal(JavaLexer.COMPILATION_UNIT)
      .is(
        f.newCompilationUnit(
          b.token(JavaLexer.SPACING),
          b.optional(PACKAGE_DECLARATION()),
          b.zeroOrMore(IMPORT_DECLARATION()),
          b.optional(MODULE_DECLARATION()),
          b.zeroOrMore(TYPE_DECLARATION()),
          b.token(JavaLexer.EOF)));
  }

  public ModuleDeclarationTree MODULE_DECLARATION() {
    return b.<ModuleDeclarationTree>nonterminal(JavaLexer.MODULE_DECLARATION)
      .is(
        f.newModuleDeclaration(
          b.zeroOrMore(ANNOTATION()),
          b.optional(b.token(JavaRestrictedKeyword.OPEN)),
          b.token(JavaRestrictedKeyword.MODULE),
          MODULE_NAME(),
          b.token(JavaPunctuator.LWING),
          b.zeroOrMore(MODULE_DIRECTIVE()),
          b.token(JavaPunctuator.RWING)));
  }

  public ModuleNameTree MODULE_NAME() {
    return b.<ModuleNameTree>nonterminal(JavaLexer.MODULE_NAME)
      .is(
        f.newModuleName(
          b.token(JavaTokenType.IDENTIFIER),
          b.zeroOrMore(f.moduleNameRest(b.token(JavaPunctuator.DOT), b.token(JavaTokenType.IDENTIFIER)))));
  }

  public ModuleNameListTreeImpl MODULE_NAME_LIST() {
    return b.<ModuleNameListTreeImpl>nonterminal(JavaLexer.MODULE_NAME_LIST)
      .is(
        f.newModuleNameListTreeImpl(
          MODULE_NAME(),
          b.zeroOrMore(f
            .moduleNamesRest(
              b.token(JavaPunctuator.COMMA),
              MODULE_NAME()))));
  }

  public ModuleDirectiveTree MODULE_DIRECTIVE() {
    return b.<ModuleDirectiveTree>nonterminal(JavaLexer.MODULE_DIRECTIVE)
      .is(b.firstOf(
        REQUIRES_MODULE_DIRECTIVE(),
        EXPORTS_MODULE_DIRECTIVE(),
        OPENS_MODULE_DIRECTIVE(),
        USES_MODULE_DIRECTIVE(),
        PROVIDES_MODULE_DIRECTIVE()));
  }

  public ModuleDirectiveTree REQUIRES_MODULE_DIRECTIVE() {
    return b.<ModuleDirectiveTree>nonterminal(JavaLexer.REQUIRES_DIRECTIVE)
      .is(b.firstOf(
        // JLS9 - §3.9 : 'transitive' restricted keyword can be used as module name instead of modifier
        f.newRequiresModuleDirective(
          b.token(JavaRestrictedKeyword.REQUIRES),
          b.token(JavaRestrictedKeyword.TRANSITIVE),
          b.token(JavaPunctuator.SEMI)),
        f.newRequiresModuleDirective(
          b.token(JavaRestrictedKeyword.REQUIRES),
          b.token(JavaKeyword.STATIC),
          b.token(JavaRestrictedKeyword.TRANSITIVE),
          b.token(JavaPunctuator.SEMI)),
        // ordinary requires directives
        f.newRequiresModuleDirective(
          b.token(JavaRestrictedKeyword.REQUIRES),
          b.zeroOrMore(REQUIRES_MODIFIER()),
          MODULE_NAME(),
          b.token(JavaPunctuator.SEMI))));
  }

  public InternalSyntaxToken REQUIRES_MODIFIER() {
    return b.<InternalSyntaxToken>nonterminal(JavaLexer.REQUIRES_MODIFIER)
      .is(b.firstOf(
        b.token(JavaKeyword.STATIC),
        b.token(JavaRestrictedKeyword.TRANSITIVE)));
  }

  public ModuleDirectiveTree EXPORTS_MODULE_DIRECTIVE() {
    return b.<ModuleDirectiveTree>nonterminal(JavaLexer.EXPORTS_DIRECTIVE)
      .is(
        f.newExportsModuleDirective(
          b.token(JavaRestrictedKeyword.EXPORTS),
          EXPRESSION_QUALIFIED_IDENTIFIER(),
          b.optional(
            f.toModuleNames(
              b.token(JavaRestrictedKeyword.TO),
              MODULE_NAME_LIST())),
          b.token(JavaPunctuator.SEMI)));
  }

  public ModuleDirectiveTree OPENS_MODULE_DIRECTIVE() {
    return b.<ModuleDirectiveTree>nonterminal(JavaLexer.OPENS_DIRECTIVE)
      .is(
        f.newOpensModuleDirective(
          b.token(JavaRestrictedKeyword.OPENS),
          EXPRESSION_QUALIFIED_IDENTIFIER(),
          b.optional(
            f.toModuleNames2(
              b.token(JavaRestrictedKeyword.TO),
              MODULE_NAME_LIST())),
          b.token(JavaPunctuator.SEMI)));
  }

  public ModuleDirectiveTree USES_MODULE_DIRECTIVE() {
    return b.<ModuleDirectiveTree>nonterminal(JavaLexer.USES_DIRECTIVE)
      .is(
        f.newUsesModuleDirective(
          b.token(JavaRestrictedKeyword.USES),
          TYPE_QUALIFIED_IDENTIFIER(),
          b.token(JavaPunctuator.SEMI)));
  }

  public ModuleDirectiveTree PROVIDES_MODULE_DIRECTIVE() {
    return b.<ModuleDirectiveTree>nonterminal(JavaLexer.PROVIDES_DIRECTIVE)
      .is(
        f.newProvidesModuleDirective(
          b.token(JavaRestrictedKeyword.PROVIDES),
          TYPE_QUALIFIED_IDENTIFIER(),
          b.token(JavaRestrictedKeyword.WITH),
          QUALIFIED_IDENTIFIER_LIST(),
          b.token(JavaPunctuator.SEMI)));
  }

  public PackageDeclarationTree PACKAGE_DECLARATION() {
    return b.<PackageDeclarationTree>nonterminal(JavaLexer.PACKAGE_DECLARATION)
      .is(f.newPackageDeclaration(b.zeroOrMore(ANNOTATION()), b.token(JavaKeyword.PACKAGE), EXPRESSION_QUALIFIED_IDENTIFIER(), b.token(JavaPunctuator.SEMI)));
  }

  private ExpressionTree EXPRESSION_QUALIFIED_IDENTIFIER() {
    return this.QUALIFIED_IDENTIFIER();
  }

  public ImportClauseTree IMPORT_DECLARATION() {
    return b.<ImportClauseTree>nonterminal(JavaLexer.IMPORT_DECLARATION)
      .is(
        b.firstOf(
          f.newImportDeclaration(
            b.token(JavaKeyword.IMPORT), b.optional(b.token(JavaKeyword.STATIC)), EXPRESSION_QUALIFIED_IDENTIFIER(),
            b.optional(f.newTuple17(b.token(JavaPunctuator.DOT), b.token(JavaPunctuator.STAR))),
            b.token(JavaPunctuator.SEMI)),
          // javac accepts empty statements in import declarations
          f.newEmptyImport(b.token(JavaPunctuator.SEMI))));
  }

  public Tree TYPE_DECLARATION() {
    return b.<Tree>nonterminal(JavaLexer.TYPE_DECLARATION)
      .is(
        b.firstOf(
          // TODO Unfactor MODIFIERS? It always seems to precede CLASS_DECLARATION()
          f.newTypeDeclaration(
            MODIFIERS(),
            b.firstOf(
              CLASS_DECLARATION(),
              ENUM_DECLARATION(),
              INTERFACE_DECLARATION(),
              ANNOTATION_TYPE_DECLARATION())),
          // javac accepts empty statements in type declarations
          f.newEmptyType(b.token(JavaPunctuator.SEMI))));
  }

  // End of compilation unit

  // Types

  public TypeTree TYPE() {
    return b.<TypeTree>nonterminal(JavaLexer.TYPE)
      .is(
        f.newType(
          b.firstOf(
            BASIC_TYPE(),
            TYPE_QUALIFIED_IDENTIFIER()),
          b.zeroOrMore(ANNOTATED_DIMENSION())));
  }

  public TypeArgumentListTreeImpl TYPE_ARGUMENTS() {
    return b.<TypeArgumentListTreeImpl>nonterminal(JavaLexer.TYPE_ARGUMENTS)
      .is(
        b.firstOf(
          f.newTypeArgumentList(
            b.token(JavaPunctuator.LPOINT),
            TYPE_ARGUMENT(),
            b.zeroOrMore(f.newTuple19(b.token(JavaPunctuator.COMMA), TYPE_ARGUMENT())),
            b.token(JavaPunctuator.RPOINT)),
          f.newDiamondTypeArgument(b.token(JavaPunctuator.LPOINT), b.token(JavaPunctuator.RPOINT))));
  }

  public Tree TYPE_ARGUMENT() {
    return b.<Tree>nonterminal(JavaLexer.TYPE_ARGUMENT)
      .is(
        b.firstOf(
          f.newBasicTypeArgument(TYPE()),
          f.completeWildcardTypeArgument(
            b.zeroOrMore(ANNOTATION()),
            b.token(JavaPunctuator.QUERY),
            b.optional(
              f.newWildcardTypeArguments(
                b.firstOf(
                  b.token(JavaKeyword.EXTENDS),
                  b.token(JavaKeyword.SUPER)),
                b.zeroOrMore(ANNOTATION()),
                TYPE())))));
  }

  public TypeParameterListTreeImpl TYPE_PARAMETERS() {
    return b.<TypeParameterListTreeImpl>nonterminal(JavaLexer.TYPE_PARAMETERS)
      .is(
        f.newTypeParameterList(
          b.token(JavaPunctuator.LPOINT),
          TYPE_PARAMETER(),
          b.zeroOrMore(f.newTuple22(b.token(JavaPunctuator.COMMA), TYPE_PARAMETER())),
          b.token(JavaPunctuator.RPOINT)));
  }

  public TypeParameterTreeImpl TYPE_PARAMETER() {
    return b.<TypeParameterTreeImpl>nonterminal(JavaLexer.TYPE_PARAMETER)
      .is(
        f.completeTypeParameter(
          b.zeroOrMore(ANNOTATION()),
          b.token(JavaTokenType.IDENTIFIER),
          b.optional(
            f.newTypeParameter(b.token(JavaKeyword.EXTENDS), BOUND()))));
  }

  public BoundListTreeImpl BOUND() {
    return b.<BoundListTreeImpl>nonterminal(JavaLexer.BOUND)
      .is(
        f.newBounds(
            TYPE_QUALIFIED_IDENTIFIER(),
            b.zeroOrMore(f.newTuple21(b.token(JavaPunctuator.AND), QUALIFIED_IDENTIFIER()))));
  }

  // End of types

  // Classes

  public ClassTreeImpl CLASS_DECLARATION() {
    return b.<ClassTreeImpl>nonterminal(JavaLexer.CLASS_DECLARATION)
      .is(
        f.completeClassDeclaration(
            b.token(JavaKeyword.CLASS),
            b.token(JavaTokenType.IDENTIFIER), b.optional(TYPE_PARAMETERS()),
            b.optional(f.newTuple7(b.token(JavaKeyword.EXTENDS), TYPE_QUALIFIED_IDENTIFIER())),
            b.optional(f.newTuple14(b.token(JavaKeyword.IMPLEMENTS), QUALIFIED_IDENTIFIER_LIST())),
            CLASS_BODY()));
  }

  public ClassTreeImpl CLASS_BODY() {
    return b.<ClassTreeImpl>nonterminal(JavaLexer.CLASS_BODY)
      .is(f.newClassBody(b.token(JavaPunctuator.LWING), b.zeroOrMore(CLASS_MEMBER()), b.token(JavaPunctuator.RWING)));
  }

  public JavaTree CLASS_MEMBER() {
    return b.<JavaTree>nonterminal(JavaLexer.MEMBER_DECL)
      .is(
        b.firstOf(
          f.completeMember(
            MODIFIERS(),
            b.firstOf(
              METHOD_OR_CONSTRUCTOR_DECLARATION(),
              FIELD_DECLARATION(),
              CLASS_DECLARATION(),
              ANNOTATION_TYPE_DECLARATION(),
              INTERFACE_DECLARATION(),
              ENUM_DECLARATION())),
          f.newInitializerMember(b.optional(b.token(JavaKeyword.STATIC)), BLOCK()),
          // javac accepts empty statements in member declarations
          f.newEmptyMember(b.token(JavaPunctuator.SEMI))));
  }

  public MethodTreeImpl METHOD_OR_CONSTRUCTOR_DECLARATION() {
    return b.<MethodTreeImpl>nonterminal()
      .is(
        b.firstOf(
          f.completeGenericMethodOrConstructorDeclaration(TYPE_PARAMETERS(), METHOD_OR_CONSTRUCTOR_DECLARATION()),
          f.newMethod(
            TYPE(), b.token(JavaTokenType.IDENTIFIER), FORMAL_PARAMETERS(),
            b.zeroOrMore(ANNOTATED_DIMENSION()),
            b.optional(f.newTuple10(b.token(JavaKeyword.THROWS), QUALIFIED_IDENTIFIER_LIST())),
            b.firstOf(
              BLOCK(),
              b.token(JavaPunctuator.SEMI))),
          // TODO Largely duplicated with method, but there is a prefix capture on the TYPE, it can be improved
          f.newConstructor(
            b.token(JavaTokenType.IDENTIFIER), FORMAL_PARAMETERS(),
            b.zeroOrMore(ANNOTATED_DIMENSION()),
            b.optional(f.newTuple16(b.token(JavaKeyword.THROWS), QUALIFIED_IDENTIFIER_LIST())),
            b.firstOf(
              BLOCK(),
              b.token(JavaPunctuator.SEMI)))));
  }

  public VariableDeclaratorListTreeImpl FIELD_DECLARATION() {
    return b.<VariableDeclaratorListTreeImpl>nonterminal(JavaLexer.FIELD_DECLARATION)
      .is(f.completeFieldDeclaration(TYPE(), VARIABLE_DECLARATORS(), b.token(JavaPunctuator.SEMI)));
  }

  // End of classes

  // Enums

  public ClassTreeImpl ENUM_DECLARATION() {
    return b.<ClassTreeImpl>nonterminal(JavaLexer.ENUM_DECLARATION)
      .is(
        f.newEnumDeclaration(
          b.token(JavaKeyword.ENUM),
          b.token(JavaTokenType.IDENTIFIER),
          b.optional(f.newTuple12(b.token(JavaKeyword.IMPLEMENTS), QUALIFIED_IDENTIFIER_LIST())),
          b.token(JavaPunctuator.LWING),
          b.zeroOrMore(ENUM_CONSTANT()),
          // Grammar has been relaxed
          b.optional(b.token(JavaPunctuator.SEMI)),
          b.zeroOrMore(CLASS_MEMBER()),
          b.token(JavaPunctuator.RWING)));
  }

  public EnumConstantTreeImpl ENUM_CONSTANT() {
    return b.<EnumConstantTreeImpl>nonterminal(JavaLexer.ENUM_CONSTANT)
      .is(
        f.newEnumConstant(
          b.zeroOrMore(ANNOTATION()), b.token(JavaTokenType.IDENTIFIER),
          b.optional(ARGUMENTS()),
          b.optional(CLASS_BODY()),
          b.optional(b.token(JavaPunctuator.COMMA))));
  }

  // End of enums

  // Interfaces

  public ClassTreeImpl INTERFACE_DECLARATION() {
    return b.<ClassTreeImpl>nonterminal(JavaLexer.INTERFACE_DECLARATION)
      .is(
        f.completeInterfaceDeclaration(
          b.token(JavaKeyword.INTERFACE),
          b.token(JavaTokenType.IDENTIFIER),
          b.optional(TYPE_PARAMETERS()),
          b.optional(f.newTuple11(b.token(JavaKeyword.EXTENDS), QUALIFIED_IDENTIFIER_LIST())),
          INTERFACE_BODY()));
  }

  public ClassTreeImpl INTERFACE_BODY() {
    return b.<ClassTreeImpl>nonterminal()
      .is(f.newInterfaceBody(b.token(JavaPunctuator.LWING), b.zeroOrMore(CLASS_MEMBER()), b.token(JavaPunctuator.RWING)));
  }

  // End of interfaces

  // Annotations

  // TODO modifiers
  public ClassTreeImpl ANNOTATION_TYPE_DECLARATION() {
    return b.<ClassTreeImpl>nonterminal(JavaLexer.ANNOTATION_TYPE_DECLARATION)
      .is(
        f.completeAnnotationType(
          b.token(JavaPunctuator.AT),
          b.token(JavaKeyword.INTERFACE),
          b.token(JavaTokenType.IDENTIFIER),
          ANNOTATION_TYPE_BODY()));
  }

  public ClassTreeImpl ANNOTATION_TYPE_BODY() {
    return b.<ClassTreeImpl>nonterminal(JavaLexer.ANNOTATION_TYPE_BODY)
      .is(
        f.newAnnotationType(
          b.token(JavaPunctuator.LWING), b.zeroOrMore(ANNOTATION_TYPE_ELEMENT_DECLARATION()), b.token(JavaPunctuator.RWING)));
  }

  public JavaTree ANNOTATION_TYPE_ELEMENT_DECLARATION() {
    return b.<JavaTree>nonterminal(JavaLexer.ANNOTATION_TYPE_ELEMENT_DECLARATION)
      .is(
        b.firstOf(
          f.completeAnnotationTypeMember(MODIFIERS(), ANNOTATION_TYPE_ELEMENT_REST()),
          b.token(JavaPunctuator.SEMI)));
  }

  public JavaTree ANNOTATION_TYPE_ELEMENT_REST() {
    return b.<JavaTree>nonterminal(JavaLexer.ANNOTATION_TYPE_ELEMENT_REST)
      .is(
        b.firstOf(
          f.completeAnnotationMethod(
            TYPE(), b.token(JavaTokenType.IDENTIFIER), ANNOTATION_METHOD_REST(), b.token(JavaPunctuator.SEMI)),
          FIELD_DECLARATION(),
          CLASS_DECLARATION(),
          ENUM_DECLARATION(),
          INTERFACE_DECLARATION(),
          ANNOTATION_TYPE_DECLARATION()));
  }

  public MethodTreeImpl ANNOTATION_METHOD_REST() {
    return b.<MethodTreeImpl>nonterminal(JavaLexer.ANNOTATION_METHOD_REST)
      .is(
        f.newAnnotationTypeMethod(
          b.token(JavaPunctuator.LPAR),
          b.token(JavaPunctuator.RPAR),
          b.optional(DEFAULT_VALUE())));
  }

  public Tuple<InternalSyntaxToken, ExpressionTree> DEFAULT_VALUE() {
    return b.<Tuple<InternalSyntaxToken, ExpressionTree>>nonterminal(JavaLexer.DEFAULT_VALUE)
      .is(f.newDefaultValue(
          b.token(JavaKeyword.DEFAULT),
          ELEMENT_VALUE()));
  }

  public AnnotationTreeImpl ANNOTATION() {
    return b.<AnnotationTreeImpl>nonterminal(JavaLexer.ANNOTATION)
      .is(
        f.newAnnotation(
          b.token(JavaPunctuator.AT),
          f.annotationIdentifier(b.token(JavaTokenType.IDENTIFIER),
              b.zeroOrMore(f.newTuple8(b.token(JavaPunctuator.DOT), b.token(JavaTokenType.IDENTIFIER)))),
          b.optional(ANNOTATION_REST())));
  }

  public ArgumentListTreeImpl ANNOTATION_REST() {
    return b.<ArgumentListTreeImpl>nonterminal(JavaLexer.ANNOTATION_REST)
      .is(
        b.firstOf(
          NORMAL_ANNOTATION_REST(),
          SINGLE_ELEMENT_ANNOTATION_REST()));
  }

  public ArgumentListTreeImpl NORMAL_ANNOTATION_REST() {
    return b.<ArgumentListTreeImpl>nonterminal(JavaLexer.NORMAL_ANNOTATION_REST)
      .is(
        f.completeNormalAnnotation(
          b.token(JavaPunctuator.LPAR),
          b.optional(ELEMENT_VALUE_PAIRS()),
          b.token(JavaPunctuator.RPAR)));
  }

  public ArgumentListTreeImpl ELEMENT_VALUE_PAIRS() {
    return b.<ArgumentListTreeImpl>nonterminal(JavaLexer.ELEMENT_VALUE_PAIRS)
      .is(
        f.newNormalAnnotation(
          ELEMENT_VALUE_PAIR(), b.zeroOrMore(f.newTuple24(b.token(JavaPunctuator.COMMA), ELEMENT_VALUE_PAIR()))));
  }

  public AssignmentExpressionTreeImpl ELEMENT_VALUE_PAIR() {
    return b.<AssignmentExpressionTreeImpl>nonterminal(JavaLexer.ELEMENT_VALUE_PAIR)
      .is(
        f.newElementValuePair(
          b.token(JavaTokenType.IDENTIFIER),
          b.token(JavaPunctuator.EQU),
          ELEMENT_VALUE()));
  }

  public ExpressionTree ELEMENT_VALUE() {
    return b.<ExpressionTree>nonterminal(JavaLexer.ELEMENT_VALUE)
      .is(
        b.firstOf(
          CONDITIONAL_EXPRESSION(),
          ANNOTATION(),
          ELEMENT_VALUE_ARRAY_INITIALIZER()));
  }

  public NewArrayTreeImpl ELEMENT_VALUE_ARRAY_INITIALIZER() {
    return b.<NewArrayTreeImpl>nonterminal(JavaLexer.ELEMENT_VALUE_ARRAY_INITIALIZER)
      .is(
        f.completeElementValueArrayInitializer(
          b.token(JavaPunctuator.LWING),
          b.optional(ELEMENT_VALUES()),
          b.token(JavaPunctuator.RWING)));
  }

  public NewArrayTreeImpl ELEMENT_VALUES() {
    return b.<NewArrayTreeImpl>nonterminal(JavaLexer.ELEMENT_VALUES)
      .is(
        f.newElementValueArrayInitializer(
          b.oneOrMore(f.newTuple23(ELEMENT_VALUE(), b.optional(b.token(JavaPunctuator.COMMA))))));
  }

  public ArgumentListTreeImpl SINGLE_ELEMENT_ANNOTATION_REST() {
    return b.<ArgumentListTreeImpl>nonterminal(JavaLexer.SINGLE_ELEMENT_ANNOTATION_REST)
      .is(f.newSingleElementAnnotation(b.token(JavaPunctuator.LPAR), ELEMENT_VALUE(), b.token(JavaPunctuator.RPAR)));
  }

  // End of annotations

  // Formal parameters

  public FormalParametersListTreeImpl FORMAL_PARAMETERS() {
    return b.<FormalParametersListTreeImpl>nonterminal(JavaLexer.FORMAL_PARAMETERS)
      .is(
        f.completeParenFormalParameters(
          b.token(JavaPunctuator.LPAR),
          b.optional(FORMAL_PARAMETERS_DECLS()),
          b.token(JavaPunctuator.RPAR)));
  }

  public FormalParametersListTreeImpl FORMAL_PARAMETERS_DECLS() {
    return b.<FormalParametersListTreeImpl>nonterminal(JavaLexer.FORMAL_PARAMETER_DECLS)
      .is(
        f.completeTypeFormalParameters(
          MODIFIERS(),
          TYPE(),
          FORMAL_PARAMETERS_DECLS_REST()));
  }

  public FormalParametersListTreeImpl FORMAL_PARAMETERS_DECLS_REST() {
    return b.<FormalParametersListTreeImpl>nonterminal(JavaLexer.FORMAL_PARAMETERS_DECLS_REST)
      .is(
        b.firstOf(
          f.prependNewFormalParameter(b.firstOf(RECEIVER_PARAMETER_ID(), VARIABLE_DECLARATOR_ID()),
              b.optional(f.newTuple18(b.token(JavaPunctuator.COMMA), FORMAL_PARAMETERS_DECLS()))),
          f.newVariableArgumentFormalParameter(b.zeroOrMore(ANNOTATION()), b.token(JavaPunctuator.ELLIPSIS), VARIABLE_DECLARATOR_ID())));
  }

  public VariableTreeImpl RECEIVER_PARAMETER_ID() {
    return b.<VariableTreeImpl>nonterminal(JavaLexer.RECEIVER_PARAMETER_ID).is(
      f.receiverParameterId(b.zeroOrMore(f.newTuple9(b.token(JavaTokenType.IDENTIFIER), b.token(JavaPunctuator.DOT))), b.token(JavaKeyword.THIS)));
  }

  public VariableTreeImpl VARIABLE_DECLARATOR_ID() {
    return b.<VariableTreeImpl>nonterminal(JavaLexer.VARIABLE_DECLARATOR_ID)
      .is(
        f.newVariableDeclaratorId(
          b.token(JavaTokenType.IDENTIFIER),
          b.zeroOrMore(ANNOTATED_DIMENSION())));
  }

  public VariableTreeImpl FORMAL_PARAMETER() {
    // TODO Dim
    return b.<VariableTreeImpl>nonterminal(JavaLexer.FORMAL_PARAMETER)
      .is(
        f.newFormalParameter(
          MODIFIERS(),
          LOCAL_VARIABLE_TYPE(),
          VARIABLE_DECLARATOR_ID()));
  }

  // End of formal parameters

  // Statements

  public VariableDeclaratorListTreeImpl LOCAL_VARIABLE_DECLARATION_STATEMENT() {
    return b.<VariableDeclaratorListTreeImpl>nonterminal(JavaLexer.LOCAL_VARIABLE_DECLARATION_STATEMENT)
      .is(f.completeLocalVariableDeclaration(MODIFIERS(), LOCAL_VARIABLE_TYPE(), VARIABLE_DECLARATORS(), b.token(JavaPunctuator.SEMI)));
  }

  public TypeTree LOCAL_VARIABLE_TYPE() {
    return b.<TypeTree>nonterminal(JavaLexer.LOCAL_VARIABLE_TYPE)
      .is(b.firstOf(VAR_TYPE(), TYPE()));
  }

  public VarTypeTreeImpl VAR_TYPE() {
    return b.<VarTypeTreeImpl>nonterminal(JavaLexer.VAR_TYPE)
      .is(f.newVarType(b.token(JavaSpecialIdentifier.VAR)));
  }

  public VariableDeclaratorListTreeImpl VARIABLE_DECLARATORS() {
    return b.<VariableDeclaratorListTreeImpl>nonterminal(JavaLexer.VARIABLE_DECLARATORS)
      .is(f.newVariableDeclarators(VARIABLE_DECLARATOR(), b.zeroOrMore(f.newTuple3(b.token(JavaPunctuator.COMMA), VARIABLE_DECLARATOR()))));
  }

  public VariableTreeImpl VARIABLE_DECLARATOR() {
    return b.<VariableTreeImpl>nonterminal(JavaLexer.VARIABLE_DECLARATOR)
      .is(
        f.completeVariableDeclarator(
          b.token(JavaTokenType.IDENTIFIER), b.zeroOrMore(ANNOTATED_DIMENSION()),
          b.optional(
            f.newVariableDeclarator(b.token(JavaPunctuator.EQU), VARIABLE_INITIALIZER()))));
  }

  public StatementTree STATEMENT() {
    return b.<StatementTree>nonterminal(JavaLexer.STATEMENT)
      .is(
        b.firstOf(
          BLOCK(),
          ASSERT_STATEMENT(),
          IF_STATEMENT(),
          FOR_STATEMENT(),
          WHILE_STATEMENT(),
          DO_WHILE_STATEMENT(),
          TRY_STATEMENT(),
          SWITCH_STATEMENT(),
          SYNCHRONIZED_STATEMENT(),
          RETURN_STATEMENT(),
          THROW_STATEMENT(),
          BREAK_STATEMENT(),
          CONTINUE_STATEMENT(),
          LABELED_STATEMENT(),
          EXPRESSION_STATEMENT(),
          EMPTY_STATEMENT()));
  }

  public BlockTreeImpl BLOCK() {
    return b.<BlockTreeImpl>nonterminal(JavaLexer.BLOCK)
      .is(f.block(b.token(JavaPunctuator.LWING), BLOCK_STATEMENTS(), b.token(JavaPunctuator.RWING)));
  }

  public AssertStatementTreeImpl ASSERT_STATEMENT() {
    return b.<AssertStatementTreeImpl>nonterminal(JavaLexer.ASSERT_STATEMENT)
      .is(f.completeAssertStatement(
        b.token(JavaKeyword.ASSERT), EXPRESSION(),
        b.optional(
          f.newAssertStatement(b.token(JavaPunctuator.COLON), EXPRESSION())),
        b.token(JavaPunctuator.SEMI)));
  }

  public IfStatementTreeImpl IF_STATEMENT() {
    return b.<IfStatementTreeImpl>nonterminal(JavaLexer.IF_STATEMENT)
      .is(
        f.completeIf(
          b.token(JavaKeyword.IF), b.token(JavaPunctuator.LPAR), EXPRESSION(), b.token(JavaPunctuator.RPAR),
          STATEMENT(),
          b.optional(
            f.newIfWithElse(b.token(JavaKeyword.ELSE), STATEMENT()))));
  }

  public StatementTree FOR_STATEMENT() {
    return b.<StatementTree>nonterminal(JavaLexer.FOR_STATEMENT)
      .is(
        b.<StatementTree>firstOf(
          STANDARD_FOR_STATEMENT(),
          FOREACH_STATEMENT()));
  }

  public ForStatementTreeImpl STANDARD_FOR_STATEMENT() {
    return b.<ForStatementTreeImpl>nonterminal()
      .is(
        f.newStandardForStatement(
          b.token(JavaKeyword.FOR),
          b.token(JavaPunctuator.LPAR),
          b.optional(FOR_INIT()), b.token(JavaPunctuator.SEMI),
          b.optional(EXPRESSION()), b.token(JavaPunctuator.SEMI),
          b.optional(FOR_UPDATE()),
          b.token(JavaPunctuator.RPAR),
          STATEMENT()));
  }

  public StatementExpressionListTreeImpl FOR_INIT() {
    return b.<StatementExpressionListTreeImpl>nonterminal()
      .is(
        b.firstOf(
          FOR_INIT_DECLARATION(),
          FOR_INIT_EXPRESSIONS()));
  }

  public StatementExpressionListTreeImpl FOR_INIT_DECLARATION() {
    return b.<StatementExpressionListTreeImpl>nonterminal()
      .is(f.newForInitDeclaration(MODIFIERS(), LOCAL_VARIABLE_TYPE(), VARIABLE_DECLARATORS()));
  }

  public StatementExpressionListTreeImpl FOR_INIT_EXPRESSIONS() {
    return b.<StatementExpressionListTreeImpl>nonterminal()
      .is(STATEMENT_EXPRESSIONS());
  }

  public StatementExpressionListTreeImpl FOR_UPDATE() {
    return b.<StatementExpressionListTreeImpl>nonterminal()
      .is(STATEMENT_EXPRESSIONS());
  }

  public StatementExpressionListTreeImpl STATEMENT_EXPRESSIONS() {
    return b.<StatementExpressionListTreeImpl>nonterminal()
      .is(
        f.newStatementExpressions(
          EXPRESSION(), b.zeroOrMore(f.newTuple25(b.token(JavaPunctuator.COMMA), EXPRESSION()))));
  }

  public ForEachStatementImpl FOREACH_STATEMENT() {
    return b.<ForEachStatementImpl>nonterminal()
      .is(
        f.newForeachStatement(
          b.token(JavaKeyword.FOR),
          b.token(JavaPunctuator.LPAR), FORMAL_PARAMETER(), b.token(JavaPunctuator.COLON), EXPRESSION(), b.token(JavaPunctuator.RPAR),
          STATEMENT()));
  }

  public WhileStatementTreeImpl WHILE_STATEMENT() {
    return b.<WhileStatementTreeImpl>nonterminal(JavaLexer.WHILE_STATEMENT)
      .is(f.whileStatement(b.token(JavaKeyword.WHILE), b.token(JavaPunctuator.LPAR), EXPRESSION(), b.token(JavaPunctuator.RPAR),
        STATEMENT()));
  }

  public DoWhileStatementTreeImpl DO_WHILE_STATEMENT() {
    return b.<DoWhileStatementTreeImpl>nonterminal(JavaLexer.DO_STATEMENT)
      .is(
        f.doWhileStatement(b.token(JavaKeyword.DO), STATEMENT(),
          b.token(JavaKeyword.WHILE), b.token(JavaPunctuator.LPAR), EXPRESSION(), b.token(JavaPunctuator.RPAR),
          b.token(JavaPunctuator.SEMI)));
  }

  public TryStatementTreeImpl TRY_STATEMENT() {
    return b.<TryStatementTreeImpl>nonterminal(JavaLexer.TRY_STATEMENT)
      .is(
        b.firstOf(
          STANDARD_TRY_STATEMENT(),
          TRY_WITH_RESOURCES_STATEMENT()));
  }

  public TryStatementTreeImpl STANDARD_TRY_STATEMENT() {
    return b.<TryStatementTreeImpl>nonterminal()
      .is(
        f.completeStandardTryStatement(
          b.token(JavaKeyword.TRY),
          BLOCK(),
          b.firstOf(
            f.newTryCatch(b.zeroOrMore(CATCH_CLAUSE()), b.optional(FINALLY())),
            FINALLY())));
  }

  public CatchTreeImpl CATCH_CLAUSE() {
    return b.<CatchTreeImpl>nonterminal(JavaLexer.CATCH_CLAUSE)
      .is(
        f.newCatchClause(
          b.token(JavaKeyword.CATCH), b.token(JavaPunctuator.LPAR), CATCH_FORMAL_PARAMETER(), b.token(JavaPunctuator.RPAR), BLOCK()));
  }

  public VariableTreeImpl CATCH_FORMAL_PARAMETER() {
    return b.<VariableTreeImpl>nonterminal()
      .is(
        f.newCatchFormalParameter(MODIFIERS(), CATCH_TYPE(), VARIABLE_DECLARATOR_ID()));
  }

  public TypeTree CATCH_TYPE() {
    return b.<TypeTree>nonterminal()
      .is(
        f.newCatchType(TYPE_QUALIFIED_IDENTIFIER(), b.zeroOrMore(f.newTuple26(b.token(JavaPunctuator.OR), TYPE_QUALIFIED_IDENTIFIER()))));
  }

  public TryStatementTreeImpl FINALLY() {
    return b.<TryStatementTreeImpl>nonterminal(JavaLexer.FINALLY_)
      .is(
        f.newFinallyBlock(b.token(JavaKeyword.FINALLY), BLOCK()));
  }

  public TryStatementTreeImpl TRY_WITH_RESOURCES_STATEMENT() {
    return b.<TryStatementTreeImpl>nonterminal()
      .is(
        f.newTryWithResourcesStatement(
          b.token(JavaKeyword.TRY),
          b.token(JavaPunctuator.LPAR),
          RESOURCES(),
          b.token(JavaPunctuator.RPAR),
          BLOCK(),
          b.zeroOrMore(CATCH_CLAUSE()),
          b.optional(FINALLY())));
  }

  public ResourceListTreeImpl RESOURCES() {
    return b.<ResourceListTreeImpl>nonterminal()
      .is(
        f.newResources(b.oneOrMore(f.newTuple27(RESOURCE(), b.optional(b.token(JavaPunctuator.SEMI))))));
  }

  public Tree RESOURCE() {
    return b.<Tree>nonterminal(JavaLexer.RESOURCE)
      .is(b.firstOf(
        f.newResource(
          MODIFIERS(),
          b.firstOf(
            VAR_TYPE(),
            TYPE_QUALIFIED_IDENTIFIER()),
          VARIABLE_DECLARATOR_ID(),
          b.token(JavaPunctuator.EQU),
          EXPRESSION()),
        PRIMARY_WITH_SELECTOR()));
  }

  public SwitchStatementTree SWITCH_STATEMENT() {
    return b.<SwitchStatementTree>nonterminal(JavaLexer.SWITCH_STATEMENT)
      .is(f.switchStatement(SWITCH_EXPRESSION()));
  }

  public SwitchExpressionTree SWITCH_EXPRESSION() {
    return b.<SwitchExpressionTree>nonterminal(JavaLexer.SWITCH_EXPRESSION)
      .is(
        f.switchExpression(
          b.token(JavaKeyword.SWITCH), b.token(JavaPunctuator.LPAR), EXPRESSION(), b.token(JavaPunctuator.RPAR),
          b.token(JavaPunctuator.LWING),
          b.zeroOrMore(SWITCH_GROUP()),
          b.token(JavaPunctuator.RWING)));
  }

  public CaseGroupTreeImpl SWITCH_GROUP() {
    return b.<CaseGroupTreeImpl>nonterminal(JavaLexer.SWITCH_BLOCK_STATEMENT_GROUP)
      .is(f.switchGroup(b.oneOrMore(SWITCH_CASE_OR_DEFAULT_CLAUSE()), BLOCK_STATEMENTS()));
  }

  public CaseLabelTreeImpl SWITCH_CASE_OR_DEFAULT_CLAUSE() {
    return b.<CaseLabelTreeImpl>nonterminal(JavaLexer.SWITCH_LABEL)
      .is(
        b.firstOf(
          f.newSwitchCase(
            b.token(JavaKeyword.CASE),
            SWITCH_CASE_EXPRESSION_LIST(),
            b.firstOf(
              b.token(JavaPunctuator.COLON),
              b.token(JavaLexer.ARROW))),
          f.newSwitchDefault(
            b.token(JavaKeyword.DEFAULT),
            b.firstOf(
              b.token(JavaPunctuator.COLON),
              b.token(JavaLexer.ARROW)))));
  }

  public ArgumentListTreeImpl SWITCH_CASE_EXPRESSION_LIST() {
    return b.<ArgumentListTreeImpl>nonterminal(JavaLexer.SWITCH_CASE_EXPRESSION_LIST)
      .is(f.newArguments(
          EXPRESSION_NOT_LAMBDA(),
          b.zeroOrMore(f.newTuple20(b.token(JavaPunctuator.COMMA), EXPRESSION_NOT_LAMBDA()))));
  }

  public SynchronizedStatementTreeImpl SYNCHRONIZED_STATEMENT() {
    return b.<SynchronizedStatementTreeImpl>nonterminal(JavaLexer.SYNCHRONIZED_STATEMENT)
      .is(
        f.synchronizedStatement(b.token(JavaKeyword.SYNCHRONIZED), b.token(JavaPunctuator.LPAR), EXPRESSION(), b.token(JavaPunctuator.RPAR),
          BLOCK()));
  }

  public BreakStatementTreeImpl BREAK_STATEMENT() {
    return b.<BreakStatementTreeImpl>nonterminal(JavaLexer.BREAK_STATEMENT)
      .is(f.breakStatement(b.token(JavaKeyword.BREAK), b.optional(EXPRESSION()), b.token(JavaPunctuator.SEMI)));
  }

  public ContinueStatementTreeImpl CONTINUE_STATEMENT() {
    return b.<ContinueStatementTreeImpl>nonterminal(JavaLexer.CONTINUE_STATEMENT)
      .is(f.continueStatement(b.token(JavaKeyword.CONTINUE), b.optional(b.token(JavaTokenType.IDENTIFIER)), b.token(JavaPunctuator.SEMI)));
  }

  public ReturnStatementTreeImpl RETURN_STATEMENT() {
    return b.<ReturnStatementTreeImpl>nonterminal(JavaLexer.RETURN_STATEMENT)
      .is(f.returnStatement(b.token(JavaKeyword.RETURN), b.optional(EXPRESSION()), b.token(JavaPunctuator.SEMI)));
  }

  public ThrowStatementTreeImpl THROW_STATEMENT() {
    return b.<ThrowStatementTreeImpl>nonterminal(JavaLexer.THROW_STATEMENT)
      .is(f.throwStatement(b.token(JavaKeyword.THROW), EXPRESSION(), b.token(JavaPunctuator.SEMI)));
  }

  public LabeledStatementTreeImpl LABELED_STATEMENT() {
    return b.<LabeledStatementTreeImpl>nonterminal(JavaLexer.LABELED_STATEMENT)
      .is(f.labeledStatement(b.token(IDENTIFIER), b.token(COLON), STATEMENT()));
  }

  public ExpressionStatementTreeImpl EXPRESSION_STATEMENT() {
    return b.<ExpressionStatementTreeImpl>nonterminal(JavaLexer.EXPRESSION_STATEMENT)
      .is(f.expressionStatement(EXPRESSION(), b.token(JavaPunctuator.SEMI)));
  }

  public EmptyStatementTreeImpl EMPTY_STATEMENT() {
    return b.<EmptyStatementTreeImpl>nonterminal(JavaLexer.EMPTY_STATEMENT)
      .is(f.emptyStatement(b.token(JavaPunctuator.SEMI)));
  }

  public BlockStatementListTreeImpl BLOCK_STATEMENTS() {
    return b.<BlockStatementListTreeImpl>nonterminal(JavaLexer.BLOCK_STATEMENTS)
      .is(f.blockStatements(b.zeroOrMore(BLOCK_STATEMENT())));
  }

  public BlockStatementListTreeImpl BLOCK_STATEMENT() {
    return b.<BlockStatementListTreeImpl>nonterminal(JavaLexer.BLOCK_STATEMENT)
      .is(
        b.firstOf(
          f.wrapInBlockStatements(LOCAL_VARIABLE_DECLARATION_STATEMENT()),
          f.newInnerClassOrEnum(
            MODIFIERS(),
            b.firstOf(
              CLASS_DECLARATION(),
              ENUM_DECLARATION())),
          f.wrapInBlockStatements(STATEMENT())));
  }

  // End of statements

  // Expressions

  public ExpressionTree EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaLexer.EXPRESSION)
      .is(b.firstOf(
        LAMBDA_EXPRESSION(),
        ASSIGNMENT_EXPRESSION()));
  }

  public ExpressionTree EXPRESSION_NOT_LAMBDA() {
    return b.<ExpressionTree>nonterminal(JavaLexer.EXPRESSION_NOT_LAMBDA)
      .is(ASSIGNMENT_EXPRESSION());
  }

  public ExpressionTree ASSIGNMENT_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaLexer.ASSIGNMENT_EXPRESSION)
      .is(
        f.assignmentExpression(
          CONDITIONAL_EXPRESSION(),
          b.zeroOrMore(
            f.newOperatorAndOperand11(
              b.firstOf(
                b.token(JavaPunctuator.EQU),
                b.token(JavaPunctuator.PLUSEQU),
                b.token(JavaPunctuator.MINUSEQU),
                b.token(JavaPunctuator.STAREQU),
                b.token(JavaPunctuator.DIVEQU),
                b.token(JavaPunctuator.ANDEQU),
                b.token(JavaPunctuator.OREQU),
                b.token(JavaPunctuator.HATEQU),
                b.token(JavaPunctuator.MODEQU),
                b.token(JavaPunctuator.SLEQU),
                b.token(JavaPunctuator.SREQU),
                b.token(JavaPunctuator.BSREQU)),
              b.firstOf(
                LAMBDA_EXPRESSION(),
                CONDITIONAL_EXPRESSION())))));
  }

  public ExpressionTree CONDITIONAL_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaLexer.CONDITIONAL_EXPRESSION)
      .is(
        f.completeTernaryExpression(
          CONDITIONAL_OR_EXPRESSION(),
          b.optional(
            f.newTernaryExpression(
              b.token(JavaPunctuator.QUERY),
              EXPRESSION(),
              b.token(JavaPunctuator.COLON),
              b.firstOf(
                LAMBDA_EXPRESSION(),
                CONDITIONAL_EXPRESSION())))));
  }

  public ExpressionTree CONDITIONAL_OR_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaLexer.CONDITIONAL_OR_EXPRESSION)
      .is(
        f.binaryExpression10(
          CONDITIONAL_AND_EXPRESSION(),
          b.zeroOrMore(
            f.newOperatorAndOperand10(
              b.token(JavaPunctuator.OROR),
              CONDITIONAL_AND_EXPRESSION()))));
  }

  public ExpressionTree CONDITIONAL_AND_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaLexer.CONDITIONAL_AND_EXPRESSION)
      .is(
        f.binaryExpression9(
          INCLUSIVE_OR_EXPRESSION(),
          b.zeroOrMore(
            f.newOperatorAndOperand9(
              b.token(JavaPunctuator.ANDAND),
              INCLUSIVE_OR_EXPRESSION()))));
  }

  public ExpressionTree INCLUSIVE_OR_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaLexer.INCLUSIVE_OR_EXPRESSION)
      .is(
        f.binaryExpression8(
          EXCLUSIVE_OR_EXPRESSION(),
          b.zeroOrMore(
            f.newOperatorAndOperand8(
              b.token(JavaPunctuator.OR),
              EXCLUSIVE_OR_EXPRESSION()))));
  }

  public ExpressionTree EXCLUSIVE_OR_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaLexer.EXCLUSIVE_OR_EXPRESSION)
      .is(
        f.binaryExpression7(
          AND_EXPRESSION(),
          b.zeroOrMore(
            f.newOperatorAndOperand7(
              b.token(JavaPunctuator.HAT),
              AND_EXPRESSION()))));
  }

  public ExpressionTree AND_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaLexer.AND_EXPRESSION)
      .is(
        f.binaryExpression6(
          EQUALITY_EXPRESSION(),
          b.zeroOrMore(
            f.newOperatorAndOperand6(
              b.token(JavaPunctuator.AND),
              EQUALITY_EXPRESSION()))));
  }

  public ExpressionTree EQUALITY_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaLexer.EQUALITY_EXPRESSION)
      .is(
        f.binaryExpression5(
          INSTANCEOF_EXPRESSION(),
          b.zeroOrMore(
            f.newOperatorAndOperand5(
              b.firstOf(
                b.token(JavaPunctuator.EQUAL),
                b.token(JavaPunctuator.NOTEQUAL)),
              INSTANCEOF_EXPRESSION()))));
  }

  public ExpressionTree INSTANCEOF_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaLexer.RELATIONAL_EXPRESSION)
      .is(
        f.completeInstanceofExpression(
          RELATIONAL_EXPRESSION(),
          b.optional(f.newInstanceofExpression(b.token(JavaKeyword.INSTANCEOF), TYPE()))));
  }

  public ExpressionTree RELATIONAL_EXPRESSION() {
    return b.<ExpressionTree>nonterminal()
      .is(
        f.binaryExpression4(
          SHIFT_EXPRESSION(),
          b.zeroOrMore(
            f.newOperatorAndOperand4(
              b.firstOf(
                b.token(JavaPunctuator.GE),
                b.token(JavaPunctuator.GT),
                b.token(JavaPunctuator.LE),
                b.token(JavaPunctuator.LT)),
              SHIFT_EXPRESSION()))));
  }

  public ExpressionTree SHIFT_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaLexer.SHIFT_EXPRESSION)
      .is(
        f.binaryExpression3(
          ADDITIVE_EXPRESSION(),
          b.zeroOrMore(
            f.newOperatorAndOperand3(
              b.firstOf(
                b.token(JavaPunctuator.SL),
                b.token(JavaPunctuator.BSR),
                b.token(JavaPunctuator.SR)),
              ADDITIVE_EXPRESSION()))));
  }

  public ExpressionTree ADDITIVE_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaLexer.ADDITIVE_EXPRESSION)
      .is(
        f.binaryExpression2(
          MULTIPLICATIVE_EXPRESSION(),
          b.zeroOrMore(
            f.newOperatorAndOperand2(
              b.firstOf(
                b.token(JavaPunctuator.PLUS),
                b.token(JavaPunctuator.MINUS)),
              MULTIPLICATIVE_EXPRESSION()))));
  }

  public ExpressionTree MULTIPLICATIVE_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaLexer.MULTIPLICATIVE_EXPRESSION)
      .is(
        f.binaryExpression1(
          UNARY_EXPRESSION(),
          b.zeroOrMore(
            f.newOperatorAndOperand1(
              b.firstOf(
                b.token(JavaPunctuator.STAR),
                b.token(JavaPunctuator.DIV),
                b.token(JavaPunctuator.MOD)),
              UNARY_EXPRESSION()))));
  }

  public ExpressionTree UNARY_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaLexer.UNARY_EXPRESSION)
      .is(
        b.firstOf(
          f.newPrefixedExpression(
            b.firstOf(
              b.token(JavaPunctuator.INC),
              b.token(JavaPunctuator.DEC),
              b.token(JavaPunctuator.PLUS),
              b.token(JavaPunctuator.MINUS)),
            UNARY_EXPRESSION()),
          UNARY_EXPRESSION_NOT_PLUS_MINUS()));
  }

  public ExpressionTree UNARY_EXPRESSION_NOT_PLUS_MINUS() {
    return b.<ExpressionTree>nonterminal(JavaLexer.UNARY_EXPRESSION_NOT_PLUS_MINUS)
      .is(
        b.firstOf(
          CAST_EXPRESSION(),
          METHOD_REFERENCE(),
          // TODO Extract postfix expressions somewhere else
          f.newPostfixExpression(
            PRIMARY_WITH_SELECTOR(),
            b.optional(
              b.firstOf(
                b.token(JavaPunctuator.INC),
                b.token(JavaPunctuator.DEC)))),
          f.newTildaExpression(b.token(JavaPunctuator.TILDA), UNARY_EXPRESSION()),
          f.newBangExpression(b.token(JavaPunctuator.BANG), UNARY_EXPRESSION()),
          SWITCH_EXPRESSION()));
  }

  public ExpressionTree PRIMARY_WITH_SELECTOR() {
    return b.<ExpressionTree>nonterminal().is(
      f.applySelectors1(PRIMARY(), b.zeroOrMore(SELECTOR()))
    );
  }

  public ExpressionTree CAST_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaLexer.CAST_EXPRESSION)
      .is(
        f.completeCastExpression(
          b.token(JavaPunctuator.LPAR),
          b.firstOf(
            f.newBasicTypeCastExpression(BASIC_TYPE(), b.token(JavaPunctuator.RPAR), UNARY_EXPRESSION()),
            f.newClassCastExpression(
                TYPE(),
                b.optional(f.newTuple29(b.token(JavaPunctuator.AND), BOUND())),
                b.token(JavaPunctuator.RPAR),
                b.firstOf(
                  LAMBDA_EXPRESSION(),
                  UNARY_EXPRESSION_NOT_PLUS_MINUS())))));
  }

  public ExpressionTree METHOD_REFERENCE() {
    return b.<ExpressionTree>nonterminal(JavaLexer.METHOD_REFERENCE)
      .is(
        f.completeMethodReference(
          b.firstOf(
            f.newSuperMethodReference(b.token(JavaKeyword.SUPER), b.token(JavaPunctuator.DBLECOLON)),
            f.newTypeMethodReference(TYPE(), b.token(JavaPunctuator.DBLECOLON)),
            // TODO This is a postfix expression followed by a double colon
            f.newPrimaryMethodReference(PRIMARY_WITH_SELECTOR(), b.token(JavaPunctuator.DBLECOLON))),
          b.optional(TYPE_ARGUMENTS()),
          b.firstOf(
            b.token(JavaKeyword.NEW),
            b.token(JavaTokenType.IDENTIFIER))));
  }

  public ExpressionTree PRIMARY() {
    return b.<ExpressionTree>nonterminal(JavaLexer.PRIMARY)
      .is(
        b.firstOf(
          IDENTIFIER_OR_METHOD_INVOCATION(),
          PARENTHESIZED_EXPRESSION(),
          LITERAL(),
          NEW_EXPRESSION(),
          BASIC_CLASS_EXPRESSION()));
  }

  public ExpressionTree LAMBDA_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaLexer.LAMBDA_EXPRESSION)
      .is(f.lambdaExpression(LAMBDA_PARAMETERS(), b.token(JavaLexer.ARROW), LAMBDA_BODY()));
  }

  public LambdaParameterListTreeImpl LAMBDA_PARAMETERS() {
    return b.<LambdaParameterListTreeImpl>nonterminal(JavaLexer.LAMBDA_PARAMETERS)
      .is(
        b.firstOf(
          MULTIPLE_INFERED_PARAMETERS(),
          f.formalLambdaParameters(FORMAL_PARAMETERS()),
          f.singleInferedParameter(INFERED_PARAMETER())));
  }

  public LambdaParameterListTreeImpl MULTIPLE_INFERED_PARAMETERS() {
    return b.<LambdaParameterListTreeImpl>nonterminal(JavaLexer.INFERED_PARAMS)
      .is(
        f.newInferedParameters(
          b.token(JavaPunctuator.LPAR),
          b.optional(
            f.newTuple2(
              INFERED_PARAMETER(),
              b.zeroOrMore(f.newTuple1(b.token(JavaPunctuator.COMMA), INFERED_PARAMETER())))),
          b.token(JavaPunctuator.RPAR)));
  }

  public VariableTreeImpl INFERED_PARAMETER() {
    return b.<VariableTreeImpl>nonterminal()
      .is(
        f.newSimpleParameter(b.token(JavaTokenType.IDENTIFIER)));
  }

  public Tree LAMBDA_BODY() {
    return b.<Tree>nonterminal(JavaLexer.LAMBDA_BODY)
      .is(
        b.firstOf(
          BLOCK(),
          EXPRESSION()));
  }

  public ParenthesizedTreeImpl PARENTHESIZED_EXPRESSION() {
    return b.<ParenthesizedTreeImpl>nonterminal(JavaLexer.PAR_EXPRESSION)
      .is(f.parenthesizedExpression(b.token(JavaPunctuator.LPAR), EXPRESSION(), b.token(JavaPunctuator.RPAR)));
  }

  public ExpressionTree NEW_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaLexer.NEW_EXPRESSION)
      .is(f.newExpression(b.token(JavaKeyword.NEW), b.zeroOrMore(ANNOTATION()), CREATOR()));
  }

  public ExpressionTree CREATOR() {
    return b.<ExpressionTree>nonterminal(JavaLexer.CREATOR)
      .is(
        b.firstOf(
          f.newClassCreator(b.optional(TYPE_ARGUMENTS()), TYPE_QUALIFIED_IDENTIFIER(), CLASS_CREATOR_REST()),
          f.newArrayCreator(
            b.firstOf(
              TYPE_QUALIFIED_IDENTIFIER(),
              BASIC_TYPE()),
            ARRAY_CREATOR_REST())));
  }

  public NewArrayTreeImpl ARRAY_CREATOR_REST() {
    return b.<NewArrayTreeImpl>nonterminal(JavaLexer.ARRAY_CREATOR_REST)
      .is(
        f.completeArrayCreator(
          b.zeroOrMore(ANNOTATION()),
          b.firstOf(
            f.newArrayCreatorWithInitializer(
              b.token(JavaPunctuator.LBRK), b.token(JavaPunctuator.RBRK), b.zeroOrMore(ANNOTATED_DIMENSION()), ARRAY_INITIALIZER()),
            f.newArrayCreatorWithDimension(
              b.token(JavaPunctuator.LBRK), EXPRESSION(), b.token(JavaPunctuator.RBRK),
              b.zeroOrMore(ARRAY_ACCESS_EXPRESSION()),
              b.zeroOrMore(ANNOTATED_DIMENSION())))));
  }

  // TODO This method should go away
  public ExpressionTree BASIC_CLASS_EXPRESSION() {
    return b
      .<ExpressionTree>nonterminal(JavaLexer.BASIC_CLASS_EXPRESSION)
      .is(
        f.basicClassExpression(BASIC_TYPE(), b.zeroOrMore(DIMENSION()), b.token(JavaPunctuator.DOT), b.token(JavaKeyword.CLASS)));
  }

  public PrimitiveTypeTreeImpl BASIC_TYPE() {
    return b.<PrimitiveTypeTreeImpl>nonterminal(JavaLexer.BASIC_TYPE)
      .is(
        f.newBasicType(
          b.zeroOrMore(ANNOTATION()),
          b.firstOf(
            b.token(JavaKeyword.BYTE),
            b.token(JavaKeyword.SHORT),
            b.token(JavaKeyword.CHAR),
            b.token(JavaKeyword.INT),
            b.token(JavaKeyword.LONG),
            b.token(JavaKeyword.FLOAT),
            b.token(JavaKeyword.DOUBLE),
            b.token(JavaKeyword.BOOLEAN),
            b.token(JavaKeyword.VOID))));
  }

  public ArgumentListTreeImpl ARGUMENTS() {
    return b.<ArgumentListTreeImpl>nonterminal(JavaLexer.ARGUMENTS)
      .is(
        f.completeArguments(
            b.token(JavaPunctuator.LPAR),
            b.optional(
                f.newArguments(
                    EXPRESSION(),
                    b.zeroOrMore(f.newTuple20(b.token(JavaPunctuator.COMMA), EXPRESSION())))),
            b.token(JavaPunctuator.RPAR)));
  }

  public <T extends Tree> T QUALIFIED_IDENTIFIER() {
    return b.<T>nonterminal(JavaLexer.QUALIFIED_IDENTIFIER)
      .is(
        f.<T>newQualifiedIdentifier(
            ANNOTATED_PARAMETERIZED_IDENTIFIER(), b.zeroOrMore(f.newTuple5(b.token(JavaPunctuator.DOT), ANNOTATED_PARAMETERIZED_IDENTIFIER()))));
  }

  private TypeTree TYPE_QUALIFIED_IDENTIFIER() {
    return QUALIFIED_IDENTIFIER();
  }

  public ExpressionTree ANNOTATED_PARAMETERIZED_IDENTIFIER() {
    return b.<ExpressionTree>nonterminal(JavaLexer.ANNOTATED_PARAMETERIZED_IDENTIFIER)
      .is(f.newAnnotatedParameterizedIdentifier(b.zeroOrMore(ANNOTATION()), b.token(JavaTokenType.IDENTIFIER), b.optional(TYPE_ARGUMENTS())));
  }

  public ExpressionTree VARIABLE_INITIALIZER() {
    return b.<ExpressionTree>nonterminal(JavaLexer.VARIABLE_INITIALIZER)
      .is(
        b.firstOf(
          EXPRESSION(),
          ARRAY_INITIALIZER()));
  }

  public NewArrayTreeImpl ARRAY_INITIALIZER() {
    return b.<NewArrayTreeImpl>nonterminal(JavaLexer.ARRAY_INITIALIZER)
      .is(
        f.newArrayInitializer(
          b.token(JavaPunctuator.LWING),
          b.optional(b.token(JavaPunctuator.COMMA)),
          b.zeroOrMore(f.newTuple28(VARIABLE_INITIALIZER(), b.optional(b.token(JavaPunctuator.COMMA)))),
          b.token(JavaPunctuator.RWING)));
  }

  public QualifiedIdentifierListTreeImpl QUALIFIED_IDENTIFIER_LIST() {
    return b.<QualifiedIdentifierListTreeImpl>nonterminal(JavaLexer.QUALIFIED_IDENTIFIER_LIST)
      .is(f.newQualifiedIdentifierList(TYPE_QUALIFIED_IDENTIFIER(), b.zeroOrMore(f.newTuple4(b.token(JavaPunctuator.COMMA), TYPE_QUALIFIED_IDENTIFIER()))));
  }

  public ArrayAccessExpressionTreeImpl ARRAY_ACCESS_EXPRESSION() {
    return b.<ArrayAccessExpressionTreeImpl>nonterminal(JavaLexer.DIM_EXPR)
      .is(f.newArrayAccessExpression(b.zeroOrMore(ANNOTATION()), b.token(JavaPunctuator.LBRK), EXPRESSION(), b.token(JavaPunctuator.RBRK)));
  }

  public NewClassTreeImpl CLASS_CREATOR_REST() {
    return b.<NewClassTreeImpl>nonterminal(JavaLexer.CLASS_CREATOR_REST)
      .is(f.newClassCreatorRest(ARGUMENTS(), b.optional(CLASS_BODY())));
  }

  public Tuple<Optional<List<AnnotationTreeImpl>>, Tuple<InternalSyntaxToken, InternalSyntaxToken>> ANNOTATED_DIMENSION() {
    return b.<Tuple<Optional<List<AnnotationTreeImpl>>, Tuple<InternalSyntaxToken, InternalSyntaxToken>>>nonterminal(JavaLexer.ANNOTATED_DIM)
      .is(f.newAnnotatedDimension(b.zeroOrMore(ANNOTATION()), DIMENSION()));
  }

  public Tuple<InternalSyntaxToken, InternalSyntaxToken> DIMENSION() {
    return b.<Tuple<InternalSyntaxToken, InternalSyntaxToken>>nonterminal(JavaLexer.DIM)
      .is(f.newTuple6(b.token(JavaPunctuator.LBRK), b.token(JavaPunctuator.RBRK)));
  }

  public Tuple<Optional<InternalSyntaxToken>, ExpressionTree> SELECTOR() {
    return b.<Tuple<Optional<InternalSyntaxToken>, ExpressionTree>>nonterminal(JavaLexer.SELECTOR)
      .is(
        b.firstOf(
          f.completeMemberSelectOrMethodSelector(b.token(JavaPunctuator.DOT), IDENTIFIER_OR_METHOD_INVOCATION()),
          // TODO Perhaps NEW_EXPRESSION() is not as good as before, as it allows NewArrayTree to be constructed
          f.completeCreatorSelector(b.token(JavaPunctuator.DOT), NEW_EXPRESSION()),
          f.<ExpressionTree>newTupleAbsent1(ARRAY_ACCESS_EXPRESSION()),
          f.newTupleAbsent2(f.newDotClassSelector(b.zeroOrMore(DIMENSION()), b.token(JavaPunctuator.DOT), b.token(JavaKeyword.CLASS)))
        ));
  }

  public ExpressionTree IDENTIFIER_OR_METHOD_INVOCATION() {
    return b.<ExpressionTree>nonterminal(JavaLexer.IDENTIFIER_OR_METHOD_INVOCATION)
      .is(
        f.newIdentifierOrMethodInvocation(
          b.optional(TYPE_ARGUMENTS()),
          b.firstOf(
            b.token(JavaTokenType.IDENTIFIER),
            b.token(JavaKeyword.THIS),
            b.token(JavaKeyword.SUPER)),
          b.optional(ARGUMENTS())));
  }

  // End of expressions

}
