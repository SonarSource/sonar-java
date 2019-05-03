/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.model.declaration;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.sonar.java.ast.parser.FormalParametersListTreeImpl;
import org.sonar.java.ast.parser.QualifiedIdentifierListTreeImpl;
import org.sonar.java.ast.parser.TypeParameterListTreeImpl;
import org.sonar.java.cfg.CFG;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeParameters;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

public class MethodTreeImpl extends JavaTree implements MethodTree {

  private ModifiersTree modifiers;
  private TypeParameters typeParameters;
  @Nullable
  private TypeTree returnType;
  private IdentifierTree simpleName;
  private final SyntaxToken openParenToken;
  private final FormalParametersListTreeImpl parameters;
  private final SyntaxToken closeParenToken;
  @Nullable
  private final BlockTree block;
  @Nullable
  private SyntaxToken semicolonToken;
  @Nullable
  private final SyntaxToken throwsToken;
  private final ListTree<TypeTree> throwsClauses;
  private final SyntaxToken defaultToken;
  private final ExpressionTree defaultValue;

  @Nullable
  private CFG cfg;

  //FIXME nullable if semantic analysis is not set. Should have a default value.
  @Nullable
  private JavaSymbol.MethodJavaSymbol symbol;

  public MethodTreeImpl(FormalParametersListTreeImpl parameters, @Nullable SyntaxToken defaultToken, @Nullable ExpressionTree defaultValue) {
    super(Kind.METHOD);
    this.typeParameters = new TypeParameterListTreeImpl();
    this.parameters = parameters;
    this.openParenToken = parameters.openParenToken();
    this.closeParenToken = parameters.closeParenToken();
    this.block = null;
    this.throwsToken = null;
    this.throwsClauses = QualifiedIdentifierListTreeImpl.emptyList();
    this.defaultToken = defaultToken;
    this.defaultValue = defaultValue;
  }

  public MethodTreeImpl(
    @Nullable TypeTree returnType,
    IdentifierTree simpleName,
    FormalParametersListTreeImpl parameters,
    @Nullable SyntaxToken throwsToken,
    ListTree<TypeTree> throwsClauses,
    @Nullable BlockTree block,
    @Nullable SyntaxToken semicolonToken) {

    super(returnType == null ? Kind.CONSTRUCTOR : Kind.METHOD);

    this.typeParameters = new TypeParameterListTreeImpl();
    this.modifiers = null;
    this.returnType = returnType;
    this.simpleName = Objects.requireNonNull(simpleName);
    this.parameters = Objects.requireNonNull(parameters);
    this.openParenToken = parameters.openParenToken();
    this.closeParenToken = parameters.closeParenToken();
    this.block = block;
    this.semicolonToken = semicolonToken;
    this.throwsToken = throwsToken;
    this.throwsClauses = Objects.requireNonNull(throwsClauses);
    this.defaultToken = null;
    this.defaultValue = null;
  }

  public MethodTreeImpl complete(TypeTree returnType, IdentifierTree simpleName, SyntaxToken semicolonToken) {
    Preconditions.checkState(this.simpleName == null);
    this.returnType = returnType;
    this.simpleName = simpleName;
    this.semicolonToken = semicolonToken;

    return this;
  }

  public MethodTreeImpl completeWithTypeParameters(TypeParameterListTreeImpl typeParameters) {
    this.typeParameters = typeParameters;
    return this;
  }

  public MethodTreeImpl completeWithModifiers(ModifiersTreeImpl modifiers) {
    Preconditions.checkState(this.modifiers == null);
    this.modifiers = modifiers;

    return this;
  }

  @Override
  public Kind kind() {
    return returnType == null ? Kind.CONSTRUCTOR : Kind.METHOD;
  }

  @Override
  public ModifiersTree modifiers() {
    return modifiers;
  }

  @Override
  public TypeParameters typeParameters() {
    return typeParameters;
  }

  @Nullable
  @Override
  public TypeTree returnType() {
    return returnType;
  }

  @Override
  public IdentifierTree simpleName() {
    return simpleName;
  }

  @Override
  public SyntaxToken openParenToken() {
    return openParenToken;
  }

  @Override
  public List<VariableTree> parameters() {
    return (List) parameters;
  }

  @Override
  public SyntaxToken closeParenToken() {
    return closeParenToken;
  }

  @Override
  public SyntaxToken throwsToken() {
    return throwsToken;
  }
  @Override
  public ListTree<TypeTree> throwsClauses() {
    return throwsClauses;
  }

  @Nullable
  @Override
  public BlockTree block() {
    return block;
  }

  @Nullable
  @Override
  public SyntaxToken semicolonToken() {
    return semicolonToken;
  }

  @Nullable
  @Override
  public SyntaxToken defaultToken() {
    return defaultToken;
  }

  @Nullable
  @Override
  public ExpressionTree defaultValue() {
    return defaultValue;
  }

  @Override
  public Symbol.MethodSymbol symbol() {
    return symbol;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitMethod(this);
  }

  public void setSymbol(JavaSymbol.MethodJavaSymbol symbol) {
    Preconditions.checkState(this.symbol == null);
    this.symbol = symbol;
  }

  @Override
  public int getLine() {
    return parameters.openParenToken().getLine();
  }

  @Nullable
  @Override
  public CFG cfg() {
    if (block == null) {
      return null;
    }
    if (cfg == null) {
      cfg = CFG.build(this);
    }
    return cfg;
  }

  @Override
  public Iterable<Tree> children() {
    ImmutableList.Builder<Tree> iteratorBuilder = ImmutableList.builder();
    iteratorBuilder.add(modifiers, typeParameters);
    if (returnType != null) {
      iteratorBuilder.add(returnType);
    }
    iteratorBuilder.add(simpleName, openParenToken);
    iteratorBuilder.addAll(parameters.iterator());
    iteratorBuilder.add(closeParenToken);
    if (throwsToken != null) {
      iteratorBuilder.add(throwsToken);
      iteratorBuilder.add(throwsClauses);
    }
    if (defaultToken != null) {
      iteratorBuilder.add(defaultToken, defaultValue);
    }
    if (block != null) {
      iteratorBuilder.add(block);
    } else {
      iteratorBuilder.add(semicolonToken);
    }
    return iteratorBuilder.build();
  }

  @Override
  @Nullable
  public Boolean isOverriding() {
    if (isStatic() || isPrivate()) {
      return false;
    }
    if (isAnnotatedOverride()) {
      return true;
    }
    if (symbol == null) {
      return null;
    }
    Symbol.MethodSymbol methodSymbol = symbol.overriddenSymbol();
    if (methodSymbol != null) {
      return methodSymbol.isUnknown() ? null : true;
    }
    return false;
  }

  private boolean isStatic() {
    return ModifiersUtils.hasModifier(modifiers, Modifier.STATIC);
  }

  private boolean isPrivate() {
    return ModifiersUtils.hasModifier(modifiers, Modifier.PRIVATE);
  }

  public boolean isAnnotatedOverride() {
    for (AnnotationTree annotationTree : modifiers.annotations()) {
      if (isJavaLangOverride(annotationTree.annotationType())) {
        return true;
      }
    }
    return false;
  }

  private static boolean isJavaLangOverride(TypeTree annotationType) {
    return (annotationType.is(Tree.Kind.IDENTIFIER) && isOverride((IdentifierTree) annotationType))
      || (annotationType.is(Kind.MEMBER_SELECT) && isJavaLangOverride((MemberSelectExpressionTree) annotationType));
  }

  private static boolean isJavaLangOverride(MemberSelectExpressionTree annotationType) {
    MemberSelectExpressionTree mse = annotationType;
    if(isOverride(mse.identifier()) && mse.expression().is(Kind.MEMBER_SELECT)) {
      mse = (MemberSelectExpressionTree) mse.expression();
      return "lang".equals(mse.identifier().name()) && mse.expression().is(Kind.IDENTIFIER) && "java".equals(((IdentifierTree) mse.expression()).name());
    }
    return false;
  }

  private static boolean isOverride(IdentifierTree id) {
    return "Override".equals(id.name());
  }

}
