/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S1149")
public class SynchronizedClassUsageCheck extends IssuableSubscriptionVisitor {

  private static final Map<String, String> REPLACEMENTS = Map.of(
    "java.util.Vector", "\"ArrayList\" or \"LinkedList\"",
    "java.util.Hashtable", "\"HashMap\"",
    "java.lang.StringBuffer", "\"StringBuilder\"",
    "java.util.Stack", "\"Deque\"");

  private final Deque<Set<String>> exclusions = new ArrayDeque<>();

  private final Set<Tree> visited = new HashSet<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    // We register on compilation units to clear the visited set when scanning a new file
    return List.of(Tree.Kind.CLASS, Tree.Kind.COMPILATION_UNIT, Tree.Kind.ENUM, Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.COMPILATION_UNIT)) {
      // We clear the visited set when entering every file
      visited.clear();
      return;
    }
    ExclusionsVisitor exclusionsVisitor = new ExclusionsVisitor();
    tree.accept(exclusionsVisitor);
    Set<String> currentClassExclusions = exclusionsVisitor.exclusions;
    if (!exclusions.isEmpty()) {
      currentClassExclusions.addAll(exclusions.peek());
    }
    exclusions.push(currentClassExclusions);
    tree.accept(new DeprecatedTypeVisitor());
  }


  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Tree.Kind.COMPILATION_UNIT)) {
      return;
    }
    exclusions.pop();
  }

  private static class ExclusionsVisitor extends BaseTreeVisitor {
    Set<String> exclusions = new HashSet<>();

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (tree.symbol().isMethodSymbol() && tree.symbol().declaration() == null) {
        String fqn = tree.symbol().owner().type().fullyQualifiedName();
        if (isMethodFromJavaPackage(fqn)) {
          Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) tree.symbol();
          List<Type> types = new ArrayList<>(methodSymbol.parameterTypes());
          Symbol.TypeSymbol returnType = methodSymbol.returnType();
          if (returnType != null) {
            types.add(returnType.type());
          }
          types.forEach(t -> exclusions.addAll(REPLACEMENTS.keySet().stream().filter(t::isSubtypeOf).collect(Collectors.toSet())));
        }
      }
      super.visitMethodInvocation(tree);
    }

    private static boolean isMethodFromJavaPackage(String fqn) {
      return fqn.startsWith("java") && !REPLACEMENTS.containsKey(fqn);
    }
  }

  private class DeprecatedTypeVisitor extends BaseTreeVisitor {

    @Override
    public void visitClass(ClassTree tree) {
      TypeTree superClass = tree.superClass();
      if (superClass != null) {
        reportIssueOnDeprecatedType(ExpressionsHelper.reportOnClassTree(tree), superClass.symbolType());
      }

      scan(tree.members());
    }

    @Override
    public void visitMethod(MethodTree tree) {
      TypeTree returnTypeTree = tree.returnType();
      if (isNotOverriding(tree) || returnTypeTree == null) {
        if (returnTypeTree != null) {
          reportIssueOnDeprecatedType(returnTypeTree, returnTypeTree.symbolType());
        }
        scan(tree.parameters());
      }
      scan(tree.block());
    }

    private boolean isNotOverriding(MethodTree tree) {
      // In case it can not be determined (isOverriding returns null), return false to avoid FP.
      return Boolean.FALSE.equals(tree.isOverriding());
    }

    @Override
    public void visitVariable(VariableTree tree) {
      ExpressionTree initializer = tree.initializer();
      if (!reportIssueOnDeprecatedType(tree.type(), tree.symbol().type()) && initializer != null && !initializer.is(Tree.Kind.METHOD_INVOCATION)) {
        reportIssueOnDeprecatedType(initializer, initializer.symbolType());
      }
    }

    @Override
    public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
      // Skip parameter types
      scan(lambdaExpressionTree.body());
    }

    private boolean reportIssueOnDeprecatedType(Tree tree, Type type) {
      if (visited.contains(tree)) {
        return false;
      }
      visited.add(tree);
      if (isDeprecatedType(type)) {
        reportIssue(tree, "Replace the synchronized class \"" + type.name() + "\" by an unsynchronized one such as " + REPLACEMENTS.get(type.fullyQualifiedName()) + ".");
        return true;
      }
      return false;
    }

    private boolean isDeprecatedType(Type symbolType) {
      if (symbolType.isClass()) {
        for (String deprecatedType : REPLACEMENTS.keySet()) {
          if (symbolType.is(deprecatedType)) {
            return !exclusions.peek().contains(deprecatedType);
          }
        }
      }
      return false;
    }
  }
}
