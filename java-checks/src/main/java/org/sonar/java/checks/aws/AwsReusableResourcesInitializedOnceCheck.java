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
package org.sonar.java.checks.aws;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.TreeHelper;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6243")
public class AwsReusableResourcesInitializedOnceCheck extends AbstractAwsMethodVisitor {

  private static final String MESSAGE_TEMPLATE = "Instantiate this %s outside the Lambda function.";
  private static final String MESSAGE_CLIENT = String.format(MESSAGE_TEMPLATE, "client");
  private static final String MESSAGE_DATABASE_CONNECTION = String.format(MESSAGE_TEMPLATE, "database connection");

  private static final String SDK_CLIENT_TYPE = "software.amazon.awssdk.core.SdkClient";

  private static final MethodMatchers CREATION_METHODS_MATCHERS = MethodMatchers.create().ofAnyType().names("build").withAnyParameters().build();

  private static final MethodMatchers CONNECTION_CREATION_MATCHERS = MethodMatchers.create()
    .ofTypes("java.sql.DriverManager").names("getConnection").withAnyParameters().build();

  @Override
  void visitReachableMethodsFromHandleRequest(Set<MethodTree> methodTrees) {
    var finder = new ResourceCreationFinder();

    methodTrees.forEach(m -> m.accept(finder));

    finder.getBuilderInvocations().forEach((call, msg) ->
      reportIssue(ExpressionUtils.methodName(call), msg)
    );
    finder.getConstructorInvocations().forEach((call, msg) ->
      reportIssue(call.identifier(), msg)
    );
  }

  private static class ResourceCreationFinder extends BaseTreeVisitor {
    private final Map<MethodInvocationTree, String> builderInvocations = new IdentityHashMap<>();
    private final Map<NewClassTree, String> constructorInvocations = new IdentityHashMap<>();

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      methodCreatesResource(tree).ifPresent(msgPart -> builderInvocations.put(tree, msgPart));
    }

    @Override
    public void visitNewClass(NewClassTree tree) {
      if (tree.symbolType().isSubtypeOf(SDK_CLIENT_TYPE)) {
        constructorInvocations.put(tree, MESSAGE_CLIENT);
      }
    }

    private static Optional<String> methodCreatesResource(MethodInvocationTree tree) {
      if (CREATION_METHODS_MATCHERS.matches(tree) && tree.symbolType().isSubtypeOf(SDK_CLIENT_TYPE)) {
        return Optional.of(MESSAGE_CLIENT);
      } else if (CONNECTION_CREATION_MATCHERS.matches(tree)) {
        return Optional.of(MESSAGE_DATABASE_CONNECTION);
      } else {
        return Optional.empty();
      }
    }

    public Map<MethodInvocationTree, String> getBuilderInvocations() {
      return builderInvocations;
    }

    public Map<NewClassTree, String> getConstructorInvocations() {
      return constructorInvocations;
    }
  }
}
