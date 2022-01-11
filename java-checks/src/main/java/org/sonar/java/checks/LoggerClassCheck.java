/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import java.util.Collections;
import java.util.List;
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
      .forEach(field -> checkField(clazz, (Symbol.VariableSymbol) field));
  }

  private void checkField(Symbol.TypeSymbol clazz, Symbol.VariableSymbol field) {
    VariableTree declaration = field.declaration();
    if (declaration == null) {
      return;
    }
    ExpressionTree initializer = declaration.initializer();
    if (initializer != null && initializer.is(Tree.Kind.METHOD_INVOCATION)
      && LOG_FACTORIES.matches((MethodInvocationTree) initializer)) {
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
      if (mset.identifier().name().equals("class")) {
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
