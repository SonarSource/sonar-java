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
package org.sonar.java.checks.spring;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.model.springcontext.BeanDefinitionHolder;
import org.sonar.java.model.springcontext.BeanDefinitionKind;
import org.sonar.java.model.springcontext.ProjectPackageScan;
import org.sonar.java.model.springcontext.SpringContextModel;
import org.sonar.plugins.java.api.JavaCheck;

/**
 * Not an AST visitor: called directly by {@code SpringContextModelSensor} once the {@link SpringContextModel}
 * has been fully populated by the gatherers, since deciding whether a bean is reachable requires knowing every
 * package scanned anywhere in the analyzed scope, not a single file.
 */
@Rule(key = "S4605")
public class SpringBeansShouldBeAccessibleCheck implements JavaCheck, SpringContextCheck {

  private static final String MESSAGE_FORMAT = "'%s' is not reachable by @ComponentScan or @SpringBootApplication. "
    + "Either move it to a package configured in @ComponentScan or update your @ComponentScan configuration.";

  /**
   * Creates the list of issues using the Spring context model.
   * Only beans declared by a stereotype annotation are considered: a {@code @Bean} factory method is registered by
   * the configuration class that declares it, so its own package is irrelevant, and a {@code @Configuration} class
   * may legitimately be registered without being scanned.
   * A bean is reported when its package is covered by no {@code @ComponentScan} or {@code @SpringBootApplication}
   * declared in any module of the project, since a component scan in one module can reach a bean in another.
   * Issues are deduplicated because a single bean type may be registered under several names.
   *
   * @param model the Spring context model of the project
   */
  @Override
  public List<SpringContextIssue> execute(SpringContextModel model) {
    Set<String> scannedPackages = allScannedPackages(model.getProjectPackageScan());
    return model.getBeanDefinitionRegistry().getAllBeanDefinitions().stream()
      .filter(bean -> bean.getKind() == BeanDefinitionKind.STEREOTYPE)
      .filter(bean -> isUncovered(bean, scannedPackages))
      .map(SpringBeansShouldBeAccessibleCheck::issue)
      .distinct()
      .toList();
  }

  private static Set<String> allScannedPackages(ProjectPackageScan projectPackageScan) {
    return projectPackageScan.getModules().stream()
      .flatMap(module -> projectPackageScan.getPackagesForModule(module).stream())
      .collect(Collectors.toUnmodifiableSet());
  }

  private static boolean isUncovered(BeanDefinitionHolder bean, Set<String> scannedPackages) {
    return scannedPackages.stream().noneMatch(scannedPackage -> isWithinPackage(bean.getBeanPackage(), scannedPackage));
  }

  /**
   * A scanned package covers its own members and everything below it. The empty package is the default package,
   * which a scan rooted there covers entirely.
   */
  private static boolean isWithinPackage(String beanPackage, String scannedPackage) {
    return scannedPackage.isEmpty() || beanPackage.equals(scannedPackage) || beanPackage.startsWith(scannedPackage + ".");
  }

  private static SpringContextIssue issue(BeanDefinitionHolder bean) {
    return new SpringContextIssue(bean.getLocation(), String.format(MESSAGE_FORMAT, simpleName(bean.getType())));
  }

  /**
   * A nested type's fully qualified name joins its enclosing class with {@code $} rather than {@code .}, so the last
   * segment is whichever separator comes last.
   */
  private static String simpleName(String fullyQualifiedName) {
    int packageSeparator = fullyQualifiedName.lastIndexOf('.');
    int nestedClassSeparator = fullyQualifiedName.lastIndexOf('$');
    return fullyQualifiedName.substring(Math.max(packageSeparator, nestedClassSeparator) + 1);
  }

}
