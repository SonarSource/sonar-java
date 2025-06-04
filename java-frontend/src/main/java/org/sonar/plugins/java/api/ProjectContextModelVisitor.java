package org.sonar.plugins.java.api;

import java.util.HashSet;
import java.util.List;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.plugins.java.api.tree.Tree;

public class ProjectContextModelVisitor extends IssuableSubscriptionVisitor {

  private final HashSet<String> springComponents = new HashSet<>();

  public ProjectContextModel buildProjectContextModel() {
    return new ProjectContextModel(springComponents);
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree instanceof ClassTreeImpl classTree) {
      visitClass(classTree);
    }
  }

  private void visitClass(ClassTreeImpl classTree) {
    if (classTree.modifiers().annotations().stream()
      .anyMatch(a -> a.symbolType().is("org.springframework.stereotype.Component"))) {
      springComponents.add(classTree.symbol().type().fullyQualifiedName());
    }
  }

}
