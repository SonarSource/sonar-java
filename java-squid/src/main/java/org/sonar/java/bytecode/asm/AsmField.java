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
package org.sonar.java.bytecode.asm;

public class AsmField extends AsmResource {

  private final String name;

  public AsmField(AsmClass parent, String name) {
    this.name = name;
    this.parent = parent;
  }

  public String getName() {
    return name;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object instanceof AsmField) {
      AsmField otherField = (AsmField) object;
      return parent.equals(otherField.parent) && name.equals(otherField.name);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return parent.hashCode() + name.hashCode();
  }

  @Override
  public String toString() {
    return name;
  }
}
