/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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

import java.util.Arrays;
import java.util.List;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonarsource.analyzer.commons.collections.ListUtils;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.InferedTypeTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Objects;

public class VariableTreeImpl extends JavaTree implements VariableTree {
  private ModifiersTree modifiers;
  private TypeTree type;
  private IdentifierTree simpleName;
  @Nullable
  private SyntaxToken equalToken;
  @Nullable
  private ExpressionTree initializer;
  @Nullable
  private SyntaxToken endToken;

  public IVariableBinding variableBinding;

  public VariableTreeImpl(
    ModifiersTree modifiers,
    TypeTree type,
    IdentifierTree simpleName
  ) {
    this.modifiers = modifiers;
    this.type = type;
    this.simpleName = simpleName;
  }

  public VariableTreeImpl(IdentifierTreeImpl simpleName) {
    this.modifiers = ModifiersTreeImpl.emptyModifiers();
    this.simpleName = simpleName;
    this.initializer = null;
    this.type = new InferedTypeTree();
  }

  public VariableTreeImpl(ModifiersTree modifiers, IdentifierTree simpleName, @Nullable ExpressionTree initializer) {
    this.modifiers = Objects.requireNonNull(modifiers);
    this.simpleName = Objects.requireNonNull(simpleName);
    this.initializer = initializer;
  }

  public VariableTreeImpl completeType(TypeTree type) {
    this.type = type;
    return this;
  }

  public VariableTreeImpl completeModifiers(ModifiersTreeImpl modifiers) {
    this.modifiers = modifiers;
    return this;
  }

  public VariableTreeImpl completeModifiersAndType(ModifiersTreeImpl modifiers, TypeTree type) {
    return completeModifiers(modifiers).
      completeType(type);
  }

  public VariableTreeImpl completeTypeAndInitializer(TypeTree type, InternalSyntaxToken equalToken, ExpressionTree initializer) {
    this.initializer = initializer;
    this.equalToken = equalToken;

    return completeType(type);
  }

  @Override
  public Kind kind() {
    return Kind.VARIABLE;
  }

  @Override
  public ModifiersTree modifiers() {
    return modifiers;
  }

  @Override
  public TypeTree type() {
    return type;
  }

  @Override
  public IdentifierTree simpleName() {
    return simpleName;
  }

  @Nullable
  @Override
  public ExpressionTree initializer() {
    return initializer;
  }

  @Nullable
  @Override
  public SyntaxToken equalToken() {
    return equalToken;
  }

  @Override
  public Symbol symbol() {
    return variableBinding != null
      ? root.sema.variableSymbol(variableBinding)
      : Symbol.UNKNOWN_SYMBOL;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitVariable(this);
  }

  @Override
  public int getLine() {
    return ((IdentifierTreeImpl) simpleName()).getLine();
  }

  @Override
  public List<Tree> children() {
    return ListUtils.concat(
      Arrays.asList(modifiers, type, simpleName),
      initializer != null ? Arrays.asList(equalToken, initializer) : Collections.<Tree>emptyList(),
      endToken != null ? Collections.singletonList(endToken) : Collections.<Tree>emptyList()
    );
  }

  @CheckForNull
  @Override
  public SyntaxToken endToken() {
    return endToken;
  }

  public void setEndToken(InternalSyntaxToken endToken) {
    this.endToken = endToken;
  }
}
