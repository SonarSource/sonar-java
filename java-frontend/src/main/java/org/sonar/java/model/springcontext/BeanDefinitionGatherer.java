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
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.caching.FileCachingCheck;
import org.sonar.java.model.JUtils;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.telemetry.Telemetry;
import org.sonar.java.utils.PackageUtils;
import org.sonar.java.utils.SpringUtils;
import org.sonar.plugins.java.api.InputFileScannerContext;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.utils.SpringUtils.collectAutowiredDependenciesOnClass;
import static org.sonar.java.utils.SpringUtils.collectDependenciesOnMethod;

/**
 * Collects Spring bean definitions discovered during AST traversal.
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
 */
public class BeanDefinitionGatherer extends SpringContextModelGatherer
  implements FileCachingCheck<List<BeanDefinitionHolder.InputFileData>> {

  private static final String PRIMARY_ANNOTATION = "org.springframework.context.annotation.Primary";
  private static final String CACHE_KEY_PREFIX = "java:spring:bean-definitions:";

  /**
   * Beans found in the file currently being scanned, used for per-file cache writes.
   */
  private final List<BeanDefinitionHolder.InputFileData> beansCollectedAtFileLevel = new ArrayList<>();

  public BeanDefinitionGatherer(SpringContextGatheringModel springContextGatheringModel, Telemetry telemetry) {
    super(springContextGatheringModel, telemetry);
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
    InputFile currentFile = context.getInputFile();
    springContextGatheringModel.collectBeans(context.getModuleKey(), currentFile.key(), currentFile, beans);
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
    InputFile currentFile = context.getInputFile();
    springContextGatheringModel.collectBeans(context.getModuleKey(), currentFile.key(), currentFile, beans);
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

}
