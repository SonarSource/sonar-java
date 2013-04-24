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
package org.sonar.java.resolve;

import com.google.common.base.Preconditions;
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
 * TODO compute type of binary operators
 * TODO compute type of method calls
 */
public class ExpressionVisitor extends JavaAstVisitor {

  private final Map<AstNodeType, Type> typesOfLiterals = Maps.newHashMap();

  private final AstNodeType[] binaryOperatorAstNodeTypes;

  private final Symbols symbols;
  private final Resolve resolve;
  private final Resolve.Env env;

  private final Map<AstNode, Type> types = Maps.newHashMap();

  public ExpressionVisitor(Symbols symbols, Resolve resolve, Resolve.Env env) {
    this.symbols = symbols;
    this.resolve = resolve;
    this.env = env;

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
      JavaGrammar.CONDITIONAL_OR_EXPRESSION,
      JavaGrammar.CONDITIONAL_EXPRESSION,
      JavaGrammar.ASSIGNMENT_EXPRESSION
    };
  }

  @Override
  public void init() {
    subscribeTo(
      JavaGrammar.EXPRESSION,
      JavaGrammar.PRIMARY,
      JavaGrammar.UNARY_EXPRESSION,
      JavaGrammar.LITERAL,
      JavaGrammar.TYPE);
    subscribeTo(binaryOperatorAstNodeTypes);
  }

  @Override
  public void leaveNode(AstNode astNode) {
    final Type type;
    if (astNode.is(JavaGrammar.EXPRESSION)) {
      type = visitExpression(astNode);
    } else if (astNode.is(JavaGrammar.PRIMARY)) {
      type = visitPrimary(astNode);
    } else if (astNode.is(JavaGrammar.UNARY_EXPRESSION)) {
      type = visitUnaryExpression(astNode);
    } else if (astNode.is(JavaGrammar.LITERAL)) {
      type = visitLiteral(astNode);
    } else if (astNode.is(JavaGrammar.TYPE)) {
      type = visitType(astNode);
    } else if (astNode.is(binaryOperatorAstNodeTypes)) {
      type = visitBinaryOperation(astNode);
    } else {
      throw new IllegalArgumentException("Unexpected AstNodeType: " + astNode.getType());
    }
    types.put(astNode, type);
  }

  private Type visitExpression(AstNode astNode) {
    return getType(astNode.getFirstChild());
  }

  private Type visitLiteral(AstNode astNode) {
    astNode = astNode.getFirstChild();
    Type result = typesOfLiterals.get(astNode.getType());
    return Preconditions.checkNotNull(result, "Unexpected AstNodeType: " + astNode.getType());
  }

  private Type visitType(AstNode astNode) {
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

  private Type visitPrimary(AstNode astNode) {
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
        result = symbols.unknownType;
      } else {
        // super.field
        Type type = getTypeOfSymbol(resolve.findIdent(env, "super", Symbol.VAR));
        // FIXME associateReference
        result = getTypeOfSymbol(resolve.findIdentInType(env, type.symbol, superSuffixNode.getFirstChild(JavaTokenType.IDENTIFIER).getTokenValue(), Symbol.VAR));
      }
    } else if (firstChildNode.is(JavaGrammar.PAR_EXPRESSION)) {
      // (expression)
      result = getType(firstChildNode.getFirstChild(JavaGrammar.EXPRESSION));
    } else if (firstChildNode.is(JavaKeyword.NEW)) {
      // new...
      result = symbols.unknownType;
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
            if (type instanceof Type.ArrayType) {
              result = ((Type.ArrayType) type).elementType;
            } else {
              result = symbols.unknownType;
            }
          }
        } else if (identifierSuffixNode.getFirstChild().is(JavaGrammar.ARGUMENTS)) {
          // id(arguments)
          result = symbols.unknownType;
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
    } else {
      throw new IllegalArgumentException("Unexpected AstNodeType: " + firstChildNode.getType());
    }
    return result;
  }

  private Type visitUnaryExpression(AstNode astNode) {
    final Type result;
    AstNode firstChildNode = astNode.getFirstChild();
    if (firstChildNode.is(JavaPunctuator.LPAR)) {
      // type cast
      result = getType(astNode.getFirstChild(JavaGrammar.TYPE));
    } else if (firstChildNode.is(JavaGrammar.PRIMARY)) {
      Type type = getType(firstChildNode);
      for (AstNode selectorNode : astNode.getChildren(JavaGrammar.SELECTOR)) {
        type = applySelector(type, selectorNode);
      }
      result = type;
    } else if (astNode.getFirstChild().is(JavaGrammar.PREFIX_OP)) {
      result = getType(astNode.getFirstChild().getNextSibling());
    } else {
      throw new IllegalArgumentException("Unexpected AstNodeType: " + firstChildNode.getType());
    }
    return result;
  }

  private Type applySelector(Type type, AstNode astNode) {
    Preconditions.checkArgument(astNode.is(JavaGrammar.SELECTOR));
    final Type result;
    if (type == symbols.unknownType) {
      return type;
    } else if (astNode.getFirstChild().is(JavaGrammar.DIM_EXPR)) {
      // array access
      // TODO get rid of "instanceof"
      if (type instanceof Type.ArrayType) {
        result = ((Type.ArrayType) type).elementType;
      } else {
        result = symbols.unknownType;
      }
    } else if (astNode.hasDirectChildren(JavaTokenType.IDENTIFIER)) {
      if (astNode.hasDirectChildren(JavaGrammar.ARGUMENTS)) {
        // method call
        result = symbols.unknownType;
      } else {
        // field access
        // FIXME associateReference
        result = getTypeOfSymbol(resolve.findIdentInType(env, type.symbol, astNode.getFirstChild(JavaTokenType.IDENTIFIER).getTokenValue(), Symbol.VAR));
      }
    } else {
      result = symbols.unknownType;
    }
    return result;
  }

  private Type visitBinaryOperation(AstNode astNode) {
    return symbols.unknownType;
  }

  /**
   * TODO duplication of {@link org.sonar.java.resolve.ThirdPass#resolve(com.sonar.sslr.api.AstNode)}
   */
  private Type resolveQualifiedIdentifier(Resolve.Env env, AstNode astNode) {
    Preconditions.checkArgument(astNode.is(JavaGrammar.QUALIFIED_IDENTIFIER));

    List<AstNode> identifiers = astNode.getChildren(JavaTokenType.IDENTIFIER);

    Symbol site = resolve.findIdent(env, identifiers.get(0).getTokenValue(), Symbol.VAR | Symbol.TYP | Symbol.PCK);
    // FIXME associateReference(identifiers.get(0), site);
    for (AstNode identifierNode : identifiers.subList(1, identifiers.size())) {
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
      // FIXME associateReference(identifierNode, site);
    }
    return getTypeOfSymbol(site);
  }

  /**
   * TODO duplication of {@link org.sonar.java.resolve.SecondPass#resolveType(org.sonar.java.resolve.Resolve.Env, com.sonar.sslr.api.AstNode)}
   */
  private Type resolveType(Resolve.Env env, AstNode astNode) {
    Preconditions.checkArgument(astNode.is(JavaGrammar.CLASS_TYPE));

    env = env.dup();
    List<AstNode> identifiers = astNode.getChildren(JavaTokenType.IDENTIFIER);
    Symbol site = resolve.findIdent(env, identifiers.get(0).getTokenValue(), Symbol.TYP | Symbol.PCK);
    // FIXME associateReference(identifiers.get(0), site);
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
      // FIXME associateReference(identifierNode, site);
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

}
