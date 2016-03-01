/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.collect.Maps;

import org.sonar.java.resolve.JavaType.WildCardType.BoundType;

import java.util.EnumMap;
import java.util.Map;

public class ParametrizedTypeCache {

  private Map<JavaSymbol, Map<TypeSubstitution, JavaType.ParametrizedTypeJavaType>> typeCache = Maps.newHashMap();
  private Map<JavaType, Map<JavaType.WildCardType.BoundType, JavaType.WildCardType>> wildcardCache = Maps.newHashMap();

  public JavaType getParametrizedTypeType(JavaSymbol.TypeJavaSymbol symbol, TypeSubstitution typeSubstitution) {
    if (symbol.getType().isUnknown()) {
      return symbol.getType();
    }
    if (typeCache.get(symbol) == null) {
      Map<TypeSubstitution, JavaType.ParametrizedTypeJavaType> map = Maps.newHashMap();
      typeCache.put(symbol, map);
    }
    if (typeCache.get(symbol).get(typeSubstitution) == null) {
      typeCache.get(symbol).put(typeSubstitution, new JavaType.ParametrizedTypeJavaType(symbol, typeSubstitution));
    }
    return typeCache.get(symbol).get(typeSubstitution);
  }

  public JavaType getWildcardType(JavaType bound, BoundType boundType) {
    Map<JavaType.WildCardType.BoundType, JavaType.WildCardType> map = wildcardCache.get(bound);
    if (map == null) {
      map = new EnumMap<>(JavaType.WildCardType.BoundType.class);
      wildcardCache.put(bound, map);
    }
    JavaType.WildCardType wildcardType = map.get(boundType);
    if (wildcardType == null) {
      wildcardType = new JavaType.WildCardType(bound, boundType);
      map.put(boundType, wildcardType);
    }
    return wildcardType;
  }

}
