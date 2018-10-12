/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.java.checks.AtLeastOneConstructorCheck;
import org.sonar.java.checks.ConstantsShouldBeStaticFinalCheck;
import org.sonar.java.checks.EqualsNotOverriddenInSubclassCheck;
import org.sonar.java.checks.EqualsNotOverridenWithCompareToCheck;
import org.sonar.java.checks.PrivateFieldUsedLocallyCheck;
import org.sonar.java.checks.UtilityClassWithPublicConstructorCheck;
import org.sonar.java.checks.naming.BadFieldNameCheck;
import org.sonar.java.checks.unused.UnusedPrivateFieldCheck;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

public class LombokFilter extends BaseTreeVisitorIssueFilter {

  private static final Set<Class<? extends JavaCheck>> FILTERED_RULES = ImmutableSet.<Class<? extends JavaCheck>>of(
    UnusedPrivateFieldCheck.class,
    PrivateFieldUsedLocallyCheck.class,
    EqualsNotOverriddenInSubclassCheck.class,
    EqualsNotOverridenWithCompareToCheck.class,
    UtilityClassWithPublicConstructorCheck.class,
    AtLeastOneConstructorCheck.class,
    BadFieldNameCheck.class,
    ConstantsShouldBeStaticFinalCheck.class);

  private static final List<String> GENERATE_UNUSED_FIELD_RELATED_METHODS = ImmutableList.<String>builder()
    .add("lombok.Getter")
    .add("lombok.Setter")
    .add("lombok.Builder")
    .add("lombok.ToString")
    .add("lombok.AllArgsConstructor")
    .add("lombok.NoArgsConstructor")
    .add("lombok.RequiredArgsConstructor")
    .build();

  private static final List<String> GENERATE_CONSTRUCTOR = ImmutableList.<String>builder()
    .add("lombok.AllArgsConstructor")
    .add("lombok.NoArgsConstructor")
    .add("lombok.RequiredArgsConstructor")
    .build();

  private static final List<String> GENERATE_EQUALS = ImmutableList.<String>builder()
    .add("lombok.EqualsAndHashCode")
    .add("lombok.Data")
    .add("lombok.Value")
    .build();

  private static final List<String> UTILITY_CLASS = ImmutableList.<String>builder()
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
      excludeLines(tree, PrivateFieldUsedLocallyCheck.class);
    } else {
      acceptLines(tree, UnusedPrivateFieldCheck.class);
      acceptLines(tree, PrivateFieldUsedLocallyCheck.class);
    }

    if (usesAnnotation(tree, GENERATE_CONSTRUCTOR)) {
      excludeLines(tree, AtLeastOneConstructorCheck.class);
    } else {
      acceptLines(tree, AtLeastOneConstructorCheck.class);
    }

    if (generatesEquals) {
      excludeLines(tree, EqualsNotOverriddenInSubclassCheck.class);
      excludeLines(tree, EqualsNotOverridenWithCompareToCheck.class);
    } else {
      acceptLines(tree, EqualsNotOverriddenInSubclassCheck.class);
      acceptLines(tree, EqualsNotOverridenWithCompareToCheck.class);
    }

    if (generatesPrivateConstructor(tree)) {
      excludeLines(tree, UtilityClassWithPublicConstructorCheck.class);
    } else {
      acceptLines(tree, UtilityClassWithPublicConstructorCheck.class);
    }

    if (usesAnnotation(tree, UTILITY_CLASS)) {
      excludeLines(tree, BadFieldNameCheck.class);
      excludeLines(tree, ConstantsShouldBeStaticFinalCheck.class);
    } else {
      acceptLines(tree, BadFieldNameCheck.class);
      acceptLines(tree, ConstantsShouldBeStaticFinalCheck.class);
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

  private static boolean generatesPrivateConstructor(ClassTree classTree) {
    if (usesAnnotation(classTree, UTILITY_CLASS)) {
      return true;
    }
    SymbolMetadata metadata = classTree.symbol().metadata();
    return GENERATE_CONSTRUCTOR.stream()
      .map(metadata::valuesForAnnotation)
      .filter(Objects::nonNull)
      .anyMatch(LombokFilter::generatesPrivateAccess);
  }

  private static boolean generatesPrivateAccess(List<SymbolMetadata.AnnotationValue> values) {
    return values.stream().anyMatch(av -> "access".equals(av.name()) && "PRIVATE".equals(getAccessLevelValue(av.value())));
  }

  @Nullable
  private static String getAccessLevelValue(Object value) {
    if (value instanceof Tree) {
      Tree tree = (Tree) value;
      if (tree.is(Tree.Kind.MEMBER_SELECT)) {
        return ((MemberSelectExpressionTree) tree).identifier().name();
      } else if (tree.is(Tree.Kind.IDENTIFIER)) {
        return ((IdentifierTree) tree).name();
      }
    }
    // can not be anything else than a Tree, because we start from the syntax tree
    return null;
  }
}
