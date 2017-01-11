/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.apache.commons.lang.BooleanUtils;
import org.sonar.check.Rule;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.resolve.ClassJavaType;
import org.sonar.java.resolve.JavaSymbol.MethodJavaSymbol;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;
import java.util.Set;

import static org.sonar.plugins.java.api.semantic.Symbol.MethodSymbol;
import static org.sonar.plugins.java.api.semantic.Symbol.TypeSymbol;

@Rule(key = "S2177")
public class ConfusingOverloadCheck extends IssuableSubscriptionVisitor {
  private static final Set<String> SERIALIZATION_METHOD_NAME = Sets.newHashSet("writeObject", "readObject", "readObjectNoData", "writeReplace", "readResolve");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    MethodTreeImpl methodTree = (MethodTreeImpl) tree;
    if (BooleanUtils.isFalse(methodTree.isOverriding())) {
      MethodSymbol methodSymbol = methodTree.symbol();
      TypeSymbol owner = (TypeSymbol) methodSymbol.owner();
      Type superClass = owner.superClass();
      if(superClass != null && !SERIALIZATION_METHOD_NAME.contains(methodSymbol.name())) {
        boolean reportStaticIssue = checkMethod(methodTree.simpleName(), methodSymbol, superClass);
        superClass = superClass.symbol().superClass();
        while (superClass != null && !reportStaticIssue) {
          reportStaticIssue = checkStaticMethod(methodTree.simpleName(), methodSymbol, superClass);
          superClass = superClass.symbol().superClass();
        }
      }
    }
  }

  private boolean checkStaticMethod(Tree reportTree, MethodSymbol methodSymbol, Type superClass) {
    for (Symbol methodWithSameName : superClass.symbol().lookupSymbols(methodSymbol.name())) {
      if (methodWithSameName.isMethodSymbol() && hideStaticMethod(methodSymbol, superClass, methodWithSameName)) {
        reportIssue(reportTree, "Rename this method or make it \"static\".");
        return true;
      }
    }
    return false;
  }

  private boolean checkMethod(Tree reportTree, MethodSymbol methodSymbol, Type superClass) {
    boolean reportStaticIssue = false;
    for (Symbol methodWithSameName : superClass.symbol().lookupSymbols(methodSymbol.name())) {
      if (methodWithSameName.isMethodSymbol()) {
        if (hideStaticMethod(methodSymbol, superClass, methodWithSameName)) {
          reportIssue(reportTree, "Rename this method or make it \"static\".");
          reportStaticIssue = true;
        } else if (confusingOverload(methodSymbol, (MethodSymbol) methodWithSameName)) {
          reportIssue(reportTree, getMessage(methodWithSameName));
        }
      }
    }
    return reportStaticIssue;
  }

  private static String getMessage(Symbol methodWithSameName) {
    String message = "Rename this method or correct the type of the argument(s) to override the parent class method.";
    if(methodWithSameName.isPrivate()) {
      message = "Rename this method; there is a \"private\" method in the parent class with the same name.";
    }
    return message;
  }

  private static boolean hideStaticMethod(MethodSymbol methodSymbol, Type superClass, Symbol symbolWithSameName) {
    return symbolWithSameName.isStatic()
      && !methodSymbol.isStatic()
      && BooleanUtils.isTrue(((MethodJavaSymbol) methodSymbol).checkOverridingParameters((MethodJavaSymbol) symbolWithSameName, (ClassJavaType) superClass));
  }

  private static boolean confusingOverload(MethodSymbol methodSymbol, MethodSymbol methodWithSameName) {
    if (methodSymbol.isStatic()) {
      return false;
    }
    List<Type> argTypes = methodSymbol.parameterTypes();
    List<Type> parameterTypes = methodWithSameName.parameterTypes();
    if (argTypes.size() != parameterTypes.size()) {
      return false;
    }
    for (int i = 0; i < argTypes.size(); i++) {
      Type argType = argTypes.get(i);
      if (argType.isUnknown() || !argType.name().equals(parameterTypes.get(i).name())) {
        return false;
      }
    }
    return true;
  }

}
