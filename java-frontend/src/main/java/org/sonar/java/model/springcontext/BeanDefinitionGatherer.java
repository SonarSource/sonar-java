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
package org.sonar.java.model.springcontext;

import java.beans.Introspector;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.utils.PackageUtils;
import org.sonar.java.utils.SpringUtils;
import org.sonar.plugins.java.api.InputFileScannerContext;
import org.sonar.plugins.java.api.ModuleScannerContext;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

/**
 * Collects Spring bean definitions discovered during AST traversal, and registers them in the
 * {@link BeanDefinitionRegistry} of the shared {@link SpringContextModel} at the end of the module analysis.
 *
 * <p>Discovers beans from:
 * <ul>
 *   <li>Classes annotated with stereotype annotations: {@code @Component}, {@code @Service},
 *       {@code @Repository}, {@code @Controller}, {@code @RestController}, {@code @Configuration}</li>
 *   <li>{@code @Bean} methods inside {@code @Configuration} or {@code @Component} classes</li>
 * </ul>
 *
 * <p>Also captures:
 * <ul>
 *   <li>{@code @Primary} designation</li>
 *   <li>Dependencies via {@code @Autowired} fields, constructors, and setters for class-level beans</li>
 *   <li>Dependencies via method parameters for {@code @Bean} method beans</li>
 * </ul>
 */
public class BeanDefinitionGatherer extends SpringContextModelGatherer {

  private static final String PRIMARY_ANNOTATION = "org.springframework.context.annotation.Primary";

  private final List<BeanData> collectedBeans = new ArrayList<>();

  private record BeanData(
    String beanName,
    String type,
    String beanPackage,
    InputFile inputFile,
    AnalyzerMessage.TextSpan textSpan,
    boolean isPrimary,
    List<String> dependingBeans) {
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (classTree.simpleName() == null) {
      return;
    }

    SymbolMetadata meta = classTree.symbol().metadata();
    String fqn = classTree.symbol().type().fullyQualifiedName();
    String pkg = PackageUtils.packageNameOf(classTree.symbol());

    if (SpringUtils.STEREOTYPE_ANNOTATIONS.stream().anyMatch(meta::isAnnotatedWith)) {
      String beanName = extractBeanName(meta)
        .orElseGet(() -> defaultBeanName(classTree.simpleName().name()));
      List<String> deps = collectAutowiredDependencies(classTree);
      // Class-level bean (stereotype annotations)
      collectedBeans.add(new BeanData(
        beanName, fqn, pkg,
        context.getInputFile(),
        AnalyzerMessage.textSpanFor(classTree.simpleName()),
        meta.isAnnotatedWith(PRIMARY_ANNOTATION),
        deps));

      // @Bean methods — only if class is a configuration/component class
      for (MethodTree method : SpringUtils.getBeanMethods(classTree)) {
        collectBeanMethod(method, pkg);
      }
    }
  }

  @Override
  public void gatherSpringContextData(ModuleScannerContext context, SpringContextModel springContextModel) {
    for (BeanData data : collectedBeans) {
      var location = new BeanLocation(data.inputFile(), data.textSpan());
      var holderBuilder = new BeanDefinitionHolder.Builder(
        data.type(), context.getModuleKey(), data.beanPackage(), location)
        .dependingBeans(data.dependingBeans());
      if (data.isPrimary()) {
        holderBuilder.primary();
      }
      springContextModel.getBeanDefinitionRegistry()
        .addBeanDefinition(data.beanName(), holderBuilder.build());
    }
  }

  @Override
  public boolean scanWithoutParsing(InputFileScannerContext ctx) {
    // Bean data is not cached yet; force parsing so beans are
    // always collected even for unchanged files in incremental runs.
    return false;
  }

  private static Optional<String> extractBeanName(SymbolMetadata meta) {
    for (String annotation : SpringUtils.STEREOTYPE_ANNOTATIONS) {
      List<SymbolMetadata.AnnotationValue> attrs = meta.valuesForAnnotation(annotation);
      if (attrs != null) {
        Optional<String> name = attrs.stream()
          .filter(v -> "value".equals(v.name()) || "name".equals(v.name()))
          .map(v -> (String) v.value())
          .filter(s -> !s.isBlank())
          .findFirst();
        if (name.isPresent()) {
          return name;
        }
      }
    }
    return Optional.empty();
  }

  private static String defaultBeanName(String simpleName) {
    return Introspector.decapitalize(simpleName);
  }

  private void collectBeanMethod(MethodTree method, String pkg) {
    SymbolMetadata beanMeta = method.symbol().metadata();
    List<SymbolMetadata.AnnotationValue> attrs = beanMeta.valuesForAnnotation(SpringUtils.BEAN_ANNOTATION);
    String beanName = Optional.ofNullable(attrs)
      .flatMap(list -> list.stream()
        .filter(v -> "value".equals(v.name()) || "name".equals(v.name()))
        .map(v -> {
          Object val = v.value();
          if (val instanceof Object[] arr && arr.length > 0) {
            return (String) arr[0];
          }
          return val instanceof String s ? s : null;
        })
        .filter(s -> s != null && !s.isBlank())
        .findFirst())
      .orElseGet(() -> method.simpleName().name());

    String returnTypeFqn = method.returnType() != null
      ? method.returnType().symbolType().fullyQualifiedName()
      : "";

    List<String> paramDeps = method.parameters().stream()
      .map(p -> p.symbol().type().fullyQualifiedName())
      .toList();

    collectedBeans.add(new BeanData(
      beanName, returnTypeFqn, pkg,
      context.getInputFile(),
      AnalyzerMessage.textSpanFor(method.simpleName()),
      beanMeta.isAnnotatedWith(PRIMARY_ANNOTATION),
      paramDeps));
  }

  private static List<String> collectAutowiredDependencies(ClassTree classTree) {
    List<String> deps = new ArrayList<>();
    for (Tree member : classTree.members()) {
      if (member.is(Tree.Kind.VARIABLE)) {
        VariableTree field = (VariableTree) member;
        if (field.symbol().metadata().isAnnotatedWith(SpringUtils.AUTOWIRED_ANNOTATION)) {
          deps.add(field.symbol().type().fullyQualifiedName());
        }
      } else if (member.is(Tree.Kind.CONSTRUCTOR, Tree.Kind.METHOD)) {
        MethodTree method = (MethodTree) member;
        if (method.symbol().metadata().isAnnotatedWith(SpringUtils.AUTOWIRED_ANNOTATION)) {
          method.parameters().stream()
            .map(p -> p.symbol().type().fullyQualifiedName())
            .forEach(deps::add);
        }
      }
    }
    return deps;
  }

}
