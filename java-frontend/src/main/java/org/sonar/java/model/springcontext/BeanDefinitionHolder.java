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
public class BeanDefinitionHolder {
  /** Fully-qualified class name of the bean. */
  private final String type;

  /** Module in which the bean is declared. */
  private final String module;

  /** Package of the bean's declaring class. */
  private final String beanPackage;

  /** Source location where the bean definition appears. */
  private final BeanLocation location;

  /**
   * Dependencies this bean requires, keyed by required type FQN.
   * Each value is the set of qualifier values or field/parameter names at each injection point of that type.
   */
  private Map<String, Set<String>> dependingBeans;

  /** Comma-separated Spring profile expressions under which this bean is active, or {@code null} if unconditional. */
  @Nullable
  private String profiles;

  /** Whether the bean is marked as {@code @Primary}, making it the preferred candidate for autowiring. */
  private boolean isPrimary = false;

  /** Value of the {@code @Qualifier} annotation declared on the bean itself, or {@code null} if absent. */
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
      holder.setQualifier(qualifier);
      if (isPrimary) {
        holder.setPrimary();
      }
      return holder;
    }
  }
}
