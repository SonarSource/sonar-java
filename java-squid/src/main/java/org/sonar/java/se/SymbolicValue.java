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

import com.google.common.base.Objects;

public interface SymbolicValue {

  boolean isNull();

  enum NullSymbolicValue {
    NULL,
    NOT_NULL,
    UNKNOWN
  }

  class ObjectSymbolicValue implements SymbolicValue {
    private final NullSymbolicValue value;

    public ObjectSymbolicValue(NullSymbolicValue value) {
      this.value = value;
    }

    public boolean isNull() {
      return value == NullSymbolicValue.NULL;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ObjectSymbolicValue that = (ObjectSymbolicValue) o;
      return Objects.equal(value, that.value);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(value);
    }

    @Override
    public String toString() {
      return "SV#" + value.name();
    }
  }




}
