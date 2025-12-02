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
package org.sonar.java.model.expression;

import java.util.Arrays;
import java.util.List;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.sonar.java.ast.parser.ArgumentListTreeImpl;
import org.sonarsource.analyzer.commons.collections.ListUtils;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeArguments;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Objects;

public class MethodInvocationTreeImpl extends AssessableExpressionTree implements MethodInvocationTree {

  private final ExpressionTree methodSelect;
  private final Arguments arguments;
  @Nullable
  private TypeArguments typeArguments;

  @Nullable
  public IMethodBinding methodBinding;

  public MethodInvocationTreeImpl(ExpressionTree methodSelect, @Nullable TypeArguments typeArguments, ArgumentListTreeImpl arguments) {
    this.methodSelect = Objects.requireNonNull(methodSelect);
    this.typeArguments = typeArguments;
    this.arguments = Objects.requireNonNull(arguments);
  }

  @Override
  public Kind kind() {
    return Kind.METHOD_INVOCATION;
  }

  @Nullable
  @Override
  public TypeArguments typeArguments() {
    return typeArguments;
  }

  @Override
  public SyntaxToken firstToken() {
    if (typeArguments() != null && methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      ExpressionTree expression = ((MemberSelectExpressionTree) methodSelect).expression();
      SyntaxToken firstToken = expression.firstToken();
      if (firstToken != null) {
        return firstToken;
      }
    }
    return super.firstToken();
  }

  @Override
  public ExpressionTree methodSelect() {
    return methodSelect;
  }

  @Override
  public Arguments arguments() {
    return arguments;
  }

  @Override
  public Symbol symbol() {
    return methodSymbol();
  }

  @Override
  public Symbol.MethodSymbol methodSymbol() {
    return methodBinding != null
      ? root.sema.methodSymbol(methodBinding)
      : Symbol.MethodSymbol.UNKNOWN_METHOD;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitMethodInvocation(this);
  }

  @Override
  public List<Tree> children() {
    return ListUtils.concat(
      typeArguments != null ? Collections.singletonList(typeArguments) : Collections.<Tree>emptyList(),
      Arrays.asList(methodSelect, arguments));
  }
}
