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
package org.sonar.java.model.declaration;

import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifierKeywordTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;

public class ModifierKeywordTreeImpl extends InternalSyntaxToken implements ModifierKeywordTree {

  private final Modifier modifier;

  public ModifierKeywordTreeImpl(Modifier modifier, InternalSyntaxToken internalSyntaxToken) {
    super(internalSyntaxToken);
    this.modifier = modifier;
  }

  @Override
  public Modifier modifier() {
    return modifier;
  }

  @Override
  public SyntaxToken keyword() {
    return this;
  }
}
