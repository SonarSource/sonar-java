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
package org.sonar.java.se.constraint;

import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonar.java.collections.PCollections;
import org.sonar.java.collections.PMap;

public class ConstraintsByDomain {

  private final PMap<Class<? extends Constraint>, Constraint> constraintPMap;

  private static final ConstraintsByDomain EMPTY = new ConstraintsByDomain(PCollections.emptyMap());

  private ConstraintsByDomain(PMap<Class<? extends Constraint>, Constraint> constraintPMap) {
    this.constraintPMap = constraintPMap;
  }

  public static ConstraintsByDomain empty() {
    return EMPTY;
  }

  public ConstraintsByDomain remove(Class<? extends Constraint> domain) {
    PMap<Class<? extends Constraint>, Constraint> remove = constraintPMap.remove(domain);
    if (remove == constraintPMap) {
      return this;
    }
    return remove.isEmpty() ? EMPTY : new ConstraintsByDomain(remove);
  }

  @Nullable
  public Constraint get(Class<? extends Constraint> domain) {
    return constraintPMap.get(domain);
  }

  public boolean hasConstraint(Constraint constraint) {
    return constraint.equals(constraintPMap.get(constraint.getClass()));
  }

  public void forEach(BiConsumer<Class<? extends Constraint>, Constraint> action) {
    constraintPMap.forEach(action);
  }

  public boolean isEmpty() {
    return constraintPMap.isEmpty();
  }

  public ConstraintsByDomain put(Constraint constraint) {
    PMap<Class<? extends Constraint>, Constraint> newConstraints = constraintPMap.put(constraint.getClass(), constraint);
    return newConstraints == constraintPMap ? this : new ConstraintsByDomain(newConstraints);
  }

  public Stream<Constraint> stream() {
    Stream.Builder<Constraint> builder = Stream.builder();
    constraintPMap.forEach((d, constraint) -> builder.add(constraint));
    return builder.build();
  }

  public Stream<Class<? extends Constraint>> domains() {
    Stream.Builder<Class<? extends Constraint>> builder = Stream.builder();
    constraintPMap.forEach((domain, constraint) -> builder.add(domain));
    return builder.build();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConstraintsByDomain that = (ConstraintsByDomain) o;
    return constraintPMap.equals(that.constraintPMap);
  }

  @Override
  public String toString() {
    return stream().map(Constraint::toString).sorted().collect(Collectors.joining(",", "[", "]"));
  }

  @Override
  public int hashCode() {
    return constraintPMap.hashCode();
  }
}
