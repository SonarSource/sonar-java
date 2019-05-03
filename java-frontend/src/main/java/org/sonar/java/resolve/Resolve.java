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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.model.expression.ConditionalExpressionTreeImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.Tree;

/**
 * Routines for name resolution.
 * <p/>
 * Lookup by name and then filter by type is performant, because amount of symbols with same name are relatively small.
 * <p/>
 * Naming conventions:
 * env - is the environment where the symbol was mentioned
 * site - is the type of which symbol is a member
 * name - is the symbol's name
 * <p/>
 * TODO site should be represented by class Type
 */
public class Resolve {

  private static final String CONSTRUCTOR_NAME = "<init>";

  private final JavaSymbolNotFound symbolNotFound = new JavaSymbolNotFound();

  private final BytecodeCompleter bytecodeCompleter;
  private final TypeSubstitutionSolver typeSubstitutionSolver;
  private final Types types = new Types();
  private final Symbols symbols;

  public Resolve(Symbols symbols, BytecodeCompleter bytecodeCompleter, ParametrizedTypeCache parametrizedTypeCache) {
    this.symbols = symbols;
    this.bytecodeCompleter = bytecodeCompleter;
    this.typeSubstitutionSolver = new TypeSubstitutionSolver(parametrizedTypeCache, symbols);
  }

  @Nullable
  private static JavaSymbol.TypeJavaSymbol superclassSymbol(JavaSymbol.TypeJavaSymbol c) {
    JavaType supertype = c.getSuperclass();
    return supertype == null ? null : supertype.symbol;
  }

  public JavaSymbol.TypeJavaSymbol registerClass(JavaSymbol.TypeJavaSymbol classSymbol) {
    return bytecodeCompleter.registerClass(classSymbol);
  }

  public Scope createStarImportScope(JavaSymbol owner) {
    return new Scope.StarImportScope(owner, bytecodeCompleter);
  }

  public Scope createStaticStarImportScope(JavaSymbol owner) {
    return new Scope.StaticStarImportScope(owner, bytecodeCompleter);
  }

  public JavaType functionType(ParametrizedTypeJavaType javaType) {
    return typeSubstitutionSolver.functionType(javaType);
  }

  public JavaType resolveTypeSubstitution(JavaType type, JavaType definition) {
    return typeSubstitutionSolver.applySiteSubstitution(type, definition);
  }
  public List<JavaType> resolveTypeSubstitution(List<JavaType> formals, TypeSubstitution substitution) {
    return typeSubstitutionSolver.applySubstitutionToFormalParameters(formals, substitution);
  }

  public JavaType applySubstitution(JavaType type, TypeSubstitution substitution) {
    return typeSubstitutionSolver.applySubstitution(type, substitution);
  }

  public JavaType resolveTypeSubstitutionWithDiamondOperator(ParametrizedTypeJavaType type, JavaType definition) {
    ParametrizedTypeJavaType result = type;
    if (definition.isParameterized()) {
      TypeSubstitution substitution = TypeSubstitutionSolver.substitutionFromSuperType(type, (ParametrizedTypeJavaType) definition);
      result = (ParametrizedTypeJavaType) typeSubstitutionSolver.applySubstitution(type, substitution);
    }
    return typeSubstitutionSolver.erasureSubstitution(result);
  }

  public JavaType parametrizedTypeWithErasure(ParametrizedTypeJavaType type) {
    return typeSubstitutionSolver.erasureSubstitution(type);
  }

  /**
   * Finds field with given name.
   */
  private Resolution findField(Env env, JavaSymbol.TypeJavaSymbol site, String name, JavaSymbol.TypeJavaSymbol c) {
    Resolution bestSoFar = unresolved();
    for (JavaSymbol symbol : c.members().lookup(name)) {
      if (symbol.kind == JavaSymbol.VAR) {
        return resolveField(env, site, c, symbol);
      }
    }
    Resolution resolution;
    if (c.getSuperclass() != null) {
      resolution = findField(env, site, name, c.getSuperclass().symbol);
      if (resolution.symbol.kind < bestSoFar.symbol.kind) {
        resolution.type = typeSubstitutionSolver.applySiteSubstitution(resolution.type(), c.getSuperclass());
        bestSoFar = resolution;
      }
    }
    for (JavaType interfaceType : c.getInterfaces()) {
      resolution = findField(env, site, name, interfaceType.symbol);
      if (resolution.symbol.kind < bestSoFar.symbol.kind) {
        bestSoFar = resolution;
      }
    }
    return bestSoFar;
  }

  private Resolution resolveField(Env env, JavaSymbol.TypeJavaSymbol site, JavaSymbol.TypeJavaSymbol owner, JavaSymbol field) {
    if (isAccessible(env, site, field)) {
      Resolution resolution = Resolution.resolution(field);
      if (field.type != null) {
        // type may not have been resolved yet, but will be in 2nd pass
        resolution.type = typeSubstitutionSolver.applySiteSubstitution(field.type, owner.type);
      }
      return resolution;
    }
    return Resolution.resolution(new AccessErrorJavaSymbol(field, Symbols.unknownType));
  }

  /**
   * Finds variable or field with given name.
   */
  private Resolution findVar(Env env, String name) {
    Resolution bestSoFar = unresolved();

    Env env1 = env;
    while (env1.outer != null) {
      Resolution sym = new Resolution();
      for (JavaSymbol symbol : env1.scope.lookup(name)) {
        if (symbol.kind == JavaSymbol.VAR) {
          sym.symbol = symbol;
        }
      }
      if (sym.symbol == null) {
        sym = findField(env1, env1.enclosingClass, name, env1.enclosingClass);
      }
      if (sym.symbol.kind < JavaSymbol.ERRONEOUS) {
        // symbol exists
        return sym;
      } else if (sym.symbol.kind < bestSoFar.symbol.kind) {
        bestSoFar = sym;
      }
      env1 = env1.outer;
    }

    JavaSymbol symbol = findVarInStaticImport(env, name);
    if (symbol.kind < JavaSymbol.ERRONEOUS) {
      // symbol exists
      return Resolution.resolution(symbol);
    } else if (symbol.kind < bestSoFar.symbol.kind) {
      bestSoFar = Resolution.resolution(symbol);
    }
    return bestSoFar;
  }

  private JavaSymbol findVarInStaticImport(Env env, String name) {
    JavaSymbol bestSoFar = symbolNotFound;
    for (JavaSymbol symbol : env.namedImports.lookup(name)) {
      if ((JavaSymbol.VAR & symbol.kind) != 0) {
        return symbol;
      }
    }
    for (JavaSymbol symbol : env.staticStarImports.lookup(name)) {
      if ((JavaSymbol.VAR & symbol.kind) != 0) {
        return symbol;
      }
    }
    return bestSoFar;
  }

  private JavaSymbol findMemberType(Env env, JavaSymbol.TypeJavaSymbol site, String name, JavaSymbol.TypeJavaSymbol c) {
    JavaSymbol bestSoFar = symbolNotFound;
    for (JavaSymbol symbol : c.members().lookup(name)) {
      if (symbol.kind == JavaSymbol.TYP) {
        return isAccessible(env, site, symbol)
            ? symbol
            : new AccessErrorJavaSymbol(symbol, Symbols.unknownType);
      }
    }
    if (c.getSuperclass() != null) {
      JavaSymbol symbol = findMemberType(env, site, name, c.getSuperclass().symbol);
      if (symbol.kind < bestSoFar.kind) {
        bestSoFar = symbol;
      }
    }
    if (c.getInterfaces() == null) {
      // Invariant to check that interfaces are not set only when we are looking into the symbol we are currently completing.
      // required for generics
      Preconditions.checkState(c.completing, "interfaces of a symbol not currently completing are not set.");
      Preconditions.checkState(c == site);
    } else {
      for (JavaType interfaceType : c.getInterfaces()) {
        JavaSymbol symbol = findMemberType(env, site, name, interfaceType.symbol);
        if (symbol.kind < bestSoFar.kind) {
          bestSoFar = symbol;
        }
      }
    }
    return bestSoFar;
  }

  /**
   * Finds type with given name.
   */
  private JavaSymbol findType(Env env, String name) {
    JavaSymbol bestSoFar = symbolNotFound;
    for (Env env1 = env; env1 != null; env1 = env1.outer) {
      for (JavaSymbol symbol : env1.scope.lookup(name)) {
        if (symbol.kind == JavaSymbol.TYP) {
          return symbol;
        }
      }
      if (env1.outer != null) {
        JavaSymbol symbol = findMemberType(env1, env1.enclosingClass, name, env1.enclosingClass);
        if (symbol.kind < JavaSymbol.ERRONEOUS) {
          // symbol exists
          return symbol;
        } else if (symbol.kind < bestSoFar.kind) {
          bestSoFar = symbol;
        }
      }
    }

    //checks predefined types
    JavaSymbol predefinedSymbol = findMemberType(env, symbols.predefClass, name, symbols.predefClass);
    if (predefinedSymbol.kind < bestSoFar.kind) {
      return predefinedSymbol;
    }

    //JLS8 6.4.1 Shadowing rules
    //named imports
    for (JavaSymbol symbol : env.namedImports.lookup(name)) {
      if (symbol.kind == JavaSymbol.TYP) {
        return symbol;
      }
    }
    //package types
    JavaSymbol sym = findIdentInPackage(env.packge, name, JavaSymbol.TYP);
    if (sym.kind < bestSoFar.kind) {
      return sym;
    }
    //on demand imports
    for (JavaSymbol symbol : env.starImports.lookup(name)) {
      if (symbol.kind == JavaSymbol.TYP) {
        return symbol;
      }
    }
    //java.lang
    JavaSymbol.PackageJavaSymbol javaLang = bytecodeCompleter.enterPackage("java.lang");
    for (JavaSymbol symbol : javaLang.completedMembers().lookup(name)) {
      if (symbol.kind == JavaSymbol.TYP) {
        return symbol;
      }
    }
    return bestSoFar;
  }

  /**
   * @param kind subset of {@link JavaSymbol#VAR}, {@link JavaSymbol#TYP}, {@link JavaSymbol#PCK}
   */
  public Resolution findIdent(Env env, String name, int kind) {
    Resolution bestSoFar = unresolved();
    if ((kind & JavaSymbol.VAR) != 0) {
      Resolution res = findVar(env, name);
      if (res.symbol.kind < JavaSymbol.ERRONEOUS) {
        // symbol exists
        return res;
      } else if (res.symbol.kind < bestSoFar.symbol.kind) {
        bestSoFar = res;
      }
    }
    if ((kind & JavaSymbol.TYP) != 0) {
      Resolution res = new Resolution();
      res.symbol = findType(env, name);
      if (res.symbol.kind < JavaSymbol.ERRONEOUS) {
        // symbol exists
        return res;
      } else if (res.symbol.kind < bestSoFar.symbol.kind) {
        bestSoFar = res;
      }
    }
    if ((kind & JavaSymbol.PCK) != 0) {
      Resolution res = new Resolution();
      res.symbol = findIdentInPackage(symbols.defaultPackage, name, JavaSymbol.PCK);
      if (res.symbol.kind < JavaSymbol.ERRONEOUS) {
        // symbol exists
        return res;
      } else if (res.symbol.kind < bestSoFar.symbol.kind) {
        bestSoFar = res;
      }
    }
    return bestSoFar;
  }

  /**
   * @param kind subset of {@link JavaSymbol#TYP}, {@link JavaSymbol#PCK}
   */
  public JavaSymbol findIdentInPackage(JavaSymbol site, String name, int kind) {
    String fullname = bytecodeCompleter.formFullName(name, site);
    JavaSymbol bestSoFar = symbolNotFound;
    //Try to find a type matching the name.
    if ((kind & JavaSymbol.TYP) != 0) {
      JavaSymbol sym = bytecodeCompleter.loadClass(fullname);
      if (sym.kind < bestSoFar.kind) {
        bestSoFar = sym;
      }
    }
    //We did not find the class so identifier must be a package.
    if ((kind & JavaSymbol.PCK) != 0 && bestSoFar.kind >= symbolNotFound.kind) {
      bestSoFar = bytecodeCompleter.enterPackage(fullname);
    }
    return bestSoFar;
  }

  /**
   * @param kind subset of {@link JavaSymbol#VAR}, {@link JavaSymbol#TYP}
   */
  public Resolution findIdentInType(Env env, JavaSymbol.TypeJavaSymbol site, String name, int kind) {
    Resolution bestSoFar = unresolved();
    Resolution resolution;
    JavaSymbol symbol;
    if ((kind & JavaSymbol.VAR) != 0) {
      resolution = findField(env, site, name, site);
      if (resolution.symbol.kind < JavaSymbol.ERRONEOUS) {
        // symbol exists
        return resolution;
      } else if (resolution.symbol.kind < bestSoFar.symbol.kind) {
        bestSoFar = resolution;
      }
    }
    if ((kind & JavaSymbol.TYP) != 0) {
      symbol = findMemberType(env, site, name, site);
      if (symbol.kind < JavaSymbol.ERRONEOUS) {
        // symbol exists
        return Resolution.resolution(symbol);
      } else if (symbol.kind < bestSoFar.symbol.kind) {
        bestSoFar = Resolution.resolution(symbol);
      }
    }
    return bestSoFar;
  }

  /**
   * Finds method matching given name and types of arguments.
   */
  public Resolution findMethod(Env env, String name, List<JavaType> argTypes, List<JavaType> typeParamTypes) {
    Resolution bestSoFar = unresolved();
    Env env1 = env;
    while (env1.outer != null) {
      Resolution res = findMethod(env1, env1.enclosingClass.getType(), name, argTypes, typeParamTypes);
      if (res.symbol.kind < JavaSymbol.ERRONEOUS) {
        // symbol exists
        return res;
      } else if (res.symbol.kind < bestSoFar.symbol.kind) {
        bestSoFar = res;
      }
      env1 = env1.outer;
    }
    Resolution res = findMethodInStaticImport(env, name, argTypes, typeParamTypes);
    if (res.symbol.kind < JavaSymbol.ERRONEOUS) {
      // symbol exists
      return res;
    } else if (res.symbol.kind < bestSoFar.symbol.kind) {
      bestSoFar = res;
    }
    return bestSoFar;
  }

  private Resolution findMethodInStaticImport(Env env, String name, List<JavaType> argTypes, List<JavaType> typeParamTypes) {
    Resolution bestSoFar = unresolved();
    JavaType enclosingType = env.enclosingClass.getType();
    bestSoFar = lookupInScope(env, enclosingType, enclosingType, name, argTypes, typeParamTypes, false, false, env.namedImports, bestSoFar);
    if (bestSoFar.symbol.kind < JavaSymbol.ERRONEOUS) {
      // symbol exists
      return bestSoFar;
    }
    bestSoFar = lookupInScope(env, enclosingType, enclosingType, name, argTypes, typeParamTypes, false, false, env.staticStarImports, bestSoFar);
    if (bestSoFar.symbol.kind < JavaSymbol.ERRONEOUS) {
      // symbol exists
      return bestSoFar;
    }
    bestSoFar = lookupInScope(env, enclosingType, enclosingType, name, argTypes, typeParamTypes, true, true, env.namedImports, bestSoFar);
    if (bestSoFar.symbol.kind < JavaSymbol.ERRONEOUS) {
      // symbol exists
      return bestSoFar;
    }
    bestSoFar = lookupInScope(env, enclosingType, enclosingType, name, argTypes, typeParamTypes, true, true, env.staticStarImports, bestSoFar);
    return bestSoFar;
  }

  public Resolution findMethod(Env env, JavaType site, String name, List<JavaType> argTypes) {
    return findMethod(env, site, site, name, argTypes, Collections.emptyList());
  }

  public Resolution findMethod(Env env, JavaType site, String name, List<JavaType> argTypes, List<JavaType> typeParams) {
    return findMethod(env, site, site, name, argTypes, typeParams);
  }

  private Resolution findMethod(Env env, JavaType callSite, JavaType site, String name, List<JavaType> argTypes, List<JavaType> typeParams) {
    // handle constructors
    if ("this".equals(name)) {
      return findConstructor(env, site, argTypes, typeParams);
    } else if ("super".equals(name)) {
      JavaType superclass = site.getSuperType();
      if (superclass == null) {
        return unresolved();
      }
      return findConstructor(env, superclass, argTypes, typeParams);
    }

    return findMethodByStrictThenLooseInvocation(env, callSite, site, name, argTypes, typeParams);
  }

  private Resolution findConstructor(Env env, JavaType site, List<JavaType> argTypes, List<JavaType> typeParams) {
    List<JavaType> newArgTypes = argTypes;
    JavaSymbol owner = site.symbol.owner();
    if (!owner.isPackageSymbol() && !site.symbol.isStatic()) {
      // JLS8 - 8.8.1 & 8.8.9 : constructors of inner class have an implicit first arg of its directly enclosing class type
      newArgTypes = ImmutableList.<JavaType>builder().add(owner.enclosingClass().type).addAll(argTypes).build();
    }
    return findMethodByStrictThenLooseInvocation(env, site, site, CONSTRUCTOR_NAME, newArgTypes, typeParams);
  }

  private Resolution findMethodByStrictThenLooseInvocation(Env env, JavaType callSite, JavaType site, String name, List<JavaType> argTypes, List<JavaType> typeParams) {
    // JLS8 - §5.3 searching by strict invocation, then loose invocation
    Resolution bestSoFar = findMethod(env, callSite, site, name, argTypes, typeParams, false, false);
    // searching for a specific method applicable with fixed arity and loose invocation
    if (!argTypes.isEmpty() && bestSoFar.symbol.kind >= JavaSymbol.ERRONEOUS) {
      // retry with loose invocation
      bestSoFar = findMethod(env, callSite, site, name, argTypes, typeParams, true, false);
    }
    if(bestSoFar.symbol.kind >= JavaSymbol.ERRONEOUS) {
      // loose invocation and var arity
      bestSoFar = findMethod(env, callSite, site, name, argTypes, typeParams, true, true);
    }
    return bestSoFar;
  }

  private Resolution findMethod(Env env, JavaType callSite, JavaType site, String name, List<JavaType> argTypes, List<JavaType> typeParams,
                                boolean looseInvocation, boolean varArity) {
    return findMethod(env, callSite, site, name, argTypes, typeParams, looseInvocation, varArity, new HashSet<>());
  }

  private Resolution findMethod(Env env, JavaType callSite, JavaType site, String name, List<JavaType> argTypes, List<JavaType> typeParams,
                                boolean looseInvocation, boolean varArity, Set<JavaType> visited) {

    Resolution bestSoFar = unresolved();
    if (!visited.add(site) || argTypes.stream().anyMatch(JavaType::isUnknown)) {
      return bestSoFar;
    }
    bestSoFar = lookupInScope(env, callSite, site, name, argTypes, typeParams, looseInvocation, varArity, site.getSymbol().members(), bestSoFar);
    if (name.equals(CONSTRUCTOR_NAME) && !site.symbol.isInterface()) {
      // Interfaces do not have constructors, but for anonymous classes of interfaces, the Object constructor should be resolved
      return bestSoFar;
    }
    JavaType superclass = site.getSuperType();
    // FIXME SONARJAVA-2096: interrupt exploration if the most specific method has already been found by strict invocation context
    //look in supertypes for more specialized method (overloading).
    if (superclass != null) {
      Resolution method = findMethod(env, callSite, superclass, name, argTypes, typeParams, looseInvocation, varArity, visited);
      Resolution best = selectBest(env, superclass, callSite, argTypes, typeParams, method.symbol, bestSoFar, looseInvocation);
      if (best.symbol == method.symbol) {
        method.type = typeSubstitutionSolver.applySiteSubstitution(method.type, site, superclass);
        bestSoFar = method;
      }
    }
    for (JavaType interfaceType : site.getSymbol().getInterfaces()) {
      Resolution method = findMethod(env, callSite, interfaceType, name, argTypes, typeParams, looseInvocation, varArity, visited);
      Resolution best = selectBest(env, interfaceType, callSite, argTypes, typeParams, method.symbol, bestSoFar, looseInvocation);
      if (best.symbol == method.symbol) {
        method.type = typeSubstitutionSolver.applySiteSubstitution(method.type, site, interfaceType);
        bestSoFar = method;
      }
    }
    return bestSoFar;
  }

  private Resolution lookupInScope(Env env, JavaType callSite, JavaType site, String name, List<JavaType> argTypes, List<JavaType> typeParams,
                                   boolean looseInvocation, boolean varArity, Scope scope, Resolution bestFound) {
    Resolution bestSoFar = bestFound;
    // look in site members
    for (JavaSymbol symbol : scope.lookup(name)) {
      if (symbol.kind == JavaSymbol.MTH) {
        boolean diffArity = varArity || (!((JavaSymbol.MethodJavaSymbol) symbol).isVarArgs())
          || (argTypes.size() == ((JavaSymbol.MethodJavaSymbol) symbol).parameterTypes().size() && argTypes.get(argTypes.size() -1 ).isArray());
        if(diffArity) {
          Resolution best = selectBest(env, site, callSite, argTypes, typeParams, symbol, bestSoFar, looseInvocation);
          if (best.symbol == symbol) {
            bestSoFar = best;
          }
        }
      }
    }
    return bestSoFar;
  }

  /**
   * @param candidate    candidate
   * @param bestSoFar previously found best match
   */
  private Resolution selectBest(Env env, JavaType defSite, JavaType callSite, List<JavaType> argTypes, List<JavaType> typeParams,
                                JavaSymbol candidate, Resolution bestSoFar, boolean looseInvocation) {
    JavaSymbol.TypeJavaSymbol siteSymbol = callSite.symbol;
    // TODO get rid of null check
    if (candidate.kind >= JavaSymbol.ERRONEOUS || !isInheritedIn(candidate, siteSymbol) || candidate.type == null) {
      return bestSoFar;
    }
    JavaSymbol.MethodJavaSymbol methodJavaSymbol = (JavaSymbol.MethodJavaSymbol) candidate;
    if(!hasCompatibleArity(methodJavaSymbol.parameterTypes().size(), argTypes.size(), methodJavaSymbol.isVarArgs())) {
      return bestSoFar;
    }
    TypeSubstitution substitution = typeSubstitutionSolver.getTypeSubstitution(methodJavaSymbol, callSite, typeParams, argTypes);
    if (substitution == null) {
      return bestSoFar;
    }
    List<JavaType> formals = ((MethodJavaType) methodJavaSymbol.type).argTypes;
    formals = typeSubstitutionSolver.applySiteSubstitutionToFormalParameters(formals, callSite);
    if(defSite != callSite) {
      formals = typeSubstitutionSolver.applySiteSubstitutionToFormalParameters(formals, defSite);
    }
    formals = typeSubstitutionSolver.applySubstitutionToFormalParameters(formals, substitution);
    if (!isArgumentsAcceptable(argTypes, formals, methodJavaSymbol.isVarArgs(), looseInvocation)) {
      return bestSoFar;
    }
    // TODO ambiguity, errors, ...
    if (!isAccessible(env, siteSymbol, candidate)) {
      Resolution resolution = new Resolution(new AccessErrorJavaSymbol(candidate, Symbols.unknownType));
      resolution.type = Symbols.unknownType;
      return resolution;
    }
    JavaSymbol mostSpecific = selectMostSpecific(candidate, bestSoFar.symbol, argTypes, substitution, callSite);
    if (mostSpecific.isKind(JavaSymbol.AMBIGUOUS)) {
      // same signature, we keep the first symbol found (overrides the other one).
      return bestSoFar;
    }

    Resolution resolution = new Resolution(mostSpecific);
    JavaSymbol.MethodJavaSymbol mostSpecificMethod = (JavaSymbol.MethodJavaSymbol) mostSpecific;
    List<JavaType> thrownTypes = ((MethodJavaType) mostSpecific.type).thrown;
    JavaType returnType = ((MethodJavaType) mostSpecificMethod.type).resultType;
    if((substitution.isUnchecked() || applicableWithUncheckedConversion(mostSpecificMethod, defSite, typeParams)) && !mostSpecificMethod.isConstructor()) {
      returnType = returnType.erasure();
      thrownTypes = erasure(thrownTypes);
    } else {
      returnType = typeSubstitutionSolver.getReturnType(returnType, defSite, callSite, substitution, mostSpecificMethod);
      thrownTypes = thrownTypes.stream()
        .map(t -> typeSubstitutionSolver.applySiteSubstitution(t, callSite))
        .map(t -> typeSubstitutionSolver.applySubstitution(t, substitution))
        .collect(Collectors.toList());
    }
    if (callSite.isArray() && candidate.name().equals("clone")) {
      returnType = callSite;
    }
    resolution.type = new MethodJavaType(formals, returnType, thrownTypes, defSite.symbol);
    return resolution;
  }

  private static List<JavaType> erasure(List<JavaType> types) {
    return types.stream().map(JavaType::erasure).collect(Collectors.toList());
  }

  private static boolean applicableWithUncheckedConversion(JavaSymbol.MethodJavaSymbol candidate, JavaType callSite, List<JavaType> typeParams) {
    return !candidate.isStatic() && isRawTypeOfParametrizedType(callSite) && typeParams.isEmpty();
  }
  private static boolean isRawTypeOfParametrizedType(JavaType site) {
    return !site.isParameterized() && !site.symbol.typeVariableTypes.isEmpty();
  }

  private static boolean hasCompatibleArity(int formalArgSize, int argSize, boolean isVarArgs) {
    if(isVarArgs) {
      return argSize - formalArgSize >= -1;
    }
    return formalArgSize == argSize;
  }

  /**
   * @param argTypes types of arguments
   * @param formals  types of formal parameters of method
   */
  private boolean isArgumentsAcceptable(List<JavaType> argTypes, List<JavaType> formals, boolean isVarArgs, boolean autoboxing) {
    int argsSize = argTypes.size();
    int formalsSize = formals.size();
    int nbArgToCheck = argsSize - formalsSize;
    if (isVarArgs) {
      // check at least last parameter for varargs compatibility
      nbArgToCheck++;
    }
    for (int i = 1; i <= nbArgToCheck; i++) {
      ArrayJavaType lastFormal = (ArrayJavaType) formals.get(formalsSize - 1);
      JavaType argType = argTypes.get(argsSize - i);
      // check type of element of array or if we invoke with an array that it is a compatible array type
      if (!isAcceptableType(argType, lastFormal.elementType, autoboxing) && (nbArgToCheck != 1 || !isAcceptableType(argType, lastFormal, autoboxing))) {
        return false;
      }
    }
    for (int i = 0; i < argsSize - nbArgToCheck; i++) {
      JavaType arg = argTypes.get(i);
      JavaType formal = formals.get(i);
      if (!isAcceptableType(arg, formal, autoboxing)) {
        return false;
      }
    }
    return true;
  }

  private boolean isAcceptableType(JavaType arg, JavaType formal, boolean autoboxing) {
    if(arg.isTagged(JavaType.DEFERRED)) {
      return isAcceptableDeferredType((DeferredType) arg, formal);
    }
    if(formal.isTagged(JavaType.TYPEVAR) && !arg.isTagged(JavaType.TYPEVAR)) {
      return subtypeOfTypeVar(arg, (TypeVariableJavaType) formal);
    }
    if (formal.isArray() && arg.isArray()) {
      return isAcceptableType(((ArrayJavaType) arg).elementType(), ((ArrayJavaType) formal).elementType(), autoboxing);
    }
    if (arg.isParameterized() || formal.isParameterized() || isWilcardType(arg) || isWilcardType(formal)) {
      return callWithRawType(arg, formal) || types.isSubtype(arg, formal) || isAcceptableByAutoboxing(arg, formal.erasure());
    }
    // fall back to behavior based on erasure
    return types.isSubtype(arg.erasure(), formal.erasure()) || (autoboxing && isAcceptableByAutoboxing(arg, formal.erasure()));
  }

  private boolean isAcceptableDeferredType(DeferredType arg, JavaType formal) {
    AbstractTypedTree tree = arg.tree();
    if(tree.is(Tree.Kind.METHOD_REFERENCE, Tree.Kind.LAMBDA_EXPRESSION) && (!formal.symbol.isFlag(Flags.INTERFACE) || !findSamMethodArgsRecursively(formal).isPresent())) {
      return false;
    }
    // we accept all deferred type as we will resolve this later, but reject lambdas with incorrect arity
    return !tree.is(Tree.Kind.LAMBDA_EXPRESSION) || ((LambdaExpressionTree) tree).parameters().size() == findSamMethodArgs(formal).size();
  }

  Resolution findMethodReference(Env env, List<JavaType> samMethodArgs, MethodReferenceTree methodRefTree) {
    Tree expression = methodRefTree.expression();
    JavaType expressionType = (JavaType) ((AbstractTypedTree) expression).symbolType();
    String methodName = getMethodReferenceMethodName(methodRefTree.method().name());
    Resolution resolution = findMethod(env, expressionType, methodName, samMethodArgs);
    // JLS §15.13.1
    if (secondSearchRequired(expression, expressionType, resolution.symbol, samMethodArgs)) {
      resolution = findMethod(env, expressionType, methodName, samMethodArgs.stream().skip(1).collect(Collectors.toList()));
    }
    return resolution;
  }

  private static String getMethodReferenceMethodName(String methodName) {
    return JavaKeyword.NEW.getValue().equals(methodName) ? CONSTRUCTOR_NAME : methodName;
  }

  private static boolean secondSearchRequired(Tree expression, JavaType expressionType, JavaSymbol symbol, List<JavaType> samMethodArgs) {
    return isMethodRefOnType(expression) && firstParamSubtypeOfRefType(expressionType, samMethodArgs) && (symbol.isUnknown() || !symbol.isStatic());
  }

  private static boolean isMethodRefOnType(Tree expression) {
    if (expression.is(Tree.Kind.MEMBER_SELECT)) {
      return ((MemberSelectExpressionTree) expression).identifier().symbol().isTypeSymbol();
    } else if (expression.is(Tree.Kind.IDENTIFIER)) {
      return ((IdentifierTree) expression).symbol().isTypeSymbol();
    }
    return false;
  }

  private static boolean firstParamSubtypeOfRefType(JavaType expressionType, List<JavaType> samMethodArgs) {
    return samMethodArgs.isEmpty() || samMethodArgs.get(0).isSubtypeOf(expressionType.erasure());
  }

  private boolean callWithRawType(JavaType arg, JavaType formal) {
    return formal.isParameterized() && !arg.isParameterized() && types.isSubtype(arg, formal.erasure());
  }

  private static boolean subtypeOfTypeVar(JavaType arg, TypeVariableJavaType formal) {
    for (JavaType bound : formal.bounds()) {
      if ((bound.isTagged(JavaType.TYPEVAR) && !subtypeOfTypeVar(arg, (TypeVariableJavaType) bound))
        || !arg.isSubtypeOf(bound)) {
        return false;
      }
    }
    return true;
  }

  private static boolean isWilcardType(JavaType type) {
    return type.isTagged(JavaType.WILDCARD);
  }

  private boolean isAcceptableByAutoboxing(JavaType expressionType, JavaType formalType) {
    if (expressionType.isPrimitive()) {
      return types.isSubtype(symbols.boxedTypes.get(expressionType), formalType);
    } else {
      JavaType unboxedType = symbols.boxedTypes.inverse().get(expressionType);
      if (unboxedType != null) {
        return types.isSubtype(unboxedType, formalType);
      }
    }
    return false;
  }

  /**
   * JLS7 15.12.2.5. Choosing the Most Specific Method
   */
  private JavaSymbol selectMostSpecific(JavaSymbol m1, JavaSymbol m2, List<JavaType> argTypes, TypeSubstitution m1Substitution, JavaType callSite) {
    // FIXME get rig of null check
    if (m2.type == null || !m2.isKind(JavaSymbol.MTH)) {
      return m1;
    }

    JavaSymbol.MethodJavaSymbol m1MethodSymbol = (JavaSymbol.MethodJavaSymbol) m1;
    JavaSymbol.MethodJavaSymbol m2MethodSymbol = (JavaSymbol.MethodJavaSymbol) m2;

    TypeSubstitution m2Substitution = null;
    boolean m1IsGeneric = m1MethodSymbol.isParametrized();
    boolean m2IsGeneric = m2MethodSymbol.isParametrized();
    if (m2IsGeneric) {
      m2Substitution = typeSubstitutionSolver.getTypeSubstitution(m2MethodSymbol, callSite, Collections.emptyList(), argTypes);
    }
    if (m2Substitution == null) {
      m2Substitution = new TypeSubstitution();
    }
    boolean m1SignatureMoreSpecific = isSignatureMoreSpecific(m1MethodSymbol, m2MethodSymbol, argTypes, m1Substitution, m2Substitution);
    boolean m2SignatureMoreSpecific = isSignatureMoreSpecific(m2MethodSymbol, m1MethodSymbol, argTypes, m2Substitution, m1Substitution);
    if (m1SignatureMoreSpecific && m2SignatureMoreSpecific) {
      // JLS8 18.5.4 naive implementation of most specific when inferring of parametric method is involved
      if(!m1IsGeneric && m2IsGeneric) {
        return m1;
      } else if(m1IsGeneric && !m2IsGeneric) {
        return m2;
      }
      return new AmbiguityErrorJavaSymbol();
    } else if (m1SignatureMoreSpecific) {
      return m1;
    } else if (m2SignatureMoreSpecific) {
      return m2;
    }
    return new AmbiguityErrorJavaSymbol();
  }

  /**
   * @return true, if signature of m1 is more specific than signature of m2
   */
  private boolean isSignatureMoreSpecific(JavaSymbol.MethodJavaSymbol m1, JavaSymbol.MethodJavaSymbol m2, List<JavaType> argTypes,
                                          TypeSubstitution m1Substitution, TypeSubstitution m2Substitution) {
    List<JavaType> m1ArgTypes = ((MethodJavaType) m1.type).argTypes;
    List<JavaType> m2ArgTypes = ((MethodJavaType) m2.type).argTypes;
    boolean m1VarArity = m1.isVarArgs();
    boolean m2VarArity = m2.isVarArgs();
    if (m1VarArity != m2VarArity) {
      // last arg is an array
      boolean lastArgIsArray = !argTypes.isEmpty() && argTypes.get(argTypes.size() -1).isArray() && (argTypes.size() == m2ArgTypes.size() || argTypes.size() == m1ArgTypes.size());
      // general case : prefer strict arity invocation over varArity, so if m2 is variadic, m1 is most specific, but not if last arg of invocation is an array
      return lastArgIsArray ^ m2VarArity;
    }
    if (m1VarArity) {
      m1ArgTypes = expandVarArgsToFitSize(m1ArgTypes, m2ArgTypes.size());
    }
    if(!hasCompatibleArity(m1ArgTypes.size(), m2ArgTypes.size(), m2VarArity)) {
      return false;
    }
    List<JavaType> m1SubstitutedArgTypes = typeSubstitutionSolver.applySubstitutionToFormalParameters(m1ArgTypes, m1Substitution);
    List<JavaType> m2SubstitutedArgTypes = typeSubstitutionSolver.applySubstitutionToFormalParameters(m2ArgTypes, m2Substitution);
    if (m1SubstitutedArgTypes.equals(m2SubstitutedArgTypes)
      // approximation for generic methods
      || (m1.isParametrized() && m2.isParametrized() && erasure(m1SubstitutedArgTypes).equals(erasure(m2SubstitutedArgTypes)))) {
      // Same types once substituted, so we should select the most specific one before substitution.
      // i.e: 'T[]' is more specific than 'T', or 'List<T>' is more specific than 'T'
      return isArgumentsAcceptable(m1ArgTypes, m2ArgTypes, m2VarArity, true);
    }
    return isArgumentsAcceptable(m1SubstitutedArgTypes, m2SubstitutedArgTypes, m2VarArity, true);
  }

  private static List<JavaType> expandVarArgsToFitSize(List<JavaType> m1ArgTypes, int size) {
    List<JavaType> newArgTypes = new ArrayList<>(m1ArgTypes);
    int m1ArgTypesSize = newArgTypes.size();
    int m1ArgTypesLast = m1ArgTypesSize - 1;
    Type lastElementType = ((Type.ArrayType) newArgTypes.get(m1ArgTypesLast)).elementType();
    // replace last element type from GivenType[] to GivenType
    newArgTypes.set(m1ArgTypesLast, (JavaType) lastElementType);
    // if m1ArgTypes smaller than size pad it with lastElementType
    for (int i = m1ArgTypesSize; i < size - 1; i++) {
      if (i < newArgTypes.size()) {
        newArgTypes.set(i, (JavaType) lastElementType);
      } else {
        newArgTypes.add((JavaType) lastElementType);
      }
    }
    return newArgTypes;
  }

  /**
   * Is class accessible in given environment?
   */
  @VisibleForTesting
  static boolean isAccessible(Env env, JavaSymbol.TypeJavaSymbol c) {
    final boolean result;
    switch (c.flags() & Flags.ACCESS_FLAGS) {
      case Flags.PRIVATE:
        result = sameOutermostClass(env.enclosingClass, c.owner());
        break;
      case 0:
        result = env.packge == c.packge();
        break;
      case Flags.PUBLIC:
        result = true;
        break;
      case Flags.PROTECTED:
        result = env.packge == c.packge() || isInnerSubClass(env.enclosingClass, c.owner());
        break;
      default:
        throw new IllegalStateException();
    }
    // TODO check accessibility of enclosing type: isAccessible(env, c.type.getEnclosingType())
    return result;
  }

  /**
   * Is given class a subclass of given base class, or an inner class of a subclass?
   */
  private static boolean isInnerSubClass(JavaSymbol.TypeJavaSymbol c, JavaSymbol base) {
    while (c != null && isSubClass(c, base)) {
      c = c.owner().enclosingClass();
    }
    return c != null;
  }

  /**
   * Is given class a subclass of given base class?
   */
  @VisibleForTesting
  static boolean isSubClass(@Nullable JavaSymbol.TypeJavaSymbol c, JavaSymbol base) {
    // TODO get rid of null check
    if (c == null) {
      return false;
    }
    // TODO see Javac
    if (c == base) {
      // same class
      return true;
    } else if (Flags.isFlagged(base.flags(), Flags.INTERFACE)) {
      // check if class implements base
      for (JavaType interfaceType : c.getInterfaces()) {
        if (isSubClass(interfaceType.symbol, base)) {
          return true;
        }
      }
      // check if superclass implements base
      return isSubClass(superclassSymbol(c), base);
    } else {
      // check if class extends base or its superclass extends base
      return isSubClass(superclassSymbol(c), base);
    }
  }

  /**
   * Is symbol accessible as a member of given class in given environment?
   * <p/>
   * Symbol is accessible only if not overridden by another symbol. If overridden, then strictly speaking it is not a member.
   */
  private static boolean isAccessible(Env env, JavaSymbol.TypeJavaSymbol site, JavaSymbol symbol) {
    switch (symbol.flags() & Flags.ACCESS_FLAGS) {
      case Flags.PRIVATE:
        //if enclosing class is null, we are checking accessibility for imports so we return false.
        // no check of overriding, because private members cannot be overridden
        return env.enclosingClass != null && sameOutermostClass(env.enclosingClass, symbol.owner())
            && isInheritedIn(symbol, site);
      case 0:
        return (env.packge == symbol.packge())
            && isAccessible(env, site)
            && isInheritedIn(symbol, site)
            && notOverriddenIn(site, symbol);
      case Flags.PUBLIC:
        return isAccessible(env, site)
            && notOverriddenIn(site, symbol);
      case Flags.PROTECTED:
        return ((env.packge == symbol.packge()) || isProtectedAccessible(symbol, env.enclosingClass, site))
            && isAccessible(env, site)
            && notOverriddenIn(site, symbol);
      default:
        throw new IllegalStateException();
    }
  }

  static boolean sameOutermostClass(JavaSymbol s1, JavaSymbol s2) {
    return s1.outermostClass() == s2.outermostClass();
  }

  private static boolean notOverriddenIn(JavaSymbol.TypeJavaSymbol site, JavaSymbol symbol) {
    // TODO see Javac
    return true;
  }

  /**
   * Is symbol inherited in given class?
   */
  @VisibleForTesting
  static boolean isInheritedIn(JavaSymbol symbol, JavaSymbol.TypeJavaSymbol clazz) {
    switch (symbol.flags() & Flags.ACCESS_FLAGS) {
      case Flags.PUBLIC:
        return true;
      case Flags.PRIVATE:
        return symbol.owner() == clazz;
      case Flags.PROTECTED:
        // TODO see Javac
        return true;
      case 0:
        // TODO see Javac
        JavaSymbol.PackageJavaSymbol thisPackage = symbol.packge();
        for (JavaSymbol.TypeJavaSymbol sup = clazz; sup != null && sup != symbol.owner(); sup = superclassSymbol(sup)) {
          if (sup.packge() != thisPackage) {
            return false;
          }
        }
        return true;
      default:
        throw new IllegalStateException();
    }
  }

  private static boolean isProtectedAccessible(JavaSymbol symbol, JavaSymbol.TypeJavaSymbol c, JavaSymbol.TypeJavaSymbol site) {
    // TODO see Javac
    return true;
  }

  Type leastUpperBound(Set<Type> refTypes) {
    return typeSubstitutionSolver.leastUpperBound(refTypes);
  }

  Resolution unresolved() {
    Resolution resolution = new Resolution(symbolNotFound);
    resolution.type = Symbols.unknownType;
    return resolution;
  }

  public JavaType conditionalExpressionType(ConditionalExpressionTree tree, JavaType trueType, JavaType falseType) {
    if (trueType.isTagged(JavaType.DEFERRED)) {
      return falseType.isTagged(JavaType.DEFERRED) ? symbols.deferedType((ConditionalExpressionTreeImpl) tree) : falseType;
    }
    if (falseType.isTagged(JavaType.DEFERRED)) {
      return trueType;
    }
    if (trueType == falseType) {
      return trueType;
    }
    if (trueType.isTagged(JavaType.BOT)) {
      return falseType.isPrimitive() ? falseType.primitiveWrapperType() : falseType;
    }
    if (falseType.isTagged(JavaType.BOT)) {
      return trueType.isPrimitive() ? trueType.primitiveWrapperType() : trueType;
    }
    JavaType secondOperand = getPrimitive(trueType);
    JavaType thirdOperand = getPrimitive(falseType);
    if (secondOperand != null && thirdOperand != null && isNumericalConditionalExpression(secondOperand, thirdOperand)) {
      // If operand is a constant int that can fits a narrow type it should be narrowed. We always narrow to approximate things properly for
      // method resolution.
      if ((secondOperand.tag < thirdOperand.tag || secondOperand.isTagged(JavaType.INT)) && !thirdOperand.isTagged(JavaType.INT)) {
        return thirdOperand;
      } else {
        return secondOperand;
      }
    }
    return (JavaType) leastUpperBound(Sets.<Type>newHashSet(trueType, falseType));
  }

  private static boolean isNumericalConditionalExpression(JavaType secondOperand, JavaType thirdOperand) {
    return secondOperand.isNumerical() && thirdOperand.isNumerical();
  }

  @CheckForNull
  private static JavaType getPrimitive(JavaType primitiveOrWrapper) {
    if(primitiveOrWrapper.isPrimitiveWrapper()) {
      return primitiveOrWrapper.primitiveType();
    }
    return primitiveOrWrapper.isPrimitive() ? primitiveOrWrapper : null;
  }

  public List<JavaType> findSamMethodArgs(Type type) {
    return findSamMethodArgsRecursively(type).orElse(new ArrayList<>());
  }

  private Optional<List<JavaType>> findSamMethodArgsRecursively(@Nullable Type type) {
    if (type == null) {
      return Optional.empty();
    }
    return getSamMethod((JavaType) type)
      .map(m -> ((MethodJavaType) m.type).argTypes)
      .map(samTypes -> applySamSubstitution(type, samTypes));
  }

  private List<JavaType> applySamSubstitution(Type type, List<JavaType> samTypes) {
    JavaType functionType = (JavaType) type;
    if(functionType instanceof ParametrizedTypeJavaType) {
      functionType = typeSubstitutionSolver.functionType((ParametrizedTypeJavaType) functionType);
    }
    List<JavaType> argTypes = typeSubstitutionSolver.applySiteSubstitutionToFormalParameters(samTypes, functionType);
    return argTypes.stream().map(argType -> {
      if (argType.isTagged(JavaType.WILDCARD)) {
        // JLS8 9.9 Function types : this is approximated for ? extends X types (cf JLS)
        return ((WildCardType) argType).bound;
      }
      return argType;
    }).collect(Collectors.toList());
  }

  public Optional<JavaSymbol.MethodJavaSymbol> getSamMethod(JavaType lambdaType) {
    List<JavaSymbol.MethodJavaSymbol> methodSymbols = abstractMethodsOfType(lambdaType);
    if (methodSymbols.size() > 1) {
      return Optional.empty();
    }
    if (methodSymbols.size() == 1) {
      JavaSymbol.MethodJavaSymbol override = methodSymbols.get(0).overriddenSymbol();
      Optional<JavaSymbol.MethodJavaSymbol> otherAbstractMethod = lambdaType.symbol.superTypes()
        .stream()
        .filter(t -> t.symbol().isInterface())
        .flatMap(t -> abstractMethodsOfType(t).stream())
        .filter(m -> override == null || !override.equals(m))
        .findFirst();
      if (otherAbstractMethod.isPresent()) {
        return Optional.empty();
      } else {
        return Optional.of(methodSymbols.get(0));
      }
    } else {
      // FIXME if list of supertypes define multiple abstract methods we might end up with a wrong samMethod from a non-functional interface
      return lambdaType.symbol.superTypes()
        .stream()
        .flatMap(t -> abstractMethodsOfType(t).stream())
        .findFirst();
    }
  }

  private List<JavaSymbol.MethodJavaSymbol> abstractMethodsOfType(JavaType javaType) {
    return javaType.symbol().memberSymbols().stream().filter(Resolve::isAbstractMethod).map(JavaSymbol.MethodJavaSymbol.class::cast)
      .filter(m -> !isObjectMethod(m))
      .collect(Collectors.toList());
  }

  private boolean isObjectMethod(JavaSymbol.MethodJavaSymbol methodJavaSymbol) {
    JavaSymbol.MethodJavaSymbol overriddenSymbol = methodJavaSymbol.overriddenSymbol();
    boolean isObjectMethod = false;
    while (overriddenSymbol != null && !isObjectMethod) {
      isObjectMethod = overriddenSymbol.owner.type == symbols.objectType;
      overriddenSymbol = overriddenSymbol.overriddenSymbol();
    }
    return isObjectMethod;
  }

  private static boolean isAbstractMethod(Symbol member) {
    return member.isMethodSymbol() && member.isAbstract();
  }

  /**
   * Resolution holds the symbol resolved and its type in this context.
   * This is required to handle type substitution for generics.
   */
  static class Resolution {

    private JavaSymbol symbol;
    private JavaType type;

    private Resolution(JavaSymbol symbol) {
      this.symbol = symbol;
    }

    Resolution() {
    }

    static Resolution resolution(JavaSymbol symbol) {
      return new Resolution(symbol);
    }

    JavaSymbol symbol() {
      return symbol;
    }

    public JavaType type() {
      if (type == null) {
        if(symbol.isKind(JavaSymbol.MTH)) {
          return ((MethodJavaType)symbol.type).resultType;
        }
        if(symbol.isUnknown() || symbol.isKind(JavaSymbol.PCK)) {
          return Symbols.unknownType;
        }
        return symbol.type;
      }
      return type;
    }
  }

  static class Env {
    /**
     * The next enclosing environment.
     */
    Env next;

    /**
     * The environment enclosing the current class.
     */
    @Nullable
    Env outer;

    JavaSymbol.PackageJavaSymbol packge;

    @Nullable
    JavaSymbol.TypeJavaSymbol enclosingClass;

    Scope scope;
    Scope namedImports;
    Scope starImports;
    Scope staticStarImports;

    public Env dup() {
      Env env = new Env();
      env.next = this;
      env.outer = this.outer;
      env.packge = this.packge;
      env.enclosingClass = this.enclosingClass;
      env.scope = this.scope;
      env.namedImports = this.namedImports;
      env.starImports = this.starImports;
      env.staticStarImports = this.staticStarImports;
      return env;
    }

  }

  public static class JavaSymbolNotFound extends JavaSymbol {
    public JavaSymbolNotFound() {
      super(JavaSymbol.ABSENT, 0, null, Symbols.unknownSymbol);
    }

    @Override
    public boolean isUnknown() {
      return true;
    }
  }

  public static class AmbiguityErrorJavaSymbol extends JavaSymbol {
    public AmbiguityErrorJavaSymbol() {
      super(JavaSymbol.AMBIGUOUS, 0, null, null);
    }
  }

  public static class AccessErrorJavaSymbol extends JavaSymbol {
    /**
     * The invalid symbol found during resolution.
     */
    JavaSymbol symbol;

    public AccessErrorJavaSymbol(JavaSymbol symbol, JavaType type) {
      super(JavaSymbol.ERRONEOUS, 0, null, null);
      this.symbol = symbol;
      this.type = type;
    }
  }
}
