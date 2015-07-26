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
package org.sonar.java.resolve;

import org.sonar.plugins.java.api.tree.Modifier;

import java.util.EnumSet;

public class Flags {

  private Flags() {
  }

  public static final int PUBLIC = 1 << 0;
  public static final int PRIVATE = 1 << 1;
  public static final int PROTECTED = 1 << 2;

  public static final int STATIC = 1 << 3;
  public static final int FINAL = 1 << 4;
  public static final int SYNCHRONIZED = 1 << 5;
  public static final int VOLATILE = 1 << 6;
  public static final int TRANSIENT = 1 << 7;
  /**
   * Same value as for TRANSIENT as transient for method has no sense as well as vararg for a field.
   */
  public static final int VARARGS = 1 << 7;
  public static final int NATIVE = 1 << 8;

  /**
   * Interface or annotation type.
   */
  public static final int INTERFACE = 1 << 9;

  public static final int ABSTRACT = 1 << 10;

  public static final int STRICTFP = 1 << 11;

  public static final int SYNTHETIC = 1 << 12;

  /**
   * Annotation type.
   */
  public static final int ANNOTATION = 1 << 13;

  /**
   * An enumeration type or an enumeration constant.
   */
  public static final int ENUM = 1 << 14;

  /**
   * Flag that marks either a default method or an interface containing default methods.
   * Warning : This value is not compliant with openJDK (default is 1L<<43 and 1<<15 is MANDATE)
   */
  public static final int DEFAULT = 1 << 15;

  public static final int DEPRECATED = 1 << 17;

  /**
   * Masks.
   */
  public static final int ACCESS_FLAGS = PUBLIC | PROTECTED | PRIVATE;

  public static EnumSet<Flag> asFlagSet(int flags) {
    EnumSet<Flag> result = EnumSet.noneOf(Flag.class);
    int mask = 1;
    for (int i = 0; i < 15; i++) {
      if ((flags & mask) != 0) {
        result.add(Flag.values()[i]);
      }
      mask = mask << 1;
    }
    return result;
  }

  enum Flag {
    PUBLIC,
    PRIVATE,
    PROTECTED,
    STATIC,
    FINAL,
    SYNCHRONIZED,
    VOLATILE,
    TRANSIENT,
    NATIVE,
    INTERFACE,
    ABSTRACT,
    STRICTFP,
    SYNTHETIC,
    ANNOTATION,
    ENUM;

    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }


  public static int flagForModifier(Modifier modifier) {
    int result;
    switch (modifier) {
      case PUBLIC:
        result = Flags.PUBLIC;
        break;
      case PRIVATE:
        result = Flags.PRIVATE;
        break;
      case PROTECTED:
        result = Flags.PROTECTED;
        break;
      case ABSTRACT:
        result = Flags.ABSTRACT;
        break;
      case STATIC:
        result = Flags.STATIC;
        break;
      case FINAL:
        result = Flags.FINAL;
        break;
      case TRANSIENT:
        result = Flags.TRANSIENT;
        break;
      case VOLATILE:
        result = Flags.VOLATILE;
        break;
      case SYNCHRONIZED:
        result = Flags.SYNCHRONIZED;
        break;
      case NATIVE:
        result = Flags.NATIVE;
        break;
      case DEFAULT:
        result = Flags.DEFAULT;
        break;
      case STRICTFP:
        result = Flags.STRICTFP;
        break;
      default:
        result = 0;
    }
    return result;
  }

}
