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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

/**
 * Represents an area of visibility.
 */
public class Scope {

  final Symbol owner;
  final Scope next;

  private ArrayListMultimap<String, Symbol> symbols = ArrayListMultimap.create();

  public Scope(Symbol owner) {
    this.owner = owner;
    this.next = null;
  }

  public Scope(Scope next) {
    this.owner = next.owner;
    this.next = next;
  }

  public void enter(Symbol symbol) {
    symbols.put(symbol.name, symbol);
  }

  public List<Symbol> lookup(String name) {
    Scope scope = this;
    while (scope != null && !scope.symbols.containsKey(name)) {
      scope = scope.next;
    }
    return scope == null ? ImmutableList.<Symbol>of() : scope.symbols.get(name);
  }

}
