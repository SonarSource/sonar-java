/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.java.checks.methods;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Symbol.MethodSymbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

public class MethodMatcher {

  private TypeCriteria typeDefinition;
  private TypeCriteria callSite;
  private NameCriteria methodName;
  private List<TypeCriteria> parameterTypes;

  MethodMatcher() {
    parameterTypes = Lists.newArrayList();
  }

  public static MethodMatcher create() {
    return new MethodMatcher();
  }

  public MethodMatcher name(String methodName) {
    this.methodName = NameCriteria.is(methodName);
    return this;
  }

  public MethodMatcher name(NameCriteria methodName) {
    Preconditions.checkState(this.methodName == null);
    this.methodName = methodName;
    return this;
  }

  public MethodMatcher typeDefinition(TypeCriteria typeDefinition) {
    Preconditions.checkState(this.typeDefinition == null);
    this.typeDefinition = typeDefinition;
    return this;
  }

  public MethodMatcher typeDefinition(String fullyQualifiedTypeName) {
    Preconditions.checkState(typeDefinition == null);
    this.typeDefinition = TypeCriteria.is(fullyQualifiedTypeName);
    return this;
  }

  public MethodMatcher callSite(TypeCriteria callSite) {
    this.callSite = callSite;
    return this;
  }

  public MethodMatcher addParameter(String fullyQualifiedTypeParameterName) {
    Preconditions.checkState(parameterTypes != null);
    parameterTypes.add(TypeCriteria.is(fullyQualifiedTypeParameterName));
    return this;
  }

  public MethodMatcher addParameter(TypeCriteria parameterTypeCriteria) {
    Preconditions.checkState(parameterTypes != null);
    parameterTypes.add(parameterTypeCriteria);
    return this;
  }

  public MethodMatcher withNoParameterConstraint() {
    Preconditions.checkState(parameterTypes == null || parameterTypes.isEmpty());
    parameterTypes = null;
    return this;
  }

  public boolean matches(NewClassTree newClassTree) {
    return matches(newClassTree.constructorSymbol(), null);
  }

  public boolean matches(MethodInvocationTree mit) {
    IdentifierTree id = getIdentifier(mit);
    return id != null && matches(id.symbol(), getCallSiteType(mit));
  }

  public boolean matches(MethodTree methodTree) {
    MethodSymbol symbol = methodTree.symbol();
    Symbol.TypeSymbol enclosingClass = symbol.enclosingClass();
    return enclosingClass != null && matches(symbol, enclosingClass.type());
  }

  private boolean matches(Symbol symbol, Type callSiteType) {
    return symbol.isMethodSymbol() && isSearchedMethod((MethodSymbol) symbol, callSiteType);
  }

  private static Type getCallSiteType(MethodInvocationTree mit) {
    ExpressionTree methodSelect = mit.methodSelect();
    if (methodSelect.is(Tree.Kind.IDENTIFIER)) {
      Symbol.TypeSymbol enclosingClassSymbol = ((IdentifierTree) methodSelect).symbol().enclosingClass();
      if (enclosingClassSymbol != null) {
        return enclosingClassSymbol.type();
      }
    } else if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) methodSelect;
      return memberSelect.expression().symbolType();
    }
    return null;
  }

  private boolean isSearchedMethod(MethodSymbol symbol, Type callSiteType) {
    boolean result = nameAcceptable(symbol) && parametersAcceptable(symbol);
    if (typeDefinition != null) {
      result &= typeDefinition.matches(symbol.owner().type());
    }
    if (callSite != null) {
      result &= callSiteType != null && callSite.matches(callSiteType);
    }
    return result;
  }

  private boolean nameAcceptable(MethodSymbol symbol) {
    return methodName != null && methodName.matches(symbol.name());
  }

  private boolean parametersAcceptable(MethodSymbol methodSymbol) {
    if (parameterTypes == null) {
      return true;
    }
    List<Type> parametersTypes = methodSymbol.parameterTypes();
    List<TypeCriteria> arguments = parameterTypes;
    if (parametersTypes.size() == arguments.size()) {
      int i = 0;
      for (Type parameterType : parametersTypes) {
        if (!arguments.get(i).matches(parameterType)) {
          return false;
        }
        i++;
      }
    } else {
      return false;
    }
    return true;
  }

  private static IdentifierTree getIdentifier(MethodInvocationTree mit) {
    IdentifierTree id = null;
    if (mit.methodSelect().is(Tree.Kind.IDENTIFIER)) {
      id = (IdentifierTree) mit.methodSelect();
    } else if (mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      id = ((MemberSelectExpressionTree) mit.methodSelect()).identifier();
    }
    return id;
  }
}
