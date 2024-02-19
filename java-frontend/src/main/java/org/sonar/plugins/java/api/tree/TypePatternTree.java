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
package org.sonar.plugins.java.api.tree;

import org.sonar.java.annotations.Beta;

/**
 * Typed Pattern tree.
 * Introduced with Java 17 and JEP-406.
 * Finalized with Java 21 and JEP-441.
 *
 * <pre>
 *   switch(o) {
 *     case {@link #patternVariable()} : ...
 *     case {@link #patternVariable()} -> ...
 *   }
 * </pre>
 *
 * @since Java 17
 */
@Beta
public interface TypePatternTree extends PatternTree {

  VariableTree patternVariable();

}
