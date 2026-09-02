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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.model.JUtils;
import org.sonar.java.model.springcontext.TypeToDependenciesIndex.InjectionPoint;
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
 *   <li>{@code @Profile} expression, if any; for {@code @Bean} methods, the method's own {@code @Profile}
 *       is combined with (not overridden by) the one declared on the enclosing {@code @Configuration}/{@code @Component}
 *       class, since Spring requires both to match for the bean to be active</li>
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

  /** Joins the values of a single {@code @Profile} annotation, which are OR-ed together by Spring. */
  private static final String PROFILE_SEPARATOR = ",";
  /** Joins the class-level and method-level {@code @Profile} expressions of a {@code @Bean} method, which are AND-ed together by Spring. */
  private static final String PROFILE_AND_SEPARATOR = ";";
  private static final String VALUE_ATTRIBUTE = "value";

  private static final String PRIMARY_ANNOTATION = "org.springframework.context.annotation.Primary";

  private final List<BeanData> collectedBeans = new ArrayList<>();

  /** Beans found in the file currently being scanned, used for per-file cache writes. */
  private final List<BeanData> beansCollectedAtFileLevel = new ArrayList<>();

  record BeanData(
    String beanName,
    String type,
    String beanPackage,
    InputFile inputFile,
    AnalyzerMessage.TextSpan textSpan,
    boolean isPrimary,
    @Nullable String profiles,
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
   * Visits class nodes and registers all beans defined in the class.
   *
   * Registers a bean when the class carries a stereotype annotation ({@code @Component},
   * {@code @Service}, {@code @Repository}, {@code @Controller}, {@code @RestController}, {@code @Configuration}),
   * then registers beans for {@code @Bean} factory methods on that same class.
   *
   * @param tree The class tree to visit
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
      String beanName = SpringUtils.extractBeanName(meta, classTree.simpleName().name());
      // collect autowired dependencies as InjectionPoints to store in TypeToDependenciesIndex
      Map<String, Set<InjectionPoint>> injectionPoints = collectAutowiredDependencies(classTree, context.getInputFile());
      // also collect their names mapped by type to store in dependingBeans
      Map<String, Set<String>> deps = toNameMap(injectionPoints);
      Set<String> typeHierarchy = JUtils.collectTypeHierarchy(classTree.symbol());
      String classProfiles = extractProfiles(meta);
      var beanData = new BeanData(
        beanName, fqn, pkg,
        context.getInputFile(),
        AnalyzerMessage.textSpanFor(classTree.simpleName()),
        meta.isAnnotatedWith(PRIMARY_ANNOTATION),
        classProfiles,
        deps,
        injectionPoints,
        typeHierarchy);
      collectedBeans.add(beanData);
      beansCollectedAtFileLevel.add(beanData);

      for (MethodTree method : SpringUtils.getBeanMethods(classTree)) {
        collectBeanMethod(method, pkg, classProfiles);
      }
    }
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    if (context.getCacheContext().isCacheEnabled()) {
      SpringContextCacheHelper.writeBeanDefinitionsToCache(context, LOG, beansCollectedAtFileLevel);
    }
    beansCollectedAtFileLevel.clear();
  }

  /**
   * Transfers all beans collected across the module into the shared {@link SpringContextModel}.
   *
   * Registers all encountered bean definitions in {@link BeanDefinitionRegistry},
   * their position in every ancestor/interface type in {@link TypeToBeanNamesIndex}, and
   * each of their dependencies by type in {@link TypeToDependenciesIndex}.
   *
   * @param context Scanner context used here to access the current module key
   * @param springContextModel Shared cross-module Spring context
   */
  @Override
  public void gatherSpringContextData(ModuleScannerContext context, SpringContextModel springContextModel) {
    for (BeanData data : collectedBeans) {
      var location = new BeanLocation(data.inputFile(), data.textSpan());
      var holderBuilder = new BeanDefinitionHolder.Builder(
        data.type(), context.getModuleKey(), data.beanPackage(), location)
        .dependingBeans(data.dependingBeans())
        .profiles(data.profiles());
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
    return SpringContextCacheHelper.readBeanDefinitionsFromCache(ctx, LOG).map(beans -> {
      collectedBeans.addAll(beans);
      return true;
    }).orElse(false);
  }

  /**
   * Collects {@link BeanData} for a bean registered with the {@code @Bean} factory method.
   *
   * If multiple aliases are declared (e.g. {@code @Bean({"a", "b"})}), one {@link BeanData} is
   * registered for each alias.
   *
   * @param method The {@code @Bean} factory method to visit
   * @param pkg The bean's package (carried through to be stored in BeanData)
   * @param classProfiles The {@code @Profile} expression declared on the enclosing class, if any
   */
  private void collectBeanMethod(MethodTree method, String pkg, @Nullable String classProfiles) {
    SymbolMetadata beanMeta = method.symbol().metadata();
    List<String> beanNames = SpringUtils.extractBeanMethodNames(beanMeta, method);

    String returnTypeFqn = method.returnType() != null
      ? method.returnType().symbolType().fullyQualifiedName()
      : "";
    Set<String> typeHierarchy = method.returnType() != null
      ? JUtils.collectTypeHierarchy(method.returnType().symbolType().symbol())
      : Set.of();

    var inputFile = context.getInputFile();
    // Unlike class-level beans, a {@code @Bean} method's dependencies come only from its own parameters.
    Map<String, Set<InjectionPoint>> injectionPoints = parameterDependencies(method, inputFile);
    Map<String, Set<String>> paramDeps = toNameMap(injectionPoints);
    boolean isPrimary = beanMeta.isAnnotatedWith(PRIMARY_ANNOTATION);
    String ownProfiles = extractProfiles(beanMeta);
    String profiles = composeProfiles(classProfiles, ownProfiles);
    var textSpan = AnalyzerMessage.textSpanFor(method.simpleName());

    for (String beanName : beanNames) {
      var beanData = new BeanData(beanName, returnTypeFqn, pkg, inputFile, textSpan, isPrimary, profiles, paramDeps, injectionPoints, typeHierarchy);
      collectedBeans.add(beanData);
      beansCollectedAtFileLevel.add(beanData);
    }
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
  private static Map<String, Set<InjectionPoint>> collectAutowiredDependencies(ClassTree classTree, InputFile inputFile) {
    Map<String, Set<InjectionPoint>> deps = new LinkedHashMap<>();
    List<MethodTree> unannotatedConstructors = new ArrayList<>();
    boolean hasAutowiredConstructor = false;
    for (Tree member : classTree.members()) {
      if (member instanceof VariableTree field && field.symbol().metadata().isAnnotatedWith(SpringUtils.AUTOWIRED_ANNOTATION)) {
        String typeFqn = field.symbol().type().fullyQualifiedName();
        String name = dependencyKey(field.simpleName().name(), SpringUtils.extractQualifier(field.symbol().metadata()));
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
   * Collect the given method's parameters as dependencies.
   *
   * @param method Method whose parameters are stored as dependencies, either {@code @Autowired} constructors/setters or
   * {@code @Bean} factory methods
   * @param inputFile The file {@code method} was parsed from, used to locate each injection point
   * @return The collected dependencies, mapped by required type FQN to the {@link InjectionPoint}s that require it
   */
  private static Map<String, Set<InjectionPoint>> parameterDependencies(MethodTree method, InputFile inputFile) {
    Map<String, Set<InjectionPoint>> deps = new LinkedHashMap<>();
    for (var p : method.parameters()) {
      String typeFqn = p.symbol().type().fullyQualifiedName();
      String name = dependencyKey(p.simpleName().name(), SpringUtils.extractQualifier(p.symbol().metadata()));
      var location = new BeanLocation(inputFile, AnalyzerMessage.textSpanFor(p.simpleName()));
      deps.computeIfAbsent(typeFqn, k -> new LinkedHashSet<>()).add(new InjectionPoint(name, location));
    }
    return deps;
  }

  /** Projects each type's injection points down to just their names, discarding location — the flat view stored in {@code BeanDefinitionHolder}. */
  static Map<String, Set<String>> toNameMap(Map<String, Set<InjectionPoint>> injectionPointsByType) {
    Map<String, Set<String>> names = new LinkedHashMap<>();
    injectionPointsByType.forEach((typeFqn, points) -> names.put(typeFqn, points.stream()
      .map(InjectionPoint::name)
      .collect(Collectors.toCollection(LinkedHashSet::new))));
    return names;
  }

  private static String dependencyKey(String fieldOrParamName, @Nullable String qualifier) {
    return qualifier != null ? qualifier : fieldOrParamName;
  }

  /**
   * Reads the {@code @Profile} annotation's "value" attribute, joining every profile name it lists.
   *
   * @param metadata The symbol metadata of the class or {@code @Bean} method to check for a {@code @Profile}
   * @return The joined profile expression, or {@code null} if none is declared
   */
  @Nullable
  private static String extractProfiles(SymbolMetadata metadata) {
    List<SymbolMetadata.AnnotationValue> attrs = metadata.valuesForAnnotation(SpringUtils.PROFILE_ANNOTATION);
    if (attrs == null) {
      return null;
    }
    List<String> profiles = attrs.stream()
      .filter(v -> VALUE_ATTRIBUTE.equals(v.name()))
      .flatMap(v -> {
        Object val = v.value();
        if (val instanceof Object[] arr) {
          return Arrays.stream(arr).filter(String.class::isInstance).map(String.class::cast);
        }
        return Stream.empty();
      })
      .filter(s -> !s.isBlank())
      .toList();
    return profiles.isEmpty() ? null : String.join(PROFILE_SEPARATOR, profiles);
  }

  /**
   * Combines a {@code @Bean} method's own {@code @Profile} with the one declared on its enclosing class.
   * Spring requires both to match for the bean to be active, so the two expressions are AND-ed rather
   * than one overriding the other.
   */
  @Nullable
  private static String composeProfiles(@Nullable String classProfiles, @Nullable String ownProfiles) {
    if (classProfiles == null) {
      return ownProfiles;
    }
    if (ownProfiles == null) {
      return classProfiles;
    }
    return classProfiles + PROFILE_AND_SEPARATOR + ownProfiles;
  }

}
