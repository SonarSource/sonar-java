/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.java.model.pattern;

import java.util.Optional;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.Symbols;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.PatternTree;
import org.sonar.plugins.java.api.tree.Tree;

/**
 * JDK 17 Preview feature  (JEP-406), deprecated by design until it will be final
 * java:S1874 = "@Deprecated" code should not be used
 */
@SuppressWarnings("java:S1874")
public abstract class AbstractPatternTree extends JavaTree implements PatternTree {

  private final Tree.Kind kind;

  AbstractPatternTree(Tree.Kind kind) {
    this.kind = kind;
  }

  @Override
  public Type symbolType() {
    return Symbols.unknownType;
  }

  @Override
  public Optional<Object> asConstant() {
    return Optional.empty();
  }

  @Override
  public <T> Optional<T> asConstant(Class<T> type) {
    return Optional.empty();
  }

  @Override
  public Tree.Kind kind() {
    return kind;
  }
}
