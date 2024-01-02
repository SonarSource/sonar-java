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
 * Common interface shared between all possible pattern as introduced with Java 17 and JEP-406.
 * Currently.
 * <li>
 *   <ul>{@link TypePatternTree}</ul>
 *   <ul>{@link GuardedPatternTree}</ul>
 *   <ul>{@link NullPatternTree}</ul>
 *   <ul>{@link DefaultPatternTree}</ul>
 *   <ul>{@link RecordPatternTree}</ul>
 * </li>
 *
 * @since Java 17
 * @deprecated Preview Feature
 */
@Beta
@Deprecated(since = "7.7", forRemoval = false)
public interface PatternTree extends ExpressionTree {

}
