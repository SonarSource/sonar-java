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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.caching.FileCachingCheck;
import org.sonar.java.model.JUtils;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.telemetry.Telemetry;
import org.sonar.java.utils.PackageUtils;
import org.sonar.java.utils.SpringUtils;
import org.sonar.plugins.java.api.InputFileScannerContext;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.ModuleScannerContext;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.utils.SpringUtils.collectAutowiredDependenciesOnClass;
import static org.sonar.java.utils.SpringUtils.collectDependenciesOnMethod;

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
 *   <li>The {@link ProfileExpression} declared by {@code @Profile}, if any; for {@code @Bean} methods, the
 *       method's own expression is conjoined with (not overridden by) the one declared on the enclosing
 *       {@code @Configuration}/{@code @Component} class, since Spring requires both to match for the bean to
 *       be active</li>
 *   <li>Dependencies via {@code @Autowired} fields, constructors, and setters for class-level beans</li>
 *   <li>Dependencies via method parameters for {@code @Bean} method beans</li>
 *   <li>Implicit single-constructor injection (no {@code @Autowired} required)</li>
 * </ul>
 *
 * <p>Also populates:
 * <ul>
 *   <li>{@link TypeToBeansIndex} with the full type hierarchy of each bean</li>
 *   <li>{@link TypeToDependenciesIndex} with all the dependencies collected by type</li>
 * </ul>
 */
public class BeanDefinitionGatherer extends SpringContextModelGatherer
  implements FileCachingCheck<List<BeanDefinitionHolder.InputFileData>> {

  private static final String PRIMARY_ANNOTATION = "org.springframework.context.annotation.Primary";
  private static final String CACHE_KEY_PREFIX = "java:spring:bean-definitions:";

  private final Map<InputFile, List<BeanDefinitionHolder.InputFileData>> beansCollectedByFile = new LinkedHashMap<>();

  /**
   * Beans found in the file currently being scanned, used for per-file cache writes.
   */
  private final List<BeanDefinitionHolder.InputFileData> beansCollectedAtFileLevel = new ArrayList<>();

  public BeanDefinitionGatherer(Telemetry telemetry) {
    super(telemetry);
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
   * <p>
   * Registers a bean when the class carries a stereotype annotation ({@code @Component},
   * {@code @Service}, {@code @Repository}, {@code @Controller}, {@code @RestController}, {@code @Configuration}),
   * then registers beans for {@code @Bean} factory methods on that same class.
   *
   * @param tree The class tree to visit
   */
  @Override
  protected void visitSpringNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (classTree.simpleName() == null) {
      return;
    }

    SymbolMetadata meta = classTree.symbol().metadata();
    String fqn = classTree.symbol().type().fullyQualifiedName();
    String pkg = PackageUtils.packageNameOf(classTree.symbol());

    if (SpringUtils.STEREOTYPE_ANNOTATIONS.stream().anyMatch(meta::isAnnotatedWith)) {
      String beanName = SpringUtils.extractBeanNameFromAnnotation(meta, classTree.simpleName().name());
      Map<String, Set<InjectionPoint.InputFileData>> dependencies = collectAutowiredDependenciesOnClass(classTree);
      Set<String> typeHierarchy = JUtils.collectTypeHierarchy(classTree.symbol().type());
      ProfileExpression classProfiles = SpringUtils.extractProfileExpression(meta);
      var beanData = new BeanDefinitionHolder.InputFileData(
        beanName, fqn, pkg,
        AnalyzerMessage.textSpanFor(classTree.simpleName()),
        meta.isAnnotatedWith(PRIMARY_ANNOTATION),
        classProfiles,
        SpringUtils.extractQualifierValue(meta),
        dependencies,
        typeHierarchy);
      beansCollectedAtFileLevel.add(beanData);

      for (MethodTree method : SpringUtils.getBeanMethods(classTree)) {
        collectBeanMethod(method, pkg, classProfiles);
      }
    }
  }

  @Override
  protected void leaveSpringFile(JavaFileScannerContext context) {
    var beans = List.copyOf(beansCollectedAtFileLevel);
    beansCollectedByFile.put(context.getInputFile(), beans);
    writeToCache(context, beans);
    beansCollectedAtFileLevel.clear();
  }

  @Override
  public String cacheKeyPrefix() {
    return CACHE_KEY_PREFIX;
  }

  @Override
  public byte[] serialize(List<BeanDefinitionHolder.InputFileData> beans) {
    return SpringContextCacheHelper.serializeBeans(beans);
  }

  @Override
  public List<BeanDefinitionHolder.InputFileData> deserialize(byte[] data) {
    return SpringContextCacheHelper.deserializeBeans(data);
  }

  @Override
  public void restore(InputFileScannerContext context, List<BeanDefinitionHolder.InputFileData> beans) {
    beansCollectedByFile.put(context.getInputFile(), List.copyOf(beans));
  }

  /**
   * Transfers all beans collected across the module into the shared {@link SpringContextModel}, pairing each
   * bean's spans with the file it was collected from to form the {@link BeanLocation}s the model exposes.
   * <p>
   * Registers all encountered bean definitions in {@link BeanDefinitionRegistry},
   * their position in every ancestor/interface type in {@link TypeToBeansIndex}, and
   * each of their dependencies by type in {@link TypeToDependenciesIndex}.
   *
   * @param context            Scanner context used here to access the current module key
   * @param springContextModel Shared cross-module Spring context
   */
  @Override
  public void gatherSpringContextData(ModuleScannerContext context, SpringContextModel springContextModel) {
    beansCollectedByFile.forEach((inputFile, beans) -> {
      for (BeanDefinitionHolder.InputFileData data : beans) {
        var location = new BeanLocation(inputFile, data.textSpan());
        var holderBuilder = new BeanDefinitionHolder.Builder(
          data.type(), context.getModuleKey(), data.beanPackage(), location)
          .dependingBeans(projectToNames(data.dependencies()))
          .profileExpression(data.profileExpression())
          .qualifier(data.qualifier());
        if (data.isPrimary()) {
          holderBuilder.primary();
        }
        springContextModel.getBeanDefinitionRegistry()
          .addBeanDefinition(data.beanName(), holderBuilder.build());
        for (String typeFqn : data.typeHierarchy()) {
          springContextModel.getTypeToBeansIndex().addBeanForType(typeFqn, data.beanName(), context.getModuleKey(), data.beanPackage());
        }
        data.dependencies().forEach((typeFqn, points) -> points.forEach(point -> springContextModel.getTypeToDependenciesIndex()
          .addDependencyForType(typeFqn, point.name(), context.getModuleKey(), data.profileExpression(), new BeanLocation(inputFile, point.span()), point.multiple())));
      }
    });
  }

  @Override
  protected boolean scanSpringFileWithoutParsing(InputFileScannerContext context) {
    return restoreFromCache(context);
  }

  /**
   * Collects {@link BeanDefinitionHolder.InputFileData} for a bean registered with the {@code @Bean} factory method.
   * <p>
   * If multiple aliases are declared (e.g. {@code @Bean({"a", "b"})}), one {@link BeanDefinitionHolder.InputFileData} is
   * registered for each alias.
   *
   * @param method        The {@code @Bean} factory method to visit.
   * @param pkg           The bean's package (carried through to be stored in the bean's serializable data).
   * @param classProfiles The condition declared by the enclosing class's {@code @Profile}.
   */
  private void collectBeanMethod(MethodTree method, String pkg, ProfileExpression classProfiles) {
    SymbolMetadata beanMeta = method.symbol().metadata();
    List<String> beanNames = SpringUtils.extractBeanNameFromMethod(method);

    String returnTypeFqn = method.returnType() != null
      ? method.returnType().symbolType().fullyQualifiedName()
      : "";
    Set<String> typeHierarchy = method.returnType() != null
      ? JUtils.collectTypeHierarchy(method.returnType().symbolType())
      : Set.of();

    // Unlike class-level beans, a {@code @Bean} method's dependencies come only from its own parameters.
    Map<String, Set<InjectionPoint.InputFileData>> dependencies = collectDependenciesOnMethod(method);
    boolean isPrimary = beanMeta.isAnnotatedWith(PRIMARY_ANNOTATION);
    ProfileExpression profiles = ProfileExpression.and(List.of(classProfiles, SpringUtils.extractProfileExpression(beanMeta)));
    String qualifier = SpringUtils.extractQualifierValue(beanMeta);
    var textSpan = AnalyzerMessage.textSpanFor(method.simpleName());

    for (String beanName : beanNames) {
      var beanData = new BeanDefinitionHolder.InputFileData(beanName, returnTypeFqn, pkg, textSpan, isPrimary, profiles, qualifier, dependencies, typeHierarchy);
      beansCollectedAtFileLevel.add(beanData);
    }
  }

  /**
   * Projects each type's injection points down to just their names, discarding spans — the flat view stored in {@code BeanDefinitionHolder}.
   *
   * @param injectionPointsByType Injection points mapped by type, as collected for a bean
   * @return The name of each dependency, mapped by type.
   */
  private static Map<String, Set<String>> projectToNames(Map<String, Set<InjectionPoint.InputFileData>> injectionPointsByType) {
    Map<String, Set<String>> names = new LinkedHashMap<>();
    injectionPointsByType.forEach((typeFqn, points) -> names.put(typeFqn, points.stream()
      .map(InjectionPoint.InputFileData::name)
      .collect(Collectors.toCollection(LinkedHashSet::new))));
    return names;
  }

}
