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

  SymbolicValue NULL_LITERAL = new ObjectSymbolicValue(0);
  SymbolicValue TRUE_LITERAL = new ObjectSymbolicValue(1);
  SymbolicValue FALSE_LITERAL = new ObjectSymbolicValue(2);


  class ObjectSymbolicValue implements SymbolicValue {

    private final int id;

    public ObjectSymbolicValue(int id) {
      this.id = id;
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
      return Objects.equal(id, that.id);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(id);
    }

    @Override
    public String toString() {
      return "SV#" + id;
    }
  }




}
