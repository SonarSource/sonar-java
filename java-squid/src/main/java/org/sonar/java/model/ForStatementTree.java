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
 * 'for' statement.
 *
 * JLS 14.14
 *
 * <pre>
 *   for ( {@link #initializer()} ; {@link #condition()} ; {@link #update()} ) {@link #statement()}
 * </pre>
 *
 * @since Java 1.3
 */
public interface ForStatementTree extends StatementTree {

  List<? extends StatementTree> initializer();

  @Nullable
  ExpressionTree condition();

  List<? extends StatementTree> update();

  StatementTree statement();

}
