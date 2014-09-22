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

import com.sonar.sslr.api.GenericTokenType;
import org.apache.commons.lang.ArrayUtils;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerlessGrammarBuilder;

import java.util.Arrays;

import static org.sonar.java.ast.api.JavaKeyword.CLASS;
import static org.sonar.java.ast.api.JavaKeyword.ENUM;
import static org.sonar.java.ast.api.JavaKeyword.EXTENDS;
import static org.sonar.java.ast.api.JavaKeyword.IMPLEMENTS;
import static org.sonar.java.ast.api.JavaKeyword.IMPORT;
import static org.sonar.java.ast.api.JavaKeyword.INTERFACE;
import static org.sonar.java.ast.api.JavaKeyword.NEW;
import static org.sonar.java.ast.api.JavaKeyword.PACKAGE;
import static org.sonar.java.ast.api.JavaKeyword.STATIC;
import static org.sonar.java.ast.api.JavaKeyword.THROWS;
import static org.sonar.java.ast.api.JavaKeyword.VOID;
import static org.sonar.java.ast.api.JavaPunctuator.AND;
import static org.sonar.java.ast.api.JavaPunctuator.ANDAND;
import static org.sonar.java.ast.api.JavaPunctuator.ANDEQU;
import static org.sonar.java.ast.api.JavaPunctuator.AT;
import static org.sonar.java.ast.api.JavaPunctuator.BANG;
import static org.sonar.java.ast.api.JavaPunctuator.BSR;
import static org.sonar.java.ast.api.JavaPunctuator.BSREQU;
import static org.sonar.java.ast.api.JavaPunctuator.COLON;
import static org.sonar.java.ast.api.JavaPunctuator.COMMA;
import static org.sonar.java.ast.api.JavaPunctuator.DBLECOLON;
import static org.sonar.java.ast.api.JavaPunctuator.DEC;
import static org.sonar.java.ast.api.JavaPunctuator.DIV;
import static org.sonar.java.ast.api.JavaPunctuator.DIVEQU;
import static org.sonar.java.ast.api.JavaPunctuator.DOT;
import static org.sonar.java.ast.api.JavaPunctuator.ELLIPSIS;
import static org.sonar.java.ast.api.JavaPunctuator.EQU;
import static org.sonar.java.ast.api.JavaPunctuator.EQUAL;
import static org.sonar.java.ast.api.JavaPunctuator.GE;
import static org.sonar.java.ast.api.JavaPunctuator.GT;
import static org.sonar.java.ast.api.JavaPunctuator.HAT;
import static org.sonar.java.ast.api.JavaPunctuator.HATEQU;
import static org.sonar.java.ast.api.JavaPunctuator.INC;
import static org.sonar.java.ast.api.JavaPunctuator.LBRK;
import static org.sonar.java.ast.api.JavaPunctuator.LE;
import static org.sonar.java.ast.api.JavaPunctuator.LPAR;
import static org.sonar.java.ast.api.JavaPunctuator.LPOINT;
import static org.sonar.java.ast.api.JavaPunctuator.LT;
import static org.sonar.java.ast.api.JavaPunctuator.LWING;
import static org.sonar.java.ast.api.JavaPunctuator.MINUS;
import static org.sonar.java.ast.api.JavaPunctuator.MINUSEQU;
import static org.sonar.java.ast.api.JavaPunctuator.MOD;
import static org.sonar.java.ast.api.JavaPunctuator.MODEQU;
import static org.sonar.java.ast.api.JavaPunctuator.NOTEQUAL;
import static org.sonar.java.ast.api.JavaPunctuator.OR;
import static org.sonar.java.ast.api.JavaPunctuator.OREQU;
import static org.sonar.java.ast.api.JavaPunctuator.OROR;
import static org.sonar.java.ast.api.JavaPunctuator.PLUS;
import static org.sonar.java.ast.api.JavaPunctuator.PLUSEQU;
import static org.sonar.java.ast.api.JavaPunctuator.QUERY;
import static org.sonar.java.ast.api.JavaPunctuator.RBRK;
import static org.sonar.java.ast.api.JavaPunctuator.RPAR;
import static org.sonar.java.ast.api.JavaPunctuator.RPOINT;
import static org.sonar.java.ast.api.JavaPunctuator.RWING;
import static org.sonar.java.ast.api.JavaPunctuator.SEMI;
import static org.sonar.java.ast.api.JavaPunctuator.SL;
import static org.sonar.java.ast.api.JavaPunctuator.SLEQU;
import static org.sonar.java.ast.api.JavaPunctuator.SR;
import static org.sonar.java.ast.api.JavaPunctuator.SREQU;
import static org.sonar.java.ast.api.JavaPunctuator.STAR;
import static org.sonar.java.ast.api.JavaPunctuator.STAREQU;
import static org.sonar.java.ast.api.JavaPunctuator.TILDA;
import static org.sonar.java.ast.api.JavaTokenType.CHARACTER_LITERAL;
import static org.sonar.java.ast.api.JavaTokenType.DOUBLE_LITERAL;
import static org.sonar.java.ast.api.JavaTokenType.FLOAT_LITERAL;
import static org.sonar.java.ast.api.JavaTokenType.INTEGER_LITERAL;
import static org.sonar.java.ast.api.JavaTokenType.LONG_LITERAL;

public enum JavaGrammar implements GrammarRuleKey {

  COMPILATION_UNIT,
  PACKAGE_DECLARATION,
  IMPORT_DECLARATION,
  TYPE_DECLARATION,

  ANNOTATION,
  QUALIFIED_IDENTIFIER,
  QUALIFIED_IDENTIFIER_LIST,

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
  STATEMENT_EXPRESSION,

  TRY_STATEMENT,
  TRY_WITH_RESOURCES_STATEMENT,
  RESOURCE_SPECIFICATION,

  SWITCH_BLOCK_STATEMENT_GROUP,
  SWITCH_LABEL,

  BASIC_TYPE,
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
  NEW_EXPRESSION,
  BASIC_CLASS_EXPRESSION,
  VOID_CLASS_EXPRESSION,
  SELECTOR,
  POST_FIX_OP,
  NON_WILDCARD_TYPE_ARGUMENTS,
  LITERAL,
  CREATOR,
  INNER_CREATOR,
  DIM_EXPR,
  CREATED_NAME,
  CLASS_CREATOR_REST,
  DIAMOND,
  ARRAY_CREATOR_REST,
  ARRAY_INITIALIZER,

  EOF,

  LETTER_OR_DIGIT,
  KEYWORD,
  SPACING,

  METHOD_REFERENCE,
  LAMBDA_EXPRESSION,
  LAMBDA_PARAMETERS,
  LAMBDA_BODY,
  ARROW,
  UNARY_EXPRESSION_NOT_PLUS_MINUS,
  CAST_EXPRESSION,

  // Helpers
  // TODO Introduce ANNOTATIONS?

  MODIFIERS,

  ANNOTATION_ARGUMENTS,
  INFERED_PARAMS,

  MEMBER_SELECT_OR_METHOD_INVOCATION;

  public static LexerlessGrammarBuilder createGrammarBuilder() {
    LexerlessGrammarBuilder b = LexerlessGrammarBuilder.create();

    punctuators(b);
    keywords(b);

    compilationsUnits(b);
    classDeclaration(b);
    interfaceDeclarations(b);
    enums(b);
    blocksAndStatements(b);
    expressions(b);
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
    punctuator(b, DBLECOLON, "::");
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
    punctuator(b, MINUSEQU, "-=");
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

    punctuator(b, ARROW, "->");
  }

  private static void keywords(LexerlessGrammarBuilder b) {
    b.rule(LETTER_OR_DIGIT).is(javaIdentifierPart(b));
    for (JavaKeyword tokenType : JavaKeyword.values()) {
      b.rule(tokenType).is(tokenType.getValue(), b.nextNot(LETTER_OR_DIGIT), SPACING);
    }
    String[] keywords = JavaKeyword.keywordValues();
    Arrays.sort(keywords);
    ArrayUtils.reverse(keywords);
    b.rule(KEYWORD).is(
      b.firstOf(
        keywords[0],
        keywords[1],
        ArrayUtils.subarray(keywords, 2, keywords.length)),
      b.nextNot(LETTER_OR_DIGIT));
  }

  private static void punctuator(LexerlessGrammarBuilder b, GrammarRuleKey ruleKey, String value) {
    b.rule(ruleKey).is(value, SPACING);
  }

  private static void punctuator(LexerlessGrammarBuilder b, GrammarRuleKey ruleKey, String value, Object element) {
    b.rule(ruleKey).is(value, element, SPACING);
  }

  private static final String EXP_REGEXP = "(?:[Ee][+-]?+[0-9_]++)";
  private static final String BINARY_EXP_REGEXP = "(?:[Pp][+-]?+[0-9_]++)";
  private static final String FLOATING_LITERAL_WITHOUT_SUFFIX_REGEXP = "(?:" +
    // Decimal
    "[0-9][0-9_]*+\\.([0-9_]++)?+" + EXP_REGEXP + "?+" +
    "|" + "\\.[0-9][0-9_]*+" + EXP_REGEXP + "?+" +
    "|" + "[0-9][0-9_]*+" + EXP_REGEXP +
    // Hexadecimal
    "|" + "0[xX][0-9_a-fA-F]++\\.[0-9_a-fA-F]*+" + BINARY_EXP_REGEXP +
    "|" + "0[xX][0-9_a-fA-F]++" + BINARY_EXP_REGEXP +
    ")";

  private static final String INTEGER_LITERAL_REGEXP = "(?:" +
    // Hexadecimal
    "0[xX][0-9_a-fA-F]++" +
    // Binary (Java 7)
    "|" + "0[bB][01_]++" +
    // Decimal and Octal
    "|" + "[0-9][0-9_]*+" +
    ")";

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

    b.rule(CHARACTER_LITERAL).is(characterLiteral(b), SPACING);
    b.rule(JavaTokenType.LITERAL).is(stringLiteral(b), SPACING);

    b.rule(FLOAT_LITERAL).is(b.regexp(FLOATING_LITERAL_WITHOUT_SUFFIX_REGEXP + "[fF]|[0-9][0-9_]*+[fF]"), SPACING);
    b.rule(DOUBLE_LITERAL).is(b.regexp(FLOATING_LITERAL_WITHOUT_SUFFIX_REGEXP + "[dD]?+|[0-9][0-9_]*+[dD]"), SPACING);

    b.rule(LONG_LITERAL).is(b.regexp(INTEGER_LITERAL_REGEXP + "[lL]"), SPACING);
    b.rule(INTEGER_LITERAL).is(b.regexp(INTEGER_LITERAL_REGEXP), SPACING);

    b.rule(JavaTokenType.IDENTIFIER).is(
      b.firstOf(
        b.next(ENUM),
        b.nextNot(KEYWORD)),
      javaIdentifier(b),
      SPACING);
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
   * 7.3. Compilation Units
   */
  private static void compilationsUnits(LexerlessGrammarBuilder b) {
    b.rule(COMPILATION_UNIT).is(SPACING, b.optional(PACKAGE_DECLARATION), b.zeroOrMore(IMPORT_DECLARATION), b.zeroOrMore(TYPE_DECLARATION), EOF);

    b.rule(PACKAGE_DECLARATION).is(b.zeroOrMore(ANNOTATION), PACKAGE, QUALIFIED_IDENTIFIER, SEMI);
    b.rule(IMPORT_DECLARATION).is(IMPORT, b.optional(STATIC), QUALIFIED_IDENTIFIER, b.optional(DOT, STAR), SEMI);
    b.rule(TYPE_DECLARATION).is(b.firstOf(
      b.sequence(MODIFIERS, b.firstOf(CLASS_DECLARATION, ENUM_DECLARATION, INTERFACE_DECLARATION, ANNOTATION_TYPE_DECLARATION)),
      SEMI));
  }

  /**
   * 8.1. Class Declaration
   */
  private static void classDeclaration(LexerlessGrammarBuilder b) {
    b.rule(CLASS_DECLARATION).is(CLASS, JavaTokenType.IDENTIFIER, b.optional(TYPE_PARAMETERS), b.optional(EXTENDS, CLASS_TYPE), b.optional(IMPLEMENTS, CLASS_TYPE_LIST),
      CLASS_BODY);

    b.rule(CLASS_BODY).is(LWING, b.zeroOrMore(CLASS_BODY_DECLARATION), RWING);
    b.rule(CLASS_BODY_DECLARATION).is(b.firstOf(
      SEMI,
      CLASS_INIT_DECLARATION,
      b.sequence(MODIFIERS, MEMBER_DECL)));
    b.rule(CLASS_INIT_DECLARATION).is(b.optional(STATIC), BLOCK);
    b.rule(MEMBER_DECL).is(b.firstOf(
      b.sequence(TYPE_PARAMETERS, GENERIC_METHOD_OR_CONSTRUCTOR_REST),
      b.sequence(TYPE, JavaTokenType.IDENTIFIER, METHOD_DECLARATOR_REST),
      FIELD_DECLARATION,
      b.sequence(VOID, JavaTokenType.IDENTIFIER, VOID_METHOD_DECLARATOR_REST),
      b.sequence(JavaTokenType.IDENTIFIER, CONSTRUCTOR_DECLARATOR_REST),
      INTERFACE_DECLARATION,
      CLASS_DECLARATION,
      ENUM_DECLARATION,
      ANNOTATION_TYPE_DECLARATION));
    b.rule(FIELD_DECLARATION).is(TYPE, VARIABLE_DECLARATORS, SEMI);
    b.rule(GENERIC_METHOD_OR_CONSTRUCTOR_REST).is(b.firstOf(
      b.sequence(b.firstOf(TYPE, VOID), JavaTokenType.IDENTIFIER, METHOD_DECLARATOR_REST),
      b.sequence(JavaTokenType.IDENTIFIER, CONSTRUCTOR_DECLARATOR_REST)));
    b.rule(METHOD_DECLARATOR_REST).is(FORMAL_PARAMETERS, b.zeroOrMore(b.zeroOrMore(ANNOTATION), DIM), b.optional(THROWS, QUALIFIED_IDENTIFIER_LIST), b.firstOf(METHOD_BODY, SEMI));
    b.rule(VOID_METHOD_DECLARATOR_REST).is(FORMAL_PARAMETERS, b.optional(THROWS, QUALIFIED_IDENTIFIER_LIST), b.firstOf(METHOD_BODY, SEMI));
    b.rule(CONSTRUCTOR_DECLARATOR_REST).is(FORMAL_PARAMETERS, b.optional(THROWS, QUALIFIED_IDENTIFIER_LIST), METHOD_BODY);
    b.rule(METHOD_BODY).is(BLOCK);
  }

  /**
   * 8.9. Enums
   */
  private static void enums(LexerlessGrammarBuilder b) {
    b.rule(ENUM_DECLARATION).is(ENUM, JavaTokenType.IDENTIFIER, b.optional(IMPLEMENTS, CLASS_TYPE_LIST), ENUM_BODY);
    b.rule(ENUM_BODY).is(LWING, b.optional(ENUM_CONSTANTS), b.optional(COMMA), b.optional(ENUM_BODY_DECLARATIONS), RWING);
    b.rule(ENUM_CONSTANTS).is(ENUM_CONSTANT, b.zeroOrMore(COMMA, ENUM_CONSTANT));
    b.rule(ENUM_CONSTANT).is(b.zeroOrMore(ANNOTATION), JavaTokenType.IDENTIFIER, b.optional(ARGUMENTS), b.optional(CLASS_BODY));
    b.rule(ENUM_BODY_DECLARATIONS).is(SEMI, b.zeroOrMore(CLASS_BODY_DECLARATION));
  }

  /**
   * 9.1. Interface Declarations
   */
  private static void interfaceDeclarations(LexerlessGrammarBuilder b) {
    b.rule(INTERFACE_DECLARATION).is(INTERFACE, JavaTokenType.IDENTIFIER, b.optional(TYPE_PARAMETERS), b.optional(EXTENDS, CLASS_TYPE_LIST), INTERFACE_BODY);

    b.rule(INTERFACE_BODY).is(LWING, b.zeroOrMore(INTERFACE_BODY_DECLARATION), RWING);
    b.rule(INTERFACE_BODY_DECLARATION).is(b.firstOf(
      b.sequence(MODIFIERS, INTERFACE_MEMBER_DECL),
      SEMI));
    b.rule(INTERFACE_MEMBER_DECL).is(b.firstOf(
      INTERFACE_METHOD_OR_FIELD_DECL,
      INTERFACE_GENERIC_METHOD_DECL,
      b.sequence(VOID, JavaTokenType.IDENTIFIER, VOID_INTERFACE_METHOD_DECLARATORS_REST),
      INTERFACE_DECLARATION,
      ANNOTATION_TYPE_DECLARATION,
      CLASS_DECLARATION,
      ENUM_DECLARATION));
    b.rule(INTERFACE_METHOD_OR_FIELD_DECL).is(TYPE, JavaTokenType.IDENTIFIER, INTERFACE_METHOD_OR_FIELD_REST);
    b.rule(INTERFACE_METHOD_OR_FIELD_REST).is(b.firstOf(
      b.sequence(CONSTANT_DECLARATORS_REST, SEMI),
      INTERFACE_METHOD_DECLARATOR_REST));
    b.rule(INTERFACE_METHOD_DECLARATOR_REST).is(FORMAL_PARAMETERS, b.zeroOrMore(b.zeroOrMore(ANNOTATION), DIM),
      b.optional(THROWS, QUALIFIED_IDENTIFIER_LIST), b.firstOf(SEMI, METHOD_BODY));
    b.rule(INTERFACE_GENERIC_METHOD_DECL).is(TYPE_PARAMETERS, b.firstOf(TYPE, VOID), JavaTokenType.IDENTIFIER, INTERFACE_METHOD_DECLARATOR_REST);
    b.rule(VOID_INTERFACE_METHOD_DECLARATORS_REST).is(FORMAL_PARAMETERS, b.optional(THROWS, QUALIFIED_IDENTIFIER_LIST), b.firstOf(SEMI, METHOD_BODY));
    b.rule(CONSTANT_DECLARATORS_REST).is(CONSTANT_DECLARATOR_REST, b.zeroOrMore(COMMA, CONSTANT_DECLARATOR));
    b.rule(CONSTANT_DECLARATOR).is(JavaTokenType.IDENTIFIER, CONSTANT_DECLARATOR_REST);
    b.rule(CONSTANT_DECLARATOR_REST).is(b.zeroOrMore(DIM), EQU, VARIABLE_INITIALIZER);
  }

  /**
   * 14. Blocks and Statements
   */
  private static void blocksAndStatements(LexerlessGrammarBuilder b) {
    // 14.2. Blocks
    b.rule(BLOCK_STATEMENTS).is(b.zeroOrMore(BLOCK_STATEMENT));
    b.rule(BLOCK_STATEMENT).is(
      b.firstOf(
        LOCAL_VARIABLE_DECLARATION_STATEMENT,
        b.sequence(MODIFIERS, b.firstOf(CLASS_DECLARATION, ENUM_DECLARATION)),
        STATEMENT));
  }

  /**
   * 15. Expressions
   */
  private static void expressions(LexerlessGrammarBuilder b) {
    b.rule(NON_WILDCARD_TYPE_ARGUMENTS).is(LPOINT, TYPE, b.zeroOrMore(COMMA, TYPE), RPOINT);
    b.rule(SELECTOR).is(
      b.firstOf(
        b.sequence(DOT, MEMBER_SELECT_OR_METHOD_INVOCATION),
        // TODO: Alternative with IDENTIFIER, ARUGMENTS is now consumed by METHOD_INVOCATION
        b.sequence(DOT, NEW, b.optional(NON_WILDCARD_TYPE_ARGUMENTS), INNER_CREATOR),
        DIM_EXPR,
        // Specific to IDENTIFIER_SUFFIX
        b.sequence(b.zeroOrMore(DIM), DOT, CLASS)));

    b.rule(MEMBER_SELECT_OR_METHOD_INVOCATION).is(
      b.optional(NON_WILDCARD_TYPE_ARGUMENTS),
      b.firstOf(
        JavaTokenType.IDENTIFIER,
        JavaKeyword.THIS,
        JavaKeyword.SUPER),
      b.optional(ARGUMENTS));

    // TODO Factorize annotated identifier
    b.rule(CREATED_NAME).is(b.zeroOrMore(ANNOTATION), JavaTokenType.IDENTIFIER, b.optional(NON_WILDCARD_TYPE_ARGUMENTS),
      b.zeroOrMore(DOT, b.zeroOrMore(ANNOTATION), JavaTokenType.IDENTIFIER, b.optional(NON_WILDCARD_TYPE_ARGUMENTS)));
    b.rule(INNER_CREATOR).is(JavaTokenType.IDENTIFIER, CLASS_CREATOR_REST);
    b.rule(CLASS_CREATOR_REST).is(b.optional(b.firstOf(DIAMOND, TYPE_ARGUMENTS)), ARGUMENTS, b.optional(CLASS_BODY));
    b.rule(DIAMOND).is(LT, GT);
    b.rule(DIM).is(LBRK, RBRK);
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
