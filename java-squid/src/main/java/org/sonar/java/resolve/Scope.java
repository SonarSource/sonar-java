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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.List;

/**
 * Represents an area of visibility.
 */
public class Scope {

  final JavaSymbol owner;
  final Scope next;

  protected ArrayListMultimap<String, JavaSymbol> symbols = ArrayListMultimap.create();

  public Scope(JavaSymbol owner) {
    this.owner = owner;
    this.next = null;
  }

  public Scope(Scope next) {
    this.owner = next.owner;
    this.next = next;
  }

  public void enter(JavaSymbol symbol) {
    symbols.put(symbol.name, symbol);
  }

  public List<JavaSymbol> lookup(String name) {
    Scope scope = this;
    while (scope != null && !scope.symbols.containsKey(name)) {
      scope = scope.next;
    }
    return scope == null ? ImmutableList.<JavaSymbol>of() : scope.symbols.get(name);
  }

  public Collection<JavaSymbol> scopeSymbols() {
    return ImmutableList.copyOf(symbols.values());
  }

  public static class OrderedScope extends Scope {
    private List<JavaSymbol> orderedSymbols;

    public OrderedScope(JavaSymbol owner) {
      super(owner);
      this.orderedSymbols = Lists.newArrayList();
    }

    @Override
    public void enter(JavaSymbol symbol) {
      this.orderedSymbols.add(symbol);
      super.enter(symbol);
    }

    /**
     * returns the symbols contained in this scope, in declaration order.
     *
     * @return list of symbols, in declaration order
     */
    @Override
    public List<JavaSymbol> scopeSymbols() {
      return ImmutableList.copyOf(orderedSymbols);
    }
  }

  public static class StarImportScope extends Scope {

    private final BytecodeCompleter bytecodeCompleter;

    public StarImportScope(JavaSymbol owner, BytecodeCompleter bytecodeCompleter) {
      super(owner);
      this.bytecodeCompleter = bytecodeCompleter;
    }

    @Override
    public List<JavaSymbol> lookup(String name) {
      List<JavaSymbol> symbolsList = Lists.newArrayList();
      for (JavaSymbol site : symbols.values()) {
        JavaSymbol symbol = bytecodeCompleter.loadClass(bytecodeCompleter.formFullName(name, site));
        if (symbol.kind < JavaSymbol.ERRONEOUS) {
          symbolsList.add(symbol);
        }
      }
      return symbolsList;
    }
  }


  public static class StaticStarImportScope extends Scope {

    private final BytecodeCompleter bytecodeCompleter;

    public StaticStarImportScope(JavaSymbol owner, BytecodeCompleter bytecodeCompleter) {
      super(owner);
      this.bytecodeCompleter = bytecodeCompleter;
    }

    @Override
    public List<JavaSymbol> lookup(String name) {
      List<JavaSymbol> symbolsList = Lists.newArrayList();
      for (JavaSymbol site : symbols.values()) {
        //site is a package, try to load referenced type.
        if ((site.kind & JavaSymbol.PCK) != 0) {
          JavaSymbol symbol = bytecodeCompleter.loadClass(bytecodeCompleter.formFullName(name, site));
          if (symbol.kind < JavaSymbol.ERRONEOUS) {
            symbolsList.add(symbol);
          }
        }

        //site is a type, try to find a matching type or field
        if ((site.kind & JavaSymbol.TYP) != 0) {
          List<JavaSymbol> resolved = ((JavaSymbol.TypeJavaSymbol) site).members().lookup(name);
          for (JavaSymbol symbol : resolved) {
            //TODO check accessibility
            //TODO factorize with static named import ?
            if (symbol.kind < JavaSymbol.ERRONEOUS && (symbol.flags & Flags.STATIC) != 0) {
              symbolsList.add(symbol);
            }
          }
        }

      }
      return symbolsList;
    }
  }

}
