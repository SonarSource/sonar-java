package org.sonar.java.checks.springcontext;

import com.google.gson.Gson;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.PackageDeclarationTree;
import org.sonar.plugins.java.api.tree.TypeTree;

public class BeanDefinitionCollector extends BaseTreeVisitor {

  private static final String SPRING_BEAN_ANNOTATION_FQN = "org.springframework.context.annotation.Bean";

  private String currentPackageName;

  @Override
  public void visitCompilationUnit(CompilationUnitTree tree) {
    PackageDeclarationTree packageDeclaration = tree.packageDeclaration();
    if (packageDeclaration != null) {
      currentPackageName = packageDeclaration.packageName().asConstant(String.class).orElse("");
    }
  }

  @Override
  public void visitMethod(MethodTree methodTree) {
    String className = methodTree.parent().symbol().name();
    if (methodTree.modifiers().annotations().stream()
      .map(AnnotationTree::annotationType)
      .anyMatch(BeanDefinitionCollector::isTypeSpringBean)) {
      String methodName = methodTree.simpleName().name();
    }
  }

  private static boolean isTypeSpringBean(TypeTree typeTree) {

    return SPRING_BEAN_ANNOTATION_FQN.equals(typeTree.symbolType().fullyQualifiedName());
  }

  record SpringBeanInfo(String name, String className, String packageName) {
    public String toJson() {
      return new Gson().toJson(this);
    }
  }

}
