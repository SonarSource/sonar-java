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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S3416")
public class LoggerClassCheck extends IssuableSubscriptionVisitor {

  private static final String CLASS = "java.lang.Class";
  public static final String STRING = "java.lang.String";

  private static final MethodMatchers LOG_FACTORIES = MethodMatchers.or(
    // covers slf4j, log4j, java.util.logging and perhaps many others
    MethodMatchers.create()
      .ofAnyType()
      .names("getLogger")
      .addParametersMatcher(CLASS)
      .addParametersMatcher(STRING)
      .build(),
    // Apache commons-logging
    MethodMatchers.create()
      .ofTypes("org.apache.commons.logging.LogFactory")
      .names("getLog")
      .addParametersMatcher(CLASS)
      .addParametersMatcher(STRING)
      .build(),
    // sonar-api
    MethodMatchers.create()
      .ofTypes("org.sonar.api.utils.log.Loggers")
      .names("get")
      .addParametersMatcher(CLASS)
      .addParametersMatcher(STRING)
      .build());

  private static final MethodMatchers CLAZZ_GETNAME = MethodMatchers.create()
    .ofTypes(CLASS)
    .names("getName")
    .addWithoutParametersMatcher()
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    Symbol.TypeSymbol clazz = ((ClassTree) tree).symbol();
    clazz.memberSymbols().stream()
      .filter(Symbol::isVariableSymbol)
      .map(Symbol.VariableSymbol.class::cast)
      .map(Symbol.VariableSymbol::declaration)
      .filter(Objects::nonNull)
      .map(VariableTree::initializer)
      .filter(Objects::nonNull)
      .forEach(initializer -> checkField(clazz, initializer));
  }

  private void checkField(Symbol.TypeSymbol clazz, ExpressionTree initializer) {
    if (initializer.is(Tree.Kind.METHOD_INVOCATION) && LOG_FACTORIES.matches((MethodInvocationTree) initializer)) {
      ExpressionTree firstArg = ((MethodInvocationTree) initializer).arguments().get(0);
      Symbol classLiteral = classLiteral(firstArg);
      if (classLiteral != null && !clazz.type().erasure().equals(classLiteral.type().erasure())) {
        reportIssue(firstArg, "Update this logger to use \"" + clazz.name() + ".class\".");
      }
    }
  }

  @CheckForNull
  private static Symbol classLiteral(ExpressionTree expression) {
    if (expression.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mset = (MemberSelectExpressionTree) expression;
      if ("class".equals(mset.identifier().name())) {
        return mset.expression().symbolType().symbol();
      }
    }
    if (expression.is(Tree.Kind.METHOD_INVOCATION) && CLAZZ_GETNAME.matches(((MethodInvocationTree) expression))) {
      MethodInvocationTree mit = (MethodInvocationTree) expression;
      if (mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
        return classLiteral(((MemberSelectExpressionTree) mit.methodSelect()).expression());
      }
    }
    return null;
  }

}
