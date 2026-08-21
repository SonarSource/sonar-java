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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

  private static final String PRIMARY_ANNOTATION = "org.springframework.context.annotation.Primary";

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
    List<String> dependingBeans) {
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
      String beanName = SpringUtils.resolveStereotypeBeanName(meta, classTree.simpleName().name());
      List<String> deps = collectAutowiredDependencies(classTree);
      // Class-level bean (stereotype annotations)
      collectedBeans.add(new BeanData(
        beanName, fqn, pkg,
        context.getInputFile(),
        AnalyzerMessage.textSpanFor(classTree.simpleName()),
        meta.isAnnotatedWith(PRIMARY_ANNOTATION),
        deps));
      beansCollectedAtFileLevel.add(new BeanData(
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
    var deps = String.join(DEP_SEPARATOR, bean.dependingBeans());
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
    List<String> deps = fields[5].isEmpty() ? List.of() : List.of(fields[5].split(DEP_SEPARATOR));
    return new BeanData(beanName, type, beanPackage, inputFile, textSpan, isPrimary, deps);
  }

  private void collectBeanMethod(MethodTree method, String pkg) {
    SymbolMetadata beanMeta = method.symbol().metadata();
    String beanName = SpringUtils.resolveBeanMethodName(method);

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
    beansCollectedAtFileLevel.add(new BeanData(
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
