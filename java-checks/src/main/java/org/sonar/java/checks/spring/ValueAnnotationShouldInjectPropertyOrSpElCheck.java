package org.sonar.java.checks.spring;

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S604")
public class ValueAnnotationShouldInjectPropertyOrSpElCheck extends IssuableSubscriptionVisitor {

  private static final String SPRING_VALUE = "org.springframework.beans.factory.annotation.Value";
  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.ANNOTATION);
  }

  @Override
  public void visitNode(Tree tree) {
    AnnotationTree ann = (AnnotationTree) tree;
    List<ExpressionTree> arguments = ann.arguments();
    if(ann.symbolType().is(SPRING_VALUE) && !arguments.isEmpty() && arguments.get(0).is(Tree.Kind.STRING_LITERAL)){
      LiteralTree literal = (LiteralTree)arguments.get(0);
      String value = literal.value();

      if(!isPropertyName(value) && !isSpEL(value)){
        reportIssue(ann, "Only a simple value is injected, replace the \"@Value\" annotation with a standard field initialization.");
      }

    }
  }

  private static boolean isPropertyName(String value){
    return value.startsWith("\"${") && value.endsWith("}\"");
  }

  private static boolean isSpEL(String value){
    return value.startsWith("\"#{") && value.endsWith("}\"");
  }


}
