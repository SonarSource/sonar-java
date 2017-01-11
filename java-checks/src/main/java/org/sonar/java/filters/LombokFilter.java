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
package org.sonar.java.filters;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.sonar.java.checks.EqualsNotOverriddenInSubclassCheck;
import org.sonar.java.checks.EqualsNotOverridenWithCompareToCheck;
import org.sonar.java.checks.UtilityClassWithPublicConstructorCheck;
import org.sonar.java.checks.unused.UnusedPrivateFieldCheck;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.ClassTree;

import java.util.List;
import java.util.Set;

public class LombokFilter extends BaseTreeVisitorIssueFilter {

  private static final Set<Class<? extends JavaCheck>> FILTERED_RULES = ImmutableSet.<Class<? extends JavaCheck>>of(
    UnusedPrivateFieldCheck.class,
    EqualsNotOverriddenInSubclassCheck.class,
    EqualsNotOverridenWithCompareToCheck.class,
    UtilityClassWithPublicConstructorCheck.class);

  private static final List<String> GENERATE_UNUSED_FIELD_RELATED_METHODS = ImmutableList.<String>builder()
    .add("lombok.Getter")
    .add("lombok.Setter")
    .add("lombok.Builder")
    .add("lombok.ToString")
    .add("lombok.AllArgsConstructor")
    .add("lombok.NoArgsConstructor")
    .add("lombok.RequiredArgsConstructor")
    .build();

  private static final List<String> GENERATE_EQUALS = ImmutableList.<String>builder()
    .add("lombok.EqualsAndHashCode")
    .add("lombok.Data")
    .add("lombok.Value")
    .build();

  private static final List<String> GENERATE_PRIVATE_CONSTRUCTOR = ImmutableList.<String>builder()
    .add("lombok.experimental.UtilityClass")
    .build();

  @Override
  public Set<Class<? extends JavaCheck>> filteredRules() {
    return FILTERED_RULES;
  }

  @Override
  public void visitClass(ClassTree tree) {
    boolean generatesEquals = usesAnnotation(tree, GENERATE_EQUALS);

    if (generatesEquals || usesAnnotation(tree, GENERATE_UNUSED_FIELD_RELATED_METHODS)) {
      excludeLines(tree, UnusedPrivateFieldCheck.class);
    } else {
      acceptLines(tree, UnusedPrivateFieldCheck.class);
    }

    if (generatesEquals) {
      excludeLines(tree, EqualsNotOverriddenInSubclassCheck.class);
      excludeLines(tree, EqualsNotOverridenWithCompareToCheck.class);
    } else {
      acceptLines(tree, EqualsNotOverriddenInSubclassCheck.class);
      acceptLines(tree, EqualsNotOverridenWithCompareToCheck.class);
    }

    if (usesAnnotation(tree, GENERATE_PRIVATE_CONSTRUCTOR)) {
      excludeLines(tree, UtilityClassWithPublicConstructorCheck.class);
    } else {
      acceptLines(tree, UtilityClassWithPublicConstructorCheck.class);
    }

    super.visitClass(tree);
  }

  private static boolean usesAnnotation(ClassTree classTree, List<String> annotations) {
    SymbolMetadata metadata = classTree.symbol().metadata();
    for (String annotation : annotations) {
      if (metadata.isAnnotatedWith(annotation)) {
        return true;
      }
    }
    return false;
  }
}
