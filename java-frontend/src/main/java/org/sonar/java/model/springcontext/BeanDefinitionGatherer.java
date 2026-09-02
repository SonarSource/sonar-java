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
import org.sonar.java.model.springcontext.TypeToDependenciesIndex.InjectionPoint;
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
 *   <li>Dependencies via {@code @Autowired} fields, constructors, and setters for class-level beans</li>
 *   <li>Dependencies via method parameters for {@code @Bean} method beans</li>
 *   <li>Implicit single-constructor injection (no {@code @Autowired} required)</li>
 * </ul>
 *
 * <p>Also populates:
 * <ul>
 *   <li>{@link TypeToBeanNamesIndex} with the full type hierarchy of each bean</li>
 *   <li>{@link TypeToDependenciesIndex} with all the dependencies collected by type</li>
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
    Map<String, Set<String>> dependingBeans,
    Map<String, Set<InjectionPoint>> dependencyInjectionPoints,
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

  /**
   * Visits class nodes and registers a bean when the class carries a stereotype annotation ({@code @Component},
   * {@code @Service}, {@code @Repository}, {@code @Controller}, {@code @RestController}, {@code @Configuration}),
   * then looks for {@code @Bean} factory methods on that same class.
   */
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
      String beanName = extractBeanName(meta).orElseGet(() -> defaultBeanName(classTree.simpleName().name()));
      // collect autowired dependencies as InjectionPoints to store in TypeToDependenciesIndex
      Map<String, Set<InjectionPoint>> injectionPoints = collectAutowiredDependencies(classTree, context.getInputFile());
      // also collect their names mapped by type to store in dependingBeans
      Map<String, Set<String>> deps = toNameMap(injectionPoints);
      Set<String> typeHierarchy = collectTypeHierarchy(classTree.symbol());
      var beanData = new BeanData(
        beanName, fqn, pkg,
        context.getInputFile(),
        AnalyzerMessage.textSpanFor(classTree.simpleName()),
        meta.isAnnotatedWith(PRIMARY_ANNOTATION),
        deps,
        injectionPoints,
        typeHierarchy);
      collectedBeans.add(beanData);
      beansCollectedAtFileLevel.add(beanData);

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

  /** Serializes and writes this file's beans as newline-joined lines, mirrored by {@link #deserializeBean} via {@code String#lines}. */
  private static void writeToCache(JavaFileScannerContext context, List<BeanData> beans) {
    var cacheKey = SpringContextCacheHelper.cacheKey(CACHE_KEY_PREFIX, context);
    var data = beans.stream()
      .map(BeanDefinitionGatherer::serializeBean)
      .collect(Collectors.joining(BEAN_SEPARATOR));
    SpringContextCacheHelper.writeToCache(context, LOG, cacheKey, data);
  }

  /**
   * Serializes one bean into a single "|"-delimited line. Any string sourced from user code (bean name,
   * dependency type keys, injection point names) is Base64-encoded first, since {@code |}, {@code :},
   * {@code ,}, {@code ;} or {@code #} could otherwise appear in an identifier and be mistaken for a
   * field/entry separator.
   */
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
    return String.join(FIELD_SEPARATOR,
      encodedName,
      bean.type(),
      bean.beanPackage(),
      span.startLine + ":" + span.startCharacter + ":" + span.endLine + ":" + span.endCharacter,
      Boolean.toString(bean.isPrimary()),
      deps,
      typeHierarchy);
  }

  /** Encodes one injection point (dependency name, location) as {@code <base64 name>#<span>}; see {@link #serializeBean} for why the name is encoded. */
  private static String encodeInjectionPoint(InjectionPoint point) {
    var span = point.location().mainLocation();
    return Base64.getEncoder().encodeToString(point.name().getBytes(StandardCharsets.UTF_8))
      + DEP_LOCATION_SEPARATOR
      + span.startLine + ":" + span.startCharacter + ":" + span.endLine + ":" + span.endCharacter;
  }

  /**
   * Transfers all beans collected across the module into the shared {@link SpringContextModel}: the bean
   * definition itself, its position in every ancestor/interface type (for {@link TypeToBeanNamesIndex}), and
   * each of its dependencies by type (for {@link TypeToDependenciesIndex}).
   */
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
      for (String typeFqn : data.typeHierarchy()) {
        springContextModel.getTypeToBeanNamesIndex().addBeanForType(typeFqn, data.beanName());
      }
      data.dependencyInjectionPoints().forEach((typeFqn, points) -> points.forEach(point -> springContextModel.getTypeToDependenciesIndex()
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
    var cacheKey = SpringContextCacheHelper.cacheKey(CACHE_KEY_PREFIX, ctx);
    // A file with zero beans still has a cache entry (an empty string), handled explicitly here rather than
    // relying on String#lines() returning an empty stream for "".
    return SpringContextCacheHelper.readFromCache(ctx, LOG, cacheKey, content -> content.isEmpty()
      ? List.<BeanData>of()
      : content.lines().map(line -> deserializeBean(line, ctx.getInputFile())).toList());
  }

  /**
   * Reverse of {@link #serializeBean}: splits the "|"-delimited line back into a bean's fields, decoding
   * every value that was Base64-encoded on write.
   */
  private static BeanData deserializeBean(String line, InputFile inputFile) {
    // -1 keeps trailing empty fields (e.g. no dependencies/no type hierarchy) so the fixed field indices below stay aligned.
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
    Map<String, Set<InjectionPoint>> injectionPoints = new LinkedHashMap<>();
    if (!fields[5].isEmpty()) {
      for (String entry : fields[5].split(DEP_SEPARATOR)) {
        // indexOf is safe: Base64 output never contains ':', so the first ':' is unambiguously the key/value boundary.
        int idx = entry.indexOf(DEP_KEY_VALUE_SEPARATOR);
        String typeFqn = new String(Base64.getDecoder().decode(entry.substring(0, idx)), StandardCharsets.UTF_8);
        Set<InjectionPoint> points = Arrays.stream(entry.substring(idx + 1).split(DEP_NAMES_SEPARATOR))
          .map(token -> decodeInjectionPoint(token, inputFile))
          .collect(Collectors.toCollection(LinkedHashSet::new));
        injectionPoints.put(typeFqn, points);
      }
    }
    Map<String, Set<String>> deps = toNameMap(injectionPoints);
    Set<String> typeHierarchy = !fields[6].isEmpty()
      ? new LinkedHashSet<>(List.of(fields[6].split(TYPE_HIERARCHY_SEPARATOR)))
      : new LinkedHashSet<>();
    return new BeanData(beanName, type, beanPackage, inputFile, textSpan, isPrimary, deps, injectionPoints, typeHierarchy);
  }

  /** Reverse of {@link #encodeInjectionPoint}. {@code idx} lookup is safe since Base64 output never contains '#'. */
  private static InjectionPoint decodeInjectionPoint(String token, InputFile inputFile) {
    int idx = token.indexOf(DEP_LOCATION_SEPARATOR);
    String name = new String(Base64.getDecoder().decode(token.substring(0, idx)), StandardCharsets.UTF_8);
    String[] spanParts = token.substring(idx + 1).split(":");
    var span = new AnalyzerMessage.TextSpan(
      Integer.parseInt(spanParts[0]),
      Integer.parseInt(spanParts[1]),
      Integer.parseInt(spanParts[2]),
      Integer.parseInt(spanParts[3]));
    return new InjectionPoint(name, new BeanLocation(inputFile, span));
  }

  /**
   * Finds an explicit bean name from whichever stereotype annotation is present, checking both the "value"
   * attribute (e.g. {@code @Component("foo")}) and the "name" attribute (e.g. {@code @Controller(name = "foo")}).
   * A class only ever carries one stereotype annotation in valid code, but
   * this loops over all of them defensively rather than assuming which one is present.
   */
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

  /** Spring's own default name for an unnamed bean: the class simple name with a decapitalized first letter. */
  private static String defaultBeanName(String simpleName) {
    return Introspector.decapitalize(simpleName);
  }

  /**
   * Registers one {@link BeanData} per name declared on the {@code @Bean} method. In case of aliases like
   * {@code @Bean({"a", "b"})} each name is recorded separately in the registry.
   * Falls back to the method name when no name/value attribute is given, matching Spring's own default.
   */
  private void collectBeanMethod(MethodTree method, String pkg) {
    SymbolMetadata beanMeta = method.symbol().metadata();
    List<String> beanNames = extractBeanMethodNames(beanMeta, method);

    String returnTypeFqn = method.returnType() != null
      ? method.returnType().symbolType().fullyQualifiedName()
      : "";
    Set<String> typeHierarchy = method.returnType() != null
      ? collectTypeHierarchy(method.returnType().symbolType().symbol())
      : Set.of();

    var inputFile = context.getInputFile();
    // Unlike class-level beans, a {@code @Bean} method's dependencies come only from its own parameters.
    Map<String, Set<InjectionPoint>> injectionPoints = parameterDependencies(method, inputFile);
    Map<String, Set<String>> paramDeps = toNameMap(injectionPoints);
    boolean isPrimary = beanMeta.isAnnotatedWith(PRIMARY_ANNOTATION);
    var textSpan = AnalyzerMessage.textSpanFor(method.simpleName());

    for (String beanName : beanNames) {
      var beanData = new BeanData(beanName, returnTypeFqn, pkg, inputFile, textSpan, isPrimary, paramDeps, injectionPoints, typeHierarchy);
      collectedBeans.add(beanData);
      beansCollectedAtFileLevel.add(beanData);
    }
  }

  /**
   * Reads the {@code @Bean} method's explicit "value"/"name" attribute (accepting one or several aliases),
   * falling back to the method's own name when none is given, matching Spring's own default.
   */
  private static List<String> extractBeanMethodNames(SymbolMetadata beanMeta, MethodTree method) {
    List<SymbolMetadata.AnnotationValue> attrs = beanMeta.valuesForAnnotation(SpringUtils.BEAN_ANNOTATION);
    List<String> names = new ArrayList<>();
    if (attrs != null) {
      for (SymbolMetadata.AnnotationValue attr : attrs) {
        collectBeanMethodAttributeNames(attr, names);
      }
    }
    if (names.isEmpty()) {
      names.add(method.simpleName().name());
    }
    return names;
  }

  private static void collectBeanMethodAttributeNames(SymbolMetadata.AnnotationValue attr, List<String> names) {
    if ((VALUE_ATTRIBUTE.equals(attr.name()) || "name".equals(attr.name())) && attr.value() instanceof Object[] values) {
      for (Object value : values) {
        if (value instanceof String name && !name.isBlank()) {
          names.add(name);
        }
      }
    }
  }

  /**
   * Collects a class-level bean's dependencies from @Autowired fields/setters/constructors, plus Spring's
   * implicit single-constructor injection: if no constructor is @Autowired and the class has exactly one
   * constructor, Spring uses it for injection without requiring the annotation. {@code hasAutowiredConstructor}
   * guards against misapplying that fallback when an @Autowired constructor already exists alongside other,
   * unannotated ones.
   */
  private static Map<String, Set<InjectionPoint>> collectAutowiredDependencies(ClassTree classTree, InputFile inputFile) {
    Map<String, Set<InjectionPoint>> deps = new LinkedHashMap<>();
    List<MethodTree> unannotatedConstructors = new ArrayList<>();
    boolean hasAutowiredConstructor = false;
    for (Tree member : classTree.members()) {
      if (member instanceof VariableTree field && field.symbol().metadata().isAnnotatedWith(SpringUtils.AUTOWIRED_ANNOTATION)) {
        String typeFqn = field.symbol().type().fullyQualifiedName();
        String name = dependencyKey(field.simpleName().name(), extractQualifier(field.symbol().metadata()));
        var location = new BeanLocation(inputFile, AnalyzerMessage.textSpanFor(field.simpleName()));
        deps.computeIfAbsent(typeFqn, k -> new LinkedHashSet<>()).add(new InjectionPoint(name, location));
      } else if (member instanceof MethodTree method) {
        if (method.symbol().metadata().isAnnotatedWith(SpringUtils.AUTOWIRED_ANNOTATION)) {
          hasAutowiredConstructor |= method.is(Tree.Kind.CONSTRUCTOR);
          parameterDependencies(method, inputFile).forEach((type, points) -> deps.computeIfAbsent(type, k -> new LinkedHashSet<>()).addAll(points));
        } else if (method.is(Tree.Kind.CONSTRUCTOR)) {
          // Held back until the class has been fully scanned, in case an @Autowired constructor appears
          // later among the members and disqualifies the implicit single-constructor rule below.
          unannotatedConstructors.add(method);
        }
      }
    }
    if (!hasAutowiredConstructor && unannotatedConstructors.size() == 1) {
      parameterDependencies(unannotatedConstructors.get(0), inputFile).forEach((type, points) -> deps.computeIfAbsent(type, k -> new LinkedHashSet<>()).addAll(points));
    }
    return deps;
  }

  /**
   * Returns the given method's parameters as injection points, grouped by type FQN: each parameter becomes one
   * {@link InjectionPoint} whose dependency name is its {@code @Qualifier} value if present, otherwise its own
   * name. Shared by {@code @Autowired} constructors/setters and {@code @Bean} factory methods, whose
   * dependencies both come from method parameters.
   */
  private static Map<String, Set<InjectionPoint>> parameterDependencies(MethodTree method, InputFile inputFile) {
    Map<String, Set<InjectionPoint>> deps = new LinkedHashMap<>();
    for (var p : method.parameters()) {
      String typeFqn = p.symbol().type().fullyQualifiedName();
      String name = dependencyKey(p.simpleName().name(), extractQualifier(p.symbol().metadata()));
      var location = new BeanLocation(inputFile, AnalyzerMessage.textSpanFor(p.simpleName()));
      deps.computeIfAbsent(typeFqn, k -> new LinkedHashSet<>()).add(new InjectionPoint(name, location));
    }
    return deps;
  }

  /** Projects each type's injection points down to just their names, discarding location — the flat view stored in {@code BeanDefinitionHolder}. */
  private static Map<String, Set<String>> toNameMap(Map<String, Set<InjectionPoint>> injectionPointsByType) {
    Map<String, Set<String>> names = new LinkedHashMap<>();
    injectionPointsByType.forEach((typeFqn, points) -> names.put(typeFqn, points.stream()
      .map(InjectionPoint::name)
      .collect(Collectors.toCollection(LinkedHashSet::new))));
    return names;
  }

  /**
   * The name Spring uses to resolve this dependency: an explicit @Qualifier
   * value if present, otherwise the field/parameter name.
   */
  private static String dependencyKey(String fieldOrParamName, @Nullable String qualifier) {
    return qualifier != null ? qualifier : fieldOrParamName;
  }

  /**
   * Reads the {@code @Qualifier} value, if any. A missing or blank value is treated as no
   * qualifier, so callers fall back to the field/parameter name via {@link #dependencyKey}.
   */
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

  /**
   * Recursively walks superclasses and interfaces, recording each type's FQN. {@code java.lang.Object} and
   * unknown/unresolved types are excluded, and {@code visited} serves as both the result and a guard against
   * walking the same type twice, e.g. a common ancestor reached through more than one interface.
   */
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
