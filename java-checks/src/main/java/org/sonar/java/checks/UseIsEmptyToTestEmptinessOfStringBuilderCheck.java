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

import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.ast.parser.ArgumentListTreeImpl;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.model.LiteralUtils.isEmptyString;

@Rule(key = "S3033")
public class UseIsEmptyToTestEmptinessOfStringBuilderCheck extends AbstractMethodDetection {

  private static final MethodMatchers TO_STRING = MethodMatchers.create()
    .ofTypes("java.lang.StringBuilder", "java.lang.StringBuffer")
    .names("toString")
    .withAnyParameters()
    .build();
  private static final MethodMatchers EQUALS = MethodMatchers.create()
    .ofTypes("java.lang.String")
    .names("equals")
    .withAnyParameters()
    .build();
  private static final MethodMatchers IS_EMPTY = MethodMatchers.create()
    .ofTypes("java.lang.String")
    .names("isEmpty")
    .withAnyParameters()
    .build();
  private static final MethodMatchers LENGTH = MethodMatchers.create()
    .ofTypes("java.lang.String")
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
  protected void onMethodInvocationFound(MethodInvocationTree toStringInvocation) {
    MethodInvocationTree mit = argumentSide(toStringInvocation)
      .or(() -> methodSelectSide(toStringInvocation))
      .orElse(null);

    if (mit == null) {
      return;
    }

    if (EQUALS.matches(mit) && isEqualsWithEmptyString(mit)) {
      reportIssue(mit, MESSAGE_EQUALS);
    } else if (IS_EMPTY.matches(mit)) {
      reportIssue(mit, MESSAGE_IS_EMPTY);
    } else if (LENGTH.matches(mit) && isComparedToZero(mit)) {
      reportIssue(mit, MESSAGE_LENGTH);
    }
  }

  // example: "".equals(sb.toString()) and toStringInvocation=sb.toString() -> "".equals(sb.toString())
  private static Optional<MethodInvocationTree> argumentSide(MethodInvocationTree toStringInvocation) {
    Tree parent = toStringInvocation.parent();
    if (parent instanceof ArgumentListTreeImpl args && args.parent() instanceof MethodInvocationTree mit) {
      return Optional.of(mit);
    }
    return Optional.empty();
  }

  // example: sb.toString().equals("") and toStringInvocation=sb.toString() -> sb.toString().equals("")
  private static Optional<MethodInvocationTree> methodSelectSide(MethodInvocationTree toStringInvocation) {
    Tree parent = toStringInvocation.parent();
    if (parent instanceof MemberSelectExpressionTree select && select.parent() instanceof MethodInvocationTree mit) {
      return Optional.of(mit);
    }
    return Optional.empty();
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
