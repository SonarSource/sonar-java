/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.se;

import java.util.Arrays;
import org.sonar.java.se.checks.SECheck;
import org.sonar.plugins.java.api.JavaCheck;

public enum SymbolicExecutionMode {
  DISABLED,
  ENABLED_WITHOUT_X_FILE,
  ENABLED;

  public static SymbolicExecutionMode getMode(JavaCheck[] visitors, boolean xFileEnabled) {
    if (hasASymbolicExecutionCheck(visitors)) {
      return xFileEnabled ? SymbolicExecutionMode.ENABLED : SymbolicExecutionMode.ENABLED_WITHOUT_X_FILE;
    }
    return SymbolicExecutionMode.DISABLED;
  }

  private static boolean hasASymbolicExecutionCheck(JavaCheck[] visitors) {
    return Arrays.stream(visitors).anyMatch(v -> v instanceof SECheck);
  }

  public boolean isEnabled() {
    return this != DISABLED;
  }

  public boolean isCrossFileEnabled() {
    return this == ENABLED;
  }
}
