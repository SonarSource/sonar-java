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

@Beta
public enum Modifier {

  PUBLIC,
  PROTECTED,
  PRIVATE,
  ABSTRACT,
  STATIC,
  FINAL,
  TRANSIENT,
  VOLATILE,
  SYNCHRONIZED,
  NATIVE,
  DEFAULT,
  STRICTFP,
  /**
   * @since Java 15 (preview), Java 17 (final)
   */
  SEALED,
  /**
   * @since Java 15 (preview), Java 17 (final)
   */
  NON_SEALED,
  // since java 9, only used by Requires Module Directive (JLS9 - §7.7)
  TRANSITIVE;

}
