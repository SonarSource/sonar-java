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
package org.sonar.plugins.java.api.semantic;

import javax.annotation.CheckForNull;
import java.util.Collection;
import java.util.List;

/**
 * Interface to access symbol information.
 */
public interface Symbol {

  /**
   * Name of this symbol.
   * @return simple name of the symbol.
   */
  String name();

  /**
   * The owner of this symbol.
   * @return the symbol that owns this symbol.
   */
  Symbol owner();

  Type type();

  //kinds of symbols
  boolean isVariableSymbol();

  boolean isTypeSymbol();

  boolean isMethodSymbol();



  //flags method
  boolean isStatic();

  boolean isFinal();

  boolean isEnum();

  boolean isAbstract();

  boolean isPublic();

  boolean isPrivate();

  boolean isProtected();

  boolean isPackageVisibility();

  boolean isDeprecated();

  boolean isVolatile();

  TypeSymbolSemantic enclosingClass();


  interface TypeSymbolSemantic extends Symbol {

    /**
     * Returns the superclass of this type symbol.
     * @return null for java.lang.Object, the superclass for every other type.
     */
    @CheckForNull
    Type superClass();

    List<Type> interfaces();

    /**
     * List of symbols defined by this type symbols.
     * This will not return any inherited symbol.
     * @return The collection of symbols defined by this type.
     */
    Collection<Symbol> memberSymbols();

    /**
     * Lookup symbols accessible from this type with the name passed in parameter.
     * @param name name of searched symbol.
     * @return A collection of symbol matching the looked up name.
     */
    Collection<Symbol> lookupSymbols(String name);

  }

  interface VariableSymbolSemantic  extends Symbol {

  }

  interface MethodSymbolSemantic  extends Symbol {

  }


}
