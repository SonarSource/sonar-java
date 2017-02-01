/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java.resolve;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.expression.ConditionalExpressionTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.LambdaExpressionTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.model.expression.MethodReferenceTreeImpl;
import org.sonar.java.model.expression.NewClassTreeImpl;
import org.sonar.java.model.expression.ParenthesizedTreeImpl;
import org.sonar.java.model.expression.TypeArgumentListTreeImpl;
import org.sonar.java.resolve.Resolve.Resolution;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
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
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeArguments;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.UnionTypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WildcardTree;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
  public void visitMethodInvocation(MethodInvocationTree tree) {
    MethodInvocationTreeImpl mit = (MethodInvocationTreeImpl) tree;
    Resolve.Env methodEnv = semanticModel.getEnv(tree);
    if(mit.isTypeSet() && mit.symbol().isMethodSymbol()) {
      TypeSubstitution typeSubstitution = inferedSubstitution(mit);
      List<JavaType> argTypes = getParameterTypes(tree.arguments());
      JavaSymbol.MethodJavaSymbol methodSymbol = (JavaSymbol.MethodJavaSymbol) mit.symbol();
      List<JavaType> formals = methodSymbol.parameterTypes().stream().map(t -> (JavaType) t).collect(Collectors.toList());
      List<JavaType> inferedArgTypes = resolve.resolveTypeSubstitution(formals, typeSubstitution);
      int size = inferedArgTypes.size();
      IntStream.range(0, argTypes.size()).forEach(
        i -> {
          JavaType arg = argTypes.get(i);
          Type formal = inferedArgTypes.get(Math.min(i, size - 1));
          if (formal != arg) {
            AbstractTypedTree argTree = (AbstractTypedTree) mit.arguments().get(i);
            argTree.setInferedType(formal);
            argTree.accept(this);
          }
        }
      );
      List<JavaType> typeParamTypes = getParameterTypes(tree.typeArguments());
      inferReturnTypeFromInferedArgs(tree, methodEnv, argTypes, typeParamTypes, (JavaType) mit.symbolType(), typeSubstitution);
      return;
    }
    scan(tree.arguments());
    scan(tree.typeArguments());
    List<JavaType> argTypes = getParameterTypes(tree.arguments());
    List<JavaType> typeParamTypes = getParameterTypes(tree.typeArguments());
    Resolve.Resolution resolution = resolveMethodSymbol(tree.methodSelect(), methodEnv, argTypes, typeParamTypes);
    JavaSymbol symbol;
    JavaType returnType;
    if(resolution == null) {
      returnType = symbols.deferedType(mit);
      symbol = Symbols.unknownSymbol;
    } else {
      symbol = resolution.symbol();
      returnType = resolution.type();
      if(symbol.isMethodSymbol()) {
        MethodJavaType methodType = (MethodJavaType) resolution.type();
        returnType = methodType.resultType;
      }
    }
    mit.setSymbol(symbol);
    if(returnType != null && returnType.isTagged(JavaType.DEFERRED)) {
      ((DeferredType) returnType).setTree(mit);
    }
    registerType(tree, returnType);
    if(resolution != null) {
      inferArgumentTypes(argTypes, resolution);
      inferReturnTypeFromInferedArgs(tree, methodEnv, argTypes, typeParamTypes, returnType, new TypeSubstitution());
    }
  }

  private void inferReturnTypeFromInferedArgs(MethodInvocationTree tree, Resolve.Env methodEnv, List<JavaType> argTypes, List<JavaType> typeParamTypes,
                                              JavaType returnType, TypeSubstitution typeSubstitution) {
    List<JavaType> parameterTypes = getParameterTypes(tree.arguments());
    Resolution resolution = null;
    Tree methodSelect = tree.methodSelect();
    if(!parameterTypes.equals(argTypes)) {
      IdentifierTree identifier;
      if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree mset = (MemberSelectExpressionTree) methodSelect;
        JavaType type = getType(mset.expression());
        if(type.isTagged(JavaType.DEFERRED)) {
          throw new IllegalStateException("type of arg should not be defered anymore ??");
        }
        identifier = mset.identifier();
        resolution = resolve.findMethod(methodEnv, type, identifier.name(), parameterTypes, typeParamTypes);
      } else if (methodSelect.is(Tree.Kind.IDENTIFIER)) {
        identifier = (IdentifierTree) methodSelect;
        resolution = resolve.findMethod(methodEnv, identifier.name(), parameterTypes, typeParamTypes);
      }
      if(resolution != null && returnType != resolution.type() && resolution.symbol().isMethodSymbol()) {
        MethodJavaType methodType = (MethodJavaType) resolution.type();
        if(!methodType.resultType.isTagged(JavaType.DEFERRED)) {
          registerType(tree, resolve.applySubstitution(methodType.resultType, typeSubstitution));
        }
      }
    } else {
      registerType(tree, resolve.applySubstitution(returnType, typeSubstitution));
    }
  }

  private static TypeSubstitution inferedSubstitution(MethodInvocationTreeImpl mit) {
    JavaSymbol.MethodJavaSymbol methodSymbol = (JavaSymbol.MethodJavaSymbol) mit.symbol();
    JavaType methodReturnedType = (JavaType) mit.symbolType();
    TypeSubstitution typeSubstitution = new TypeSubstitution();
    if(methodReturnedType.isTagged(JavaType.PARAMETERIZED)) {
      JavaType resultType = ((MethodJavaType) methodSymbol.type).resultType;
      if(resultType.isTagged(JavaType.PARAMETERIZED)) {
        typeSubstitution =((ParametrizedTypeJavaType) resultType).typeSubstitution.combine(((ParametrizedTypeJavaType) methodReturnedType).typeSubstitution);
      } else if(resultType.isTagged(JavaType.TYPEVAR)) {
        typeSubstitution.add((TypeVariableJavaType) resultType, methodReturnedType);
      }
    }
    return typeSubstitution;
  }

  private void setInferedType(Type infered, DeferredType deferredType) {
    AbstractTypedTree inferedExpression = deferredType.tree();
    Type newType = infered;
    if (inferedExpression.is(Tree.Kind.NEW_CLASS)) {
      Type newClassType = ((NewClassTree) inferedExpression).identifier().symbolType();
      if(((JavaType) newClassType).isParameterized()) {
        newType = resolve.resolveTypeSubstitutionWithDiamondOperator((ParametrizedTypeJavaType) newClassType, (JavaType) infered);
      }
    }
    inferedExpression.setInferedType(newType);
    inferedExpression.accept(this);
  }

  private static List<JavaType> getParameterTypes(@Nullable List<? extends Tree> args) {
    if(args == null) {
      return new ArrayList<>();
    }
    return args.stream().map(e ->
      ((AbstractTypedTree) e).isTypeSet() ?
        (JavaType) ((AbstractTypedTree) e).symbolType() : Symbols.unknownType).collect(Collectors.toList());
  }

  @CheckForNull
  private Resolve.Resolution resolveMethodSymbol(Tree methodSelect, Resolve.Env methodEnv, List<JavaType> argTypes, List<JavaType> typeParamTypes) {
    Resolve.Resolution resolution;
    IdentifierTree identifier;
    if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mset = (MemberSelectExpressionTree) methodSelect;
      resolveAs(mset.expression(), JavaSymbol.TYP | JavaSymbol.VAR);
      JavaType type = getType(mset.expression());
      if(type.isTagged(JavaType.DEFERRED)) {
        return null;
      }
      identifier = mset.identifier();
      resolution = resolve.findMethod(methodEnv, type, identifier.name(), argTypes, typeParamTypes);
    } else if (methodSelect.is(Tree.Kind.IDENTIFIER)) {
      identifier = (IdentifierTree) methodSelect;
      resolution = resolve.findMethod(methodEnv, identifier.name(), argTypes, typeParamTypes);
    } else {
      throw new IllegalStateException("Method select in method invocation is not of the expected type " + methodSelect);
    }
    registerType(identifier, resolution.type());
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
          // resolve type of expression xxx.class
          return resolveClassType(tree, resolveEnv, mse);
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
      type = symbols.deferedType((AbstractTypedTree) tree);
      registerType(tree, type);
    }
    if (type == null) {
      throw new IllegalStateException("Type not resolved " + tree);
    }
    return type.symbol;
  }

  private JavaSymbol resolveClassType(Tree tree, Resolve.Env resolveEnv, MemberSelectExpressionTree mse) {
    resolveAs(mse.expression(), JavaSymbol.TYP, resolveEnv);
    // member select ending with .class
    JavaType expressionType = getType(mse.expression());
    if (expressionType.isPrimitive()) {
      expressionType = expressionType.primitiveWrapperType();
    }
    TypeSubstitution typeSubstitution = new TypeSubstitution();
    typeSubstitution.add(symbols.classType.getSymbol().typeVariableTypes.get(0), expressionType);
    JavaType parametrizedClassType = parametrizedTypeCache.getParametrizedTypeType(symbols.classType.symbol, typeSubstitution);
    registerType(tree, parametrizedClassType);
    return parametrizedClassType.symbol;
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

    registerType(tree, parametrizedTypeWithTypeArguments(getType(tree.type()).getSymbol(), tree.typeArguments()));
  }

  private JavaType parametrizedTypeWithTypeArguments(JavaSymbol.TypeJavaSymbol symbol, TypeArguments typeArguments) {
    TypeSubstitution typeSubstitution = new TypeSubstitution();
    if (typeArguments.size() <= symbol.typeVariableTypes.size()) {
      for (int i = 0; i < typeArguments.size(); i++) {
        typeSubstitution.add(symbol.typeVariableTypes.get(i), getType(typeArguments.get(i)));
      }
    }
    return parametrizedTypeCache.getParametrizedTypeType(symbol, typeSubstitution);
  }

  @Override
  public void visitWildcard(WildcardTree tree) {
    if (tree.is(Tree.Kind.UNBOUNDED_WILDCARD)) {
      registerType(tree, symbols.unboundedWildcard);
    } else {
      resolveAs(tree.bound(), JavaSymbol.TYP);
      JavaType bound = getType(tree.bound());
      WildCardType.BoundType boundType = tree.is(Tree.Kind.SUPER_WILDCARD) ? WildCardType.BoundType.SUPER : WildCardType.BoundType.EXTENDS;
      registerType(tree, parametrizedTypeCache.getWildcardType(bound, boundType));
    }
  }

  @Override
  public void visitConditionalExpression(ConditionalExpressionTree tree) {
    if(((ConditionalExpressionTreeImpl) tree).isTypeSet()) {
      JavaType trueType = getType(tree.trueExpression());
      if(trueType.isTagged(JavaType.DEFERRED)) {
        setInferedType(tree.symbolType(), (DeferredType) trueType);
      }
      JavaType falseType = getType(tree.falseExpression());
      if(falseType.isTagged(JavaType.DEFERRED)) {
        setInferedType(tree.symbolType(), (DeferredType) falseType);
      }
    } else {
      resolveAs(tree.condition(), JavaSymbol.VAR);
      resolveAs(tree.trueExpression(), JavaSymbol.VAR);
      resolveAs(tree.falseExpression(), JavaSymbol.VAR);

      registerType(tree, resolve.conditionalExpressionType(tree, (JavaType) tree.trueExpression().symbolType(),(JavaType) tree.falseExpression().symbolType()));
    }
  }

  @Override
  public void visitThrowStatement(ThrowStatementTree tree) {
    resolveAs(tree.expression(), JavaSymbol.VAR);
  }

  @Override
  public void visitLambdaExpression(LambdaExpressionTree tree) {
    LambdaExpressionTreeImpl lambdaExpressionTree = (LambdaExpressionTreeImpl) tree;
    if (lambdaExpressionTree.isTypeSet()) {
      // type should be tied to a SAM interface
      JavaType lambdaType = (JavaType) lambdaExpressionTree.symbolType();
      List<JavaType> samMethodArgs = resolve.findSamMethodArgs(lambdaType);
      for (int i = 0; i < samMethodArgs.size(); i++) {
        VariableTree param = lambdaExpressionTree.parameters().get(i);
        if (param.type().is(Tree.Kind.INFERED_TYPE)) {
          JavaType inferedType = samMethodArgs.get(i);
          if(inferedType.isTagged(JavaType.WILDCARD)) {
            // JLS8 18.5.3
            inferedType = ((WildCardType) inferedType).bound;
          }
          ((AbstractTypedTree) param.type()).setInferedType(inferedType);
          ((JavaSymbol.VariableJavaSymbol) param.symbol()).type = inferedType;
        }
      }
      super.visitLambdaExpression(tree);
      if(lambdaType.isUnknown() || lambdaType.isTagged(JavaType.DEFERRED)) {
        return;
      }
      refineLambdaType(lambdaExpressionTree, lambdaType);
    } else {
      registerType(tree, symbols.deferedType(lambdaExpressionTree));
    }
  }

  private void refineLambdaType(LambdaExpressionTreeImpl lambdaExpressionTree, JavaType lambdaType) {
    Optional<JavaSymbol.MethodJavaSymbol> samMethod = resolve.getSamMethod(lambdaType);
    if (!samMethod.isPresent()) {
      return;
    }
    JavaType samReturnType = (JavaType) samMethod.get().returnType().type();
    JavaType capturedReturnType = resolve.resolveTypeSubstitution(samReturnType, lambdaType);
    if (capturedReturnType.is("void") || !lambdaType.isParameterized()) {
      return;
    }
    JavaType refinedReturnType = capturedReturnType;
    if (lambdaExpressionTree.body().is(Tree.Kind.BLOCK)) {
      LambdaBlockReturnVisitor lambdaBlockReturnVisitor = new LambdaBlockReturnVisitor();
      lambdaExpressionTree.body().accept(lambdaBlockReturnVisitor);
      if(!lambdaBlockReturnVisitor.types.isEmpty()) {
        refinedReturnType = (JavaType) resolve.leastUpperBound(lambdaBlockReturnVisitor.types);
      }
    } else {
      refinedReturnType = (JavaType) ((AbstractTypedTree) lambdaExpressionTree.body()).symbolType();
    }
    refineType(lambdaExpressionTree, lambdaType, capturedReturnType, refinedReturnType);
  }

  private void refineType(AbstractTypedTree expression, JavaType expressionType, JavaType capturedReturnType, JavaType refinedReturnType) {
    if (refinedReturnType != capturedReturnType) {
      // found a lambda return type different from the one infered : update infered type
      if (expressionType.isTagged(JavaType.PARAMETERIZED)) {
        ParametrizedTypeJavaType functionType = (ParametrizedTypeJavaType) resolve.functionType((ParametrizedTypeJavaType) expressionType);
        TypeSubstitution typeSubstitution = ((ParametrizedTypeJavaType) expressionType).typeSubstitution;
        typeSubstitution.substitutionEntries().stream()
          .filter(e -> e.getValue() == capturedReturnType)
          .map(Map.Entry::getKey)
          .findFirst()
          .ifPresent(t -> {
            if(refinedReturnType instanceof DeferredType) {
              setInferedType(functionType.typeSubstitution.substitutedType(t), (DeferredType) refinedReturnType);
            } else  {
              TypeSubstitution refinedSubstitution = new TypeSubstitution(typeSubstitution).add(t, refinedReturnType);
              JavaType refinedType = parametrizedTypeCache.getParametrizedTypeType(expressionType.symbol, refinedSubstitution);
              expression.setType(refinedType);
            }
          });
      } else {
        expression.setType(refinedReturnType);
      }
    }
  }

  @Override
  public void visitReturnStatement(ReturnStatementTree tree) {
    super.visitReturnStatement(tree);
    ExpressionTree expression = tree.expression();
    if (expression != null && ((JavaType) expression.symbolType()).isTagged(JavaType.DEFERRED)) {
      // get owner of return (method or lambda)
      Tree parent = tree.parent();
      while (!parent.is(Tree.Kind.METHOD, Tree.Kind.LAMBDA_EXPRESSION)) {
        parent = parent.parent();
        if(parent == null) {
          throw new IllegalStateException("Return statement was unexpected here");
        }
      }
      Type infered;
      if(parent.is(Tree.Kind.METHOD)) {
        infered = ((MethodTree) parent).returnType().symbolType();
      } else {
        infered = ((LambdaExpressionTree) parent).symbolType();
      }
      setInferedType(infered, (DeferredType) expression.symbolType());
    }
  }

  @Override
  public void visitNewArray(NewArrayTree tree) {
    resolveAs(tree.type(), JavaSymbol.TYP);
    scan(tree.dimensions());
    resolveAs((List<? extends Tree>) tree.initializers(), JavaSymbol.VAR);
    JavaType type = getType(tree.type());
    int dimensions = tree.dimensions().size();
    // TODO why?
    type = new ArrayJavaType(type, symbols.arrayClass);
    for (int i = 1; i < dimensions; i++) {
      type = new ArrayJavaType(type, symbols.arrayClass);
    }
    registerType(tree, type);
  }

  @Override
  public void visitParenthesized(ParenthesizedTree tree) {
    if(((ParenthesizedTreeImpl) tree).isTypeSet()) {
      JavaType expType = getType(tree.expression());
      if(expType.isTagged(JavaType.DEFERRED)) {
        setInferedType(tree.symbolType(), (DeferredType) expType);
      }
    } else {
      resolveAs(tree.expression(), JavaSymbol.VAR);
      JavaType parenthesizedExpressionType = getType(tree.expression());
      if(parenthesizedExpressionType.isTagged(JavaType.DEFERRED)) {
        parenthesizedExpressionType = symbols.deferedType((AbstractTypedTree) tree);
      }
      registerType(tree, parenthesizedExpressionType);
    }
  }

  @Override
  public void visitArrayAccessExpression(ArrayAccessExpressionTree tree) {
    resolveAs(tree.expression(), JavaSymbol.VAR);
    scan(tree.dimension());
    JavaType type = getType(tree.expression());
    if (type != null && type.tag == JavaType.ARRAY) {
      registerType(tree, ((ArrayJavaType) type).elementType);
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
    super.visitBinaryExpression(tree);
    JavaType left = getType(tree.leftOperand());
    JavaType right = getType(tree.rightOperand());
    // TODO avoid nulls
    if (left == null || right == null) {
      registerType(tree, Symbols.unknownType);
      return;
    }
    if("+".equals(tree.operatorToken().text()) && (left == symbols.stringType || right == symbols.stringType)) {
      registerType(tree, symbols.stringType);
      return;
    }
    JavaSymbol symbol = resolve.findMethod(semanticModel.getEnv(tree), symbols.predefClass.type, tree.operatorToken().text(), ImmutableList.of(left, right)).symbol();
    if (symbol.kind != JavaSymbol.MTH) {
      // not found
      registerType(tree, Symbols.unknownType);
      return;
    }
    registerType(tree, ((MethodJavaType) symbol.type).resultType);
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    NewClassTreeImpl newClassTreeImpl = (NewClassTreeImpl) tree;
    if (newClassTreeImpl.isTypeSet()) {
      return;
    }
    List<JavaType> typeArgumentsTypes = ImmutableList.of();
    if (tree.typeArguments() != null) {
      resolveAs((List<Tree>) tree.typeArguments(), JavaSymbol.TYP);
      typeArgumentsTypes = tree.typeArguments().stream().map(this::getType).collect(Collectors.toList());
    }
    resolveAs((List<ExpressionTree>) tree.arguments(), JavaSymbol.VAR);
    List<JavaType> parameterTypes = getParameterTypes(tree.arguments());
    Resolve.Env newClassEnv = semanticModel.getEnv(tree);
    ExpressionTree enclosingExpression = tree.enclosingExpression();

    TypeTree typeTree = tree.identifier();
    IdentifierTree constructorIdentifier = newClassTreeImpl.getConstructorIdentifier();
    JavaType identifierType = resolveIdentifierType(newClassEnv, enclosingExpression, typeTree, constructorIdentifier.name());
    JavaSymbol.TypeJavaSymbol constructorIdentifierSymbol = (JavaSymbol.TypeJavaSymbol) identifierType.symbol();
    parameterTypes = addImplicitOuterClassParameter(parameterTypes, constructorIdentifierSymbol);
    Resolution resolution = resolveConstructorSymbol(constructorIdentifier, identifierType, newClassEnv, parameterTypes, typeArgumentsTypes);
    ClassTree classBody = tree.classBody();
    JavaType constructedType = identifierType;
    if (classBody != null) {
      ClassJavaType anonymousClassType = (ClassJavaType) classBody.symbol().type();
      if (identifierType.getSymbol().isInterface()) {
        anonymousClassType.interfaces = ImmutableList.of(identifierType);
        anonymousClassType.supertype = symbols.objectType;
      } else {
        anonymousClassType.supertype = identifierType;
        anonymousClassType.interfaces = ImmutableList.of();
      }
      anonymousClassType.symbol.members.enter(new JavaSymbol.VariableJavaSymbol(Flags.FINAL, "super", anonymousClassType.supertype, anonymousClassType.symbol));
      scan(classBody);
      constructedType = anonymousClassType;
    } else if (resolution.symbol().isMethodSymbol()) {
      constructedType = ((MethodJavaType) resolution.type()).resultType;
      if (constructedType.isTagged(JavaType.DEFERRED)) {
        Tree parent = newClassTreeImpl.parent();
        if (parent.is(Tree.Kind.MEMBER_SELECT)) {
          constructedType = resolve.parametrizedTypeWithErasure((ParametrizedTypeJavaType) identifierType);
        } else {
          // will be resolved by type inference
          ((DeferredType) constructedType).setTree(newClassTreeImpl);
        }
      }
    }
    registerType(tree, constructedType);
  }

  private JavaType resolveIdentifierType(Resolve.Env newClassEnv, @Nullable ExpressionTree enclosingExpression, TypeTree typeTree, String typeName) {
    if (enclosingExpression != null) {
      resolveAs(enclosingExpression, JavaSymbol.VAR);
      Resolution idType = resolve.findIdentInType(newClassEnv, (JavaSymbol.TypeJavaSymbol) enclosingExpression.symbolType().symbol(), typeName, JavaSymbol.TYP);
      JavaType type = idType.type();
      if (typeTree.is(Tree.Kind.PARAMETERIZED_TYPE)) {
        TypeArguments typeArguments = ((ParameterizedTypeTree) typeTree).typeArguments();
        scan(typeArguments);
        type = parametrizedTypeWithTypeArguments(type.symbol, typeArguments);
      }
      registerType(typeTree, type);
    } else {
      resolveAs(typeTree, JavaSymbol.TYP, newClassEnv, false);
    }
    return (JavaType) typeTree.symbolType();
  }

  private static List<JavaType> addImplicitOuterClassParameter(List<JavaType> parameterTypes, JavaSymbol.TypeJavaSymbol constructorIdentifierSymbol) {
    List<JavaType> result = parameterTypes;
    JavaSymbol owner = constructorIdentifierSymbol.owner();
    if (!owner.isPackageSymbol() && !constructorIdentifierSymbol.isStatic()) {
      result = ImmutableList.<JavaType>builder().add(owner.enclosingClass().type).addAll(parameterTypes).build();
    }
    return result;
  }

  @Override
  public void visitExpressionStatement(ExpressionStatementTree tree) {
    super.visitExpressionStatement(tree);
    ExpressionTree expression = tree.expression();
    if (((JavaType) expression.symbolType()).isTagged(JavaType.DEFERRED) && expression.is(Tree.Kind.NEW_CLASS)) {
      JavaType parametrizedTypeWithObject = resolve.parametrizedTypeWithErasure((ParametrizedTypeJavaType) getType(((NewClassTree) expression).identifier()));
      setInferedType(parametrizedTypeWithObject, (DeferredType) expression.symbolType());
    }
  }

  private Resolve.Resolution resolveConstructorSymbol(IdentifierTree identifier, Type type, Resolve.Env methodEnv, List<JavaType> argTypes) {
    return resolveConstructorSymbol(identifier, type, methodEnv, argTypes, ImmutableList.of());
  }

  private Resolve.Resolution resolveConstructorSymbol(IdentifierTree identifier, Type type, Resolve.Env methodEnv, List<JavaType> argTypes, List<JavaType> typeArgumentsTypes) {
    Resolve.Resolution resolution = resolve.findMethod(methodEnv, (JavaType) type, "<init>", argTypes, typeArgumentsTypes);
    JavaSymbol symbol = resolution.symbol();
    inferArgumentTypes(argTypes, resolution);
    associateReference(identifier, symbol);
    return resolution;
  }

  private void inferArgumentTypes(List<JavaType> argTypes, Resolve.Resolution resolution) {
    Type formal = Symbols.unknownType;
    for (int i = 0; i < argTypes.size(); i++) {
      JavaType arg = argTypes.get(i);
      if (resolution.symbol().isMethodSymbol()) {
        List<JavaType> resolvedFormals = ((MethodJavaType) resolution.type()).argTypes;
        int size = resolvedFormals.size();
        formal = resolvedFormals.get((i < size) ? i : (size - 1));
      }
      if (arg.isTagged(JavaType.DEFERRED)) {
        setInferedType(formal, (DeferredType) arg);
      }
    }
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
    ExpressionTree initializer = tree.initializer();
    if (initializer != null) {
      resolveAs(initializer, JavaSymbol.VAR);
      if(((JavaType) initializer.symbolType()).isTagged(JavaType.DEFERRED)) {
        setInferedType(tree.type().symbolType(), (DeferredType) initializer.symbolType());
      }
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
    if(((JavaType) tree.expression().symbolType()).isTagged(JavaType.DEFERRED)) {
      setInferedType(type, (DeferredType) tree.expression().symbolType());
    }
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
    scan(tree.annotations());
    registerType(tree, new ArrayJavaType(getType(tree.type()), symbols.arrayClass));
  }

  @Override
  public void visitTypeCast(TypeCastTree tree) {
    resolveAs(tree.type(), JavaSymbol.TYP);
    resolveAs(tree.expression(), JavaSymbol.VAR);
    JavaType castType = getType(tree.type());
    Type expressionType = tree.expression().symbolType();
    if(expressionType instanceof DeferredType) {
      setInferedType(castType, (DeferredType) expressionType);
    }
    registerType(tree, castType);
  }

  @Override
  public void visitUnionType(UnionTypeTree tree) {
    resolveAs((List<TypeTree>) tree.typeAlternatives(), JavaSymbol.TYP);
    ImmutableSet.Builder<Type> uniontype = ImmutableSet.builder();
    for (TypeTree typeTree : tree.typeAlternatives()) {
      uniontype.add(typeTree.symbolType());
    }
    registerType(tree, (JavaType) resolve.leastUpperBound(uniontype.build()));
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
      ((ClassJavaType) classBody.symbol().type()).supertype = getType(newClassTree.identifier());
    }
    resolveConstructorSymbol(tree.simpleName(), newClassTree.identifier().symbolType(), semanticModel.getEnv(tree), getParameterTypes(newClassTree.arguments()));
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
    resolveAs(tree, JavaSymbol.VAR);
  }

  @Override
  public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
    if (!((AbstractTypedTree) tree).isTypeSet()) {
      resolveAs(tree, JavaSymbol.VAR);
    }
  }

  @Override
  public void visitMethodReference(MethodReferenceTree methodReferenceTree) {
    MethodReferenceTreeImpl methodRefTree = (MethodReferenceTreeImpl) methodReferenceTree;
    if (methodRefTree.isTypeSet() && methodReferenceTree.typeArguments() == null) {
      resolve.getSamMethod((JavaType) methodRefTree.symbolType()).ifPresent(samMethod -> resolveMethodReference(samMethod, methodRefTree));
    } else {
      // TODO : SONARJAVA-1663 : consider type arguments for method resolution and substitution
      scan(methodReferenceTree.typeArguments());
      resolveAs(methodReferenceTree.expression(), JavaSymbol.VAR | JavaSymbol.TYP);
      registerType(methodRefTree, symbols.deferedType(methodRefTree));
    }
  }

  private void resolveMethodReference(JavaSymbol.MethodJavaSymbol samMethod, MethodReferenceTreeImpl methodRefTree) {
    JavaType methodRefType = (JavaType) methodRefTree.symbolType();
    JavaType samReturnType = (JavaType) samMethod.returnType().type();
    List<JavaType> samMethodArgs = resolve.findSamMethodArgs(methodRefType);
    Resolution resolution = resolve.findMethodReference(semanticModel.getEnv(methodRefTree), samMethodArgs, methodRefTree);
    JavaSymbol methodSymbol = resolution.symbol();
    if (methodSymbol.isMethodSymbol()) {
      IdentifierTree methodIdentifier = methodRefTree.method();
      addMethodRefReference(methodIdentifier, methodSymbol);
      setMethodRefType(methodRefTree, methodRefType, resolution.type());

      JavaType capturedReturnType = resolve.resolveTypeSubstitution(samReturnType, methodRefType);
      JavaType refinedReturnType = ((MethodJavaType) methodIdentifier.symbolType()).resultType();
      if ("<init>".equals(methodSymbol.name)) {
        refinedReturnType = refinedTypeForConstructor(capturedReturnType, refinedReturnType);
      }
      if (refinedReturnType instanceof DeferredType) {
        ((DeferredType) refinedReturnType).setTree((AbstractTypedTree) methodRefTree.method());
      }
      refineType(methodRefTree, methodRefType, capturedReturnType, refinedReturnType);
    } else {
      handleNewArray(methodRefTree, methodRefType, samReturnType);
    }
  }

  private static void addMethodRefReference(IdentifierTree methodIdentifier, JavaSymbol methodSymbol) {
    ((IdentifierTreeImpl) methodIdentifier).setSymbol(methodSymbol);
    methodSymbol.addUsage(methodIdentifier);
  }

  private static void setMethodRefType(MethodReferenceTree methodRef, JavaType methodRefType, JavaType methodType) {
    ((AbstractTypedTree) methodRef).setType(methodRefType);
    ((AbstractTypedTree) methodRef.method()).setType(methodType);
  }

  private JavaType refinedTypeForConstructor(JavaType capturedReturnType, JavaType refinedReturnType) {
    JavaType sanitizedCaptured = capturedReturnType;
    JavaType refinedConstructorType = refinedReturnType;
    if (refinedConstructorType.symbol().isTypeSymbol() && !((JavaSymbol.TypeJavaSymbol) refinedConstructorType.symbol()).typeParameters().scopeSymbols().isEmpty()) {
      refinedConstructorType = parametrizedTypeCache.getParametrizedTypeType(refinedConstructorType.symbol, new TypeSubstitution());
    }
    if (sanitizedCaptured.isTagged(JavaType.TYPEVAR)) {
      sanitizedCaptured = ((TypeVariableJavaType) sanitizedCaptured).bounds.get(0);
    }
    if (refinedConstructorType.isParameterized()) {
      refinedConstructorType = resolve.resolveTypeSubstitutionWithDiamondOperator((ParametrizedTypeJavaType) refinedConstructorType, sanitizedCaptured);
    }
    return refinedConstructorType;
  }

  private void handleNewArray(MethodReferenceTree methodReferenceTree, JavaType methodRefType, JavaType samReturnType) {
    JavaType expressionType = getType(methodReferenceTree.expression());
    if (expressionType != null && expressionType.isArray() && "new".equals(methodReferenceTree.method().name())) {
      JavaType capturedReturnType = resolve.resolveTypeSubstitution(samReturnType, methodRefType);
      refineType((MethodReferenceTreeImpl) methodReferenceTree, methodRefType, capturedReturnType, expressionType);
    }
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

  private static void associateReference(IdentifierTree tree, JavaSymbol symbol) {
    if (symbol.kind < JavaSymbol.ERRONEOUS) {
      ((IdentifierTreeImpl) tree).setSymbol(symbol);
      symbol.addUsage(tree);
    }
  }

}
