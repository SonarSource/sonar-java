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
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.resolve.AnnotationInstance;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

@Rule(
    key = RepeatAnnotationCheck.RULE_KEY,
    priority = Priority.CRITICAL,
    tags = {"java8"})
public class RepeatAnnotationCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String RULE_KEY = "S1710";
  private final RuleKey ruleKey = RuleKey.of(CheckList.REPOSITORY_KEY, RULE_KEY);

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitAnnotation(AnnotationTree annotationTree) {
    if (isArrayInitialized(annotationTree)) {
      NewArrayTree arrayTree = (NewArrayTree) annotationTree.arguments().get(0);
      if (isAllSameAnnotation(arrayTree.initializers()) && isAnnotationRepeatable(arrayTree.initializers().get(0))) {
        context.addIssue(annotationTree, ruleKey, "Remove the '" + getAnnotationName(annotationTree) + "' wrapper from this annotation group");
      }
    }
    super.visitAnnotation(annotationTree);
  }

  private boolean isAnnotationRepeatable(ExpressionTree expressionTree) {
    List<AnnotationInstance> annotations = ((AbstractTypedTree) expressionTree).getSymbolType().getSymbol().metadata().annotations();
    for (AnnotationInstance annotation : annotations) {
      if(annotation.isTyped("java.lang.annotation.Repeatable")) {
        return true;
      }
    }
    return false;
  }

  private boolean isAllSameAnnotation(List<ExpressionTree> initializers) {
    if (initializers.isEmpty()) {
      return false;
    }
    String annotationName = getAnnotationName(initializers.get(0));
    if(annotationName.isEmpty()){
      return false;
    }
    for (int i = 1; i < initializers.size(); i++) {
      if (!annotationName.equals(getAnnotationName(initializers.get(i)))) {
        return false;
      }
    }
    return true;
  }

  private String getAnnotationName(ExpressionTree initializer) {
    String result = "";
    if (initializer.is(Tree.Kind.ANNOTATION)) {
      Tree annotationType = ((AnnotationTree) initializer).annotationType();
      if (annotationType.is(Tree.Kind.IDENTIFIER)) {
        result = ((IdentifierTree) annotationType).name();
      } else if (annotationType.is(Tree.Kind.MEMBER_SELECT)) {
        result = fullName((MemberSelectExpressionTree) annotationType);
      }
    }
    return result;
  }

  private String fullName(MemberSelectExpressionTree tree) {
    if (tree.expression().is(Tree.Kind.IDENTIFIER)) {
      return ((IdentifierTree) tree.expression()).name() + "." + tree.identifier().name();
    }
    return fullName((MemberSelectExpressionTree) tree.expression()) + "." + tree.identifier().name();
  }

  private boolean isArrayInitialized(AnnotationTree annotationTree) {
    return annotationTree.arguments().size() == 1 && annotationTree.arguments().get(0).is(Tree.Kind.NEW_ARRAY);
  }
}
