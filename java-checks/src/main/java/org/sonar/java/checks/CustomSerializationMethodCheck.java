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
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.resolve.Symbol.MethodSymbol;
import org.sonar.java.resolve.Symbol.TypeSymbol;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2061",
  name = "Custom serialization method signatures should meet requirements",
  tags = {"bug"},
  priority = Priority.CRITICAL)
@BelongsToProfile(title = "Sonar way", priority = Priority.CRITICAL)
@SqaleSubCharacteristic(value = RulesDefinition.SubCharacteristics.DATA_RELIABILITY)
@SqaleConstantRemediation(value = "5min")
public class CustomSerializationMethodCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTreeImpl methodTree = (MethodTreeImpl) tree;
    MethodSymbol methodSymbol = methodTree.getSymbol();
    if (hasSemantic() && isOwnedBySerializable(methodSymbol)) {
      if (hasSignature(methodSymbol, "writeObject", "java.io.ObjectOutputStream")) {
        checkPrivate(methodTree);
        checkNotStatic(methodTree);
      } else if (hasSignature(methodSymbol, "readObject", "java.io.ObjectInputStream")) {
        checkPrivate(methodTree);
        checkNotStatic(methodTree);
      } else if (hasSignature(methodSymbol, "readObjectNoData")) {
        checkPrivate(methodTree);
        checkNotStatic(methodTree);
      } else if (hasSignature(methodSymbol, "writeReplace")) {
        checkNotStatic(methodTree);
        checkReturnType(methodTree, "java.lang.Object");
      } else if (hasSignature(methodSymbol, "readResolve")) {
        checkNotStatic(methodTree);
        checkReturnType(methodTree, "java.lang.Object");
      }
    }
  }

  private boolean isOwnedBySerializable(MethodSymbol methodSymbol) {
    TypeSymbol owner = (TypeSymbol) methodSymbol.owner();
    return owner.getType().isSubtypeOf("java.io.Serializable");
  }

  private boolean hasSignature(MethodSymbol methodSymbol, String name, String paramType) {
    return name.equals(methodSymbol.getName()) && hasSingleParam(methodSymbol, paramType);
  }

  private boolean hasSignature(MethodSymbol methodSymbol, String name) {
    return name.equals(methodSymbol.getName()) && methodSymbol.getParametersTypes().isEmpty();
  }

  private boolean hasSingleParam(MethodSymbol methodSymbol, String searchedParamType) {
    List<Type> parametersTypes = methodSymbol.getParametersTypes();
    return parametersTypes.size() == 1 && parametersTypes.get(0).is(searchedParamType);
  }

  private void checkNotStatic(MethodTreeImpl methodTree) {
    MethodSymbol methodSymbol = methodTree.getSymbol();
    if (methodSymbol.isStatic()) {
      addIssue(methodTree, "The \"static\" modifier should not be applied to \"" + methodSymbol.getName() + "\".");
    }
  }

  private void checkPrivate(MethodTreeImpl methodTree) {
    MethodSymbol methodSymbol = methodTree.getSymbol();
    if (!methodSymbol.isPrivate()) {
      addIssue(methodTree, "Make \"" + methodSymbol.getName() + "\" \"private\".");
    }
  }

  private void checkReturnType(MethodTreeImpl methodTree, String requiredReturnType) {
    MethodSymbol methodSymbol = methodTree.getSymbol();
    if (!methodSymbol.getReturnType().getType().is(requiredReturnType)) {
      addIssue(methodTree, "\"" + methodSymbol.getName() + "\" should return \"" + requiredReturnType + "\".");
    }
  }

}
