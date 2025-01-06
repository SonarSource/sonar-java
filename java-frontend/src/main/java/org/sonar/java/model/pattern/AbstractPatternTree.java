/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model.pattern;

import java.util.Optional;
import javax.annotation.Nullable;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.Symbols;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.PatternTree;
import org.sonar.plugins.java.api.tree.Tree;

/**
 * JDK 17 Preview feature  (JEP-405, JEP-406), finalized in JDK 21 (JEP-440, JEP-441).
 */
public abstract class AbstractPatternTree extends JavaTree implements PatternTree {

  private final Tree.Kind kind;

  @Nullable
  public ITypeBinding typeBinding;

  AbstractPatternTree(Tree.Kind kind, @Nullable ITypeBinding typeBinding) {
    this.kind = kind;
    this.typeBinding = typeBinding;
  }

  @Override
  public Type symbolType() {
    return typeBinding != null
      ? root.sema.type(typeBinding)
      : Symbols.unknownType;
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
