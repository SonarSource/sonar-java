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

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S6876")
public class ReversedMethodSequencedCollectionCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  private static final String ISSUE_MESSAGE = "Use the \"reversed()\" method instead of manually iterating the list in reverse.";

  private static final MethodMatchers LIST_ITERATOR_MATCHER = MethodMatchers.create()
    .ofTypes("java.util.List")
    .names("listIterator")
    .addParametersMatcher("int")
    .build();

  private static final MethodMatchers LIST_SIZE_MATCHER = MethodMatchers.create()
    .ofTypes("java.util.List")
    .names("size")
    .addWithoutParametersMatcher()
    .build();

  private static final MethodMatchers LIST_HAS_PREVIOUS_MATCHER = MethodMatchers.create()
    .ofTypes("java.util.ListIterator")
    .names("hasPrevious")
    .addWithoutParametersMatcher()
    .build();

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava21Compatible();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.FOR_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    var forStatement = (ForStatementTree) tree;

    ListTree<StatementTree> initializerStatments = forStatement.initializer();
    if (initializerStatments.size() != 1) {
      return;
    }
    var initializer = initializerStatments.get(0);
    boolean initializerOk = isInitializerOk(initializer);

    var condition = forStatement.condition();
    if (condition == null) {
      return;
    }
    boolean conditionOk = isConditionOk(condition);

    if (initializerOk && conditionOk) {
      reportIssue(forStatement, ISSUE_MESSAGE);
    }
  }

  private static boolean isInitializerOk(Tree initializer) {
    if (initializer.is(Tree.Kind.VARIABLE)) {
      var variableInitializer = ((VariableTree) initializer).initializer();
      if (variableInitializer != null && variableInitializer.is(Tree.Kind.METHOD_INVOCATION) && LIST_ITERATOR_MATCHER.matches((MethodInvocationTree) variableInitializer)) {
        var arg = ((MethodInvocationTree) variableInitializer).arguments().get(0);
        return arg.is(Tree.Kind.METHOD_INVOCATION) && LIST_SIZE_MATCHER.matches((MethodInvocationTree) arg);
      }
    }
    return false;
  }

  private static boolean isConditionOk(Tree condition) {
    return condition.is(Tree.Kind.METHOD_INVOCATION) && LIST_HAS_PREVIOUS_MATCHER.matches((MethodInvocationTree) condition);
  }

}
