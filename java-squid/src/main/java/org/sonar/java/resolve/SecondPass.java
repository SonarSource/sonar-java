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
import com.google.common.collect.Sets;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Set;

/**
 * Completes hierarchy of types.
 */
public class SecondPass implements Symbol.Completer {

  private final SemanticModel semanticModel;
  private final Resolve resolve;
  private final Symbols symbols;

  public SecondPass(SemanticModel semanticModel, Resolve resolve, Symbols symbols) {
    this.semanticModel = semanticModel;
    this.resolve = resolve;
    this.symbols = symbols;
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
      // If this is a class, enter symbols for "this" and "super".
      symbol.members.enter(new Symbol.VariableSymbol(Flags.FINAL, "this", symbol.type, symbol));
      // TODO super
    }

    if ("".equals(symbol.name)) {
      // Anonymous Class Declaration
      // FIXME(Godin): This case avoids NPE which occurs because semanticModel has no associations for anonymous classes.
      type.interfaces = ImmutableList.of();
      return;
    }

    ClassTree tree = (ClassTree) semanticModel.getTree(symbol);
    Tree superClassTree = tree.superClass();
    if (superClassTree != null) {
      type.supertype = resolveType(env, superClassTree);
      checkHierarchyCycles(symbol.type);
    } else {
      if (tree.is(Tree.Kind.ENUM)) {
        // JLS8 8.9: The direct superclass of an enum type E is Enum<E>.
        // TODO(Godin): generics
        type.supertype = symbols.enumType;
      } else if (tree.is(Tree.Kind.CLASS)) {
        // JLS8 8.1.4: the direct superclass of the class type C<F1,...,Fn> is
        // the type given in the extends clause of the declaration of C
        // if an extends clause is present, or Object otherwise.
        type.supertype = symbols.objectType;
      }
      // JLS8 9.1.3: While every class is an extension of class Object, there is no single interface of which all interfaces are extensions.
    }

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
    for (ExpressionTree throwClause : methodTree.throwsClauses()) {
      Type thrownType = resolveType(env, throwClause);
      if (thrownType != null) {
        ((AbstractTypedTree) throwClause).setType(thrownType);
        thrown.add(((Type.ClassType) thrownType).symbol);
      }
    }
    symbol.thrown = thrown.build();

    if ("<init>".equals(symbol.name)) {
      // no return type for constructor
      return;
    }
    Type type = resolveType(env, methodTree.returnType());
    if (type != null) {
      symbol.type = type.symbol;
    }
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
    TypeResolverVisitor typeResolverVisitor = new TypeResolverVisitor(env.dup());
    tree.accept(typeResolverVisitor);
    if(typeResolverVisitor.arrayType != null) {
      return typeResolverVisitor.arrayType;
    }
    return castToTypeIfPossible(typeResolverVisitor.site);
  }

  private boolean checkTypeOfTree(Tree tree) {
    return tree.is(Tree.Kind.MEMBER_SELECT) ||
        tree.is(Tree.Kind.IDENTIFIER) ||
        tree.is(Tree.Kind.PARAMETERIZED_TYPE) ||
        tree.is(Tree.Kind.ARRAY_TYPE) ||
        tree.is(Tree.Kind.UNION_TYPE) ||
        tree.is(Tree.Kind.PRIMITIVE_TYPE);
  }

  private Type castToTypeIfPossible(Symbol symbol) {
    return symbol instanceof Symbol.TypeSymbol ? ((Symbol.TypeSymbol) symbol).type : symbols.unknownType;
  }

  private void associateReference(IdentifierTree tree, Symbol symbol) {
    if (symbol.kind < Symbol.ERRONEOUS && semanticModel.getTree(symbol) != null) {
      semanticModel.associateReference(tree, symbol);
    }
  }

  private class TypeResolverVisitor extends BaseTreeVisitor {
    private final Resolve.Env env;
    private Symbol site;
    private Type.ArrayType arrayType;

    public TypeResolverVisitor(Resolve.Env env) {
      this.env = env;
    }

    @Override
    public void visitParameterizedType(ParameterizedTypeTree tree) {
      //Scan only the type, the generic arguments are not yet handled
      scan(tree.type());
    }

    @Override
    public void visitArrayType(ArrayTypeTree tree) {
      super.visitArrayType(tree);
      if(arrayType == null) {
        arrayType = new Type.ArrayType(site.type, symbols.arrayClass);
      }else {
        arrayType = new Type.ArrayType(arrayType, symbols.arrayClass);
      }
    }

    @Override
    public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
      scan(tree.expression());
      if (site.kind >= Symbol.ERRONEOUS) {
        return;
      }
      String name = tree.identifier().name();
      if (site.kind == Symbol.PCK) {
        env.packge = (Symbol.PackageSymbol) site;
        site = resolve.findIdentInPackage(env, site, name, Symbol.TYP | Symbol.PCK);
      } else {
        env.enclosingClass = (Symbol.TypeSymbol) site;
        site = resolve.findMemberType(env, (Symbol.TypeSymbol) site, name, (Symbol.TypeSymbol) site);
      }
      associateReference(tree.identifier(), site);
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      site = resolve.findIdent(env, tree.name(), Symbol.TYP | Symbol.PCK);
      associateReference(tree, site);
    }

    @Override
    public void visitPrimitiveType(PrimitiveTypeTree tree) {
      site = resolve.findIdent(semanticModel.getEnv(tree), tree.keyword().text(), Symbol.TYP);
    }
  }

}
