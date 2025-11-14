/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model.expression;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JLabelSymbol;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonarsource.analyzer.commons.collections.ListUtils;

public class IdentifierTreeImpl extends AssessableExpressionTree implements IdentifierTree, JavaTree.AnnotatedTypeTree {

  private final InternalSyntaxToken nameToken;
  private final boolean isUnnamedVariable;
  private List<AnnotationTree> annotations;

  public IBinding binding;
  public JLabelSymbol labelSymbol;

  public IdentifierTreeImpl(InternalSyntaxToken nameToken) {
    this(nameToken, false);
  }

  public IdentifierTreeImpl(InternalSyntaxToken nameToken, boolean isUnnamedVariable) {
    this.nameToken = Objects.requireNonNull(nameToken);
    this.isUnnamedVariable = isUnnamedVariable;
    this.annotations = Collections.emptyList();
  }

  @Override
  public void complete(List<AnnotationTree> annotations) {
    this.annotations = Objects.requireNonNull(annotations);
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

  @Override
  public boolean isUnnamedVariable() {
    return isUnnamedVariable;
  }

  @Override
  public Symbol symbol() {
    if (binding != null && !isUnnamedVariable) {
      switch (binding.getKind()) {
        case IBinding.TYPE:
          return root.sema.typeSymbol((ITypeBinding) binding);
        case IBinding.METHOD:
          return root.sema.methodSymbol((IMethodBinding) binding);
        case IBinding.VARIABLE:
          return root.sema.variableSymbol((IVariableBinding) binding);
        case IBinding.PACKAGE:
          return root.sema.packageSymbol((IPackageBinding) binding);
      }
    }
    if (labelSymbol != null) {
      return labelSymbol;
    }
    return Symbol.UNKNOWN_SYMBOL;
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
  public List<Tree> children() {
    return ListUtils.concat(annotations, Collections.singletonList(nameToken));
  }

  @Override
  public List<AnnotationTree> annotations() {
    return annotations;
  }

}
