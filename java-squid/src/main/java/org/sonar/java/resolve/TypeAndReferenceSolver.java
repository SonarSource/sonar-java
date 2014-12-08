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
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.model.expression.TypeArgumentListTreeImpl;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.BreakStatementTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.ContinueStatementTree;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.InstanceOfTree;
import org.sonar.plugins.java.api.tree.LabeledStatementTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.UnionTypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WildcardTree;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Computes types and references of Identifier and MemberSelectExpression.
 */
public class TypeAndReferenceSolver extends BaseTreeVisitor {

  private final Map<Tree.Kind, Type> typesOfLiterals = Maps.newEnumMap(Tree.Kind.class);

  private final SemanticModel semanticModel;
  private final Symbols symbols;
  private final Resolve resolve;

  private final Map<Tree, Type> types = Maps.newHashMap();
  Resolve.Env env;

  public TypeAndReferenceSolver(SemanticModel semanticModel, Symbols symbols, Resolve resolve) {
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
  public void visitMethod(MethodTree tree) {
    //skip return type, args, and throw clauses : visited in second pass.
    scan(tree.modifiers());
    scan(tree.typeParameters());
    scan(tree.defaultValue());
    scan(tree.block());
  }

  @Override
  public void visitClass(ClassTree tree) {
    //skip superclass and interfaces : visited in second pass.
    scan(tree.modifiers());
    scan(tree.typeParameters());
    scan(tree.members());
  }

  @Override
  public void visitImport(ImportTree tree) {
    //Noop, imports are not expression
  }

  @Override
  public void visitLabeledStatement(LabeledStatementTree tree) {
    //Ignore label (dedicated visitor)
    scan(tree.statement());
  }

  @Override
  public void visitBreakStatement(BreakStatementTree tree) {
    //Ignore break (dedicated visitor)
  }

  @Override
  public void visitContinueStatement(ContinueStatementTree tree) {
    //Ignore continue (dedicated visitor)
  }

  @Override
  public void visitExpressionStatement(ExpressionStatementTree tree) {
    super.visitExpressionStatement(tree);
    // TODO(Godin): strictly speaking statement can't have type
    registerType(tree, getType(tree.expression()));
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    Tree methodSelect = tree.methodSelect();
    Resolve.Env methodEnv = semanticModel.getEnv(tree);
    scan(tree.arguments());
    List<Type> argTypes = getParameterTypes(tree.arguments());
    Symbol symbol = resolveMethodSymbol(methodSelect, methodEnv, argTypes);
    ((MethodInvocationTreeImpl) tree).setSymbol(symbol);
    Type methodType = getTypeOfSymbol(symbol);
    //Register return type for method invocation.
    if (methodType == null || symbol.kind >= Symbol.ERRONEOUS) {
      registerType(tree, symbols.unknownType);
    } else {
      registerType(tree, ((Type.MethodType) methodType).resultType);
    }
  }

  private List<Type> getParameterTypes(List<ExpressionTree> args) {
    ImmutableList.Builder<Type> builder = ImmutableList.builder();
    for (ExpressionTree expressionTree : args) {
      Type symbolType = ((AbstractTypedTree) expressionTree).getSymbolType();
      if (symbolType == null) {
        symbolType = symbols.unknownType;
      }
      builder.add(symbolType);
    }
    return builder.build();
  }

  private Symbol resolveMethodSymbol(Tree methodSelect, Resolve.Env methodEnv, List<Type> argTypes) {
    Symbol symbol;
    IdentifierTree identifier;
    if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mset = (MemberSelectExpressionTree) methodSelect;
      resolveAs(mset.expression(), Symbol.TYP | Symbol.VAR);
      Type type = getType(mset.expression());
      identifier = mset.identifier();
      symbol = resolve.findMethod(methodEnv, type.symbol, identifier.name(), argTypes);
    } else if (methodSelect.is(Tree.Kind.IDENTIFIER)) {
      identifier = (IdentifierTree) methodSelect;
      symbol = resolve.findMethod(methodEnv, identifier.name(), argTypes);
    } else {
      throw new IllegalStateException("Method select in method invocation is not of the expected type " + methodSelect);
    }
    associateReference(identifier, symbol);
    return symbol;
  }

  private void resolveAs(@Nullable Tree tree, int kind) {
    if (tree == null) {
      return;
    }
    if (env == null) {
      resolveAs(tree, kind, semanticModel.getEnv(tree));
    } else {
      resolveAs(tree, kind, env);
    }
  }

  public Symbol resolveAs(Tree tree, int kind, Resolve.Env resolveEnv) {
    return resolveAs(tree, kind, resolveEnv, true);
  }
  public Symbol resolveAs(Tree tree, int kind, Resolve.Env resolveEnv, boolean associateReference) {
    if (tree.is(Tree.Kind.IDENTIFIER, Tree.Kind.MEMBER_SELECT)) {
      Symbol resolvedSymbol;
      IdentifierTree identifierTree;
      if (tree.is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree mse = (MemberSelectExpressionTree) tree;
        if (JavaKeyword.CLASS.getValue().equals(mse.identifier().name())) {
          // member select ending with .class
          registerType(tree, symbols.classType);
          return null;
        }
        identifierTree = mse.identifier();
        resolvedSymbol = getSymbolOfMemberSelectExpression(mse, kind, resolveEnv);
        registerType(identifierTree, getTypeOfSymbol(resolvedSymbol));
      } else {
        identifierTree = (IdentifierTree) tree;
        resolvedSymbol = resolve.findIdent(resolveEnv, identifierTree.name(), kind);
      }
      if(associateReference) {
        associateReference(identifierTree, resolvedSymbol);
      }
      registerType(tree, getTypeOfSymbol(resolvedSymbol));
      return resolvedSymbol;
    }
    tree.accept(this);
    Type type = getType(tree);
    if (tree.is(Tree.Kind.INFERED_TYPE)) {
      type = symbols.unknownType;
      registerType(tree, type);
    }
    if (type == null) {
      throw new IllegalStateException("Type not resolved " + tree);
    }
    return type.symbol;
  }

  private Symbol getSymbolOfMemberSelectExpression(MemberSelectExpressionTree mse, int kind, Resolve.Env resolveEnv) {
    int expressionKind = Symbol.TYP;
    if ((kind & Symbol.VAR) != 0) {
      expressionKind |= Symbol.VAR;
    }
    if ((kind & Symbol.TYP) != 0) {
      expressionKind |= Symbol.PCK;
    }

    Symbol site = resolveAs(mse.expression(), expressionKind, resolveEnv);
    if (site.kind == Symbol.VAR) {
      return resolve.findIdentInType(resolveEnv, site.type.symbol, mse.identifier().name(), Symbol.VAR);
    }
    if (site.kind == Symbol.TYP) {
      return resolve.findIdentInType(resolveEnv, (Symbol.TypeSymbol) site, mse.identifier().name(), kind);
    }
    if (site.kind == Symbol.PCK) {
      return resolve.findIdentInPackage(site, mse.identifier().name(), kind);
    }
    return symbols.unknownSymbol;
  }

  private void resolveAs(List<? extends Tree> trees, int kind) {
    for (Tree tree : trees) {
      resolveAs(tree, kind);
    }
  }

  @Override
  public void visitTypeArguments(TypeArgumentListTreeImpl trees) {
    resolveAs((List<? extends Tree>) trees, Symbol.TYP);
  }

  @Override
  public void visitInstanceOf(InstanceOfTree tree) {
    resolveAs(tree.expression(), Symbol.VAR);
    resolveAs(tree.type(), Symbol.TYP);
    registerType(tree, symbols.booleanType);
  }

  @Override
  public void visitParameterizedType(ParameterizedTypeTree tree) {
    resolveAs(tree.type(), Symbol.TYP);
    resolveAs(tree.typeArguments(), Symbol.TYP);
    //FIXME(benzonico) approximation of generic type to its raw type
    registerType(tree, getType(tree.type()));
  }

  @Override
  public void visitWildcard(WildcardTree tree) {
    if (tree.bound() == null) {
      registerType(tree, symbols.unknownType);
    } else {
      resolveAs(tree.bound(), Symbol.TYP);
      registerType(tree, getType(tree.bound()));
    }
  }

  @Override
  public void visitConditionalExpression(ConditionalExpressionTree tree) {
    resolveAs(tree.condition(), Symbol.VAR);
    resolveAs(tree.trueExpression(), Symbol.VAR);
    resolveAs(tree.falseExpression(), Symbol.VAR);
    registerType(tree, symbols.unknownType);
  }

  @Override
  public void visitThrowStatement(ThrowStatementTree tree) {
    resolveAs(tree.expression(), Symbol.VAR);
  }

  @Override
  public void visitLambdaExpression(LambdaExpressionTree tree) {
    //TODO resolve variables
    super.visitLambdaExpression(tree);
    registerType(tree, symbols.unknownType);
  }

  @Override
  public void visitNewArray(NewArrayTree tree) {
    resolveAs(tree.type(), Symbol.TYP);
    resolveAs(tree.dimensions(), Symbol.VAR);
    resolveAs(tree.initializers(), Symbol.VAR);
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
    resolveAs(tree.expression(), Symbol.VAR);
    registerType(tree, getType(tree.expression()));
  }

  @Override
  public void visitArrayAccessExpression(ArrayAccessExpressionTree tree) {
    resolveAs(tree.expression(), Symbol.VAR);
    resolveAs(tree.index(), Symbol.VAR);
    Type type = getType(tree.expression());
    if (type != null && type.tag == Type.ARRAY) {
      registerType(tree, ((Type.ArrayType) type).elementType);
    } else {
      registerType(tree, symbols.unknownType);
    }
  }

  @Override
  public void visitBinaryExpression(BinaryExpressionTree tree) {
    resolveAs(tree.leftOperand(), Symbol.VAR);
    resolveAs(tree.rightOperand(), Symbol.VAR);
    Type left = getType(tree.leftOperand());
    Type right = getType(tree.rightOperand());
    // TODO avoid nulls
    if (left == null || right == null) {
      registerType(tree, symbols.unknownType);
      return;
    }
    Symbol symbol = resolve.findMethod(semanticModel.getEnv(tree), symbols.predefClass, tree.operatorToken().text(), ImmutableList.of(left, right));
    if (symbol.kind != Symbol.MTH) {
      // not found
      registerType(tree, symbols.unknownType);
      return;
    }
    registerType(tree, ((Type.MethodType) symbol.type).resultType);
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    if (tree.enclosingExpression() != null) {
      resolveAs(tree.enclosingExpression(), Symbol.VAR);
    }
    Resolve.Env newClassEnv = semanticModel.getEnv(tree);
    resolveAs(tree.identifier(), Symbol.TYP, newClassEnv, false);
    resolveAs(tree.typeArguments(), Symbol.TYP);
    resolveAs(tree.arguments(), Symbol.VAR);
    resolveConstructorSymbol(tree.identifier(), newClassEnv, getParameterTypes(tree.arguments()));
    if (tree.classBody() != null) {
      //TODO create a new symbol and type for each anonymous class
      scan(tree.classBody());
      registerType(tree, symbols.unknownType);
    } else {
      registerType(tree, getType(tree.identifier()));
    }
  }

  private Symbol resolveConstructorSymbol(Tree constructorSelect, Resolve.Env methodEnv, List<Type> argTypes) {
    Symbol symbol;
    IdentifierTree identifier = getConstructorIdentifier(constructorSelect);
    symbol = resolve.findMethod(methodEnv, ((AbstractTypedTree) identifier).getSymbolType().getSymbol(), "<init>", argTypes);
    associateReference(identifier, symbol);
    return symbol;
  }

  private IdentifierTree getConstructorIdentifier(Tree constructorSelect) {
    IdentifierTree identifier;
    if (constructorSelect.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mset = (MemberSelectExpressionTree) constructorSelect;
      identifier = mset.identifier();
    } else if (constructorSelect.is(Tree.Kind.IDENTIFIER)) {
      identifier = (IdentifierTree) constructorSelect;
    } else if(constructorSelect.is(Tree.Kind.PARAMETERIZED_TYPE)) {
      identifier = getConstructorIdentifier(((ParameterizedTypeTree) constructorSelect).type());
    } else {
      throw new IllegalStateException("Constructor select is not of the expected type " + constructorSelect);
    }
    return identifier;
  }


  @Override
  public void visitPrimitiveType(PrimitiveTypeTree tree) {
    Type type;
    if (env == null) {
      type = resolve.findIdent(semanticModel.getEnv(tree), tree.keyword().text(), Symbol.TYP).type;
    } else {
      type = resolve.findIdent(env, tree.keyword().text(), Symbol.TYP).type;
    }
    registerType(tree, type);
  }

  @Override
  public void visitVariable(VariableTree tree) {
    scan(tree.modifiers());
    //skip type, it has been resolved in second pass
    if (tree.initializer() != null) {
      resolveAs(tree.initializer(), Symbol.VAR);
    }
  }

  /**
   * Computes type of an assignment expression. Which is always a type of lvalue.
   * For example in case of {@code double d; int i; res = d = i;} type of assignment expression {@code d = i} is double.
   */
  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    resolveAs(tree.variable(), Symbol.VAR);
    resolveAs(tree.expression(), Symbol.VAR);
    Type type = getType(tree.variable());
    registerType(tree, type);
  }

  @Override
  public void visitLiteral(LiteralTree tree) {
    Type type = typesOfLiterals.get(((JavaTree) tree).getKind());
    registerType(tree, type);
  }

  @Override
  public void visitUnaryExpression(UnaryExpressionTree tree) {
    resolveAs(tree.expression(), Symbol.VAR);
    registerType(tree, getType(tree.expression()));
  }

  @Override
  public void visitArrayType(ArrayTypeTree tree) {
    //FIXME(benzonico) respect invariant to set type. Required for cases like : int i[],j[]; Compute array element type only if not previously computed.
    if (getType(tree.type()) == null) {
      resolveAs(tree.type(), Symbol.TYP);
    }
    registerType(tree, new Type.ArrayType(getType(tree.type()), symbols.arrayClass));
  }

  @Override
  public void visitTypeCast(TypeCastTree tree) {
    resolveAs(tree.type(), Symbol.TYP);
    resolveAs(tree.expression(), Symbol.VAR);
    registerType(tree, getType(tree.type()));
  }

  @Override
  public void visitUnionType(UnionTypeTree tree) {
    resolveAs(tree.typeAlternatives(), Symbol.TYP);
    //TODO compute type of union type: lub(alternatives) cf JLS8 14.20
    registerType(tree, symbols.unknownType);
  }

  @Override
  public void visitEnumConstant(EnumConstantTree tree) {
    scan(tree.modifiers());
    NewClassTree newClassTree = (NewClassTree) tree.initializer();
    scan(newClassTree.enclosingExpression());
    // register identifier type
    registerType(newClassTree.identifier(), ((VariableTreeImpl) tree).getSymbol().getType());
    scan(newClassTree.typeArguments());
    scan(newClassTree.arguments());
    scan(newClassTree.classBody());
    resolveConstructorSymbol(tree.simpleName(), semanticModel.getEnv(tree), getParameterTypes(newClassTree.arguments()));
  }

  @Override
  public void visitAnnotation(AnnotationTree tree) {
    resolveAs(tree.annotationType(), Symbol.TYP);
    for (ExpressionTree expressionTree : tree.arguments()) {
      resolveAs(expressionTree, Symbol.VAR);
    }
    registerType(tree, getType(tree.annotationType()));
  }

  @Override
  public void visitIdentifier(IdentifierTree tree) {
    if (((AbstractTypedTree) tree).getSymbolType() == null) {
      resolveAs(tree, Symbol.VAR);
    }
  }

  @Override
  public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
    if (((AbstractTypedTree) tree).getSymbolType() == null) {
      resolveAs(tree, Symbol.VAR);
    }
  }

  @Override
  public void visitOther(Tree tree) {
    registerType(tree, symbols.unknownType);
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
