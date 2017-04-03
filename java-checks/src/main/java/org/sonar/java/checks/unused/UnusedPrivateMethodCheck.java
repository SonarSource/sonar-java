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
package org.sonar.java.checks.unused;

import com.google.common.collect.ImmutableList;

import org.sonar.check.Rule;
import org.sonar.java.RspecKey;
import org.sonar.java.checks.helpers.MethodsHelper;
import org.sonar.java.checks.serialization.SerializableContract;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Rule(key = "UnusedPrivateMethod")
@RspecKey("S1144")
public class UnusedPrivateMethodCheck extends IssuableSubscriptionVisitor {

  private final List<MethodTree> unusedPrivateMethods = new ArrayList<>();
  private final Set<String> unresolvedMethodNames = new HashSet<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR, Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS);
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    super.scanFile(context);
    reportUnusedPrivateMethods();
    unusedPrivateMethods.clear();
    unresolvedMethodNames.clear();
  }

  private void reportUnusedPrivateMethods() {
    unusedPrivateMethods.stream()
      .filter(methodTree -> !unresolvedMethodNames.contains(methodTree.simpleName().name()))
      .forEach(methodTree -> {
        IdentifierTree simpleName = methodTree.simpleName();
        reportIssue(simpleName, String.format("Remove this unused private \"%s\" %s.", simpleName.name(), methodTree.is(Tree.Kind.CONSTRUCTOR) ? "constructor" : "method"));
      });
  }

  @Override
  public void visitNode(Tree tree) {
    if(!hasSemantic()) {
      return;
    }
    if (tree.is(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR)) {
      checkIfUnused((MethodTree) tree);
    } else {
      checkIfUnknown(tree);
    }
  }

  private void checkIfUnknown(Tree tree) {
    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      if (mit.symbol().isUnknown()) {
        unresolvedMethodNames.add(MethodsHelper.methodName(mit).name());
      }
    } else {
      NewClassTree nct = (NewClassTree) tree;
      if (nct.constructorSymbol().isUnknown()) {
        unresolvedMethodNames.add(constructorName(nct.identifier()));
      }
    }
  }

  private static String constructorName(TypeTree typeTree) {
    if (typeTree.is(Tree.Kind.PARAMETERIZED_TYPE)) {
      return constructorName(((ParameterizedTypeTree) typeTree).type());
    } else if (typeTree.is(Tree.Kind.MEMBER_SELECT)) {
      return ((MemberSelectExpressionTree) typeTree).identifier().name();
    } else {
      return ((IdentifierTree) typeTree).name();
    }
  }

  private void checkIfUnused(MethodTree methodTree) {
    Symbol symbol = methodTree.symbol();
    if (methodTree.modifiers().annotations().isEmpty() && symbol.isPrivate() && symbol.usages().isEmpty()) {
      if (methodTree.is(Tree.Kind.CONSTRUCTOR)) {
        if (!methodTree.parameters().isEmpty()) {
          unusedPrivateMethods.add(methodTree);
        }
      } else if (!SerializableContract.SERIALIZABLE_CONTRACT_METHODS.contains(symbol.name())) {
        unusedPrivateMethods.add(methodTree);
      }
    }

  }

}
