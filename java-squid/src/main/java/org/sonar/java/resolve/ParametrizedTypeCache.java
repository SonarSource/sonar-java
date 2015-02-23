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

import com.google.common.collect.Maps;

import java.util.Map;

public class ParametrizedTypeCache {

  private Map<Symbol, Map<Map<Type.TypeVariableType, Type>, Type.ParametrizedTypeType>> typeCache = Maps.newHashMap();

  public Type getParametrizedTypeType(Symbol.TypeSymbol symbol, Map<Type.TypeVariableType, Type> typeSubstitution) {
    if (symbol.getType().isTagged(Type.UNKNOWN)) {
      return symbol.getType();
    }
    if (typeCache.get(symbol) == null) {
      Map<Map<Type.TypeVariableType, Type>, Type.ParametrizedTypeType> map = Maps.newHashMap();
      typeCache.put(symbol, map);
    }
    if (typeCache.get(symbol).get(typeSubstitution) == null) {
      typeCache.get(symbol).put(typeSubstitution, new Type.ParametrizedTypeType(symbol, typeSubstitution));
    }
    return typeCache.get(symbol).get(typeSubstitution);
  }

}
