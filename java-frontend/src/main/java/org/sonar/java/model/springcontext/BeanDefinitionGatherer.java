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
import java.util.stream.Stream;
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
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
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
 *   <li>{@code @Qualifier} value declared on the bean itself (as opposed to on an injection point)</li>
 *   <li>Dependencies via {@code @Autowired} fields, constructors, and setters for class-level beans</li>
 *   <li>Dependencies via method parameters for {@code @Bean} method beans</li>
 *   <li>Implicit single-constructor injection (no {@code @Autowired} required)</li>
 * </ul>
 *
 * <p>Also populates:
 * <ul>
 *   <li>{@link TypeToBeanNamesIndex} with the full type hierarchy of each bean</li>
 *   <li>{@link TypeToDependenciesIndex} with the full type hierarchy of each bean</li>
 */
public class BeanDefinitionGatherer extends SpringContextModelGatherer {

  private static final Logger LOG = LoggerFactory.getLogger(BeanDefinitionGatherer.class);

  private static final String CACHE_KEY_PREFIX = "java:spring:bean-definitions:";
  private static final String BEAN_SEPARATOR = "\n";
  private static final String FIELD_SEPARATOR = "|";
  private static final String DEP_SEPARATOR = ",";
  private static final String DEP_KEY_VALUE_SEPARATOR = ":";
  private static final String DEP_NAMES_SEPARATOR = ";";
  private static final String DEP_LOCATION_SEPARATOR = "#";
  private static final String TYPE_HIERARCHY_SEPARATOR = ";";

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
    @Nullable String qualifier,
    Map<String, Set<String>> dependingBeans,
    Map<String, Set<TypeToDependenciesIndex.InjectionPoint>> dependencyInjectionPoints,
    Set<String> typeHierarchy) {
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
      Map<String, Set<TypeToDependenciesIndex.InjectionPoint>> injectionPoints = collectAutowiredDependencies(classTree, context.getInputFile());
      Map<String, Set<String>> deps = toNameMap(injectionPoints);
      Set<String> typeHierarchy = collectTypeHierarchy(classTree.symbol());
      var beanData = new BeanData(
        beanName, fqn, pkg,
        context.getInputFile(),
        AnalyzerMessage.textSpanFor(classTree.simpleName()),
        meta.isAnnotatedWith(PRIMARY_ANNOTATION),
        extractQualifier(meta),
        deps,
        injectionPoints,
        typeHierarchy);
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
    var deps = bean.dependencyInjectionPoints().entrySet().stream()
      .map(e -> Base64.getEncoder().encodeToString(e.getKey().getBytes(StandardCharsets.UTF_8))
        + DEP_KEY_VALUE_SEPARATOR
        + e.getValue().stream()
          .map(BeanDefinitionGatherer::encodeInjectionPoint)
          .collect(Collectors.joining(DEP_NAMES_SEPARATOR)))
      .collect(Collectors.joining(DEP_SEPARATOR));
    var typeHierarchy = String.join(TYPE_HIERARCHY_SEPARATOR, bean.typeHierarchy());
    var span = bean.textSpan();
    var encodedName = Base64.getEncoder().encodeToString(bean.beanName().getBytes(StandardCharsets.UTF_8));
    var encodedQualifier = bean.qualifier() != null
      ? Base64.getEncoder().encodeToString(bean.qualifier().getBytes(StandardCharsets.UTF_8))
      : "";
    return String.join(FIELD_SEPARATOR,
      encodedName,
      bean.type(),
      bean.beanPackage(),
      span.startLine + ":" + span.startCharacter + ":" + span.endLine + ":" + span.endCharacter,
      Boolean.toString(bean.isPrimary()),
      encodedQualifier,
      deps,
      typeHierarchy);
  }

  private static String encodeInjectionPoint(TypeToDependenciesIndex.InjectionPoint point) {
    var span = point.location().mainLocation();
    return Base64.getEncoder().encodeToString(point.name().getBytes(StandardCharsets.UTF_8))
      + DEP_LOCATION_SEPARATOR
      + span.startLine + ":" + span.startCharacter + ":" + span.endLine + ":" + span.endCharacter;
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
      holderBuilder.qualifier(data.qualifier());
      springContextModel.getBeanDefinitionRegistry()
        .addBeanDefinition(data.beanName(), holderBuilder.build());
      for (String typeFqn : data.typeHierarchy()) {
        springContextModel.getTypeToBeanNamesIndex().addBeanForType(typeFqn, data.beanName());
      }
      data.dependencyInjectionPoints().forEach((typeFqn, points) ->
        points.forEach(point -> springContextModel.getTypeToDependenciesIndex()
          .addDependencyForType(typeFqn, point.name(), point.location())));
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
    String qualifier = !fields[5].isEmpty()
      ? new String(Base64.getDecoder().decode(fields[5]), StandardCharsets.UTF_8)
      : null;
    Map<String, Set<TypeToDependenciesIndex.InjectionPoint>> injectionPoints = new LinkedHashMap<>();
    if (!fields[6].isEmpty()) {
      for (String entry : fields[6].split(DEP_SEPARATOR)) {
        int idx = entry.indexOf(DEP_KEY_VALUE_SEPARATOR);
        String typeFqn = new String(Base64.getDecoder().decode(entry.substring(0, idx)), StandardCharsets.UTF_8);
        Set<TypeToDependenciesIndex.InjectionPoint> points = Arrays.stream(entry.substring(idx + 1).split(DEP_NAMES_SEPARATOR))
          .map(token -> decodeInjectionPoint(token, inputFile))
          .collect(Collectors.toCollection(LinkedHashSet::new));
        injectionPoints.put(typeFqn, points);
      }
    }
    Map<String, Set<String>> deps = toNameMap(injectionPoints);
    Set<String> typeHierarchy = !fields[7].isEmpty()
      ? new LinkedHashSet<>(List.of(fields[7].split(TYPE_HIERARCHY_SEPARATOR)))
      : new LinkedHashSet<>();
    return new BeanData(beanName, type, beanPackage, inputFile, textSpan, isPrimary, qualifier, deps, injectionPoints, typeHierarchy);
  }

  private static TypeToDependenciesIndex.InjectionPoint decodeInjectionPoint(String token, InputFile inputFile) {
    int idx = token.indexOf(DEP_LOCATION_SEPARATOR);
    String name = new String(Base64.getDecoder().decode(token.substring(0, idx)), StandardCharsets.UTF_8);
    String[] spanParts = token.substring(idx + 1).split(":");
    var span = new AnalyzerMessage.TextSpan(
      Integer.parseInt(spanParts[0]),
      Integer.parseInt(spanParts[1]),
      Integer.parseInt(spanParts[2]),
      Integer.parseInt(spanParts[3]));
    return new TypeToDependenciesIndex.InjectionPoint(name, new BeanLocation(inputFile, span));
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
    List<String> beanNames = Optional.ofNullable(attrs)
      .map(list -> list.stream()
        .filter(v -> VALUE_ATTRIBUTE.equals(v.name()) || "name".equals(v.name()))
        .flatMap(v -> {
          Object val = v.value();
          if (val instanceof Object[] arr && arr.length > 0) {
            return Arrays.stream(arr).filter(String.class::isInstance).map(String.class::cast);
          }
          return Stream.empty();
        })
        .filter(s -> !s.isBlank())
        .toList())
      .filter(names -> !names.isEmpty())
      .orElse(List.of(method.simpleName().name()));

    String returnTypeFqn = method.returnType() != null
      ? method.returnType().symbolType().fullyQualifiedName()
      : "";
    Set<String> typeHierarchy = method.returnType() != null
      ? collectTypeHierarchy(method.returnType().symbolType().symbol())
      : Set.of();

    var inputFile = context.getInputFile();
    Map<String, Set<TypeToDependenciesIndex.InjectionPoint>> injectionPoints = parameterDependencies(method, inputFile);
    Map<String, Set<String>> paramDeps = toNameMap(injectionPoints);
    boolean isPrimary = beanMeta.isAnnotatedWith(PRIMARY_ANNOTATION);
    String qualifier = extractQualifier(beanMeta);
    var textSpan = AnalyzerMessage.textSpanFor(method.simpleName());

    for (String beanName : beanNames) {
      var beanData = new BeanData(beanName, returnTypeFqn, pkg, inputFile, textSpan, isPrimary, qualifier, paramDeps, injectionPoints, typeHierarchy);
      collectedBeans.add(beanData);
      beansCollectedAtFileLevel.add(beanData);
    }
  }

  private static Map<String, Set<TypeToDependenciesIndex.InjectionPoint>> collectAutowiredDependencies(ClassTree classTree, InputFile inputFile) {
    Map<String, Set<TypeToDependenciesIndex.InjectionPoint>> deps = new LinkedHashMap<>();
    List<MethodTree> unannotatedConstructors = new ArrayList<>();
    boolean hasAutowiredConstructor = false;
    for (Tree member : classTree.members()) {
      if (member instanceof VariableTree field && field.symbol().metadata().isAnnotatedWith(SpringUtils.AUTOWIRED_ANNOTATION)) {
        String typeFqn = field.symbol().type().fullyQualifiedName();
        String name = dependencyKey(field.simpleName().name(), extractQualifier(field.symbol().metadata()));
        var location = new BeanLocation(inputFile, AnalyzerMessage.textSpanFor(field.simpleName()));
        deps.computeIfAbsent(typeFqn, k -> new LinkedHashSet<>()).add(new TypeToDependenciesIndex.InjectionPoint(name, location));
      } else if (member instanceof MethodTree method) {
        if (method.symbol().metadata().isAnnotatedWith(SpringUtils.AUTOWIRED_ANNOTATION)) {
          hasAutowiredConstructor |= method.is(Tree.Kind.CONSTRUCTOR);
          parameterDependencies(method, inputFile).forEach((type, points) ->
            deps.computeIfAbsent(type, k -> new LinkedHashSet<>()).addAll(points));
        } else if (method.is(Tree.Kind.CONSTRUCTOR)) {
          unannotatedConstructors.add(method);
        }
      }
    }
    if (!hasAutowiredConstructor && unannotatedConstructors.size() == 1) {
      parameterDependencies(unannotatedConstructors.get(0), inputFile).forEach((type, points) ->
        deps.computeIfAbsent(type, k -> new LinkedHashSet<>()).addAll(points));
    }
    return deps;
  }

  private static Map<String, Set<TypeToDependenciesIndex.InjectionPoint>> parameterDependencies(MethodTree method, InputFile inputFile) {
    Map<String, Set<TypeToDependenciesIndex.InjectionPoint>> deps = new LinkedHashMap<>();
    for (var p : method.parameters()) {
      String typeFqn = p.symbol().type().fullyQualifiedName();
      String name = dependencyKey(p.simpleName().name(), extractQualifier(p.symbol().metadata()));
      var location = new BeanLocation(inputFile, AnalyzerMessage.textSpanFor(p.simpleName()));
      deps.computeIfAbsent(typeFqn, k -> new LinkedHashSet<>()).add(new TypeToDependenciesIndex.InjectionPoint(name, location));
    }
    return deps;
  }

  private static Map<String, Set<String>> toNameMap(Map<String, Set<TypeToDependenciesIndex.InjectionPoint>> injectionPointsByType) {
    Map<String, Set<String>> names = new LinkedHashMap<>();
    injectionPointsByType.forEach((typeFqn, points) ->
      names.put(typeFqn, points.stream()
        .map(TypeToDependenciesIndex.InjectionPoint::name)
        .collect(Collectors.toCollection(LinkedHashSet::new))));
    return names;
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

  private static Set<String> collectTypeHierarchy(Symbol.TypeSymbol symbol) {
    Set<String> visited = new LinkedHashSet<>();
    walkTypeHierarchy(symbol, visited);
    return visited;
  }

  private static void walkTypeHierarchy(Symbol.TypeSymbol symbol, Set<String> visited) {
    String fqn = symbol.type().fullyQualifiedName();
    if ("java.lang.Object".equals(fqn) || symbol.type().isUnknown() || !visited.add(fqn)) {
      return;
    }
    Type superClass = symbol.superClass();
    if (superClass != null && !superClass.isUnknown()) {
      walkTypeHierarchy(superClass.symbol(), visited);
    }
    for (Type iface : symbol.interfaces()) {
      if (!iface.isUnknown()) {
        walkTypeHierarchy(iface.symbol(), visited);
      }
    }
  }

}
