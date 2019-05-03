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
package org.sonar.java.matcher;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Symbol.MethodSymbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

public class MethodMatcher {

  private TypeCriteria typeDefinition;
  private TypeCriteria callSite;
  private NameCriteria methodName;

  private ParametersCriteria parameters;
  private List<TypeCriteria> parameterTypes;

  public static MethodMatcher create() {
    return new MethodMatcher();
  }

  public MethodMatcher copy() {
    MethodMatcher copy = new MethodMatcher();
    copy.typeDefinition = typeDefinition;
    copy.callSite = callSite;
    copy.methodName = methodName;
    copy.parameterTypes = parameterTypes == null ? null : new ArrayList<>(parameterTypes);
    copy.parameters = parameterTypes == null ? null : ParametersCriteria.of(copy.parameterTypes);
    return copy;
  }

  public MethodMatcher name(String methodName) {
    Preconditions.checkState(this.methodName == null);
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
    return addParameter(TypeCriteria.is(fullyQualifiedTypeParameterName));
  }

  public MethodMatcher addParameter(TypeCriteria parameterTypeCriteria) {
    if (parameters == null) {
      parameterTypes = new ArrayList<>();
      parameters = ParametersCriteria.of(parameterTypes);
    } else {
      Preconditions.checkState(parameterTypes != null, "parameters is already initialized and doesn't support addParameter.");
    }
    parameterTypes.add(parameterTypeCriteria);
    return this;
  }

  public MethodMatcher parameters(String... parameterTypes) {
    if (parameterTypes.length == 0) {
      return withoutParameter();
    }
    for (String type : parameterTypes) {
      addParameter(type);
    }
    return this;
  }

  public MethodMatcher parameters(TypeCriteria... parameterTypes) {
    if (parameterTypes.length == 0) {
      return withoutParameter();
    }
    for (TypeCriteria type : parameterTypes) {
      addParameter(type);
    }
    return this;
  }

  public MethodMatcher withAnyParameters() {
    Preconditions.checkState(parameters == null);
    parameters = ParametersCriteria.any();
    return this;
  }

  public MethodMatcher withoutParameter() {
    Preconditions.checkState(parameters == null);
    parameters = ParametersCriteria.none();
    return this;
  }

  public boolean matches(NewClassTree newClassTree) {
    return matches(newClassTree.constructorSymbol(), null);
  }

  public boolean matches(MethodInvocationTree mit) {
    IdentifierTree id = getIdentifier(mit);
    return matches(id.symbol(), getCallSiteType(mit));
  }

  public boolean matches(MethodTree methodTree) {
    MethodSymbol symbol = methodTree.symbol();
    Symbol.TypeSymbol enclosingClass = symbol.enclosingClass();
    return enclosingClass != null && matches(symbol, enclosingClass.type());
  }

  public boolean matches(MethodReferenceTree methodReferenceTree) {
    return matches(methodReferenceTree.method().symbol(), getCallSiteType(methodReferenceTree));
  }

  public boolean matches(Symbol symbol) {
    return matches(symbol, null);
  }

  private boolean matches(Symbol symbol, @Nullable Type callSiteType) {
    return symbol.isMethodSymbol() && isSearchedMethod((MethodSymbol) symbol, callSiteType);
  }

  @CheckForNull
  private static Type getCallSiteType(MethodReferenceTree referenceTree) {
    Tree expression = referenceTree.expression();
    if(expression instanceof ExpressionTree) {
      return ((ExpressionTree) expression).symbolType();
    }
    return null;
  }

  @CheckForNull
  private static Type getCallSiteType(MethodInvocationTree mit) {
    ExpressionTree methodSelect = mit.methodSelect();
    // methodSelect can only be Tree.Kind.IDENTIFIER or Tree.Kind.MEMBER_SELECT
    if (methodSelect.is(Tree.Kind.IDENTIFIER)) {
      Symbol.TypeSymbol enclosingClassSymbol = ((IdentifierTree) methodSelect).symbol().enclosingClass();
      return enclosingClassSymbol != null ? enclosingClassSymbol.type() : null;
    } else {
      return ((MemberSelectExpressionTree) methodSelect).expression().symbolType();
    }
  }

  private boolean isSearchedMethod(MethodSymbol symbol, @Nullable Type callSiteType) {
    boolean result = nameAcceptable(symbol) && parametersAcceptable(symbol);
    if (typeDefinition != null) {
      result &= typeDefinition.test(symbol.owner().type());
    }
    if (callSite != null) {
      result &= callSiteType != null && callSite.test(callSiteType);
    }
    return result;
  }

  private boolean nameAcceptable(MethodSymbol symbol) {
    Preconditions.checkState(methodName != null);
    return methodName.test(symbol.name());
  }

  private boolean parametersAcceptable(MethodSymbol methodSymbol) {
    Preconditions.checkState(parameters != null);
    return parameters.test(methodSymbol.parameterTypes());
  }

  private static IdentifierTree getIdentifier(MethodInvocationTree mit) {
    // methodSelect can only be Tree.Kind.IDENTIFIER or Tree.Kind.MEMBER_SELECT
    if (mit.methodSelect().is(Tree.Kind.IDENTIFIER)) {
      return (IdentifierTree) mit.methodSelect();
    }
    return ((MemberSelectExpressionTree) mit.methodSelect()).identifier();
  }

}
