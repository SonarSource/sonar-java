/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.model.declaration;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.sonar.sslr.api.AstNode;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.java.model.JavaTree;
import org.sonar.java.resolve.Symbol;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MethodTreeImpl extends JavaTree implements MethodTree {

  private ModifiersTree modifiers;
  private final List<TypeParameterTree> typeParameters;
  @Nullable
  private Tree returnType;
  private IdentifierTree simpleName;
  private final List<VariableTree> parameters;
  @Nullable
  private final BlockTree block;
  private final List<ExpressionTree> throwsClauses;
  private final ExpressionTree defaultValue;

  private Symbol.MethodSymbol symbol;

  public MethodTreeImpl(@Nullable ExpressionTree defaultValue) {
    super(Kind.METHOD);
    this.typeParameters = ImmutableList.<TypeParameterTree>of();
    this.parameters = ImmutableList.<VariableTree>of();
    this.block = null;
    this.throwsClauses = ImmutableList.<ExpressionTree>of();
    this.defaultValue = defaultValue;

    if (defaultValue != null) {
      addChild((AstNode) defaultValue);
    }
  }

  public MethodTreeImpl(AstNode astNode, ModifiersTree modifiers, List<TypeParameterTree> typeParameters, @Nullable Tree returnType, IdentifierTree simpleName,
    List<VariableTree> parameters,
    @Nullable BlockTree block,
    List<ExpressionTree> throwsClauses, @Nullable ExpressionTree defaultValue) {
    super(astNode);
    this.modifiers = Preconditions.checkNotNull(modifiers);
    this.typeParameters = typeParameters;
    this.returnType = returnType;
    this.simpleName = Preconditions.checkNotNull(simpleName);
    this.parameters = Preconditions.checkNotNull(parameters);
    this.block = block;
    this.throwsClauses = Preconditions.checkNotNull(throwsClauses);
    this.defaultValue = defaultValue;
  }

  public MethodTreeImpl complete(Tree returnType, IdentifierTree simpleName) {
    Preconditions.checkState(this.simpleName == null);
    this.returnType = returnType;
    this.simpleName = simpleName;

    prependChildren((AstNode) returnType, (AstNode) simpleName);

    return this;
  }

  public MethodTreeImpl complete(ModifiersTreeImpl modifiers) {
    Preconditions.checkState(this.modifiers == null);
    this.modifiers = modifiers;

    prependChildren(modifiers);

    return this;
  }

  @Override
  public Kind getKind() {
    return returnType == null ? Kind.CONSTRUCTOR : Kind.METHOD;
  }

  @Override
  public ModifiersTree modifiers() {
    return modifiers;
  }

  @Override
  public List<TypeParameterTree> typeParameters() {
    return typeParameters;
  }

  @Nullable
  @Override
  public Tree returnType() {
    return returnType;
  }

  @Override
  public IdentifierTree simpleName() {
    return simpleName;
  }

  @Override
  public List<VariableTree> parameters() {
    return parameters;
  }

  @Override
  public List<ExpressionTree> throwsClauses() {
    return throwsClauses;
  }

  @Nullable
  @Override
  public BlockTree block() {
    return block;
  }

  @Nullable
  @Override
  public ExpressionTree defaultValue() {
    return defaultValue;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitMethod(this);
  }

  public Symbol.MethodSymbol getSymbol() {
    return symbol;
  }

  public void setSymbol(Symbol.MethodSymbol symbol) {
    Preconditions.checkState(this.symbol == null);
    this.symbol = symbol;
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return Iterators.concat(
      Iterators.singletonIterator(modifiers),
      typeParameters.iterator(),
      Iterators.forArray(
        returnType,
        simpleName
        ),
      parameters.iterator(),
      Iterators.singletonIterator(block),
      throwsClauses.iterator(),
      Iterators.singletonIterator(defaultValue)
      );
  }

  /**
   * Check if a methodTree is overriden.
   * @return true if overriden, null if it cannot be decided (method symbol not resolved or lack of bytecode for super types).
   */
  public Boolean isOverriden() {
    if(isStatic() || isPrivate()) {
      return false;
    }
    if (isAnnotatedOverride()) {
      return true;
    }
    if(symbol == null) {
      return null;
    }
    Boolean result = false;
    Symbol.TypeSymbol enclosingClass = symbol.enclosingClass();
    if (StringUtils.isEmpty(enclosingClass.getName())) {
      //FIXME : SONARJAVA-645 : exclude methods within anonymous classes
      return null;
    }
    for (Type.ClassType type : superTypes(enclosingClass)) {
      Boolean overrideFromType = overridesFromSymbol(type);
      if (overrideFromType == null) {
        result = null;
      } else if (BooleanUtils.isTrue(overrideFromType)) {
        return true;
      }
    }
    return result;
  }

  private boolean isStatic() {
    return modifiers.modifiers().contains(Modifier.STATIC);
  }

  private boolean isPrivate() {
    return modifiers.modifiers().contains(Modifier.PRIVATE);
  }

  private Set<Type.ClassType> superTypes(Symbol.TypeSymbol enclosingClass) {
    ImmutableSet.Builder<Type.ClassType> types = ImmutableSet.builder();
    Type.ClassType superClassType = (Type.ClassType) enclosingClass.getSuperclass();
    types.addAll(interfacesOfType(enclosingClass));
    while (superClassType != null) {
      types.add(superClassType);
      Symbol.TypeSymbol superClassSymbol = superClassType.getSymbol();
      types.addAll(interfacesOfType(superClassSymbol));
      superClassType = (Type.ClassType) superClassSymbol.getSuperclass();
    }
    return types.build();
  }

  private Set<Type.ClassType> interfacesOfType(Symbol.TypeSymbol typeSymbol) {
    ImmutableSet.Builder<Type.ClassType> builder = ImmutableSet.builder();
    for (Type type : typeSymbol.getInterfaces()) {
      Type.ClassType classType = (Type.ClassType) type;
      builder.add(classType);
      builder.addAll(interfacesOfType(classType.getSymbol()));
    }
    return builder.build();
  }

  private Boolean overridesFromSymbol(Type.ClassType classType) {
    Boolean result = false;
    if (classType.isTagged(Type.UNKNOWN)) {
      return null;
    }
    List<Symbol> symbols = classType.getSymbol().members().lookup(symbol.getName());
    for (Symbol overrideSymbol : symbols) {
      if (overrideSymbol.isKind(Symbol.MTH) && isOverridableBy((Symbol.MethodSymbol) overrideSymbol)) {
        Boolean isOverriding = isOverriding((Symbol.MethodSymbol) overrideSymbol);
        if (isOverriding == null) {
          result = null;
        } else if (BooleanUtils.isTrue(isOverriding)) {
          return true;
        }
      }
    }
    return result;
  }

  /**
   * Methods have the same name and overidee is in a supertype of the enclosing class of overrider.
   */
  private boolean isOverridableBy(Symbol.MethodSymbol overridee) {
    if (overridee.isPackageVisibility()) {
      return overridee.outermostClass().owner().equals(symbol.outermostClass().owner());
    }
    return !overridee.isPrivate();
  }

  private Boolean isOverriding(Symbol.MethodSymbol overridee) {
    //same number and type of formal parameters
    if (symbol.getParametersTypes().size() != overridee.getParametersTypes().size()) {
      return false;
    }
    for (int i = 0; i < symbol.getParametersTypes().size(); i++) {
      Type paramOverrider = symbol.getParametersTypes().get(i);
      if (paramOverrider.isTagged(Type.UNKNOWN)) {
        //FIXME : complete symbol table should not have unknown types.
        return null;
      }
      if (!paramOverrider.equals(overridee.getParametersTypes().get(i))) {
        return false;
      }
    }
    //we assume code is compiling so no need to check return type at this point.
    return true;
  }

  private boolean isAnnotatedOverride() {
    for (AnnotationTree annotationTree : modifiers.annotations()) {
      if (annotationTree.annotationType().is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree identifier = (IdentifierTree) annotationTree.annotationType();
        if (Override.class.getSimpleName().equals(identifier.name())) {
          return true;
        }
      }
    }
    return false;
  }



}
