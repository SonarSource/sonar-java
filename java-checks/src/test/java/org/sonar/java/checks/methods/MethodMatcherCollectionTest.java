/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonar.java.checks.methods;

import org.junit.Test;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MethodMatcherCollectionTest {

  @Test
  public void should_create_a_collection() {
    assertThat(MethodInvocationMatcherCollection.create()).isNotNull();
  }

  @Test
  public void should_create_a_collection_with_MethodInvocationMatcher() {
    assertThat(MethodInvocationMatcherCollection.create(MethodMatcher.create())).isNotNull();
  }

  @Test
  public void should_be_able_to_add_MethodInvocationMatcher() {
    assertThat(MethodInvocationMatcherCollection.create().add(MethodMatcher.create())).isNotNull();
  }

  @Test
  public void should_not_match_if_there_is_no_matcher() {
    assertThat(MethodInvocationMatcherCollection.create().anyMatch(mock(MethodInvocationTree.class))).isFalse();
  }

  @Test
  public void should_not_match_when_method_invocation_tree_does_not_match() {
    MethodMatcher matcher = mock(MethodMatcher.class);
    when(matcher.matches(any(MethodInvocationTree.class))).thenReturn(false);
    assertThat(MethodInvocationMatcherCollection.create(matcher).anyMatch(mock(MethodInvocationTree.class))).isFalse();
    assertThat(MethodInvocationMatcherCollection.create(matcher).anyMatch(mock(MethodTree.class))).isFalse();
  }

  @Test
  public void should_match_if_any_of_the_matchers_match() {
    MethodMatcher matcher1 = mock(MethodMatcher.class);
    when(matcher1.matches(any(MethodInvocationTree.class))).thenReturn(false);
    MethodMatcher matcher2 = mock(MethodMatcher.class);
    when(matcher2.matches(any(MethodInvocationTree.class))).thenReturn(true);
    assertThat(MethodInvocationMatcherCollection.create(matcher1, matcher2).anyMatch(mock(MethodInvocationTree.class))).isTrue();

    matcher1 = mock(MethodMatcher.class);
    when(matcher1.matches(any(MethodTree.class))).thenReturn(false);
    matcher2 = mock(MethodMatcher.class);
    when(matcher2.matches(any(MethodTree.class))).thenReturn(true);
    assertThat(MethodInvocationMatcherCollection.create(matcher1, matcher2).anyMatch(mock(MethodTree.class))).isTrue();
  }
}
