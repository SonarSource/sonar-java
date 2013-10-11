/*
 * SonarQube Java
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
package org.sonar.java.model;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Class, enum, interface or annotation declaration.
 *
 * JLS 8.1, 8.9, 9.1, 9.6
 *
 * <pre>
 *   {@link #modifiers()} class {@link #simpleName()} {@link #typeParameters()} extends {@link #superClass()} implements {@link #superInterfaces()} {
 *     {@link #members()}
 *   }
 *
 *   {@link #modifiers()} interface {@link #simpleName()} {@link #typeParameters()} extends {@link #superInterfaces()} {
 *     {@link #members()}
 *   }
 * </pre>
 *
 * @since Java 1.3
 */
public interface ClassTree extends StatementTree {

  @Nullable
  String simpleName();

  List<? extends Tree> typeParameters();

  ModifiersTree modifiers();

  @Nullable
  Tree superClass();

  List<? extends Tree> superInterfaces();

  List<? extends Tree> members();

}
