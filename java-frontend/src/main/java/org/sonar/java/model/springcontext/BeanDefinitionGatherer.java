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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.utils.PackageUtils;
import org.sonar.java.utils.SpringUtils;
import org.sonar.plugins.java.api.InputFileScannerContext;
import org.sonar.plugins.java.api.JavaFileScannerContext;
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

  private static final Logger LOG = LoggerFactory.getLogger(BeanDefinitionGatherer.class);

  private static final String CACHE_KEY_PREFIX = "java:spring:bean-definitions:";
  private static final String BEAN_SEPARATOR = "\n";
  private static final String FIELD_SEPARATOR = "|";
  private static final String DEP_SEPARATOR = ",";
  private static final String DEP_KEY_VALUE_SEPARATOR = ":";
  private static final String DEP_NAMES_SEPARATOR = ";";

  private static final String PRIMARY_ANNOTATION = "org.springframework.context.annotation.Primary";
  private static final String VALUE_ATTRIBUTE = "value";

  private final List<BeanData> collectedBeans = new ArrayList<>();

  /** Beans found in the file currently being scanned, used for per-file cache writes. */
  private final List<BeanData> beansCollectedAtFileLevel = new ArrayList<>();

  private record BeanData(
    String beanName,
    String type,
    String beanPackage,
    InputFile inputFile,
    AnalyzerMessage.TextSpan textSpan,
    boolean isPrimary,
    Map<String, Set<String>> dependingBeans) {
  }

  @Override
  public void setContext(JavaFileScannerContext context) {
    beansCollectedAtFileLevel.clear();
    super.setContext(context);
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
      Map<String, Set<String>> deps = collectAutowiredDependencies(classTree);
      // Class-level bean (stereotype annotations)
      var beanData = new BeanData(
        beanName, fqn, pkg,
        context.getInputFile(),
        AnalyzerMessage.textSpanFor(classTree.simpleName()),
        meta.isAnnotatedWith(PRIMARY_ANNOTATION),
        deps);
      collectedBeans.add(beanData);
      beansCollectedAtFileLevel.add(beanData);

      // @Bean methods — only if class is a configuration/component class
      for (MethodTree method : SpringUtils.getBeanMethods(classTree)) {
        collectBeanMethod(method, pkg);
      }
    }
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    if (context.getCacheContext().isCacheEnabled()) {
      writeToCache(context, beansCollectedAtFileLevel);
    }
    beansCollectedAtFileLevel.clear();
  }

  private static String cacheKey(InputFile inputFile) {
    return CACHE_KEY_PREFIX + inputFile.key();
  }

  private static void writeToCache(JavaFileScannerContext context, List<BeanData> beans) {
    var cacheKey = cacheKey(context.getInputFile());
    var data = beans.stream()
      .map(BeanDefinitionGatherer::serializeBean)
      .collect(Collectors.joining(BEAN_SEPARATOR))
      .getBytes(StandardCharsets.UTF_8);
    try {
      context.getCacheContext().getWriteCache().write(cacheKey, data);
    } catch (IllegalArgumentException e) {
      LOG.trace("Tried to write multiple times to cache key '{}'. Ignoring writes after the first.", cacheKey);
    }
  }

  private static String serializeBean(BeanData bean) {
    var deps = bean.dependingBeans().entrySet().stream()
      .map(e -> Base64.getEncoder().encodeToString(e.getKey().getBytes(StandardCharsets.UTF_8))
        + DEP_KEY_VALUE_SEPARATOR
        + e.getValue().stream()
          .map(n -> Base64.getEncoder().encodeToString(n.getBytes(StandardCharsets.UTF_8)))
          .collect(Collectors.joining(DEP_NAMES_SEPARATOR)))
      .collect(Collectors.joining(DEP_SEPARATOR));
    var span = bean.textSpan();
    var encodedName = Base64.getEncoder().encodeToString(bean.beanName().getBytes(StandardCharsets.UTF_8));
    return String.join(FIELD_SEPARATOR,
      encodedName,
      bean.type(),
      bean.beanPackage(),
      span.startLine + ":" + span.startCharacter + ":" + span.endLine + ":" + span.endCharacter,
      Boolean.toString(bean.isPrimary()),
      deps);
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
    return readFromCache(ctx).map(beans -> {
      collectedBeans.addAll(beans);
      return true;
    }).orElse(false);
  }

  private static Optional<List<BeanData>> readFromCache(InputFileScannerContext ctx) {
    var cacheKey = cacheKey(ctx.getInputFile());
    var bytes = ctx.getCacheContext().getReadCache().readBytes(cacheKey);
    if (bytes == null) {
      return Optional.empty();
    }
    String content = new String(bytes, StandardCharsets.UTF_8);
    if (content.isEmpty()) {
      ctx.getCacheContext().getWriteCache().copyFromPrevious(cacheKey);
      return Optional.of(List.of());
    }
    try {
      var beans = content.lines()
        .map(line -> deserializeBean(line, ctx.getInputFile()))
        .toList();
      ctx.getCacheContext().getWriteCache().copyFromPrevious(cacheKey);
      return Optional.of(beans);
    } catch (RuntimeException e) {
      LOG.trace("Failed to deserialize cached beans for '{}', will re-parse.", cacheKey);
      return Optional.empty();
    }
  }

  private static BeanData deserializeBean(String line, InputFile inputFile) {
    String[] fields = line.split("\\" + FIELD_SEPARATOR, -1);
    String beanName = new String(Base64.getDecoder().decode(fields[0]), StandardCharsets.UTF_8);
    String type = fields[1];
    String beanPackage = fields[2];
    String[] spanParts = fields[3].split(":");
    var textSpan = new AnalyzerMessage.TextSpan(
      Integer.parseInt(spanParts[0]),
      Integer.parseInt(spanParts[1]),
      Integer.parseInt(spanParts[2]),
      Integer.parseInt(spanParts[3]));
    boolean isPrimary = Boolean.parseBoolean(fields[4]);
    Map<String, Set<String>> deps = new LinkedHashMap<>();
    if (!fields[5].isEmpty()) {
      for (String entry : fields[5].split(DEP_SEPARATOR)) {
        int idx = entry.indexOf(DEP_KEY_VALUE_SEPARATOR);
        String typeFqn = new String(Base64.getDecoder().decode(entry.substring(0, idx)), StandardCharsets.UTF_8);
        Set<String> names = Arrays.stream(entry.substring(idx + 1).split(DEP_NAMES_SEPARATOR))
          .map(n -> new String(Base64.getDecoder().decode(n), StandardCharsets.UTF_8))
          .collect(Collectors.toCollection(LinkedHashSet::new));
        deps.put(typeFqn, names);
      }
    }
    return new BeanData(beanName, type, beanPackage, inputFile, textSpan, isPrimary, deps);
  }

  private static Optional<String> extractBeanName(SymbolMetadata meta) {
    for (String annotation : SpringUtils.STEREOTYPE_ANNOTATIONS) {
      List<SymbolMetadata.AnnotationValue> attrs = meta.valuesForAnnotation(annotation);
      if (attrs != null) {
        Optional<String> name = attrs.stream()
          .filter(v -> VALUE_ATTRIBUTE.equals(v.name()) || "name".equals(v.name()))
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
        .filter(v -> VALUE_ATTRIBUTE.equals(v.name()) || "name".equals(v.name()))
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

    Map<String, Set<String>> paramDeps = parameterDependencies(method);

    var beanData = new BeanData(
      beanName, returnTypeFqn, pkg,
      context.getInputFile(),
      AnalyzerMessage.textSpanFor(method.simpleName()),
      beanMeta.isAnnotatedWith(PRIMARY_ANNOTATION),
      paramDeps);
    collectedBeans.add(beanData);
    beansCollectedAtFileLevel.add(beanData);
  }

  private static Map<String, Set<String>> collectAutowiredDependencies(ClassTree classTree) {
    Map<String, Set<String>> deps = new LinkedHashMap<>();
    for (Tree member : classTree.members()) {
      if (member instanceof VariableTree field) {
        if (field.symbol().metadata().isAnnotatedWith(SpringUtils.AUTOWIRED_ANNOTATION)) {
          String typeFqn = field.symbol().type().fullyQualifiedName();
          String name = dependencyKey(field.simpleName().name(), extractQualifier(field.symbol().metadata()));
          deps.computeIfAbsent(typeFqn, k -> new LinkedHashSet<>()).add(name);
        }
      } else if (member.is(Tree.Kind.CONSTRUCTOR, Tree.Kind.METHOD)) {
        MethodTree method = (MethodTree) member;
        if (method.symbol().metadata().isAnnotatedWith(SpringUtils.AUTOWIRED_ANNOTATION)) {
          parameterDependencies(method).forEach((type, names) ->
            deps.computeIfAbsent(type, k -> new LinkedHashSet<>()).addAll(names));
        }
      }
    }
    return deps;
  }

  private static Map<String, Set<String>> parameterDependencies(MethodTree method) {
    Map<String, Set<String>> deps = new LinkedHashMap<>();
    for (var p : method.parameters()) {
      String typeFqn = p.symbol().type().fullyQualifiedName();
      String name = dependencyKey(p.simpleName().name(), extractQualifier(p.symbol().metadata()));
      deps.computeIfAbsent(typeFqn, k -> new LinkedHashSet<>()).add(name);
    }
    return deps;
  }

  private static String dependencyKey(String fieldOrParamName, @Nullable String qualifier) {
    return qualifier != null ? qualifier : fieldOrParamName;
  }

  @Nullable
  private static String extractQualifier(SymbolMetadata metadata) {
    List<SymbolMetadata.AnnotationValue> attrs = metadata.valuesForAnnotation(SpringUtils.QUALIFIER_ANNOTATION);
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

}
