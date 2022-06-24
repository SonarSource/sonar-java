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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S6241")
public class AwsRegionShouldBeSetExplicitlyCheck extends IssuableSubscriptionVisitor {

  private static final String SDK_CLIENT_TYPE = "software.amazon.awssdk.core.SdkClient";
  private static final String SDK_CLIENT_BUILDER_TYPE = "software.amazon.awssdk.utils.builder.SdkBuilder";
  private static final String AWS_CLIENT_BUILDER_TYPE = "software.amazon.awssdk.awscore.client.builder.AwsClientBuilder";
  private static final MethodMatchers BUILD_METHOD = MethodMatchers.create()
    .ofSubTypes(SDK_CLIENT_BUILDER_TYPE)
    .names("build")
    .addWithoutParametersMatcher()
    .build();

  private static final MethodMatchers REGION_METHOD = MethodMatchers.create()
    .ofSubTypes(AWS_CLIENT_BUILDER_TYPE)
    .names("region")
    .addParametersMatcher("software.amazon.awssdk.regions.Region")
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree invocation = (MethodInvocationTree) tree;
    if (!BUILD_METHOD.matches(invocation)) {
      return;
    }
    // We first look for a call to region within the same chain of calls.
    if (setsRegion(invocation)) {
      return;
    }
    // If the call to build is made on a builder variable, we look into initialization and usages for a call to region
    Optional<VariableTree> declaration = getDeclaration(invocation);
    if (declaration.isPresent()) {
      VariableTree actualDeclaration = declaration.get();
      ExpressionTree initializer = actualDeclaration.initializer();
      // If the builder variable is not initialized using a method call, we return
      if (!initializer.is(Tree.Kind.METHOD_INVOCATION)) {
        return;
      }
      MethodInvocationTree initializationChain = (MethodInvocationTree) initializer;
      if (setsRegion(initializationChain)) {
        return;
      }
      // If no call to region is found in the call, we go to the other usages
      // If one of the usages is passing the builder to a method, we assume it might set there
      boolean regionIsSet = actualDeclaration.symbol().usages().stream()
        .anyMatch(usage -> isPassedAsArgument(usage) || setsRegion(usage));
      if (regionIsSet) {
        return;
      }
      reportIssue(actualDeclaration, "Set the region explicitly on this builder.");
    } else {
      reportIssue(invocation, "Set the region explicitly on this builder.");
    }
  }

  /**
   * Crawls up a call chain using methodSelect to determine whether one of the methods called sets the region.
   *
   * @param invocation The first in the chain to inspect
   * @return True if the region is potentially set by one of the method calls in the chain
   */
  private static boolean setsRegion(MethodInvocationTree invocation) {
    CallChainVisitor visitor = new CallChainVisitor(REGION_METHOD);
    invocation.accept(visitor);
    return visitor.methodIsInvoked;
  }

  /**
   * Crawls down the method calls following an SdkBuilder identifier to check if one of them returns sets the region.
   *
   * @param identifier The SdkBuilder to check
   * @return True if one of the method calls sets the region. False otherwise
   */
  private static boolean setsRegion(IdentifierTree identifier) {
    ReverseCallChainVisitor visitor = new ReverseCallChainVisitor(REGION_METHOD);
    identifier.accept(visitor);
    return visitor.methodIsInvoked;
  }

  private static Optional<VariableTree> getDeclaration(MethodInvocationTree terminalCall) {
    ExpressionTree expression = terminalCall.methodSelect();
    while (expression.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree memberSelectExpressionTree = (MemberSelectExpressionTree) expression;
      ExpressionTree currentExpression = memberSelectExpressionTree.expression();
      if (currentExpression.is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree identifier = (IdentifierTree) currentExpression;
        Tree declaration = identifier.symbol().declaration();
        if (declaration != null && declaration.is(Tree.Kind.VARIABLE)) {
          return Optional.of((VariableTree) declaration);
        }
      }
      if (!currentExpression.is(Tree.Kind.METHOD_INVOCATION)) {
        return Optional.empty();
      }
      MethodInvocationTree currentInvocation = (MethodInvocationTree) currentExpression;
      expression = currentInvocation.methodSelect();
    }
    return Optional.empty();
  }

  private static boolean isPassedAsArgument(IdentifierTree identifier) {
    Tree parent = identifier.parent();
    return parent != null && parent.is(Tree.Kind.ARGUMENTS);
  }

  private static class CallChainVisitor extends BaseTreeVisitor {
    protected boolean methodIsInvoked;
    private MethodMatchers target;

    CallChainVisitor(MethodMatchers matcher) {
      this.target = matcher;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (inspectCall(tree)) {
        return;
      }
      ExpressionTree expression = tree.methodSelect();
      expression.accept(this);
    }

    protected boolean inspectCall(MethodInvocationTree invocation) {
      methodIsInvoked = target.matches(invocation) || !methodBelongsToSdk(invocation);
      return methodIsInvoked;
    }

    /**
     * Tests if the method invoked is owned a child of the SDKBuilder or the SDKClient.
     * The result can be used to decide whether the method sets the region on the builder.
     *
     * @param invocation The method invocation to inspect
     * @return True if the method belongs to SDKBuilder or the SDKClient
     */
    private static boolean methodBelongsToSdk(MethodInvocationTree invocation) {
      Symbol owner = invocation.symbol().owner();
      return owner.type().isSubtypeOf(SDK_CLIENT_BUILDER_TYPE) ||
        owner.type().isSubtypeOf(SDK_CLIENT_TYPE);
    }
  }

  private static class ReverseCallChainVisitor extends CallChainVisitor {
    ReverseCallChainVisitor(MethodMatchers matcher) {
      super(matcher);
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      Tree parent = tree.parent();
      if (parent != null && parent.is(Tree.Kind.MEMBER_SELECT)) {
        parent.accept(this);
      }
    }

    @Override
    public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
      Tree parent = tree.parent();
      if (parent != null && parent.is(Tree.Kind.METHOD_INVOCATION)) {
        parent.accept(this);
      }
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (inspectCall(tree)) {
        return;
      }
      Tree parent = tree.parent();
      if (parent != null && parent.is(Tree.Kind.MEMBER_SELECT)) {
        parent.accept(this);
      }
    }
  }
}
