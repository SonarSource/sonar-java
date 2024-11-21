/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import javax.annotation.CheckForNull;
import org.sonar.java.ast.visitors.PublicApiChecker;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

public class DeprecatedCheckerHelper {

  private static final Kind[] CLASS_KINDS = PublicApiChecker.classKinds();
  private static final Kind[] METHOD_KINDS = PublicApiChecker.methodKinds();

  private DeprecatedCheckerHelper() {
    // Helper class, should not be implemented.
  }

  public static boolean hasJavadocDeprecatedTag(Tree tree) {
    return PublicApiChecker.getApiJavadoc(tree).filter(comment -> comment.contains("@deprecated")).isPresent();
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

  public static Tree reportTreeForDeprecatedTree(Tree tree) {
    Tree reportTree = tree;
    if (reportTree.is(PublicApiChecker.classKinds())) {
      reportTree = ExpressionsHelper.reportOnClassTree((ClassTree) reportTree);
    } else if (reportTree.is(PublicApiChecker.methodKinds())) {
      reportTree = ((MethodTree) reportTree).simpleName();
    } else if (reportTree.is(Tree.Kind.VARIABLE)) {
      reportTree = ((VariableTree) reportTree).simpleName();
    }
    return reportTree;
  }

  private static boolean isDeprecated(AnnotationTree tree) {
    return tree.annotationType().is(Kind.IDENTIFIER) &&
      "Deprecated".equals(((IdentifierTree) tree.annotationType()).name());
  }

}
