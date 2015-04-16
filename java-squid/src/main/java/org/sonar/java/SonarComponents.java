/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.sonar.api.BatchExtension;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.source.Highlightable;
import org.sonar.api.source.Symbolizable;
import org.sonar.plugins.java.api.CheckRegistrar;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.squidbridge.api.CodeVisitor;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Collection;
import java.util.List;

public class SonarComponents implements BatchExtension {

  private final FileLinesContextFactory fileLinesContextFactory;
  private final ResourcePerspectives resourcePerspectives;
  private final JavaTestClasspath javaTestClasspath;
  private final CheckFactory checkFactory;
  private final JavaClasspath javaClasspath;
  private final Project project;
  private final List<Checks<JavaCheck>> checks;
  private Checks<JavaCheck> testChecks;

  public SonarComponents(FileLinesContextFactory fileLinesContextFactory, ResourcePerspectives resourcePerspectives, Project project,
                         JavaClasspath javaClasspath, JavaTestClasspath javaTestClasspath,
                         CheckFactory checkFactory) {
    this(fileLinesContextFactory, resourcePerspectives, project, javaClasspath, javaTestClasspath, checkFactory, null);
  }

  public SonarComponents(FileLinesContextFactory fileLinesContextFactory, ResourcePerspectives resourcePerspectives, Project project,
                         JavaClasspath javaClasspath, JavaTestClasspath javaTestClasspath, CheckFactory checkFactory,
                         @Nullable CheckRegistrar[] checkRegistrars) {
    this.fileLinesContextFactory = fileLinesContextFactory;
    this.resourcePerspectives = resourcePerspectives;
    this.project = project;
    this.javaClasspath = javaClasspath;
    this.javaTestClasspath = javaTestClasspath;
    this.checkFactory = checkFactory;
    checks = Lists.newArrayList();

    if(checkRegistrars != null) {
      CheckRegistrar.RegistrarContext registrarContext = new CheckRegistrar.RegistrarContext();
      for (CheckRegistrar checkClassesRegister : checkRegistrars) {
        checkClassesRegister.register(registrarContext);
        registerCheckClasses(registrarContext.repositoryKey(), Lists.newArrayList(registrarContext.checkClasses()));
      }
    }
  }

  public Resource resourceFromIOFile(File file) {
    return org.sonar.api.resources.File.fromIOFile(file, project);
  }

  public FileLinesContext fileLinesContextFor(File file) {
    return fileLinesContextFactory.createFor(resourceFromIOFile(file));
  }

  public Symbolizable symbolizableFor(File file) {
    return resourcePerspectives.as(Symbolizable.class, resourceFromIOFile(file));
  }

  public Highlightable highlightableFor(File file) {
    return resourcePerspectives.as(Highlightable.class, resourceFromIOFile(file));
  }

  public List<File> getJavaClasspath() {
    if (javaClasspath == null) {
      return Lists.newArrayList();
    }
    return javaClasspath.getElements();
  }

  public List<File> getJavaTestClasspath() {
    return javaTestClasspath.getElements();
  }

  public ResourcePerspectives getResourcePerspectives() {
    return resourcePerspectives;
  }

  public void registerCheckClasses(String repositoryKey, Collection<Class<? extends JavaCheck>> checkClasses) {
    checks.add(checkFactory.<JavaCheck>create(repositoryKey).addAnnotatedChecks(checkClasses));
  }

  public CodeVisitor[] checkClasses() {
    List<CodeVisitor> visitors = Lists.newArrayList();
    for (Checks<JavaCheck> check : checks) {
      visitors.addAll(check.all());
    }
    return visitors.toArray(new CodeVisitor[visitors.size()]);
  }

  public Iterable<Checks<JavaCheck>> checks() {
    return Iterables.concat(checks, Lists.newArrayList(testChecks));
  }

  public void registerTestCheckClasses(String repositoryKey, List<Class<? extends JavaCheck>> javaTestChecks) {
    testChecks = checkFactory.<JavaCheck>create(repositoryKey).addAnnotatedChecks(javaTestChecks);
  }

  public Collection<JavaCheck> testCheckClasses() {
    if(testChecks == null) {
      return Lists.newArrayList();
    }
    return testChecks.all();
  }



}
