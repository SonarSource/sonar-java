/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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

import java.util.List;

public class AbstractDeprecatedChecker extends SubscriptionBaseVisitor {


  private PublicApiChecker publicApiChecker = new PublicApiChecker();

  @Override
  public List<Kind> nodesToVisit() {
    return Lists.newArrayList(PublicApiChecker.API_KINDS);
  }

  public boolean hasJavadocDeprecatedTag(Tree tree) {
    String javadoc = publicApiChecker.getApiJavadoc(tree);
    return hasJavadocDeprecatedTag(javadoc);
  }

  public static boolean hasJavadocDeprecatedTag(String comment) {
    return comment != null && comment.startsWith("/**") && comment.contains("@deprecated");
  }

  public boolean hasDeprecatedAnnotation(Tree tree) {
    if (tree.is(PublicApiChecker.CLASS_KINDS)) {
      return hasDeprecatedAnnotation((ClassTree) tree);
    } else if (tree.is(PublicApiChecker.METHOD_KINDS)) {
      return hasDeprecatedAnnotation((MethodTree) tree);
    } else if (tree.is(Kind.VARIABLE)) {
      return hasDeprecatedAnnotation((VariableTree) tree);
    }
    return false;
  }

  private boolean hasDeprecatedAnnotation(ClassTree classTree) {
    return hasDeprecatedAnnotation(classTree.modifiers().annotations());
  }

  private boolean hasDeprecatedAnnotation(VariableTree variableTree) {
    return hasDeprecatedAnnotation(variableTree.modifiers().annotations());
  }

  private boolean hasDeprecatedAnnotation(MethodTree methodTree) {
    return hasDeprecatedAnnotation(methodTree.modifiers().annotations());
  }

  private boolean hasDeprecatedAnnotation(Iterable<AnnotationTree> annotations) {
    for (AnnotationTree annotationTree : annotations) {
      if (isDeprecated(annotationTree)) {
        return true;
      }
    }
    return false;
  }

  public boolean isDeprecated(AnnotationTree tree) {
    return tree.annotationType().is(Kind.IDENTIFIER) &&
        "Deprecated".equals(((IdentifierTree) tree.annotationType()).name());
  }

}
