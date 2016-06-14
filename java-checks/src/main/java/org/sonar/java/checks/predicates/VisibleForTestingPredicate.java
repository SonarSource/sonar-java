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

import com.google.common.annotations.VisibleForTesting;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * This is a predicate that finds if given modifiers tree contains {@link VisibleForTesting} annotation.
 *
 * @author <a href="mailto:krzysztof.suszynski@coi.gov.pl">Krzysztof Suszynski</a>
 * @since 07.06.16
 */
public class VisibleForTestingPredicate implements Predicate<ModifiersTree> {

  public static final String GUAVA_FQCN = VisibleForTesting.class.getName();

  @Override
  public boolean test(ModifiersTree modifierTrees) {
    return isVisibleForTesting(modifierTrees);
  }

  private static boolean isVisibleForTesting(ModifiersTree modifiers) {
    Collection<AnnotationTree> annotations = modifiers.annotations();
    Optional<AnnotationTree> found = annotations.stream()
      .filter(VisibleForTestingPredicate::isVisibleForTesting)
      .findFirst();
    return found.isPresent();
  }

  private static boolean isVisibleForTesting(AnnotationTree annotationTree) {
    return annotationTree.annotationType()
      .symbolType()
      .is(GUAVA_FQCN);
  }
}
