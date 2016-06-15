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

package org.sonar.java.checks.predicates;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.OngoingStubbing;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.TypeTree;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="mailto:krzysztof.suszynski@coi.gov.pl">Krzysztof Suszynski</a>
 * @since 07.06.16
 */
@RunWith(MockitoJUnitRunner.class)
public class VisibleForTestingPredicateTest {

  @Mock
  private AnnotationTree annotationTree;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private AnnotationTree secondAnnotationTree;

  @Mock
  private ModifiersTree modifiersTree;

  @Mock
  private TypeTree typeTree;

  @Mock
  private Type type;

  @Test
  public void testPositive() {
    // given
    List<AnnotationTree> annotations = ImmutableList.of(annotationTree);
    prepareMockingWithVisibleForTesting(true)
      .thenReturn(annotations);
    VisibleForTestingPredicate predicate = new VisibleForTestingPredicate();

    // when
    boolean result = predicate.test(modifiersTree);

    // then
    assertThat(result).isTrue();
    verify(type, times(1)).is(VisibleForTestingPredicate.GUAVA_FQCN);
  }

  @Test
  public void testNegative() {
    // given
    List<AnnotationTree> annotations = ImmutableList.of(annotationTree);
    prepareMockingWithVisibleForTesting(false)
      .thenReturn(annotations);
    VisibleForTestingPredicate predicate = new VisibleForTestingPredicate();

    // when
    boolean result = predicate.test(modifiersTree);

    // then
    assertThat(result).isFalse();
    verify(type, times(1)).is(VisibleForTestingPredicate.GUAVA_FQCN);
  }

  @Test
  public void testPositiveOnMultiple() {
    // given
    List<AnnotationTree> annotations = ImmutableList.of(
      secondAnnotationTree, annotationTree
    );
    prepareMockingWithVisibleForTesting(true)
      .thenReturn(annotations);
    VisibleForTestingPredicate predicate = new VisibleForTestingPredicate();

    // when
    boolean result = predicate.test(modifiersTree);

    // then
    assertThat(result).isTrue();
    verify(type, times(1)).is(VisibleForTestingPredicate.GUAVA_FQCN);
    verify(annotationTree, times(1)).annotationType();
    verify(secondAnnotationTree, times(1)).annotationType();
  }

  private OngoingStubbing<List<AnnotationTree>> prepareMockingWithVisibleForTesting(boolean isVisibleForTesting) {
    when(annotationTree.annotationType()).thenReturn(typeTree);
    when(typeTree.symbolType()).thenReturn(type);
    when(type.is(anyString())).thenReturn(isVisibleForTesting);
    return when(modifiersTree.annotations());
  }
}
