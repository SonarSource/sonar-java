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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sonar.check.Rule;
import org.sonar.java.checks.SystemOutOrErrUsageCheck;
import org.sonar.java.checks.helpers.TreeHelper;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.model.JUtils.isLocalVariable;

@Rule(key = "S6243")
public class AwsLambdaCallsLambdaCheck extends AwsReusableResourcesInitializedOnceCheck {

  private static final List<Tree.Kind> NODES_TO_VISIT = List.of(Tree.Kind.METHOD);

  private static final MethodMatchers HANDLE_REQUEST_MATCHER = MethodMatchers.or(
    MethodMatchers.create()
      .ofSubTypes("com.amazonaws.services.lambda.runtime.RequestHandler")
      .names("handleRequest")
      .addParametersMatcher("java.lang.Object", "com.amazonaws.services.lambda.runtime.Context")
      .build(),
    MethodMatchers.create()
      .ofSubTypes("com.amazonaws.services.lambda.runtime.RequestStreamHandler")
      .names("handleRequest")
      .addParametersMatcher("java.io.InputStream", "java.io.OutputStream", "com.amazonaws.services.lambda.runtime.Context")
      .build());

  private static final String MESSAGE = "Avoid synchronous calls to other lambdas";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return NODES_TO_VISIT;
  }

  @Override
  public void visitNode(Tree handleRequestMethodTree) {
    var methodTree = (MethodTree) handleRequestMethodTree;
    if (!HANDLE_REQUEST_MATCHER.matches(methodTree)) return;

    var finder = new InvokeFinder();
    methodTree.accept(finder);
    TreeHelper.findReachableMethodsInSameFile(methodTree).forEach(tree -> tree.accept(finder));

    finder.getInvokeCalls().forEach((call, msg) -> reportIssue(ExpressionUtils.methodName(call), msg));
  }

  private static class InvokeFinder extends BaseTreeVisitor {
    private final Map<MethodInvocationTree, String> invokeInvocations = new IdentityHashMap<>();

    private static final MethodMatchers INVOKE_MATCHERS = MethodMatchers.create()
      .ofSubTypes("com.amazonaws.services.lambda.AWSLambda").names("invoke")
      .addParametersMatcher("com.amazonaws.services.lambda.model.InvokeRequest").build();

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      methodCallsInvoke(tree).ifPresent(msgPart -> invokeInvocations.put(tree, msgPart));
    }

    private static Optional<String> methodCallsInvoke(MethodInvocationTree tree) {
      if (INVOKE_MATCHERS.matches(tree)) {
        // TODO: get the argument of this call 'tree'., i.e. it is the invokeRequest var
        // find the usages of the arg
        // is_compliant if - subject of a call, - arg of the current method we are in, - has calls to 'setInvocationType' or 'withInvocationType'


        // Because of the INVOKE_MATCHER, we know there is one argument and it is of type IdentifierTree
        IdentifierTree invokeRequest = (IdentifierTree)tree.arguments().get(0);
        // Put .usages() and .declaration() together
        // filter on isLocalVariable
        // filter if current usage = invokeRequest , i.e. the point where all started
        List<IdentifierTree> usages = invokeRequest.symbol().usages();
        List<IdentifierTree> res = usages.stream().filter(u -> isLocalVariable(u.symbol()) && !u.equals(invokeRequest)).collect(Collectors.toList());
        return Optional.of(MESSAGE);
      } else {
        return Optional.empty();
      }
    }

    public Map<MethodInvocationTree, String> getInvokeCalls() {
      return invokeInvocations;
    }
  }
}
