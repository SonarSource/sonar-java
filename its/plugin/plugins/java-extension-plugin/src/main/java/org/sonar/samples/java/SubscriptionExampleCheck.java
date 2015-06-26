package org.sonar.samples.java;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "subscriptionexamplecheck", priority = Priority.MINOR, name = "SubscriptionExampleCheck", description = "SubscriptionExampleCheck")
public class SubscriptionExampleCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    addIssue(tree, "Issue on methods");
  }
}
