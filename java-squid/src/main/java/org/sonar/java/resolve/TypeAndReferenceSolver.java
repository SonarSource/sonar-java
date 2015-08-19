/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.model.expression.NewClassTreeImpl;
import org.sonar.java.model.expression.TypeArgumentListTreeImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.ArrayDimensionTree;
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
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.UnionTypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WildcardTree;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Computes types and references of Identifier and MemberSelectExpression.
 */
public class TypeAndReferenceSolver extends BaseTreeVisitor {

  private final Map<Tree.Kind, JavaType> typesOfLiterals = Maps.newEnumMap(Tree.Kind.class);

  private final SemanticModel semanticModel;
  private final Symbols symbols;
  private final Resolve resolve;
  private final ParametrizedTypeCache parametrizedTypeCache;

  private final Map<Tree, JavaType> types = Maps.newHashMap();
  Resolve.Env env;

  public TypeAndReferenceSolver(SemanticModel semanticModel, Symbols symbols, Resolve resolve, ParametrizedTypeCache parametrizedTypeCache) {
    this.semanticModel = semanticModel;
    this.symbols = symbols;
    this.resolve = resolve;
    this.parametrizedTypeCache = parametrizedTypeCache;
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
    //skip return type, and throw clauses : visited in second pass.
    scan(tree.modifiers());
    completeMetadata((JavaSymbol.MethodJavaSymbol) tree.symbol(), tree.modifiers().annotations());
    scan(tree.typeParameters());
    // revisits the parameters to resolve their annotations.
    scan(tree.parameters());
    scan(tree.defaultValue());
    scan(tree.block());
  }

  @Override
  public void visitClass(ClassTree tree) {
    //skip superclass and interfaces : visited in second pass.
    scan(tree.modifiers());
    completeMetadata((JavaSymbol) tree.symbol(), tree.modifiers().annotations());
    scan(tree.typeParameters());
    scan(tree.members());
  }

  private static void completeMetadata(JavaSymbol symbol, List<AnnotationTree> annotations) {
    for (AnnotationTree tree : annotations) {
      AnnotationInstanceResolve annotationInstance = new AnnotationInstanceResolve((JavaSymbol.TypeJavaSymbol) tree.symbolType().symbol());
      symbol.metadata().addAnnotation(annotationInstance);
      Arguments arguments = tree.arguments();
      if (arguments.size() > 1 || (!arguments.isEmpty() && arguments.get(0).is(Tree.Kind.ASSIGNMENT))) {
        for (ExpressionTree expressionTree : arguments) {
          AssignmentExpressionTree aet = (AssignmentExpressionTree) expressionTree;
          // TODO: Store more precise value than the expression (real value in case of literal, symbol for enums, array of values, solve constants?)
          annotationInstance.addValue(new AnnotationValueResolve(((IdentifierTree) aet.variable()).name(), aet.expression()));
        }
      } else {
        // Constant
        addConstantValue(tree, annotationInstance);
      }
    }
  }
  
  private static void addConstantValue(AnnotationTree tree, AnnotationInstanceResolve annotationInstance) {
    Collection<Symbol> scopeSymbols = tree.annotationType().symbolType().symbol().memberSymbols();
    for (ExpressionTree expressionTree : tree.arguments()) {
      String name = "";
      for (Symbol scopeSymbol : scopeSymbols) {
        if(scopeSymbol.isMethodSymbol()) {
          name = scopeSymbol.name();
          break;
        }
      }
      annotationInstance.addValue(new AnnotationValueResolve(name, expressionTree));
    }
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
    scan(tree.typeArguments());
    List<JavaType> argTypes = getParameterTypes(tree.arguments());
    List<JavaType> typeParamTypes = Lists.newArrayList();
    if(tree.typeArguments() != null ) {
      typeParamTypes = getParameterTypes(tree.typeArguments());
    }
    Resolve.Resolution resolution = resolveMethodSymbol(methodSelect, methodEnv, argTypes, typeParamTypes);
    JavaSymbol symbol = resolution.symbol();
    ((MethodInvocationTreeImpl) tree).setSymbol(symbol);
    registerType(tree, resolution.type());
  }

  private static List<JavaType> getParameterTypes(List<? extends Tree> args) {
    ImmutableList.Builder<JavaType> builder = ImmutableList.builder();
    for (Tree expressionTree : args) {
      JavaType symbolType = Symbols.unknownType;
      if (((AbstractTypedTree) expressionTree).isTypeSet()) {
        symbolType = (JavaType) ((AbstractTypedTree) expressionTree).symbolType();
      }
      builder.add(symbolType);
    }
    return builder.build();
  }

  private Resolve.Resolution resolveMethodSymbol(Tree methodSelect, Resolve.Env methodEnv, List<JavaType> argTypes, List<JavaType> typeParamTypes) {
    Resolve.Resolution resolution;
    IdentifierTree identifier;
    if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mset = (MemberSelectExpressionTree) methodSelect;
      resolveAs(mset.expression(), JavaSymbol.TYP | JavaSymbol.VAR);
      JavaType type = getType(mset.expression());
      identifier = mset.identifier();
      resolution = resolve.findMethod(methodEnv, type, identifier.name(), argTypes, typeParamTypes);
    } else if (methodSelect.is(Tree.Kind.IDENTIFIER)) {
      identifier = (IdentifierTree) methodSelect;
      resolution = resolve.findMethod(methodEnv, identifier.name(), argTypes, typeParamTypes);
    } else {
      throw new IllegalStateException("Method select in method invocation is not of the expected type " + methodSelect);
    }
    associateReference(identifier, resolution.symbol());
    return resolution;
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

  public JavaSymbol resolveAs(Tree tree, int kind, Resolve.Env resolveEnv) {
    return resolveAs(tree, kind, resolveEnv, true);
  }

  public JavaSymbol resolveAs(Tree tree, int kind, Resolve.Env resolveEnv, boolean associateReference) {
    if (tree.is(Tree.Kind.IDENTIFIER, Tree.Kind.MEMBER_SELECT)) {
      JavaSymbol resolvedSymbol;
      IdentifierTree identifierTree;
      if (tree.is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree mse = (MemberSelectExpressionTree) tree;
        if (JavaKeyword.CLASS.getValue().equals(mse.identifier().name())) {
          resolveAs(mse.expression(), JavaSymbol.TYP, resolveEnv);
          // member select ending with .class
          registerType(tree, symbols.classType);
          return symbols.classType.symbol;
        }

        identifierTree = mse.identifier();
        Resolve.Resolution res = getSymbolOfMemberSelectExpression(mse, kind, resolveEnv);
        resolvedSymbol = res.symbol();
        JavaType resolvedType = resolve.resolveTypeSubstitution(res.type(), getType(mse.expression()));
        registerType(identifierTree, resolvedType);
        registerType(tree, resolvedType);
      } else {
        identifierTree = (IdentifierTree) tree;
        Resolve.Resolution resolution = resolve.findIdent(resolveEnv, identifierTree.name(), kind);
        resolvedSymbol = resolution.symbol();
        registerType(tree, resolution.type());
      }
      if(associateReference) {
        associateReference(identifierTree, resolvedSymbol);
      }
      return resolvedSymbol;
    }
    tree.accept(this);
    JavaType type = getType(tree);
    if (tree.is(Tree.Kind.INFERED_TYPE)) {
      type = Symbols.unknownType;
      registerType(tree, type);
    }
    if (type == null) {
      throw new IllegalStateException("Type not resolved " + tree);
    }
    return type.symbol;
  }

  private Resolve.Resolution getSymbolOfMemberSelectExpression(MemberSelectExpressionTree mse, int kind, Resolve.Env resolveEnv) {
    int expressionKind = JavaSymbol.TYP;
    if ((kind & JavaSymbol.VAR) != 0) {
      expressionKind |= JavaSymbol.VAR;
    }
    if ((kind & JavaSymbol.TYP) != 0) {
      expressionKind |= JavaSymbol.PCK;
    }
    //TODO: resolveAs result is only used here, should probably be refactored
    JavaSymbol site = resolveAs(mse.expression(), expressionKind, resolveEnv);
    if (site.kind == JavaSymbol.VAR) {
      return resolve.findIdentInType(resolveEnv, getType(mse.expression()).symbol, mse.identifier().name(), JavaSymbol.VAR);
    }
    if (site.kind == JavaSymbol.TYP) {
      return resolve.findIdentInType(resolveEnv, (JavaSymbol.TypeJavaSymbol) site, mse.identifier().name(), kind);
    }
    if (site.kind == JavaSymbol.PCK) {
      return Resolve.Resolution.resolution(resolve.findIdentInPackage(site, mse.identifier().name(), kind));
    }
    return resolve.unresolved();
  }

  private void resolveAs(List<? extends Tree> trees, int kind) {
    for (Tree tree : trees) {
      resolveAs(tree, kind);
    }
  }

  @Override
  public void visitTypeParameter(TypeParameterTree typeParameter) {
    //Type parameters have been handled in first and second pass.
  }

  @Override
  public void visitTypeArguments(TypeArgumentListTreeImpl trees) {
    resolveAs((List<? extends Tree>) trees, JavaSymbol.TYP);
  }

  @Override
  public void visitInstanceOf(InstanceOfTree tree) {
    resolveAs(tree.expression(), JavaSymbol.VAR);
    resolveAs(tree.type(), JavaSymbol.TYP);
    registerType(tree, symbols.booleanType);
  }

  @Override
  public void visitParameterizedType(ParameterizedTypeTree tree) {
    resolveAs(tree.type(), JavaSymbol.TYP);
    resolveAs((List<Tree>) tree.typeArguments(), JavaSymbol.TYP);

    JavaType type = getType(tree.type());
    //Type substitution for parametrized type.
    TypeSubstitution typeSubstitution = new TypeSubstitution();
    if (tree.typeArguments().size() <= type.getSymbol().typeVariableTypes.size()) {
      for (int i = 0; i < tree.typeArguments().size(); i++) {
        typeSubstitution.add(type.getSymbol().typeVariableTypes.get(i), getType(tree.typeArguments().get(i)));
      }
    }
    registerType(tree, parametrizedTypeCache.getParametrizedTypeType(type.getSymbol(), typeSubstitution));
  }

  @Override
  public void visitWildcard(WildcardTree tree) {
    if (tree.bound() == null) {
      registerType(tree, Symbols.unknownType);
    } else {
      resolveAs(tree.bound(), JavaSymbol.TYP);
      registerType(tree, getType(tree.bound()));
    }
  }

  @Override
  public void visitConditionalExpression(ConditionalExpressionTree tree) {
    resolveAs(tree.condition(), JavaSymbol.VAR);
    resolveAs(tree.trueExpression(), JavaSymbol.VAR);
    resolveAs(tree.falseExpression(), JavaSymbol.VAR);
    registerType(tree, Symbols.unknownType);
  }

  @Override
  public void visitThrowStatement(ThrowStatementTree tree) {
    resolveAs(tree.expression(), JavaSymbol.VAR);
  }

  @Override
  public void visitLambdaExpression(LambdaExpressionTree tree) {
    //TODO resolve variables
    super.visitLambdaExpression(tree);
    registerType(tree, Symbols.unknownType);
  }

  @Override
  public void visitNewArray(NewArrayTree tree) {
    resolveAs(tree.type(), JavaSymbol.TYP);
    scan(tree.dimensions());
    resolveAs((List<? extends Tree>) tree.initializers(), JavaSymbol.VAR);
    JavaType type = getType(tree.type());
    int dimensions = tree.dimensions().size();
    // TODO why?
    type = new JavaType.ArrayJavaType(type, symbols.arrayClass);
    for (int i = 1; i < dimensions; i++) {
      type = new JavaType.ArrayJavaType(type, symbols.arrayClass);
    }
    registerType(tree, type);
  }

  @Override
  public void visitParenthesized(ParenthesizedTree tree) {
    resolveAs(tree.expression(), JavaSymbol.VAR);
    registerType(tree, getType(tree.expression()));
  }

  @Override
  public void visitArrayAccessExpression(ArrayAccessExpressionTree tree) {
    resolveAs(tree.expression(), JavaSymbol.VAR);
    scan(tree.dimension());
    JavaType type = getType(tree.expression());
    if (type != null && type.tag == JavaType.ARRAY) {
      registerType(tree, ((JavaType.ArrayJavaType) type).elementType);
    } else {
      registerType(tree, Symbols.unknownType);
    }
  }

  @Override
  public void visitArrayDimension(ArrayDimensionTree tree) {
    resolveAs(tree.expression(), JavaSymbol.VAR);
    registerType(tree, getType(tree.expression()));
  }

  @Override
  public void visitBinaryExpression(BinaryExpressionTree tree) {
    resolveAs(tree.leftOperand(), JavaSymbol.VAR);
    resolveAs(tree.rightOperand(), JavaSymbol.VAR);
    JavaType left = getType(tree.leftOperand());
    JavaType right = getType(tree.rightOperand());
    // TODO avoid nulls
    if (left == null || right == null) {
      registerType(tree, Symbols.unknownType);
      return;
    }
    JavaSymbol symbol = resolve.findMethod(semanticModel.getEnv(tree), symbols.predefClass.type, tree.operatorToken().text(), ImmutableList.of(left, right)).symbol();
    if (symbol.kind != JavaSymbol.MTH) {
      // not found
      registerType(tree, Symbols.unknownType);
      return;
    }
    registerType(tree, ((JavaType.MethodJavaType) symbol.type).resultType);
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    if (tree.enclosingExpression() != null) {
      resolveAs(tree.enclosingExpression(), JavaSymbol.VAR);
    }
    Resolve.Env newClassEnv = semanticModel.getEnv(tree);
    resolveAs(tree.identifier(), JavaSymbol.TYP, newClassEnv, false);
    if (tree.typeArguments() != null) {
      resolveAs((List<Tree>) tree.typeArguments(), JavaSymbol.TYP);
    }
    resolveAs((List<ExpressionTree>) tree.arguments(), JavaSymbol.VAR);
    NewClassTreeImpl newClassTreeImpl = (NewClassTreeImpl) tree;
    resolveConstructorSymbol(newClassTreeImpl.getConstructorIdentifier(), newClassEnv, getParameterTypes(tree.arguments()));
    ClassTree classBody = tree.classBody();
    if (classBody != null) {
      JavaType type = (JavaType) tree.identifier().symbolType();
      JavaType.ClassJavaType anonymousClassType = (JavaType.ClassJavaType) classBody.symbol().type();
      if (type.getSymbol().isFlag(Flags.INTERFACE)) {
        anonymousClassType.interfaces = ImmutableList.of(type);
        anonymousClassType.supertype = symbols.objectType;
      } else {
        anonymousClassType.supertype = type;
        anonymousClassType.interfaces = ImmutableList.of();
      }
      scan(classBody);
      registerType(tree, anonymousClassType);
    } else {
      registerType(tree, getType(tree.identifier()));
    }
  }

  private JavaSymbol resolveConstructorSymbol(IdentifierTree identifier, Resolve.Env methodEnv, List<JavaType> argTypes) {
    JavaSymbol symbol = resolve.findMethod(methodEnv, (JavaType) identifier.symbolType(), "<init>", argTypes).symbol();
    associateReference(identifier, symbol);
    return symbol;
  }

  @Override
  public void visitPrimitiveType(PrimitiveTypeTree tree) {
    Resolve.Env primitiveEnv = env;
    if (env == null) {
      primitiveEnv = semanticModel.getEnv(tree);
    }
    registerType(tree, resolve.findIdent(primitiveEnv, tree.keyword().text(), JavaSymbol.TYP).type());
  }

  @Override
  public void visitVariable(VariableTree tree) {
    scan(tree.modifiers());
    completeMetadata(((VariableTreeImpl) tree).getSymbol(), tree.modifiers().annotations());
    //skip type, it has been resolved in second pass
    if (tree.initializer() != null) {
      resolveAs(tree.initializer(), JavaSymbol.VAR);
    }
  }

  /**
   * Computes type of an assignment expression. Which is always a type of lvalue.
   * For example in case of {@code double d; int i; res = d = i;} type of assignment expression {@code d = i} is double.
   */
  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    resolveAs(tree.variable(), JavaSymbol.VAR);
    resolveAs(tree.expression(), JavaSymbol.VAR);
    JavaType type = getType(tree.variable());
    registerType(tree, type);
  }

  @Override
  public void visitLiteral(LiteralTree tree) {
    JavaType type = typesOfLiterals.get(tree.kind());
    registerType(tree, type);
  }

  @Override
  public void visitUnaryExpression(UnaryExpressionTree tree) {
    resolveAs(tree.expression(), JavaSymbol.VAR);
    registerType(tree, getType(tree.expression()));
  }

  @Override
  public void visitArrayType(ArrayTypeTree tree) {
    //FIXME(benzonico) respect invariant to set type. Required for cases like : int i[],j[]; Compute array element type only if not previously computed.
    if (getType(tree.type()) == null) {
      resolveAs(tree.type(), JavaSymbol.TYP);
    }
    registerType(tree, new JavaType.ArrayJavaType(getType(tree.type()), symbols.arrayClass));
  }

  @Override
  public void visitTypeCast(TypeCastTree tree) {
    resolveAs(tree.type(), JavaSymbol.TYP);
    resolveAs(tree.expression(), JavaSymbol.VAR);
    registerType(tree, getType(tree.type()));
  }

  @Override
  public void visitUnionType(UnionTypeTree tree) {
    resolveAs((List<? extends Tree>) tree.typeAlternatives(), JavaSymbol.TYP);
    //TODO compute type of union type: lub(alternatives) cf JLS8 14.20
    registerType(tree, Symbols.unknownType);
  }

  @Override
  public void visitEnumConstant(EnumConstantTree tree) {
    scan(tree.modifiers());
    NewClassTree newClassTree = tree.initializer();
    scan(newClassTree.enclosingExpression());
    // register identifier type
    registerType(newClassTree.identifier(), ((VariableTreeImpl) tree).getSymbol().getType());
    scan(newClassTree.typeArguments());
    scan(newClassTree.arguments());
    ClassTree classBody = newClassTree.classBody();
    if(classBody != null) {
      scan(classBody);
      ((JavaType.ClassJavaType) classBody.symbol().type()).supertype = getType(newClassTree.identifier());
    }
    resolveConstructorSymbol(tree.simpleName(), semanticModel.getEnv(tree), getParameterTypes(newClassTree.arguments()));
  }

  @Override
  public void visitAnnotation(AnnotationTree tree) {
    if (((AbstractTypedTree) tree.annotationType()).isTypeSet()) {
      //FIXME: annotation type is set, so we skip this annotation as it was already visited.
      // This handle the case where type and its annotation is shared between two variables.
      return;
    }
    resolveAs(tree.annotationType(), JavaSymbol.TYP);
    Arguments arguments = tree.arguments();
    if (arguments.size() > 1 || (!arguments.isEmpty() && arguments.get(0).is(Tree.Kind.ASSIGNMENT))) {
      // resolve by identifying correct identifier in assignment.
      for (ExpressionTree expressionTree : arguments) {
        AssignmentExpressionTree aet = (AssignmentExpressionTree) expressionTree;
        IdentifierTree variable = (IdentifierTree) aet.variable();
        JavaSymbol identInType = resolve.findMethod(semanticModel.getEnv(tree), getType(tree.annotationType()), variable.name(), ImmutableList.<JavaType>of()).symbol();
        associateReference(variable, identInType);
        JavaType type = identInType.type;
        if (type == null) {
          type = Symbols.unknownType;
        }
        registerType(variable, type);
        resolveAs(aet.expression(), JavaSymbol.VAR);
      }
    } else {
      for (ExpressionTree expressionTree : arguments) {
        resolveAs(expressionTree, JavaSymbol.VAR);
      }
    }
    registerType(tree, getType(tree.annotationType()));
  }

  @Override
  public void visitIdentifier(IdentifierTree tree) {
    if (!((AbstractTypedTree) tree).isTypeSet()) {
      resolveAs(tree, JavaSymbol.VAR);
    }
  }

  @Override
  public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
    if (!((AbstractTypedTree) tree).isTypeSet()) {
      resolveAs(tree, JavaSymbol.VAR);
    }
  }

  @Override
  public void visitMethodReference(MethodReferenceTree methodReferenceTree) {
    resolveAs(methodReferenceTree.expression(), JavaSymbol.VAR | JavaSymbol.TYP);
    //TODO resolve which method it is refered to
    registerType(methodReferenceTree.method(), Symbols.unknownType);
    registerType(methodReferenceTree, Symbols.unknownType);
    scan(methodReferenceTree.typeArguments());
    scan(methodReferenceTree.method());
  }

  @Override
  public void visitOther(Tree tree) {
    registerType(tree, Symbols.unknownType);
  }

  @VisibleForTesting
  JavaType getType(Tree tree) {
    return types.get(tree);
  }

  private void registerType(Tree tree, JavaType type) {
    if (AbstractTypedTree.class.isAssignableFrom(tree.getClass())) {
      ((AbstractTypedTree) tree).setType(type);
    }
    types.put(tree, type);
  }

  private void associateReference(IdentifierTree tree, JavaSymbol symbol) {
    if (symbol.kind < JavaSymbol.ERRONEOUS) {
      semanticModel.associateReference(tree, symbol);
      ((IdentifierTreeImpl) tree).setSymbol(symbol);
      symbol.addUsage(tree);
    }
  }

}
