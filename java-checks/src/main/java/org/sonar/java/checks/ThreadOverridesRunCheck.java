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

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2134")
public class ThreadOverridesRunCheck extends IssuableSubscriptionVisitor {

  private static final String JAVA_LANG_THREAD = "java.lang.Thread";
  private static final MethodMatchers RUN = MethodMatchers.create()
    .ofSubTypes(JAVA_LANG_THREAD)
    .names("run")
    .addWithoutParametersMatcher()
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    Symbol.TypeSymbol classSymbol = classTree.symbol();
    if (classSymbol != null
      && isDirectSubtypeOfThread(classSymbol)
      && !overridesRunMethod(classSymbol)
      && !hasConstructorCallingSuperWithRunnable(classTree)) {
      Tree report = classTree.simpleName();
      Tree parent = classTree.parent();
      if(parent.is(Tree.Kind.NEW_CLASS)) {
        NewClassTree newClassTree = (NewClassTree) parent;
        if (hasRunnableArgument(newClassTree.arguments())) {
          // will call the super constructor setting a runnable which will be executed by the run() method
          return;
        }
        report = newClassTree.identifier();
      }
      reportIssue(report, "Don't extend \"Thread\", since the \"run\" method is not overridden.");
    }
  }

  private static boolean isDirectSubtypeOfThread(Symbol.TypeSymbol classSymbol) {
    Type superClass = classSymbol.superClass();
    // Only 'java.lang.Object' has no super class
    return superClass != null && superClass.is(JAVA_LANG_THREAD);
  }

  private static boolean overridesRunMethod(Symbol.TypeSymbol classSymbol) {
    return classSymbol.lookupSymbols("run").stream().anyMatch(RUN::matches);
  }

  private static boolean hasConstructorCallingSuperWithRunnable(ClassTree classTree) {
    return classTree.members().stream()
      .filter(member -> member.is(Tree.Kind.CONSTRUCTOR))
      .map(MethodTree.class::cast)
      .anyMatch(ThreadOverridesRunCheck::hasCallToSuperWithRunnable);
  }

  private static boolean hasRunnableArgument(Arguments args) {
    return args.stream().map(ExpressionTree::symbolType).anyMatch(ThreadOverridesRunCheck::isRunnable);
  }

  private static boolean isRunnable(Type argType) {
    return argType.isUnknown() || argType.isSubtypeOf("java.lang.Runnable");
  }

  private static boolean hasCallToSuperWithRunnable(MethodTree constructor) {
    SuperRunnableVisitor visitor = new SuperRunnableVisitor();
    constructor.accept(visitor);
    return visitor.callSuperWithRunnable;
  }

  private static class SuperRunnableVisitor extends BaseTreeVisitor {

    private boolean callSuperWithRunnable = false;

    private static final MethodMatchers SUPER_THREAD = MethodMatchers.create()
      .ofTypes(JAVA_LANG_THREAD)
      .constructor()
      .withAnyParameters()
      .build();

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (SUPER_THREAD.matches(tree) && ThreadOverridesRunCheck.hasRunnableArgument(tree.arguments())) {
        callSuperWithRunnable = true;
        // no need to visit further
        return;
      }
      super.visitMethodInvocation(tree);
    }

    @Override
    public void visitClass(ClassTree tree) {
      // skip inner classes
    }
  }
}
