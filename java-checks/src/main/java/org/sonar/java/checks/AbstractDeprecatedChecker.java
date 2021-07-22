/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.java.checks;

import java.util.Arrays;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.java.ast.visitors.PublicApiChecker;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

public class AbstractDeprecatedChecker extends IssuableSubscriptionVisitor {

  private static final Kind[] CLASS_KINDS = PublicApiChecker.classKinds();
  private static final Kind[] METHOD_KINDS = PublicApiChecker.methodKinds();

  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(PublicApiChecker.apiKinds());
  }

  public static boolean hasJavadocDeprecatedTag(Tree tree) {
    return PublicApiChecker.getApiJavadoc(tree).filter(comment -> comment.contains("@deprecated")).isPresent();
  }

  public static boolean hasDeprecatedAnnotation(Tree tree) {
    return deprecatedAnnotation(tree) != null;
  }

  @CheckForNull
  public static AnnotationTree deprecatedAnnotation(Tree tree) {
    if (tree.is(CLASS_KINDS)) {
      return deprecatedAnnotation((ClassTree) tree);
    } else if (tree.is(METHOD_KINDS)) {
      return deprecatedAnnotation((MethodTree) tree);
    } else if (tree.is(Kind.VARIABLE)) {
      return deprecatedAnnotation((VariableTree) tree);
    }
    return null;
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

  protected static Tree getReportTree(Tree tree) {
    Tree reportTree = tree;
    if(reportTree.is(PublicApiChecker.classKinds())) {
      reportTree = ExpressionsHelper.reportOnClassTree((ClassTree) reportTree);
    } else if(reportTree.is(PublicApiChecker.methodKinds())) {
      reportTree = ((MethodTree) reportTree).simpleName();
    } else if(reportTree.is(Tree.Kind.VARIABLE)) {
      reportTree = ((VariableTree) reportTree).simpleName();
    }
    return reportTree;
  }

  public static boolean isDeprecated(AnnotationTree tree) {
    return tree.annotationType().is(Kind.IDENTIFIER) &&
      "Deprecated".equals(((IdentifierTree) tree.annotationType()).name());
  }

}
