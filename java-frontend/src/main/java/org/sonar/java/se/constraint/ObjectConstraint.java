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
package org.sonar.java.se.constraint;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Predicate;

public class ObjectConstraint implements Constraint {

  public static final ObjectConstraint NOT_NULL = new ObjectConstraint(false, true, null);

  private final boolean isNull;
  private final boolean disposable;
  @Nullable
  private final Object status;

  public ObjectConstraint(Object status) {
    this(false, true, status);
  }

  public ObjectConstraint(boolean isNull, boolean disposable, @Nullable Object status) {
    this.isNull = isNull;
    this.disposable = disposable;
    this.status = status;
  }

  public static ObjectConstraint nullConstraint() {
    return new ObjectConstraint(true, false, null);
  }

  public ObjectConstraint inverse() {
    return new ObjectConstraint(!isNull, disposable, status);
  }

  public ObjectConstraint withStatus(Object newStatus) {
    return new ObjectConstraint(isNull, disposable, newStatus);
  }

  @Override
  public boolean isNull() {
    return isNull;
  }

  @Override
  public String valueAsString() {
    return isNull ? "null" : String.valueOf(status);
  }

  public boolean isInvalidWith(@Nullable Constraint target) {
    return false;
  }

  public boolean isDisposable() {
    return disposable;
  }

  public boolean hasStatus(@Nullable Object aState) {
    if (aState == null) {
      return status == null;
    }
    return aState.equals(status);
  }

  @Override
  public String toString() {
    final StringBuilder buffer = new StringBuilder();
    buffer.append(isNull ? "NULL" : "NOT_NULL");
    if (status != null) {
      buffer.append('(');
      buffer.append(status);
      buffer.append(')');
    }
    return buffer.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ObjectConstraint that = (ObjectConstraint) o;
    return isNull == that.isNull && Objects.equals(status, that.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(isNull, status);
  }

  public static Predicate<Constraint> statusPredicate(Object status) {
    return c -> c instanceof ObjectConstraint && ((ObjectConstraint) c).hasStatus(status);
  }

}
