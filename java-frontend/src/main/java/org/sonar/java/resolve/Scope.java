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
import com.google.common.collect.ArrayListMultimap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents an area of visibility.
 */
public class Scope {

  final JavaSymbol owner;
  final Scope next;

  protected ArrayListMultimap<String, JavaSymbol> symbols = ArrayListMultimap.create();
  protected final List<JavaSymbol> scopeSymbols = new ArrayList<>();

  public Scope(JavaSymbol owner) {
    this.owner = owner;
    this.next = null;
  }

  public Scope(Scope next) {
    this.owner = next.owner;
    this.next = next;
  }

  public void enter(JavaSymbol symbol) {
    if(!symbol.isMethodSymbol() && symbols.containsKey(symbol.name)) {
      for (JavaSymbol symInScope : symbols.get(symbol.name)) {
        Preconditions.checkState(symInScope.kind != symbol.kind, "Registering symbol: '%s' twice in the same scope", symbol.name);
      }
    }
    symbols.put(symbol.name, symbol);
    scopeSymbols.add(symbol);
  }

  public List<JavaSymbol> lookup(String name) {
    Scope scope = this;
    while (scope != null && !scope.symbols.containsKey(name)) {
      scope = scope.next;
    }
    return scope == null ? Collections.emptyList() : scope.symbols.get(name);
  }

  public List<JavaSymbol> scopeSymbols() {
    return scopeSymbols;
  }

  public static class ImportScope extends Scope {
    public ImportScope(JavaSymbol owner) {
      super(owner);
    }

    @Override
    public void enter(JavaSymbol symbol) {
      symbols.put(symbol.name, symbol);
      scopeSymbols.add(symbol);
    }
  }
  public static class StarImportScope extends ImportScope {

    private final BytecodeCompleter bytecodeCompleter;

    public StarImportScope(JavaSymbol owner, BytecodeCompleter bytecodeCompleter) {
      super(owner);
      this.bytecodeCompleter = bytecodeCompleter;
    }

    @Override
    public List<JavaSymbol> lookup(String name) {
      List<JavaSymbol> symbolsList = new ArrayList<>();
      for (JavaSymbol site : symbols.values()) {
        JavaSymbol symbol = bytecodeCompleter.loadClass(bytecodeCompleter.formFullName(name, site));
        if (symbol.kind < JavaSymbol.ERRONEOUS) {
          symbolsList.add(symbol);
        }
      }
      return symbolsList;
    }
  }

  public static class StaticStarImportScope extends ImportScope {

    private final BytecodeCompleter bytecodeCompleter;

    public StaticStarImportScope(JavaSymbol owner, BytecodeCompleter bytecodeCompleter) {
      super(owner);
      this.bytecodeCompleter = bytecodeCompleter;
    }

    @Override
    public List<JavaSymbol> lookup(String name) {
      List<JavaSymbol> symbolsList = new ArrayList<>();
      for (JavaSymbol site : symbols.values()) {
        // site is a package, try to load referenced type.
        if ((site.kind & JavaSymbol.PCK) != 0) {
          JavaSymbol symbol = bytecodeCompleter.loadClass(bytecodeCompleter.formFullName(name, site));
          if (symbol.kind < JavaSymbol.ERRONEOUS) {
            symbolsList.add(symbol);
          }
        }

        // site is a type, try to find a matching type or field
        if ((site.kind & JavaSymbol.TYP) != 0 && site.kind < JavaSymbol.ERRONEOUS) {
          List<JavaSymbol> resolved = ((JavaSymbol.TypeJavaSymbol) site).members().lookup(name);
          resolved.stream()
            // TODO check accessibility
            // TODO factorize with static named import ?
            .filter(symbol -> symbol.kind < JavaSymbol.ERRONEOUS && Flags.isFlagged(symbol.flags, Flags.STATIC))
            .forEach(symbolsList::add);
        }

      }
      return symbolsList;
    }
  }

}
