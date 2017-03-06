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
package org.sonar.java.model.expression;

import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.resolve.Symbols;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IdentifierTreeImpl extends AbstractTypedTree implements IdentifierTree {

  private final InternalSyntaxToken nameToken;
  private Symbol symbol = Symbols.unknownSymbol;
  private List<AnnotationTree> annotations;

  public IdentifierTreeImpl(InternalSyntaxToken nameToken) {
    super(Kind.IDENTIFIER);
    this.nameToken = Objects.requireNonNull(nameToken);
    this.annotations = Collections.emptyList();
  }

  public IdentifierTreeImpl complete(List<AnnotationTree> annotations) {
    this.annotations = Objects.requireNonNull(annotations);
    return this;
  }

  @Override
  public Kind kind() {
    return Kind.IDENTIFIER;
  }

  @Override
  public SyntaxToken identifierToken() {
    return nameToken;
  }

  @Override
  public String name() {
    return identifierToken().text();
  }

  public void setSymbol(Symbol symbol) {
    this.symbol = symbol;
  }

  @Override
  public Symbol symbol() {
    return symbol;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitIdentifier(this);
  }

  @Override
  public String toString() {
    return name();
  }

  @Override
  public Iterable<Tree> children() {
    return Stream.of(annotations, Collections.singletonList(nameToken)).flatMap(Collection::stream).collect(Collectors.toList());
  }

  @Override
  public List<AnnotationTree> annotations() {
    return annotations;
  }

}
