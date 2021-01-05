/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.java.model;

import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import java.util.List;

public class InternalSyntaxToken extends JavaTree implements SyntaxToken {

  private final List<SyntaxTrivia> trivias;
  private final int line;
  private final int column;
  private final String value;
  private final boolean isEOF;

  protected InternalSyntaxToken(InternalSyntaxToken internalSyntaxToken) {
    this.value = internalSyntaxToken.value;
    this.line = internalSyntaxToken.line;
    this.column = internalSyntaxToken.column;
    this.trivias = internalSyntaxToken.trivias;
    this.isEOF = internalSyntaxToken.isEOF;
  }

  public InternalSyntaxToken(int line, int column, String value, List<SyntaxTrivia> trivias, boolean isEOF) {
    this.value = value;
    this.line = line;
    this.column = column;
    this.trivias = trivias;
    this.isEOF = isEOF;
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
    // FIXME do nothing at the moment
  }

  @Override
  public int getLine() {
    return line;
  }

  @Override
  public int line() {
    return line;
  }

  @Override
  public int column() {
    return column;
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
