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
package org.sonar.java.utils;

import java.beans.Introspector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.springcontext.BeanLocation;
import org.sonar.java.model.springcontext.InjectionPoint;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

public final class SpringUtils {

  public static final String SPRING_BOOT_APP_ANNOTATION = "org.springframework.boot.autoconfigure.SpringBootApplication";
  public static final String CONTROLLER_ANNOTATION = "org.springframework.stereotype.Controller";
  public static final String COMPONENT_ANNOTATION = "org.springframework.stereotype.Component";
  public static final String REPOSITORY_ANNOTATION = "org.springframework.stereotype.Repository";
  public static final String SERVICE_ANNOTATION = "org.springframework.stereotype.Service";
  public static final String AUTOWIRED_ANNOTATION = "org.springframework.beans.factory.annotation.Autowired";
  public static final String QUALIFIER_ANNOTATION = "org.springframework.beans.factory.annotation.Qualifier";
  public static final String VALUE_ANNOTATION = "org.springframework.beans.factory.annotation.Value";
  public static final String TRANSACTIONAL_ANNOTATION = "org.springframework.transaction.annotation.Transactional";
  public static final String BEAN_ANNOTATION = "org.springframework.context.annotation.Bean";
  public static final String SCOPE_ANNOTATION = "org.springframework.context.annotation.Scope";
  public static final String CONFIGURATION_ANNOTATION = "org.springframework.context.annotation.Configuration";
  public static final String PROFILE_ANNOTATION = "org.springframework.context.annotation.Profile";
  public static final String ASYNC_ANNOTATION = "org.springframework.scheduling.annotation.Async";
  public static final String DATA_REPOSITORY_ANNOTATION = "org.springframework.data.repository.Repository";
  public static final String REST_CONTROLLER_ANNOTATION = "org.springframework.web.bind.annotation.RestController";
  public static final String SPRING_BOOT_TEST_ANNOTATION = "org.springframework.boot.test.context.SpringBootTest";

  private static final String VALUE_ATTRIBUTE = "value";
  private static final String PROFILE_SEPARATOR = ",";

  public static final List<String> STEREOTYPE_ANNOTATIONS = List.of(
    COMPONENT_ANNOTATION,
    SERVICE_ANNOTATION,
    REPOSITORY_ANNOTATION,
    CONTROLLER_ANNOTATION,
    REST_CONTROLLER_ANNOTATION,
    CONFIGURATION_ANNOTATION
  );

  private SpringUtils() {
    // Utils class
  }

  public static boolean isScopeSingleton(SymbolMetadata clazzMeta) {
    List<SymbolMetadata.AnnotationValue> values = clazzMeta.valuesForAnnotation(SCOPE_ANNOTATION);
    if (values == null) {
      // Scope is singleton by default
      return true;
    }
    for (SymbolMetadata.AnnotationValue annotationValue : values) {
      if (VALUE_ATTRIBUTE.equals(annotationValue.name()) || "scopeName".equals(annotationValue.name())) {
        Object value = annotationValue.value();
        if (value instanceof String stringValue && !"singleton".equals(stringValue)) {
          return false;
        }
      }
    }
    return true;
  }

  public static boolean isAutowired(Symbol symbol) {
    return symbol.metadata().isAnnotatedWith(AUTOWIRED_ANNOTATION);
  }

  public static boolean isSpringBootTestClass(Symbol symbol) {
    return symbol.metadata().isAnnotatedWith(SPRING_BOOT_TEST_ANNOTATION);
  }

  public static boolean isSpringBootUnitTest(MethodTree methodTree) {
    Tree parentOfType = ExpressionUtils.getParentOfType(methodTree, Tree.Kind.CLASS);
    if (parentOfType == null) {
      return false;
    }
    ClassTree parentClass = (ClassTree) parentOfType;
    return UnitTestUtils.isUnitTest(methodTree) && SpringUtils.isSpringBootTestClass(parentClass.symbol());
  }

  public static List<MethodTree> getBeanMethods(ClassTree classTree) {
    return classTree.members().stream()
      .filter(member -> member.is(Tree.Kind.METHOD))
      .map(MethodTree.class::cast)
      .filter(method -> method.symbol().metadata().isAnnotatedWith(BEAN_ANNOTATION))
      .toList();
  }

  /**
   * Extracts the bean name from whichever stereotype annotation is present on the bean definition,
   * falling back to the decapitalized simple name for an unnamed bean.
   *
   * @param meta The symbol metadata of the class declaring the bean
   * @param simpleName The simple name of the class declaring the bean
   * @return The resolved bean name
   */
  public static String extractBeanNameFromAnnotation(SymbolMetadata meta, String simpleName) {
    for (String annotation : STEREOTYPE_ANNOTATIONS) {
      List<SymbolMetadata.AnnotationValue> attrs = meta.valuesForAnnotation(annotation);
      if (attrs != null) {
        Optional<String> name = attrs.stream()
          .filter(v -> VALUE_ATTRIBUTE.equals(v.name()) || "name".equals(v.name()))
          .map(v -> (String) v.value())
          .filter(s -> !s.isBlank())
          .findFirst();
        if (name.isPresent()) {
          return name.get();
        }
      }
    }
    return Introspector.decapitalize(simpleName);
  }

  /**
   * Reads the {@code @Bean} method's explicit "value"/"name" attribute (accepting one or several aliases),
   * falling back to the method's own name when none is given.
   *
   * @param method The {@code @Bean} factory method, used for its name when no alias is declared
   * @return The declared alias(es), or the method's own name when none is declared
   */
  public static List<String> extractBeanNameFromMethod(MethodTree method) {
    SymbolMetadata beanMeta = method.symbol().metadata();
    List<SymbolMetadata.AnnotationValue> attrs = beanMeta.valuesForAnnotation(BEAN_ANNOTATION);
    List<String> names = attrs == null ? List.of() : attrs.stream()
      .filter(attr -> VALUE_ATTRIBUTE.equals(attr.name()) || "name".equals(attr.name()))
      .filter(attr -> attr.value() instanceof Object[])
      .flatMap(attr -> Arrays.stream((Object[]) attr.value()))
      .filter(String.class::isInstance)
      .map(String.class::cast)
      .filter(name -> !name.isBlank())
      .toList();
    return names.isEmpty() ? List.of(method.simpleName().name()) : names;
  }

  /**
   * Reads the {@code @Qualifier} value, if any.
   *
   * @param metadata The symbol metadata of the field or parameter to check for a {@code @Qualifier}
   * @return The qualifier's value, or {@code null} if none is declared
   */
  @Nullable
  public static String extractQualifierValue(SymbolMetadata metadata) {
    List<SymbolMetadata.AnnotationValue> attrs = metadata.valuesForAnnotation(QUALIFIER_ANNOTATION);
    if (attrs == null) {
      return null;
    }
    return attrs.stream()
      .filter(v -> VALUE_ATTRIBUTE.equals(v.name()))
      .map(v -> (String) v.value())
      .filter(s -> !s.isBlank())
      .findFirst()
      .orElse(null);
  }

  /**
   * Collects a class-level bean's dependencies from {@code @Autowired} fields, constructors and setters.
   *
   * Also applies Spring's implicit single-constructor injection if no constructor is {@code @Autowired}
   * and the class declares exactly one constructor. {@code hasAutowiredConstructor} guards against
   * misapplying that fallback when an {@code @Autowired} constructor already exists alongside other,
   * unannotated ones.
   *
   * @param classTree The class whose members are scanned for dependencies
   * @param inputFile The file {@code classTree} was parsed from, used to locate each injection point
   * @return The class's dependencies, mapped by required type FQN to the {@link InjectionPoint}s that require it
   */
  public static Map<String, Set<InjectionPoint>> collectAutowiredDependenciesOnClass(ClassTree classTree, InputFile inputFile) {
    Map<String, Set<InjectionPoint>> deps = new LinkedHashMap<>();
    List<MethodTree> unannotatedConstructors = new ArrayList<>();
    boolean hasAutowiredConstructor = false;
    for (Tree member : classTree.members()) {
      if (member instanceof VariableTree field && field.symbol().metadata().isAnnotatedWith(SpringUtils.AUTOWIRED_ANNOTATION)) {
        String typeFqn = field.symbol().type().fullyQualifiedName();
        String name = dependencyKey(field.simpleName().name(), SpringUtils.extractQualifierValue(field.symbol().metadata()));
        var location = new BeanLocation(inputFile, AnalyzerMessage.textSpanFor(field.simpleName()));
        deps.computeIfAbsent(typeFqn, k -> new LinkedHashSet<>()).add(new InjectionPoint(name, location));
      } else if (member instanceof MethodTree method) {
        if (method.symbol().metadata().isAnnotatedWith(SpringUtils.AUTOWIRED_ANNOTATION)) {
          hasAutowiredConstructor |= method.is(Tree.Kind.CONSTRUCTOR);
          collectDependenciesOnMethod(method, inputFile).forEach((type, points) -> deps.computeIfAbsent(type, k -> new LinkedHashSet<>()).addAll(points));
        } else if (method.is(Tree.Kind.CONSTRUCTOR)) {
          // Held back until the class has been fully scanned, in case an @Autowired constructor appears
          // later among the members and disqualifies the implicit single-constructor rule below.
          unannotatedConstructors.add(method);
        }
      }
    }
    if (!hasAutowiredConstructor && unannotatedConstructors.size() == 1) {
      collectDependenciesOnMethod(unannotatedConstructors.get(0), inputFile).forEach((type, points) -> deps.computeIfAbsent(type, k -> new LinkedHashSet<>()).addAll(points));
    }
    return deps;
  }

  /**
   * Collect the given method's parameters as dependencies.
   *
   * @param method Method whose parameters are stored as dependencies, either {@code @Autowired} constructors/setters or
   * {@code @Bean} factory methods
   * @param inputFile The file {@code method} was parsed from, used to locate each injection point
   * @return The collected dependencies, mapped by required type FQN to the {@link InjectionPoint}s that require it
   */
  public static Map<String, Set<InjectionPoint>> collectDependenciesOnMethod(MethodTree method, InputFile inputFile) {
    Map<String, Set<InjectionPoint>> deps = new LinkedHashMap<>();
    for (var p : method.parameters()) {
      String typeFqn = p.symbol().type().fullyQualifiedName();
      String name = dependencyKey(p.simpleName().name(), SpringUtils.extractQualifierValue(p.symbol().metadata()));
      var location = new BeanLocation(inputFile, AnalyzerMessage.textSpanFor(p.simpleName()));
      deps.computeIfAbsent(typeFqn, k -> new LinkedHashSet<>()).add(new InjectionPoint(name, location));
    }
    return deps;
  }

  private static String dependencyKey(String fieldOrParamName, @Nullable String qualifier) {
    return qualifier != null ? qualifier : fieldOrParamName;
  }

  /**
   * Reads the {@code @Profile} annotation's "value" attribute, joining every profile name it lists with ",".
   *
   * @param metadata The symbol metadata of the class or {@code @Bean} method to check for a {@code @Profile}
   * @return The joined profile expression, or {@code null} if none is declared
   */
  @Nullable
  public static String extractProfiles(SymbolMetadata metadata) {
    List<SymbolMetadata.AnnotationValue> attrs = metadata.valuesForAnnotation(PROFILE_ANNOTATION);
    List<String> profiles = attrs == null ? List.of() : attrs.stream()
                                                        .filter(attr -> VALUE_ATTRIBUTE.equals(attr.name()))
                                                        .filter(attr -> attr.value() instanceof Object[])
                                                        .flatMap(attr -> Arrays.stream((Object[]) attr.value()))
                                                        .filter(String.class::isInstance)
                                                        .map(String.class::cast)
                                                        .filter(profile -> !profile.isBlank())
                                                        .toList();
    return profiles.isEmpty() ? null : String.join(PROFILE_SEPARATOR, profiles);
  }

}
