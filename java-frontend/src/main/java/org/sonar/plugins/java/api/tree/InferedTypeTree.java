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
package org.sonar.plugins.java.api.tree;

import org.sonar.java.model.AbstractTypedTree;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class InferedTypeTree extends AbstractTypedTree implements TypeTree{

  @Override
  public Kind kind() {
    return Kind.INFERED_TYPE;
  }

  @Override
  public boolean isLeaf() {
    return true;
  }

  @Override
  @Nullable
  public SyntaxToken firstToken() {
    return null;
  }

  @Override
  @Nullable
  public SyntaxToken lastToken() {
    return null;
  }

  @Override
  public List<Tree> children() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void accept(TreeVisitor visitor) {
    //Do nothing.
  }

  @Override
  public List<AnnotationTree> annotations() {
    return Collections.emptyList();
  }

}
