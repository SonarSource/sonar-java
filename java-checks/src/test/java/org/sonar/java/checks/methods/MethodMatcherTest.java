/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.java.checks.methods;

import com.google.common.collect.ImmutableList;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.checks.SubscriptionBaseVisitor;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

public class MethodMatcherTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void should_fail_if_addParameter_is_called_after_withNoParameterConstraint() throws Exception {
    MethodMatcher matcher = MethodMatcher.create().name("name")
      .withNoParameterConstraint()
      .withNoParameterConstraint();
    exception.expect(IllegalStateException.class);
    matcher.addParameter("int");
  }

  @Test
  public void should_fail_if_withNoParameterConstraint_is_called_after_addParameter() throws Exception {
    MethodMatcher matcher = MethodMatcher.create().name("name").addParameter("int");
    exception.expect(IllegalStateException.class);
    matcher.withNoParameterConstraint();
  }

  @Test
  public void detected() {
    MethodMatcher objectToString = MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf("java.lang.Object")).name("toString");
    MethodMatcher integerToString = MethodMatcher.create().typeDefinition("java.lang.Integer").name("toString");

    Map<MethodMatcher, List<Integer>> matches = new HashMap<>();
    matches.put(objectToString, new ArrayList<Integer>());
    matches.put(integerToString, new ArrayList<Integer>());

    JavaAstScanner.scanSingleFileForTests(new File("src/test/files/checks/methodMatcher/Test.java"), new VisitorsBridge(new Visitor(matches)));

    assertThat(matches.get(objectToString)).containsExactly(6, 14);
    assertThat(matches.get(integerToString)).containsExactly(14);
  }

  class Visitor extends SubscriptionBaseVisitor {

    public Map<MethodMatcher, List<Integer>> matches;

    public Visitor(Map<MethodMatcher, List<Integer>> matches) {
      this.matches = matches;
    }

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return ImmutableList.of(Tree.Kind.METHOD, Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS);
    }

    @Override
    public void visitNode(Tree tree) {
      super.visitNode(tree);
      for (Map.Entry<MethodMatcher, List<Integer>> entry : matches.entrySet()) {
        boolean match  = false;
        MethodMatcher matcher = entry.getKey();
        if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
          match = matcher.matches((MethodInvocationTree) tree);
        } else if (tree.is(Tree.Kind.METHOD)) {
          match = matcher.matches((MethodTree) tree);
        } else if (tree.is(Tree.Kind.METHOD)) {
          match = matcher.matches((MethodTree) tree);
        }
        if (match) {
          entry.getValue().add(((JavaTree) tree).getLine());
        }
      }
    }
  }

}
