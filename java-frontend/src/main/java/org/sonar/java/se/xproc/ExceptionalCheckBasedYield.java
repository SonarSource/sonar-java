/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java.se.xproc;

import com.google.common.base.Preconditions;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.sonar.java.se.ExplodedGraph.Node;
import org.sonar.java.se.checks.SECheck;
import org.sonar.plugins.java.api.semantic.Type;

import java.util.Arrays;

public class ExceptionalCheckBasedYield extends ExceptionalYield {

  private final Class<? extends SECheck> check;

  public ExceptionalCheckBasedYield(Type exceptionType, Class<? extends SECheck> check, Node node, MethodBehavior behavior) {
    super(node, behavior);
    this.check = check;
    Preconditions.checkArgument(exceptionType != null, "exceptionType is required");
    super.setExceptionType(exceptionType);
  }

  @Override
  public void setExceptionType(Type exceptionType) {
    throw new UnsupportedOperationException("Exception type can not be changed");
  }

  public Class<? extends SECheck> check() {
    return check;
  }

  @Override
  public String toString() {
    Type exceptionType = exceptionType();
    Preconditions.checkState(exceptionType != null);
    return String.format("{params: %s, exceptional (%s), check: %s}", Arrays.toString(parametersConstraints()), exceptionType.fullyQualifiedName(), check.getSimpleName());
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(11, 1297)
      .appendSuper(super.hashCode())
      .append(check)
      .hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    ExceptionalCheckBasedYield other = (ExceptionalCheckBasedYield) obj;
    return new EqualsBuilder()
      .appendSuper(super.equals(obj))
      .append(check, other.check)
      .isEquals();
  }
}
