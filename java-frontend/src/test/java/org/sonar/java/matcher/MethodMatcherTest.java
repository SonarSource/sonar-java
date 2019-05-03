/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.java.TestUtils;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.java.se.JavaCheckVerifier;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

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
    exception.expect(IllegalStateException.class);
    matcher.addParameter("int");
  }

  @Test
  public void should_fail_if_addParameter_is_called_after_withoutParameter() throws Exception {
    MethodMatcher matcher = MethodMatcher.create().name("name")
      .withoutParameter();
    exception.expect(IllegalStateException.class);
    matcher.addParameter("int");
  }

  @Test
  public void should_fail_if_withAnyParameters_is_called_after_addParameter() throws Exception {
    MethodMatcher matcher = MethodMatcher.create().name("name").addParameter("int");
    exception.expect(IllegalStateException.class);
    matcher.withAnyParameters();
  }

  @Test
  public void should_fail_if_withAnyParameters_is_called_after_withoutParameter() throws Exception {
    MethodMatcher matcher = MethodMatcher.create().name("name")
        .withoutParameter();
    exception.expect(IllegalStateException.class);
    matcher.withAnyParameters();
  }

  @Test
  public void should_fail_if_withAnyParameters_is_called_after_empty_parameters() throws Exception {
    MethodMatcher matcher = MethodMatcher.create().name("name").parameters(new String[0]);
    exception.expect(IllegalStateException.class);
    matcher.withAnyParameters();
  }

  @Test
  public void should_fail_if_withAnyParameters_is_called_after_parameters() throws Exception {
    MethodMatcher matcher = MethodMatcher.create().name("name").parameters("int", "int");
    exception.expect(IllegalStateException.class);
    matcher.withAnyParameters();
  }

  @Test
  public void should_fail_if_withoutParameter_is_called_after_parameters() throws Exception {
    MethodMatcher matcher = MethodMatcher.create().name("name").parameters("int", "int");
    exception.expect(IllegalStateException.class);
    matcher.withoutParameter();
  }

  @Test
  public void should_fail_if_withAnyParameters_is_called_after_empty_parameters_TypeCriteria() throws Exception {
    MethodMatcher matcher = MethodMatcher.create().name("name").parameters(new TypeCriteria[0]);
    exception.expect(IllegalStateException.class);
    matcher.withAnyParameters();
  }

  @Test
  public void should_fail_if_withAnyParameters_is_called_after_parameters_TypeCriteria() throws Exception {
    MethodMatcher matcher = MethodMatcher.create().name("name").parameters(TypeCriteria.is("int"));
    exception.expect(IllegalStateException.class);
    matcher.withAnyParameters();
  }

  @Test
  public void should_fail_if_name_called_twice() throws Exception {
    MethodMatcher matcher = MethodMatcher.create().name("name");
    exception.expect(IllegalStateException.class);
    matcher.name("otherName");
  }

  @Test
  public void should_fail_if_name_criteria_called_twice() throws Exception {
    MethodMatcher matcher = MethodMatcher.create().name(NameCriteria.is("name"));
    exception.expect(IllegalStateException.class);
    matcher.name(NameCriteria.any());
  }

  @Test
  public void should_fail_if_typeDefinition_called_twice() throws Exception {
    MethodMatcher matcher = MethodMatcher.create().typeDefinition("int");
    exception.expect(IllegalStateException.class);
    matcher.typeDefinition("long");
  }

  @Test
  public void should_fail_if_typeDefinition_criteria_called_twice() throws Exception {
    MethodMatcher matcher = MethodMatcher.create().typeDefinition(TypeCriteria.is("int"));
    exception.expect(IllegalStateException.class);
    matcher.typeDefinition(TypeCriteria.anyType());
  }

  @Test
  public void should_fail_if_parameters_are_not_defined() throws Exception {
    MethodMatcher matcher = MethodMatcher.create().name("toString");
    MethodTree tree = methodTreeMock("toString", mock(Symbol.TypeSymbol.class));
    exception.expect(IllegalStateException.class);
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
    exception.expect(IllegalStateException.class);
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
    MethodMatcher foo = MethodMatcher.create().typeDefinition(TypeCriteria.anyType()).name("foo").withoutParameter();
    MethodMatcher callSiteIsTest = MethodMatcher.create().typeDefinition(TypeCriteria.anyType()).name(NameCriteria.any()).withAnyParameters().callSite(TypeCriteria.is("Test"));

    Map<MethodMatcher, List<Integer>> matches = new HashMap<>();
    matches.put(objectToString, new ArrayList<>());
    matches.put(objectToStringWithIntParam, new ArrayList<>());
    matches.put(objectToStringWithStringParam, new ArrayList<>());
    matches.put(objectToStringWithAnyParam, new ArrayList<>());
    matches.put(integerToString, new ArrayList<>());
    matches.put(foo, new ArrayList<>());
    matches.put(callSiteIsTest, new ArrayList<>());

    JavaAstScanner.scanSingleFileForTests(
      TestUtils.inputFile("src/test/files/matcher/Test.java"),
      new VisitorsBridge(Collections.singletonList(new Visitor(matches)), new ArrayList<>(), null));

    assertThat(matches.get(objectToString)).containsExactly(6, 19, 27, 39, 40);
    assertThat(matches.get(objectToStringWithIntParam)).containsExactly(10);
    assertThat(matches.get(objectToStringWithStringParam)).containsExactly(11, 14);
    assertThat(matches.get(objectToStringWithAnyParam)).containsExactly(6, 10, 11, 14, 39);
    assertThat(matches.get(integerToString)).containsExactly(19);
    assertThat(matches.get(foo)).containsExactly(35, 36);
    assertThat(matches.get(callSiteIsTest)).containsExactly(6, 10, 11, 14, 18, 22, 38, 39);
  }

  @Test
  public void test_copy() throws Exception {
    MethodMatcher vanilla = MethodMatcher.create().typeDefinition("Test").name("f").withoutParameter();
    MethodMatcher copyInt = vanilla.copy().addParameter("int");
    MethodMatcher copyString = vanilla.copy().addParameter("java.lang.String");
    Map<MethodMatcher, List<Integer>> matches = new HashMap<>();
    matches.put(vanilla, new ArrayList<>());
    matches.put(copyInt, new ArrayList<>());
    matches.put(copyString, new ArrayList<>());
    JavaCheckVerifier.verifyNoIssue("src/test/files/matcher/Copy.java", new IssuableSubscriptionVisitor() {
      @Override
      public List<Tree.Kind> nodesToVisit() {
        return Collections.singletonList(Tree.Kind.METHOD);
      }

      @Override
      public void visitNode(Tree tree) {
        MethodTree methodTree = (MethodTree) tree;
        matches.forEach((matcher, list) -> {
          if (matcher.matches(methodTree)) {
            list.add(methodTree.firstToken().line());
          }
        });
      }
    });
    assertThat(matches.get(vanilla)).containsExactly(3);
    assertThat(matches.get(copyInt)).containsExactly(5);
    assertThat(matches.get(copyString)).containsExactly(7);
  }

  class Visitor extends SubscriptionVisitor {

    public Map<MethodMatcher, List<Integer>> matches;

    public Visitor(Map<MethodMatcher, List<Integer>> matches) {
      this.matches = matches;
    }

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Arrays.asList(Tree.Kind.METHOD, Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS, Tree.Kind.METHOD_REFERENCE);
    }

    @Override
    public void visitNode(Tree tree) {
      super.visitNode(tree);
      for (Map.Entry<MethodMatcher, List<Integer>> entry : matches.entrySet()) {
        boolean match = false;
        MethodMatcher matcher = entry.getKey();
        if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
          MethodInvocationTree mit = (MethodInvocationTree) tree;
          Symbol symbol = mit.symbol();
          if ("foo".equals(symbol.name())) {
            // only 'foo' is tested with symbol
            match = matcher.matches(symbol);
          } else {
            match = matcher.matches(mit);
          }
        } else if (tree.is(Tree.Kind.METHOD)) {
          MethodTree methodTree = (MethodTree) tree;
          Symbol.MethodSymbol symbol = methodTree.symbol();
          if ("foo".equals(symbol.name())) {
            // only 'foo' is tested with symbol
            match = matcher.matches(symbol);
          } else {
            match = matcher.matches(methodTree);
          }
        } else if (tree.is(Tree.Kind.NEW_CLASS)) {
          match = matcher.matches((NewClassTree) tree);
        } else if (tree.is(Tree.Kind.METHOD_REFERENCE)) {
          match = matcher.matches((MethodReferenceTree) tree);
        }
        if (match) {
          entry.getValue().add(((JavaTree) tree).getLine());
        }
      }
    }
  }

}
