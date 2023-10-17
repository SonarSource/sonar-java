package org.sonar.java.checks.spring;

import java.util.List;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.Tree;

public class ValueAnnotationShouldInjectPropertyOrSpElCheck extends IssuableSubscriptionVisitor {
  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.ANNOTATION);
  }

  @Override
  public void visitNode(Tree tree) {

  }


}
