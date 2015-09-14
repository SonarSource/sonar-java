/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.collect.Lists;
import org.sonar.java.ast.visitors.PublicApiChecker;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.Nullable;
import java.util.List;

public class AbstractDeprecatedChecker extends SubscriptionBaseVisitor {

  private static final Kind[] CLASS_KINDS = PublicApiChecker.classKinds();
  private static final Kind[] METHOD_KINDS = PublicApiChecker.methodKinds();
  private static final Kind[] API_KINDS = PublicApiChecker.apiKinds();

  @Override
  public List<Kind> nodesToVisit() {
    return Lists.newArrayList(API_KINDS);
  }

  public static boolean hasJavadocDeprecatedTag(Tree tree) {
    return hasJavadocDeprecatedTag(PublicApiChecker.getApiJavadoc(tree));
  }

  public static boolean hasJavadocDeprecatedTag(@Nullable String comment) {
    return comment != null && comment.startsWith("/**") && comment.contains("@deprecated");
  }

  public static boolean hasDeprecatedAnnotation(Tree tree) {
    if (tree.is(CLASS_KINDS)) {
      return hasDeprecatedAnnotation((ClassTree) tree);
    } else if (tree.is(METHOD_KINDS)) {
      return hasDeprecatedAnnotation((MethodTree) tree);
    } else if (tree.is(Kind.VARIABLE)) {
      return hasDeprecatedAnnotation((VariableTree) tree);
    }
    return false;
  }

  private static boolean hasDeprecatedAnnotation(ClassTree classTree) {
    return hasDeprecatedAnnotation(classTree.modifiers().annotations());
  }

  private static boolean hasDeprecatedAnnotation(VariableTree variableTree) {
    return hasDeprecatedAnnotation(variableTree.modifiers().annotations());
  }

  private static boolean hasDeprecatedAnnotation(MethodTree methodTree) {
    return hasDeprecatedAnnotation(methodTree.modifiers().annotations());
  }

  private static boolean hasDeprecatedAnnotation(Iterable<AnnotationTree> annotations) {
    for (AnnotationTree annotationTree : annotations) {
      if (isDeprecated(annotationTree)) {
        return true;
      }
    }
    return false;
  }

  public static boolean isDeprecated(AnnotationTree tree) {
    return tree.annotationType().is(Kind.IDENTIFIER) &&
      "Deprecated".equals(((IdentifierTree) tree.annotationType()).name());
  }

}
