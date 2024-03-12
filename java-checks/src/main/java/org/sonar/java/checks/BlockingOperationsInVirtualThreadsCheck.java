/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.ArrayList;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6881")
public class BlockingOperationsInVirtualThreadsCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  /*
   * #### Disclaimers ####
   *
   * This rule would benefit from a call graph analysis to determine which blocking operations are executed in a thread. For proper
   * reporting, it would be important to report issues with a flow, clearly showing the path from the blocking operation to the thread
   * creation. While we already have tools to find reachable methods (see TreeHelper#findReachableMethodsInSameFile), they do not provide
   * the necessary information to build a flow and hence a report would be quite confusing. For now, the rule is limited to only very basic
   * cases.
   *
   * On the other hand, this rule may falsely trigger on some cases where the blocking operation is not executed in the thread, but e.g.
   * returned within a lambda or object to be executed later. I.e. a thread creating a runnable for later execution.
   */

  private static final String JAVA_LANG_THREAD = "java.lang.Thread";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.NEW_CLASS, Tree.Kind.METHOD_INVOCATION, Tree.Kind.METHOD);
  }

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava21Compatible();
  }

  // This rule currently only supports a limited set of blocking operations, focused on core Java HTTP requests.
  private static final MethodMatchers blockingOperations = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes("java.net.URLConnection", "java.net.HttpURLConnection")
      .names("getResponseCode", "getResponseMessage")
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofTypes(JAVA_LANG_THREAD)
      .names("sleep")
      .addParametersMatcher("long")
      .build());

  private static final MethodMatchers threadCreationConstructors = MethodMatchers.create()
    .ofSubTypes(JAVA_LANG_THREAD)
    .constructor()
    .addParametersMatcher("java.lang.Runnable")
    .build();

  private static final MethodMatchers platformThreadMethods = MethodMatchers.create()
    .ofTypes(JAVA_LANG_THREAD + "$Builder$OfPlatform")
    .names("start", "unstarted")
    .addParametersMatcher("java.lang.Runnable")
    .build();

  // For cases where a method is overwritten to define a thread's behavior (instead of e.g. passing a lambda to a Thread's constructor).
  private static final MethodMatchers overwritableThreadMethods = MethodMatchers.create()
    .ofSubTypes(JAVA_LANG_THREAD)
    .names("run")
    .addWithoutParametersMatcher()
    .build();

  @Override
  public void visitNode(Tree tree) {
    switch (tree.kind()) {
      case NEW_CLASS -> onConstructorFound((NewClassTree) tree);
      case METHOD_INVOCATION -> onMethodInvocationFound((MethodInvocationTree) tree);
      case METHOD -> onMethodFound((MethodTree) tree);
    }
  }

  private void onConstructorFound(NewClassTree newClassTree) {
    if (threadCreationConstructors.matches(newClassTree)) {
      analyzeForIssues(newClassTree.arguments().get(0), newClassTree.identifier());
    }
  }

  private void onMethodInvocationFound(MethodInvocationTree mit) {
    if (platformThreadMethods.matches(mit)) {
      analyzeForIssues(mit.arguments().get(0), mit.methodSelect());
    }
  }

  private void onMethodFound(MethodTree tree) {
    if (overwritableThreadMethods.matches(tree)) {
      analyzeForIssues(tree, tree.simpleName());
    }
  }

  private void analyzeForIssues(Tree tree, Tree secondary) {
    var finder = new BlockingOperationFinder();
    tree.accept(finder);
    finder.collectedBlockingOperations.forEach(mit -> reportIssue(
      ExpressionUtils.methodName(mit),
      "Use virtual threads for heavy blocking operations.",
      List.of(new JavaFileScannerContext.Location("Containing thread", secondary)),
      null));
  }

  private static class BlockingOperationFinder extends BaseTreeVisitor {
    final List<MethodInvocationTree> collectedBlockingOperations = new ArrayList<>();

    @Override
    public void visitMethodInvocation(MethodInvocationTree mit) {
      if (blockingOperations.matches(mit)) {
        collectedBlockingOperations.add(mit);
      }
    }
  }
}
