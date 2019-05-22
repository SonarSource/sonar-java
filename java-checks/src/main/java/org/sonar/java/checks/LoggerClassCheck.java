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

import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.MethodMatcherCollection;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S3416")
public class LoggerClassCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatcherCollection LOG_FACTORIES = MethodMatcherCollection.create(
    // covers slf4j, log4j, java.util.logging and perhaps many others
    MethodMatcher.create().typeDefinition(TypeCriteria.anyType()).name("getLogger").addParameter("java.lang.Class"),
    MethodMatcher.create().typeDefinition(TypeCriteria.anyType()).name("getLogger").addParameter("java.lang.String"),
    // Apache commons-logging
    MethodMatcher.create().typeDefinition("org.apache.commons.logging.LogFactory").name("getLog").addParameter("java.lang.Class"),
    MethodMatcher.create().typeDefinition("org.apache.commons.logging.LogFactory").name("getLog").addParameter("java.lang.String"),
    // sonar-api
    MethodMatcher.create().typeDefinition("org.sonar.api.utils.log.Loggers").name("get").addParameter("java.lang.Class"),
    MethodMatcher.create().typeDefinition("org.sonar.api.utils.log.Loggers").name("get").addParameter("java.lang.String")
  );

  private static final MethodMatcher CLAZZ_GETNAME = MethodMatcher.create()
    .typeDefinition("java.lang.Class").name("getName").withoutParameter();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
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
      && LOG_FACTORIES.anyMatch((MethodInvocationTree) initializer)) {
      ExpressionTree firstArg = ((MethodInvocationTree) initializer).arguments().get(0);
      Symbol classLiteral = classLiteral(firstArg);
      if (classLiteral != null && !clazz.equals(classLiteral)) {
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
