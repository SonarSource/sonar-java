/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S6901")
public class VirtualThreadUnsupportedMethodsCheck extends AbstractMethodDetection implements JavaVersionAwareVisitor {

  private static final String ISSUE_MESSAGE = "Method '%s' is not supported on virtual threads.";
  private static final String SECONDARY_LOCATION_ISSUE_MESSAGE = "Virtual thread initialized here.";

  private static final MethodMatchers VIRTUAL_THREAD_BUILDER_METHODS = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes("java.lang.Thread$Builder$OfVirtual")
      .names("unstarted", "start")
      .addParametersMatcher("java.lang.Runnable").build(),
    MethodMatchers.create()
      .ofTypes("java.lang.Thread")
      .names("startVirtualThread")
      .addParametersMatcher("java.lang.Runnable").build()
  );

  private static final MethodMatchers VIRTUAL_THREAD_UNSUPPORTED_METHODS = MethodMatchers.create()
    .ofTypes("java.lang.Thread")
    .names("setDaemon", "setPriority", "getThreadGroup")
    .withAnyParameters()
    .build();

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava21Compatible();
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return VIRTUAL_THREAD_UNSUPPORTED_METHODS;
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    var memberSelect = (MemberSelectExpressionTree) mit.methodSelect();
    var expression = memberSelect.expression();
    var virtualThreadExpression = getVirtualThreadInitializer(expression);
    if (virtualThreadExpression.isPresent()) {
      reportIssue(
        memberSelect.identifier(),
        String.format(ISSUE_MESSAGE, memberSelect.identifier().name()),
        List.of(new JavaFileScannerContext.Location(SECONDARY_LOCATION_ISSUE_MESSAGE, ExpressionUtils.methodName(virtualThreadExpression.get()))),
        null);
    }
  }

  private static Optional<MethodInvocationTree> getVirtualThreadInitializer(ExpressionTree expression) {
    var isMit = getMethodInvocationAndReturningVirtualThread(expression);
    if (isMit.isPresent()) {
      return isMit;
    } else {
      return getIdentifierAndVirtualThread(expression);
    }
  }

  private static Optional<MethodInvocationTree> getIdentifierAndVirtualThread(ExpressionTree expression) {
    if (expression instanceof IdentifierTree identifier && identifier.symbol().declaration() instanceof VariableTree variableTree) {
      return getMethodInvocationAndReturningVirtualThread(variableTree.initializer());
    }
    return Optional.empty();
  }

  private static Optional<MethodInvocationTree> getMethodInvocationAndReturningVirtualThread(ExpressionTree expression) {
    if (expression instanceof MethodInvocationTree mit && VIRTUAL_THREAD_BUILDER_METHODS.matches(mit)) {
      return Optional.of(mit);
    }
    return Optional.empty();
  }

}
