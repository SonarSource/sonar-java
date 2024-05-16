package org.sonar.java.checks.prettyprint;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

public final class SubstitutionVisitor extends BaseTreeVisitor {

  private static final String SUBST_TARGET_PREFIX = "$";

  private final Map<String, Tree> substitutions;

  public SubstitutionVisitor(Map<String, Tree> substitutions) {
    for (Map.Entry<String, Tree> entry : substitutions.entrySet()) {
      if (!entry.getKey().startsWith(SUBST_TARGET_PREFIX)){
        throw new IllegalArgumentException();
      }
    }
    this.substitutions = Map.copyOf(substitutions);
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {

    if (isSubstitutionTarget(tree)){
      // FIXME ugly
      var name = ((IdentifierTree) tree.methodSelect()).name();
      var repl = Objects.requireNonNull(substitutions.get(name));
      var parent = tree.parent();
      if (parent instanceof List ls){
        ls.set(ls.indexOf(tree), repl);
      } else {
        var fields = parent.getClass().getFields();
        for (Field field : fields) {
          field.setAccessible(true);
          try {
            if (field.get(parent) == tree) {
              field.set(parent, repl);
              break;
            }
          } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
          }
        }
      }

    }

    super.visitMethodInvocation(tree);
  }

  private boolean isSubstitutionTarget(MethodInvocationTree invocation) {
    var typeArgs = invocation.typeArguments();
    return (typeArgs == null || typeArgs.isEmpty())
      && invocation.arguments().isEmpty()
      && invocation.methodSelect() instanceof IdentifierTree idTree
      && idTree.name().startsWith(SUBST_TARGET_PREFIX);
  }

}
