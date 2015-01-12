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
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.TypeParameters;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Completes hierarchy of types.
 */
public class SecondPass implements Symbol.Completer {

  private final SemanticModel semanticModel;
  private final Symbols symbols;
  private final TypeAndReferenceSolver typeAndReferenceSolver;

  public SecondPass(SemanticModel semanticModel, Symbols symbols, TypeAndReferenceSolver typeAndReferenceSolver) {
    this.semanticModel = semanticModel;
    this.symbols = symbols;
    this.typeAndReferenceSolver = typeAndReferenceSolver;
  }

  @Override
  public void complete(Symbol symbol) {
    if (symbol.kind == Symbol.TYP) {
      complete((Symbol.TypeSymbol) symbol);
    } else if (symbol.kind == Symbol.MTH) {
      complete((Symbol.MethodSymbol) symbol);
    } else if (symbol.kind == Symbol.VAR) {
      complete((Symbol.VariableSymbol) symbol);
    } else {
      throw new IllegalArgumentException();
    }
  }

  private void complete(Symbol.TypeSymbol symbol) {
    Resolve.Env env = semanticModel.getEnv(symbol);
    Type.ClassType type = (Type.ClassType) symbol.type;

    if ((symbol.flags() & Flags.INTERFACE) == 0) {
      // If this is a class, enter symbol for "this"
      symbol.members.enter(new Symbol.VariableSymbol(Flags.FINAL, "this", symbol.type, symbol));
    }

    if ("".equals(symbol.name)) {
      // Anonymous Class Declaration
      // FIXME(Godin): This case avoids NPE which occurs because semanticModel has no associations for anonymous classes.
      type.interfaces = ImmutableList.of();
      return;
    }

    ClassTree tree = (ClassTree) semanticModel.getTree(symbol);
    completeTypeParameters(tree.typeParameters(), env);

    //Superclass
    Tree superClassTree = tree.superClass();
    if (superClassTree != null) {
      type.supertype = resolveType(env, superClassTree);
      checkHierarchyCycles(symbol.type);
      //enter symbol for super for superclass.
      symbol.members.enter(new Symbol.VariableSymbol(Flags.FINAL, "super", ((Type.ClassType) symbol.type).supertype, symbol));
    } else {
      if (tree.is(Tree.Kind.ENUM)) {
        // JLS8 8.9: The direct superclass of an enum type E is Enum<E>.
        type.supertype = symbols.enumType;
      } else if (tree.is(Tree.Kind.CLASS)) {
        // JLS8 8.1.4: the direct superclass of the class type C<F1,...,Fn> is
        // the type given in the extends clause of the declaration of C
        // if an extends clause is present, or Object otherwise.
        type.supertype = symbols.objectType;
      }
      // JLS8 9.1.3: While every class is an extension of class Object, there is no single interface of which all interfaces are extensions.
    }

    //Interfaces
    ImmutableList.Builder<Type> interfaces = ImmutableList.builder();
    for (Tree interfaceTree : tree.superInterfaces()) {
      Type interfaceType = resolveType(env, interfaceTree);
      if (interfaceType != null) {
        interfaces.add(interfaceType);
      }
    }

    if (tree.is(Tree.Kind.ANNOTATION_TYPE)) {
      // JLS8 9.6: The direct superinterface of every annotation type is java.lang.annotation.Annotation.
      // (Godin): Note that "extends" and "implements" clauses are forbidden by grammar for annotation types
      interfaces.add(symbols.annotationType);
    }

    type.interfaces = interfaces.build();
  }

  private void completeTypeParameters(TypeParameters typeParameters, Resolve.Env env) {
    for (TypeParameterTree typeParameterTree : typeParameters) {
      List<Type> bounds = Lists.newArrayList();
      if(typeParameterTree.bounds().isEmpty()) {
        bounds.add(symbols.objectType);
      } else {
        for (Tree boundTree : typeParameterTree.bounds()) {
          bounds.add(resolveType(env, boundTree));
        }
      }
      ((Type.TypeVariableType) semanticModel.getSymbol(typeParameterTree).type).bounds = bounds;
    }
  }

  private void checkHierarchyCycles(Type baseType) {
    Set<Type.ClassType> types = Sets.newHashSet();
    Type.ClassType type = (Type.ClassType) baseType;
    while (type != null) {
      if (!types.add(type)) {
        throw new IllegalStateException("Cycling class hierarchy detected with symbol : " + baseType.symbol.name + ".");
      }
      type = (Type.ClassType) type.symbol.getSuperclass();
    }
  }

  private void complete(Symbol.MethodSymbol symbol) {
    MethodTree methodTree = (MethodTree) semanticModel.getTree(symbol);
    Resolve.Env env = semanticModel.getEnv(symbol);

    ImmutableList.Builder<Symbol.TypeSymbol> thrown = ImmutableList.builder();
    ImmutableList.Builder<Type> thrownTypes = ImmutableList.builder();
    for (ExpressionTree throwClause : methodTree.throwsClauses()) {
      Type thrownType = resolveType(env, throwClause);
      if (thrownType != null) {
        thrownTypes.add(thrownType);
        thrown.add(((Type.ClassType) thrownType).symbol);
      }
    }
    symbol.thrown = thrown.build();

    Type returnType = null;
    // no return type for constructor
    if (!"<init>".equals(symbol.name)) {
      returnType = resolveType(env, methodTree.returnType());
      if (returnType != null) {
        symbol.type = returnType.symbol;
      }
    }
    List<Type> argTypes = Lists.newArrayList();
    Collection<Symbol> scopeSymbols = symbol.parameters.scopeSymbols();
    // Guarantee order of params.
    for (VariableTree variableTree : methodTree.parameters()) {
      for (Symbol param : scopeSymbols) {
        if (variableTree.simpleName().name().equals(param.getName())) {
          param.complete();
          argTypes.add(param.getType());
        }
      }
      if(((VariableTreeImpl)variableTree).isVararg()) {
        symbol.flags |= Flags.VARARGS;
      }
    }
    Type.MethodType methodType = new Type.MethodType(argTypes, returnType, thrownTypes.build(), (Symbol.TypeSymbol) symbol.owner);
    symbol.setMethodType(methodType);
  }

  private void complete(Symbol.VariableSymbol symbol) {
    VariableTree variableTree = (VariableTree) semanticModel.getTree(symbol);
    Resolve.Env env = semanticModel.getEnv(symbol);
    if (variableTree.is(Tree.Kind.ENUM_CONSTANT)) {
      symbol.type = env.enclosingClass().type;
    } else {
      symbol.type = resolveType(env, variableTree.type());
    }
  }

  private Type resolveType(Resolve.Env env, Tree tree) {
    Preconditions.checkArgument(checkTypeOfTree(tree), "Kind of tree unexpected " + ((JavaTree) tree).getKind());
    //FIXME(benzonico) as long as Variables share the same node type, (int i,j; or worse : int i[], j[];) check nullity to respect invariance.
    Type type = ((AbstractTypedTree) tree).getSymbolType();
    if (type != null) {
      return type;
    }
    typeAndReferenceSolver.env = env;
    typeAndReferenceSolver.resolveAs(tree, Symbol.TYP, env);
    typeAndReferenceSolver.env = null;
    return ((AbstractTypedTree) tree).getSymbolType();
  }

  private boolean checkTypeOfTree(Tree tree) {
    return tree.is(Tree.Kind.MEMBER_SELECT) ||
        tree.is(Tree.Kind.IDENTIFIER) ||
        tree.is(Tree.Kind.PARAMETERIZED_TYPE) ||
        tree.is(Tree.Kind.ARRAY_TYPE) ||
        tree.is(Tree.Kind.UNION_TYPE) ||
        tree.is(Tree.Kind.PRIMITIVE_TYPE) ||
        tree.is(Tree.Kind.INFERED_TYPE);
  }

}
