/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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
package org.sonar.java.checks.helpers;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;

import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.ast.visitors.PublicApiChecker;
import org.sonar.java.model.expression.AssignmentExpressionTreeImpl;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.java.model.ExpressionUtils.annotationAttributeName;

public class DeprecatedCheckerHelper {

  private static final String DEPRECATED_TAG = "@deprecated";
  private static final Kind[] CLASS_KINDS = PublicApiChecker.classKinds();
  private static final Kind[] METHOD_KINDS = PublicApiChecker.methodKinds();

  private static final Set<String> REMOVAL_TIMELINE_TERMS = Set.of(
    "will be removed in",
    "removed in version",
    "to be removed in",
    "scheduled for removal",
    "deprecated since"
  );

  private static final Pattern DEPRECATED_TAG_CONTENT_PATTERN = Pattern.compile(
    DEPRECATED_TAG + "(.*)(?:\\n\\s*\\*\\s*@|$)",
    Pattern.DOTALL
  );

  private DeprecatedCheckerHelper() {
    // Helper class, should not be implemented.
  }

  public static boolean hasJavadocDeprecatedTag(Tree tree) {
    return PublicApiChecker.getApiJavadoc(tree)
      .filter(comment -> comment.contains(DEPRECATED_TAG))
      .isPresent();
  }

  public static boolean hasJavadocDeprecatedTagWithoutLegitimateDocumentation(Tree tree) {
    return PublicApiChecker.getApiJavadoc(tree)
      .filter(comment -> comment.contains(DEPRECATED_TAG))
      .filter(comment -> !hasLegitimateDeprecationDocumentation(comment))
      .isPresent();
  }

  @VisibleForTesting
  static boolean hasLegitimateDeprecationDocumentation(String javadoc) {
    String deprecatedSection = extractDeprecatedTagContent(javadoc);
    if (deprecatedSection.isEmpty()) {
      return false;
    }

    // Check for migration guidance indicators or removal timeline
    return hasMigrationGuidance(deprecatedSection) || hasRemovalTimeline(deprecatedSection);
  }

  private static String extractDeprecatedTagContent(String javadoc) {
    // Extract content from @deprecated (including linebreaks) until next javadoc tag or end
    // Pattern: @deprecated followed by everything until (newline + whitespaces + * + whitespaces + @) or end
    Matcher matcher = DEPRECATED_TAG_CONTENT_PATTERN.matcher(javadoc);

    if (!matcher.find()) {
      return "";
    }

    String content = matcher.group(1);
    // Clean up javadoc formatting: remove leading asterisks and extra whitespace from continuation lines
    return content.replaceAll("(?m)^\\s*\\*\\s*", " ").trim();
  }

  private static boolean hasMigrationGuidance(String deprecatedContent) {
    String lowerContent = deprecatedContent.toLowerCase(Locale.ROOT);
    return lowerContent.contains("use") && (lowerContent.contains("instead") || lowerContent.contains("new"));
  }

  private static boolean hasRemovalTimeline(String deprecatedContent) {
    String lowerContent = deprecatedContent.toLowerCase(Locale.ROOT);
    return REMOVAL_TIMELINE_TERMS.stream()
      .anyMatch(lowerContent::contains);
  }

  @CheckForNull
  public static AnnotationTree deprecatedAnnotation(Tree tree) {
    AnnotationTree annotationTree = null;
    if (tree.is(CLASS_KINDS)) {
      annotationTree = deprecatedAnnotation((ClassTree) tree);
    } else if (tree.is(METHOD_KINDS)) {
      annotationTree = deprecatedAnnotation((MethodTree) tree);
    } else if (tree.is(Kind.VARIABLE)) {
      annotationTree = deprecatedAnnotation((VariableTree) tree);
    }
    return annotationTree;
  }

  @CheckForNull
  private static AnnotationTree deprecatedAnnotation(ClassTree classTree) {
    return deprecatedAnnotation(classTree.modifiers().annotations());
  }

  @CheckForNull
  private static AnnotationTree deprecatedAnnotation(VariableTree variableTree) {
    return deprecatedAnnotation(variableTree.modifiers().annotations());
  }

  @CheckForNull
  private static AnnotationTree deprecatedAnnotation(MethodTree methodTree) {
    return deprecatedAnnotation(methodTree.modifiers().annotations());
  }

  @CheckForNull
  private static AnnotationTree deprecatedAnnotation(Iterable<AnnotationTree> annotations) {
    for (AnnotationTree annotationTree : annotations) {
      if (isDeprecated(annotationTree)) {
        return annotationTree;
      }
    }
    return null;
  }

  private static boolean isDeprecated(AnnotationTree tree) {
    return tree.annotationType().is(Kind.IDENTIFIER) &&
      "Deprecated".equals(((IdentifierTree) tree.annotationType()).name());
  }

  /**
   * @param annotationTree The annotation tree to check
   * @param attributeName  The attribute name of the attribute to get the value for
   * @return an Optional containing the value of the attribute if found, otherwise an empty Optional
   */
  public static <T> Optional<T> getAnnotationAttributeValue(AnnotationTree annotationTree, String attributeName, Class<T> valueType) {
    Optional<ExpressionTree> valueExpression = annotationTree.arguments().stream()
      .filter(argument -> attributeName.equals(annotationAttributeName(argument)))
      .map(argument -> {
        // arguments of an annotation are either an assignment (name=value) or an expression (ofter a literal value)
        if (argument.is(Kind.ASSIGNMENT)) {
          return ((AssignmentExpressionTreeImpl) argument).expression();
        } else {
          return argument;
        }
      })
      .findFirst();
    return valueExpression.flatMap(expressionTree -> expressionTree.asConstant(valueType));
  }

}
