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
import org.sonar.java.checks.RegexPatternsNeedlesslyCheck;
import org.sonar.java.checks.SillyEqualsCheck;
import org.sonar.java.checks.StaticMethodCheck;
import org.sonar.java.checks.UselessImportCheck;
import org.sonar.java.checks.UtilityClassWithPublicConstructorCheck;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.naming.BadFieldNameCheck;
import org.sonar.java.checks.spring.SpringComponentWithNonAutowiredMembersCheck;
import org.sonar.java.checks.tests.AssertionTypesCheck;
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

  private static final Set<Class<? extends JavaCheck>> FILTERED_RULES = Set.of(
    // alphabetically sorted
    /* S5845 */ AssertionTypesCheck.class,
    /* S1258 */ AtLeastOneConstructorCheck.class,
    /* .S116 */ BadFieldNameCheck.class,
    /* S2175 */ CollectionInappropriateCallsCheck.class,
    /* S1170 */ ConstantsShouldBeStaticFinalCheck.class,
    /* S1210 */ EqualsNotOverriddenInSubclassCheck.class,
    /* S1210 */ EqualsNotOverridenWithCompareToCheck.class,
    /* S1165 */ ExceptionsShouldBeImmutableCheck.class,
    /* S2039 */ FieldModifierCheck.class,
    /* S1450 */ PrivateFieldUsedLocallyCheck.class,
    /* S4248 */ RegexPatternsNeedlesslyCheck.class,
    /* S2159 */ SillyEqualsCheck.class,
    /* S3749 */ SpringComponentWithNonAutowiredMembersCheck.class,
    /* S2325 */ StaticMethodCheck.class,
    /* S1068 */ UnusedPrivateFieldCheck.class,
    /* S1128 */ UselessImportCheck.class,
    /* S1118 */ UtilityClassWithPublicConstructorCheck.class
  );

  private static final String LOMBOK_BUILDER = "lombok.Builder";
  private static final String LOMBOK_SUPER_BUILDER = "lombok.SuperBuilder";
  private static final String LOMBOK_BUILDER_DEFAULT = "lombok.Builder$Default";
  private static final String LOMBOK_VAL = "lombok.val";
  private static final String LOMBOK_VALUE = "lombok.Value";
  private static final String LOMBOK_FIELD_DEFAULTS = "lombok.experimental.FieldDefaults";
  private static final String LOMBOK_DATA = "lombok.Data";

  private static final List<String> GENERATE_UNUSED_FIELD_RELATED_METHODS = List.of(
    "lombok.Getter",
    "lombok.Setter",
    LOMBOK_BUILDER,
    LOMBOK_SUPER_BUILDER,
    "lombok.ToString",
    "lombok.AllArgsConstructor",
    "lombok.NoArgsConstructor",
    "lombok.RequiredArgsConstructor");

  private static final List<String> GENERATE_CONSTRUCTOR = List.of(
    "lombok.AllArgsConstructor",
    "lombok.NoArgsConstructor",
    "lombok.RequiredArgsConstructor",
    LOMBOK_DATA);

  private static final List<String> GENERATE_EQUALS = List.of(
    "lombok.EqualsAndHashCode",
    LOMBOK_DATA,
    LOMBOK_VALUE);

  private static final List<String> UTILITY_CLASS = Collections.singletonList("lombok.experimental.UtilityClass");

  private static final List<String> NON_FINAL = Collections.singletonList("lombok.experimental.NonFinal");

  @Override
  public Set<Class<? extends JavaCheck>> filteredRules() {
    return FILTERED_RULES;
  }

  @Override
  public void visitImport(ImportTree tree) {
    String fullyQualifiedName = ExpressionsHelper.concatenate((ExpressionTree) tree.qualifiedIdentifier());

    excludeLinesIfTrue("lombok.var".equals(fullyQualifiedName) || LOMBOK_VAL.equals(fullyQualifiedName), tree, UselessImportCheck.class);

    super.visitImport(tree);
  }

  @Override
  public void visitClass(ClassTree tree) {
    boolean generatesEquals = usesAnnotation(tree, GENERATE_EQUALS);

    excludeLinesIfTrue(generatesEquals || usesAnnotation(tree, GENERATE_UNUSED_FIELD_RELATED_METHODS), tree, UnusedPrivateFieldCheck.class, PrivateFieldUsedLocallyCheck.class);
    excludeLinesIfTrue(usesAnnotation(tree, GENERATE_CONSTRUCTOR), tree, AtLeastOneConstructorCheck.class, SpringComponentWithNonAutowiredMembersCheck.class);
    excludeLinesIfTrue(generatesEquals, tree, EqualsNotOverriddenInSubclassCheck.class, EqualsNotOverridenWithCompareToCheck.class);
    excludeLinesIfTrue(generatesNonPublicConstructor(tree), tree, UtilityClassWithPublicConstructorCheck.class);
    boolean isUtilityClass = usesAnnotation(tree, UTILITY_CLASS);
    excludeLinesIfTrue(isUtilityClass, tree, BadFieldNameCheck.class, ConstantsShouldBeStaticFinalCheck.class, StaticMethodCheck.class);

    if (isUtilityClass) {
      tree.members().stream()
        .filter(t -> t.is(Tree.Kind.VARIABLE))
        .forEach(v -> excludeLines(v, RegexPatternsNeedlesslyCheck.class));
    }

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

    // Exclude final fields annotated with @Builder.Default in a @Builder class
    if (usesAnnotation(tree, List.of(LOMBOK_BUILDER, LOMBOK_SUPER_BUILDER))) {
      tree.members().stream()
        .filter(t -> t.is(Tree.Kind.VARIABLE))
        .map(VariableTree.class::cast)
        .filter(v -> v.symbol().isFinal() && v.symbol().metadata().isAnnotatedWith(LOMBOK_BUILDER_DEFAULT))
        .forEach(v -> excludeLines(v, ConstantsShouldBeStaticFinalCheck.class));
    }

    super.visitClass(tree);
  }

  private static boolean usesAnnotation(ClassTree classTree, List<String> annotations) {
    SymbolMetadata classMetadata = classTree.symbol().metadata();
    return annotations.stream().anyMatch(classMetadata::isAnnotatedWith);
  }

  private static boolean generatesNonPublicConstructor(ClassTree classTree) {
    if (usesAnnotation(classTree, UTILITY_CLASS)) {
      return true;
    }
    SymbolMetadata metadata = classTree.symbol().metadata();
    return GENERATE_CONSTRUCTOR.stream()
      .map(metadata::valuesForAnnotation)
      .filter(Objects::nonNull)
      // By default, constructor is public
      .anyMatch(LombokFilter::generatesNonPublicAccess);
  }

  private static boolean generatesNonPublicAccess(List<SymbolMetadata.AnnotationValue> values) {
    return values.stream().anyMatch(av -> "access".equals(av.name()) && !"PUBLIC".equals(getAccessLevelValue(av.value())));
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
          av -> "level".equals(av.name()) && "PRIVATE".equals(getAccessLevelValue(av.value())));
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
          av -> "makeFinal".equals(av.name()) && getMakeFinalValue(av.value()));
  }

  private static boolean generatesNonFinal(VariableTree tree) {
    return tree.symbol().metadata().isAnnotatedWith("lombok.experimental.NonFinal");
  }

  @Nullable
  private static String getAccessLevelValue(Object value) {
    if (value instanceof Symbol symbol) {
      return symbol.name();
    }
    return null;
  }

  private static boolean getMakeFinalValue(Object value) {
    if (value instanceof Boolean bool) {
      return bool;
    }
    return false;
  }

  @Override
  public void visitIdentifier(IdentifierTree tree) {
    Symbol symbol = tree.symbol();
    if (symbol.isVariableSymbol() && symbol.type().is(LOMBOK_VAL)) {
      parentMethodInvocation(tree)
        .ifPresent(mit -> excludeLines(mit, SillyEqualsCheck.class, CollectionInappropriateCallsCheck.class, AssertionTypesCheck.class));
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
