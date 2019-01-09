/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import org.sonar.check.Rule;
import org.sonar.java.RspecKey;
import org.sonar.java.ast.visitors.PublicApiChecker;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.Deque;
import java.util.LinkedList;

@Rule(key = "MissingDeprecatedCheck")
@RspecKey("S1123")
public class MissingDeprecatedCheck extends AbstractDeprecatedChecker {

  private static final Kind[] CLASS_KINDS = PublicApiChecker.classKinds();
  private static final Kind[] METHOD_KINDS = PublicApiChecker.methodKinds();

  private final Deque<Tree> currentParent = new LinkedList<>();
  private final Deque<Boolean> classOrInterfaceIsDeprecated = new LinkedList<>();
  private boolean isJava9 = false;

  @Override
  public void setContext(JavaFileScannerContext context) {
    isJava9 = context.getJavaVersion().asInt() >= 9;
    super.setContext(context);
  }

  @Override
  public void visitNode(Tree tree) {
    boolean isLocalVar = false;
    if (tree.is(Tree.Kind.VARIABLE)) {
      isLocalVar = currentParent.peek().is(METHOD_KINDS);
    } else {
      currentParent.push(tree);
    }

    AnnotationTree deprecatedAnnotation = deprecatedAnnotation(tree);
    boolean hasDeprecatedAnnotation = deprecatedAnnotation != null;
    boolean hasJavadocDeprecatedTag = hasJavadocDeprecatedTag(tree);
    if (currentClassNotDeprecated() && !isLocalVar) {
      if (hasDeprecatedAnnotation) {
        if (!hasJavadocDeprecatedTag) {
          reportIssue(getReportTree(tree), "Add the missing @deprecated Javadoc tag.");
        } else if (isJava9 && deprecatedAnnotation.arguments().isEmpty()) {
          reportIssue(getReportTree(deprecatedAnnotation), "Add 'since' and/or 'forRemoval' arguments to the @Deprecated annotation.");
        }
      } else if (hasJavadocDeprecatedTag) {
        reportIssue(getReportTree(tree), "Add the missing @Deprecated annotation.");
      }
    }
    if (tree.is(CLASS_KINDS)) {
      classOrInterfaceIsDeprecated.push(hasDeprecatedAnnotation || hasJavadocDeprecatedTag);
    }
  }

  private boolean currentClassNotDeprecated() {
    return classOrInterfaceIsDeprecated.isEmpty() || !classOrInterfaceIsDeprecated.peek();
  }

  @Override
  public void leaveNode(Tree tree) {
    if (!tree.is(Tree.Kind.VARIABLE)) {
      currentParent.pop();
    }
    if (tree.is(CLASS_KINDS)) {
      classOrInterfaceIsDeprecated.pop();
    }
  }
}
