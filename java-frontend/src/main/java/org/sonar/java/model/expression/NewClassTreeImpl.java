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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeArguments;
import org.sonar.plugins.java.api.tree.TypeTree;

import javax.annotation.Nullable;

public class NewClassTreeImpl extends AssessableExpressionTree implements NewClassTree {

  @Nullable
  private ExpressionTree enclosingExpression;
  @Nullable
  private SyntaxToken dotToken;
  @Nullable
  private SyntaxToken newKeyword;
  @Nullable
  private TypeArguments typeArguments;
  private TypeTree identifier;
  private final Arguments arguments;
  @Nullable
  private final ClassTree classBody;

  public NewClassTreeImpl(TypeTree identifier, Arguments arguments, @Nullable ClassTreeImpl classBody) {
    this.enclosingExpression = null;
    this.identifier = identifier;
    this.arguments = arguments;
    this.classBody = classBody;
  }


  public NewClassTreeImpl completeWithEnclosingExpression(ExpressionTree enclosingExpression) {
    this.enclosingExpression = enclosingExpression;
    return this;
  }

  public NewClassTreeImpl completeWithNewKeyword(SyntaxToken newKeyword) {
    this.newKeyword = newKeyword;
    return this;
  }

  public NewClassTreeImpl completeWithTypeArguments(@Nullable TypeArgumentListTreeImpl typeArguments) {
    this.typeArguments = typeArguments;
    return this;
  }

  @Override
  public Kind kind() {
    return Kind.NEW_CLASS;
  }

  @Nullable
  @Override
  public ExpressionTree enclosingExpression() {
    return enclosingExpression;
  }

  @Nullable
  @Override
  public TypeArguments typeArguments() {
    return typeArguments;
  }

  @Override
  public TypeTree identifier() {
    return identifier;
  }

  @Override
  public Arguments arguments() {
    return arguments;
  }

  @Nullable
  @Override
  public ClassTree classBody() {
    return classBody;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitNewClass(this);
  }

  @Override
  public List<Tree> children() {
    List<Tree> builder = new ArrayList<>();
    addIfNotNull(builder, enclosingExpression, dotToken, newKeyword, typeArguments);
    builder.add(identifier);
    builder.add(arguments);
    addIfNotNull(builder, classBody);
    return Collections.unmodifiableList(builder);
  }

  public IdentifierTree getConstructorIdentifier() {
    return getConstructorIdentifier(identifier());
  }

  private static IdentifierTree getConstructorIdentifier(Tree constructorSelect) {
    IdentifierTree constructorIdentifier;
    if (constructorSelect.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mset = (MemberSelectExpressionTree) constructorSelect;
      constructorIdentifier = mset.identifier();
    } else if (constructorSelect.is(Tree.Kind.IDENTIFIER)) {
      constructorIdentifier = (IdentifierTree) constructorSelect;
    } else if (constructorSelect.is(Tree.Kind.PARAMETERIZED_TYPE)) {
      constructorIdentifier = getConstructorIdentifier(((ParameterizedTypeTree) constructorSelect).type());
    } else {
      throw new IllegalStateException("Constructor select is not of the expected type " + constructorSelect);
    }
    return constructorIdentifier;
  }
  @Nullable
  @Override
  public SyntaxToken newKeyword() {
    return newKeyword;
  }

  public void completeWithDotToken(InternalSyntaxToken dotToken) {
    this.dotToken = dotToken;
  }

  @Nullable
  @Override
  public SyntaxToken dotToken() {
    return dotToken;
  }

  @Override
  public Symbol constructorSymbol() {
    return methodSymbol();
  }

  @Override
  public Symbol.MethodSymbol methodSymbol() {
    Symbol constructorSymbol = this.getConstructorIdentifier().symbol();
    return constructorSymbol.isUnknown() ?
      Symbol.MethodSymbol.UNKNOWN_METHOD :
      (Symbol.MethodSymbol) constructorSymbol;
  }

  private static void addIfNotNull(List<Tree> list, Tree... trees) {
    for (Tree tree : trees) {
      if (tree != null) {
        list.add(tree);
      }
    }
  }
}
