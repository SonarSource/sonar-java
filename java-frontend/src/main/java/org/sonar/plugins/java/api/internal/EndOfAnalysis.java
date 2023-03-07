/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.plugins.java.api.internal;

import org.sonar.java.annotations.Beta;
import org.sonar.plugins.java.api.ModuleScannerContext;

/**
 * Common interface for providing callbacks that are triggered at the end of a module's analysis, after all files have been scanned.
 * <b>Warning: keeping state between files can lead to memory leaks. Implement with care.</b>
 */
@Beta
public interface EndOfAnalysis {

  /**
   * A method called when all files in the module have been processed.
   * @param context ModuleScannerContext that can be used to get module information or report issues at project level.
   */
  void endOfAnalysis(ModuleScannerContext context);
}
