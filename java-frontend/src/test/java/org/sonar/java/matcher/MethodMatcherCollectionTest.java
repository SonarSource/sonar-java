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

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MethodMatcherCollectionTest {

  @Test
  public void should_create_a_collection() {
    assertThat(MethodMatcherCollection.create()).isNotNull();
  }

  @Test
  public void should_create_a_collection_with_MethodInvocationMatcher() {
    assertThat(MethodMatcherCollection.create(MethodMatcher.create())).isNotNull();
  }

  @Test
  public void should_be_able_to_add_MethodInvocationMatcher() {
    assertThat(MethodMatcherCollection.create().add(MethodMatcher.create())).isNotNull();
  }

  @Test
  public void should_not_match_if_there_is_no_matcher() {
    assertThat(MethodMatcherCollection.create().anyMatch(mock(MethodInvocationTree.class))).isFalse();
  }

  @Test
  public void should_not_match_when_method_invocation_tree_does_not_match() {
    MethodMatcher matcher = mock(MethodMatcher.class);
    when(matcher.matches(any(MethodInvocationTree.class))).thenReturn(false);
    assertThat(MethodMatcherCollection.create(matcher).anyMatch(mock(MethodInvocationTree.class))).isFalse();
    assertThat(MethodMatcherCollection.create(matcher).anyMatch(mock(MethodTree.class))).isFalse();
  }

  @Test
  public void should_match_if_any_of_the_matchers_match() {
    MethodMatcher matcher1 = mock(MethodMatcher.class);
    when(matcher1.matches(any(MethodInvocationTree.class))).thenReturn(false);
    MethodMatcher matcher2 = mock(MethodMatcher.class);
    when(matcher2.matches(any(MethodInvocationTree.class))).thenReturn(true);
    assertThat(MethodMatcherCollection.create(matcher1, matcher2).anyMatch(mock(MethodInvocationTree.class))).isTrue();
    assertThat(MethodMatcherCollection.create(matcher1).anyMatch(mock(MethodInvocationTree.class))).isFalse();

    matcher1 = mock(MethodMatcher.class);
    when(matcher1.matches(any(MethodTree.class))).thenReturn(false);
    matcher2 = mock(MethodMatcher.class);
    when(matcher2.matches(any(MethodTree.class))).thenReturn(true);
    assertThat(MethodMatcherCollection.create(matcher1, matcher2).anyMatch(mock(MethodTree.class))).isTrue();
    assertThat(MethodMatcherCollection.create(matcher1).anyMatch(mock(MethodTree.class))).isFalse();

    matcher1 = mock(MethodMatcher.class);
    when(matcher1.matches(any(NewClassTree.class))).thenReturn(false);
    matcher2 = mock(MethodMatcher.class);
    when(matcher2.matches(any(NewClassTree.class))).thenReturn(true);
    assertThat(MethodMatcherCollection.create(matcher1, matcher2).anyMatch(mock(NewClassTree.class))).isTrue();
    assertThat(MethodMatcherCollection.create(matcher1).anyMatch(mock(NewClassTree.class))).isFalse();

    matcher1 = mock(MethodMatcher.class);
    when(matcher1.matches(any(MethodReferenceTree.class))).thenReturn(false);
    matcher2 = mock(MethodMatcher.class);
    when(matcher2.matches(any(MethodReferenceTree.class))).thenReturn(true);
    assertThat(MethodMatcherCollection.create(matcher1, matcher2).anyMatch(mock(MethodReferenceTree.class))).isTrue();
    assertThat(MethodMatcherCollection.create(matcher1).anyMatch(mock(MethodReferenceTree.class))).isFalse();
  }

  @Test
  public void should_not_call_matchers_if_symbol_is_not_method_symbol() {
    Symbol mockSymbol = mock(Symbol.class);
    when(mockSymbol.isMethodSymbol()).thenReturn(false);
    MethodMatcher mockMatcher = mock(MethodMatcher.class);
    assertThat(MethodMatcherCollection.create(mockMatcher).anyMatch(mockSymbol)).isFalse();
    verify(mockMatcher, never()).matches(mockSymbol);
  }

  @Test
  public void should_match_if_any_of_the_matchers_match_symbol() {
    Symbol mockSymbol = mock(Symbol.class);
    when(mockSymbol.isMethodSymbol()).thenReturn(true);
    MethodMatcher matcher1 = mock(MethodMatcher.class);
    when(matcher1.matches(any(Symbol.class))).thenReturn(false);
    MethodMatcher matcher2 = mock(MethodMatcher.class);
    when(matcher2.matches(any(Symbol.class))).thenReturn(true);
    assertThat(MethodMatcherCollection.create(matcher1, matcher2).anyMatch(mockSymbol)).isTrue();

    when(matcher1.matches(any(Symbol.class))).thenReturn(false);
    when(matcher2.matches(any(Symbol.class))).thenReturn(false);
    assertThat(MethodMatcherCollection.create(matcher1, matcher2).anyMatch(mockSymbol)).isFalse();
  }

  @Test
  public void should_add_all_matchers() throws Exception {
    MethodMatcher matcher1 = mock(MethodMatcher.class);
    when(matcher1.matches(any(MethodTree.class))).thenReturn(false);
    MethodMatcher matcher2 = mock(MethodMatcher.class);
    when(matcher2.matches(any(MethodTree.class))).thenReturn(true);
    MethodMatcherCollection mmc = MethodMatcherCollection.create();
    mmc.addAll(Collections.singletonList(matcher1));
    assertThat(mmc.anyMatch(mock(MethodTree.class))).isFalse();
    mmc.addAll(ImmutableList.of(matcher1, matcher2));
    assertThat(mmc.anyMatch(mock(MethodTree.class))).isTrue();
  }
}
