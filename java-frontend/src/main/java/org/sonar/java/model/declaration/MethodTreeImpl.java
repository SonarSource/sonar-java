/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.sonar.java.Preconditions;
import org.sonar.java.ast.parser.FormalParametersListTreeImpl;
import org.sonar.java.ast.parser.QualifiedIdentifierListTreeImpl;
import org.sonar.java.ast.parser.TypeParameterListTreeImpl;
import org.sonar.java.cfg.CFG;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JUtils;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.java.model.Symbols;
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
  @Nullable
  private final SyntaxToken openParenToken;
  private final FormalParametersListTreeImpl parameters;
  @Nullable
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

  @Nullable
  public IMethodBinding methodBinding;

  /**
   * Cache for {@link #isOverriding()}.
   */
  @Nullable
  private Boolean isOverriding;
  // isOverriding is Nullable, we need a way to know if the value null is a cache miss or a computed value.
  private boolean isOverridingCached = false;

  public MethodTreeImpl(FormalParametersListTreeImpl parameters, @Nullable SyntaxToken defaultToken, @Nullable ExpressionTree defaultValue) {
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

  @Nullable
  @Override
  public SyntaxToken openParenToken() {
    return openParenToken;
  }

  @Override
  public List<VariableTree> parameters() {
    return (List) parameters;
  }

  @Nullable
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
    return methodBinding != null
      ? root.sema.methodSymbol(methodBinding)
      : Symbols.unknownMethodSymbol;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitMethod(this);
  }

  @Override
  public int getLine() {
    InternalSyntaxToken token = parameters.openParenToken();
    if (token != null) {
      return token.getLine();
    } else {
      // type cast may fail, it is fine. We will just add a new case if it happens.
      // could first try with type cast and fallback parameters
      // but cannot reach full coverage
      InternalSyntaxToken name = (InternalSyntaxToken) simpleName().identifierToken();
      return name.getLine();
    }
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
  public List<Tree> children() {
    List<Tree> list = new ArrayList<>();
    list.add(modifiers);
    list.add(typeParameters);
    if (returnType != null) {
      list.add(returnType);
    }
    list.add(simpleName);
    if (openParenToken != null) {
      list.add(openParenToken);
      list.addAll(parameters);
      list.add(closeParenToken);
    }
    if (throwsToken != null) {
      list.add(throwsToken);
      list.add(throwsClauses);
    }
    if (defaultToken != null) {
      list.add(defaultToken);
      list.add(defaultValue);
    }
    if (block != null) {
      list.add(block);
    } else {
      list.add(semicolonToken);
    }
    return Collections.unmodifiableList(list);
  }

  @Override
  @Nullable
  public Boolean isOverriding() {
    if (isOverridingCached) {
      return isOverriding;
    }
    isOverridingCached = true;

    if (isStatic() || isPrivate()) {
      isOverriding = false;
    } else if (isAnnotatedOverride()) {
      isOverriding = true;
    } else {
      Symbol.MethodSymbol symbol = symbol();
      List<Symbol.MethodSymbol> overriddenSymbols = symbol.overriddenSymbols();
      if (overriddenSymbols.isEmpty()) {
        isOverriding = JUtils.hasUnknownTypePreventingOverrideResolution(symbol) ? null : false;
      } else {
        isOverriding = overriddenSymbols.stream().allMatch(Symbol::isUnknown) ? null : true;
      }
    }
    return isOverriding;
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
