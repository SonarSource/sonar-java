/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.TypeParameters;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

/**
 * Completes hierarchy of types.
 */
public class SecondPass implements JavaSymbol.Completer {

  private static final String CONSTRUCTOR_NAME = "<init>";

  private final SemanticModel semanticModel;
  private final Symbols symbols;
  private final TypeAndReferenceSolver typeAndReferenceSolver;
  private final ParametrizedTypeCache parametrizedTypeCache;

  public SecondPass(SemanticModel semanticModel, Symbols symbols, ParametrizedTypeCache parametrizedTypeCache, TypeAndReferenceSolver typeAndReferenceSolver) {
    this.semanticModel = semanticModel;
    this.symbols = symbols;
    this.parametrizedTypeCache = parametrizedTypeCache;
    this.typeAndReferenceSolver = typeAndReferenceSolver;
  }

  @Override
  public void complete(JavaSymbol symbol) {
    if (symbol.kind == JavaSymbol.TYP) {
      complete((JavaSymbol.TypeJavaSymbol) symbol);
    } else if (symbol.kind == JavaSymbol.MTH) {
      complete((JavaSymbol.MethodJavaSymbol) symbol);
    } else if (symbol.kind == JavaSymbol.VAR) {
      complete((JavaSymbol.VariableJavaSymbol) symbol);
    } else {
      throw new IllegalArgumentException();
    }
  }

  private void complete(JavaSymbol.TypeJavaSymbol symbol) {
    Resolve.Env env = semanticModel.getEnv(symbol);
    ClassJavaType type = (ClassJavaType) symbol.type;

    if (!symbol.isFlag(Flags.ANNOTATION)) {
      // JLS8 15.8.3 If this is a class or interface (default methods), enter symbol for "this"
      symbol.members.enter(new JavaSymbol.VariableJavaSymbol(Flags.FINAL, "this", symbol.type, symbol));
    }

    if ("".equals(symbol.name)) {
      // Anonymous Class Declaration
      // FIXME(Godin): This case avoids NPE which occurs because semanticModel has no associations for anonymous classes.
      type.interfaces = Collections.emptyList();
      return;
    }

    ClassTree tree = symbol.declaration;
    completeTypeParameters(tree.typeParameters(), env);

    //Interfaces
    ImmutableList.Builder<JavaType> interfaces = ImmutableList.builder();
    tree.superInterfaces().stream().map(interfaceTree -> resolveType(env, interfaceTree)).filter(Objects::nonNull).forEach(interfaces::add);

    if (tree.is(Tree.Kind.ANNOTATION_TYPE)) {
      // JLS8 9.6: The direct superinterface of every annotation type is java.lang.annotation.Annotation.
      // (Godin): Note that "extends" and "implements" clauses are forbidden by grammar for annotation types
      interfaces.add(symbols.annotationType);
    }

    if (tree.is(Tree.Kind.ENUM, Tree.Kind.INTERFACE) && symbol.owner.isKind(JavaSymbol.TYP)) {
      // JSL8 8.9: A nested enum type is implicitly static. It is permitted for the declaration of a nested 
      // enum type to redundantly specify the static modifier.
      symbol.flags |= Flags.STATIC;
    }

    type.interfaces = interfaces.build();

    populateSuperclass(symbol, env, type);

    if ((symbol.flags() & Flags.INTERFACE) == 0) {
      symbol.members.enter(new JavaSymbol.VariableJavaSymbol(Flags.FINAL, "super", type.supertype, symbol));
    } else {
      // JLS9 - 15.12.1 : Used in form 'TypeName.super.foo()', where 'TypeName' is an interface. To support invocation
      // of default methods from super-interfaces, 'TypeName' may also refer to a direct super-interface of the current
      // class or interface. The method being invoked ('foo()') has to be searched in that super-interface.
      symbol.members.enter(new JavaSymbol.VariableJavaSymbol(Flags.FINAL, "super", type, symbol));
      // Note: The above "super" symbol will always be qualified when referenced. e.g. A.super.hashCode()
      // because it's a compilation error to use unqualified "super" in default method. e.g. super.hashCode()
      // Note: interface/class can extend/implement multiple interfaces containing default methods with the same
      // signature. Mentioning the super-interfaces explicitly removes any ambiguity.
    }

    // Register default constructor
    if (tree.is(Tree.Kind.CLASS, Tree.Kind.ENUM) && symbol.lookupSymbols(CONSTRUCTOR_NAME).isEmpty()) {
      List<JavaType> argTypes = Collections.emptyList();
      if (!symbol.isStatic()) {
        // JLS8 - 8.8.1 & 8.8.9 : constructors of inner class have an implicit first arg of its directly enclosing class type
        JavaSymbol owner = symbol.owner();
        if (!owner.isPackageSymbol()) {
          argTypes = Collections.singletonList(Objects.requireNonNull(owner.enclosingClass().type));
        }
      }
      JavaSymbol.MethodJavaSymbol defaultConstructor = new JavaSymbol.MethodJavaSymbol(symbol.flags & Flags.ACCESS_FLAGS, CONSTRUCTOR_NAME, symbol);
      MethodJavaType defaultConstructorType = new MethodJavaType(argTypes, null, Collections.emptyList(), symbol);
      defaultConstructor.setMethodType(defaultConstructorType);
      defaultConstructor.parameters = new Scope(defaultConstructor);
      symbol.members.enter(defaultConstructor);
    }
  }

  private void populateSuperclass(JavaSymbol.TypeJavaSymbol symbol, Resolve.Env env, ClassJavaType type) {
    ClassTree tree = symbol.declaration;
    Tree superClassTree = tree.superClass();
    if (superClassTree != null) {
      type.supertype = resolveType(env, superClassTree);
      checkHierarchyCycles(symbol.type);
    } else if (tree.is(Tree.Kind.ENUM)) {
      // JLS8 8.9: The direct superclass of an enum type E is Enum<E>.
      Scope enumParameters = ((JavaSymbol.TypeJavaSymbol) symbols.enumType.symbol()).typeParameters();
      TypeVariableJavaType enumParameter = (TypeVariableJavaType) enumParameters.lookup("E").get(0).type();
      type.supertype = parametrizedTypeCache.getParametrizedTypeType(symbols.enumType.symbol, new TypeSubstitution().add(enumParameter, type));
    } else if (tree.is(Tree.Kind.CLASS, Tree.Kind.INTERFACE)) {
      // For CLASS JLS8 8.1.4: the direct superclass of the class type C<F1,...,Fn> is
      // the type given in the extends clause of the declaration of C
      // if an extends clause is present, or Object otherwise.
      // For INTERFACE JLS8 9.1.3: While every class is an extension of class Object, there is no single interface of which all interfaces are
      // extensions.
      // but we can call object method on any interface type.
      type.supertype = symbols.objectType;
    }
  }

  private void completeTypeParameters(TypeParameters typeParameters, Resolve.Env env) {
    for (TypeParameterTree typeParameterTree : typeParameters) {
      List<JavaType> bounds = new ArrayList<>();
      if(typeParameterTree.bounds().isEmpty()) {
        bounds.add(symbols.objectType);
      } else {
        for (Tree boundTree : typeParameterTree.bounds()) {
          bounds.add(resolveType(env, boundTree));
        }
      }
      ((TypeVariableJavaType) semanticModel.getSymbol(typeParameterTree).type()).bounds = bounds;
    }
  }

  private static void checkHierarchyCycles(JavaType baseType) {
    Set<ClassJavaType> types = new HashSet<>();
    ClassJavaType type = (ClassJavaType) baseType;
    while (type != null) {
      if (!types.add(type)) {
        throw new IllegalStateException("Cycling class hierarchy detected with symbol : " + baseType.symbol.name + ".");
      }
      type = (ClassJavaType) type.symbol.getSuperclass();
    }
  }

  private void complete(JavaSymbol.MethodJavaSymbol symbol) {
    MethodTree methodTree = symbol.declaration;
    Resolve.Env env = semanticModel.getEnv(symbol);
    completeTypeParameters(methodTree.typeParameters(), env);
    ImmutableList.Builder<JavaType> thrownTypes = ImmutableList.builder();
    for (TypeTree throwClause : methodTree.throwsClauses()) {
      JavaType thrownType = resolveType(env, throwClause);
      if (thrownType != null) {
        thrownTypes.add(thrownType);
      }
    }

    JavaType returnType = null;
    List<JavaType> argTypes = new ArrayList<>();
    // no return type for constructor
    if (!CONSTRUCTOR_NAME.equals(symbol.name)) {
      returnType = resolveType(env, methodTree.returnType());
      if (returnType != null) {
        symbol.returnType = returnType.symbol;
      }
    } else if (!symbol.enclosingClass().isStatic()) {
      JavaSymbol owner = symbol.enclosingClass().owner();
      if (!owner.isPackageSymbol()) {
        // JLS8 - 8.8.1 & 8.8.9 : constructors of inner class have an implicit first arg of its directly enclosing class type
        argTypes.add(owner.enclosingClass().type);
      }
    }
    List<VariableTree> parametersTree = methodTree.parameters();
    List<JavaSymbol> scopeSymbols = symbol.parameters.scopeSymbols();
    for(int i = 0; i < parametersTree.size(); i += 1) {
      VariableTree variableTree = parametersTree.get(i);
      JavaSymbol param = scopeSymbols.get(i);
      if (variableTree.simpleName().name().equals(param.getName())) {
        param.complete();
        argTypes.add(param.getType());
      }
      if(((VariableTreeImpl)variableTree).isVararg()) {
        symbol.flags |= Flags.VARARGS;
      }
    }
    MethodJavaType methodType = new MethodJavaType(argTypes, returnType, thrownTypes.build(), (JavaSymbol.TypeJavaSymbol) symbol.owner);
    symbol.setMethodType(methodType);
  }

  private void complete(JavaSymbol.VariableJavaSymbol symbol) {
    VariableTree variableTree = symbol.declaration;
    Resolve.Env env = semanticModel.getEnv(symbol);
    if (variableTree.is(Tree.Kind.ENUM_CONSTANT)) {
      symbol.type = env.enclosingClass.type;
    } else {
      symbol.type = resolveType(env, variableTree.type());
    }
  }

  private JavaType resolveType(Resolve.Env env, Tree tree) {
    Preconditions.checkArgument(checkTypeOfTree(tree), "Kind of tree unexpected " + tree.kind());
    //FIXME(benzonico) as long as Variables share the same node type, (int i,j; or worse : int i[], j[];) check nullity to respect invariance.
    if (((AbstractTypedTree) tree).isTypeSet()) {
      return (JavaType) ((AbstractTypedTree) tree).symbolType();
    }
    typeAndReferenceSolver.env = env;
    typeAndReferenceSolver.resolveAs(tree, JavaSymbol.TYP, env);
    typeAndReferenceSolver.env = null;
    return (JavaType) ((AbstractTypedTree) tree).symbolType();
  }

  private static boolean checkTypeOfTree(Tree tree) {
    return tree.is(Tree.Kind.MEMBER_SELECT,
      Tree.Kind.IDENTIFIER,
      Tree.Kind.PARAMETERIZED_TYPE,
      Tree.Kind.ARRAY_TYPE,
      Tree.Kind.UNION_TYPE,
      Tree.Kind.PRIMITIVE_TYPE,
      Tree.Kind.VAR_TYPE,
      Tree.Kind.INFERED_TYPE);
  }

}
