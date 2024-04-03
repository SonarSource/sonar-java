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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4423")
public class WeakSSLContextCheck extends IssuableSubscriptionVisitor {

  private static final String ISSUE_MESSAGE = "Change this code to use a stronger protocol.";
  private static final String SECONDARY_LOCATION_MESSAGE = "Other weak protocol.";

  private static final Set<String> STRONG_PROTOCOLS = new HashSet<>(Arrays.asList("TLSv1.2", "DTLSv1.2", "TLSv1.3", "DTLSv1.3"));
  private static final Set<String> STRONG_AFTER_JAVA_8 = new HashSet<>(Arrays.asList("TLS", "DTLS"));
  private static final Set<String> WEAK_FOR_OK_HTTP = new HashSet<>(Arrays.asList("TLSv1", "TLSv1.1", "TLS_1_0", "TLS_1_1"));
  private static final Set<String> WEAK_FOR_SET_ENABLED_PROTOCOLS = new HashSet<>(Set.of("TLSv1.0", "TLSv1.1"));

  private static final MethodMatchers SSLCONTEXT_GETINSTANCE_MATCHER = MethodMatchers.create()
    .ofTypes("javax.net.ssl.SSLContext")
    .names("getInstance")
    .withAnyParameters()
    .build();

  private static final MethodMatchers OK_HTTP_TLS_VERSION = MethodMatchers.create()
    .ofTypes("okhttp3.ConnectionSpec$Builder")
    .names("tlsVersions")
    .withAnyParameters()
    .build();

  private static final MethodMatchers OPTIONS_ENABLED_PROTOCOLS = MethodMatchers.create()
    .ofTypes("org.springframework.boot.autoconfigure.ssl.SslBundleProperties$Options")
    .names("setEnabledProtocols")
    .addParametersMatcher("java.util.Set")
    .build();

  private boolean javaVersionNotSetOr8OrHigher;

  @Override
  public void setContext(JavaFileScannerContext context) {
    javaVersionNotSetOr8OrHigher = context.getJavaVersion().isJava8Compatible();
    super.setContext(context);
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree mit = (MethodInvocationTree) tree;
    Arguments arguments = mit.arguments();
    if (SSLCONTEXT_GETINSTANCE_MATCHER.matches(mit)) {
      ExpressionTree firstArgument = arguments.get(0);
      firstArgument.asConstant(String.class).ifPresent(protocol -> {
        if (!isStrongProtocol(protocol)) {
          reportIssue(firstArgument, ISSUE_MESSAGE);
        }
      });
    } else if (OK_HTTP_TLS_VERSION.matches(mit)) {
      List<ExpressionTree> unsecureVersions = getUnsecureVersionsInArguments(arguments);
      if (!unsecureVersions.isEmpty()) {
        List<JavaFileScannerContext.Location> secondaries = unsecureVersions.stream()
          .skip(1)
          .map(secondary -> new JavaFileScannerContext.Location(SECONDARY_LOCATION_MESSAGE, secondary))
          .toList();
        reportIssue(unsecureVersions.get(0), ISSUE_MESSAGE, secondaries, null);
      }
    } else if (OPTIONS_ENABLED_PROTOCOLS.matches(mit)) {
      ExpressionTree argument = arguments.get(0);
      if (argument instanceof MethodInvocationTree methodInvocation) {
        List<JavaFileScannerContext.Location> secondaryLocations = methodInvocation.arguments().stream()
          .filter(arg -> {
            var argValue = ExpressionUtils.resolveAsConstant(arg);
            return argValue != null && WEAK_FOR_SET_ENABLED_PROTOCOLS.contains(argValue);
          })
          .map(arg -> new JavaFileScannerContext.Location(SECONDARY_LOCATION_MESSAGE, arg))
          .toList();
        if (!secondaryLocations.isEmpty()) {
          reportIssue(((MemberSelectExpressionTree) mit.methodSelect()).identifier(), ISSUE_MESSAGE, secondaryLocations, null);
        }
      }
    }
  }

  private boolean isStrongProtocol(String protocol) {
    // A project with a version not set is very likely to be >= Java 8
    return STRONG_PROTOCOLS.contains(protocol) || (javaVersionNotSetOr8OrHigher && STRONG_AFTER_JAVA_8.contains(protocol));
  }

  private static List<ExpressionTree> getUnsecureVersionsInArguments(Arguments arguments) {
    return arguments.stream()
      .filter(WeakSSLContextCheck::isUnsecureVersion)
      .toList();
  }

  private static boolean isUnsecureVersion(ExpressionTree expressionTree) {
    String argumentValue = null;
    Optional<String> stringArgument = expressionTree.asConstant(String.class);
    if (stringArgument.isPresent()) {
      argumentValue = stringArgument.get();
    } else if (expressionTree.is(Tree.Kind.IDENTIFIER)) {
      argumentValue = ((IdentifierTree) expressionTree).name();
    } else if (expressionTree.is(Tree.Kind.MEMBER_SELECT)) {
      argumentValue = ((MemberSelectExpressionTree) expressionTree).identifier().name();
    }
    return WEAK_FOR_OK_HTTP.contains(argumentValue);
  }

}
