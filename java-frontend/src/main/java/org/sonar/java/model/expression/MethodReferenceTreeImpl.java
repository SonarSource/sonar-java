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
package org.sonar.java.model.expression;

import java.util.Arrays;
import java.util.List;
import org.sonarsource.analyzer.commons.collections.ListUtils;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeArguments;

import javax.annotation.Nullable;
import java.util.Collections;

public class MethodReferenceTreeImpl extends AssessableExpressionTree implements MethodReferenceTree {

  private Tree expression;
  private SyntaxToken doubleColon;
  private IdentifierTree method;
  private TypeArguments typeArgument;

  public MethodReferenceTreeImpl(Tree expression, InternalSyntaxToken doubleColon) {
    this.expression = expression;
    this.doubleColon = doubleColon;
  }

  public void complete(@Nullable TypeArguments typeArgument, IdentifierTreeImpl method) {
    this.typeArgument = typeArgument;
    this.method = method;
  }

  @Override
  public Kind kind() {
    return Kind.METHOD_REFERENCE;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitMethodReference(this);
  }

  @Override
  public List<Tree> children() {
    return ListUtils.concat(
      typeArgument != null ? Collections.singletonList(typeArgument) : Collections.<Tree>emptyList(),
      Arrays.asList(expression, doubleColon, method));
  }

  @Override
  public Tree expression() {
    return expression;
  }

  @Override
  public SyntaxToken doubleColon() {
    return doubleColon;
  }

  @Override
  public IdentifierTree method() {
    return method;
  }

  @Nullable
  @Override
  public TypeArguments typeArguments() {
    return typeArgument;
  }
}
