/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package jspecify.nullunmarkedonly;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.NullUnmarked;

// NullMarked at the module level
class ChangeMethodContractCheck {

  @interface MyAnnotation {}

  @NullUnmarked
  String annotatedUnmarked(Object a) { return null; }
}

class ChangeMethodContractCheck_B extends ChangeMethodContractCheck {

  @NullMarked
  @Override
  String annotatedUnmarked(Object a) { return null; } // Noncompliant {{Fix the incompatibility of the annotation @NullMarked to honor @NullUnmarked of the overridden method.}}

}

