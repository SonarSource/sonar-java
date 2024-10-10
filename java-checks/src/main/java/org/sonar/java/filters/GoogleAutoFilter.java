/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.List;
import java.util.Set;
import org.sonar.java.checks.AbstractClassNoFieldShouldBeInterfaceCheck;
import org.sonar.java.checks.EqualsNotOverriddenWithCompareToCheck;
import org.sonar.java.checks.EqualsOverriddenWithHashCodeCheck;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.ClassTree;

public class GoogleAutoFilter extends BaseTreeVisitorIssueFilter {

  private static final Set<Class<? extends JavaCheck>> FILTERED_RULES = Set.of(
    EqualsOverriddenWithHashCodeCheck.class,
    EqualsNotOverriddenWithCompareToCheck.class,
    AbstractClassNoFieldShouldBeInterfaceCheck.class);

  private static final String AUTO_VALUE_ANNOTATION = "com.google.auto.value.AutoValue";

  private static final List<String> AUTO_ANNOTATIONS = List.of(
    "com.google.auto.value.AutoValue$Builder",
    "com.google.auto.value.AutoOneOf");

  @Override
  public Set<Class<? extends JavaCheck>> filteredRules() {
    return FILTERED_RULES;
  }

  @Override
  public void visitClass(ClassTree tree) {
    SymbolMetadata classMetadata = tree.symbol().metadata();

    boolean isAnnotatedWithAutoValue = classMetadata.isAnnotatedWith(AUTO_VALUE_ANNOTATION);
    excludeLinesIfTrue(isAnnotatedWithAutoValue,
      tree, EqualsOverriddenWithHashCodeCheck.class, EqualsNotOverriddenWithCompareToCheck.class);
    excludeLinesIfTrue(isAnnotatedWithAutoValue || AUTO_ANNOTATIONS.stream().anyMatch(classMetadata::isAnnotatedWith),
      tree.simpleName(), AbstractClassNoFieldShouldBeInterfaceCheck.class);

    super.visitClass(tree);
  }

}
