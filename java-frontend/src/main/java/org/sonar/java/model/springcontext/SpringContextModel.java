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

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.telemetry.SizeEstimable;
import org.sonar.java.telemetry.SizeEstimator;

/**
 * Aggregates all Spring context information collected during project scanning.
 *
 * <p>Acts as the top-level model passed to rules that need to reason about the Spring
 * application context. Each field is a specialized index loaded after Spring context collection:
 * <ul>
 *   <li>{@link BeanDefinitionRegistry} — bean definitions indexed by bean name</li>
 *   <li>{@link ProjectPackageScan} — packages registered for component scanning, per module</li>
 *   <li>{@link TypeToBeansIndex} — bean names indexed by type</li>
 *   <li>{@link EntityClassToPropertiesIndex} — JPA {@code @Entity} class properties</li>
 * </ul>
 */
public class SpringContextModel implements SizeEstimable {
  /**
   * Registry of all bean definitions discovered during scanning.
   */
  private final BeanDefinitionRegistry beanDefinitionRegistry = new BeanDefinitionRegistry();

  /**
   * Packages registered for Spring component scanning, grouped by module.
   */
  private final ProjectPackageScan projectPackageScan = new ProjectPackageScan();

  /**
   * Index for resolving bean names by their fully-qualified type.
   */
  private final TypeToBeansIndex typeToBeansIndex = new TypeToBeansIndex();

  /**
   * Index for storing injected dependencies by their fully-qualified type.
   */
  private final TypeToDependenciesIndex typeToDependenciesIndex = new TypeToDependenciesIndex();

  /**
   * Index of properties associated with Spring Data / Hibernate {@code @Entity} classes.
   */
  private final EntityClassToPropertiesIndex entityClassToPropertiesIndex = new EntityClassToPropertiesIndex();

  /**
   * Builds a new model from every module and file contribution collected during Spring context gathering.
   */
  public static SpringContextModel of(SpringContextGatheringModel gatheringModel) {
    var model = new SpringContextModel();
    gatheringModel.filesData().forEach((moduleKey, files) -> {
      Set<String> packages = new HashSet<>();
      files.values().forEach(fileData -> {
        packages.addAll(fileData.packages());
        model.addBeans(moduleKey, fileData);
      });
      model.projectPackageScan.addPackages(moduleKey, packages);
    });
    return model;
  }

  public BeanDefinitionRegistry getBeanDefinitionRegistry() {
    return beanDefinitionRegistry;
  }

  public ProjectPackageScan getProjectPackageScan() {
    return projectPackageScan;
  }

  public TypeToBeansIndex getTypeToBeansIndex() {
    return typeToBeansIndex;
  }

  public TypeToDependenciesIndex getTypeToDependenciesIndex() {
    return typeToDependenciesIndex;
  }

  public EntityClassToPropertiesIndex getEntityClassToPropertiesIndex() {
    return entityClassToPropertiesIndex;
  }

  @Override
  public long estimateSize(SizeEstimator estimator) {
    return estimator.estimateShallowObject(this, 5, 0)
      + estimator.estimateObject(beanDefinitionRegistry)
      + estimator.estimateObject(projectPackageScan)
      + estimator.estimateObject(typeToBeansIndex)
      + estimator.estimateObject(typeToDependenciesIndex)
      + estimator.estimateObject(entityClassToPropertiesIndex);
  }

  private void addBeans(String moduleKey, SpringContextGatheringModel.InputFileData fileData) {
    InputFile inputFile = fileData.inputFile();
    for (BeanDefinitionHolder.InputFileData data : fileData.beans()) {
      var location = new BeanLocation(inputFile, data.textSpan());
      var holderBuilder = new BeanDefinitionHolder.Builder(data.type(), moduleKey, data.beanPackage(), location)
        .dependingBeans(projectToNames(data.dependencies()))
        .profileExpression(data.profileExpression())
        .qualifier(data.qualifier());
      if (data.isPrimary()) {
        holderBuilder.primary();
      }
      beanDefinitionRegistry.addBeanDefinition(data.beanName(), holderBuilder.build());
      for (String typeFqn : data.typeHierarchy()) {
        typeToBeansIndex.addBeanForType(typeFqn, data.beanName(), moduleKey, data.beanPackage());
      }
      data.dependencies().forEach((typeFqn, points) -> points.forEach(point -> typeToDependenciesIndex
        .addDependencyForType(typeFqn, point.name(), moduleKey, data.profileExpression(), new BeanLocation(inputFile, point.span()), point.multiple())));
    }
  }

  private static Map<String, Set<String>> projectToNames(Map<String, Set<InjectionPoint.InputFileData>> injectionPointsByType) {
    Map<String, Set<String>> names = new LinkedHashMap<>();
    injectionPointsByType.forEach((typeFqn, points) -> names.put(typeFqn, points.stream()
      .map(InjectionPoint.InputFileData::name)
      .collect(Collectors.toCollection(LinkedHashSet::new))));
    return names;
  }
}
