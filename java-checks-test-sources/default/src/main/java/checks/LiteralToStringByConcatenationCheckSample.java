/*
 * SonarQube Java
 * Copyright (C) 2012-2026 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package checks;

class LiteralToStringByConcatenationCheckSample {

  private static final String EMPTY = "";

  String convertLiterals(int value) {
    String itemCount = "" + 123; // Noncompliant {{Use an explicit conversion instead of concatenating this literal with an empty string.}}
//                     ^^^^^^^^
    String enabled = "" + true; // Noncompliant {{Use an explicit conversion instead of concatenating this literal with an empty string.}}
//                   ^^^^^^^^^
    String ratio = "" + 3.14f; // Noncompliant {{Use an explicit conversion instead of concatenating this literal with an empty string.}}
//                 ^^^^^^^^^^
    String initial = "" + 'A'; // Noncompliant {{Use an explicit conversion instead of concatenating this literal with an empty string.}}
//                   ^^^^^^^^^
    String timeout = 42L + ""; // Noncompliant {{Use an explicit conversion instead of concatenating this literal with an empty string.}}
//                   ^^^^^^^^
    String rate = ("") + (2.5d); // Noncompliant {{Use an explicit conversion instead of concatenating this literal with an empty string.}}
//                ^^^^^^^^^^^^^^
    return itemCount + enabled + ratio + initial + timeout + rate;
  }

  String explicitOrOrdinaryConcatenation(int value) {
    String explicitInteger = Integer.toString(123);
    String explicitBoolean = Boolean.toString(true);
    String explicitGeneric = String.valueOf(3.14f);
    String prefixed = "items=" + 123;
    String variable = "" + value;
    String absent = "" + null;
    String text = "" + "ready";
    String constant = EMPTY + 123;
    String computed = "" + (1 + 2);
    return explicitInteger + explicitBoolean + explicitGeneric + prefixed + variable + absent + text + constant + computed;
  }
}
