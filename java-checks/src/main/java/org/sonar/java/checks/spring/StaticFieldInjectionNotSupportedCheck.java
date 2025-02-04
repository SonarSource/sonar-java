package org.sonar.java.checks.spring;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S7178")
public class StaticFieldInjectionNotSupportedCheck extends IssuableSubscriptionVisitor {
  private static final Set<String> INJECTIONS_ANNOTATIONS = Set.of(
    "javax.inject.Inject",
    "org.springframework.beans.factory.annotation.Autowired",
    "jakarta.inject.Inject",
    "org.springframework.beans.factory.annotation.Value"
  );

  private static final String STATIC_FIELD_MESSAGE = "Remove the injection annotation targeting the static field.";
  private static final String STATIC_METHOD_MESSAGE = "Remove the injection annotation targeting the static method.";
  private static final String STATIC_PARAMETER_MESSAGE = "Remove the injection annotation targeting the parameter.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD, Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree instanceof ClassTree clazz) {
      Stream<VariableTree> staticFields = clazz.members().stream()
        .filter(child -> child instanceof VariableTree v && ModifiersUtils.hasModifier(v.modifiers(), Modifier.STATIC))
        .map(field -> (VariableTree) field);

      staticFields
        .map(v -> injectionAnnotations(v.modifiers()))
        .filter(anns -> !anns.isEmpty())
        .forEach(anns -> {
          anns.forEach(ann -> reportIssue(ann, STATIC_FIELD_MESSAGE));
        });

    } else if (tree instanceof MethodTree method && ModifiersUtils.hasModifier(method.modifiers(), Modifier.STATIC)) {

      //report on method annotations
      injectionAnnotations(method.modifiers())
        .forEach(ann -> reportIssue(ann, STATIC_METHOD_MESSAGE));

      //report on parameters
      method.parameters().stream()
        .map(p -> injectionAnnotations(p.modifiers()))
        .filter(anns -> !anns.isEmpty())
        .forEach(anns -> anns.forEach(ann -> reportIssue(ann, STATIC_PARAMETER_MESSAGE)));

    }
  }

  private static List<AnnotationTree> injectionAnnotations(ModifiersTree m) {
    return m.annotations().stream()
      .filter(ann ->
        INJECTIONS_ANNOTATIONS.contains(ann.annotationType().symbolType().fullyQualifiedName()))
      .toList();
  }
}
