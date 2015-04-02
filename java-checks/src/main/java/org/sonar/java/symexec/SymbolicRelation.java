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
package org.sonar.java.symexec;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

enum SymbolicRelation {

  EQUAL_TO,
  GREATER_EQUAL,
  GREATER_THAN,
  LESS_EQUAL,
  LESS_THAN,
  NOT_EQUAL,
  UNKNOWN;

  private static final Map<SymbolicRelation, SymbolicRelation> NEGATE_MAP = ImmutableMap.<SymbolicRelation, SymbolicRelation>builder()
    .put(EQUAL_TO, NOT_EQUAL)
    .put(GREATER_EQUAL, LESS_THAN)
    .put(GREATER_THAN, LESS_EQUAL)
    .put(LESS_EQUAL, GREATER_THAN)
    .put(LESS_THAN, GREATER_EQUAL)
    .put(NOT_EQUAL, EQUAL_TO)
    .put(UNKNOWN, UNKNOWN)
    .build();

  private static final Map<SymbolicRelation, SymbolicRelation> SWAP_MAP = ImmutableMap.<SymbolicRelation, SymbolicRelation>builder()
    .put(EQUAL_TO, EQUAL_TO)
    .put(GREATER_EQUAL, LESS_EQUAL)
    .put(GREATER_THAN, LESS_THAN)
    .put(LESS_EQUAL, GREATER_EQUAL)
    .put(LESS_THAN, GREATER_THAN)
    .put(NOT_EQUAL, NOT_EQUAL)
    .put(UNKNOWN, UNKNOWN)
    .build();

  public SymbolicRelation negate() {
    return NEGATE_MAP.get(this);
  }

  public SymbolicRelation swap() {
    return SWAP_MAP.get(this);
  }

}
