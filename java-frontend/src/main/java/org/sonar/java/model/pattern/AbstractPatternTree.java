/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.SemanticValues;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.PatternTree;
import org.sonar.plugins.java.api.tree.Tree;

/**
 * JDK 17 Preview feature  (JEP-405, JEP-406), finalized in JDK 21 (JEP-440, JEP-441).
 */
public abstract class AbstractPatternTree extends JavaTree implements PatternTree {

  private final Tree.Kind kind;

  public Supplier<Type> type;

  AbstractPatternTree(Tree.Kind kind, @Nullable Supplier<Type> type) {
    this.kind = kind;
    this.type = type != null ? type : SemanticValues.UNKNOWN_TYPE;
  }

  @Override
  public Type symbolType() {
    return type.get();
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
