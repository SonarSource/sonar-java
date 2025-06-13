/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.ast.parser.ArgumentListTreeImpl;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.model.LiteralUtils.isEmptyString;

@Rule(key = "S3033")
public class UseIsEmptyToTestEmptinessOfStringBuilderCheck extends AbstractMethodDetection implements JavaVersionAwareVisitor {
  private static final String JAVA_LANG_STRING = "java.lang.String";

  private static final MethodMatchers TO_STRING = MethodMatchers.create()
    .ofTypes("java.lang.StringBuilder", "java.lang.StringBuffer")
    .names("toString")
    .withAnyParameters()
    .build();
  private static final MethodMatchers STRING_EQUALS = MethodMatchers.create()
    .ofTypes(JAVA_LANG_STRING)
    .names("equals")
    .withAnyParameters()
    .build();
  private static final MethodMatchers STRING_IS_EMPTY = MethodMatchers.create()
    .ofTypes(JAVA_LANG_STRING)
    .names("isEmpty")
    .withAnyParameters()
    .build();
  private static final MethodMatchers STRING_LENGTH = MethodMatchers.create()
    .ofTypes(JAVA_LANG_STRING)
    .names("length")
    .withAnyParameters()
    .build();

  private static final String MESSAGE_EQUALS = "Replace \"equals()\" with \"isEmpty()\".";
  private static final String MESSAGE_IS_EMPTY = "Replace \"toString().isEmpty()\" with \"isEmpty()\".";
  private static final String MESSAGE_LENGTH = "Replace \"toString().length()\" with \"isEmpty()\".";

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return TO_STRING;
  }

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava15Compatible();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree toStringInvocation) {
    MethodInvocationTree mit = argumentSide(toStringInvocation)
      .or(() -> methodSelectSide(toStringInvocation))
      .orElse(null);

    if (mit == null) {
      return;
    }

    if (STRING_EQUALS.matches(mit) && isEqualsWithEmptyString(mit)) {
      reportIssue(mit, MESSAGE_EQUALS);
    } else if (STRING_IS_EMPTY.matches(mit)) {
      var operator = ((MemberSelectExpressionTree) toStringInvocation.methodSelect()).operatorToken();
      var edit = JavaTextEdit.replaceBetweenTree(operator, mit, ".isEmpty()");

      QuickFixHelper.newIssue(context)
        .forRule(this)
        .onTree(mit)
        .withMessage(MESSAGE_IS_EMPTY)
        .withQuickFixes(() -> List.of(
          JavaQuickFix.newQuickFix("Replace with \"isEmpty()\"").addTextEdit(edit).build()
        ))
        .report();
    } else if (STRING_LENGTH.matches(mit) && isComparedToZero(mit)) {
      reportIssue(mit, MESSAGE_LENGTH);
    }
  }

  // example: "".equals(sb.toString()) and toStringInvocation=sb.toString() -> "".equals(sb.toString())
  private static Optional<MethodInvocationTree> argumentSide(MethodInvocationTree toStringInvocation) {
    return Optional.ofNullable(toStringInvocation.parent())
      .filter(ArgumentListTreeImpl.class::isInstance)
      .map(ArgumentListTreeImpl.class::cast)
      .map(ArgumentListTreeImpl::parent)
      .filter(MethodInvocationTree.class::isInstance)
      .map(MethodInvocationTree.class::cast);
  }

  // example: sb.toString().equals("") and toStringInvocation=sb.toString() -> sb.toString().equals("")
  private static Optional<MethodInvocationTree> methodSelectSide(MethodInvocationTree toStringInvocation) {
    return Optional.ofNullable(toStringInvocation.parent())
      .filter(MemberSelectExpressionTree.class::isInstance)
      .map(MemberSelectExpressionTree.class::cast)
      .map(MemberSelectExpressionTree::parent)
      .filter(MethodInvocationTree.class::isInstance)
      .map(MethodInvocationTree.class::cast);
  }

  private static boolean isEqualsWithEmptyString(MethodInvocationTree equalsInvocation) {
    Tree arg = equalsInvocation.arguments().get(0);

    return isEmptyString(arg) ||
      (equalsInvocation.methodSelect() instanceof MemberSelectExpressionTree sel && isEmptyString(sel.expression()));
  }

  private static boolean isComparedToZero(MethodInvocationTree lengthInvocation) {
    Tree parent = lengthInvocation.parent();
    if (parent != null && parent.is(Tree.Kind.EQUAL_TO, Tree.Kind.NOT_EQUAL_TO)) {
      BinaryExpressionTree binary = (BinaryExpressionTree) parent;
      return LiteralUtils.isZero(binary.rightOperand()) || LiteralUtils.isZero(binary.leftOperand());
    }
    return false;
  }
}
