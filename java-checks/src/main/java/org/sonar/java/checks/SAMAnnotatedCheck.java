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

import org.sonar.api.rule.RuleKey;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

@Rule(
    key = SAMAnnotatedCheck.RULE_KEY,
    priority = Priority.MAJOR,
    tags = {"java8"})
public class SAMAnnotatedCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String RULE_KEY = "S1609";
  private static final RuleKey RULE = RuleKey.of(CheckList.REPOSITORY_KEY, RULE_KEY);

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitClass(ClassTree tree) {
    if (isSAM(tree) && !isAnnotated(tree)) {
      context.addIssue(tree, RULE, "Annotate the \"" + tree.simpleName().name() + "\" interface with the @FunctionInterface annotation");
    }
    super.visitClass(tree);
  }

  private boolean isAnnotated(ClassTree tree) {
    for (AnnotationTree annotationTree : tree.modifiers().annotations()) {
      Tree annotationType = annotationTree.annotationType();
      if (annotationType.is(Tree.Kind.IDENTIFIER) && "FunctionalInterface".equals(((IdentifierTree) annotationType).name())) {
        return true;
      }
    }
    return false;
  }

  private boolean isSAM(ClassTree tree) {
    return tree.is(Tree.Kind.INTERFACE) && tree.members().size() == 1 && isNonStaticNonDefaultMethod(tree.members().get(0));
  }

  private boolean isNonStaticNonDefaultMethod(Tree memberTree) {
    boolean result = memberTree.is(Tree.Kind.METHOD);
    if (result) {
      MethodTree methodTree = (MethodTree) memberTree;
      List<Modifier> modifiers = methodTree.modifiers().modifiers();
      result = !modifiers.contains(Modifier.STATIC) && !modifiers.contains(Modifier.DEFAULT);
    }
    return result;
  }
}
