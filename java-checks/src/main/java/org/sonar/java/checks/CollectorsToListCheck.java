/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S6204")
public class CollectorsToListCheck extends AbstractMethodDetection implements JavaVersionAwareVisitor {
  private static class Issue {
    private static final String MESSAGE = "Replace this usage of 'Stream.collect(Collectors.%s())' with 'Stream.toList()'";

    Tree tree;
    boolean isMutable;
    @CheckForNull
    Symbol assignedVariable;

    Issue(Tree tree, boolean isMutable, @CheckForNull Symbol assignedVariable) {
      this.tree = tree;
      this.isMutable = isMutable;
      this.assignedVariable = assignedVariable;
    }

    String message() {
      return String.format(MESSAGE, isMutable ? "toList" : "toUnmodifiableList");
    }
  }

  private static final MethodMatchers COLLECT = MethodMatchers.create()
    .ofSubTypes("java.util.stream.Stream")
    .names("collect")
    .withAnyParameters()
    .build();

  private static final MethodMatchers COLLECTORS_TO_LIST = MethodMatchers.create()
    .ofSubTypes("java.util.stream.Collectors")
    .names("toList")
    .addWithoutParametersMatcher()
    .build();

  private static final MethodMatchers COLLECTORS_TO_UNMODIFIABLE_LIST = MethodMatchers.create()
    .ofSubTypes("java.util.stream.Collectors")
    .names("toUnmodifiableList")
    .addWithoutParametersMatcher()
    .build();

  private static final MethodMatchers LIST_MODIFICATION_METHODS = MethodMatchers.create()
    .ofSubTypes("java.util.List")
    .names("add", "addAll", "remove", "removeAll", "replaceAll", "set", "sort", "clear")
    .withAnyParameters()
    .build();

  private final Set<Symbol> mutableLists = new HashSet<>();
  private final List<Issue> issues = new ArrayList<>();

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava16Compatible();
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(COLLECT, LIST_MODIFICATION_METHODS);
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.COMPILATION_UNIT, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.COMPILATION_UNIT)) {
      mutableLists.clear();
      issues.clear();
    }
    super.visitNode(tree);
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Tree.Kind.COMPILATION_UNIT)) {
      for (Issue issue : issues) {
        if (!issue.isMutable || issue.assignedVariable == null || !mutableLists.contains(issue.assignedVariable)) {
          reportIssue(issue.tree, issue.message());
        }
      }
    }
    super.leaveNode(tree);
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (COLLECT.matches(mit)) {
      boolean mutable;
      if (!mit.arguments().get(0).is(Tree.Kind.METHOD_INVOCATION)) return;
      MethodInvocationTree collector = (MethodInvocationTree) mit.arguments().get(0);
      if (COLLECTORS_TO_LIST.matches(collector)) {
        mutable = true;
      } else if (COLLECTORS_TO_UNMODIFIABLE_LIST.matches(collector)) {
        mutable = false;
      } else {
        return;
      }
      Symbol assignedVariable = findAssignedVariable(mit);
      issues.add(new Issue(collector, mutable, assignedVariable));
    } else if (mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) mit.methodSelect();
      if (memberSelect.expression().is(Tree.Kind.IDENTIFIER)) {
        mutableLists.add(((IdentifierTree) memberSelect.expression()).symbol());
      }
    }
  }

  @CheckForNull
  private static Symbol findAssignedVariable(MethodInvocationTree mit) {
    if (mit.parent().is(Tree.Kind.ASSIGNMENT)) {
      ExpressionTree variable = ((AssignmentExpressionTree) mit.parent()).variable();
      if (variable.is(Tree.Kind.IDENTIFIER)) {
        return ((IdentifierTree) variable).symbol();
      }
    }
    if (mit.parent().is(Tree.Kind.VARIABLE)) {
      return ((VariableTree) mit.parent()).symbol();
    }
    return null;
  }
}
