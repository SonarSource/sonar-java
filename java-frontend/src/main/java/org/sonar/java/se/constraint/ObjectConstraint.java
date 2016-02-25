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

import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;

public class ObjectConstraint implements Constraint {

  public static final ObjectConstraint NOT_NULL = new ObjectConstraint(false, true, null, null);

  private final boolean isNull;
  private final boolean disposable;
  private final Tree syntaxNode;
  @Nullable
  private final Object status;

  public ObjectConstraint(Tree syntaxNode, Object status) {
    this(false, true, syntaxNode, status);
  }

  public ObjectConstraint(boolean isNull, boolean disposable, @Nullable Tree syntaxNode, @Nullable Object status) {
    this.isNull = isNull;
    this.disposable = disposable;
    this.syntaxNode = syntaxNode;
    this.status = status;
  }

  public static ObjectConstraint nullConstraint() {
    return nullConstraint(null);
  }

  public static ObjectConstraint nullConstraint(@Nullable Tree syntaxNode) {
    return new ObjectConstraint(true, false, syntaxNode, null);
  }

  public ObjectConstraint inverse() {
    return new ObjectConstraint(!isNull, disposable, syntaxNode, status);
  }

  public ObjectConstraint withStatus(Object newStatus) {
    return new ObjectConstraint(isNull, disposable, syntaxNode, newStatus);
  }

  @Override
  public boolean isNull() {
    return isNull;
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

  public Tree syntaxNode() {
    return syntaxNode;
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
}
