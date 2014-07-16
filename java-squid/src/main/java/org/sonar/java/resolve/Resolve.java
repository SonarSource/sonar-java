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

  public Symbol.TypeSymbol registerClass(Symbol.TypeSymbol classSymbol) {
    return bytecodeCompleter.registerClass(classSymbol);
  }

  public Scope createStarImportScope(Symbol owner) {
    return new Scope.StarImportScope(owner, bytecodeCompleter);
  }

  public Scope createStaticStarImportScope(Symbol owner) {
    return new Scope.StaticStarImportScope(owner, bytecodeCompleter);
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

  /**
   * Finds field with given name.
   */
  private Symbol findField(Env env, Symbol.TypeSymbol site, String name, Symbol.TypeSymbol c) {
    Symbol bestSoFar = symbolNotFound;
    for (Symbol symbol : c.members().lookup(name)) {
      if (symbol.kind == Symbol.VAR) {
        return isAccessible(env, site, symbol)
            ? symbol
            : new AccessErrorSymbol(symbol);
      }
    }
    Symbol symbol;
    if (c.getSuperclass() != null) {
      symbol = findField(env, site, name, c.getSuperclass().symbol);
      if (symbol.kind < bestSoFar.kind) {
        bestSoFar = symbol;
      }
    }
    for (Type interfaceType : c.getInterfaces()) {
      symbol = findField(env, site, name, interfaceType.symbol);
      if (symbol.kind < bestSoFar.kind) {
        bestSoFar = symbol;
      }
    }
    return bestSoFar;
  }

  /**
   * Finds variable or field with given name.
   */
  private Symbol findVar(Env env, String name) {
    Symbol bestSoFar = symbolNotFound;

    Env env1 = env;
    while (env1.outer() != null) {
      Symbol sym = null;
      for (Symbol symbol : env1.scope().lookup(name)) {
        if (symbol.kind == Symbol.VAR) {
          sym = symbol;
        }
      }
      if (sym == null) {
        sym = findField(env1, env1.enclosingClass(), name, env1.enclosingClass());
      }
      if (sym.kind < Symbol.ERRONEOUS) {
        // symbol exists
        return sym;
      } else if (sym.kind < bestSoFar.kind) {
        bestSoFar = sym;
      }
      env1 = env1.outer();
    }

    //imports
    //TODO rules to distinguish static imports ??
    for (Symbol symbol : env.namedImports().lookup(name)) {
      if (symbol.kind < bestSoFar.kind) {
        return symbol;
      }
    }
    for (Symbol symbol : env.staticStarImports().lookup(name)) {
      if (symbol.kind < bestSoFar.kind) {
        return symbol;
      }
    }

    return bestSoFar;
  }

  public Symbol findMemberType(Env env, Symbol.TypeSymbol site, String name, Symbol.TypeSymbol c) {
    Symbol bestSoFar = symbolNotFound;
    for (Symbol symbol : c.members().lookup(name)) {
      if (symbol.kind == Symbol.TYP) {
        return isAccessible(env, site, symbol)
            ? symbol
            : new AccessErrorSymbol(symbol);
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
    //TODO check that symbol is indeed a type.
    //named imports
    for (Symbol symbol : env.namedImports().lookup(name)) {
      if (symbol.kind < bestSoFar.kind) {
        return symbol;
      }
    }
    //package types
    Symbol sym = findIdentInPackage(env, env.packge(), name, Symbol.TYP);
    if (sym.kind < bestSoFar.kind) {
      return sym;
    }
    //on demand imports
    for (Symbol symbol : env.starImports().lookup(name)) {
      if (symbol.kind < bestSoFar.kind) {
        return symbol;
      }
    }
    //java.lang
    Symbol.PackageSymbol javaLang = bytecodeCompleter.enterPackage("java.lang");
    for (Symbol symbol : javaLang.members().lookup(name)) {
      if (symbol.kind < bestSoFar.kind) {
        return symbol;
      }
    }
    return bestSoFar;
  }

  /**
   * @param kind subset of {@link Symbol#VAR}, {@link Symbol#TYP}, {@link Symbol#PCK}
   */
  public Symbol findIdent(Env env, String name, int kind) {
    Symbol bestSoFar = symbolNotFound;
    Symbol symbol;
    if ((kind & Symbol.VAR) != 0) {
      symbol = findVar(env, name);
      if (symbol.kind < Symbol.ERRONEOUS) {
        // symbol exists
        return symbol;
      } else if (symbol.kind < bestSoFar.kind) {
        bestSoFar = symbol;
      }
    }
    if ((kind & Symbol.TYP) != 0) {
      symbol = findType(env, name);
      if (symbol.kind < Symbol.ERRONEOUS) {
        // symbol exists
        return symbol;
      } else if (symbol.kind < bestSoFar.kind) {
        bestSoFar = symbol;
      }
    }
    if ((kind & Symbol.PCK) != 0) {
      // TODO read package
    }
    return bestSoFar;
  }

  /**
   * @param kind subset of {@link Symbol#TYP}, {@link Symbol#PCK}
   */
  public Symbol findIdentInPackage(Env env, Symbol site, String name, int kind) {
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
      symbol = findField(env, site, name, site);
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
  public Symbol findMethod(Env env, String name, List<Type> argTypes) {
    Symbol bestSoFar = symbolNotFound;
    Env env1 = env;
    while (env1.outer() != null) {
      Symbol sym = findMethod(env1, env1.enclosingClass(), name, argTypes);
      if (sym.kind < Symbol.ERRONEOUS) {
        // symbol exists
        return sym;
      } else if (sym.kind < bestSoFar.kind) {
        bestSoFar = sym;
      }
      env1 = env1.outer;
    }
    // TODO imports
    return bestSoFar;
  }

  public Symbol findMethod(Env env, Symbol.TypeSymbol site, String name, List<Type> argTypes) {
    Symbol bestSoFar = symbolNotFound;

    // TODO search in supertypes
    for (Symbol symbol : site.members().lookup(name)) {
      if (symbol.kind == Symbol.MTH) {
        bestSoFar = selectBest(env, site, argTypes, symbol, bestSoFar);
      }
    }

    // best guess: method with unique name
    // TODO remove, when search will be improved
    if (bestSoFar.kind < Symbol.ERRONEOUS) {
      return bestSoFar;
    }
    for (Symbol symbol : site.enclosingClass().members().lookup(name)) {
      if ((symbol.kind == Symbol.MTH) && isAccessible(env, site, symbol)) {
        if (bestSoFar.kind < Symbol.ERRONEOUS) {
          return new AmbiguityErrorSymbol();
        }
        bestSoFar = symbol;
      }
    }

    return bestSoFar;
  }

  /**
   * @param symbol    candidate
   * @param bestSoFar previously found best match
   */
  private Symbol selectBest(Env env, Symbol.TypeSymbol site, List<Type> argTypes, Symbol symbol, Symbol bestSoFar) {
    if (!isInheritedIn(symbol, site)) {
      return bestSoFar;
    }
    // TODO get rid of null check
    if (symbol.type == null) {
      return bestSoFar;
    }
    if (!isArgumentsAcceptable(argTypes, ((Type.MethodType) symbol.type).argTypes)) {
      return bestSoFar;
    }
    // TODO ambiguity, errors, ...
    if (!isAccessible(env, site, symbol)) {
      return new AccessErrorSymbol(symbol);
    }
    return selectMostSpecific(symbol, bestSoFar);
  }

  /**
   * @param argTypes types of arguments
   * @param formals  types of formal parameters of method
   */
  private boolean isArgumentsAcceptable(List<Type> argTypes, List<Type> formals) {
    // TODO varargs
    if (argTypes.size() != formals.size()) {
      return false;
    }
    for (int i = 0; i < argTypes.size(); i++) {
      if (!types.isSubtype(argTypes.get(i), formals.get(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * JLS7 15.12.2.5. Choosing the Most Specific Method
   */
  private Symbol selectMostSpecific(Symbol m1, Symbol m2) {
    // FIXME get rig of null check
    if (m2.type == null) {
      return m1;
    }

    boolean m1SignatureMoreSpecific = isSignatureMoreSpecific(m1, m2);
    boolean m2SignatureMoreSpecific = isSignatureMoreSpecific(m2, m1);
    if (m1SignatureMoreSpecific && m2SignatureMoreSpecific) {
      // TODO handle this case
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
    return isArgumentsAcceptable(((Type.MethodType) m1.type).argTypes, ((Type.MethodType) m2.type).argTypes);
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
        // no check of overriding, because private members cannot be overridden
        return (env.enclosingClass().outermostClass() == symbol.owner().outermostClass())
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

    public AccessErrorSymbol(Symbol symbol) {
      super(Symbol.ERRONEOUS, 0, null, null);
      this.symbol = symbol;
    }
  }

  private static Symbol.TypeSymbol superclassSymbol(Symbol.TypeSymbol c) {
    Type supertype = c.getSuperclass();
    return supertype == null ? null : supertype.symbol;
  }

}
