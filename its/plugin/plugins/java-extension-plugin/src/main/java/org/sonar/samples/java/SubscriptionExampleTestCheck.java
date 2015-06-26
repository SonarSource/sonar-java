package org.sonar.samples.java;

import java.util.List;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.Tree;
import com.google.common.collect.ImmutableList;

@Rule(key = "subscriptionexampletestcheck", priority = Priority.MINOR, name = "SubscriptionExampleTestCheck", description = "SubscriptionExampleTestCheck")
public class SubscriptionExampleTestCheck extends IssuableSubscriptionVisitor {

    @Override
    public List<Tree.Kind> nodesToVisit() {
        return ImmutableList.of(Tree.Kind.METHOD);
    }

    @Override
    public void visitNode(final Tree tree) {
        addIssue(tree, "Issue on test methods");
    }
}
