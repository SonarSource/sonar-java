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
package org.sonar.java.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LabeledStatementTree;

import javax.annotation.Nullable;

public class JLabelSymbol extends Symbols.DefaultSymbol implements Symbol.LabelSymbol {

  private final String name;
  LabeledStatementTree declaration;
  final List<IdentifierTree> usages = new ArrayList<>();

  JLabelSymbol(String name) {
    this.name = name;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public LabeledStatementTree declaration() {
    return declaration;
  }

  @Override
  public List<IdentifierTree> usages() {
    return Collections.unmodifiableList(usages);
  }

  @Override
  public Symbol owner() {
    return null;
  }

  @Override
  public Type type() {
    return Type.UNKNOWN;
  }

  @Override
  public boolean isUnknown() {
    return false;
  }

  @Nullable
  @Override
  public TypeSymbol enclosingClass() {
    return null;
  }

}
