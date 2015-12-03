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
package org.sonar.java.se;

import org.sonar.plugins.java.api.tree.Tree;

public class ObjectConstraint {

  public static final ObjectConstraint NULL = new ObjectConstraint(true, null, null);
  public static final ObjectConstraint NOT_NULL = NULL.inverse();

  private final boolean isNull;
  private final Tree syntaxNode;
  private final Object state;

  public ObjectConstraint(Tree syntaxNode, Object state) {
    this(false, syntaxNode, state);
  }

  private ObjectConstraint(boolean isNull, Tree syntaxNode, Object state) {
    this.isNull = isNull;
    this.syntaxNode = syntaxNode;
    this.state = state;
  }

  public ObjectConstraint inverse() {
    if (isNull) {
      return new ObjectConstraint(!isNull, syntaxNode, state);
    }
    return ObjectConstraint.NULL;
  }

  public ObjectConstraint inState(Object newState) {
    return new ObjectConstraint(isNull, syntaxNode, newState);
  }

  public boolean isNull() {
    return isNull;
  }

  public boolean hasState(Object aState) {
    if (aState == null) {
      return state == null;
    }
    return aState.equals(state);
  }

  public Tree syntaxNode() {
    return syntaxNode;
  }

  @Override
  public String toString() {
    final StringBuilder buffer = new StringBuilder();
    buffer.append(isNull ? "NULL" : "NOT_NULL");
    if (state != null) {
      buffer.append('(');
      buffer.append(state);
      buffer.append(')');
    }
    return buffer.toString();
  }
}
