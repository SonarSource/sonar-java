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

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.matcher.TreeMatcher;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.matcher.TreeMatcher.forEachStatement;
import static org.sonar.java.matcher.TreeMatcher.hasSize;
import static org.sonar.java.matcher.TreeMatcher.matching;
import static org.sonar.java.matcher.TreeMatcher.statementAt;
import static org.sonar.java.matcher.TreeMatcher.withBody;
import static org.sonar.java.matcher.TreeMatcher.withExpression;

@Rule(key = "S7478")
public class UseTransformClassInsteadOfBuildCheck extends IssuableSubscriptionVisitor {

  private static final String CLASS_MODEL_CLASSNAME = "java.lang.classfile.ClassModel";
  private static final String CLASS_DESC_CLASSNAME = "java.lang.constant.ClassDesc";
  private static final String CONSUMER_CLASSNAME = "java.util.function.Consumer";
  private static final String CLASS_ENTRY_CLASSNAME = "java.lang.classfile.constantpool.ClassEntry";

  private final MethodMatchers classFileBuildMatcher = MethodMatchers.create()
    .ofTypes("java.lang.classfile.ClassFile")
    .names("build")
    .addParametersMatcher(
      CLASS_DESC_CLASSNAME,
      CONSUMER_CLASSNAME)
    .addParametersMatcher(
      CLASS_ENTRY_CLASSNAME,
      "java.lang.classfile.constantpool.ConstantPoolBuilder",
      CONSUMER_CLASSNAME)
    .build();

  /**
   * Matcher for expressions of the form `x -> { for (ClassModel cm : m) { ... } }`.
   *  Note that we handle only lambda expressions here, not method references. 
   */
  private final TreeMatcher<ExpressionTree> treeMatcher = TreeMatcher
    .isLambdaExpression(
      withBody(
        hasSize(1).and(
          statementAt(0,
            forEachStatement(
              withExpression(
                matching(e -> e.symbolType().fullyQualifiedName().equals(CLASS_MODEL_CLASSNAME))))))));

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree methodInvocation = (MethodInvocationTree) tree;
    if (classFileBuildMatcher.matches(methodInvocation) && !methodInvocation.arguments().isEmpty()) {
      ExpressionTree lastArgument = methodInvocation.arguments().get(methodInvocation.arguments().size() - 1);
      if (treeMatcher.check(lastArgument)) {
        reportIssue(methodInvocation.methodSelect(), "Replace this 'build()' call with 'transformClass()'.");
      }
    }
  }
}
