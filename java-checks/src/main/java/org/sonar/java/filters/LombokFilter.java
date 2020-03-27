/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.java.checks.AtLeastOneConstructorCheck;
import org.sonar.java.checks.CollectionInappropriateCallsCheck;
import org.sonar.java.checks.ConstantsShouldBeStaticFinalCheck;
import org.sonar.java.checks.EqualsNotOverriddenInSubclassCheck;
import org.sonar.java.checks.EqualsNotOverridenWithCompareToCheck;
import org.sonar.java.checks.ExceptionsShouldBeImmutableCheck;
import org.sonar.java.checks.FieldModifierCheck;
import org.sonar.java.checks.PrivateFieldUsedLocallyCheck;
import org.sonar.java.checks.SillyEqualsCheck;
import org.sonar.java.checks.UselessImportCheck;
import org.sonar.java.checks.UtilityClassWithPublicConstructorCheck;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.naming.BadFieldNameCheck;
import org.sonar.java.checks.unused.UnusedPrivateFieldCheck;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

public class LombokFilter extends BaseTreeVisitorIssueFilter {

  private static final Set<Class<? extends JavaCheck>> FILTERED_RULES = ImmutableSet.<Class<? extends JavaCheck>>of(
    UnusedPrivateFieldCheck.class,
    PrivateFieldUsedLocallyCheck.class,
    EqualsNotOverriddenInSubclassCheck.class,
    EqualsNotOverridenWithCompareToCheck.class,
    UtilityClassWithPublicConstructorCheck.class,
    AtLeastOneConstructorCheck.class,
    BadFieldNameCheck.class,
    ConstantsShouldBeStaticFinalCheck.class,
    SillyEqualsCheck.class,
    CollectionInappropriateCallsCheck.class,
    UselessImportCheck.class,
    FieldModifierCheck.class,
    ExceptionsShouldBeImmutableCheck.class);

  private static final String LOMBOK_VAL = "lombok.val";
  private static final String LOMBOK_VALUE = "lombok.Value";
  private static final String LOMBOK_FIELD_DEFAULTS = "lombok.experimental.FieldDefaults";

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
    .add(LOMBOK_VALUE)
    .build();

  private static final List<String> UTILITY_CLASS = ImmutableList.<String>builder()
    .add("lombok.experimental.UtilityClass")
    .build();

  private static final List<String> NON_FINAL = ImmutableList.<String>builder()
    .add("lombok.experimental.NonFinal")
    .build();

  @Override
  public Set<Class<? extends JavaCheck>> filteredRules() {
    return FILTERED_RULES;
  }

  @Override
  public void visitImport(ImportTree tree) {
    String fullyQualifiedName = ExpressionsHelper.concatenate((ExpressionTree) tree.qualifiedIdentifier());

    excludeLinesIfTrue(fullyQualifiedName.startsWith("lombok."), tree, UselessImportCheck.class);

    super.visitImport(tree);
  }

  @Override
  public void visitClass(ClassTree tree) {
    boolean generatesEquals = usesAnnotation(tree, GENERATE_EQUALS);

    excludeLinesIfTrue(generatesEquals || usesAnnotation(tree, GENERATE_UNUSED_FIELD_RELATED_METHODS), tree, UnusedPrivateFieldCheck.class, PrivateFieldUsedLocallyCheck.class);
    excludeLinesIfTrue(usesAnnotation(tree, GENERATE_CONSTRUCTOR), tree, AtLeastOneConstructorCheck.class);
    excludeLinesIfTrue(generatesEquals, tree, EqualsNotOverriddenInSubclassCheck.class, EqualsNotOverridenWithCompareToCheck.class);
    excludeLinesIfTrue(generatesPrivateConstructor(tree), tree, UtilityClassWithPublicConstructorCheck.class);
    excludeLinesIfTrue(usesAnnotation(tree, UTILITY_CLASS), tree, BadFieldNameCheck.class, ConstantsShouldBeStaticFinalCheck.class);

    if (generatesPrivateFields(tree)) {
      tree.members().stream()
        .filter(t -> t.is(Tree.Kind.VARIABLE))
        .map(VariableTree.class::cast)
        .filter(v -> !generatesPackagePrivateAccess(v))
        .forEach(v -> excludeLines(v, FieldModifierCheck.class));
    }

    if (generatesFinalFields(tree)) {
      tree.members().stream()
        .filter(t -> t.is(Tree.Kind.VARIABLE))
        .map(VariableTree.class::cast)
        .filter(v -> !generatesNonFinal(v))
        .forEach(v -> excludeLines(v, ExceptionsShouldBeImmutableCheck.class));
    }

    super.visitClass(tree);
  }

  @SafeVarargs
  private final void excludeLinesIfTrue(boolean shouldExclude, Tree tree, Class<? extends JavaCheck>... rules) {
    for (Class<? extends JavaCheck> rule : rules) {
      excludeLinesIfTrue(shouldExclude, tree, rule);
    }
  }

  private void excludeLinesIfTrue(boolean shouldExclude, Tree tree, Class<? extends JavaCheck> rule) {
    if (shouldExclude) {
      excludeLines(tree, rule);
    } else {
      acceptLines(tree, rule);
    }
  }

  private static boolean usesAnnotation(ClassTree classTree, List<String> annotations) {
    SymbolMetadata classMetadata = classTree.symbol().metadata();
    return annotations.stream().anyMatch(classMetadata::isAnnotatedWith);
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

  private static boolean generatesPrivateFields(ClassTree tree) {
    if (usesAnnotation(tree, Collections.singletonList(LOMBOK_VALUE))) {
      return true;
    }
    // Annotated with @lombok.experimental.FieldDefaults(level = lombok.AccessLevel.PRIVATE)
    List<SymbolMetadata.AnnotationValue> annotationValues = tree.symbol().metadata().valuesForAnnotation(LOMBOK_FIELD_DEFAULTS);
    return annotationValues != null &&
      annotationValues.stream()
        .filter(Objects::nonNull)
        .anyMatch(
          av -> "level".equals(av.name()) && "PRIVATE".equals(getAccessLevelValue(av.value()))
        );
  }

  private static boolean generatesPackagePrivateAccess(VariableTree tree) {
    return tree.symbol().metadata().isAnnotatedWith("lombok.experimental.PackagePrivate");
  }

  private static boolean generatesFinalFields(ClassTree classTree) {
    if (usesAnnotation(classTree, Collections.singletonList(LOMBOK_VALUE)) && !usesAnnotation(classTree, NON_FINAL)) {
      return true;
    }
    // Annotated with @lombok.experimental.FieldDefaults(makeFinal=true)
    List<SymbolMetadata.AnnotationValue> annotationValues = classTree.symbol().metadata().valuesForAnnotation(LOMBOK_FIELD_DEFAULTS);
    return annotationValues != null &&
      annotationValues.stream()
        .filter(Objects::nonNull)
        .anyMatch(
          av -> "makeFinal".equals(av.name()) && getMakeFinalValue(av.value())
        );
  }

  private static boolean generatesNonFinal(VariableTree tree) {
    return tree.symbol().metadata().isAnnotatedWith("lombok.experimental.NonFinal");
  }

  @Nullable
  private static String getAccessLevelValue(Object value) {
    if (value instanceof Symbol) {
      return ((Symbol) value).name();
    }
    return null;
  }

  private static boolean getMakeFinalValue(Object value) {
    if (value instanceof Boolean) {
      return (Boolean) value;
    }
    return false;
  }

  @Override
  public void visitIdentifier(IdentifierTree tree) {
    Symbol symbol = tree.symbol();
    if (symbol.isVariableSymbol() && symbol.type().is(LOMBOK_VAL)) {
      parentMethodInvocation(tree)
        .ifPresent(mit -> excludeLines(mit, Arrays.asList(SillyEqualsCheck.class, CollectionInappropriateCallsCheck.class)));
    }
    super.visitIdentifier(tree);
  }

  private static Optional<Tree> parentMethodInvocation(IdentifierTree identifier) {
    Tree parent = identifier.parent();
    while (parent != null && !parent.is(Tree.Kind.METHOD_INVOCATION)) {
      parent = parent.parent();
    }
    return Optional.ofNullable(parent);
  }

}
