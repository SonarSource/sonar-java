/*
 * Sonar Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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

public class Flags {

  private Flags() {
  }

  public static final int PUBLIC = 1 << 0;
  public static final int PRIVATE = 1 << 1;
  public static final int PROTECTED = 1 << 2;

  /**
   * Interface or AnnotationType.
   */
  public static final int INTERFACE = 1 << 9;

  /**
   * Masks.
   */
  public static final int ACCESS_FLAGS = PUBLIC | PROTECTED | PRIVATE;

}
