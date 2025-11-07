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
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S2131")
public class PrimitiveTypeBoxingWithToStringCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final MethodMatchers TO_STRING_MATCHERS = MethodMatchers.create().ofSubTypes(
    "java.lang.Byte",
    "java.lang.Character",
    "java.lang.Short",
    "java.lang.Integer",
    "java.lang.Long",
    "java.lang.Float",
    "java.lang.Double",
    "java.lang.Boolean")
    .names("toString")
    .addWithoutParametersMatcher()
    .build();

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    if (TO_STRING_MATCHERS.matches(tree)) {
      ExpressionTree abstractTypedTree = ((MemberSelectExpressionTree) tree.methodSelect()).expression();
      if (abstractTypedTree.is(Kind.NEW_CLASS) || isValueOfInvocation(abstractTypedTree)) {
        String typeName = abstractTypedTree.symbolType().toString();
        createIssue(tree, typeName);
      }
    }
    super.visitMethodInvocation(tree);
  }


  private void createIssue(Tree reportingTree, String wrapperName) {
    context.reportIssue(this, reportingTree, "Use \"" + wrapperName + ".toString\" instead.");
  }

  @Override
  public void visitAnnotation(AnnotationTree annotationTree) {
    scan(annotationTree.annotationType());
    // skip arguments of annotation as it should be compile time constant so it is not relevant here.
  }

  private static boolean isValueOfInvocation(ExpressionTree abstractTypedTree) {
    if (!abstractTypedTree.is(Kind.METHOD_INVOCATION)) {
      return false;
    }
    Type type = abstractTypedTree.symbolType();
    MethodMatchers valueOfMatcher = MethodMatchers.create()
      .ofTypes(type.fullyQualifiedName())
      .names("valueOf")
      .addParametersMatcher(type.primitiveType().fullyQualifiedName())
      .build();
    return valueOfMatcher.matches((MethodInvocationTree) abstractTypedTree);
  }
}
