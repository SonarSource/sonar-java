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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.java.reporting.AnalyzerMessage;

/**
 * Immutable representation of a Spring bean definition discovered during project scanning.
 *
 * <p>Captures the bean's fully-qualified type, the module and package it belongs to,
 * its source {@link BeanLocation location}, the {@link ProfileExpression condition} under which it is
 * active, its dependency names, and whether the bean is marked as {@code @Primary}.
 *
 * <p>Use {@link Builder} to construct instances:
 * <pre>{@code
 * BeanDefinitionHolder bean = new BeanDefinitionHolder.Builder(type, module, pkg, location)
 *     .profileExpression(ProfileExpression.profile("prod"))
 *     .primary()
 *     .build();
 * }</pre>
 *
 * @see BeanDefinitionRegistry
 * @see BeanLocation
 */
public class BeanDefinitionHolder {
  /**
   * Fully-qualified class name of the bean.
   */
  private final String type;

  /**
   * Module in which the bean is declared.
   */
  private final String module;

  /**
   * Package of the bean's declaring class.
   */
  private final String beanPackage;

  /**
   * Source location where the bean definition appears.
   */
  private final BeanLocation location;

  /**
   * Dependencies this bean requires, keyed by required type FQN.
   * Each value is the set of qualifier values or field/parameter names at each injection point of that type.
   */
  private Map<String, Set<String>> dependingBeans;

  /**
   * Condition under which this bean is active, as declared by {@code @Profile}. A bean carrying no
   * {@code @Profile} holds {@link ProfileExpression#UNCONDITIONAL}, so this is never null. For a
   * {@code @Bean} method, the class-level and method-level expressions are already composed here, since
   * Spring requires both to match.
   */
  private ProfileExpression profileExpression = ProfileExpression.UNCONDITIONAL;

  /**
   * Whether the bean is marked as {@code @Primary}, making it the preferred candidate for autowiring.
   */
  private boolean isPrimary = false;

  private BeanDefinitionHolder(String type, String module, String beanPackage, BeanLocation location) {
    this.type = type;
    this.module = module;
    this.beanPackage = beanPackage;
    this.location = location;
  }

  private void setDependingBeans(Map<String, Set<String>> beans) {
    this.dependingBeans = beans;
  }

  private void setProfileExpression(ProfileExpression profileExpression) {
    this.profileExpression = profileExpression;
  }

  private void setPrimary() {
    this.isPrimary = true;
  }

  public String getType() {
    return type;
  }

  public String getModule() {
    return module;
  }

  public String getBeanPackage() {
    return beanPackage;
  }

  public BeanLocation getLocation() {
    return location;
  }

  public Map<String, Set<String>> getDependingBeans() {
    return dependingBeans;
  }

  public ProfileExpression getProfileExpression() {
    return profileExpression;
  }

  public boolean isPrimary() {
    return isPrimary;
  }

  public static class Builder {
    private final String type;
    private final String module;
    private final String beanPackage;
    private final BeanLocation location;
    private Map<String, Set<String>> dependingBeans = new LinkedHashMap<>();
    private ProfileExpression profileExpression = ProfileExpression.UNCONDITIONAL;
    private boolean isPrimary = false;

    public Builder(String type, String module, String beanPackage, BeanLocation location) {
      this.type = type;
      this.module = module;
      this.beanPackage = beanPackage;
      this.location = location;
    }

    public Builder dependingBeans(Map<String, Set<String>> beans) {
      this.dependingBeans = beans;
      return this;
    }

    public Builder profileExpression(ProfileExpression profileExpression) {
      this.profileExpression = profileExpression;
      return this;
    }

    public Builder primary() {
      this.isPrimary = true;
      return this;
    }

    public BeanDefinitionHolder build() {
      BeanDefinitionHolder holder = new BeanDefinitionHolder(type, module, beanPackage, location);
      holder.setDependingBeans(dependingBeans.entrySet().stream()
        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> Set.copyOf(e.getValue()))));
      holder.setProfileExpression(profileExpression);
      if (isPrimary) {
        holder.setPrimary();
      }
      return holder;
    }
  }

  /**
   * A bean definition as collected from a single file, holding no reference to that file.
   *
   * <p>This is the form kept by {@link BeanDefinitionGatherer} and written to the cache. Locations are text spans
   * only, the file they belong to being the one the bean is stored under, which is what makes a cache entry
   * readable without knowing which file it describes. Pairing it with that file yields the
   * {@link BeanDefinitionHolder} the model exposes.
   *
   * @param beanName          The name the bean is registered under.
   * @param type              The fully-qualified name of the bean's type.
   * @param beanPackage       The package of the class declaring the bean.
   * @param textSpan          The text span identifying the bean declaration within its own file.
   * @param isPrimary         Whether the bean is annotated with {@code @Primary}.
   * @param profileExpression The condition under which the bean is active, {@link ProfileExpression#UNCONDITIONAL} if it carries no {@code @Profile}.
   * @param dependencies      The bean's dependencies, mapped by required type FQN to the injection points that require them.
   * @param typeHierarchy     The fully-qualified names of the bean's own type and of all its ancestors and interfaces.
   */
  public record InputFileData(
    String beanName,
    String type,
    String beanPackage,
    AnalyzerMessage.TextSpan textSpan,
    boolean isPrimary,
    ProfileExpression profileExpression,
    Map<String, Set<InjectionPoint.InputFileData>> dependencies,
    Set<String> typeHierarchy) {
  }
}
