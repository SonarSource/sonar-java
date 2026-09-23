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

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sonar.java.reporting.AnalyzerMessage;

import static org.assertj.core.api.Assertions.assertThat;

class SpringContextModelMetricsTest {

  private static final long EMPTY_CONTEXT_MODEL_SIZE_BYTES = 352L;

  @Test
  void empty_model_has_no_spring_data_and_estimates_all_empty_components() {
    var metrics = SpringContextModelMetrics.of(new SpringContextModel());

    assertThat(metrics.beanCount()).isZero();
    assertThat(metrics.beanNameCount()).isZero();
    assertThat(metrics.injectionPointCount()).isZero();
    assertThat(metrics.componentScanPackageCount()).isZero();
    assertThat(metrics.estimatedSizeInBytes()).isEqualTo(EMPTY_CONTEXT_MODEL_SIZE_BYTES);
  }

  @Test
  void single_bean_is_counted_once() {
    var model = new SpringContextModel();
    long emptySize = SpringContextModelMetrics.of(model).estimatedSizeInBytes();
    model.getBeanDefinitionRegistry().addBeanDefinition("myBean", newHolder("com.acme.MyBean"));

    var metrics = SpringContextModelMetrics.of(model);

    assertThat(metrics.beanCount()).isEqualTo(1);
    assertThat(metrics.beanNameCount()).isEqualTo(1);
    assertThat(metrics.injectionPointCount()).isZero();
    assertThat(metrics.estimatedSizeInBytes()).isGreaterThan(emptySize);
  }

  @Test
  void duplicate_bean_names_are_counted_separately_by_bean_count_only() {
    var model = new SpringContextModel();
    model.getBeanDefinitionRegistry().addBeanDefinition("myBean", newHolder("com.acme.MyBean"));
    model.getBeanDefinitionRegistry().addBeanDefinition("myBean", newHolder("com.acme.OtherBean"));

    var metrics = SpringContextModelMetrics.of(model);

    assertThat(metrics.beanCount()).isEqualTo(2);
    assertThat(metrics.beanNameCount()).isEqualTo(1);
  }

  @Test
  void injection_points_are_summed_over_all_required_types() {
    var model = new SpringContextModel();
    long emptySize = SpringContextModelMetrics.of(model).estimatedSizeInBytes();
    var holder = new BeanDefinitionHolder.Builder("com.acme.MyBean", "module-a", "com.acme", newLocation())
      .dependingBeans(Map.of("com.acme.Collaborator", Set.of("collaborator")))
      .profileExpression(ProfileExpression.not(ProfileExpression.profile("test")))
      .build();
    model.getBeanDefinitionRegistry().addBeanDefinition("myBean", holder);
    model.getTypeToDependenciesIndex().addDependencyForType("com.acme.Collaborator", "collaborator", "module-a", newLocation(), false);
    model.getTypeToDependenciesIndex().addDependencyForType("com.acme.Collaborator", "otherCollaborator", "module-a", newLocation(), false);
    model.getTypeToDependenciesIndex().addDependencyForType("com.acme.Repository", "repository", "module-a", newLocation(), false);

    var metrics = SpringContextModelMetrics.of(model);

    assertThat(metrics.injectionPointCount()).isEqualTo(3);
    assertThat(metrics.estimatedSizeInBytes()).isGreaterThan(emptySize);
  }

  @Test
  void component_scan_packages_are_summed_over_all_modules() {
    var model = new SpringContextModel();
    model.getProjectPackageScan().addPackages("module-a", List.of("com.acme.a", "com.acme.shared"));
    model.getProjectPackageScan().addPackage("module-b", "com.acme.b");

    var metrics = SpringContextModelMetrics.of(model);

    assertThat(metrics.componentScanPackageCount()).isEqualTo(3);
  }

  @Test
  void bean_names_by_type_are_not_counted_as_component_scan_packages() {
    var model = new SpringContextModel();
    long emptySize = SpringContextModelMetrics.of(model).estimatedSizeInBytes();
    model.getTypeToBeansIndex().addBeanForType("com.acme.MyBean", "myBean", "module-a", "com.acme");
    model.getTypeToBeansIndex().addBeanForType("com.acme.MyBean", "myOtherBean", "module-a", "com.acme");

    var metrics = SpringContextModelMetrics.of(model);

    assertThat(metrics.componentScanPackageCount()).isZero();
    assertThat(metrics.estimatedSizeInBytes()).isGreaterThan(emptySize);
  }

  @Test
  void depending_beans_are_not_counted_as_component_scan_packages() {
    var model = new SpringContextModel();
    var holder = new BeanDefinitionHolder.Builder("com.acme.MyBean", "module-a", "com.acme", newLocation())
      .dependingBeans(Map.of("com.acme.Collaborator", Set.of("collaborator", "otherCollaborator")))
      .build();
    model.getBeanDefinitionRegistry().addBeanDefinition("myBean", holder);

    var metrics = SpringContextModelMetrics.of(model);

    assertThat(metrics.beanCount()).isEqualTo(1);
    assertThat(metrics.componentScanPackageCount()).isZero();
  }

  @Test
  void shared_bean_definition_instance_is_counted_per_occurrence_but_charged_once() {
    var sharedHolder = newHolder("com.acme.MyBean");
    var sharing = new SpringContextModel();
    sharing.getBeanDefinitionRegistry().addBeanDefinition("myBean", sharedHolder);
    sharing.getBeanDefinitionRegistry().addBeanDefinition("myOtherBean", sharedHolder);

    var duplicating = new SpringContextModel();
    duplicating.getBeanDefinitionRegistry().addBeanDefinition("myBean", newHolder("com.acme.MyBean"));
    duplicating.getBeanDefinitionRegistry().addBeanDefinition("myOtherBean", newHolder("com.acme.MyBean"));

    var metrics = SpringContextModelMetrics.of(sharing);

    assertThat(metrics.beanCount()).isEqualTo(2);
    assertThat(metrics.beanNameCount()).isEqualTo(2);
    assertThat(metrics.estimatedSizeInBytes())
      .isLessThan(SpringContextModelMetrics.of(duplicating).estimatedSizeInBytes());
  }

  @Test
  void entity_properties_contribute_to_the_size_but_to_no_counter() {
    var model = new SpringContextModel();
    long emptySize = SpringContextModelMetrics.of(model).estimatedSizeInBytes();
    model.getEntityClassToPropertiesIndex().addProperty("com.acme.MyEntity", "table", "my_entity");

    var metrics = SpringContextModelMetrics.of(model);

    assertThat(metrics.beanCount()).isZero();
    assertThat(metrics.estimatedSizeInBytes()).isGreaterThan(emptySize);
  }

  @Test
  void shared_string_instances_are_charged_once() {
    String beanName = "aBeanNameLongEnoughToMakeTheDifferenceVisible";

    var sharing = new SpringContextModel();
    String sharedName = new String(beanName);
    sharing.getBeanDefinitionRegistry().addBeanDefinition(sharedName, newHolder("com.acme.MyBean"));
    sharing.getTypeToBeansIndex().addBeanForType("com.acme.MyBean", sharedName, "module-a", "com.acme");

    var duplicating = new SpringContextModel();
    duplicating.getBeanDefinitionRegistry().addBeanDefinition(new String(beanName), newHolder("com.acme.MyBean"));
    duplicating.getTypeToBeansIndex().addBeanForType("com.acme.MyBean", new String(beanName), "module-a", "com.acme");

    long sharingSize = SpringContextModelMetrics.of(sharing).estimatedSizeInBytes();
    long duplicatingSize = SpringContextModelMetrics.of(duplicating).estimatedSizeInBytes();

    assertThat(sharingSize).isLessThan(duplicatingSize);
    assertThat(duplicatingSize - sharingSize).isGreaterThanOrEqualTo(beanName.length());
  }

  @Test
  void size_never_decreases_as_the_model_grows() {
    var model = new SpringContextModel();
    long afterEmpty = SpringContextModelMetrics.of(model).estimatedSizeInBytes();

    model.getProjectPackageScan().addPackage("module-a", "com.acme");
    long afterPackage = SpringContextModelMetrics.of(model).estimatedSizeInBytes();

    model.getBeanDefinitionRegistry().addBeanDefinition("myBean", newHolder("com.acme.MyBean"));
    long afterBean = SpringContextModelMetrics.of(model).estimatedSizeInBytes();

    model.getTypeToBeansIndex().addBeanForType("com.acme.MyBean", "myBean", "module-a", "com.acme");
    long afterBeanName = SpringContextModelMetrics.of(model).estimatedSizeInBytes();

    model.getTypeToDependenciesIndex().addDependencyForType("com.acme.Collaborator", "collaborator", "module-a", newLocation(), false);
    long afterDependency = SpringContextModelMetrics.of(model).estimatedSizeInBytes();

    assertThat(afterEmpty)
      .isLessThan(afterPackage)
      .isLessThan(afterBean);
    assertThat(afterPackage).isLessThan(afterBean);
    assertThat(afterBean).isLessThanOrEqualTo(afterBeanName);
    assertThat(afterBeanName).isLessThan(afterDependency);
  }

  @Test
  void metrics_are_stable_across_repeated_walks() {
    var model = new SpringContextModel();
    model.getBeanDefinitionRegistry().addBeanDefinition("myBean", newHolder("com.acme.MyBean"));
    model.getTypeToBeansIndex().addBeanForType("com.acme.MyBean", "myBean", "module-a", "com.acme");
    model.getTypeToDependenciesIndex().addDependencyForType("com.acme.Collaborator", "collaborator", "module-a", newLocation(), false);
    model.getEntityClassToPropertiesIndex().addProperty("com.acme.MyEntity", "table", "my_entity");
    model.getProjectPackageScan().addPackage("module-a", "com.acme");

    assertThat(SpringContextModelMetrics.of(model)).isEqualTo(SpringContextModelMetrics.of(model));
  }

  private static BeanDefinitionHolder newHolder(String type) {
    return new BeanDefinitionHolder.Builder(type, "module-a", "com.acme", newLocation()).build();
  }

  private static BeanLocation newLocation() {
    return new BeanLocation(null, new AnalyzerMessage.TextSpan(1));
  }
}
