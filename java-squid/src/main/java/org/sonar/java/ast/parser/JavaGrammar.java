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
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.Arrays;

import static org.sonar.java.ast.api.JavaKeyword.ABSTRACT;
import static org.sonar.java.ast.api.JavaKeyword.ASSERT;
import static org.sonar.java.ast.api.JavaKeyword.BOOLEAN;
import static org.sonar.java.ast.api.JavaKeyword.BREAK;
import static org.sonar.java.ast.api.JavaKeyword.BYTE;
import static org.sonar.java.ast.api.JavaKeyword.CASE;
import static org.sonar.java.ast.api.JavaKeyword.CATCH;
import static org.sonar.java.ast.api.JavaKeyword.CHAR;
import static org.sonar.java.ast.api.JavaKeyword.CLASS;
import static org.sonar.java.ast.api.JavaKeyword.CONTINUE;
import static org.sonar.java.ast.api.JavaKeyword.DEFAULT;
import static org.sonar.java.ast.api.JavaKeyword.DO;
import static org.sonar.java.ast.api.JavaKeyword.DOUBLE;
import static org.sonar.java.ast.api.JavaKeyword.ELSE;
import static org.sonar.java.ast.api.JavaKeyword.ENUM;
import static org.sonar.java.ast.api.JavaKeyword.EXTENDS;
import static org.sonar.java.ast.api.JavaKeyword.FALSE;
import static org.sonar.java.ast.api.JavaKeyword.FINAL;
import static org.sonar.java.ast.api.JavaKeyword.FINALLY;
import static org.sonar.java.ast.api.JavaKeyword.FLOAT;
import static org.sonar.java.ast.api.JavaKeyword.FOR;
import static org.sonar.java.ast.api.JavaKeyword.IF;
import static org.sonar.java.ast.api.JavaKeyword.IMPLEMENTS;
import static org.sonar.java.ast.api.JavaKeyword.IMPORT;
import static org.sonar.java.ast.api.JavaKeyword.INSTANCEOF;
import static org.sonar.java.ast.api.JavaKeyword.INT;
import static org.sonar.java.ast.api.JavaKeyword.INTERFACE;
import static org.sonar.java.ast.api.JavaKeyword.LONG;
import static org.sonar.java.ast.api.JavaKeyword.NATIVE;
import static org.sonar.java.ast.api.JavaKeyword.NEW;
import static org.sonar.java.ast.api.JavaKeyword.NULL;
import static org.sonar.java.ast.api.JavaKeyword.PACKAGE;
import static org.sonar.java.ast.api.JavaKeyword.PRIVATE;
import static org.sonar.java.ast.api.JavaKeyword.PROTECTED;
import static org.sonar.java.ast.api.JavaKeyword.PUBLIC;
import static org.sonar.java.ast.api.JavaKeyword.RETURN;
import static org.sonar.java.ast.api.JavaKeyword.SHORT;
import static org.sonar.java.ast.api.JavaKeyword.STATIC;
import static org.sonar.java.ast.api.JavaKeyword.STRICTFP;
import static org.sonar.java.ast.api.JavaKeyword.SUPER;
import static org.sonar.java.ast.api.JavaKeyword.SWITCH;
import static org.sonar.java.ast.api.JavaKeyword.SYNCHRONIZED;
import static org.sonar.java.ast.api.JavaKeyword.THIS;
import static org.sonar.java.ast.api.JavaKeyword.THROW;
import static org.sonar.java.ast.api.JavaKeyword.THROWS;
import static org.sonar.java.ast.api.JavaKeyword.TRANSIENT;
import static org.sonar.java.ast.api.JavaKeyword.TRUE;
import static org.sonar.java.ast.api.JavaKeyword.TRY;
import static org.sonar.java.ast.api.JavaKeyword.VOID;
import static org.sonar.java.ast.api.JavaKeyword.VOLATILE;
import static org.sonar.java.ast.api.JavaKeyword.WHILE;
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
import static org.sonar.java.ast.api.JavaTokenType.IDENTIFIER;
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
  CAST_EXPRESSION;

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

    b.rule(IDENTIFIER).is(
        b.firstOf(
            b.next(ENUM),
            b.nextNot(KEYWORD)),
        javaIdentifier(b),
        SPACING);

    b.rule(LITERAL).is(b.firstOf(
        TRUE,
        FALSE,
        NULL,
        CHARACTER_LITERAL,
        JavaTokenType.LITERAL,
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
    b.rule(TYPE).is(b.firstOf(BASIC_TYPE, CLASS_TYPE), b.zeroOrMore(b.zeroOrMore(ANNOTATION), DIM));
    b.rule(CLASS_TYPE).is(b.zeroOrMore(ANNOTATION), IDENTIFIER, b.optional(TYPE_ARGUMENTS), b.zeroOrMore(DOT, b.zeroOrMore(ANNOTATION), IDENTIFIER, b.optional(TYPE_ARGUMENTS)));
    b.rule(CLASS_TYPE_LIST).is(CLASS_TYPE, b.zeroOrMore(COMMA, CLASS_TYPE));
    b.rule(TYPE_ARGUMENTS).is(LPOINT, TYPE_ARGUMENT, b.zeroOrMore(COMMA, TYPE_ARGUMENT), RPOINT);
    b.rule(TYPE_ARGUMENT).is(
        b.zeroOrMore(ANNOTATION),
        b.firstOf(
          TYPE,
          b.sequence(QUERY, b.optional(b.firstOf(EXTENDS, SUPER), b.zeroOrMore(ANNOTATION), TYPE)))
    );
    b.rule(TYPE_PARAMETERS).is(LPOINT, TYPE_PARAMETER, b.zeroOrMore(COMMA, TYPE_PARAMETER), RPOINT);
    b.rule(TYPE_PARAMETER).is(b.zeroOrMore(ANNOTATION), IDENTIFIER, b.optional(EXTENDS, BOUND));
    b.rule(BOUND).is(CLASS_TYPE, b.zeroOrMore(AND, b.zeroOrMore(ANNOTATION), CLASS_TYPE));
    b.rule(MODIFIER).is(b.firstOf(
        ANNOTATION,
        PUBLIC,
        PROTECTED,
        PRIVATE,
        ABSTRACT,
        STATIC,
        FINAL,
        TRANSIENT,
        VOLATILE,
        SYNCHRONIZED,
        NATIVE,
        DEFAULT,
        STRICTFP));
  }

  /**
   * 7.3. Compilation Units
   */
  private static void compilationsUnits(LexerlessGrammarBuilder b) {
    b.rule(COMPILATION_UNIT).is(SPACING, b.optional(PACKAGE_DECLARATION), b.zeroOrMore(IMPORT_DECLARATION), b.zeroOrMore(TYPE_DECLARATION), EOF);

    b.rule(PACKAGE_DECLARATION).is(b.zeroOrMore(ANNOTATION), PACKAGE, QUALIFIED_IDENTIFIER, SEMI);
    b.rule(IMPORT_DECLARATION).is(IMPORT, b.optional(STATIC), QUALIFIED_IDENTIFIER, b.optional(DOT, STAR), SEMI);
    b.rule(TYPE_DECLARATION).is(b.firstOf(
        b.sequence(b.zeroOrMore(MODIFIER), b.firstOf(CLASS_DECLARATION, ENUM_DECLARATION, INTERFACE_DECLARATION, ANNOTATION_TYPE_DECLARATION)),
        SEMI));
  }

  /**
   * 8.1. Class Declaration
   */
  private static void classDeclaration(LexerlessGrammarBuilder b) {
    b.rule(CLASS_DECLARATION).is(CLASS, IDENTIFIER, b.optional(TYPE_PARAMETERS), b.optional(EXTENDS, CLASS_TYPE), b.optional(IMPLEMENTS, CLASS_TYPE_LIST),
        CLASS_BODY);

    b.rule(CLASS_BODY).is(LWING, b.zeroOrMore(CLASS_BODY_DECLARATION), RWING);
    b.rule(CLASS_BODY_DECLARATION).is(b.firstOf(
        SEMI,
        CLASS_INIT_DECLARATION,
        b.sequence(b.zeroOrMore(MODIFIER), MEMBER_DECL)));
    b.rule(CLASS_INIT_DECLARATION).is(b.optional(STATIC), BLOCK);
    b.rule(MEMBER_DECL).is(b.firstOf(
        b.sequence(TYPE_PARAMETERS, GENERIC_METHOD_OR_CONSTRUCTOR_REST),
        b.sequence(TYPE, IDENTIFIER, METHOD_DECLARATOR_REST),
        FIELD_DECLARATION,
        b.sequence(VOID, IDENTIFIER, VOID_METHOD_DECLARATOR_REST),
        b.sequence(IDENTIFIER, CONSTRUCTOR_DECLARATOR_REST),
        INTERFACE_DECLARATION,
        CLASS_DECLARATION,
        ENUM_DECLARATION,
        ANNOTATION_TYPE_DECLARATION));
    b.rule(FIELD_DECLARATION).is(TYPE, VARIABLE_DECLARATORS, SEMI);
    b.rule(GENERIC_METHOD_OR_CONSTRUCTOR_REST).is(b.firstOf(
        b.sequence(b.firstOf(TYPE, VOID), IDENTIFIER, METHOD_DECLARATOR_REST),
        b.sequence(IDENTIFIER, CONSTRUCTOR_DECLARATOR_REST)));
    b.rule(METHOD_DECLARATOR_REST).is(FORMAL_PARAMETERS, b.zeroOrMore(b.zeroOrMore(ANNOTATION), DIM), b.optional(THROWS, QUALIFIED_IDENTIFIER_LIST), b.firstOf(METHOD_BODY, SEMI));
    b.rule(VOID_METHOD_DECLARATOR_REST).is(FORMAL_PARAMETERS, b.optional(THROWS, QUALIFIED_IDENTIFIER_LIST), b.firstOf(METHOD_BODY, SEMI));
    b.rule(CONSTRUCTOR_DECLARATOR_REST).is(FORMAL_PARAMETERS, b.optional(THROWS, QUALIFIED_IDENTIFIER_LIST), METHOD_BODY);
    b.rule(METHOD_BODY).is(BLOCK);
  }

  /**
   * 8.9. Enums
   */
  private static void enums(LexerlessGrammarBuilder b) {
    b.rule(ENUM_DECLARATION).is(ENUM, IDENTIFIER, b.optional(IMPLEMENTS, CLASS_TYPE_LIST), ENUM_BODY);
    b.rule(ENUM_BODY).is(LWING, b.optional(ENUM_CONSTANTS), b.optional(COMMA), b.optional(ENUM_BODY_DECLARATIONS), RWING);
    b.rule(ENUM_CONSTANTS).is(ENUM_CONSTANT, b.zeroOrMore(COMMA, ENUM_CONSTANT));
    b.rule(ENUM_CONSTANT).is(b.zeroOrMore(ANNOTATION), IDENTIFIER, b.optional(ARGUMENTS), b.optional(CLASS_BODY));
    b.rule(ENUM_BODY_DECLARATIONS).is(SEMI, b.zeroOrMore(CLASS_BODY_DECLARATION));
  }

  /**
   * 9.1. Interface Declarations
   */
  private static void interfaceDeclarations(LexerlessGrammarBuilder b) {
    b.rule(INTERFACE_DECLARATION).is(INTERFACE, IDENTIFIER, b.optional(TYPE_PARAMETERS), b.optional(EXTENDS, CLASS_TYPE_LIST), INTERFACE_BODY);

    b.rule(INTERFACE_BODY).is(LWING, b.zeroOrMore(INTERFACE_BODY_DECLARATION), RWING);
    b.rule(INTERFACE_BODY_DECLARATION).is(b.firstOf(
        b.sequence(b.zeroOrMore(MODIFIER), INTERFACE_MEMBER_DECL),
        SEMI));
    b.rule(INTERFACE_MEMBER_DECL).is(b.firstOf(
        INTERFACE_METHOD_OR_FIELD_DECL,
        INTERFACE_GENERIC_METHOD_DECL,
        b.sequence(VOID, IDENTIFIER, VOID_INTERFACE_METHOD_DECLARATORS_REST),
        INTERFACE_DECLARATION,
        ANNOTATION_TYPE_DECLARATION,
        CLASS_DECLARATION,
        ENUM_DECLARATION));
    b.rule(INTERFACE_METHOD_OR_FIELD_DECL).is(TYPE, IDENTIFIER, INTERFACE_METHOD_OR_FIELD_REST);
    b.rule(INTERFACE_METHOD_OR_FIELD_REST).is(b.firstOf(
        b.sequence(CONSTANT_DECLARATORS_REST, SEMI),
        INTERFACE_METHOD_DECLARATOR_REST));
    b.rule(INTERFACE_METHOD_DECLARATOR_REST).is(FORMAL_PARAMETERS, b.zeroOrMore(b.zeroOrMore(ANNOTATION), DIM),
        b.optional(THROWS, QUALIFIED_IDENTIFIER_LIST), b.firstOf(SEMI, BLOCK));
    b.rule(INTERFACE_GENERIC_METHOD_DECL).is(TYPE_PARAMETERS, b.firstOf(TYPE, VOID), IDENTIFIER, INTERFACE_METHOD_DECLARATOR_REST);
    b.rule(VOID_INTERFACE_METHOD_DECLARATORS_REST).is(FORMAL_PARAMETERS, b.optional(THROWS, QUALIFIED_IDENTIFIER_LIST), b.firstOf(SEMI, METHOD_BODY));
    b.rule(CONSTANT_DECLARATORS_REST).is(CONSTANT_DECLARATOR_REST, b.zeroOrMore(COMMA, CONSTANT_DECLARATOR));
    b.rule(CONSTANT_DECLARATOR).is(IDENTIFIER, CONSTANT_DECLARATOR_REST);
    b.rule(CONSTANT_DECLARATOR_REST).is(b.zeroOrMore(DIM), EQU, VARIABLE_INITIALIZER);
  }

  /**
   * 8.4.1. Formal Parameters
   */
  private static void formalParameters(LexerlessGrammarBuilder b) {
    b.rule(FORMAL_PARAMETERS).is(LPAR, b.optional(FORMAL_PARAMETER_DECLS), RPAR);
    b.rule(FORMAL_PARAMETER).is(b.zeroOrMore(b.firstOf(FINAL, ANNOTATION)), TYPE, VARIABLE_DECLARATOR_ID);
    b.rule(FORMAL_PARAMETER_DECLS).is(b.zeroOrMore(b.firstOf(FINAL, ANNOTATION)), TYPE, FORMAL_PARAMETERS_DECLS_REST);
    b.rule(FORMAL_PARAMETERS_DECLS_REST).is(b.firstOf(
        b.sequence(VARIABLE_DECLARATOR_ID, b.optional(COMMA, FORMAL_PARAMETER_DECLS)),
        b.sequence(b.zeroOrMore(ANNOTATION), ELLIPSIS, VARIABLE_DECLARATOR_ID)));
    b.rule(VARIABLE_DECLARATOR_ID).is(IDENTIFIER, b.zeroOrMore(b.zeroOrMore(ANNOTATION), DIM));
  }

  /**
   * 9.7. Annotations
   */
  private static void annotations(LexerlessGrammarBuilder b) {
    b.rule(ANNOTATION_TYPE_DECLARATION).is(AT, INTERFACE, IDENTIFIER, ANNOTATION_TYPE_BODY);
    b.rule(ANNOTATION_TYPE_BODY).is(LWING, b.zeroOrMore(ANNOTATION_TYPE_ELEMENT_DECLARATION), RWING);
    b.rule(ANNOTATION_TYPE_ELEMENT_DECLARATION).is(b.firstOf(
        b.sequence(b.zeroOrMore(MODIFIER), ANNOTATION_TYPE_ELEMENT_REST),
        SEMI));
    b.rule(ANNOTATION_TYPE_ELEMENT_REST).is(b.firstOf(
        b.sequence(TYPE, IDENTIFIER, ANNOTATION_METHOD_OR_CONSTANT_REST, SEMI),
        CLASS_DECLARATION,
        ENUM_DECLARATION,
        INTERFACE_DECLARATION,
        ANNOTATION_TYPE_DECLARATION));
    b.rule(ANNOTATION_METHOD_OR_CONSTANT_REST).is(b.firstOf(
        ANNOTATION_METHOD_REST,
        CONSTANT_DECLARATORS_REST));
    b.rule(ANNOTATION_METHOD_REST).is(LPAR, RPAR, b.optional(DEFAULT_VALUE));
    b.rule(DEFAULT_VALUE).is(DEFAULT, ELEMENT_VALUE);
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
    b.rule(LOCAL_VARIABLE_DECLARATION_STATEMENT).is(b.optional(VARIABLE_MODIFIERS), TYPE, VARIABLE_DECLARATORS, SEMI);
    b.rule(VARIABLE_MODIFIERS).is(b.oneOrMore(b.firstOf(
        ANNOTATION,
        FINAL)));
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
    b.rule(IF_STATEMENT).is(IF, PAR_EXPRESSION, STATEMENT, b.optional(ELSE, STATEMENT));
    // 14.10. The assert Statement
    b.rule(ASSERT_STATEMENT).is(ASSERT, EXPRESSION, b.optional(COLON, EXPRESSION), SEMI);

    // 14.11. The switch statement
    b.rule(SWITCH_STATEMENT).is(SWITCH, PAR_EXPRESSION, LWING, SWITCH_BLOCK_STATEMENT_GROUPS, RWING);
    b.rule(SWITCH_BLOCK_STATEMENT_GROUPS).is(b.zeroOrMore(SWITCH_BLOCK_STATEMENT_GROUP));
    b.rule(SWITCH_BLOCK_STATEMENT_GROUP).is(SWITCH_LABEL, BLOCK_STATEMENTS);
    b.rule(SWITCH_LABEL).is(b.firstOf(
        b.sequence(CASE, CONSTANT_EXPRESSION, COLON),
        b.sequence(DEFAULT, COLON)));

    // 14.12. The while Statement
    b.rule(WHILE_STATEMENT).is(WHILE, PAR_EXPRESSION, STATEMENT);
    // 14.13. The do Statement
    b.rule(DO_STATEMENT).is(DO, STATEMENT, WHILE, PAR_EXPRESSION, SEMI);

    // 14.14. The for Statement
    b.rule(FOR_STATEMENT).is(b.firstOf(
        b.sequence(FOR, LPAR, b.optional(FOR_INIT), SEMI, b.optional(EXPRESSION), SEMI, b.optional(FOR_UPDATE), RPAR, STATEMENT),
        b.sequence(FOR, LPAR, FORMAL_PARAMETER, COLON, EXPRESSION, RPAR, STATEMENT)));
    b.rule(FOR_INIT).is(b.firstOf(
        b.sequence(b.zeroOrMore(b.firstOf(FINAL, ANNOTATION)), TYPE, VARIABLE_DECLARATORS),
        b.sequence(STATEMENT_EXPRESSION, b.zeroOrMore(COMMA, STATEMENT_EXPRESSION))));
    b.rule(FOR_UPDATE).is(STATEMENT_EXPRESSION, b.zeroOrMore(COMMA, STATEMENT_EXPRESSION));

    // 14.15. The break Statement
    b.rule(BREAK_STATEMENT).is(BREAK, b.optional(IDENTIFIER), SEMI);
    // 14.16. The continue Statement
    b.rule(CONTINUE_STATEMENT).is(CONTINUE, b.optional(IDENTIFIER), SEMI);
    // 14.17. The return Statement
    b.rule(RETURN_STATEMENT).is(RETURN, b.optional(EXPRESSION), SEMI);
    // 14.18. The throw Statement
    b.rule(THROW_STATEMENT).is(THROW, EXPRESSION, SEMI);
    // 14.19. The synchronized Statement
    b.rule(SYNCHRONIZED_STATEMENT).is(SYNCHRONIZED, PAR_EXPRESSION, BLOCK);

    // 14.20. The try Statement
    b.rule(TRY_STATEMENT).is(b.firstOf(
        b.sequence(TRY, BLOCK, b.firstOf(b.sequence(b.oneOrMore(CATCH_CLAUSE), b.optional(FINALLY_)), FINALLY_)),
        TRY_WITH_RESOURCES_STATEMENT));
    b.rule(TRY_WITH_RESOURCES_STATEMENT).is(TRY, RESOURCE_SPECIFICATION, BLOCK, b.zeroOrMore(CATCH_CLAUSE), b.optional(FINALLY_));
    b.rule(RESOURCE_SPECIFICATION).is(LPAR, RESOURCE, b.zeroOrMore(SEMI, RESOURCE), b.optional(SEMI), RPAR);
    b.rule(RESOURCE).is(b.optional(VARIABLE_MODIFIERS), CLASS_TYPE, VARIABLE_DECLARATOR_ID, EQU, EXPRESSION);

    b.rule(CATCH_CLAUSE).is(CATCH, LPAR, CATCH_FORMAL_PARAMETER, RPAR, BLOCK);
    b.rule(CATCH_FORMAL_PARAMETER).is(b.optional(VARIABLE_MODIFIERS), CATCH_TYPE, VARIABLE_DECLARATOR_ID);
    b.rule(CATCH_TYPE).is(QUALIFIED_IDENTIFIER, b.zeroOrMore(OR, QUALIFIED_IDENTIFIER));

    b.rule(FINALLY_).is(FINALLY, BLOCK);
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
        MINUSEQU,
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
        b.sequence(INSTANCEOF, TYPE)))).skipIfOneChild();
    b.rule(SHIFT_EXPRESSION).is(ADDITIVE_EXPRESSION, b.zeroOrMore(b.firstOf(SL, BSR, SR), ADDITIVE_EXPRESSION)).skipIfOneChild();
    b.rule(ADDITIVE_EXPRESSION).is(MULTIPLICATIVE_EXPRESSION, b.zeroOrMore(b.firstOf(PLUS, MINUS), MULTIPLICATIVE_EXPRESSION)).skipIfOneChild();
    b.rule(MULTIPLICATIVE_EXPRESSION).is(UNARY_EXPRESSION, b.zeroOrMore(b.firstOf(STAR, DIV, MOD), UNARY_EXPRESSION)).skipIfOneChild();
    b.rule(UNARY_EXPRESSION_NOT_PLUS_MINUS).is(b.firstOf(
        CAST_EXPRESSION,
        METHOD_REFERENCE,
        b.sequence(PRIMARY, b.zeroOrMore(SELECTOR), b.zeroOrMore(POST_FIX_OP)),
        b.sequence(TILDA, UNARY_EXPRESSION),
        b.sequence(BANG, UNARY_EXPRESSION)
    )).skipIfOneChild();
    b.rule(CAST_EXPRESSION).is(LPAR, b.firstOf(
      b.sequence(BASIC_TYPE, RPAR, UNARY_EXPRESSION),
      b.sequence(TYPE, b.zeroOrMore(AND, CLASS_TYPE), RPAR, UNARY_EXPRESSION_NOT_PLUS_MINUS)
    ));
    b.rule(UNARY_EXPRESSION).is(b.firstOf(
        b.sequence(PREFIX_OP, UNARY_EXPRESSION),
        UNARY_EXPRESSION_NOT_PLUS_MINUS
    )).skipIfOneChild();
    b.rule(PRIMARY).is(b.firstOf(
        LAMBDA_EXPRESSION,
        PAR_EXPRESSION,
        b.sequence(NON_WILDCARD_TYPE_ARGUMENTS, b.firstOf(EXPLICIT_GENERIC_INVOCATION_SUFFIX, b.sequence(THIS, ARGUMENTS))),
        b.sequence(THIS, b.optional(ARGUMENTS)),
        b.sequence(SUPER, SUPER_SUFFIX),
        LITERAL,
        b.sequence(NEW, b.zeroOrMore(ANNOTATION), CREATOR),
        b.sequence(QUALIFIED_IDENTIFIER, b.optional(IDENTIFIER_SUFFIX)),
        b.sequence(BASIC_TYPE, b.zeroOrMore(DIM), DOT, CLASS),
        b.sequence(VOID, DOT, CLASS)
        ));

    b.rule(METHOD_REFERENCE).is(b.firstOf(
        b.sequence(SUPER, DBLECOLON),
        b.sequence(TYPE, DBLECOLON),
        b.sequence(PRIMARY, b.zeroOrMore(SELECTOR), DBLECOLON)
        ),
        b.optional(TYPE_ARGUMENTS), b.firstOf(NEW,IDENTIFIER)
    );
    b.rule(IDENTIFIER_SUFFIX).is(b.firstOf(
        b.sequence(LBRK, b.firstOf(b.sequence(RBRK, b.zeroOrMore(DIM), DOT, CLASS), b.sequence(EXPRESSION, RBRK))),
        ARGUMENTS,
        b.sequence(DOT, b.firstOf(
            CLASS,
            EXPLICIT_GENERIC_INVOCATION,
            THIS,
            b.sequence(SUPER, ARGUMENTS),
            b.sequence(NEW, b.optional(NON_WILDCARD_TYPE_ARGUMENTS), INNER_CREATOR)))));
    b.rule(EXPLICIT_GENERIC_INVOCATION).is(NON_WILDCARD_TYPE_ARGUMENTS, EXPLICIT_GENERIC_INVOCATION_SUFFIX);
    b.rule(NON_WILDCARD_TYPE_ARGUMENTS).is(LPOINT, TYPE, b.zeroOrMore(COMMA, TYPE), RPOINT);
    b.rule(EXPLICIT_GENERIC_INVOCATION_SUFFIX).is(b.firstOf(
        b.sequence(SUPER, SUPER_SUFFIX),
        b.sequence(IDENTIFIER, ARGUMENTS)));
    b.rule(PREFIX_OP).is(b.firstOf(
        INC,
        DEC,
        PLUS,
        MINUS));
    b.rule(POST_FIX_OP).is(b.firstOf(
        INC,
        DEC));
    b.rule(SELECTOR).is(b.firstOf(
        b.sequence(DOT, IDENTIFIER, b.optional(ARGUMENTS)),
        b.sequence(DOT, EXPLICIT_GENERIC_INVOCATION),
        b.sequence(DOT, THIS),
        b.sequence(DOT, SUPER, SUPER_SUFFIX),
        b.sequence(DOT, NEW, b.optional(NON_WILDCARD_TYPE_ARGUMENTS), INNER_CREATOR),
        DIM_EXPR));
    b.rule(SUPER_SUFFIX).is(b.firstOf(
        ARGUMENTS,
        b.sequence(DOT, IDENTIFIER, b.optional(ARGUMENTS)),
        b.sequence(DOT, NON_WILDCARD_TYPE_ARGUMENTS, IDENTIFIER, ARGUMENTS)));
    b.rule(BASIC_TYPE).is(b.zeroOrMore(ANNOTATION), b.firstOf(
        BYTE,
        SHORT,
        CHAR,
        INT,
        LONG,
        FLOAT,
        DOUBLE,
        BOOLEAN));
    b.rule(ARGUMENTS).is(LPAR, b.optional(EXPRESSION, b.zeroOrMore(COMMA, EXPRESSION)), RPAR);
    b.rule(CREATOR).is(b.firstOf(
        b.sequence(b.optional(NON_WILDCARD_TYPE_ARGUMENTS), CREATED_NAME, CLASS_CREATOR_REST),
        b.sequence(b.optional(NON_WILDCARD_TYPE_ARGUMENTS), b.firstOf(CLASS_TYPE, BASIC_TYPE), ARRAY_CREATOR_REST)));
    b.rule(CREATED_NAME).is(b.zeroOrMore(ANNOTATION), IDENTIFIER, b.optional(NON_WILDCARD_TYPE_ARGUMENTS),
        b.zeroOrMore(DOT, b.zeroOrMore(ANNOTATION), IDENTIFIER, b.optional(NON_WILDCARD_TYPE_ARGUMENTS)));
    b.rule(INNER_CREATOR).is(IDENTIFIER, CLASS_CREATOR_REST);
    b.rule(ARRAY_CREATOR_REST).is(b.zeroOrMore(ANNOTATION), LBRK, b.firstOf(
        b.sequence(RBRK, b.zeroOrMore(DIM), ARRAY_INITIALIZER),
        b.sequence(EXPRESSION, RBRK, b.zeroOrMore(DIM_EXPR), b.zeroOrMore(b.zeroOrMore(ANNOTATION), DIM))));
    b.rule(CLASS_CREATOR_REST).is(b.optional(b.firstOf(DIAMOND, TYPE_ARGUMENTS)), ARGUMENTS, b.optional(CLASS_BODY));
    b.rule(DIAMOND).is(LT, GT);
    b.rule(ARRAY_INITIALIZER).is(LWING, b.optional(VARIABLE_INITIALIZER, b.zeroOrMore(COMMA, VARIABLE_INITIALIZER)), b.optional(COMMA), RWING);
    b.rule(VARIABLE_INITIALIZER).is(b.firstOf(ARRAY_INITIALIZER, EXPRESSION));
    b.rule(PAR_EXPRESSION).is(LPAR, EXPRESSION, RPAR);
    b.rule(QUALIFIED_IDENTIFIER).is(b.zeroOrMore(ANNOTATION), IDENTIFIER, b.zeroOrMore(DOT, b.zeroOrMore(ANNOTATION), IDENTIFIER));
    b.rule(QUALIFIED_IDENTIFIER_LIST).is(QUALIFIED_IDENTIFIER, b.zeroOrMore(COMMA, QUALIFIED_IDENTIFIER));
    b.rule(DIM).is(LBRK, RBRK);
    b.rule(DIM_EXPR).is(b.zeroOrMore(ANNOTATION), LBRK, EXPRESSION, RBRK);

    //Java 8 lambda expressions.
    b.rule(LAMBDA_EXPRESSION).is(LAMBDA_PARAMETERS, ARROW, LAMBDA_BODY);
    b.rule(LAMBDA_PARAMETERS).is(b.firstOf(
     b.sequence(LPAR, b.optional(IDENTIFIER, b.zeroOrMore(COMMA, IDENTIFIER)), RPAR),
     FORMAL_PARAMETERS,
     IDENTIFIER
    ));
    b.rule(LAMBDA_BODY).is(b.firstOf(BLOCK, EXPRESSION));

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
