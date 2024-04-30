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

package org.sonar.java.checks.prettyprint;

public record FileConfig(IndentMode indentMode, String endOfLine) {

  public static final FileConfig DEFAULT_FILE_CONFIG = new FileConfig(new FileConfig.IndentMode.Spaces(2), "\n");

  public String indent() {
    return indentMode.indent();
  }

  public sealed interface IndentMode {
    char indentChar();

    int nRep();

    default String indentCharAsStr(){
      return Character.toString(indentChar());
    }

    default String indent() {
      return indentCharAsStr().repeat(nRep());
    }

    record Spaces(int nRep) implements IndentMode {
      @Override
      public char indentChar() {
        return ' ';
      }
    }

    record Tabs(int nRep) implements IndentMode {
      @Override
      public char indentChar() {
        return '\t';
      }
    }

  }

}
