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

public abstract class AsmClassProvider {

  public enum DETAIL_LEVEL {
    /**
     * Nothing is loaded from the bytecode
     */
    NOTHING(1),
    /**
     * Superclass and interfaces are loaded along with fields and methods but not types used by fields or methods
     */
    STRUCTURE(2),
    /**
     * Calls to other methods are loaded
     */
    STRUCTURE_AND_CALLS(3);

    private int internalLevel;

    private DETAIL_LEVEL(int level) {
      this.internalLevel = level;
    }

    boolean isGreaterThan(DETAIL_LEVEL level) {
      return this.internalLevel > level.internalLevel;
    }
  }

  public abstract AsmClass getClass(String internalName, DETAIL_LEVEL level);

  public final AsmClass getClass(String internalName) {
    return getClass(internalName, DETAIL_LEVEL.STRUCTURE_AND_CALLS);
  }

}
