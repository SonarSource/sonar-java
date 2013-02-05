/*
 * Sonar Java
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

import com.sonar.sslr.api.GenericTokenType;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.lexer.FloatLiteralChannel;
import org.sonar.java.ast.lexer.IntegerLiteralChannel;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerlessGrammarBuilder;
import org.sonar.sslr.parser.LexerlessGrammar;

public enum JavaGrammar implements GrammarRuleKey {

  COMPILATION_UNIT,
  PACKAGE_DECLARATION,
  IMPORT_DECLARATION,
  TYPE_DECLARATION,

  ANNOTATION,
  QUALIFIED_IDENTIFIER,

  MODIFIER,
  CLASS_DECLARATION,
  ENUM_DECLARATION,
  INTERFACE_DECLARATION,
  ANNOTATION_TYPE_DECLARATION,

  TYPE_PARAMETERS,
  CLASS_TYPE,
  CLASS_TYPE_LIST,
  CLASS_BODY,

  CLASS_BODY_DECLARATION,
  CLASS_INIT_DECLARATION,

  BLOCK,
  MEMBER_DECL,

  FIELD_DECLARATION,

  GENERIC_METHOD_OR_CONSTRUCTOR_REST,
  TYPE,
  METHOD_DECLARATOR_REST,
  VARIABLE_DECLARATORS,
  VOID_METHOD_DECLARATOR_REST,
  CONSTRUCTOR_DECLARATOR_REST,

  FORMAL_PARAMETERS,
  DIM,
  METHOD_BODY,

  INTERFACE_BODY,

  INTERFACE_BODY_DECLARATION,

  INTERFACE_MEMBER_DECL,

  INTERFACE_METHOD_OR_FIELD_DECL,
  INTERFACE_GENERIC_METHOD_DECL,
  VOID_INTERFACE_METHOD_DECLARATORS_REST,

  INTERFACE_METHOD_OR_FIELD_REST,

  CONSTANT_DECLARATORS_REST,
  INTERFACE_METHOD_DECLARATOR_REST,

  CONSTANT_DECLARATOR_REST,
  CONSTANT_DECLARATOR,

  VARIABLE_INITIALIZER,

  ENUM_BODY,

  ENUM_CONSTANTS,
  ENUM_BODY_DECLARATIONS,

  ENUM_CONSTANT,

  ARGUMENTS,

  LOCAL_VARIABLE_DECLARATION_STATEMENT,
  VARIABLE_MODIFIERS,
  VARIABLE_DECLARATOR,

  FORMAL_PARAMETER,
  FORMAL_PARAMETER_DECLS,
  FORMAL_PARAMETERS_DECLS_REST,

  VARIABLE_DECLARATOR_ID,

  BLOCK_STATEMENTS,
  BLOCK_STATEMENT,

  STATEMENT,
  LABELED_STATEMENT,
  EXPRESSION_STATEMENT,
  IF_STATEMENT,
  WHILE_STATEMENT,
  FOR_STATEMENT,
  ASSERT_STATEMENT,
  SWITCH_STATEMENT,
  DO_STATEMENT,
  BREAK_STATEMENT,
  CONTINUE_STATEMENT,
  RETURN_STATEMENT,
  SYNCHRONIZED_STATEMENT,
  THROW_STATEMENT,
  EMPTY_STATEMENT,

  EXPRESSION,
  RESOURCE,
  PAR_EXPRESSION,
  FOR_INIT,
  FOR_UPDATE,

  CATCH_CLAUSE,
  CATCH_FORMAL_PARAMETER,
  CATCH_TYPE,

  FINALLY_,
  SWITCH_BLOCK_STATEMENT_GROUPS,
  STATEMENT_EXPRESSION,

  TRY_STATEMENT,
  TRY_WITH_RESOURCES_STATEMENT,
  RESOURCE_SPECIFICATION,

  SWITCH_BLOCK_STATEMENT_GROUP,

  SWITCH_LABEL,

  CONSTANT_EXPRESSION,
  ENUM_CONSTANT_NAME,

  BASIC_TYPE,
  REFERENCE_TYPE,
  TYPE_ARGUMENTS,
  TYPE_ARGUMENT,
  TYPE_PARAMETER,
  BOUND,

  CONDITIONAL_EXPRESSION,
  DEFAULT_VALUE,

  ANNOTATION_TYPE_BODY,
  ANNOTATION_TYPE_ELEMENT_DECLARATION,
  ANNOTATION_TYPE_ELEMENT_REST,
  ANNOTATION_METHOD_OR_CONSTANT_REST,
  ANNOTATION_METHOD_REST,
  ANNOTATION_CONSTANT_REST,
  ANNOTATION_REST,
  NORMAL_ANNOTATION_REST,
  ELEMENT_VALUE_PAIRS,
  ELEMENT_VALUE_PAIR,
  ELEMENT_VALUE,
  ELEMENT_VALUE_ARRAY_INITIALIZER,
  ELEMENT_VALUES,
  SINGLE_ELEMENT_ANNOTATION_REST,

  ASSIGNMENT_EXPRESSION,
  ASSIGNMENT_OPERATOR,
  CONDITIONAL_OR_EXPRESSION,
  CONDITIONAL_AND_EXPRESSION,
  INCLUSIVE_OR_EXPRESSION,
  EXCLUSIVE_OR_EXPRESSION,
  AND_EXPRESSION,
  EQUALITY_EXPRESSION,
  RELATIONAL_EXPRESSION,
  SHIFT_EXPRESSION,
  ADDITIVE_EXPRESSION,
  MULTIPLICATIVE_EXPRESSION,
  UNARY_EXPRESSION,
  PREFIX_OP,
  PRIMARY,
  SELECTOR,
  POST_FIX_OP,
  NON_WILDCARD_TYPE_ARGUMENTS,
  EXPLICIT_GENERIC_INVOCATION_SUFFIX,
  SUPER_SUFFIX,
  LITERAL,
  CREATOR,
  IDENTIFIER_SUFFIX,
  EXPLICIT_GENERIC_INVOCATION,
  INNER_CREATOR,
  DIM_EXPR,
  CREATED_NAME,
  CLASS_CREATOR_REST,
  DIAMOND,
  ARRAY_CREATOR_REST,
  ARRAY_INITIALIZER,

  AT,
  AND,
  ANDAND,
  ANDEQU,
  BANG,
  BSR,
  BSREQU,
  COLON,
  COMMA,
  DEC,
  DIV,
  DIVEQU,
  DOT,
  ELLIPSIS,
  EQU,
  EQUAL,
  GE,
  GT,
  HAT,
  HATEQU,
  INC,
  LBRK,
  LT,
  LE,
  LPAR,
  LWING,
  MINUS,
  MINSEQU,
  MOD,
  MODEQU,
  NOTEQUAL,
  OR,
  OREQU,
  OROR,
  PLUS,
  PLUSEQU,
  QUERY,
  RBRK,
  RPAR,
  RWING,
  SEMI,
  SL,
  SLEQU,
  SR,
  SREQU,
  STAR,
  STAREQU,
  TILDA,

  LPOINT,
  RPOINT,

  ASSERT_KEYWORD,
  BREAK_KEYWORD,
  CASE_KEYWORD,
  CATCH_KEYWORD,
  CLASS_KEYWORD,
  CONTINUE_KEYWORD,
  DEFAULT_KEYWORD,
  DO_KEYWORD,
  ELSE_KEYWORD,
  ENUM_KEYWORD,
  EXTENDS_KEYWORD,
  FINALLY_KEYWORD,
  FINAL_KEYWORD,
  FOR_KEYWORD,
  IF_KEYWORD,
  IMPLEMENTS_KEYWORD,
  IMPORT_KEYWORD,
  INTERFACE_KEYWORD,
  INSTANCEOF_KEYWORD,
  NEW_KEYWORD,
  PACKAGE_KEYWORD,
  RETURN_KEYWORD,
  STATIC_KEYWORD,
  SUPER_KEYWORD,
  SWITCH_KEYWORD,
  SYNCHRONIZED_KEYWORD,
  THIS_KEYWORD,
  THROWS_KEYWORD,
  THROW_KEYWORD,
  TRY_KEYWORD,
  VOID_KEYWORD,
  WHILE_KEYWORD,
  TRUE_KEYWORD,
  FALSE_KEYWORD,
  NULL_KEYWORD,
  PUBLIC_KEYWORD,
  PROTECTED_KEYWORD,
  PRIVATE_KEYWORD,
  ABSTRACT_KEYWORD,
  NATIVE_KEYWORD,
  TRANSIENT_KEYWORD,
  VOLATILE_KEYWORD,
  STRICTFP_KEYWORD,
  BYTE_KEYWORD,
  SHORT_KEYWORD,
  CHAR_KEYWORD,
  INT_KEYWORD,
  LONG_KEYWORD,
  FLOAT_KEYWORD,
  DOUBLE_KEYWORD,
  BOOLEAN_KEYWORD,

  IDENTIFIER,
  EOF,
  DOUBLE_LITERAL,
  FLOAT_LITERAL,
  LONG_LITERAL,
  INTEGER_LITERAL,
  CHARACTER_LITERAL,
  STRING_LITERAL,

  LETTER_OR_DIGIT,
  KEYWORD,
  SPACING;

  public static LexerlessGrammar createGrammar() {
    return createGrammarBuilder().build();
  }

  public static LexerlessGrammarBuilder createGrammarBuilder() {
    LexerlessGrammarBuilder b = LexerlessGrammarBuilder.create();

    punctuators(b);
    keywords(b);

    compilationsUnits(b);
    classDeclaration(b);
    interfaceDeclarations(b);
    enums(b);
    formalParameters(b);
    blocksAndStatements(b);
    expressions(b);
    types(b);
    annotations(b);
    literals(b);

    b.setRootRule(COMPILATION_UNIT);

    return b;
  }

  private static void punctuators(LexerlessGrammarBuilder b) {
    punctuator(b, AT, "@");
    punctuator(b, AND, "&", b.nextNot(b.firstOf("=", "&")));
    punctuator(b, ANDAND, "&&");
    punctuator(b, ANDEQU, "&=");
    punctuator(b, BANG, "!", b.nextNot("="));
    punctuator(b, BSR, ">>>", b.nextNot("="));
    punctuator(b, BSREQU, ">>>=");
    punctuator(b, COLON, ":");
    punctuator(b, COMMA, ",");
    punctuator(b, DEC, "--");
    punctuator(b, DIV, "/", b.nextNot("="));
    punctuator(b, DIVEQU, "/=");
    punctuator(b, DOT, ".");
    punctuator(b, ELLIPSIS, "...");
    punctuator(b, EQU, "=", b.nextNot("="));
    punctuator(b, EQUAL, "==");
    punctuator(b, GE, ">=");
    punctuator(b, GT, ">", b.nextNot(b.firstOf("=", ">")));
    punctuator(b, HAT, "^", b.nextNot("="));
    punctuator(b, HATEQU, "^=");
    punctuator(b, INC, "++");
    punctuator(b, LBRK, "[");
    punctuator(b, LT, "<", b.nextNot(b.firstOf("=", "<")));
    punctuator(b, LE, "<=");
    punctuator(b, LPAR, "(");
    punctuator(b, LWING, "{");
    punctuator(b, MINUS, "-", b.nextNot(b.firstOf("-", "=")));
    punctuator(b, MINSEQU, "-=");
    punctuator(b, MOD, "%", b.nextNot("="));
    punctuator(b, MODEQU, "%=");
    punctuator(b, NOTEQUAL, "!=");
    punctuator(b, OR, "|", b.nextNot(b.firstOf("=", "|")));
    punctuator(b, OREQU, "|=");
    punctuator(b, OROR, "||");
    punctuator(b, PLUS, "+", b.nextNot(b.firstOf("=", "+")));
    punctuator(b, PLUSEQU, "+=");
    punctuator(b, QUERY, "?");
    punctuator(b, RBRK, "]");
    punctuator(b, RPAR, ")");
    punctuator(b, RWING, "}");
    punctuator(b, SEMI, ";");
    punctuator(b, SL, "<<", b.nextNot("="));
    punctuator(b, SLEQU, "<<=");
    punctuator(b, SR, ">>", b.nextNot(b.firstOf("=", ">")));
    punctuator(b, SREQU, ">>=");
    punctuator(b, STAR, "*", b.nextNot("="));
    punctuator(b, STAREQU, "*=");
    punctuator(b, TILDA, "~");

    punctuator(b, LPOINT, "<");
    punctuator(b, RPOINT, ">");
  }

  private static void keywords(LexerlessGrammarBuilder b) {
    keyword(b, ASSERT_KEYWORD, "assert");
    keyword(b, BREAK_KEYWORD, "break");
    keyword(b, CASE_KEYWORD, "case");
    keyword(b, CATCH_KEYWORD, "catch");
    keyword(b, CLASS_KEYWORD, "class");
    keyword(b, CONTINUE_KEYWORD, "continue");
    keyword(b, DEFAULT_KEYWORD, "default");
    keyword(b, DO_KEYWORD, "do");
    keyword(b, ELSE_KEYWORD, "else");
    keyword(b, ENUM_KEYWORD, "enum");
    keyword(b, EXTENDS_KEYWORD, "extends");
    keyword(b, FINALLY_KEYWORD, "finally");
    keyword(b, FINAL_KEYWORD, "final");
    keyword(b, FOR_KEYWORD, "for");
    keyword(b, IF_KEYWORD, "if");
    keyword(b, IMPLEMENTS_KEYWORD, "implements");
    keyword(b, IMPORT_KEYWORD, "import");
    keyword(b, INTERFACE_KEYWORD, "interface");
    keyword(b, INSTANCEOF_KEYWORD, "instanceof");
    keyword(b, NEW_KEYWORD, "new");
    keyword(b, PACKAGE_KEYWORD, "package");
    keyword(b, RETURN_KEYWORD, "return");
    keyword(b, STATIC_KEYWORD, "static");
    keyword(b, SUPER_KEYWORD, "super");
    keyword(b, SWITCH_KEYWORD, "switch");
    keyword(b, SYNCHRONIZED_KEYWORD, "synchronized");
    keyword(b, THIS_KEYWORD, "this");
    keyword(b, THROWS_KEYWORD, "throws");
    keyword(b, THROW_KEYWORD, "throw");
    keyword(b, TRY_KEYWORD, "try");
    keyword(b, VOID_KEYWORD, "void");
    keyword(b, WHILE_KEYWORD, "while");
    keyword(b, TRUE_KEYWORD, "true");
    keyword(b, FALSE_KEYWORD, "false");
    keyword(b, NULL_KEYWORD, "null");
    keyword(b, PUBLIC_KEYWORD, "public");
    keyword(b, PROTECTED_KEYWORD, "protected");
    keyword(b, PRIVATE_KEYWORD, "private");
    keyword(b, ABSTRACT_KEYWORD, "abstract");
    keyword(b, NATIVE_KEYWORD, "native");
    keyword(b, TRANSIENT_KEYWORD, "transient");
    keyword(b, VOLATILE_KEYWORD, "volatile");
    keyword(b, STRICTFP_KEYWORD, "strictfp");
    keyword(b, BYTE_KEYWORD, "byte");
    keyword(b, SHORT_KEYWORD, "short");
    keyword(b, CHAR_KEYWORD, "char");
    keyword(b, INT_KEYWORD, "int");
    keyword(b, LONG_KEYWORD, "long");
    keyword(b, FLOAT_KEYWORD, "float");
    keyword(b, DOUBLE_KEYWORD, "double");
    keyword(b, BOOLEAN_KEYWORD, "boolean");
  }

  private static void keyword(LexerlessGrammarBuilder b, GrammarRuleKey ruleKey, String value) {
    for (JavaKeyword tokenType : JavaKeyword.values()) {
      if (value.equals(tokenType.getValue())) {
        b.rule(ruleKey).is(b.sequence(b.token(tokenType, value), b.nextNot(LETTER_OR_DIGIT), SPACING)).skip();
        return;
      }
    }
    throw new IllegalStateException(value);
  }

  private static void punctuator(LexerlessGrammarBuilder b, GrammarRuleKey ruleKey, String value) {
    for (JavaPunctuator tokenType : JavaPunctuator.values()) {
      if (value.equals(tokenType.getValue())) {
        b.rule(ruleKey).is(b.sequence(b.token(tokenType, value), SPACING)).skip();
        return;
      }
    }
    b.rule(ruleKey).is(b.sequence(b.token(JavaTokenType.SPECIAL, value), SPACING)).skip();
  }

  private static void punctuator(LexerlessGrammarBuilder b, GrammarRuleKey ruleKey, String value, Object element) {
    for (JavaPunctuator tokenType : JavaPunctuator.values()) {
      if (value.equals(tokenType.getValue())) {
        b.rule(ruleKey).is(b.sequence(b.token(tokenType, value), element, SPACING)).skip();
        return;
      }
    }
    b.rule(ruleKey).is(b.sequence(b.token(JavaTokenType.SPECIAL, value), element, SPACING)).skip();
  }

  /**
   * 3.10. Literals
   */
  private static void literals(LexerlessGrammarBuilder b) {
    b.rule(SPACING).is(
        b.skippedTrivia(whitespace(b)),
        b.zeroOrMore(
            b.commentTrivia(b.firstOf(inlineComment(b), multilineComment(b))),
            b.skippedTrivia(whitespace(b)))).skip();

    b.rule(EOF).is(b.token(GenericTokenType.EOF, b.endOfInput())).skip();

    b.rule(CHARACTER_LITERAL).is(b.token(JavaTokenType.CHARACTER_LITERAL, characterLiteral(b)), SPACING).skip();
    b.rule(STRING_LITERAL).is(b.token(GenericTokenType.LITERAL, stringLiteral(b)), SPACING).skip();

    b.rule(FLOAT_LITERAL).is(b.token(JavaTokenType.FLOAT_LITERAL, b.regexp(FloatLiteralChannel.FLOATING_LITERAL_WITHOUT_SUFFIX + "[fF]|[0-9][0-9_]*+[fF]")), SPACING).skip();
    b.rule(DOUBLE_LITERAL).is(b.token(JavaTokenType.DOUBLE_LITERAL, b.regexp(FloatLiteralChannel.FLOATING_LITERAL_WITHOUT_SUFFIX + "[dD]?+|[0-9][0-9_]*+[dD]")), SPACING).skip();

    b.rule(LONG_LITERAL).is(b.token(JavaTokenType.LONG_LITERAL, b.regexp(IntegerLiteralChannel.INTEGER_LITERAL + "[lL]")), SPACING).skip();
    b.rule(INTEGER_LITERAL).is(b.token(JavaTokenType.INTEGER_LITERAL, b.regexp(IntegerLiteralChannel.INTEGER_LITERAL)), SPACING).skip();

    b.rule(KEYWORD).is(b.firstOf("assert", "break", "case", "catch", "class", "const", "continue", "default", "do", "else",
        "enum", "extends", "finally", "final", "for", "goto", "if", "implements", "import", "interface",
        "instanceof", "new", "package", "return", "static", "super", "switch", "synchronized", "this",
        "throws", "throw", "try", "void", "while"), b.nextNot(LETTER_OR_DIGIT));
    b.rule(LETTER_OR_DIGIT).is(javaIdentifierPart(b));
    b.rule(IDENTIFIER).is(b.nextNot(KEYWORD), b.token(GenericTokenType.IDENTIFIER, javaIdentifier(b)), SPACING).skip();

    b.rule(LITERAL).is(b.firstOf(
        TRUE_KEYWORD,
        FALSE_KEYWORD,
        NULL_KEYWORD,
        CHARACTER_LITERAL,
        STRING_LITERAL,
        FLOAT_LITERAL,
        DOUBLE_LITERAL,
        LONG_LITERAL,
        INTEGER_LITERAL));
  }

  private static Object characterLiteral(LexerlessGrammarBuilder b) {
    return b.sequence(b.next("'"), b.regexp("'([^'\\\\]*+(\\\\[\\s\\S])?+)*+'"));
  }

  private static Object stringLiteral(LexerlessGrammarBuilder b) {
    return b.sequence(b.next("\""), b.regexp("\"([^\"\\\\]*+(\\\\[\\s\\S])?+)*+\""));
  }

  private static Object whitespace(LexerlessGrammarBuilder b) {
    return b.regexp("\\s*+");
  }

  private static Object inlineComment(LexerlessGrammarBuilder b) {
    return b.regexp("//[^\\n\\r]*+");
  }

  private static Object multilineComment(LexerlessGrammarBuilder b) {
    return b.regexp("/\\*[\\s\\S]*?\\*\\/");
  }

  private static Object javaIdentifier(LexerlessGrammarBuilder b) {
    return b.regexp("\\p{javaJavaIdentifierStart}++\\p{javaJavaIdentifierPart}*+");
  }

  private static Object javaIdentifierPart(LexerlessGrammarBuilder b) {
    return b.regexp("\\p{javaJavaIdentifierPart}");
  }

  /**
   * 4. Types, Values and Variables
   */
  private static void types(LexerlessGrammarBuilder b) {
    b.rule(TYPE).is(b.firstOf(BASIC_TYPE, CLASS_TYPE), b.zeroOrMore(DIM));
    b.rule(REFERENCE_TYPE).is(b.firstOf(
        b.sequence(BASIC_TYPE, b.zeroOrMore(DIM)),
        b.sequence(CLASS_TYPE, b.zeroOrMore(DIM))));
    b.rule(CLASS_TYPE).is(IDENTIFIER, b.optional(TYPE_ARGUMENTS), b.zeroOrMore(DOT, IDENTIFIER, b.optional(TYPE_ARGUMENTS)));
    b.rule(CLASS_TYPE_LIST).is(CLASS_TYPE, b.zeroOrMore(COMMA, CLASS_TYPE));
    b.rule(TYPE_ARGUMENTS).is(LPOINT, TYPE_ARGUMENT, b.zeroOrMore(COMMA, TYPE_ARGUMENT), RPOINT);
    b.rule(TYPE_ARGUMENT).is(b.firstOf(
        REFERENCE_TYPE,
        b.sequence(QUERY, b.optional(b.firstOf(EXTENDS_KEYWORD, SUPER_KEYWORD), REFERENCE_TYPE))));
    b.rule(TYPE_PARAMETERS).is(LPOINT, TYPE_PARAMETER, b.zeroOrMore(COMMA, TYPE_PARAMETER), RPOINT);
    b.rule(TYPE_PARAMETER).is(IDENTIFIER, b.optional(EXTENDS_KEYWORD, BOUND));
    b.rule(BOUND).is(CLASS_TYPE, b.zeroOrMore(AND, CLASS_TYPE));
    b.rule(MODIFIER).is(b.firstOf(
        ANNOTATION,
        PUBLIC_KEYWORD,
        PROTECTED_KEYWORD,
        PRIVATE_KEYWORD,
        STATIC_KEYWORD,
        ABSTRACT_KEYWORD,
        FINAL_KEYWORD,
        NATIVE_KEYWORD,
        SYNCHRONIZED_KEYWORD,
        TRANSIENT_KEYWORD,
        VOLATILE_KEYWORD,
        STRICTFP_KEYWORD));
  }

  /**
   * 7.3. Compilation Units
   */
  private static void compilationsUnits(LexerlessGrammarBuilder b) {
    b.rule(COMPILATION_UNIT).is(SPACING, b.optional(PACKAGE_DECLARATION), b.zeroOrMore(IMPORT_DECLARATION), b.zeroOrMore(TYPE_DECLARATION), EOF);

    b.rule(PACKAGE_DECLARATION).is(b.zeroOrMore(ANNOTATION), PACKAGE_KEYWORD, QUALIFIED_IDENTIFIER, SEMI);
    b.rule(IMPORT_DECLARATION).is(IMPORT_KEYWORD, b.optional(STATIC_KEYWORD), QUALIFIED_IDENTIFIER, b.optional(DOT, STAR), SEMI);
    b.rule(TYPE_DECLARATION).is(b.firstOf(
        b.sequence(b.zeroOrMore(MODIFIER), b.firstOf(CLASS_DECLARATION, ENUM_DECLARATION, INTERFACE_DECLARATION, ANNOTATION_TYPE_DECLARATION)),
        SEMI));
  }

  /**
   * 8.1. Class Declaration
   */
  private static void classDeclaration(LexerlessGrammarBuilder b) {
    b.rule(CLASS_DECLARATION).is(CLASS_KEYWORD, IDENTIFIER, b.optional(TYPE_PARAMETERS), b.optional(EXTENDS_KEYWORD, CLASS_TYPE), b.optional(IMPLEMENTS_KEYWORD, CLASS_TYPE_LIST),
        CLASS_BODY);

    b.rule(CLASS_BODY).is(LWING, b.zeroOrMore(CLASS_BODY_DECLARATION), RWING);
    b.rule(CLASS_BODY_DECLARATION).is(b.firstOf(
        SEMI,
        CLASS_INIT_DECLARATION,
        b.sequence(b.zeroOrMore(MODIFIER), MEMBER_DECL)));
    b.rule(CLASS_INIT_DECLARATION).is(b.optional(STATIC_KEYWORD), BLOCK);
    b.rule(MEMBER_DECL).is(b.firstOf(
        b.sequence(TYPE_PARAMETERS, GENERIC_METHOD_OR_CONSTRUCTOR_REST),
        b.sequence(TYPE, IDENTIFIER, METHOD_DECLARATOR_REST),
        FIELD_DECLARATION,
        b.sequence(VOID_KEYWORD, IDENTIFIER, VOID_METHOD_DECLARATOR_REST),
        b.sequence(IDENTIFIER, CONSTRUCTOR_DECLARATOR_REST),
        INTERFACE_DECLARATION,
        CLASS_DECLARATION,
        ENUM_DECLARATION,
        ANNOTATION_TYPE_DECLARATION));
    b.rule(FIELD_DECLARATION).is(TYPE, VARIABLE_DECLARATORS, SEMI);
    b.rule(GENERIC_METHOD_OR_CONSTRUCTOR_REST).is(b.firstOf(
        b.sequence(b.firstOf(TYPE, VOID_KEYWORD), IDENTIFIER, METHOD_DECLARATOR_REST),
        b.sequence(IDENTIFIER, CONSTRUCTOR_DECLARATOR_REST)));
    b.rule(METHOD_DECLARATOR_REST).is(FORMAL_PARAMETERS, b.zeroOrMore(DIM), b.optional(THROWS_KEYWORD, CLASS_TYPE_LIST), b.firstOf(METHOD_BODY, SEMI));
    b.rule(VOID_METHOD_DECLARATOR_REST).is(FORMAL_PARAMETERS, b.optional(THROWS_KEYWORD, CLASS_TYPE_LIST), b.firstOf(METHOD_BODY, SEMI));
    b.rule(CONSTRUCTOR_DECLARATOR_REST).is(FORMAL_PARAMETERS, b.optional(THROWS_KEYWORD, CLASS_TYPE_LIST), METHOD_BODY);
    b.rule(METHOD_BODY).is(BLOCK);
  }

  /**
   * 8.9. Enums
   */
  private static void enums(LexerlessGrammarBuilder b) {
    b.rule(ENUM_DECLARATION).is(ENUM_KEYWORD, IDENTIFIER, b.optional(IMPLEMENTS_KEYWORD, CLASS_TYPE_LIST), ENUM_BODY);
    b.rule(ENUM_BODY).is(LWING, b.optional(ENUM_CONSTANTS), b.optional(COMMA), b.optional(ENUM_BODY_DECLARATIONS), RWING);
    b.rule(ENUM_CONSTANTS).is(ENUM_CONSTANT, b.zeroOrMore(COMMA, ENUM_CONSTANT));
    b.rule(ENUM_CONSTANT).is(b.zeroOrMore(ANNOTATION), IDENTIFIER, b.optional(ARGUMENTS), b.optional(CLASS_BODY));
    b.rule(ENUM_BODY_DECLARATIONS).is(SEMI, b.zeroOrMore(CLASS_BODY_DECLARATION));
  }

  /**
   * 9.1. Interface Declarations
   */
  private static void interfaceDeclarations(LexerlessGrammarBuilder b) {
    b.rule(INTERFACE_DECLARATION).is(INTERFACE_KEYWORD, IDENTIFIER, b.optional(TYPE_PARAMETERS), b.optional(EXTENDS_KEYWORD, CLASS_TYPE_LIST), INTERFACE_BODY);

    b.rule(INTERFACE_BODY).is(LWING, b.zeroOrMore(INTERFACE_BODY_DECLARATION), RWING);
    b.rule(INTERFACE_BODY_DECLARATION).is(b.firstOf(
        b.sequence(b.zeroOrMore(MODIFIER), INTERFACE_MEMBER_DECL),
        SEMI));
    b.rule(INTERFACE_MEMBER_DECL).is(b.firstOf(
        INTERFACE_METHOD_OR_FIELD_DECL,
        INTERFACE_GENERIC_METHOD_DECL,
        b.sequence(VOID_KEYWORD, IDENTIFIER, VOID_INTERFACE_METHOD_DECLARATORS_REST),
        INTERFACE_DECLARATION,
        ANNOTATION_TYPE_DECLARATION,
        CLASS_DECLARATION,
        ENUM_DECLARATION));
    b.rule(INTERFACE_METHOD_OR_FIELD_DECL).is(TYPE, IDENTIFIER, INTERFACE_METHOD_OR_FIELD_REST);
    b.rule(INTERFACE_METHOD_OR_FIELD_REST).is(b.firstOf(
        b.sequence(CONSTANT_DECLARATORS_REST, SEMI),
        INTERFACE_METHOD_DECLARATOR_REST));
    b.rule(INTERFACE_METHOD_DECLARATOR_REST).is(FORMAL_PARAMETERS, b.zeroOrMore(DIM), b.optional(THROWS_KEYWORD, CLASS_TYPE_LIST), SEMI);
    b.rule(INTERFACE_GENERIC_METHOD_DECL).is(TYPE_PARAMETERS, b.firstOf(TYPE, VOID_KEYWORD), IDENTIFIER, INTERFACE_METHOD_DECLARATOR_REST);
    b.rule(VOID_INTERFACE_METHOD_DECLARATORS_REST).is(FORMAL_PARAMETERS, b.optional(THROWS_KEYWORD, CLASS_TYPE_LIST), SEMI);
    b.rule(CONSTANT_DECLARATORS_REST).is(CONSTANT_DECLARATOR_REST, b.zeroOrMore(COMMA, CONSTANT_DECLARATOR));
    b.rule(CONSTANT_DECLARATOR).is(IDENTIFIER, CONSTANT_DECLARATOR_REST);
    b.rule(CONSTANT_DECLARATOR_REST).is(b.zeroOrMore(DIM), EQU, VARIABLE_INITIALIZER);
  }

  /**
   * 8.4.1. Formal Parameters
   */
  private static void formalParameters(LexerlessGrammarBuilder b) {
    b.rule(FORMAL_PARAMETERS).is(LPAR, b.optional(FORMAL_PARAMETER_DECLS), RPAR);
    b.rule(FORMAL_PARAMETER).is(b.zeroOrMore(b.firstOf(FINAL_KEYWORD, ANNOTATION)), TYPE, VARIABLE_DECLARATOR_ID);
    b.rule(FORMAL_PARAMETER_DECLS).is(b.zeroOrMore(b.firstOf(FINAL_KEYWORD, ANNOTATION)), TYPE, FORMAL_PARAMETERS_DECLS_REST);
    b.rule(FORMAL_PARAMETERS_DECLS_REST).is(b.firstOf(
        b.sequence(VARIABLE_DECLARATOR_ID, b.optional(COMMA, FORMAL_PARAMETER_DECLS)),
        b.sequence(ELLIPSIS, VARIABLE_DECLARATOR_ID)));
    b.rule(VARIABLE_DECLARATOR_ID).is(IDENTIFIER, b.zeroOrMore(DIM));
  }

  /**
   * 9.7. Annotations
   */
  private static void annotations(LexerlessGrammarBuilder b) {
    b.rule(ANNOTATION_TYPE_DECLARATION).is(AT, INTERFACE_KEYWORD, IDENTIFIER, ANNOTATION_TYPE_BODY);
    b.rule(ANNOTATION_TYPE_BODY).is(LWING, b.zeroOrMore(ANNOTATION_TYPE_ELEMENT_DECLARATION), RWING);
    b.rule(ANNOTATION_TYPE_ELEMENT_DECLARATION).is(b.firstOf(
        b.sequence(b.zeroOrMore(MODIFIER), ANNOTATION_TYPE_ELEMENT_REST),
        SEMI));
    b.rule(ANNOTATION_TYPE_ELEMENT_REST).is(b.firstOf(
        b.sequence(TYPE, ANNOTATION_METHOD_OR_CONSTANT_REST, SEMI),
        CLASS_DECLARATION,
        ENUM_DECLARATION,
        INTERFACE_DECLARATION,
        ANNOTATION_TYPE_DECLARATION));
    b.rule(ANNOTATION_METHOD_OR_CONSTANT_REST).is(b.firstOf(
        ANNOTATION_METHOD_REST,
        ANNOTATION_CONSTANT_REST));
    b.rule(ANNOTATION_METHOD_REST).is(IDENTIFIER, LPAR, RPAR, b.optional(DEFAULT_VALUE));
    b.rule(ANNOTATION_CONSTANT_REST).is(VARIABLE_DECLARATORS);
    b.rule(DEFAULT_VALUE).is(DEFAULT_KEYWORD, ELEMENT_VALUE);
    b.rule(ANNOTATION).is(AT, QUALIFIED_IDENTIFIER, b.optional(ANNOTATION_REST));
    b.rule(ANNOTATION_REST).is(b.firstOf(
        NORMAL_ANNOTATION_REST,
        SINGLE_ELEMENT_ANNOTATION_REST));
    b.rule(NORMAL_ANNOTATION_REST).is(LPAR, b.optional(ELEMENT_VALUE_PAIRS), RPAR);
    b.rule(ELEMENT_VALUE_PAIRS).is(ELEMENT_VALUE_PAIR, b.zeroOrMore(COMMA, ELEMENT_VALUE_PAIR));
    b.rule(ELEMENT_VALUE_PAIR).is(IDENTIFIER, EQU, ELEMENT_VALUE);
    b.rule(ELEMENT_VALUE).is(b.firstOf(
        CONDITIONAL_EXPRESSION,
        ANNOTATION,
        ELEMENT_VALUE_ARRAY_INITIALIZER));
    b.rule(ELEMENT_VALUE_ARRAY_INITIALIZER).is(LWING, b.optional(ELEMENT_VALUES), b.optional(COMMA), RWING);
    b.rule(ELEMENT_VALUES).is(ELEMENT_VALUE, b.zeroOrMore(COMMA, ELEMENT_VALUE));
    b.rule(SINGLE_ELEMENT_ANNOTATION_REST).is(LPAR, ELEMENT_VALUE, RPAR);
  }

  /**
   * 14. Blocks and Statements
   */
  private static void blocksAndStatements(LexerlessGrammarBuilder b) {
    // 14.2. Blocks
    b.rule(BLOCK).is(LWING, BLOCK_STATEMENTS, RWING);
    b.rule(BLOCK_STATEMENTS).is(b.zeroOrMore(BLOCK_STATEMENT));
    b.rule(BLOCK_STATEMENT).is(b.firstOf(
        LOCAL_VARIABLE_DECLARATION_STATEMENT,
        b.sequence(b.zeroOrMore(MODIFIER), b.firstOf(CLASS_DECLARATION, ENUM_DECLARATION)),
        STATEMENT));

    // 14.4. Local Variable Declaration Statements
    b.rule(LOCAL_VARIABLE_DECLARATION_STATEMENT).is(VARIABLE_MODIFIERS, TYPE, VARIABLE_DECLARATORS, SEMI);
    b.rule(VARIABLE_MODIFIERS).is(b.zeroOrMore(b.firstOf(
        ANNOTATION,
        FINAL_KEYWORD)));
    b.rule(VARIABLE_DECLARATORS).is(VARIABLE_DECLARATOR, b.zeroOrMore(COMMA, VARIABLE_DECLARATOR));
    b.rule(VARIABLE_DECLARATOR).is(IDENTIFIER, b.zeroOrMore(DIM), b.optional(EQU, VARIABLE_INITIALIZER));

    // 14.5. Statements
    b.rule(STATEMENT).is(b.firstOf(
        BLOCK,
        ASSERT_STATEMENT,
        IF_STATEMENT,
        FOR_STATEMENT,
        WHILE_STATEMENT,
        DO_STATEMENT,
        TRY_STATEMENT,
        SWITCH_STATEMENT,
        SYNCHRONIZED_STATEMENT,
        RETURN_STATEMENT,
        THROW_STATEMENT,
        BREAK_STATEMENT,
        CONTINUE_STATEMENT,
        LABELED_STATEMENT,
        EXPRESSION_STATEMENT,
        EMPTY_STATEMENT));

    // 14.6. The Empty Statement
    b.rule(EMPTY_STATEMENT).is(SEMI);
    // 14.7. Labeled Statements
    b.rule(LABELED_STATEMENT).is(IDENTIFIER, COLON, STATEMENT);
    // 14.8. Expression Statements
    b.rule(EXPRESSION_STATEMENT).is(STATEMENT_EXPRESSION, SEMI);
    // 14.9. The if Statement
    b.rule(IF_STATEMENT).is(IF_KEYWORD, PAR_EXPRESSION, STATEMENT, b.optional(ELSE_KEYWORD, STATEMENT));
    // 14.10. The assert Statement
    b.rule(ASSERT_STATEMENT).is(ASSERT_KEYWORD, EXPRESSION, b.optional(COLON, EXPRESSION), SEMI);

    // 14.11. The switch statement
    b.rule(SWITCH_STATEMENT).is(SWITCH_KEYWORD, PAR_EXPRESSION, LWING, SWITCH_BLOCK_STATEMENT_GROUPS, RWING);
    b.rule(SWITCH_BLOCK_STATEMENT_GROUPS).is(b.zeroOrMore(SWITCH_BLOCK_STATEMENT_GROUP));
    b.rule(SWITCH_BLOCK_STATEMENT_GROUP).is(SWITCH_LABEL, BLOCK_STATEMENTS);
    b.rule(SWITCH_LABEL).is(b.firstOf(
        b.sequence(CASE_KEYWORD, CONSTANT_EXPRESSION, COLON),
        b.sequence(CASE_KEYWORD, ENUM_CONSTANT_NAME, COLON),
        b.sequence(DEFAULT_KEYWORD, COLON)));
    b.rule(ENUM_CONSTANT_NAME).is(IDENTIFIER);

    // 14.12. The while Statement
    b.rule(WHILE_STATEMENT).is(WHILE_KEYWORD, PAR_EXPRESSION, STATEMENT);
    // 14.13. The do Statement
    b.rule(DO_STATEMENT).is(DO_KEYWORD, STATEMENT, WHILE_KEYWORD, PAR_EXPRESSION, SEMI);

    // 14.14. The for Statement
    b.rule(FOR_STATEMENT).is(b.firstOf(
        b.sequence(FOR_KEYWORD, LPAR, b.optional(FOR_INIT), SEMI, b.optional(EXPRESSION), SEMI, b.optional(FOR_UPDATE), RPAR, STATEMENT),
        b.sequence(FOR_KEYWORD, LPAR, FORMAL_PARAMETER, COLON, EXPRESSION, RPAR, STATEMENT)));
    b.rule(FOR_INIT).is(b.firstOf(
        b.sequence(b.zeroOrMore(b.firstOf(FINAL_KEYWORD, ANNOTATION)), TYPE, VARIABLE_DECLARATORS),
        b.sequence(STATEMENT_EXPRESSION, b.zeroOrMore(COMMA, STATEMENT_EXPRESSION))));
    b.rule(FOR_UPDATE).is(STATEMENT_EXPRESSION, b.zeroOrMore(COMMA, STATEMENT_EXPRESSION));

    // 14.15. The break Statement
    b.rule(BREAK_STATEMENT).is(BREAK_KEYWORD, b.optional(IDENTIFIER), SEMI);
    // 14.16. The continue Statement
    b.rule(CONTINUE_STATEMENT).is(CONTINUE_KEYWORD, b.optional(IDENTIFIER), SEMI);
    // 14.17. The return Statement
    b.rule(RETURN_STATEMENT).is(RETURN_KEYWORD, b.optional(EXPRESSION), SEMI);
    // 14.18. The throw Statement
    b.rule(THROW_STATEMENT).is(THROW_KEYWORD, EXPRESSION, SEMI);
    // 14.19. The synchronized Statement
    b.rule(SYNCHRONIZED_STATEMENT).is(SYNCHRONIZED_KEYWORD, PAR_EXPRESSION, BLOCK);

    // 14.20. The try Statement
    b.rule(TRY_STATEMENT).is(b.firstOf(
        b.sequence(TRY_KEYWORD, BLOCK, b.firstOf(b.sequence(b.oneOrMore(CATCH_CLAUSE), b.optional(FINALLY_)), FINALLY_)),
        TRY_WITH_RESOURCES_STATEMENT));
    b.rule(TRY_WITH_RESOURCES_STATEMENT).is(TRY_KEYWORD, RESOURCE_SPECIFICATION, BLOCK, b.zeroOrMore(CATCH_CLAUSE), b.optional(FINALLY_));
    b.rule(RESOURCE_SPECIFICATION).is(LPAR, RESOURCE, b.zeroOrMore(SEMI, RESOURCE), b.optional(SEMI), RPAR);
    b.rule(RESOURCE).is(b.optional(VARIABLE_MODIFIERS), TYPE, VARIABLE_DECLARATOR_ID, EQU, EXPRESSION);

    b.rule(CATCH_CLAUSE).is(CATCH_KEYWORD, LPAR, CATCH_FORMAL_PARAMETER, RPAR, BLOCK);
    b.rule(CATCH_FORMAL_PARAMETER).is(b.optional(VARIABLE_MODIFIERS), CATCH_TYPE, VARIABLE_DECLARATOR_ID);
    b.rule(CATCH_TYPE).is(CLASS_TYPE, b.zeroOrMore(OR, CLASS_TYPE));

    b.rule(FINALLY_).is(FINALLY_KEYWORD, BLOCK);
  }

  /**
   * 15. Expressions
   */
  private static void expressions(LexerlessGrammarBuilder b) {
    b.rule(STATEMENT_EXPRESSION).is(EXPRESSION);
    b.rule(CONSTANT_EXPRESSION).is(EXPRESSION);
    b.rule(EXPRESSION).is(ASSIGNMENT_EXPRESSION);
    b.rule(ASSIGNMENT_EXPRESSION).is(CONDITIONAL_EXPRESSION, b.zeroOrMore(ASSIGNMENT_OPERATOR, CONDITIONAL_EXPRESSION)).skipIfOneChild();
    b.rule(ASSIGNMENT_OPERATOR).is(b.firstOf(
        EQU,
        PLUSEQU,
        MINSEQU,
        STAREQU,
        DIVEQU,
        ANDEQU,
        OREQU,
        HATEQU,
        MODEQU,
        SLEQU,
        SREQU,
        BSREQU));
    b.rule(CONDITIONAL_EXPRESSION).is(CONDITIONAL_OR_EXPRESSION, b.zeroOrMore(QUERY, EXPRESSION, COLON, CONDITIONAL_OR_EXPRESSION)).skipIfOneChild();
    b.rule(CONDITIONAL_OR_EXPRESSION).is(CONDITIONAL_AND_EXPRESSION, b.zeroOrMore(OROR, CONDITIONAL_AND_EXPRESSION)).skipIfOneChild();
    b.rule(CONDITIONAL_AND_EXPRESSION).is(INCLUSIVE_OR_EXPRESSION, b.zeroOrMore(ANDAND, INCLUSIVE_OR_EXPRESSION)).skipIfOneChild();
    b.rule(INCLUSIVE_OR_EXPRESSION).is(EXCLUSIVE_OR_EXPRESSION, b.zeroOrMore(OR, EXCLUSIVE_OR_EXPRESSION)).skipIfOneChild();
    b.rule(EXCLUSIVE_OR_EXPRESSION).is(AND_EXPRESSION, b.zeroOrMore(HAT, AND_EXPRESSION)).skipIfOneChild();
    b.rule(AND_EXPRESSION).is(EQUALITY_EXPRESSION, b.zeroOrMore(AND, EQUALITY_EXPRESSION)).skipIfOneChild();
    b.rule(EQUALITY_EXPRESSION).is(RELATIONAL_EXPRESSION, b.zeroOrMore(b.firstOf(EQUAL, NOTEQUAL), RELATIONAL_EXPRESSION)).skipIfOneChild();
    b.rule(RELATIONAL_EXPRESSION).is(SHIFT_EXPRESSION, b.zeroOrMore(b.firstOf(
        b.sequence(b.firstOf(GE, GT, LE, LT), SHIFT_EXPRESSION),
        b.sequence(INSTANCEOF_KEYWORD, REFERENCE_TYPE)))).skipIfOneChild();
    b.rule(SHIFT_EXPRESSION).is(ADDITIVE_EXPRESSION, b.zeroOrMore(b.firstOf(SL, BSR, SR), ADDITIVE_EXPRESSION)).skipIfOneChild();
    b.rule(ADDITIVE_EXPRESSION).is(MULTIPLICATIVE_EXPRESSION, b.zeroOrMore(b.firstOf(PLUS, MINUS), MULTIPLICATIVE_EXPRESSION)).skipIfOneChild();
    b.rule(MULTIPLICATIVE_EXPRESSION).is(UNARY_EXPRESSION, b.zeroOrMore(b.firstOf(STAR, DIV, MOD), UNARY_EXPRESSION)).skipIfOneChild();
    b.rule(UNARY_EXPRESSION).is(b.firstOf(
        b.sequence(PREFIX_OP, UNARY_EXPRESSION),
        b.sequence(LPAR, TYPE, RPAR, UNARY_EXPRESSION),
        b.sequence(PRIMARY, b.zeroOrMore(SELECTOR), b.zeroOrMore(POST_FIX_OP)))).skipIfOneChild();
    b.rule(PRIMARY).is(b.firstOf(
        PAR_EXPRESSION,
        b.sequence(NON_WILDCARD_TYPE_ARGUMENTS, b.firstOf(EXPLICIT_GENERIC_INVOCATION_SUFFIX, b.sequence(THIS_KEYWORD, ARGUMENTS))),
        b.sequence(THIS_KEYWORD, b.optional(ARGUMENTS)),
        b.sequence(SUPER_KEYWORD, SUPER_SUFFIX),
        LITERAL,
        b.sequence(NEW_KEYWORD, CREATOR),
        b.sequence(QUALIFIED_IDENTIFIER, b.optional(IDENTIFIER_SUFFIX)),
        b.sequence(BASIC_TYPE, b.zeroOrMore(DIM), DOT, CLASS_KEYWORD),
        b.sequence(VOID_KEYWORD, DOT, CLASS_KEYWORD)));
    b.rule(IDENTIFIER_SUFFIX).is(b.firstOf(
        b.sequence(LBRK, b.firstOf(b.sequence(RBRK, b.zeroOrMore(DIM), DOT, CLASS_KEYWORD), b.sequence(EXPRESSION, RBRK))),
        ARGUMENTS,
        b.sequence(DOT, b.firstOf(
            CLASS_KEYWORD,
            EXPLICIT_GENERIC_INVOCATION,
            THIS_KEYWORD,
            b.sequence(SUPER_KEYWORD, ARGUMENTS),
            b.sequence(NEW_KEYWORD, b.optional(NON_WILDCARD_TYPE_ARGUMENTS), INNER_CREATOR)))));
    b.rule(EXPLICIT_GENERIC_INVOCATION).is(NON_WILDCARD_TYPE_ARGUMENTS, EXPLICIT_GENERIC_INVOCATION_SUFFIX);
    b.rule(NON_WILDCARD_TYPE_ARGUMENTS).is(LPOINT, REFERENCE_TYPE, b.zeroOrMore(COMMA, REFERENCE_TYPE), RPOINT);
    b.rule(EXPLICIT_GENERIC_INVOCATION_SUFFIX).is(b.firstOf(
        b.sequence(SUPER_KEYWORD, SUPER_SUFFIX),
        b.sequence(IDENTIFIER, ARGUMENTS)));
    b.rule(PREFIX_OP).is(b.firstOf(
        INC,
        DEC,
        BANG,
        TILDA,
        PLUS,
        MINUS));
    b.rule(POST_FIX_OP).is(b.firstOf(
        INC,
        DEC));
    b.rule(SELECTOR).is(b.firstOf(
        b.sequence(DOT, IDENTIFIER, b.optional(ARGUMENTS)),
        b.sequence(DOT, EXPLICIT_GENERIC_INVOCATION),
        b.sequence(DOT, THIS_KEYWORD),
        b.sequence(DOT, SUPER_KEYWORD, SUPER_SUFFIX),
        b.sequence(DOT, NEW_KEYWORD, b.optional(NON_WILDCARD_TYPE_ARGUMENTS), INNER_CREATOR),
        DIM_EXPR));
    b.rule(SUPER_SUFFIX).is(b.firstOf(
        ARGUMENTS,
        b.sequence(DOT, IDENTIFIER, b.optional(ARGUMENTS))));
    b.rule(BASIC_TYPE).is(b.firstOf(
        BYTE_KEYWORD,
        SHORT_KEYWORD,
        CHAR_KEYWORD,
        INT_KEYWORD,
        LONG_KEYWORD,
        FLOAT_KEYWORD,
        DOUBLE_KEYWORD,
        BOOLEAN_KEYWORD));
    b.rule(ARGUMENTS).is(LPAR, b.optional(EXPRESSION, b.zeroOrMore(COMMA, EXPRESSION)), RPAR);
    b.rule(CREATOR).is(b.firstOf(
        b.sequence(b.optional(NON_WILDCARD_TYPE_ARGUMENTS), CREATED_NAME, CLASS_CREATOR_REST),
        b.sequence(b.optional(NON_WILDCARD_TYPE_ARGUMENTS), b.firstOf(CLASS_TYPE, BASIC_TYPE), ARRAY_CREATOR_REST)));
    b.rule(CREATED_NAME).is(IDENTIFIER, b.optional(NON_WILDCARD_TYPE_ARGUMENTS), b.zeroOrMore(DOT, IDENTIFIER, b.optional(NON_WILDCARD_TYPE_ARGUMENTS)));
    b.rule(INNER_CREATOR).is(IDENTIFIER, CLASS_CREATOR_REST);
    b.rule(ARRAY_CREATOR_REST).is(LBRK, b.firstOf(
        b.sequence(RBRK, b.zeroOrMore(DIM), ARRAY_INITIALIZER),
        b.sequence(EXPRESSION, RBRK, b.zeroOrMore(DIM_EXPR), b.zeroOrMore(DIM))));
    b.rule(CLASS_CREATOR_REST).is(b.optional(DIAMOND), ARGUMENTS, b.optional(CLASS_BODY));
    b.rule(DIAMOND).is(LT, GT);
    b.rule(ARRAY_INITIALIZER).is(LWING, b.optional(VARIABLE_INITIALIZER, b.zeroOrMore(COMMA, VARIABLE_INITIALIZER)), b.optional(COMMA), RWING);
    b.rule(VARIABLE_INITIALIZER).is(b.firstOf(ARRAY_INITIALIZER, EXPRESSION));
    b.rule(PAR_EXPRESSION).is(LPAR, EXPRESSION, RPAR);
    b.rule(QUALIFIED_IDENTIFIER).is(IDENTIFIER, b.zeroOrMore(DOT, IDENTIFIER));
    b.rule(DIM).is(LBRK, RBRK);
    b.rule(DIM_EXPR).is(LBRK, EXPRESSION, RBRK);
  }

  private final String internalName;

  private JavaGrammar() {
    String name = name();
    StringBuilder sb = new StringBuilder();
    int i = 0;
    while (i < name.length()) {
      if (name.charAt(i) == '_' && i + 1 < name.length()) {
        i++;
        sb.append(name.charAt(i));
      } else {
        sb.append(Character.toLowerCase(name.charAt(i)));
      }
      i++;
    }
    this.internalName = sb.toString();
  }

  @Override
  public String toString() {
    // This allows to keep compatibility with old XPath expressions
    return internalName;
  }

}
