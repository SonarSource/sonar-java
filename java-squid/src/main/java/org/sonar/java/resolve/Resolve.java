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

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

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

  private final JavaSymbolNotFound symbolNotFound = new JavaSymbolNotFound();

  private final BytecodeCompleter bytecodeCompleter;
  private final ParametrizedTypeCache parametrizedTypeCache;
  private final Types types = new Types();
  private final Symbols symbols;

  public Resolve(Symbols symbols, BytecodeCompleter bytecodeCompleter, ParametrizedTypeCache parametrizedTypeCache) {
    this.symbols = symbols;
    this.bytecodeCompleter = bytecodeCompleter;
    this.parametrizedTypeCache = parametrizedTypeCache;
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

  public JavaType resolveTypeSubstitution(JavaType type, JavaType definition) {
    if(definition instanceof JavaType.ParametrizedTypeJavaType) {
      return substituteTypeParameter(type, ((JavaType.ParametrizedTypeJavaType) definition).typeSubstitution);
    }
    return type;
  }

  private JavaType substituteTypeParameter(JavaType type, TypeSubstitution substitution) {
    JavaType substitutedType = substitution.substitutedType(type);
    if (substitutedType != null) {
      return substitutedType;
    }
    if(type instanceof JavaType.ParametrizedTypeJavaType) {
      JavaType.ParametrizedTypeJavaType ptt = (JavaType.ParametrizedTypeJavaType) type;
      TypeSubstitution newSubstitution = new TypeSubstitution();
      for (Map.Entry<JavaType.TypeVariableJavaType, JavaType> entry : ptt.typeSubstitution.substitutionEntries()) {
        newSubstitution.add(entry.getKey(), substituteTypeParameter(entry.getValue(), substitution));
      }
      return parametrizedTypeCache.getParametrizedTypeType(ptt.rawType.getSymbol(), newSubstitution);
    }
    return type;
  }

  /**
   * Finds field with given name.
   */
  private Resolution findField(Env env, JavaSymbol.TypeJavaSymbol site, String name, JavaSymbol.TypeJavaSymbol c) {
    Resolution bestSoFar = unresolved();
    Resolution resolution = new Resolution();
    for (JavaSymbol symbol : c.members().lookup(name)) {
      if (symbol.kind == JavaSymbol.VAR) {
        if(isAccessible(env, site, symbol)) {
          resolution.symbol = symbol;
          resolution.type = resolveTypeSubstitution(symbol.type, c.type);
          return resolution;
        } else {
          return Resolution.resolution(new AccessErrorJavaSymbol(symbol, Symbols.unknownType));
        }
      }
    }
    if (c.getSuperclass() != null) {
      resolution = findField(env, site, name, c.getSuperclass().symbol);
      if (resolution.symbol.kind < bestSoFar.symbol.kind) {
        resolution.type = resolveTypeSubstitution(resolution.symbol.type, c.getSuperclass());
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

  /**
   * Finds variable or field with given name.
   */
  private Resolution findVar(Env env, String name) {
    Resolution bestSoFar = unresolved();

    Env env1 = env;
    while (env1.outer() != null) {
      Resolution sym = new Resolution();
      for (JavaSymbol symbol : env1.scope().lookup(name)) {
        if (symbol.kind == JavaSymbol.VAR) {
          sym.symbol = symbol;
        }
      }
      if (sym.symbol == null) {
        sym = findField(env1, env1.enclosingClass(), name, env1.enclosingClass());
      }
      if (sym.symbol.kind < JavaSymbol.ERRONEOUS) {
        // symbol exists
        return sym;
      } else if (sym.symbol.kind < bestSoFar.symbol.kind) {
        bestSoFar = sym;
      }
      env1 = env1.outer();
    }

    JavaSymbol symbol = findInStaticImport(env, name, JavaSymbol.VAR);
    if (symbol.kind < JavaSymbol.ERRONEOUS) {
      // symbol exists
      return Resolution.resolution(symbol);
    } else if (symbol.kind < bestSoFar.symbol.kind) {
      bestSoFar = Resolution.resolution(symbol);
    }
    return bestSoFar;
  }

  /**
   * @param kind subset of {@link JavaSymbol#VAR}, {@link JavaSymbol#MTH}
   */
  private JavaSymbol findInStaticImport(Env env, String name, int kind) {
    JavaSymbol bestSoFar = symbolNotFound;
    //imports
    //Ok because clash of name between type and var/method result in compile error: JLS8 7.5.3
    for (JavaSymbol symbol : env.namedImports().lookup(name)) {
      if ((kind & symbol.kind) != 0) {
        return symbol;
      }
    }
    for (JavaSymbol symbol : env.staticStarImports().lookup(name)) {
      if ((kind & symbol.kind) != 0) {
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
    for (JavaType interfaceType : c.getInterfaces()) {
      JavaSymbol symbol = findMemberType(env, site, name, interfaceType.symbol);
      if (symbol.kind < bestSoFar.kind) {
        bestSoFar = symbol;
      }
    }
    return bestSoFar;
  }

  /**
   * Finds type with given name.
   */
  private JavaSymbol findType(Env env, String name) {
    JavaSymbol bestSoFar = symbolNotFound;
    for (Env env1 = env; env1 != null; env1 = env1.outer()) {
      for (JavaSymbol symbol : env1.scope().lookup(name)) {
        if (symbol.kind == JavaSymbol.TYP) {
          return symbol;
        }
      }
      if (env1.outer != null) {
        JavaSymbol symbol = findMemberType(env1, env1.enclosingClass(), name, env1.enclosingClass());
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
    for (JavaSymbol symbol : env.namedImports().lookup(name)) {
      if (symbol.kind == JavaSymbol.TYP) {
        return symbol;
      }
    }
    //package types
    JavaSymbol sym = findIdentInPackage(env.packge(), name, JavaSymbol.TYP);
    if (sym.kind < bestSoFar.kind) {
      return sym;
    }
    //on demand imports
    for (JavaSymbol symbol : env.starImports().lookup(name)) {
      if (symbol.kind == JavaSymbol.TYP) {
        return symbol;
      }
    }
    //java.lang
    JavaSymbol.PackageJavaSymbol javaLang = bytecodeCompleter.enterPackage("java.lang");
    for (JavaSymbol symbol : javaLang.members().lookup(name)) {
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
    while (env1.outer() != null) {
      Resolution res = findMethod(env1, env1.enclosingClass().getType(), name, argTypes, typeParamTypes);
      if (res.symbol.kind < JavaSymbol.ERRONEOUS) {
        // symbol exists
        return res;
      } else if (res.symbol.kind < bestSoFar.symbol.kind) {
        bestSoFar = res;
      }
      env1 = env1.outer;
    }
    JavaSymbol sym = findInStaticImport(env, name, JavaSymbol.MTH);
    if (sym.kind < JavaSymbol.ERRONEOUS) {
      // symbol exists
      return Resolution.resolution(sym);
    } else if (sym.kind < bestSoFar.symbol.kind) {
      bestSoFar = Resolution.resolution(sym);
    }
    return bestSoFar;
  }

  public Resolution findMethod(Env env, JavaType site, String name, List<JavaType> argTypes) {
    return findMethod(env, site, name, argTypes, ImmutableList.<JavaType>of(), false);
  }
  public Resolution findMethod(Env env, JavaType site, String name, List<JavaType> argTypes, List<JavaType> typeParams) {
    return findMethod(env, site, name, argTypes, typeParams, false);
  }

  private Resolution findMethod(Env env, JavaType site, String name, List<JavaType> argTypes, List<JavaType> typeParams, boolean autoboxing) {
    Resolution bestSoFar = unresolved();
    for (JavaSymbol symbol : site.getSymbol().members().lookup(name)) {
      if (symbol.kind == JavaSymbol.MTH) {
        JavaSymbol best = selectBest(env, site.getSymbol(), argTypes, symbol, bestSoFar.symbol, autoboxing);
        if(best == symbol) {
          bestSoFar = Resolution.resolution(best);
          bestSoFar.type = resolveTypeSubstitution(((JavaType.MethodJavaType) best.type).resultType, site);
          JavaSymbol.MethodJavaSymbol methodSymbol = (JavaSymbol.MethodJavaSymbol) best;
          bestSoFar.type = handleTypeArguments(typeParams, bestSoFar.type, methodSymbol);
        }
      }
    }
    //look in supertypes for more specialized method (overloading).
    if (site.getSymbol().getSuperclass() != null) {
      Resolution method = findMethod(env, site.getSymbol().getSuperclass(), name, argTypes, typeParams);
      JavaSymbol best = selectBest(env, site.getSymbol(), argTypes, method.symbol, bestSoFar.symbol, autoboxing);
      if(best == method.symbol) {
        bestSoFar = method;
      }
    }
    for (JavaType interfaceType : site.getSymbol().getInterfaces()) {
      Resolution method = findMethod(env, interfaceType, name, argTypes, typeParams);
      JavaSymbol best = selectBest(env, site.getSymbol(), argTypes, method.symbol, bestSoFar.symbol, autoboxing);
      if(best == method.symbol) {
        bestSoFar = method;
      }
    }
    if(bestSoFar.symbol.kind >= JavaSymbol.ERRONEOUS && !autoboxing) {
      bestSoFar = findMethod(env, site, name, argTypes, typeParams, true);
    }
    return bestSoFar;
  }

  private JavaType handleTypeArguments(List<JavaType> typeParams, JavaType type, JavaSymbol.MethodJavaSymbol methodSymbol) {
    if (!typeParams.isEmpty() && methodSymbol.typeVariableTypes.size() == typeParams.size()) {
      TypeSubstitution typeSubstitution = new TypeSubstitution();
      int i = 0;
      for (JavaType.TypeVariableJavaType typeVariableType : methodSymbol.typeVariableTypes) {
        typeSubstitution.add(typeVariableType, typeParams.get(i));
        i++;
      }
      return substituteTypeParameter(type, typeSubstitution);
    }
    return type;
  }

  /**
   * @param symbol    candidate
   * @param bestSoFar previously found best match
   */
  private JavaSymbol selectBest(Env env, JavaSymbol.TypeJavaSymbol site, List<JavaType> argTypes, JavaSymbol symbol, JavaSymbol bestSoFar, boolean autoboxing) {
    // TODO get rid of null check
    if (symbol.kind >= JavaSymbol.ERRONEOUS || !isInheritedIn(symbol, site) || symbol.type == null) {
      return bestSoFar;
    }
    boolean isVarArgs = ((JavaSymbol.MethodJavaSymbol) symbol).isVarArgs();
    if (!isArgumentsAcceptable(argTypes, ((JavaType.MethodJavaType) symbol.type).argTypes, isVarArgs, autoboxing)) {
      return bestSoFar;
    }
    // TODO ambiguity, errors, ...
    if (!isAccessible(env, site, symbol)) {
      return new AccessErrorJavaSymbol(symbol, Symbols.unknownType);
    }
    JavaSymbol mostSpecific = selectMostSpecific(symbol, bestSoFar, argTypes);
    if (mostSpecific.isKind(JavaSymbol.AMBIGUOUS)) {
      //same signature, we keep the first symbol found (overrides the other one).
      mostSpecific = bestSoFar;
    }
    return mostSpecific;
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
      //Check at least last parameter for varags compatibility
      nbArgToCheck += 1;
      if (nbArgToCheck < 0) {
        //arity is not correct, it can only differ negatively by one for varargs.
        return false;
      }
    } else if (nbArgToCheck != 0) {
      //Not a vararg, we should have same number of arguments
      return false;
    }
    for (int i = 1; i <= nbArgToCheck; i++) {
      JavaType.ArrayJavaType lastFormal = (JavaType.ArrayJavaType) formals.get(formalsSize - 1);
      JavaType argType = argTypes.get(argsSize - i);
      //Check type of element of array or if we invoke with an array that it is a compatible array type
      if (!isAcceptableType(argType, lastFormal.elementType, autoboxing) && (nbArgToCheck != 1 || !types.isSubtype(argType, lastFormal))) {
        return false;
      }
    }
    for (int i = 0; i < argsSize - nbArgToCheck; i++) {
      if (!isAcceptableType(argTypes.get(i), formals.get(i), autoboxing)) {
        return false;
      }
    }
    return true;
  }

  private boolean isAcceptableType(JavaType arg, JavaType formal, boolean autoboxing) {
    return types.isSubtype(arg.erasure(), formal.erasure()) || (autoboxing && isAcceptableByAutoboxing(arg, formal.erasure()));
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
  private JavaSymbol selectMostSpecific(JavaSymbol m1, JavaSymbol m2, List<JavaType> argTypes) {
    // FIXME get rig of null check
    if (m2.type == null || !m2.isKind(JavaSymbol.MTH)) {
      return m1;
    }
    boolean m1SignatureMoreSpecific = isSignatureMoreSpecific(m1, m2);
    boolean m2SignatureMoreSpecific = isSignatureMoreSpecific(m2, m1);
    if (m1SignatureMoreSpecific && m2SignatureMoreSpecific) {
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
  private boolean isSignatureMoreSpecific(JavaSymbol m1, JavaSymbol m2) {
    //TODO handle specific signature with varargs
    // ((MethodSymbol) m2).isVarArgs()
    return isArgumentsAcceptable(((JavaType.MethodJavaType) m1.type).argTypes, ((JavaType.MethodJavaType) m2.type).argTypes, false, false);
  }

  /**
   * Is class accessible in given environment?
   */
  @VisibleForTesting
  boolean isAccessible(Env env, JavaSymbol.TypeJavaSymbol c) {
    final boolean result;
    switch (c.flags() & Flags.ACCESS_FLAGS) {
      case Flags.PRIVATE:
        result = env.enclosingClass().outermostClass() == c.owner().outermostClass();
        break;
      case 0:
        result = env.packge() == c.packge();
        break;
      case Flags.PUBLIC:
        result = true;
        break;
      case Flags.PROTECTED:
        result = env.packge() == c.packge() || isInnerSubClass(env.enclosingClass(), c.owner());
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
  private boolean isInnerSubClass(JavaSymbol.TypeJavaSymbol c, JavaSymbol base) {
    while (c != null && isSubClass(c, base)) {
      c = c.owner().enclosingClass();
    }
    return c != null;
  }

  /**
   * Is given class a subclass of given base class?
   */
  @VisibleForTesting
  boolean isSubClass(JavaSymbol.TypeJavaSymbol c, JavaSymbol base) {
    // TODO get rid of null check
    if (c == null) {
      return false;
    }
    // TODO see Javac
    if (c == base) {
      // same class
      return true;
    } else if ((base.flags() & Flags.INTERFACE) != 0) {
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
  private boolean isAccessible(Env env, JavaSymbol.TypeJavaSymbol site, JavaSymbol symbol) {
    switch (symbol.flags() & Flags.ACCESS_FLAGS) {
      case Flags.PRIVATE:
        //if enclosing class is null, we are checking accessibility for imports so we return false.
        // no check of overriding, because private members cannot be overridden
        return env.enclosingClass != null && (env.enclosingClass().outermostClass() == symbol.owner().outermostClass())
            && isInheritedIn(symbol, site);
      case 0:
        return (env.packge() == symbol.packge())
            && isAccessible(env, site)
            && isInheritedIn(symbol, site)
            && notOverriddenIn(site, symbol);
      case Flags.PUBLIC:
        return isAccessible(env, site)
            && notOverriddenIn(site, symbol);
      case Flags.PROTECTED:
        return ((env.packge() == symbol.packge()) || isProtectedAccessible(symbol, env.enclosingClass, site))
            && isAccessible(env, site)
            && notOverriddenIn(site, symbol);
      default:
        throw new IllegalStateException();
    }
  }

  private static boolean notOverriddenIn(JavaSymbol.TypeJavaSymbol site, JavaSymbol symbol) {
    // TODO see Javac
    return true;
  }

  /**
   * Is symbol inherited in given class?
   */
  @VisibleForTesting
  boolean isInheritedIn(JavaSymbol symbol, JavaSymbol.TypeJavaSymbol clazz) {
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

  Resolution unresolved() {
    Resolution resolution = new Resolution(symbolNotFound);
    resolution.type = Symbols.unknownType;
    return resolution;
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
          return ((JavaType.MethodJavaType)symbol.type).resultType;
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
    Env outer;

    JavaSymbol.PackageJavaSymbol packge;

    JavaSymbol.TypeJavaSymbol enclosingClass;

    Scope scope;
    Scope namedImports;
    Scope starImports;
    Scope staticStarImports;

    @Nullable
    Env outer() {
      return outer;
    }

    JavaSymbol.TypeJavaSymbol enclosingClass() {
      return enclosingClass;
    }

    public JavaSymbol.PackageJavaSymbol packge() {
      return packge;
    }

    Scope namedImports() {
      return namedImports;
    }

    Scope starImports() {
      return starImports;
    }

    public Scope staticStarImports() {
      return staticStarImports;
    }

    Scope scope() {
      return scope;
    }

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
