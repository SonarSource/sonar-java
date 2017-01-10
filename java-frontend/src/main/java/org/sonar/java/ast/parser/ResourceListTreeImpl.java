/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java.ast.parser;

import com.google.common.collect.ImmutableList;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.List;

public class ResourceListTreeImpl extends ListTreeImpl<VariableTree> {

  public ResourceListTreeImpl(List<VariableTree> resources, List<SyntaxToken> tokens) {
    super(JavaLexer.RESOURCE_SPECIFICATION, resources, tokens);
  }

  public static ListTree<VariableTree> emptyList() {
    return new ResourceListTreeImpl(ImmutableList.<VariableTree>of(), ImmutableList.<SyntaxToken>of());
  }
}
