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
package org.sonar.java.matcher;

import com.google.common.collect.ImmutableList;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MethodMatcherTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void should_fail_if_addParameter_is_called_after_withAnyParameters() throws Exception {
    MethodMatcher matcher = MethodMatcher.create().name("name")
      .withAnyParameters();
    exception.expect(IllegalArgumentException.class);
    matcher.addParameter("int");
  }

  @Test
  public void should_fail_if_addParameter_is_called_after_withoutParameter() throws Exception {
    MethodMatcher matcher = MethodMatcher.create().name("name")
      .withoutParameter();
    exception.expect(IllegalArgumentException.class);
    matcher.addParameter("int");
  }

  @Test
  public void should_fail_if_withAnyParameters_is_called_after_addParameter() throws Exception {
    MethodMatcher matcher = MethodMatcher.create().name("name").addParameter("int");
    exception.expect(IllegalArgumentException.class);
    matcher.withAnyParameters();
  }

  @Test
  public void should_fail_if_withAnyParameters_is_called_after_withoutParameter() throws Exception {
    MethodMatcher matcher = MethodMatcher.create().name("name")
        .withoutParameter();
    exception.expect(IllegalArgumentException.class);
    matcher.withAnyParameters();
  }

  @Test
  public void should_fail_if_withAnyParameters_is_called_after_empty_parameters() throws Exception {
    MethodMatcher matcher = MethodMatcher.create().name("name").parameters(new String[0]);
    exception.expect(IllegalArgumentException.class);
    matcher.withAnyParameters();
  }

  @Test
  public void should_fail_if_withAnyParameters_is_called_after_parameters() throws Exception {
    MethodMatcher matcher = MethodMatcher.create().name("name").parameters("int", "int");
    exception.expect(IllegalArgumentException.class);
    matcher.withAnyParameters();
  }

  @Test
  public void should_fail_if_withoutParameter_is_called_after_parameters() throws Exception {
    MethodMatcher matcher = MethodMatcher.create().name("name").parameters("int", "int");
    exception.expect(IllegalArgumentException.class);
    matcher.withoutParameter();
  }

  @Test
  public void should_fail_if_withAnyParameters_is_called_after_empty_parameters_TypeCriteria() throws Exception {
    MethodMatcher matcher = MethodMatcher.create().name("name").parameters(new TypeCriteria[0]);
    exception.expect(IllegalArgumentException.class);
    matcher.withAnyParameters();
  }

  @Test
  public void should_fail_if_withAnyParameters_is_called_after_parameters_TypeCriteria() throws Exception {
    MethodMatcher matcher = MethodMatcher.create().name("name").parameters(TypeCriteria.is("int"));
    exception.expect(IllegalArgumentException.class);
    matcher.withAnyParameters();
  }

  @Test
  public void should_fail_if_name_called_twice() throws Exception {
    MethodMatcher matcher = MethodMatcher.create().name("name");
    exception.expect(IllegalArgumentException.class);
    matcher.name("otherName");
  }

  @Test
  public void should_fail_if_name_criteria_called_twice() throws Exception {
    MethodMatcher matcher = MethodMatcher.create().name(NameCriteria.is("name"));
    exception.expect(IllegalArgumentException.class);
    matcher.name(NameCriteria.any());
  }

  @Test
  public void should_fail_if_typeDefinition_called_twice() throws Exception {
    MethodMatcher matcher = MethodMatcher.create().typeDefinition("int");
    exception.expect(IllegalArgumentException.class);
    matcher.typeDefinition("long");
  }

  @Test
  public void should_fail_if_typeDefinition_criteria_called_twice() throws Exception {
    MethodMatcher matcher = MethodMatcher.create().typeDefinition(TypeCriteria.is("int"));
    exception.expect(IllegalArgumentException.class);
    matcher.typeDefinition(TypeCriteria.anyType());
  }

  @Test
  public void should_fail_if_parameters_are_not_defined() throws Exception {
    MethodMatcher matcher = MethodMatcher.create().name("toString");
    MethodTree tree = methodTreeMock("toString", mock(Symbol.TypeSymbol.class));
    exception.expect(IllegalArgumentException.class);
    matcher.matches(tree);
  }

  private MethodTree methodTreeMock(String methodName, @Nullable Symbol.TypeSymbol enclosingClass) {
    Symbol.MethodSymbol methodSymbol = mock(Symbol.MethodSymbol.class);
    when(methodSymbol.isMethodSymbol()).thenReturn(true);
    when(methodSymbol.name()).thenReturn(methodName);
    when(methodSymbol.enclosingClass()).thenReturn(enclosingClass);

    MethodTree tree = mock(MethodTree.class);
    when(tree.symbol()).thenReturn(methodSymbol);
    return tree;
  }

  @Test
  public void should_fail_if_name_is_not_defined() throws Exception {
    MethodMatcher matcher = MethodMatcher.create().withoutParameter();
    MethodTree tree = methodTreeMock("toString", mock(Symbol.TypeSymbol.class));
    exception.expect(IllegalArgumentException.class);
    matcher.matches(tree);
  }

  @Test
  public void does_not_match_without_enclosingClass() throws Exception {
    MethodMatcher matcher = MethodMatcher.create().name("toString").withoutParameter();
    Symbol.MethodSymbol symbol = mock(Symbol.MethodSymbol.class);
    when(symbol.enclosingClass()).thenReturn(null);

    MethodTree tree = mock(MethodTree.class);
    when(tree.symbol()).thenReturn(symbol);

    assertThat(matcher.matches(tree)).isFalse();
  }

  @Test
  public void does_not_match_without_callSite_enclosingClass() throws Exception {
    MethodMatcher matcher = MethodMatcher.create().name(NameCriteria.any()).withAnyParameters().callSite(TypeCriteria.anyType());
    Symbol symbol = mock(Symbol.class);
    when(symbol.enclosingClass()).thenReturn(null);

    IdentifierTree id = mock(IdentifierTree.class);
    when(id.is(Tree.Kind.IDENTIFIER)).thenReturn(true);
    when(id.symbol()).thenReturn(symbol);

    MethodInvocationTree tree = mock(MethodInvocationTree.class);
    when(tree.methodSelect()).thenReturn(id);
    assertThat(matcher.matches(tree)).isFalse();
  }

  @Test
  public void detected() {
    MethodMatcher objectToString = MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf("java.lang.Object")).name("toString").withoutParameter();
    MethodMatcher objectToStringWithIntParam = MethodMatcher.create().name("toString").parameters("int");
    MethodMatcher objectToStringWithStringParam = MethodMatcher.create().typeDefinition(TypeCriteria.anyType()).name(NameCriteria.is("toString")).parameters("java.lang.String");
    MethodMatcher objectToStringWithAnyParam = MethodMatcher.create().typeDefinition(TypeCriteria.is("Test")).name("toString").withAnyParameters();
    MethodMatcher integerToString = MethodMatcher.create().typeDefinition("java.lang.Integer").name("toString").withoutParameter();
    MethodMatcher callSiteIsTest = MethodMatcher.create().typeDefinition(TypeCriteria.anyType()).name(NameCriteria.any()).withAnyParameters().callSite(TypeCriteria.is("Test"));

    Map<MethodMatcher, List<Integer>> matches = new HashMap<>();
    matches.put(objectToString, new ArrayList<>());
    matches.put(objectToStringWithIntParam, new ArrayList<>());
    matches.put(objectToStringWithStringParam, new ArrayList<>());
    matches.put(objectToStringWithAnyParam, new ArrayList<>());
    matches.put(integerToString, new ArrayList<>());
    matches.put(callSiteIsTest, new ArrayList<>());

    JavaAstScanner.scanSingleFileForTests(new File("src/test/files/matcher/Test.java"), new VisitorsBridge(new Visitor(matches)));

    assertThat(matches.get(objectToString)).containsExactly(6, 19, 27);
    assertThat(matches.get(objectToStringWithIntParam)).containsExactly(10);
    assertThat(matches.get(objectToStringWithStringParam)).containsExactly(11, 14);
    assertThat(matches.get(objectToStringWithAnyParam)).containsExactly(6, 10, 11, 14);
    assertThat(matches.get(integerToString)).containsExactly(19);
    assertThat(matches.get(callSiteIsTest)).containsExactly(6, 10, 11, 14, 18, 22);
  }

  class Visitor extends SubscriptionVisitor {

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
        boolean match = false;
        MethodMatcher matcher = entry.getKey();
        if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
          match = matcher.matches((MethodInvocationTree) tree);
        } else if (tree.is(Tree.Kind.METHOD)) {
          match = matcher.matches((MethodTree) tree);
        } else if (tree.is(Tree.Kind.NEW_CLASS)) {
          match = matcher.matches((NewClassTree) tree);
        }
        if (match) {
          entry.getValue().add(((JavaTree) tree).getLine());
        }
      }
    }
  }

}
