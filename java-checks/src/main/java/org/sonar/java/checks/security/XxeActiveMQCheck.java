/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java.checks.security;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import static org.sonar.java.checks.security.TriggeringSecuringHelper.isInvocationOnVariable;
import static org.sonar.java.matcher.TypeCriteria.subtypeOf;
import static org.sonar.java.model.ExpressionUtils.getAssignedSymbol;

@Rule(key = "S5301")
public class XxeActiveMQCheck extends AbstractMethodDetection {
  private static final String CONSTRUCTOR = "<init>";

  private static final String MQ_CONNECTION_FACTORY_CLASS_NAME = "org.apache.activemq.ActiveMQConnectionFactory";

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Collections.singletonList(MethodMatcher.create().typeDefinition(MQ_CONNECTION_FACTORY_CLASS_NAME)
      .name(CONSTRUCTOR).withAnyParameters());
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    Tree enclosingMethod = ExpressionUtils.getEnclosingMethod(newClassTree);
    if (enclosingMethod == null) {
      return;
    }

    Optional<Symbol> assignedSymbol = getAssignedSymbol(newClassTree);
    assignedSymbol.ifPresent(symbol -> {
      MethodBodyVisitor visitor = new MethodBodyVisitor(symbol);
      enclosingMethod.accept(visitor);
      if (!visitor.foundCallsToSecuringMethods()) {
        reportIssue(newClassTree,
          "Secure this \"ActiveMQConnectionFactory\" by whitelisting the trusted packages using the \"setTrustedPackages\" method and "
            + "make sure the \"setTrustAllPackages\" is not set to true.");
      }
    });
  }

  private static class MethodBodyVisitor extends BaseTreeVisitor {

    private static final MethodMatcher SET_TRUSTED_PACKAGES = MethodMatcher.create()
      .typeDefinition(subtypeOf(MQ_CONNECTION_FACTORY_CLASS_NAME)).name("setTrustedPackages")
      .addParameter(TypeCriteria.anyType());

    private static final MethodMatcher SET_TRUST_ALL_PACKAGES = MethodMatcher.create()
      .typeDefinition(subtypeOf(MQ_CONNECTION_FACTORY_CLASS_NAME)).name("setTrustAllPackages")
      .parameters("boolean");

    private boolean hasTrustedPackages = false;
    private boolean hasTrustAllPackages = false;
    private boolean callArgumentsOfSetTrustedPackages = false;

    private Symbol variable;

    MethodBodyVisitor(Symbol variable) {
      this.variable = variable;
    }

    private boolean foundCallsToSecuringMethods() {
      return hasTrustedPackages && !hasTrustAllPackages;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree methodInvocation) {
      if (isInvocationOnVariable(methodInvocation, variable)) {
        Arguments arguments = methodInvocation.arguments();
        if (SET_TRUSTED_PACKAGES.matches(methodInvocation)) {
          hasTrustedPackages |= !arguments.get(0).is(Kind.NULL_LITERAL);
          callArgumentsOfSetTrustedPackages = true;
        } else if (SET_TRUST_ALL_PACKAGES.matches(methodInvocation)) {
          hasTrustAllPackages |= Boolean.TRUE.equals(arguments.get(0).asConstant(Boolean.class).orElse(null));
        }
      }
      super.visitMethodInvocation(methodInvocation);
      callArgumentsOfSetTrustedPackages = false;
    }

    @Override
    public void visitLiteral(LiteralTree tree) {
      if (callArgumentsOfSetTrustedPackages &&
        tree.is(Kind.STRING_LITERAL) &&
        "*".equals(LiteralUtils.trimQuotes(tree.value()))) {
        hasTrustAllPackages = true;
      }
      super.visitLiteral(tree);
    }
  }
}
