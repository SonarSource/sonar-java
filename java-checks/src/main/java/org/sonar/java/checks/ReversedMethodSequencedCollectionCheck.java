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

    ListTree<StatementTree> initializerStatements = forStatement.initializer();
    if (initializerStatements.size() != 1) {
      return;
    }
    var initializer = initializerStatements.get(0);
    var condition = forStatement.condition();
    if (condition == null) {
      return;
    }

    if (isInitializerListIteratorFromLast(initializer) && isConditionHasPrevious(condition)) {
      reportIssue(forStatement.forKeyword(), ISSUE_MESSAGE);
    }
  }

  private static boolean isInitializerListIteratorFromLast(Tree initializer) {
    return initializer instanceof VariableTree variable
      && variable.initializer() instanceof MethodInvocationTree variableInitializer
      && LIST_ITERATOR_MATCHER.matches(variableInitializer)
      && variableInitializer.arguments().get(0) instanceof MethodInvocationTree arg
      && LIST_SIZE_MATCHER.matches(arg);
  }

  private static boolean isConditionHasPrevious(Tree condition) {
    return condition instanceof MethodInvocationTree invocation && LIST_HAS_PREVIOUS_MATCHER.matches(invocation);
  }

}
