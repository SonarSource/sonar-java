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
package org.sonar.plugins.java.api.tree;

import com.google.common.annotations.Beta;

/**
 * The 'provides' directive from java 9 module directives
 * 
 * JLS9 - §7.7.4
 * 
 * <pre>
 *   provides {@link #typeName()} with {@link #typeNames()} ;
 * </pre>
 * 
 * @since Java 9
 */
@Beta
public interface ProvidesDirectiveTree extends ModuleDirectiveTree {

  TypeTree typeName();

  SyntaxToken withKeyword();

  ListTree<TypeTree> typeNames();
}
