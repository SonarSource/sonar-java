/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.checks.maven.helpers;

import org.sonar.maven.model.maven2.Build;
import org.sonar.maven.model.maven2.BuildBase;
import org.sonar.maven.model.maven2.Dependency;
import org.sonar.maven.model.maven2.DependencyManagement;
import org.sonar.maven.model.maven2.DependencyManagement.Dependencies;
import org.sonar.maven.model.maven2.MavenProject;
import org.sonar.maven.model.maven2.MavenProject.Profiles;
import org.sonar.maven.model.maven2.Plugin;
import org.sonar.maven.model.maven2.Profile;

import javax.annotation.Nullable;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Class to be used to collect and filter dependencies from a Maven project pom.xml file
 */
public class MavenDependencyCollector {

  private List<Dependency> dependencies;
  private List<MavenDependencyNameMatcher> nameMatchers = Collections.emptyList();
  private MavenDependencyVersionMatcher versionMatcher = MavenDependencyVersionMatcher.alwaysMatchingVersionMatcher();
  private final MavenProject mavenProject;

  private MavenDependencyCollector(MavenProject mavenProject) {
    this.mavenProject = mavenProject;
  }

  /**
   * Create a default {@link MavenDependencyCollector} for a given {@link MavenProject}. Only manner to get a Collector.
   * @param mavenProject The maven project to inspect
   * @return a new instance of {@link MavenDependencyCollector}
   */
  public static MavenDependencyCollector forMavenProject(MavenProject mavenProject) {
    return new MavenDependencyCollector(mavenProject);
  }

  /**
   * Define the name matchers to be used when collecting dependencies. Optional.
   * @param matchers The list of matchers to be used
   * @return the current instance of {@link MavenDependencyCollector} configured to used provided matchers.
   */
  public MavenDependencyCollector withName(List<MavenDependencyNameMatcher> matchers) {
    this.nameMatchers = matchers;
    return this;
  }

  /**
   * Define the version matcher to be used when collecting dependencies. Optional.
   * @param matcher The version matcher to be used
   * @return the current instance of {@link MavenDependencyCollector} configured to used provided matchers.
   */
  public MavenDependencyCollector withVersion(MavenDependencyVersionMatcher matcher) {
    this.versionMatcher = matcher;
    return this;
  }

  /**
   * Retrieve the dependencies matching the current {@link MavenDependencyCollector} configuration.
   * @return the list of matching dependencies
   */
  public List<Dependency> getDependencies() {
    return collectDependencies();
  }

  private List<Dependency> collectDependencies() {
    this.dependencies = allDependencies(mavenProject);
    if (nameMatchers.isEmpty()) {
      return dependencies;
    }
    return filterWithMatchers(dependencies, nameMatchers, versionMatcher);
  }

  private static List<Dependency> filterWithMatchers(List<Dependency> dependencies, List<MavenDependencyNameMatcher> nameMatchers, MavenDependencyVersionMatcher versionMatcher) {
    List<Dependency> result = new LinkedList<>();
    for (Dependency dependency : dependencies) {
      for (MavenDependencyNameMatcher namePattern : nameMatchers) {
        if (namePattern.matches(dependency) && versionMatcher.matches(dependency)) {
          result.add(dependency);
          break;
        }
      }
    }
    return result;
  }

  private static List<Dependency> allDependencies(MavenProject mavenProject) {
    List<Dependency> results = new LinkedList<>();
    results.addAll(fromDependencyManagement(mavenProject.getDependencyManagement()));
    results.addAll(mavenProject.getDependencies() != null ? mavenProject.getDependencies().getDependencies() : Collections.<Dependency>emptyList());
    results.addAll(fromBuild(mavenProject.getBuild()));
    results.addAll(fromProfiles(mavenProject.getProfiles()));
    return results;
  }

  private static List<Dependency> fromBuild(@Nullable Build build) {
    if (build != null) {
      List<Dependency> results = new LinkedList<>();
      if (build.getPluginManagement() != null && build.getPluginManagement().getPlugins() != null) {
        results.addAll(fromPlugins(build.getPluginManagement().getPlugins().getPlugins()));
      }
      if (build.getPlugins() != null) {
        results.addAll(fromPlugins(build.getPlugins().getPlugins()));
      }
      return results;
    }
    return Collections.<Dependency>emptyList();
  }

  private static List<Dependency> fromPlugins(List<Plugin> plugins) {
    for (Plugin plugin : plugins) {
      if (plugin.getDependencies() != null) {
        return plugin.getDependencies().getDependencies();
      }
    }
    return Collections.<Dependency>emptyList();
  }

  private static List<Dependency> fromProfiles(@Nullable Profiles profiles) {
    if (profiles != null) {
      List<Dependency> results = new LinkedList<>();
      for (Profile profile : profiles.getProfiles()) {
        results.addAll(fromDependencyManagement(profile.getDependencyManagement()));
        results.addAll(profile.getDependencies() != null ? profile.getDependencies().getDependencies() : Collections.<Dependency>emptyList());
        results.addAll(fromBuild(profile.getBuild()));
      }
      return results;
    }
    return Collections.<Dependency>emptyList();
  }

  private static List<Dependency> fromBuild(@Nullable BuildBase build) {
    if (build != null) {
      List<Dependency> results = new LinkedList<>();
      if (build.getPluginManagement() != null && build.getPluginManagement().getPlugins() != null) {
        results.addAll(fromPlugins(build.getPluginManagement().getPlugins().getPlugins()));
      }
      if (build.getPlugins() != null) {
        results.addAll(fromPlugins(build.getPlugins().getPlugins()));
      }
      return results;
    }
    return Collections.<Dependency>emptyList();
  }

  private static List<Dependency> fromDependencyManagement(@Nullable DependencyManagement depMgmt) {
    if (depMgmt != null) {
      Dependencies dependencies = depMgmt.getDependencies();
      if (dependencies != null) {
        return dependencies.getDependencies();
      }
    }
    return Collections.<Dependency>emptyList();
  }
}
