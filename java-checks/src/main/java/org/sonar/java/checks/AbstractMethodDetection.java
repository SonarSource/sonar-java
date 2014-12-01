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
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.resolve.Symbol;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

public abstract class AbstractMethodDetection extends SubscriptionBaseVisitor {

  private MethodDefinition methodDefinition;

  protected AbstractMethodDetection(MethodDefinition methodDefinition) {
    this.methodDefinition = methodDefinition;
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree mit = (MethodInvocationTree) tree;
    if (hasSemantic() && methodDefinition.findMethod(mit, getSemanticModel())) {
      onMethodFound(mit);
    }
  }


  protected void onMethodFound(MethodInvocationTree mit) {
    //Do nothing by default
  }

  protected static class MethodDefinition {

    private String fullyQualifiedTypeName;
    private String methodName;
    private List<String> parameterTypes;

    private MethodDefinition() {
      parameterTypes = Lists.newArrayList();
    }

    public static MethodDefinition create() {
      return new MethodDefinition();
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

    public MethodDefinition name(String methodName) {
      this.methodName = methodName;
      return this;
    }

    public MethodDefinition type(String fullyQualifiedTypeName) {
      this.fullyQualifiedTypeName = fullyQualifiedTypeName;
      return this;
    }

    public MethodDefinition addParameter(String fullyQualifiedTypeParameterName) {
      parameterTypes.add(fullyQualifiedTypeParameterName);
      return this;
    }

    public boolean findMethod(MethodInvocationTree mit, SemanticModel semanticModel) {
      IdentifierTree id = getIdentifier(mit);
      if (id != null) {
        Symbol symbol = semanticModel.getReference(id);
        if (symbol != null && symbol.isKind(Symbol.MTH)) {
          Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) symbol;
          if (isSearchedMethod(methodSymbol)) {
            return true;
          }
        }
      }
      return false;
    }

    private boolean isSearchedMethod(Symbol.MethodSymbol symbol) {
      return symbol.owner().getType().is(fullyQualifiedTypeName) && symbol.getName().equals(methodName) && parametersAcceptable(symbol);
    }

    private boolean parametersAcceptable(Symbol.MethodSymbol methodSymbol) {
      boolean isSeekCall = true;
      List<Type> parametersTypes = methodSymbol.getParametersTypes();
      List<String> arguments = parameterTypes;
      if (parametersTypes.size() == arguments.size()) {
        int i = 0;
        for (Type parameterType : parametersTypes) {
          if (!parameterType.is(arguments.get(i))) {
            isSeekCall = false;
            break;
          }
          i++;
        }
      }
      return isSeekCall;
    }

  }
}
