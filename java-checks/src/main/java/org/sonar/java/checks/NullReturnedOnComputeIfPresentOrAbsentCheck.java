package org.sonar.java.checks;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6104")
public class NullReturnedOnComputeIfPresentOrAbsentCheck extends IssuableSubscriptionVisitor {
  public static final int REMEDIATION_COST_IN_MINUTES = 10;
  public static final String MESSAGE = "Use \"Map.containsKey(key)\" followed by \"Map.put(key, null)\" to add null values.";
  private static final MethodMatchers COMPUTE_IF_PRESENT = MethodMatchers
    .create()
    .ofTypes("java.util.Map")
    .names("computeIfPresent")
    .addParametersMatcher(MethodMatchers.ANY, MethodMatchers.ANY)
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree methodInvocation = (MethodInvocationTree) tree;
    if (COMPUTE_IF_PRESENT.matches(methodInvocation)) {
      inspectComputeIfPresent(methodInvocation);
    }
    //TODO Add a branch for isComputeIfAbsent
  }

  public void inspectComputeIfPresent(MethodInvocationTree invocation) {
    Arguments arguments = invocation.arguments();
    if (arguments.size() < 2) {
      return;
    }
    returnsNullExplicitly(arguments.get(1))
      .ifPresent((tree) -> reportIssue(invocation,
        MESSAGE,
        Collections.singletonList(new JavaFileScannerContext.Location("", tree)),
        null));
  }

  public static Optional<Tree> returnsNullExplicitly(Tree tree) {
    if (tree.is(Tree.Kind.LAMBDA_EXPRESSION)) {
      Tree body = ((LambdaExpressionTree) tree).body();
      if (body.is(Tree.Kind.NULL_LITERAL)) {
        return Optional.of(body);
      }
    }
    return Optional.empty();
  }
}
