package org.sonar.java.prettyprint;

import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

public final class SubstitutionVisitor extends DeepCopyVisitor {

  private static final String SUBST_TARGET_PREFIX = "$";

  private final Map<String, Tree> substitutions;

  static Tree substitute(Tree tree, Map<String, Tree> substitutions){
    var visitor = new SubstitutionVisitor(substitutions);
    tree.accept(visitor);
    return visitor.popUniqueResult();
  }

  public SubstitutionVisitor(Map<String, Tree> substitutions) {
    for (Map.Entry<String, Tree> entry : substitutions.entrySet()) {
      if (!entry.getKey().startsWith(SUBST_TARGET_PREFIX)) {
        throw new IllegalArgumentException();
      }
    }
    this.substitutions = Map.copyOf(substitutions);
  }

  private @Nullable String nameIfSubstitutionTarget(MethodInvocationTree invocation) {
    var typeArgs = invocation.typeArguments();
    return (typeArgs == null || typeArgs.isEmpty())
      && invocation.arguments().isEmpty()
      && invocation.methodSelect() instanceof IdentifierTree idTree
      && idTree.name().startsWith(SUBST_TARGET_PREFIX)
      ? idTree.name()
      : null;
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    var name = nameIfSubstitutionTarget(tree);
    if (name == null) {
      super.visitMethodInvocation(tree);
    } else {
      pushResult(substitutions.get(name));
    }
  }

}
