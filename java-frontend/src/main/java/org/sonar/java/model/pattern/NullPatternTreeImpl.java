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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.NullPatternTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

/**
 * JDK 17 Preview feature (JEP-406), finalized in JDK 21 (JEP-441).
 */
public class NullPatternTreeImpl extends AbstractPatternTree implements NullPatternTree {

  private final LiteralTree nullLiteral;

  public NullPatternTreeImpl(LiteralTree nullLiteral) {
    super(Tree.Kind.NULL_PATTERN, null);
    this.nullLiteral = nullLiteral;
  }

  @Override
  public Type symbolType() {
    return nullLiteral.symbolType();
  }

  @Override
  public Optional<Object> asConstant() {
    return nullLiteral.asConstant();
  }

  @Override
  public <T> Optional<T> asConstant(Class<T> type) {
    return nullLiteral.asConstant(type);
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitNullPattern(this);
  }

  @Override
  public LiteralTree nullLiteral() {
    return nullLiteral;
  }

  @Override
  protected List<Tree> children() {
    return Collections.singletonList(nullLiteral);
  }

}
