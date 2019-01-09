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
package org.sonar.plugins.java.api.semantic;

import java.util.Collection;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LabeledStatementTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

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

  /**
   * Type of symbol.
   * @return the type of this symbol.
   */
  Type type();

  // kinds of symbols
  boolean isVariableSymbol();

  boolean isTypeSymbol();

  boolean isMethodSymbol();

  boolean isPackageSymbol();

  // flags method
  boolean isStatic();

  boolean isFinal();

  boolean isEnum();

  boolean isInterface();

  boolean isAbstract();

  boolean isPublic();

  boolean isPrivate();

  boolean isProtected();

  boolean isPackageVisibility();

  boolean isDeprecated();

  boolean isVolatile();

  boolean isUnknown();

  /**
   * Symbol metadata informations, annotations for instance.
   * @return the metadata of this symbol.
   */
  SymbolMetadata metadata();

  /**
   * The closest enclosing class.
   * @return null for package symbols, themselves for type symbol and enclosing class of methods or variables.
   */
  @Nullable
  TypeSymbol enclosingClass();

  /**
   * The identifier trees that reference this symbol.
   * @return a list of IdentifierTree referencing this symbol. An empty list if this symbol is unused.
   */
  List<IdentifierTree> usages();

  /**
   * Declaration node of this symbol. Currently, only works for declaration within the same file.
   * @return the Tree of the declaration of this symbol. Null if declaration does not occur in the currently analyzed file.
   */
  @Nullable
  Tree declaration();

  /**
   * Symbol for a type : class, enum, interface or annotation.
   */
  interface TypeSymbol extends Symbol {

    /**
     * Returns the superclass of this type symbol.
     * @return null for java.lang.Object, the superclass for every other type.
     */
    @CheckForNull
    Type superClass();

    /**
     * Interfaces implemented by this type.
     * @return an empty list if this type does not implement any interface.
     */
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

    @Nullable
    @Override
    ClassTree declaration();

  }

  /**
   * Symbol for field, method parameters and local variables.
   */
  interface VariableSymbol extends Symbol {

    @Nullable
    @Override
    VariableTree declaration();

  }

  /**
   * Symbol for methods.
   */
  interface MethodSymbol extends Symbol {

    /**
     * Type of parameters declared by this method.
     * @return empty list if method has a zero arity.
     */
    List<Type> parameterTypes();

    TypeSymbol returnType();

    /**
     * List of the exceptions that can be thrown by the method.
     * @return empty list if no exception are declared in the throw clause of the method.
     */
    List<Type> thrownTypes();

    /**
     * Retrieve the overridden symbol, which may may not be able to determine (returning 'unknown' symbol).
     * Note that if the method returns null, the method is not overriding any method for sure.
     *
     * @return the overridden symbol, unknown if overriding can not be determined (incomplete semantic), or null if the method is not overriding any method
     */
    @Nullable
    Symbol.MethodSymbol overriddenSymbol();

    @Nullable
    @Override
    MethodTree declaration();

    /**
     * Compute the signature as identified from bytecode point of view. Will be unique for each method.
     * @return the signature of the method, as String
     */
    String signature();
  }

  /**
   * Label symbol. Note: this is not a Symbol per say.
   */
  interface LabelSymbol {

    /**
     * Name of that label.
     */
    String name();

    /**
     * Usages tree of this label.
     */
    List<IdentifierTree> usages();

    /**
     * Declaration tree of this label.
     */
    LabeledStatementTree declaration();

  }

}
