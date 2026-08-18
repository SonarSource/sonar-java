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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.checks.helpers.SpringUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
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
      List.of(SpringUtils.SERVICE_ANNOTATION, SpringUtils.REPOSITORY_ANNOTATION, SpringUtils.CONTROLLER_ANNOTATION, SpringUtils.CONFIGURATION_ANNOTATION),
      RedundantSpringAnnotationCheck::hasNoExplicitAttributes),
    new RedundancyRule(SpringUtils.CONTROLLER_ANNOTATION,
      List.of(SpringUtils.REST_CONTROLLER_ANNOTATION), null),
    // Class-level @ResponseBody only; method-level @ResponseBody in @RestController is handled by S6837
    new RedundancyRule(RESPONSE_BODY,
      List.of(SpringUtils.REST_CONTROLLER_ANNOTATION), null),
    new RedundancyRule(SpringUtils.CONFIGURATION_ANNOTATION,
      List.of(SpringUtils.SPRING_BOOT_APP_ANNOTATION), RedundantSpringAnnotationCheck::hasNoExplicitAttributes),
    new RedundancyRule(ENABLE_AUTO_CONFIGURATION,
      List.of(SpringUtils.SPRING_BOOT_APP_ANNOTATION), RedundantSpringAnnotationCheck::hasNoExplicitAttributes),
    new RedundancyRule(COMPONENT_SCAN,
      List.of(SpringUtils.SPRING_BOOT_APP_ANNOTATION), RedundantSpringAnnotationCheck::hasNoExplicitAttributes),
    new RedundancyRule(SPRING_BOOT_CONFIGURATION,
      List.of(SpringUtils.SPRING_BOOT_APP_ANNOTATION), RedundantSpringAnnotationCheck::hasNoExplicitAttributes),
    new RedundancyRule(EXTEND_WITH,
      List.of(SpringUtils.SPRING_BOOT_TEST_ANNOTATION, WEB_MVC_TEST, DATA_JPA_TEST, WEB_FLUX_TEST),
      RedundantSpringAnnotationCheck::isExtendWithSpringExtensionOnly),
    new RedundancyRule(SpringUtils.TRANSACTIONAL_ANNOTATION,
      List.of(DATA_JPA_TEST), RedundantSpringAnnotationCheck::hasNoExplicitAttributes)
  );

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS, Tree.Kind.RECORD);
  }

  @Override
  public void visitNode(Tree tree) {
    var classTree = (ClassTree) tree;
    Map<String, List<AnnotationTree>> annotationsByFqn = collectAnnotations(classTree);

    for (RedundancyRule rule : REDUNDANCY_RULES) {
      List<AnnotationTree> redundantAnnotations = annotationsByFqn.get(rule.redundantFqn);
      if (redundantAnnotations == null) {
        continue;
      }
      for (AnnotationTree redundantAnnotation : redundantAnnotations) {
        for (String impliedByFqn : rule.impliedByFqns) {
          List<AnnotationTree> impliedByAnnotations = annotationsByFqn.get(impliedByFqn);
          if (impliedByAnnotations != null && !impliedByAnnotations.isEmpty()
            && passesSpecialCondition(rule, redundantAnnotation)) {
            reportRedundancy(redundantAnnotation, impliedByAnnotations.get(0));
            break;
          }
        }
      }
    }
  }

  private static Map<String, List<AnnotationTree>> collectAnnotations(ClassTree classTree) {
    Map<String, List<AnnotationTree>> map = new HashMap<>();
    for (AnnotationTree annotation : classTree.modifiers().annotations()) {
      String fqn = annotation.annotationType().symbolType().fullyQualifiedName();
      map.computeIfAbsent(fqn, k -> new ArrayList<>()).add(annotation);
    }
    return map;
  }

  private static boolean passesSpecialCondition(RedundancyRule rule, AnnotationTree redundantAnnotation) {
    if (rule.specialCondition == null) {
      return true;
    }
    return rule.specialCondition.test(redundantAnnotation);
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

  private static boolean hasNoExplicitAttributes(AnnotationTree annotation) {
    return annotation.arguments().isEmpty();
  }

  private static boolean isExtendWithSpringExtensionOnly(AnnotationTree annotation) {
    var arguments = annotation.arguments();
    if (arguments.size() != 1) {
      return false;
    }
    ExpressionTree arg = arguments.get(0);
    if (arg.is(Tree.Kind.MEMBER_SELECT)) {
      return isSpringExtensionClassRef((MemberSelectExpressionTree) arg);
    }
    if (arg.is(Tree.Kind.NEW_ARRAY)) {
      var initializers = ((NewArrayTree) arg).initializers();
      return initializers.size() == 1
        && initializers.get(0).is(Tree.Kind.MEMBER_SELECT)
        && isSpringExtensionClassRef((MemberSelectExpressionTree) initializers.get(0));
    }
    return false;
  }

  private static boolean isSpringExtensionClassRef(MemberSelectExpressionTree memberSelect) {
    return memberSelect.expression().symbolType().is(SPRING_EXTENSION);
  }

  @FunctionalInterface
  private interface AnnotationPredicate {
    boolean test(AnnotationTree annotation);
  }

  private record RedundancyRule(String redundantFqn, List<String> impliedByFqns,
    AnnotationPredicate specialCondition) {
  }
}
