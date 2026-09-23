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
import javax.annotation.Nullable;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.telemetry.SizeEstimable;
import org.sonar.java.telemetry.SizeEstimator;

/**
 * Immutable representation of a Spring bean definition discovered during project scanning.
 *
 * <p>Captures the bean's fully-qualified type, the module and package it belongs to,
 * its source {@link BeanLocation location}, and optional metadata such as active profiles,
 * dependency names, and whether the bean is marked as {@code @Primary}.
 *
 * <p>Use {@link Builder} to construct instances:
 * <pre>{@code
 * BeanDefinitionHolder bean = new BeanDefinitionHolder.Builder(type, module, pkg, location)
 *     .profiles("prod")
 *     .primary()
 *     .build();
 * }</pre>
 *
 * @see BeanDefinitionRegistry
 * @see BeanLocation
 */
public class BeanDefinitionHolder implements SizeEstimable {
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
   * Spring profile expression under which this bean is active, or {@code null} if unconditional.
   * Comma-separated values within one {@code @Profile} annotation are OR-ed (as Spring does);
   * a class-level and a {@code @Bean} method-level {@code @Profile} are AND-ed by joining their
   * (already OR-ed) expressions with a semicolon, since Spring requires both to match.
   */
  @Nullable
  private String profiles;

  /**
   * Whether the bean is marked as {@code @Primary}, making it the preferred candidate for autowiring.
   */
  private boolean isPrimary = false;

  /**
   * The value of the {@code @Qualifier} annotation on the bean definition, if present.
   * Allows consumers to select this bean by qualifier value rather than by bean name.
   */
  @Nullable
  private String qualifier;

  private BeanDefinitionHolder(String type, String module, String beanPackage, BeanLocation location) {
    this.type = type;
    this.module = module;
    this.beanPackage = beanPackage;
    this.location = location;
  }

  private void setDependingBeans(Map<String, Set<String>> beans) {
    this.dependingBeans = beans;
  }

  private void setProfiles(@Nullable String profiles) {
    this.profiles = profiles;
  }

  private void setPrimary() {
    this.isPrimary = true;
  }

  private void setQualifier(@Nullable String qualifier) {
    this.qualifier = qualifier;
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

  @Nullable
  public String getProfiles() {
    return profiles;
  }

  public boolean isPrimary() {
    return isPrimary;
  }

  @Nullable
  public String getQualifier() {
    return qualifier;
  }

  @Override
  public long estimateSize(SizeEstimator estimator) {
    long size = estimator.estimateShallowObject(this, 7, 1)
      + estimator.estimateString(type)
      + estimator.estimateString(module)
      + estimator.estimateString(beanPackage)
      + estimator.estimateString(profiles)
      + estimator.estimateString(qualifier)
      + estimator.estimateObject(location)
      + estimator.estimateMap(dependingBeans);
    for (var entry : dependingBeans.entrySet()) {
      size += estimator.estimateString(entry.getKey()) + estimator.estimateSet(entry.getValue());
      for (var dependencyName : entry.getValue()) {
        size += estimator.estimateString(dependencyName);
      }
    }
    return size;
  }

  public static class Builder {
    private final String type;
    private final String module;
    private final String beanPackage;
    private final BeanLocation location;
    private Map<String, Set<String>> dependingBeans = new LinkedHashMap<>();
    @Nullable
    private String profiles;
    private boolean isPrimary = false;
    @Nullable
    private String qualifier;

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

    public Builder profiles(@Nullable String profiles) {
      this.profiles = profiles;
      return this;
    }

    public Builder primary() {
      this.isPrimary = true;
      return this;
    }

    public Builder qualifier(@Nullable String qualifier) {
      this.qualifier = qualifier;
      return this;
    }

    public BeanDefinitionHolder build() {
      BeanDefinitionHolder holder = new BeanDefinitionHolder(type, module, beanPackage, location);
      holder.setDependingBeans(dependingBeans.entrySet().stream()
        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> Set.copyOf(e.getValue()))));
      holder.setProfiles(profiles);
      if (isPrimary) {
        holder.setPrimary();
      }
      holder.setQualifier(qualifier);
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
   * @param beanName      the name the bean is registered under
   * @param type          fully-qualified name of the bean's type
   * @param beanPackage   package of the class declaring the bean
   * @param textSpan      the text span identifying the bean declaration within its own file
   * @param isPrimary     whether the bean is annotated with {@code @Primary}
   * @param profiles      the {@code @Profile} expression under which the bean is active, or {@code null} if unconditional
   * @param dependencies  the bean's dependencies, mapped by required type FQN to the injection points that require them
   * @param typeHierarchy fully-qualified names of the bean's own type and of all its ancestors and interfaces
   */
  public record InputFileData(
    String beanName,
    String type,
    String beanPackage,
    AnalyzerMessage.TextSpan textSpan,
    boolean isPrimary,
    @Nullable String profiles,
    @Nullable String qualifier,
    Map<String, Set<InjectionPoint.InputFileData>> dependencies,
    Set<String> typeHierarchy) {
  }
}
