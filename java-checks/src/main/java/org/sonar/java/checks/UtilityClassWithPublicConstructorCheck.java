/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.AnnotationsHelper;
import org.sonar.java.checks.helpers.ClassPatternsUtils;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S1118")
public class UtilityClassWithPublicConstructorCheck extends IssuableSubscriptionVisitor {

  /**
   * See also {@link org.sonar.java.filters.LombokFilter}.
   */
  private static final Set<String> LOMBOK_CONSTRUCTOR_GENERATORS = Set.of(
    "lombok.NoArgsConstructor",
    "lombok.AllArgsConstructor",
    "lombok.RequiredArgsConstructor");

  private static final Set<String> LOMBOK_CONSTRUCTOR_GENERATOR_NAMES =
    LOMBOK_CONSTRUCTOR_GENERATORS.stream()
      .map(AnnotationsHelper::annotationTypeIdentifier)
      .collect(Collectors.toSet());

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (!ClassPatternsUtils.isUtilityClass(classTree) || ClassPatternsUtils.isPrivateInnerClass(classTree)) {
      return;
    }
    boolean hasImplicitPublicConstructor = true;
    for (MethodTree explicitConstructor : getExplicitConstructors(classTree)) {
      hasImplicitPublicConstructor = false;
      if (isPublicConstructor(explicitConstructor)) {
        reportIssue(explicitConstructor.simpleName(), "Hide this public constructor.");
      }
    }
    if (hasImplicitPublicConstructor && !hasCompliantGeneratedConstructors(classTree)) {
      reportIssue(classTree.simpleName(), "Add a private constructor to hide the implicit public one.");
    }
  }

  private static List<MethodTree> getExplicitConstructors(ClassTree classTree) {
    return classTree.members().stream()
      .filter(UtilityClassWithPublicConstructorCheck::isConstructor)
      .map(MethodTree.class::cast)
      .toList();
  }

  private static boolean isConstructor(Tree tree) {
    return tree.is(Tree.Kind.CONSTRUCTOR);
  }

  private static boolean isPublicConstructor(Tree tree) {
    return isConstructor(tree) && hasPublicModifier((MethodTree) tree);
  }

  private static boolean hasPublicModifier(MethodTree methodTree) {
    return ModifiersUtils.hasModifier(methodTree.modifiers(), Modifier.PUBLIC);
  }

  private static boolean hasCompliantGeneratedConstructors(ClassTree classTree) {
    return classTree.modifiers().annotations().stream()
      .anyMatch(it -> isLombokConstructorGenerator(it.symbolType()) && !hasPublicAccess(it));
  }

  private static boolean isLombokConstructorGenerator(Type symbolType) {
    // This happens in automatic analysis. We match only the last part of the name.
    if (symbolType.isUnknown()) {
      return LOMBOK_CONSTRUCTOR_GENERATOR_NAMES.contains(symbolType.name());
    }

    return LOMBOK_CONSTRUCTOR_GENERATORS.contains(symbolType.fullyQualifiedName());
  }

  private static boolean hasPublicAccess(AnnotationTree annotation) {
    return annotation.arguments().stream().noneMatch(it ->
      isAccessLevelNotPublic(((AssignmentExpressionTree) it).expression())
    );
  }

  private static boolean isAccessLevelNotPublic(ExpressionTree tree) {
    String valueName;
    if (tree instanceof MemberSelectExpressionTree mset) {
      valueName = mset.identifier().name();
    } else if (tree instanceof IdentifierTree identifier) {
      valueName = identifier.name();
    } else {
      return false;
    }
    return !"PUBLIC".equals(valueName);
  }

}
