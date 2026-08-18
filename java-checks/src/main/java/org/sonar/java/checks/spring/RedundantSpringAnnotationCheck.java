/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.spring;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.checks.helpers.SpringUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S9341")
public class RedundantSpringAnnotationCheck extends IssuableSubscriptionVisitor {

  private static final String RESPONSE_BODY = "org.springframework.web.bind.annotation.ResponseBody";
  private static final String ENABLE_AUTO_CONFIGURATION = "org.springframework.boot.autoconfigure.EnableAutoConfiguration";
  private static final String COMPONENT_SCAN = "org.springframework.context.annotation.ComponentScan";
  private static final String SPRING_BOOT_CONFIGURATION = "org.springframework.boot.SpringBootConfiguration";
  private static final String EXTEND_WITH = "org.junit.jupiter.api.extension.ExtendWith";
  private static final String SPRING_EXTENSION = "org.springframework.test.context.junit.jupiter.SpringExtension";
  private static final String WEB_MVC_TEST = "org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest";
  private static final String DATA_JPA_TEST = "org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest";
  private static final String WEB_FLUX_TEST = "org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest";

  private static final List<RedundancyRule> REDUNDANCY_RULES = List.of(
    new RedundancyRule(SpringUtils.COMPONENT_ANNOTATION,
      List.of(SpringUtils.SERVICE_ANNOTATION, SpringUtils.REPOSITORY_ANNOTATION, SpringUtils.CONTROLLER_ANNOTATION, SpringUtils.CONFIGURATION_ANNOTATION), null),
    new RedundancyRule(SpringUtils.CONTROLLER_ANNOTATION,
      List.of(SpringUtils.REST_CONTROLLER_ANNOTATION), null),
    new RedundancyRule(RESPONSE_BODY,
      List.of(SpringUtils.REST_CONTROLLER_ANNOTATION), null),
    new RedundancyRule(SpringUtils.CONFIGURATION_ANNOTATION,
      List.of(SpringUtils.SPRING_BOOT_APP_ANNOTATION), null),
    new RedundancyRule(ENABLE_AUTO_CONFIGURATION,
      List.of(SpringUtils.SPRING_BOOT_APP_ANNOTATION), null),
    new RedundancyRule(COMPONENT_SCAN,
      List.of(SpringUtils.SPRING_BOOT_APP_ANNOTATION), RedundantSpringAnnotationCheck::isComponentScanWithoutCustomAttributes),
    new RedundancyRule(SPRING_BOOT_CONFIGURATION,
      List.of(SpringUtils.SPRING_BOOT_APP_ANNOTATION), null),
    new RedundancyRule(EXTEND_WITH,
      List.of(SpringUtils.SPRING_BOOT_TEST_ANNOTATION, WEB_MVC_TEST, DATA_JPA_TEST, WEB_FLUX_TEST),
      RedundantSpringAnnotationCheck::isExtendWithSpringExtension),
    new RedundancyRule(SpringUtils.TRANSACTIONAL_ANNOTATION,
      List.of(DATA_JPA_TEST), RedundantSpringAnnotationCheck::isTransactionalWithoutCustomAttributes)
  );

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    var classTree = (ClassTree) tree;
    Map<String, AnnotationTree> annotationsByFqn = collectAnnotations(classTree);

    for (RedundancyRule rule : REDUNDANCY_RULES) {
      AnnotationTree redundantAnnotation = annotationsByFqn.get(rule.redundantFqn);
      if (redundantAnnotation == null) {
        continue;
      }
      for (String impliedByFqn : rule.impliedByFqns) {
        AnnotationTree impliedByAnnotation = annotationsByFqn.get(impliedByFqn);
        if (impliedByAnnotation != null && passesSpecialCondition(rule, classTree, impliedByFqn)) {
          reportRedundancy(redundantAnnotation, impliedByAnnotation);
          break;
        }
      }
    }
  }

  private static Map<String, AnnotationTree> collectAnnotations(ClassTree classTree) {
    Map<String, AnnotationTree> map = new HashMap<>();
    for (AnnotationTree annotation : classTree.modifiers().annotations()) {
      String fqn = annotation.annotationType().symbolType().fullyQualifiedName();
      map.put(fqn, annotation);
    }
    return map;
  }

  private static boolean passesSpecialCondition(RedundancyRule rule, ClassTree classTree, String impliedByFqn) {
    if (rule.specialCondition == null) {
      return true;
    }
    return rule.specialCondition.test(classTree, impliedByFqn);
  }

  private void reportRedundancy(AnnotationTree redundantAnnotation, AnnotationTree impliedByAnnotation) {
    String redundantName = simpleName(redundantAnnotation);
    String impliedByName = simpleName(impliedByAnnotation);
    QuickFixHelper.newIssue(context)
      .forRule(this)
      .onTree(redundantAnnotation)
      .withMessage("Remove this \"@%s\" annotation, already implied by \"@%s\".", redundantName, impliedByName)
      .withSecondaries(List.of(
        new JavaFileScannerContext.Location("Already implied by this annotation.", impliedByAnnotation)))
      .report();
  }

  private static String simpleName(AnnotationTree annotation) {
    return annotation.annotationType().symbolType().name();
  }

  private static boolean isComponentScanWithoutCustomAttributes(ClassTree classTree, String impliedByFqn) {
    SymbolMetadata metadata = classTree.symbol().metadata();
    List<SymbolMetadata.AnnotationValue> values = metadata.valuesForAnnotation(COMPONENT_SCAN);
    return values == null || values.isEmpty();
  }

  private static boolean isTransactionalWithoutCustomAttributes(ClassTree classTree, String impliedByFqn) {
    SymbolMetadata metadata = classTree.symbol().metadata();
    List<SymbolMetadata.AnnotationValue> values = metadata.valuesForAnnotation(SpringUtils.TRANSACTIONAL_ANNOTATION);
    return values == null || values.isEmpty();
  }

  private static boolean isExtendWithSpringExtension(ClassTree classTree, String impliedByFqn) {
    SymbolMetadata metadata = classTree.symbol().metadata();
    List<SymbolMetadata.AnnotationValue> values = metadata.valuesForAnnotation(EXTEND_WITH);
    if (values == null) {
      return false;
    }
    for (SymbolMetadata.AnnotationValue av : values) {
      if (isOnlySpringExtensionClass(av.value())) {
        return true;
      }
    }
    return false;
  }

  private static boolean isOnlySpringExtensionClass(Object value) {
    if (value instanceof Symbol symbol) {
      return symbol.type().is(SPRING_EXTENSION);
    }
    if (value instanceof Object[] values) {
      // Skip mixed arrays like @ExtendWith({SpringExtension.class, MockitoExtension.class})
      // since the annotation cannot simply be removed
      return values.length == 1 && values[0] instanceof Symbol symbol && symbol.type().is(SPRING_EXTENSION);
    }
    return false;
  }

  private record RedundancyRule(String redundantFqn, List<String> impliedByFqns,
    BiPredicate<ClassTree, String> specialCondition) {
  }
}
