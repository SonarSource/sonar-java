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

import java.util.List;

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

  private final SymbolNotFound symbolNotFound = new SymbolNotFound();

  private final BytecodeCompleter bytecodeCompleter;
  private final Types types = new Types();
  private final Symbols symbols;

  public Resolve(Symbols symbols, BytecodeCompleter bytecodeCompleter) {
    this.symbols = symbols;
    this.bytecodeCompleter = bytecodeCompleter;
  }

  private static Symbol.TypeSymbol superclassSymbol(Symbol.TypeSymbol c) {
    Type supertype = c.getSuperclass();
    return supertype == null ? null : supertype.symbol;
  }

  public Symbol.TypeSymbol registerClass(Symbol.TypeSymbol classSymbol) {
    return bytecodeCompleter.registerClass(classSymbol);
  }

  public Scope createStarImportScope(Symbol owner) {
    return new Scope.StarImportScope(owner, bytecodeCompleter);
  }

  public Scope createStaticStarImportScope(Symbol owner) {
    return new Scope.StaticStarImportScope(owner, bytecodeCompleter);
  }

  private Type resolveTypeSubstitution(Type type, Type definition) {
    if(definition instanceof Type.ParametrizedTypeType) {
      Type substitution = ((Type.ParametrizedTypeType) definition).typeSubstitution.get(type);
      if(substitution != null) {
        return substitution;
      }
    }
    return type;
  }

  /**
   * Finds field with given name.
   */
  private Resolution findField(Env env, Symbol.TypeSymbol site, String name, Symbol.TypeSymbol c) {
    Resolution bestSoFar = unresolved();
    Resolution resolution = new Resolution();
    for (Symbol symbol : c.members().lookup(name)) {
      if (symbol.kind == Symbol.VAR) {
        if(isAccessible(env, site, symbol)) {
          resolution.symbol = symbol;
          resolution.type = resolveTypeSubstitution(symbol.type, c.type);
          return resolution;
        } else {
          return Resolution.resolution(new AccessErrorSymbol(symbol, symbols.unknownType));
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
    for (Type interfaceType : c.getInterfaces()) {
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
      for (Symbol symbol : env1.scope().lookup(name)) {
        if (symbol.kind == Symbol.VAR) {
          sym.symbol = symbol;
        }
      }
      if (sym.symbol == null) {
        sym = findField(env1, env1.enclosingClass(), name, env1.enclosingClass());
      }
      if (sym.symbol.kind < Symbol.ERRONEOUS) {
        // symbol exists
        return sym;
      } else if (sym.symbol.kind < bestSoFar.symbol.kind) {
        bestSoFar = sym;
      }
      env1 = env1.outer();
    }

    Symbol symbol = findInStaticImport(env, name, Symbol.VAR);
    if (symbol.kind < Symbol.ERRONEOUS) {
      // symbol exists
      return Resolution.resolution(symbol);
    } else if (symbol.kind < bestSoFar.symbol.kind) {
      bestSoFar = Resolution.resolution(symbol);
    }
    return bestSoFar;
  }

  /**
   * @param kind subset of {@link org.sonar.java.resolve.Symbol#VAR}, {@link org.sonar.java.resolve.Symbol#MTH}
   */
  private Symbol findInStaticImport(Env env, String name, int kind) {
    Symbol bestSoFar = symbolNotFound;
    //imports
    //Ok because clash of name between type and var/method result in compile error: JLS8 7.5.3
    for (Symbol symbol : env.namedImports().lookup(name)) {
      if ((kind & symbol.kind) != 0) {
        return symbol;
      }
    }
    for (Symbol symbol : env.staticStarImports().lookup(name)) {
      if ((kind & symbol.kind) != 0) {
        return symbol;
      }
    }
    return bestSoFar;
  }

  private Symbol findMemberType(Env env, Symbol.TypeSymbol site, String name, Symbol.TypeSymbol c) {
    Symbol bestSoFar = symbolNotFound;
    for (Symbol symbol : c.members().lookup(name)) {
      if (symbol.kind == Symbol.TYP) {
        return isAccessible(env, site, symbol)
            ? symbol
            : new AccessErrorSymbol(symbol, symbols.unknownType);
      }
    }
    if (c.getSuperclass() != null) {
      Symbol symbol = findMemberType(env, site, name, c.getSuperclass().symbol);
      if (symbol.kind < bestSoFar.kind) {
        bestSoFar = symbol;
      }
    }
    for (Type interfaceType : c.getInterfaces()) {
      Symbol symbol = findMemberType(env, site, name, interfaceType.symbol);
      if (symbol.kind < bestSoFar.kind) {
        bestSoFar = symbol;
      }
    }
    return bestSoFar;
  }

  /**
   * Finds type with given name.
   */
  private Symbol findType(Env env, String name) {
    Symbol bestSoFar = symbolNotFound;
    for (Env env1 = env; env1 != null; env1 = env1.outer()) {
      for (Symbol symbol : env1.scope().lookup(name)) {
        if (symbol.kind == Symbol.TYP) {
          return symbol;
        }
      }
      if (env1.outer != null) {
        Symbol symbol = findMemberType(env1, env1.enclosingClass(), name, env1.enclosingClass());
        if (symbol.kind < Symbol.ERRONEOUS) {
          // symbol exists
          return symbol;
        } else if (symbol.kind < bestSoFar.kind) {
          bestSoFar = symbol;
        }
      }
    }

    //checks predefined types
    Symbol predefinedSymbol = findMemberType(env, symbols.predefClass, name, symbols.predefClass);
    if (predefinedSymbol.kind < bestSoFar.kind) {
      return predefinedSymbol;
    }

    //JLS8 6.4.1 Shadowing rules
    //named imports
    for (Symbol symbol : env.namedImports().lookup(name)) {
      if (symbol.kind == Symbol.TYP) {
        return symbol;
      }
    }
    //package types
    Symbol sym = findIdentInPackage(env.packge(), name, Symbol.TYP);
    if (sym.kind < bestSoFar.kind) {
      return sym;
    }
    //on demand imports
    for (Symbol symbol : env.starImports().lookup(name)) {
      if (symbol.kind == Symbol.TYP) {
        return symbol;
      }
    }
    //java.lang
    Symbol.PackageSymbol javaLang = bytecodeCompleter.enterPackage("java.lang");
    for (Symbol symbol : javaLang.members().lookup(name)) {
      if (symbol.kind == Symbol.TYP) {
        return symbol;
      }
    }
    return bestSoFar;
  }

  /**
   * @param kind subset of {@link org.sonar.java.resolve.Symbol#VAR}, {@link org.sonar.java.resolve.Symbol#TYP}, {@link org.sonar.java.resolve.Symbol#PCK}
   */
  public Resolution findIdent(Env env, String name, int kind) {
    Resolution bestSoFar = unresolved();
    if ((kind & Symbol.VAR) != 0) {
      Resolution res = findVar(env, name);
      if (res.symbol.kind < Symbol.ERRONEOUS) {
        // symbol exists
        return res;
      } else if (res.symbol.kind < bestSoFar.symbol.kind) {
        bestSoFar = res;
      }
    }
    if ((kind & Symbol.TYP) != 0) {
      Resolution res = new Resolution();
      res.symbol = findType(env, name);
      if (res.symbol.kind < Symbol.ERRONEOUS) {
        // symbol exists
        return res;
      } else if (res.symbol.kind < bestSoFar.symbol.kind) {
        bestSoFar = res;
      }
    }
    if ((kind & Symbol.PCK) != 0) {
      Resolution res = new Resolution();
      res.symbol = findIdentInPackage(symbols.defaultPackage, name, Symbol.PCK);
      if (res.symbol.kind < Symbol.ERRONEOUS) {
        // symbol exists
        return res;
      } else if (res.symbol.kind < bestSoFar.symbol.kind) {
        bestSoFar = res;
      }
    }
    return bestSoFar;
  }

  /**
   * @param kind subset of {@link Symbol#TYP}, {@link Symbol#PCK}
   */
  public Symbol findIdentInPackage(Symbol site, String name, int kind) {
    String fullname = bytecodeCompleter.formFullName(name, site);
    Symbol bestSoFar = symbolNotFound;
    //Try to find a type matching the name.
    if ((kind & Symbol.TYP) != 0) {
      Symbol sym = bytecodeCompleter.loadClass(fullname);
      if (sym.kind < bestSoFar.kind) {
        bestSoFar = sym;
      }
    }
    //We did not find the class so identifier must be a package.
    if ((kind & Symbol.PCK) != 0 && bestSoFar.kind >= symbolNotFound.kind) {
      bestSoFar = bytecodeCompleter.enterPackage(fullname);
    }
    return bestSoFar;
  }

  /**
   * @param kind subset of {@link Symbol#VAR}, {@link Symbol#TYP}
   */
  public Symbol findIdentInType(Env env, Symbol.TypeSymbol site, String name, int kind) {
    Symbol bestSoFar = symbolNotFound;
    Symbol symbol;
    if ((kind & Symbol.VAR) != 0) {
      symbol = findField(env, site, name, site).symbol;
      if (symbol.kind < Symbol.ERRONEOUS) {
        // symbol exists
        return symbol;
      } else if (symbol.kind < bestSoFar.kind) {
        bestSoFar = symbol;
      }
    }
    if ((kind & Symbol.TYP) != 0) {
      symbol = findMemberType(env, site, name, site);
      if (symbol.kind < Symbol.ERRONEOUS) {
        // symbol exists
        return symbol;
      } else if (symbol.kind < bestSoFar.kind) {
        bestSoFar = symbol;
      }
    }
    return bestSoFar;
  }

  /**
   * Finds method matching given name and types of arguments.
   */
  public Resolution findMethod(Env env, String name, List<Type> argTypes) {
    Resolution bestSoFar = unresolved();
    Env env1 = env;
    while (env1.outer() != null) {
      Resolution res = findMethod(env1, env1.enclosingClass().getType(), name, argTypes);
      if (res.symbol.kind < Symbol.ERRONEOUS) {
        // symbol exists
        return res;
      } else if (res.symbol.kind < bestSoFar.symbol.kind) {
        bestSoFar = res;
      }
      env1 = env1.outer;
    }
    Symbol sym = findInStaticImport(env, name, Symbol.MTH);
    if (sym.kind < Symbol.ERRONEOUS) {
      // symbol exists
      return Resolution.resolution(sym);
    } else if (sym.kind < bestSoFar.symbol.kind) {
      bestSoFar = Resolution.resolution(sym);
    }
    return bestSoFar;
  }

  public Resolution findMethod(Env env, Type site, String name, List<Type> argTypes) {
    return findMethod(env, site, name, argTypes, false);
  }

  private Resolution findMethod(Env env, Type site, String name, List<Type> argTypes, boolean autoboxing) {
    Resolution bestSoFar = unresolved();
    for (Symbol symbol : site.getSymbol().members().lookup(name)) {
      if (symbol.kind == Symbol.MTH) {
        Symbol best = selectBest(env, site.getSymbol(), argTypes, symbol, bestSoFar.symbol, autoboxing);
        if(best == symbol) {
          bestSoFar = Resolution.resolution(best);
          if(best.isKind(Symbol.MTH)) {
            bestSoFar.type = resolveTypeSubstitution(((Type.MethodType) best.type).resultType, site);
          }
        }
      }
    }
    //look in supertypes for more specialized method (overloading).
    if (site.getSymbol().getSuperclass() != null) {
      Resolution method = findMethod(env, site.getSymbol().getSuperclass(), name, argTypes);
      Symbol best = selectBest(env, site.getSymbol(), argTypes, method.symbol, bestSoFar.symbol, autoboxing);
      if(best == method.symbol) {
        bestSoFar = method;
      }
    }
    for (Type interfaceType : site.getSymbol().getInterfaces()) {
      Resolution method = findMethod(env, interfaceType, name, argTypes);
      Symbol best = selectBest(env, site.getSymbol(), argTypes, method.symbol, bestSoFar.symbol, autoboxing);
      if(best == method.symbol) {
        bestSoFar = method;
      }
    }
    if(bestSoFar.symbol.kind >= Symbol.ERRONEOUS && !autoboxing) {
      bestSoFar = findMethod(env, site, name, argTypes, true);
    }
    return bestSoFar;
  }

  /**
   * @param symbol    candidate
   * @param bestSoFar previously found best match
   */
  private Symbol selectBest(Env env, Symbol.TypeSymbol site, List<Type> argTypes, Symbol symbol, Symbol bestSoFar, boolean autoboxing) {
    // TODO get rid of null check
    if (symbol.kind >= Symbol.ERRONEOUS || !isInheritedIn(symbol, site) || symbol.type == null) {
      return bestSoFar;
    }
    boolean isVarArgs = ((Symbol.MethodSymbol) symbol).isVarArgs();
    if (!isArgumentsAcceptable(argTypes, ((Type.MethodType) symbol.type).argTypes, isVarArgs, autoboxing)) {
      return bestSoFar;
    }
    // TODO ambiguity, errors, ...
    if (!isAccessible(env, site, symbol)) {
      return new AccessErrorSymbol(symbol, symbols.unknownType);
    }
    Symbol mostSpecific = selectMostSpecific(symbol, bestSoFar, argTypes);
    if (mostSpecific.isKind(Symbol.AMBIGUOUS)) {
      //same signature, we keep the first symbol found (overrides the other one).
      mostSpecific = bestSoFar;
    }
    return mostSpecific;
  }

  /**
   * @param argTypes types of arguments
   * @param formals  types of formal parameters of method
   */
  private boolean isArgumentsAcceptable(List<Type> argTypes, List<Type> formals, boolean isVarArgs, boolean autoboxing) {
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
      Type.ArrayType lastFormal = (Type.ArrayType) formals.get(formalsSize - 1);
      Type argType = argTypes.get(argsSize - i);
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

  private boolean isAcceptableType(Type arg, Type formal, boolean autoboxing) {
    return types.isSubtype(arg.erasure(), formal.erasure()) || (autoboxing && isAcceptableByAutoboxing(arg, formal));
  }

  private boolean isAcceptableByAutoboxing(Type expressionType, Type formalType) {
    if (expressionType.isPrimitive()) {
      return types.isSubtype(symbols.boxedTypes.get(expressionType), formalType);
    } else {
      Type unboxedType = symbols.boxedTypes.inverse().get(expressionType);
      if (unboxedType != null) {
        return types.isSubtype(unboxedType, formalType);
      }
    }
    return false;
  }

  /**
   * JLS7 15.12.2.5. Choosing the Most Specific Method
   */
  private Symbol selectMostSpecific(Symbol m1, Symbol m2, List<Type> argTypes) {
    // FIXME get rig of null check
    if (m2.type == null || !m2.isKind(Symbol.MTH)) {
      return m1;
    }
    boolean m1SignatureMoreSpecific = isSignatureMoreSpecific(m1, m2);
    boolean m2SignatureMoreSpecific = isSignatureMoreSpecific(m2, m1);
    if (m1SignatureMoreSpecific && m2SignatureMoreSpecific) {
      return new AmbiguityErrorSymbol();
    } else if (m1SignatureMoreSpecific) {
      return m1;
    } else if (m2SignatureMoreSpecific) {
      return m2;
    } else {
      return new AmbiguityErrorSymbol();
    }
  }

  /**
   * @return true, if signature of m1 is more specific than signature of m2
   */
  private boolean isSignatureMoreSpecific(Symbol m1, Symbol m2) {
    //TODO handle specific signature with varargs
    // ((MethodSymbol) m2).isVarArgs()
    return isArgumentsAcceptable(((Type.MethodType) m1.type).argTypes, ((Type.MethodType) m2.type).argTypes, false, false);
  }

  /**
   * Is class accessible in given environment?
   */
  @VisibleForTesting
  boolean isAccessible(Env env, Symbol.TypeSymbol c) {
    final boolean result;
    switch (c.flags() & Flags.ACCESS_FLAGS) {
      case Flags.PRIVATE:
        result = (env.enclosingClass().outermostClass() == c.owner().outermostClass());
        break;
      case 0:
        result = (env.packge() == c.packge());
        break;
      case Flags.PUBLIC:
        result = true;
        break;
      case Flags.PROTECTED:
        result = (env.packge() == c.packge()) || isInnerSubClass(env.enclosingClass(), c.owner());
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
  private boolean isInnerSubClass(Symbol.TypeSymbol c, Symbol base) {
    while (c != null && isSubClass(c, base)) {
      c = c.owner().enclosingClass();
    }
    return c != null;
  }

  /**
   * Is given class a subclass of given base class?
   */
  @VisibleForTesting
  boolean isSubClass(Symbol.TypeSymbol c, Symbol base) {
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
      for (Type interfaceType : c.getInterfaces()) {
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
  private boolean isAccessible(Env env, Symbol.TypeSymbol site, Symbol symbol) {
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

  private boolean notOverriddenIn(Symbol.TypeSymbol site, Symbol symbol) {
    // TODO see Javac
    return true;
  }

  /**
   * Is symbol inherited in given class?
   */
  @VisibleForTesting
  boolean isInheritedIn(Symbol symbol, Symbol.TypeSymbol clazz) {
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
        Symbol.PackageSymbol thisPackage = symbol.packge();
        for (Symbol.TypeSymbol sup = clazz; sup != null && sup != symbol.owner(); sup = superclassSymbol(sup)) {
          if (sup.packge() != thisPackage) {
            return false;
          }
        }
        return true;
      default:
        throw new IllegalStateException();
    }
  }

  private boolean isProtectedAccessible(Symbol symbol, Symbol.TypeSymbol c, Symbol.TypeSymbol site) {
    // TODO see Javac
    return true;
  }

  /**
   * Resolution holds the symbol resolved and its type in this context.
   * This is required to handle type substitution for generics.
   */
  static class Resolution {

    private Symbol symbol;
    private Type type;

    private Resolution(Symbol symbol) {
      this.symbol = symbol;
    }

    Resolution() {
    }

    static Resolution resolution(Symbol symbol) {
      return new Resolution(symbol);
    }

    Symbol symbol() {
      return symbol;
    }

    public Type type() {
      if (type == null) {
        if(symbol.isKind(Symbol.MTH)) {
          return ((Type.MethodType)symbol.type).resultType;
        }
        return symbol.type;
      }
      return type;
    }
  }

  private Resolution unresolved() {
    Resolution resolution = new Resolution(symbolNotFound);
    resolution.type = symbols.unknownType;
    return resolution;
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

    Symbol.PackageSymbol packge;

    Symbol.TypeSymbol enclosingClass;

    Scope scope;
    Scope namedImports;
    Scope starImports;
    Scope staticStarImports;

    Env outer() {
      return outer;
    }

    Symbol.TypeSymbol enclosingClass() {
      return enclosingClass;
    }

    public Symbol.PackageSymbol packge() {
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

  public static class SymbolNotFound extends Symbol {
    public SymbolNotFound() {
      super(Symbol.ABSENT, 0, null, null);
    }
  }

  public static class AmbiguityErrorSymbol extends Symbol {
    public AmbiguityErrorSymbol() {
      super(Symbol.AMBIGUOUS, 0, null, null);
    }
  }

  public static class AccessErrorSymbol extends Symbol {
    /**
     * The invalid symbol found during resolution.
     */
    Symbol symbol;

    public AccessErrorSymbol(Symbol symbol, Type type) {
      super(Symbol.ERRONEOUS, 0, null, null);
      this.symbol = symbol;
      this.type = type;
    }
  }

}
