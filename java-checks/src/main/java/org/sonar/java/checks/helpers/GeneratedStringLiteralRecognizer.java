/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

/**
 * Recognizes strings that are most likely not hand-written and maintained by developers.
 *
 * Currently we ignore:
 *
 * <ul>
 *   <li>{@code d1} strings in {@code kotlin.Metadata} annotations</li>
 *   <li>All strings in {@code kotlin.jvm.internal.SourceDebugExtension} annotations</li>
 *   <li>All strings in {@code kotlin.coroutines.jvm.internal.DebugMetadata} annotations</li>
 * </ul>
 */
public final class GeneratedStringLiteralRecognizer {

  private static final String KOTLIN_METADATA = "kotlin.Metadata";
  private static final String KOTLIN_SOURCE_DEBUG_EXTENSION = "kotlin.jvm.internal.SourceDebugExtension";
  private static final String KOTLIN_DEBUG_METADATA = "kotlin.coroutines.jvm.internal.DebugMetadata";

  private GeneratedStringLiteralRecognizer() {
  }

  public static boolean isGenerated(LiteralTree literal) {
    if (!literal.is(Tree.Kind.STRING_LITERAL, Tree.Kind.TEXT_BLOCK)) {
      return false;
    }

    Tree annotationArgument = literal;
    while (annotationArgument.parent() != null && !annotationArgument.parent().is(Tree.Kind.ARGUMENTS)) {
      annotationArgument = annotationArgument.parent();
    }

    Tree arguments = annotationArgument.parent();
    if (arguments == null || !(arguments.parent() instanceof AnnotationTree annotation)) {
      return false;
    }

    return isAnnotationType(annotation, KOTLIN_SOURCE_DEBUG_EXTENSION)
      || isAnnotationType(annotation, KOTLIN_DEBUG_METADATA)
      || (annotationArgument instanceof ExpressionTree argument
        && "d1".equals(ExpressionUtils.annotationAttributeName(argument))
        && isAnnotationType(annotation, KOTLIN_METADATA));
  }

  private static boolean isAnnotationType(AnnotationTree annotation, String fullyQualifiedName) {
    TypeTree annotationTypeTree = annotation.annotationType();
    Type annotationType = annotationTypeTree.symbolType();
    if (!annotationType.isUnknown()) {
      return annotationType.is(fullyQualifiedName);
    }

    String annotationName = ExpressionsHelper.concatenate((ExpressionTree) annotationTypeTree);
    if (fullyQualifiedName.equals(annotationName)) {
      return true;
    }

    String unqualifiedName = fullyQualifiedName.substring(fullyQualifiedName.lastIndexOf('.') + 1);
    return annotationName.equals(unqualifiedName)
      && hasExplicitImport(annotation, fullyQualifiedName);
  }

  private static boolean hasExplicitImport(Tree tree, String fullyQualifiedName) {
    CompilationUnitTree compilationUnit = (CompilationUnitTree) ExpressionUtils.getParentOfType(tree, Tree.Kind.COMPILATION_UNIT);
    return compilationUnit.imports().stream()
      .filter(ImportTree.class::isInstance)
      .map(ImportTree.class::cast)
      .filter(importTree -> !importTree.isStatic())
      .map(ImportTree::qualifiedIdentifier)
      .anyMatch(imported -> imported instanceof ExpressionTree expression
        && fullyQualifiedName.equals(ExpressionsHelper.concatenate(expression)));
  }

}
