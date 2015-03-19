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
package org.sonar.java.checks.methods;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.sonar.java.model.expression.NewClassTreeImpl;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.resolve.Symbol.MethodSymbol;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

public class MethodInvocationMatcher {

  private TypeCriteria typeDefinition;
  private TypeCriteria callSite;
  private String methodName;
  private List<String> parameterTypes;

  MethodInvocationMatcher() {
    parameterTypes = Lists.newArrayList();
  }

  public static MethodInvocationMatcher create() {
    return new MethodInvocationMatcher();
  }

  public MethodInvocationMatcher name(String methodName) {
    this.methodName = methodName;
    return this;
  }

  public MethodInvocationMatcher typeDefinition(TypeCriteria typeDefinition) {
    Preconditions.checkState(this.typeDefinition == null);
    this.typeDefinition = typeDefinition;
    return this;
  }

  public MethodInvocationMatcher typeDefinition(String fullyQualifiedTypeName) {
    Preconditions.checkState(typeDefinition == null);
    this.typeDefinition = TypeCriteria.is(fullyQualifiedTypeName);
    return this;
  }

  public MethodInvocationMatcher callSite(TypeCriteria callSite) {
    this.callSite = callSite;
    return this;
  }

  public MethodInvocationMatcher addParameter(String fullyQualifiedTypeParameterName) {
    Preconditions.checkState(parameterTypes != null);
    parameterTypes.add(fullyQualifiedTypeParameterName);
    return this;
  }

  public MethodInvocationMatcher withNoParameterConstraint() {
    Preconditions.checkState(parameterTypes == null || parameterTypes.isEmpty());
    parameterTypes = null;
    return this;
  }

  public boolean matches(NewClassTree newClassTree, SemanticModel semanticModel) {
    NewClassTreeImpl newClassTreeImpl = (NewClassTreeImpl) newClassTree;
    return matches(newClassTreeImpl.getConstructorIdentifier(), null, semanticModel);
  }

  public boolean matches(MethodInvocationTree mit, SemanticModel semanticModel) {
    IdentifierTree id = getIdentifier(mit);
    if (id != null) {
      return matches(id, getCallSiteType(mit, semanticModel), semanticModel);
    }
    return false;
  }

  private boolean matches(IdentifierTree id, Type callSiteType, SemanticModel semanticModel) {
    Symbol symbol = semanticModel.getReference(id);
    if (symbol != null && symbol.isMethodSymbol()) {
      MethodSymbol methodSymbol = (MethodSymbol) symbol;
      if (isSearchedMethod(methodSymbol, callSiteType)) {
        return true;
      }
    }
    return false;
  }

  private Type getCallSiteType(MethodInvocationTree mit, SemanticModel semanticModel) {
    if (mit.methodSelect().is(Tree.Kind.IDENTIFIER)) {
      return semanticModel.getEnclosingClass(mit).type();
    } else if (mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree methodSelect = (MemberSelectExpressionTree) mit.methodSelect();
      return methodSelect.expression().symbolType();
    }
    return null;
  }

  private boolean isSearchedMethod(MethodSymbol symbol, Type callSiteType) {
    boolean result = symbol.getName().equals(methodName) && parametersAcceptable(symbol);
    if (typeDefinition != null) {
      result &= typeDefinition.matches(symbol.owner().getType());
    }
    if (callSite != null) {
      result &= callSiteType != null && callSite.matches(callSiteType);
    }
    return result;
  }

  private boolean parametersAcceptable(MethodSymbol methodSymbol) {
    if (parameterTypes == null) {
      return true;
    }
    List<org.sonar.java.resolve.Type> parametersTypes = methodSymbol.getParametersTypes();
    List<String> arguments = parameterTypes;
    if (parametersTypes.size() == arguments.size()) {
      int i = 0;
      for (Type parameterType : parametersTypes) {
        if (!parameterType.is(arguments.get(i))) {
          return false;
        }
        i++;
      }
    } else {
      return false;
    }
    return true;
  }

  private IdentifierTree getIdentifier(MethodInvocationTree mit) {
    IdentifierTree id = null;
    if (mit.methodSelect().is(Tree.Kind.IDENTIFIER)) {
      id = (IdentifierTree) mit.methodSelect();
    } else if (mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      id = ((MemberSelectExpressionTree) mit.methodSelect()).identifier();
    }
    return id;
  }

}
