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
import org.sonar.java.ast.api.JavaGrammar;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.lexer.FloatLiteralChannel;
import org.sonar.java.ast.lexer.IntegerLiteralChannel;

import static org.sonar.sslr.parser.GrammarOperators.endOfInput;
import static org.sonar.sslr.parser.GrammarOperators.firstOf;
import static org.sonar.sslr.parser.GrammarOperators.next;
import static org.sonar.sslr.parser.GrammarOperators.nextNot;
import static org.sonar.sslr.parser.GrammarOperators.oneOrMore;
import static org.sonar.sslr.parser.GrammarOperators.optional;
import static org.sonar.sslr.parser.GrammarOperators.regexp;
import static org.sonar.sslr.parser.GrammarOperators.sequence;
import static org.sonar.sslr.parser.GrammarOperators.token;
import static org.sonar.sslr.parser.GrammarOperators.zeroOrMore;

public class JavaGrammarImpl extends JavaGrammar {

  public JavaGrammarImpl() {
    punctuators();
    keywords();

    compilationsUnits();
    classDeclaration();
    interfaceDeclarations();
    enums();
    formalParameters();
    blocksAndStatements();
    expressions();
    types();
    annotations();
    literals();
  }

  private void punctuators() {
    at.is(punctuator("@")).skip();
    and.is(punctuator("&", nextNot(firstOf("=", "&")))).skip();
    andand.is(punctuator("&&")).skip();
    andequ.is(punctuator("&=")).skip();
    bang.is(punctuator("!", nextNot("="))).skip();
    bsr.is(punctuator(">>>", nextNot("="))).skip();
    bsrequ.is(punctuator(">>>=")).skip();
    colon.is(punctuator(":")).skip();
    comma.is(punctuator(",")).skip();
    dec.is(punctuator("--")).skip();
    div.is(punctuator("/", nextNot("="))).skip();
    divequ.is(punctuator("/=")).skip();
    dot.is(punctuator(".")).skip();
    ellipsis.is(punctuator("...")).skip();
    equ.is(punctuator("=", nextNot("="))).skip();
    equal.is(punctuator("==")).skip();
    ge.is(punctuator(">=")).skip();
    gt.is(punctuator(">", nextNot(firstOf("=", ">")))).skip();
    hat.is(punctuator("^", nextNot("="))).skip();
    hatequ.is(punctuator("^=")).skip();
    inc.is(punctuator("++")).skip();
    lbrk.is(punctuator("[")).skip();
    lt.is(punctuator("<", nextNot(firstOf("=", "<")))).skip();
    le.is(punctuator("<=")).skip();
    lpar.is(punctuator("(")).skip();
    lwing.is(punctuator("{")).skip();
    minus.is(punctuator("-", nextNot(firstOf("-", "=")))).skip();
    minsequ.is(punctuator("-=")).skip();
    mod.is(punctuator("%", nextNot("="))).skip();
    modequ.is(punctuator("%=")).skip();
    notequal.is(punctuator("!=")).skip();
    or.is(punctuator("|", nextNot(firstOf("=", "|")))).skip();
    orequ.is(punctuator("|=")).skip();
    oror.is(punctuator("||")).skip();
    plus.is(punctuator("+", nextNot(firstOf("=", "+")))).skip();
    plusequ.is(punctuator("+=")).skip();
    query.is(punctuator("?")).skip();
    rbrk.is(punctuator("]")).skip();
    rpar.is(punctuator(")")).skip();
    rwing.is(punctuator("}")).skip();
    semi.is(punctuator(";")).skip();
    sl.is(punctuator("<<", nextNot("="))).skip();
    slequ.is(punctuator("<<=")).skip();
    sr.is(punctuator(">>", nextNot(firstOf("=", ">")))).skip();
    srequ.is(punctuator(">>=")).skip();
    star.is(punctuator("*", nextNot("="))).skip();
    starequ.is(punctuator("*=")).skip();
    tilda.is(punctuator("~")).skip();

    lpoint.is(punctuator("<")).skip();
    rpoint.is(punctuator(">")).skip();
  }

  private void keywords() {
    assertKeyword.is(keyword("assert")).skip();
    breakKeyword.is(keyword("break")).skip();
    caseKeyword.is(keyword("case")).skip();
    catchKeyword.is(keyword("catch")).skip();
    classKeyword.is(keyword("class")).skip();
    continueKeyword.is(keyword("continue")).skip();
    defaultKeyword.is(keyword("default")).skip();
    doKeyword.is(keyword("do")).skip();
    elseKeyword.is(keyword("else")).skip();
    enumKeyword.is(keyword("enum")).skip();
    extendsKeyword.is(keyword("extends")).skip();
    finallyKeyword.is(keyword("finally")).skip();
    finalKeyword.is(keyword("final")).skip();
    forKeyword.is(keyword("for")).skip();
    ifKeyword.is(keyword("if")).skip();
    implementsKeyword.is(keyword("implements")).skip();
    importKeyword.is(keyword("import")).skip();
    interfaceKeyword.is(keyword("interface")).skip();
    instanceofKeyword.is(keyword("instanceof")).skip();
    newKeyword.is(keyword("new")).skip();
    packageKeyword.is(keyword("package")).skip();
    returnKeyword.is(keyword("return")).skip();
    staticKeyword.is(keyword("static")).skip();
    superKeyword.is(keyword("super")).skip();
    switchKeyword.is(keyword("switch")).skip();
    synchronizedKeyword.is(keyword("synchronized")).skip();
    thisKeyword.is(keyword("this")).skip();
    throwsKeyword.is(keyword("throws")).skip();
    throwKeyword.is(keyword("throw")).skip();
    tryKeyword.is(keyword("try")).skip();
    voidKeyword.is(keyword("void")).skip();
    whileKeyword.is(keyword("while")).skip();
    trueKeyword.is(keyword("true")).skip();
    falseKeyword.is(keyword("false")).skip();
    nullKeyword.is(keyword("null")).skip();
    publicKeyword.is(keyword("public")).skip();
    protectedKeyword.is(keyword("protected")).skip();
    privateKeyword.is(keyword("private")).skip();
    abstractKeyword.is(keyword("abstract")).skip();
    nativeKeyword.is(keyword("native")).skip();
    transientKeyword.is(keyword("transient")).skip();
    volatileKeyword.is(keyword("volatile")).skip();
    strictfpKeyword.is(keyword("strictfp")).skip();
    byteKeyword.is(keyword("byte")).skip();
    shortKeyword.is(keyword("short")).skip();
    charKeyword.is(keyword("char")).skip();
    intKeyword.is(keyword("int")).skip();
    longKeyword.is(keyword("long")).skip();
    floatKeyword.is(keyword("float")).skip();
    doubleKeyword.is(keyword("double")).skip();
    booleanKeyword.is(keyword("boolean")).skip();
  }

  private Object keyword(String value) {
    for (JavaKeyword tokenType : JavaKeyword.values()) {
      if (value.equals(tokenType.getValue())) {
        return sequence(token(tokenType, value), nextNot(letterOrDigit), spacing);
      }
    }
    throw new IllegalStateException(value);
  }

  private Object punctuator(String value) {
    for (JavaPunctuator tokenType : JavaPunctuator.values()) {
      if (value.equals(tokenType.getValue())) {
        return sequence(token(tokenType, value), spacing);
      }
    }
    return sequence(token(JavaTokenType.SPECIAL, value), spacing);
  }

  private Object punctuator(String value, Object element) {
    for (JavaPunctuator tokenType : JavaPunctuator.values()) {
      if (value.equals(tokenType.getValue())) {
        return sequence(token(tokenType, value), element, spacing);
      }
    }
    return sequence(token(JavaTokenType.SPECIAL, value), element, spacing);
  }

  /**
   * 3.10. Literals
   */
  private void literals() {
    spacing.is(
        whitespace(),
        zeroOrMore(
            token(GenericTokenType.COMMENT, firstOf(inlineComment(), multilineComment())),
            whitespace())).skip();

    eof.is(token(GenericTokenType.EOF, endOfInput())).skip();

    characterLiteral.is(token(JavaTokenType.CHARACTER_LITERAL, characterLiteral()), spacing).skip();
    stringLiteral.is(token(GenericTokenType.LITERAL, stringLiteral()), spacing).skip();

    floatLiteral.is(token(JavaTokenType.FLOAT_LITERAL, regexp(FloatLiteralChannel.FLOATING_LITERAL_WITHOUT_SUFFIX + "[fF]|[0-9][0-9_]*+[fF]")), spacing).skip();
    doubleLiteral.is(token(JavaTokenType.DOUBLE_LITERAL, regexp(FloatLiteralChannel.FLOATING_LITERAL_WITHOUT_SUFFIX + "[dD]?+|[0-9][0-9_]*+[dD]")), spacing).skip();

    longLiteral.is(token(JavaTokenType.LONG_LITERAL, regexp(IntegerLiteralChannel.INTEGER_LITERAL + "[lL]")), spacing).skip();
    integerLiteral.is(token(JavaTokenType.INTEGER_LITERAL, regexp(IntegerLiteralChannel.INTEGER_LITERAL)), spacing).skip();

    keyword.is(firstOf("assert", "break", "case", "catch", "class", "const", "continue", "default", "do", "else",
        "enum", "extends", "finally", "final", "for", "goto", "if", "implements", "import", "interface",
        "instanceof", "new", "package", "return", "static", "super", "switch", "synchronized", "this",
        "throws", "throw", "try", "void", "while"
        ), nextNot(letterOrDigit));
    letterOrDigit.is(javaIdentifierPart());
    identifier.is(nextNot(keyword), token(GenericTokenType.IDENTIFIER, javaIdentifier()), spacing).skip();

    literal.is(firstOf(
        trueKeyword,
        falseKeyword,
        nullKeyword,
        characterLiteral,
        stringLiteral,
        floatLiteral,
        doubleLiteral,
        longLiteral,
        integerLiteral));
  }

  private Object characterLiteral() {
    return sequence(next("'"), regexp("'([^'\\\\]*+(\\\\[\\s\\S])?+)*+'"));
  }

  private Object stringLiteral() {
    return sequence(next("\""), regexp("\"([^\"\\\\]*+(\\\\[\\s\\S])?+)*+\""));
  }

  private Object whitespace() {
    return regexp("\\s*+");
  }

  private Object inlineComment() {
    return regexp("//[^\\n\\r]*+");
  }

  private Object multilineComment() {
    return regexp("/\\*[\\s\\S]*?\\*\\/");
  }

  private Object javaIdentifier() {
    return regexp("\\p{javaJavaIdentifierStart}++\\p{javaJavaIdentifierPart}*+");
  }

  private Object javaIdentifierPart() {
    return regexp("\\p{javaJavaIdentifierPart}");
  }

  /**
   * 4. Types, Values and Variables
   */
  private void types() {
    type.is(firstOf(basicType, classType), zeroOrMore(dim));
    referenceType.is(firstOf(
        sequence(basicType, zeroOrMore(dim)),
        sequence(classType, zeroOrMore(dim))));
    classType.is(identifier, optional(typeArguments), zeroOrMore(dot, identifier, optional(typeArguments)));
    classTypeList.is(classType, zeroOrMore(comma, classType));
    typeArguments.is(lpoint, typeArgument, zeroOrMore(comma, typeArgument), rpoint);
    typeArgument.is(firstOf(
        referenceType,
        sequence(query, optional(firstOf(extendsKeyword, superKeyword), referenceType))));
    typeParameters.is(lpoint, typeParameter, zeroOrMore(comma, typeParameter), rpoint);
    typeParameter.is(identifier, optional(extendsKeyword, bound));
    bound.is(classType, zeroOrMore(and, classType));
    modifier.is(firstOf(
        annotation,
        publicKeyword,
        protectedKeyword,
        privateKeyword,
        staticKeyword,
        abstractKeyword,
        finalKeyword,
        nativeKeyword,
        synchronizedKeyword,
        transientKeyword,
        volatileKeyword,
        strictfpKeyword));
  }

  /**
   * 7.3. Compilation Units
   */
  private void compilationsUnits() {
    compilationUnit.is(spacing, optional(packageDeclaration), zeroOrMore(importDeclaration), zeroOrMore(typeDeclaration), eof);

    packageDeclaration.is(zeroOrMore(annotation), packageKeyword, qualifiedIdentifier, semi);
    importDeclaration.is(importKeyword, optional(staticKeyword), qualifiedIdentifier, optional(dot, star), semi);
    typeDeclaration.is(firstOf(
        sequence(zeroOrMore(modifier), firstOf(classDeclaration, enumDeclaration, interfaceDeclaration, annotationTypeDeclaration)),
        semi));
  }

  /**
   * 8.1. Class Declaration
   */
  private void classDeclaration() {
    classDeclaration.is(classKeyword, identifier, optional(typeParameters), optional(extendsKeyword, classType), optional(implementsKeyword, classTypeList), classBody);

    classBody.is(lwing, zeroOrMore(classBodyDeclaration), rwing);
    classBodyDeclaration.is(firstOf(
        semi,
        classInitDeclaration,
        sequence(zeroOrMore(modifier), memberDecl)));
    classInitDeclaration.is(optional(staticKeyword), block);
    memberDecl.is(firstOf(
        sequence(typeParameters, genericMethodOrConstructorRest),
        sequence(type, identifier, methodDeclaratorRest),
        fieldDeclaration,
        sequence(voidKeyword, identifier, voidMethodDeclaratorRest),
        sequence(identifier, constructorDeclaratorRest),
        interfaceDeclaration,
        classDeclaration,
        enumDeclaration,
        annotationTypeDeclaration));
    fieldDeclaration.is(type, variableDeclarators, semi);
    genericMethodOrConstructorRest.is(firstOf(
        sequence(firstOf(type, voidKeyword), identifier, methodDeclaratorRest),
        sequence(identifier, constructorDeclaratorRest)));
    methodDeclaratorRest.is(formalParameters, zeroOrMore(dim), optional(throwsKeyword, classTypeList), firstOf(methodBody, semi));
    voidMethodDeclaratorRest.is(formalParameters, optional(throwsKeyword, classTypeList), firstOf(methodBody, semi));
    constructorDeclaratorRest.is(formalParameters, optional(throwsKeyword, classTypeList), methodBody);
    methodBody.is(block);
  }

  /**
   * 8.9. Enums
   */
  private void enums() {
    enumDeclaration.is(enumKeyword, identifier, optional(implementsKeyword, classTypeList), enumBody);
    enumBody.is(lwing, optional(enumConstants), optional(comma), optional(enumBodyDeclarations), rwing);
    enumConstants.is(enumConstant, zeroOrMore(comma, enumConstant));
    enumConstant.is(zeroOrMore(annotation), identifier, optional(arguments), optional(classBody));
    enumBodyDeclarations.is(semi, zeroOrMore(classBodyDeclaration));
  }

  /**
   * 9.1. Interface Declarations
   */
  private void interfaceDeclarations() {
    interfaceDeclaration.is(interfaceKeyword, identifier, optional(typeParameters), optional(extendsKeyword, classTypeList), interfaceBody);

    interfaceBody.is(lwing, zeroOrMore(interfaceBodyDeclaration), rwing);
    interfaceBodyDeclaration.is(firstOf(
        sequence(zeroOrMore(modifier), interfaceMemberDecl),
        semi));
    interfaceMemberDecl.is(firstOf(
        interfaceMethodOrFieldDecl,
        interfaceGenericMethodDecl,
        sequence(voidKeyword, identifier, voidInterfaceMethodDeclaratorsRest),
        interfaceDeclaration,
        annotationTypeDeclaration,
        classDeclaration,
        enumDeclaration));
    interfaceMethodOrFieldDecl.is(type, identifier, interfaceMethodOrFieldRest);
    interfaceMethodOrFieldRest.is(firstOf(
        sequence(constantDeclaratorsRest, semi),
        interfaceMethodDeclaratorRest));
    interfaceMethodDeclaratorRest.is(formalParameters, zeroOrMore(dim), optional(throwsKeyword, classTypeList), semi);
    interfaceGenericMethodDecl.is(typeParameters, firstOf(type, voidKeyword), identifier, interfaceMethodDeclaratorRest);
    voidInterfaceMethodDeclaratorsRest.is(formalParameters, optional(throwsKeyword, classTypeList), semi);
    constantDeclaratorsRest.is(constantDeclaratorRest, zeroOrMore(comma, constantDeclarator));
    constantDeclarator.is(identifier, constantDeclaratorRest);
    constantDeclaratorRest.is(zeroOrMore(dim), equ, variableInitializer);
  }

  /**
   * 8.4.1. Formal Parameters
   */
  private void formalParameters() {
    formalParameters.is(lpar, optional(formalParameterDecls), rpar);
    formalParameter.is(zeroOrMore(firstOf(finalKeyword, annotation)), type, variableDeclaratorId);
    formalParameterDecls.is(zeroOrMore(firstOf(finalKeyword, annotation)), type, formalParametersDeclsRest);
    formalParametersDeclsRest.is(firstOf(
        sequence(variableDeclaratorId, optional(comma, formalParameterDecls)),
        sequence(ellipsis, variableDeclaratorId)));
    variableDeclaratorId.is(identifier, zeroOrMore(dim));
  }

  /**
   * 9.7. Annotations
   */
  private void annotations() {
    annotationTypeDeclaration.is(at, interfaceKeyword, identifier, annotationTypeBody);
    annotationTypeBody.is(lwing, zeroOrMore(annotationTypeElementDeclaration), rwing);
    annotationTypeElementDeclaration.is(firstOf(
        sequence(zeroOrMore(modifier), annotationTypeElementRest),
        semi));
    annotationTypeElementRest.is(firstOf(
        sequence(type, annotationMethodOrConstantRest, semi),
        classDeclaration,
        enumDeclaration,
        interfaceDeclaration,
        annotationTypeDeclaration));
    annotationMethodOrConstantRest.is(firstOf(
        annotationMethodRest,
        annotationConstantRest));
    annotationMethodRest.is(identifier, lpar, rpar, optional(defaultValue));
    annotationConstantRest.is(variableDeclarators);
    defaultValue.is(defaultKeyword, elementValue);
    annotation.is(at, qualifiedIdentifier, optional(annotationRest));
    annotationRest.is(firstOf(
        normalAnnotationRest,
        singleElementAnnotationRest));
    normalAnnotationRest.is(lpar, optional(elementValuePairs), rpar);
    elementValuePairs.is(elementValuePair, zeroOrMore(comma, elementValuePair));
    elementValuePair.is(identifier, equ, elementValue);
    elementValue.is(firstOf(
        conditionalExpression,
        annotation,
        elementValueArrayInitializer));
    elementValueArrayInitializer.is(lwing, optional(elementValues), optional(comma), rwing);
    elementValues.is(elementValue, zeroOrMore(comma, elementValue));
    singleElementAnnotationRest.is(lpar, elementValue, rpar);
  }

  /**
   * 14. Blocks and Statements
   */
  private void blocksAndStatements() {
    // 14.2. Blocks
    block.is(lwing, blockStatements, rwing);
    blockStatements.is(zeroOrMore(blockStatement));
    blockStatement.is(firstOf(
        localVariableDeclarationStatement,
        sequence(zeroOrMore(modifier), firstOf(classDeclaration, enumDeclaration)),
        statement));

    // 14.4. Local Variable Declaration Statements
    localVariableDeclarationStatement.is(variableModifiers, type, variableDeclarators, semi);
    variableModifiers.is(zeroOrMore(firstOf(
        annotation,
        finalKeyword)));
    variableDeclarators.is(variableDeclarator, zeroOrMore(comma, variableDeclarator));
    variableDeclarator.is(identifier, zeroOrMore(dim), optional(equ, variableInitializer));

    // 14.5. Statements
    statement.is(firstOf(
        block,
        assertStatement,
        ifStatement,
        forStatement,
        whileStatement,
        doStatement,
        tryStatement,
        switchStatement,
        synchronizedStatement,
        returnStatement,
        throwStatement,
        breakStatement,
        continueStatement,
        labeledStatement,
        expressionStatement,
        emptyStatement));

    // 14.6. The Empty Statement
    emptyStatement.is(semi);
    // 14.7. Labeled Statements
    labeledStatement.is(identifier, colon, statement);
    // 14.8. Expression Statements
    expressionStatement.is(statementExpression, semi);
    // 14.9. The if Statement
    ifStatement.is(ifKeyword, parExpression, statement, optional(elseKeyword, statement));
    // 14.10. The assert Statement
    assertStatement.is(assertKeyword, expression, optional(colon, expression), semi);

    // 14.11. The switch statement
    switchStatement.is(switchKeyword, parExpression, lwing, switchBlockStatementGroups, rwing);
    switchBlockStatementGroups.is(zeroOrMore(switchBlockStatementGroup));
    switchBlockStatementGroup.is(switchLabel, blockStatements);
    switchLabel.is(firstOf(
        sequence(caseKeyword, constantExpression, colon),
        sequence(caseKeyword, enumConstantName, colon),
        sequence(defaultKeyword, colon)));
    enumConstantName.is(identifier);

    // 14.12. The while Statement
    whileStatement.is(whileKeyword, parExpression, statement);
    // 14.13. The do Statement
    doStatement.is(doKeyword, statement, whileKeyword, parExpression, semi);

    // 14.14. The for Statement
    forStatement.is(firstOf(
        sequence(forKeyword, lpar, optional(forInit), semi, optional(expression), semi, optional(forUpdate), rpar, statement),
        sequence(forKeyword, lpar, formalParameter, colon, expression, rpar, statement)));
    forInit.is(firstOf(
        sequence(zeroOrMore(firstOf(finalKeyword, annotation)), type, variableDeclarators),
        sequence(statementExpression, zeroOrMore(comma, statementExpression))));
    forUpdate.is(statementExpression, zeroOrMore(comma, statementExpression));

    // 14.15. The break Statement
    breakStatement.is(breakKeyword, optional(identifier), semi);
    // 14.16. The continue Statement
    continueStatement.is(continueKeyword, optional(identifier), semi);
    // 14.17. The return Statement
    returnStatement.is(returnKeyword, optional(expression), semi);
    // 14.18. The throw Statement
    throwStatement.is(throwKeyword, expression, semi);
    // 14.19. The synchronized Statement
    synchronizedStatement.is(synchronizedKeyword, parExpression, block);

    // 14.20. The try Statement
    tryStatement.is(firstOf(
        sequence(tryKeyword, block, firstOf(sequence(oneOrMore(catchClause), optional(finally_)), finally_)),
        tryWithResourcesStatement));
    tryWithResourcesStatement.is(tryKeyword, resourceSpecification, block, zeroOrMore(catchClause), optional(finally_));
    resourceSpecification.is(lpar, resource, zeroOrMore(semi, resource), optional(semi), rpar);
    resource.is(optional(variableModifiers), type, variableDeclaratorId, equ, expression);

    catchClause.is(catchKeyword, lpar, catchFormalParameter, rpar, block);
    catchFormalParameter.is(optional(variableModifiers), catchType, variableDeclaratorId);
    catchType.is(classType, zeroOrMore(or, classType));

    finally_.is(finallyKeyword, block);
  }

  /**
   * 15. Expressions
   */
  private void expressions() {
    statementExpression.is(expression);
    constantExpression.is(expression);
    expression.is(assignmentExpression);
    assignmentExpression.is(conditionalExpression, zeroOrMore(assignmentOperator, conditionalExpression)).skipIfOneChild();
    assignmentOperator.is(firstOf(
        equ,
        plusequ,
        minsequ,
        starequ,
        divequ,
        andequ,
        orequ,
        hatequ,
        modequ,
        slequ,
        srequ,
        bsrequ));
    conditionalExpression.is(conditionalOrExpression, zeroOrMore(query, expression, colon, conditionalOrExpression)).skipIfOneChild();
    conditionalOrExpression.is(conditionalAndExpression, zeroOrMore(oror, conditionalAndExpression)).skipIfOneChild();
    conditionalAndExpression.is(inclusiveOrExpression, zeroOrMore(andand, inclusiveOrExpression)).skipIfOneChild();
    inclusiveOrExpression.is(exclusiveOrExpression, zeroOrMore(or, exclusiveOrExpression)).skipIfOneChild();
    exclusiveOrExpression.is(andExpression, zeroOrMore(hat, andExpression)).skipIfOneChild();
    andExpression.is(equalityExpression, zeroOrMore(and, equalityExpression)).skipIfOneChild();
    equalityExpression.is(relationalExpression, zeroOrMore(firstOf(equal, notequal), relationalExpression)).skipIfOneChild();
    relationalExpression.is(shiftExpression, zeroOrMore(firstOf(
        sequence(firstOf(ge, gt, le, lt), shiftExpression),
        sequence(instanceofKeyword, referenceType)))).skipIfOneChild();
    shiftExpression.is(additiveExpression, zeroOrMore(firstOf(sl, bsr, sr), additiveExpression)).skipIfOneChild();
    additiveExpression.is(multiplicativeExpression, zeroOrMore(firstOf(plus, minus), multiplicativeExpression)).skipIfOneChild();
    multiplicativeExpression.is(unaryExpression, zeroOrMore(firstOf(star, div, mod), unaryExpression)).skipIfOneChild();
    unaryExpression.is(firstOf(
        sequence(prefixOp, unaryExpression),
        sequence(lpar, type, rpar, unaryExpression),
        sequence(primary, zeroOrMore(selector), zeroOrMore(postFixOp)))).skipIfOneChild();
    primary.is(firstOf(
        parExpression,
        sequence(nonWildcardTypeArguments, firstOf(explicitGenericInvocationSuffix, sequence(thisKeyword, arguments))),
        sequence(thisKeyword, optional(arguments)),
        sequence(superKeyword, superSuffix),
        literal,
        sequence(newKeyword, creator),
        sequence(qualifiedIdentifier, optional(identifierSuffix)),
        sequence(basicType, zeroOrMore(dim), dot, classKeyword),
        sequence(voidKeyword, dot, classKeyword)));
    identifierSuffix.is(firstOf(
        sequence(lbrk, firstOf(sequence(rbrk, zeroOrMore(dim), dot, classKeyword), sequence(expression, rbrk))),
        arguments,
        sequence(dot, firstOf(
            classKeyword,
            explicitGenericInvocation,
            thisKeyword,
            sequence(superKeyword, arguments),
            sequence(newKeyword, optional(nonWildcardTypeArguments), innerCreator)))));
    explicitGenericInvocation.is(nonWildcardTypeArguments, explicitGenericInvocationSuffix);
    nonWildcardTypeArguments.is(lpoint, referenceType, zeroOrMore(comma, referenceType), rpoint);
    explicitGenericInvocationSuffix.is(firstOf(
        sequence(superKeyword, superSuffix),
        sequence(identifier, arguments)));
    prefixOp.is(firstOf(
        inc,
        dec,
        bang,
        tilda,
        plus,
        minus));
    postFixOp.is(firstOf(
        inc,
        dec));
    selector.is(firstOf(
        sequence(dot, identifier, optional(arguments)),
        sequence(dot, explicitGenericInvocation),
        sequence(dot, thisKeyword),
        sequence(dot, superKeyword, superSuffix),
        sequence(dot, newKeyword, optional(nonWildcardTypeArguments), innerCreator),
        dimExpr));
    superSuffix.is(firstOf(
        arguments,
        sequence(dot, identifier, optional(arguments))));
    basicType.is(firstOf(
        byteKeyword,
        shortKeyword,
        charKeyword,
        intKeyword,
        longKeyword,
        floatKeyword,
        doubleKeyword,
        booleanKeyword));
    arguments.is(lpar, optional(expression, zeroOrMore(comma, expression)), rpar);
    creator.is(firstOf(
        sequence(optional(nonWildcardTypeArguments), createdName, classCreatorRest),
        sequence(optional(nonWildcardTypeArguments), firstOf(classType, basicType), arrayCreatorRest)));
    createdName.is(identifier, optional(nonWildcardTypeArguments), zeroOrMore(dot, identifier, optional(nonWildcardTypeArguments)));
    innerCreator.is(identifier, classCreatorRest);
    arrayCreatorRest.is(lbrk, firstOf(
        sequence(rbrk, zeroOrMore(dim), arrayInitializer),
        sequence(expression, rbrk, zeroOrMore(dimExpr), zeroOrMore(dim))));
    classCreatorRest.is(optional(diamond), arguments, optional(classBody));
    diamond.is(lt, gt);
    arrayInitializer.is(lwing, optional(variableInitializer, zeroOrMore(comma, variableInitializer)), optional(comma), rwing);
    variableInitializer.is(firstOf(arrayInitializer, expression));
    parExpression.is(lpar, expression, rpar);
    qualifiedIdentifier.is(identifier, zeroOrMore(dot, identifier));
    dim.is(lbrk, rbrk);
    dimExpr.is(lbrk, expression, rbrk);
  }

}
