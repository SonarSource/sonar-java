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
package org.sonar.java.checks.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.check.Rule;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.checks.helpers.CredentialMethod;
import org.sonar.java.checks.helpers.CredentialMethodsLoader;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.checks.helpers.HardcodedStringExpressionChecker.*;

@Rule(key = "S6437")
public class HardCodedCredentialsShouldNotBeUsedCheck extends IssuableSubscriptionVisitor {
  public static final String CREDENTIALS_METHODS_FILE = "/org/sonar/java/checks/security/S6437-methods.json";

  private static final Logger LOG = Loggers.get(HardCodedCredentialsShouldNotBeUsedCheck.class);

  private static final String ISSUE_MESSAGE = "Revoke and change this password, as it is compromised.";

  private Map<String, List<CredentialMethod>> methods;

  public HardCodedCredentialsShouldNotBeUsedCheck() {
    this(CREDENTIALS_METHODS_FILE);
  }

  @VisibleForTesting
  HardCodedCredentialsShouldNotBeUsedCheck(String resourcePath) {
    try {
      methods = CredentialMethodsLoader.load(resourcePath);
    } catch (IOException e) {
      LOG.error(e.getMessage());
      methods = Collections.emptyMap();
    }
  }

  public Map<String, List<CredentialMethod>> getMethods() {
    return this.methods;
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    String methodName;
    boolean isConstructor = tree.is(Tree.Kind.NEW_CLASS);
    if (isConstructor) {
      NewClassTree newClass = (NewClassTree) tree;
      methodName = newClass.symbolType().name();
    } else {
      MethodInvocationTree invocation = (MethodInvocationTree) tree;
      methodName = invocation.symbol().name();
    }
    List<CredentialMethod> candidates = methods.get(methodName);
    if (candidates == null) {
      return;
    }
    for (CredentialMethod candidate : candidates) {
      MethodMatchers matcher = candidate.methodMatcher();
      if (isConstructor) {
        NewClassTree constructor = (NewClassTree) tree;
        if (matcher.matches(constructor)) {
          checkArguments(constructor.arguments(), candidate);
        }
      } else {
        MethodInvocationTree invocation = (MethodInvocationTree) tree;
        if (matcher.matches(invocation)) {
          checkArguments(invocation.arguments(), candidate);
        }
      }
    }
  }

  private void checkArguments(Arguments arguments, CredentialMethod method) {
    for (int targetArgumentIndex : method.indices) {
      ExpressionTree argument = arguments.get(targetArgumentIndex);
      var secondaryLocations = new ArrayList<JavaFileScannerContext.Location>();
      if (isExpressionDerivedFromPlainText(argument, secondaryLocations, new HashSet<>())) {
        reportIssue(argument, ISSUE_MESSAGE, secondaryLocations, null);
      }
    }
  }

}
