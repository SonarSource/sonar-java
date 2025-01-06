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

import java.util.List;
import org.sonar.java.model.location.InternalPosition;
import org.sonar.plugins.java.api.location.Range;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

public class InternalSyntaxToken extends JavaTree implements SyntaxToken {

  private final List<SyntaxTrivia> trivias;
  private final Range range;
  private final String value;
  private final boolean isEOF;

  protected InternalSyntaxToken(InternalSyntaxToken internalSyntaxToken) {
    this.value = internalSyntaxToken.value;
    this.range = internalSyntaxToken.range;
    this.trivias = internalSyntaxToken.trivias;
    this.isEOF = internalSyntaxToken.isEOF;
  }

  public InternalSyntaxToken(int line, int columnOffset, String value, List<SyntaxTrivia> trivias, boolean isEOF) {
    this.value = value;
    this.trivias = trivias;
    this.isEOF = isEOF;
    range = value.startsWith("\"\"\"")
      ? Range.at(InternalPosition.atOffset(line, columnOffset), value)
      : Range.at(InternalPosition.atOffset(line, columnOffset), value.length());
  }

  @Override
  public Range range() {
    return range;
  }

  @Override
  public SyntaxToken firstToken() {
    return this;
  }

  @Override
  public SyntaxToken lastToken() {
    return this;
  }

  @Override
  public String text() {
    return value;
  }

  @Override
  public List<SyntaxTrivia> trivias() {
    return trivias;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    // do nothing
  }

  @Override
  public int getLine() {
    return range.start().line();
  }

  @Override
  public int line() {
    return range.start().line();
  }

  @Override
  public int column() {
    return range.start().columnOffset();
  }

  @Override
  public Kind kind() {
    return Kind.TOKEN;
  }

  @Override
  public boolean isLeaf() {
    return true;
  }

  public boolean isEOF() {
    return isEOF;
  }

  @Override
  public List<Tree> children() {
    throw new UnsupportedOperationException();
  }

}
