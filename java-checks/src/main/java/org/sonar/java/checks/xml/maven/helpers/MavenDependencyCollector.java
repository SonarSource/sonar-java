/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.java.checks.xml.maven.helpers;

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

public class MavenDependencyCollector {

  private final List<Dependency> dependencies;

  public MavenDependencyCollector(MavenProject mavenProject) {
    dependencies = collectAllDependencies(mavenProject);
  }

  public List<Dependency> allDependencies() {
    return dependencies;
  }

  private static List<Dependency> collectAllDependencies(MavenProject mavenProject) {
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
