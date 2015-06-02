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
package org.sonar.java.symexec;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.sonar.java.checks.SubscriptionBaseVisitor;
import org.sonar.java.checks.methods.MethodInvocationMatcher;
import org.sonar.java.checks.methods.TypeCriteria;
import org.sonar.java.checks.verifier.JavaCheckVerifier;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SymbolicEvaluatorTest {

  @Test
  public void test() {
    JavaCheckVerifier.verify("src/test/files/symexec/SymbolicEvaluator.java", new Visitor());
  }

  private static class Visitor extends SubscriptionBaseVisitor {
    private static MethodInvocationMatcher PROBE_MATCHER = MethodInvocationMatcher.create()
      .name("probe");
    private static MethodInvocationMatcher RELATION_MATCHER = MethodInvocationMatcher.create()
      .name("dumpRelation")
      .addParameter("java.lang.Object")
      .addParameter("java.lang.Object");
    private static MethodInvocationMatcher STATE_MATCHER = MethodInvocationMatcher.create()
      .name("dumpState")
      .addParameter(TypeCriteria.anyType());
    private static MethodInvocationMatcher VALUE_MATCHER = MethodInvocationMatcher.create()
      .name("dumpValue")
      .addParameter(TypeCriteria.anyType());

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return ImmutableList.of(Tree.Kind.METHOD);
    }

    @Override
    public void visitNode(Tree tree) {
      Check check = new Check();
      SymbolicEvaluator.evaluateMethod(new ExecutionState(), (MethodTree) tree, check);
      for (Map.Entry<Tree, Integer> entry : check.probes.entrySet()) {
        addIssue(entry.getKey(), Integer.toString(entry.getValue()));
      }
      for (Map.Entry<Tree, StringBuilder> entry : check.relations.entrySet()) {
        addIssue(entry.getKey(), entry.getValue().toString());
      }
      for (Map.Entry<Tree, StringBuilder> entry : check.states.entrySet()) {
        addIssue(entry.getKey(), entry.getValue().toString());
      }
      for (Map.Entry<Tree, StringBuilder> entry : check.values.entrySet()) {
        addIssue(entry.getKey(), entry.getValue().toString());
      }
    }

    private class Check extends SymbolicExecutionCheck {
      private final Map<Tree, Integer> probes = new HashMap<>();
      private final Map<Tree, StringBuilder> relations = new HashMap<>();
      private final Map<Tree, StringBuilder> states = new HashMap<>();
      private final Map<Tree, StringBuilder> values = new HashMap<>();

      @Override
      protected void onExecutableElementInvocation(ExecutionState executionState, Tree tree, List<SymbolicValue> arguments) {
        if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
          if (PROBE_MATCHER.matches((MethodInvocationTree) tree)) {
            probe(tree);
          } else if (RELATION_MATCHER.matches((MethodInvocationTree) tree)) {
            dumpRelation(executionState, tree, arguments.get(0), arguments.get(1));
          } else if (STATE_MATCHER.matches((MethodInvocationTree) tree)) {
            dumpState(executionState, tree, arguments);
          } else if (VALUE_MATCHER.matches((MethodInvocationTree) tree)) {
            dumpValue(tree, arguments);
          }
        }
      }

      private void dumpRelation(ExecutionState executionState, Tree tree, SymbolicValue leftValue, SymbolicValue rightValue) {
        StringBuilder builder = relations.get(tree);
        if (builder == null) {
          builder = new StringBuilder();
          relations.put(tree, builder);
        } else {
          builder.append('/');
        }
        builder.append(executionState.getRelation(leftValue, rightValue));
      }

      private void dumpState(ExecutionState executionState, Tree tree, List<SymbolicValue> arguments) {
        StringBuilder builder = states.get(tree);
        if (builder == null) {
          builder = new StringBuilder();
          states.put(tree, builder);
        } else {
          builder.append('/');
        }
        for (int i = 0; i < arguments.size(); i += 1) {
          if (i != 0) {
            builder.append(",");
          }
          builder.append(executionState.getBooleanConstraint(arguments.get(i))).toString();
        }
      }

      private void dumpValue(Tree tree, List<SymbolicValue> arguments) {
        StringBuilder builder = values.get(tree);
        if (builder == null) {
          builder = new StringBuilder();
          values.put(tree, builder);
        } else {
          builder.append('/');
        }
        for (int i = 0; i < arguments.size(); i += 1) {
          if (i != 0) {
            builder.append(",");
          }
          builder.append(arguments.get(i).toString());
        }
      }

      private void probe(Tree tree) {
        Integer value = probes.get(tree);
        probes.put(tree, value != null ? value + 1 : 1);
      }
    }
  }

}
