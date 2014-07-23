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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.InstanceOfTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

import java.util.Map;

/**
 * Computes types of expressions.
 * TODO compute type of method calls
 */
public class ExpressionVisitor extends BaseTreeVisitor {

  private final Map<Tree.Kind, Type> typesOfLiterals = Maps.newEnumMap(Tree.Kind.class);

  private final SemanticModel semanticModel;
  private final Symbols symbols;
  private final Resolve resolve;

  private final Map<Tree, Type> types = Maps.newHashMap();

  public ExpressionVisitor(SemanticModel semanticModel, Symbols symbols, Resolve resolve) {
    this.semanticModel = semanticModel;
    this.symbols = symbols;
    this.resolve = resolve;
    typesOfLiterals.put(Tree.Kind.BOOLEAN_LITERAL, symbols.booleanType);
    typesOfLiterals.put(Tree.Kind.NULL_LITERAL, symbols.nullType);
    typesOfLiterals.put(Tree.Kind.CHAR_LITERAL, symbols.charType);
    typesOfLiterals.put(Tree.Kind.STRING_LITERAL, symbols.stringType);
    typesOfLiterals.put(Tree.Kind.FLOAT_LITERAL, symbols.floatType);
    typesOfLiterals.put(Tree.Kind.DOUBLE_LITERAL, symbols.doubleType);
    typesOfLiterals.put(Tree.Kind.LONG_LITERAL, symbols.longType);
    typesOfLiterals.put(Tree.Kind.INT_LITERAL, symbols.intType);
  }

  @Override
  public void visitExpressionStatement(ExpressionStatementTree tree) {
    super.visitExpressionStatement(tree);
    // TODO(Godin): strictly speaking statement can't have type
    registerType(tree, getType(tree.expression()));
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    super.visitMethodInvocation(tree);
    Tree methodSelect = tree.methodSelect();
    Resolve.Env env = semanticModel.getEnv(tree);
    IdentifierTree identifier;
    Type type;
    String name;
    if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mset = (MemberSelectExpressionTree) methodSelect;
      type = getType(mset.expression());
      identifier = mset.identifier();
    } else if (methodSelect.is(Tree.Kind.IDENTIFIER)) {
      type = env.enclosingClass.type;
      identifier = (IdentifierTree) methodSelect;
    } else {
      throw new IllegalStateException("Method select in method invocation is not of the expected type " + methodSelect);
    }
    name = identifier.name();
    if (type == null) {
      type = symbols.unknownType;
    }
    Symbol symbol = resolve.findMethod(env, type.symbol, name, ImmutableList.<Type>of());
    associateReference(identifier, symbol);
    type = getTypeOfSymbol(symbol);
    if (type == null) {
      type = symbols.unknownType;
    }
    registerType(tree, type);
  }

  @Override
  public void visitInstanceOf(InstanceOfTree tree) {
    super.visitInstanceOf(tree);
    registerType(tree, symbols.booleanType);
  }

  @Override
  public void visitParameterizedType(ParameterizedTypeTree tree) {
    super.visitParameterizedType(tree);
    registerType(tree, symbols.unknownType);
  }

  @Override
  public void visitConditionalExpression(ConditionalExpressionTree tree) {
    super.visitConditionalExpression(tree);
    registerType(tree, symbols.unknownType);
  }

  @Override
  public void visitLambdaExpression(LambdaExpressionTree tree) {
    super.visitLambdaExpression(tree);
    registerType(tree, symbols.unknownType);
  }

  @Override
  public void visitNewArray(NewArrayTree tree) {
    super.visitNewArray(tree);
    Type type = getType(tree.type());
    int dimensions = tree.dimensions().size();
    // TODO why?
    type = new Type.ArrayType(type, symbols.arrayClass);
    for (int i = 1; i < dimensions; i++) {
      type = new Type.ArrayType(type, symbols.arrayClass);
    }
    registerType(tree, type);
  }

  @Override
  public void visitParenthesized(ParenthesizedTree tree) {
    super.visitParenthesized(tree);
    registerType(tree, getType(tree.expression()));
  }

  @Override
  public void visitArrayAccessExpression(ArrayAccessExpressionTree tree) {
    super.visitArrayAccessExpression(tree);
    Type type = getType(tree.expression());
    if (type != null && type.tag == Type.ARRAY) {
      registerType(tree, ((Type.ArrayType) type).elementType);
    } else {
      registerType(tree, symbols.unknownType);
    }
  }

  @Override
  public void visitBinaryExpression(BinaryExpressionTree tree) {
    super.visitBinaryExpression(tree);
    Resolve.Env env = semanticModel.getEnv(tree);
    Type left = getType(tree.leftOperand());
    Type right = getType(tree.rightOperand());
    // TODO avoid nulls
    if (left == null || right == null) {
      registerType(tree, symbols.unknownType);
      return;
    }
    Symbol symbol = resolve.findMethod(env, tree.operatorToken().text(), ImmutableList.of(left, right));
    if (symbol.kind != Symbol.MTH) {
      // not found
      registerType(tree, symbols.unknownType);
      return;
    }
    registerType(tree, ((Type.MethodType) symbol.type).resultType);
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    super.visitNewClass(tree);
    if (tree.classBody() != null) {
      registerType(tree, symbols.unknownType);
    } else {
      registerType(tree, getType(tree.identifier()));
    }
  }

  @Override
  public void visitPrimitiveType(PrimitiveTypeTree tree) {
    Type type = resolve.findIdent(semanticModel.getEnv(tree), tree.keyword().text(), Symbol.TYP).type;
    registerType(tree, type);
  }

  /**
   * Computes type of an assignment expression. Which is always a type of lvalue.
   * For example in case of {@code double d; int i; res = d = i;} type of assignment expression {@code d = i} is double.
   */
  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    super.visitAssignmentExpression(tree);
    Type type = getType(tree.variable());
    registerType(tree, type);
  }

  @Override
  public void visitLiteral(LiteralTree tree) {
    super.visitLiteral(tree);
    Type type = typesOfLiterals.get(((JavaTree) tree).getKind());
    registerType(tree, type);
  }

  @Override
  public void visitUnaryExpression(UnaryExpressionTree tree) {
    super.visitUnaryExpression(tree);
    registerType(tree, getType(tree.expression()));
  }

  @Override
  public void visitArrayType(ArrayTypeTree tree) {
    super.visitArrayType(tree);
    registerType(tree, new Type.ArrayType(getType(tree.type()), symbols.arrayClass));
  }

  @Override
  public void visitTypeCast(TypeCastTree tree) {
    super.visitTypeCast(tree);
    registerType(tree, getType(tree.type()));
  }

  @Override
  public void visitEnumConstant(EnumConstantTree tree) {
    scan(tree.modifiers());
    NewClassTree newClassTree = (NewClassTree) tree.initializer();
    scan(newClassTree.enclosingExpression());
    // skip identifier
    scan(newClassTree.typeArguments());
    scan(newClassTree.arguments());
    scan(newClassTree.classBody());
  }

  @Override
  public void visitIdentifier(IdentifierTree tree) {
    Symbol symbol = resolve.findIdent(semanticModel.getEnv(tree), tree.name(), Symbol.VAR | Symbol.TYP | Symbol.PCK);
    associateReference(tree, symbol);
    registerType(tree, getTypeOfSymbol(symbol));
  }

  @Override
  public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
    resolveQualifiedIdentifier(tree);
  }

  @Override
  public void visitAnnotation(AnnotationTree tree) {
    super.visitAnnotation(tree);
    registerType(tree, symbols.unknownType);
  }

  private void resolveQualifiedIdentifier(Tree tree) {
    final Resolve.Env env = semanticModel.getEnv(tree);
    class FQV extends BaseTreeVisitor {
      private Symbol site;

      @Override
      public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
        scan(tree.expression());
        String name = tree.identifier().name();
        if (JavaKeyword.CLASS.getValue().equals(name)) {
          registerType(tree, symbols.classType);
          return;
        }
        if (site.kind >= Symbol.ERRONEOUS) {
          registerType(tree, symbols.unknownType);
          return;
        }
        if (site.kind == Symbol.VAR) {
          Type siteType = ((Symbol.VariableSymbol) site).type;
          // TODO avoid null
          if (siteType == null) {
            registerType(tree, symbols.unknownType);
            return;
          }
          site = resolve.findIdentInType(env, siteType.symbol, name, Symbol.VAR | Symbol.TYP);
        } else if (site.kind == Symbol.TYP) {
          site = resolve.findIdentInType(env, (Symbol.TypeSymbol) site, name, Symbol.VAR | Symbol.TYP);
        } else if (site.kind == Symbol.PCK) {
          // FIXME(Godin): contract of method says that kind should be mask of "TYP" and "PCK", but not "VAR" as here:
          site = resolve.findIdentInPackage(env, site, name, Symbol.VAR | Symbol.PCK);
        } else if (site.kind == Symbol.MTH) {
          // FIXME(Godin): Unhandled case! For example see org.sonar.duplications.java.JavaStatementBuilder , which uses fluent interface
          registerType(tree, symbols.unknownType);
          return;
        } else {
          throw new IllegalStateException("Unexpected Symbol kind: " + site.kind);
        }
        associateReference(tree.identifier(), site);
        registerType(tree, getTypeOfSymbol(site));
      }

      @Override
      public void visitArrayType(ArrayTypeTree tree) {
        super.visitArrayType(tree);
        registerType(tree, new Type.ArrayType(getType(tree.type()), symbols.arrayClass));
      }

      @Override
      public void visitArrayAccessExpression(ArrayAccessExpressionTree tree) {
        super.visitArrayAccessExpression(tree);
        Type arrayType = getType(tree.expression());
        if (arrayType != null && arrayType.tag == Type.ARRAY) {
          site = arrayType.symbol;
          registerType(tree, ((Type.ArrayType) arrayType).elementType);
        } else {
          registerType(tree, symbols.unknownType);
        }
      }

      @Override
      public void visitIdentifier(IdentifierTree tree) {
        site = resolve.findIdent(semanticModel.getEnv(tree), tree.name(), Symbol.VAR | Symbol.TYP | Symbol.PCK);
        associateReference(tree, site);
        registerType(tree, getTypeOfSymbol(site));
      }

      @Override
      public void visitLiteral(LiteralTree tree) {
        Type literalType = typesOfLiterals.get(((JavaTree) tree).getKind());
        site = literalType.symbol;
        registerType(tree, literalType);
      }

      @Override
      public void visitPrimitiveType(PrimitiveTypeTree tree) {
        site = resolve.findIdent(semanticModel.getEnv(tree), tree.keyword().text(), Symbol.TYP);
        registerType(tree, site.type);
      }
    }
    FQV visitor = new FQV();
    tree.accept(visitor);
  }

  private Type getTypeOfSymbol(Symbol symbol) {
    if (symbol.kind < Symbol.ERRONEOUS) {
      return symbol.type;
    } else {
      return symbols.unknownType;
    }
  }

  @VisibleForTesting
  Type getType(Tree tree) {
    return types.get(tree);
  }

  private void registerType(Tree tree, Type type) {
    if (AbstractTypedTree.class.isAssignableFrom(tree.getClass())) {
      ((AbstractTypedTree) tree).setType(type);
    }
    types.put(tree, type);
  }

  private void associateReference(IdentifierTree tree, Symbol symbol) {
    if (symbol.kind < Symbol.ERRONEOUS) {
      semanticModel.associateReference(tree, symbol);
    }
  }

}
