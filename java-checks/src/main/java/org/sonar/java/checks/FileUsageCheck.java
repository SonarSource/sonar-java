package org.sonar.java.checks;

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

/**
 * Suggest replacing legacy <code>java.io.File</code> with <code>java.nio.file.Path</code>.
 */
@Rule(key = "S7208")
public class FileUsageCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  private static final MethodMatchers FILE_MATCHER =
    MethodMatchers.create()
      .ofTypes("java.io.File")
      .constructor()
      .withAnyParameters()
      .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree instanceof NewClassTree classTree) {
      if (FILE_MATCHER.matches(classTree)) {
        reportIssue(classTree, "Replace with java.nio.file.Path");
      }
    }
  }

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava12Compatible();
  }
}
