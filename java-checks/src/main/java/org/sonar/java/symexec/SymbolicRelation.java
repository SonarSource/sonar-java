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
package org.sonar.java.symexec;

import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nullable;

import java.util.Map;

enum SymbolicRelation {

  EQUAL_TO,
  GREATER_EQUAL,
  GREATER_THAN,
  LESS_EQUAL,
  LESS_THAN,
  NOT_EQUAL,
  UNKNOWN;

  private static final int FLAGS_EQUAL = 1 << 0;
  private static final int FLAGS_GREATER = 1 << 1;
  private static final int FLAGS_LESS = 1 << 2;

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

  private static final Map<SymbolicRelation, Integer> CONSTANT_FLAGS_MAP = ImmutableMap.<SymbolicRelation, Integer>builder()
    .put(EQUAL_TO, FLAGS_EQUAL)
    .put(GREATER_EQUAL, FLAGS_EQUAL | FLAGS_GREATER)
    .put(GREATER_THAN, FLAGS_GREATER)
    .put(LESS_EQUAL, FLAGS_EQUAL | FLAGS_LESS)
    .put(LESS_THAN, FLAGS_LESS)
    .put(NOT_EQUAL, FLAGS_GREATER | FLAGS_LESS)
    .put(UNKNOWN, FLAGS_EQUAL | FLAGS_GREATER | FLAGS_LESS)
    .build();

  private static final Map<Integer, SymbolicRelation> FLAGS_CONSTANT_MAP = HashBiMap.create(CONSTANT_FLAGS_MAP).inverse();

  public SymbolicRelation negate() {
    return NEGATE_MAP.get(this);
  }

  public SymbolicRelation swap() {
    return SWAP_MAP.get(this);
  }

  public SymbolicRelation union(@Nullable SymbolicRelation other) {
    return other == null ? this : FLAGS_CONSTANT_MAP.get(CONSTANT_FLAGS_MAP.get(this) | CONSTANT_FLAGS_MAP.get(other));
  }

}
