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
package org.sonar.java.resolve;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.java.ast.visitors.JavaAstVisitor;

import java.util.List;
import java.util.Map;

/**
 * Computes types of expressions.
 * TODO compute type of method calls
 */
public class ExpressionVisitor extends JavaAstVisitor {

  private final Map<AstNodeType, Type> typesOfLiterals = Maps.newHashMap();

  private final AstNodeType[] binaryOperatorAstNodeTypes;

  private final SemanticModel semanticModel;
  private final Symbols symbols;
  private final Resolve resolve;

  private final Map<AstNode, Type> types = Maps.newHashMap();

  public ExpressionVisitor(SemanticModel semanticModel, Symbols symbols, Resolve resolve) {
    this.semanticModel = semanticModel;
    this.symbols = symbols;
    this.resolve = resolve;

    typesOfLiterals.put(JavaKeyword.TRUE, symbols.booleanType);
    typesOfLiterals.put(JavaKeyword.FALSE, symbols.booleanType);
    typesOfLiterals.put(JavaKeyword.NULL, symbols.nullType);
    typesOfLiterals.put(JavaTokenType.CHARACTER_LITERAL, symbols.charType);
    typesOfLiterals.put(JavaTokenType.LITERAL, symbols.stringType);
    typesOfLiterals.put(JavaTokenType.FLOAT_LITERAL, symbols.floatType);
    typesOfLiterals.put(JavaTokenType.DOUBLE_LITERAL, symbols.doubleType);
    typesOfLiterals.put(JavaTokenType.LONG_LITERAL, symbols.longType);
    typesOfLiterals.put(JavaTokenType.INTEGER_LITERAL, symbols.intType);

    binaryOperatorAstNodeTypes = new AstNodeType[]{
      JavaGrammar.MULTIPLICATIVE_EXPRESSION,
      JavaGrammar.ADDITIVE_EXPRESSION,
      JavaGrammar.SHIFT_EXPRESSION,
      JavaGrammar.RELATIONAL_EXPRESSION,
      JavaGrammar.EQUALITY_EXPRESSION,
      JavaGrammar.AND_EXPRESSION,
      JavaGrammar.EXCLUSIVE_OR_EXPRESSION,
      JavaGrammar.INCLUSIVE_OR_EXPRESSION,
      JavaGrammar.CONDITIONAL_AND_EXPRESSION,
      JavaGrammar.CONDITIONAL_OR_EXPRESSION
    };
  }

  @Override
  public void init() {
    subscribeTo(
      JavaGrammar.EXPRESSION,
      JavaGrammar.PRIMARY,
      JavaGrammar.UNARY_EXPRESSION,
      JavaGrammar.UNARY_EXPRESSION_NOT_PLUS_MINUS,
      JavaGrammar.CAST_EXPRESSION,
      JavaGrammar.LITERAL,
      JavaGrammar.TYPE);
    subscribeTo(binaryOperatorAstNodeTypes);
    subscribeTo(JavaGrammar.CONDITIONAL_EXPRESSION);
    subscribeTo(JavaGrammar.ASSIGNMENT_EXPRESSION);
  }

  @Override
  public void leaveNode(AstNode astNode) {
    Resolve.Env env = semanticModel.getEnv(astNode);
    final Type type;
    if (astNode.is(JavaGrammar.EXPRESSION)) {
      type = visitExpression(astNode);
    } else if (astNode.is(JavaGrammar.PRIMARY)) {
      type = visitPrimary(env, astNode);
    } else if (astNode.is(JavaGrammar.UNARY_EXPRESSION)) {
      type = visitUnaryExpression(env, astNode);
    } else if (astNode.is(JavaGrammar.LITERAL)) {
      type = visitLiteral(astNode);
    } else if (astNode.is(JavaGrammar.TYPE)) {
      type = visitType(env, astNode);
    } else if (astNode.is(binaryOperatorAstNodeTypes)) {
      type = visitBinaryOperation(env, astNode);
    } else if (astNode.is(JavaGrammar.CONDITIONAL_EXPRESSION)) {
      type = visitConditionalExpression();
    } else if (astNode.is(JavaGrammar.ASSIGNMENT_EXPRESSION)) {
      type = visitAssignmentExpression(astNode);
    } else if(astNode.is(JavaGrammar.CAST_EXPRESSION)) {
      type = visitCastExpression(env, astNode);
    } else if(astNode.is(JavaGrammar.UNARY_EXPRESSION_NOT_PLUS_MINUS)) {
      type = visitUnaryNotPlusMinusExpression(env, astNode);
    } else {
      throw new IllegalArgumentException("Unexpected AstNodeType: " + astNode.getType());
    }
    types.put(astNode, type);
  }

  private Type visitUnaryNotPlusMinusExpression(Resolve.Env env, AstNode astNode) {
    Type result;
    AstNode grandChild = astNode.getFirstChild();
    if (grandChild.is(JavaGrammar.PRIMARY)) {
      Type type = getType(grandChild);
      for (AstNode selectorNode : astNode.getChildren(JavaGrammar.SELECTOR)) {
        type = applySelector(env, type, selectorNode);
      }
      result = type;
    } else {
      result = getType(astNode.getFirstChild(JavaPunctuator.BANG, JavaPunctuator.TILDA).getNextSibling());
    }
    return result;
  }

  private Type visitCastExpression(Resolve.Env env,  AstNode astNode) {
    Type result;
    AstNode type = astNode.getFirstChild(JavaPunctuator.LPAR).getNextSibling();
    if(type.is(JavaGrammar.BASIC_TYPE)) {
      result = resolve.findIdent(env, type.getTokenValue(), Symbol.TYP).type;
    } else {
      result = getType(astNode.getFirstChild(JavaGrammar.TYPE));
    }
    return result;
  }

  /**
   * Computes type of literal.
   */
  private Type visitLiteral(AstNode astNode) {
    astNode = astNode.getFirstChild();
    Type result = typesOfLiterals.get(astNode.getType());
    return Preconditions.checkNotNull(result, "Unexpected AstNodeType: " + astNode.getType());
  }

  private Type visitType(Resolve.Env env, AstNode astNode) {
    final Type result;
    AstNode firstChildNode = astNode.getFirstChild();
    if (firstChildNode.is(JavaGrammar.BASIC_TYPE)) {
      result = resolve.findIdent(env, firstChildNode.getFirstChild().getTokenValue(), Symbol.TYP).type;
    } else if (firstChildNode.is(JavaGrammar.CLASS_TYPE)) {
      result = resolveType(env, firstChildNode);
    } else {
      throw new IllegalArgumentException("Unexpected AstNodeType: " + firstChildNode.getType());
    }
    return result;
  }

  /**
   * Computes type of primary.
   */
  private Type visitPrimary(Resolve.Env env, AstNode astNode) {
    final Type result;
    AstNode firstChildNode = astNode.getFirstChild();
    if (firstChildNode.is(JavaGrammar.LITERAL)) {
      result = getType(firstChildNode);
    } else if (firstChildNode.is(JavaKeyword.THIS)) {
      if (astNode.hasDirectChildren(JavaGrammar.ARGUMENTS)) {
        // this(arguments)
        result = symbols.unknownType;
      } else {
        // this
        result = getTypeOfSymbol(resolve.findIdent(env, "this", Symbol.VAR));
      }
    } else if (firstChildNode.is(JavaKeyword.SUPER)) {
      AstNode superSuffixNode = astNode.getFirstChild(JavaGrammar.SUPER_SUFFIX);
      if (superSuffixNode.hasDirectChildren(JavaGrammar.ARGUMENTS)) {
        // super(arguments)
        // super.method(arguments)
        // super.<T>method(arguments)
        result = symbols.unknownType;
      } else {
        // super.field
        Type type = getTypeOfSymbol(resolve.findIdent(env, "super", Symbol.VAR));
        AstNode identifierNode = superSuffixNode.getFirstChild(JavaTokenType.IDENTIFIER);
        Symbol symbol = resolve.findIdentInType(env, type.symbol, identifierNode.getTokenValue(), Symbol.VAR);
        associateReference(identifierNode, symbol);
        result = getTypeOfSymbol(symbol);
      }
    } else if (firstChildNode.is(JavaGrammar.PAR_EXPRESSION)) {
      // (expression)
      result = getType(firstChildNode.getFirstChild(JavaGrammar.EXPRESSION));
    } else if (firstChildNode.is(JavaKeyword.NEW)) {
      // new...
      result = visitCreator(env, astNode.getFirstChild(JavaGrammar.CREATOR));
    } else if (firstChildNode.is(JavaGrammar.QUALIFIED_IDENTIFIER)) {
      AstNode identifierSuffixNode = astNode.getFirstChild(JavaGrammar.IDENTIFIER_SUFFIX);
      if (identifierSuffixNode == null) {
        // id
        result = resolveQualifiedIdentifier(env, firstChildNode);
      } else {
        if (identifierSuffixNode.getFirstChild().is(JavaPunctuator.LBRK)) {
          if (identifierSuffixNode.hasDirectChildren(JavaKeyword.CLASS)) {
            // id[].class
            // resolve qualified identifier, but discard result
            resolveQualifiedIdentifier(env, firstChildNode);
            result = symbols.classType;
          } else {
            // id[expression]
            Type type = resolveQualifiedIdentifier(env, firstChildNode);
            // TODO get rid of "instanceof"
            // if not array, then return errorType instead of unknownType
            result = type instanceof Type.ArrayType ? ((Type.ArrayType) type).elementType : symbols.unknownType;
          }
        } else if (identifierSuffixNode.getFirstChild().is(JavaGrammar.ARGUMENTS)) {
          // id(arguments)
          result = resolveMethod(env, astNode);
        } else if (identifierSuffixNode.getFirstChild().is(JavaPunctuator.DOT)) {
          Type type = resolveQualifiedIdentifier(env, firstChildNode);
          if (identifierSuffixNode.hasDirectChildren(JavaKeyword.CLASS)) {
            // id.class
            result = symbols.classType;
          } else if (identifierSuffixNode.hasDirectChildren(JavaGrammar.EXPLICIT_GENERIC_INVOCATION)) {
            // id.<...>...
            result = symbols.unknownType;
          } else if (identifierSuffixNode.hasDirectChildren(JavaKeyword.THIS)) {
            // id.this
            result = type;
          } else if (identifierSuffixNode.hasDirectChildren(JavaKeyword.SUPER)) {
            // id.super(arguments)
            result = symbols.unknownType;
          } else if (identifierSuffixNode.hasDirectChildren(JavaKeyword.NEW)) {
            // id.new...
            result = symbols.unknownType;
          } else {
            throw new IllegalArgumentException("Unexpected AstNodeType: " + identifierSuffixNode.getChild(1));
          }
        } else {
          throw new IllegalArgumentException("Unexpected AstNodeType: " + identifierSuffixNode.getFirstChild());
        }
      }
    } else if (firstChildNode.is(JavaGrammar.BASIC_TYPE)) {
      // int.class
      // int[].class
      result = symbols.classType;
    } else if (firstChildNode.is(JavaKeyword.VOID)) {
      // void.class
      result = symbols.classType;
    } else if (firstChildNode.is(JavaGrammar.LAMBDA_EXPRESSION)) {
      //TODO implement symbol for lambda
      result = symbols.unknownType;
    } else {
      throw new IllegalArgumentException("Unexpected AstNodeType: " + firstChildNode.getType());
    }
    return result;
  }

  private Type visitCreator(Resolve.Env env, AstNode astNode) {
    // TODO handle NON_WILDCARD_TYPE_ARGUMENTS
    final Type result;
    if (astNode.hasDirectChildren(JavaGrammar.ARRAY_CREATOR_REST)) {
      Type type = getType(astNode.getFirstChild(JavaGrammar.CLASS_TYPE, JavaGrammar.BASIC_TYPE));
      astNode = astNode.getFirstChild(JavaGrammar.ARRAY_CREATOR_REST);
      int dimensions = astNode.getChildren(JavaPunctuator.LBRK, JavaGrammar.DIM, JavaGrammar.DIM_EXPR).size();
      for (int i = 0; i < dimensions; i++) {
        type = new Type.ArrayType(type, symbols.arrayClass);
      }
      result = type;
    } else if (astNode.hasDirectChildren(JavaGrammar.CLASS_CREATOR_REST)) {
      if (astNode.getFirstChild(JavaGrammar.CLASS_CREATOR_REST).hasDirectChildren(JavaGrammar.CLASS_BODY)) {
        // Anonymous Class
        // TODO type of anonymous class can be obtained from symbol, which is stored in semanticModel
        result = symbols.unknownType;
      } else {
        astNode = astNode.getFirstChild(JavaGrammar.CREATED_NAME);
        result = resolveType(env, astNode);
      }
    } else {
      throw new IllegalArgumentException("Unexpected AstNodeType: " + astNode.getType());
    }
    return result;
  }

  /**
   * Computes type of unary expression.
   */
  private Type visitUnaryExpression(Resolve.Env env, AstNode astNode) {
    final Type result;
    AstNode firstChildNode = astNode.getFirstChild();
    if (firstChildNode.is(JavaGrammar.CAST_EXPRESSION)) {
      // type cast
      result = getType(firstChildNode);
    } else if (firstChildNode.is(JavaGrammar.PREFIX_OP)) {
      result = getType(firstChildNode.getNextSibling());
    } else if(firstChildNode.is(JavaGrammar.UNARY_EXPRESSION_NOT_PLUS_MINUS)){
      result = getType(firstChildNode);
    }else{
      throw new IllegalArgumentException("Unexpected AstNodeType: " + firstChildNode.getType());
    }
    return result;
  }

  private Type applySelector(Resolve.Env env, Type type, AstNode astNode) {
    Preconditions.checkArgument(astNode.is(JavaGrammar.SELECTOR));
    final Type result;
    if (type == symbols.unknownType || /* TODO avoid null */ type == null) {
      return symbols.unknownType;
    } else if (astNode.getFirstChild().is(JavaGrammar.DIM_EXPR)) {
      // array access
      // TODO get rid of "instanceof"
      // if not array, then return errorType instead of unknownType
      result = type instanceof Type.ArrayType ? ((Type.ArrayType) type).elementType : symbols.unknownType;
    } else if (astNode.hasDirectChildren(JavaTokenType.IDENTIFIER)) {
      if (astNode.hasDirectChildren(JavaGrammar.ARGUMENTS)) {
        // method call
        result = symbols.unknownType;
      } else {
        // field access
        AstNode identifierNode = astNode.getFirstChild(JavaTokenType.IDENTIFIER);
        Symbol symbol = resolve.findIdentInType(env, type.symbol, identifierNode.getTokenValue(), Symbol.VAR);
        associateReference(identifierNode, symbol);
        result = getTypeOfSymbol(symbol);
      }
    } else {
      result = symbols.unknownType;
    }
    return result;
  }

  /**
   * Computes type of a binary operation.
   */
  private Type visitBinaryOperation(Resolve.Env env, AstNode astNode) {
    Type left = getType(astNode.getFirstChild());
    for (int i = 1; i < astNode.getNumberOfChildren(); i += 2) {
      AstNode opNode = astNode.getChild(i);
      if (opNode.is(JavaKeyword.INSTANCEOF)) {
        left = symbols.booleanType;
      } else {
        Type right = getType(astNode.getChild(i + 1));
        // TODO avoid nulls
        if (left == null || right == null) {
          return symbols.unknownType;
        }
        Symbol symbol = resolve.findMethod(env, opNode.getTokenValue(), ImmutableList.of(left, right));
        if (symbol.kind != Symbol.MTH) {
          // not found
          return symbols.unknownType;
        }
        left = ((Type.MethodType) symbol.type).resultType;
      }
    }
    return left;
  }

  /**
   * Computes type of a conditional expression.
   */
  private Type visitConditionalExpression() {
    return symbols.unknownType;
  }

  /**
   * Computes type of an assignment expression. Which is always a type of lvalue.
   * For example in case of {@code double d; int i; res = d = i;} type of assignment expression {@code d = i} is double.
   */
  private Type visitAssignmentExpression(AstNode astNode) {
    return getType(astNode.getFirstChild());
  }

  /**
   * Computes type of an expression.
   * In grammar expression defined as an assignment expression, so simply returns its type.
   */
  private Type visitExpression(AstNode astNode) {
    return getType(astNode.getFirstChild());
  }

  private Type resolveMethod(Resolve.Env env, AstNode astNode) {
    AstNode qualifiedIdentifierNode = astNode.getFirstChild(JavaGrammar.QUALIFIED_IDENTIFIER);
    final Type type;
    if (qualifiedIdentifierNode.getNumberOfChildren() > 1) {
      type = resolveQualifiedIdentifier(env, qualifiedIdentifierNode, true);
    } else {
      type = env.enclosingClass.type;
    }
    // TODO avoid null, which may come from resolveQualifiedIdentifier
    if (type == null) {
      return symbols.unknownType;
    }
    final AstNode identifierNode = qualifiedIdentifierNode.getLastChild();

    Symbol symbol = resolve.findMethod(env, type.symbol, identifierNode.getTokenValue(), ImmutableList.<Type>of());
    associateReference(identifierNode, symbol);
    return getTypeOfSymbol(symbol);
  }

  private Type resolveQualifiedIdentifier(Resolve.Env env, AstNode astNode) {
    return resolveQualifiedIdentifier(env, astNode, false);
  }

  private Type resolveQualifiedIdentifier(Resolve.Env env, AstNode astNode, boolean method) {
    Preconditions.checkArgument(astNode.is(JavaGrammar.QUALIFIED_IDENTIFIER), "Unexpected AstNodeType: " + astNode.getType());

    List<AstNode> identifiers = astNode.getChildren(JavaTokenType.IDENTIFIER);

    Symbol site = resolve.findIdent(env, identifiers.get(0).getTokenValue(), Symbol.VAR | Symbol.TYP | Symbol.PCK);
    associateReference(identifiers.get(0), site);
    for (AstNode identifierNode : identifiers.subList(1, identifiers.size() - (method ? 1 : 0))) {
      if (site.kind >= Symbol.ERRONEOUS) {
        return symbols.unknownType;
      }
      String name = identifierNode.getTokenValue();
      if (site.kind == Symbol.VAR) {
        Type type = ((Symbol.VariableSymbol) site).type;
        // TODO avoid null
        if (type == null) {
          return symbols.unknownType;
        }
        site = resolve.findIdentInType(env, type.symbol, name, Symbol.VAR | Symbol.TYP);
      } else if (site.kind == Symbol.TYP) {
        site = resolve.findIdentInType(env, (Symbol.TypeSymbol) site, name, Symbol.VAR | Symbol.TYP);
      } else if (site.kind == Symbol.PCK) {
        site = resolve.findIdentInPackage(env, site, name, Symbol.VAR | Symbol.PCK);
      } else {
        throw new IllegalStateException();
      }
      associateReference(identifierNode, site);
    }
    return getTypeOfSymbol(site);
  }

  /**
   * TODO duplication of {@link org.sonar.java.resolve.SecondPass#resolveType(org.sonar.java.resolve.Resolve.Env, com.sonar.sslr.api.AstNode)}
   */
  private Type resolveType(Resolve.Env env, AstNode astNode) {
    Preconditions.checkArgument(astNode.is(JavaGrammar.CLASS_TYPE, JavaGrammar.CREATED_NAME));

    env = env.dup();
    List<AstNode> identifiers = astNode.getChildren(JavaTokenType.IDENTIFIER);
    Symbol site = resolve.findIdent(env, identifiers.get(0).getTokenValue(), Symbol.TYP | Symbol.PCK);
    associateReference(identifiers.get(0), site);
    for (AstNode identifierNode : identifiers.subList(1, identifiers.size())) {
      if (site.kind >= Symbol.ERRONEOUS) {
        return symbols.unknownType;
      }
      String name = identifierNode.getTokenValue();
      if (site.kind == Symbol.PCK) {
        env.packge = (Symbol.PackageSymbol) site;
        site = resolve.findIdentInPackage(env, site, name, Symbol.TYP | Symbol.PCK);
      } else {
        env.enclosingClass = (Symbol.TypeSymbol) site;
        site = resolve.findMemberType(env, (Symbol.TypeSymbol) site, name, (Symbol.TypeSymbol) site);
      }
      associateReference(identifierNode, site);
    }
    return getTypeOfSymbol(site);
  }

  private Type getTypeOfSymbol(Symbol symbol) {
    if (symbol.kind < Symbol.ERRONEOUS) {
      return symbol.type;
    } else {
      return symbols.unknownType;
    }
  }

  /**
   * Returns type associated with given AST node.
   */
  public Type getType(AstNode astNode) {
    return types.get(astNode);
  }

  private void associateReference(AstNode astNode, Symbol symbol) {
    if (symbol.kind < Symbol.ERRONEOUS && semanticModel.getAstNode(symbol) != null) {
      // symbol exists in current compilation unit
      semanticModel.associateReference(astNode, symbol);
    }
  }

}
