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
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.MethodInvocationMatcherCollection;
import org.sonar.java.checks.methods.MethodMatcher;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.TypeParameters;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Rule(
  key = "UnusedPrivateMethod",
  name = "Unused private method should be removed",
  tags = {"unused"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("5min")
public class UnusedPrivateMethodCheck extends SubscriptionBaseVisitor {

  private static final MethodInvocationMatcherCollection SERIALIZABLE_METHODS = MethodInvocationMatcherCollection.create(
    MethodMatcher.create().name("writeObject").addParameter("java.io.ObjectOutputStream"),
    MethodMatcher.create().name("readObject").addParameter("java.io.ObjectInputStream"),
    MethodMatcher.create().name("writeReplace"),
    MethodMatcher.create().name("readResolve"),
    MethodMatcher.create().name("readObjectNoData")
    );

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.COMPILATION_UNIT);
  }

  @Override
  public void visitNode(Tree tree) {
    for (Tree memberTree : ((CompilationUnitTree) tree).types()) {
      MethodInvocationVisitor methodInvocationVisitor = new MethodInvocationVisitor();
      memberTree.accept(methodInvocationVisitor);
      memberTree.accept(new MethodVisitor(methodInvocationVisitor.calledPrivateMethods));
    }
  }

  private static class MethodInvocationVisitor extends BaseTreeVisitor {
    final Set<Symbol> calledPrivateMethods = new HashSet<>();

    @Override
    public void visitNewClass(NewClassTree tree) {
      Symbol constructorSymbol = tree.constructorSymbol();
      if (constructorSymbol.isPrivate()) {
        calledPrivateMethods.add(constructorSymbol);
      }
      super.visitNewClass(tree);
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      Symbol methodSymbol = tree.symbol();
      if (methodSymbol.isPrivate()) {
        calledPrivateMethods.add(methodSymbol);
      }
      super.visitMethodInvocation(tree);
    }
  }

  private class MethodVisitor extends BaseTreeVisitor {
    final Set<Symbol> calledPrivateMethods;

    MethodVisitor(Set<Symbol> calledPrivateMethods) {
      this.calledPrivateMethods = calledPrivateMethods;
    }

    @Override
    public void visitMethod(MethodTree tree) {
      if (!containsParameterizedTypes(tree.typeParameters())) {
        Symbol methodSymbol = tree.symbol();
        if (methodSymbol.isPrivate()
          && !calledPrivateMethods.contains(methodSymbol)
          && !SERIALIZABLE_METHODS.anyMatch(tree)) {
          String kind = tree.is(Tree.Kind.CONSTRUCTOR) ? "constructor" : "method";
          addIssue(tree, "Private " + kind + " '" + tree.simpleName().name() + "' is never used.");
        }
        super.visitMethod(tree);
      }
    }

    private boolean containsParameterizedTypes(TypeParameters typeParameters) {
      for (TypeParameterTree typeParameterTree : typeParameters) {
        for (Tree bound : typeParameterTree.bounds()) {
          if (bound.is(Tree.Kind.PARAMETERIZED_TYPE)) {
            return true;
          }
        }
      }
      return false;
    }
  }

}
