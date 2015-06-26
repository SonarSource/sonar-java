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

import java.io.File;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class SonarComponents implements BatchExtension {

  private final FileLinesContextFactory fileLinesContextFactory;

  private final ResourcePerspectives resourcePerspectives;

  private final JavaTestClasspath javaTestClasspath;

  private final CheckFactory checkFactory;

  private final JavaClasspath javaClasspath;

  private final Project project;

  private final List<Checks<JavaCheck>> checks;

  private Checks<JavaCheck> testChecks;

  public SonarComponents(final FileLinesContextFactory fileLinesContextFactory, final ResourcePerspectives resourcePerspectives, final Project project,
    final JavaClasspath javaClasspath,
    final JavaTestClasspath javaTestClasspath, final CheckFactory checkFactory) {
    this(fileLinesContextFactory, resourcePerspectives, project, javaClasspath, javaTestClasspath, checkFactory, null);
  }

  public SonarComponents(final FileLinesContextFactory fileLinesContextFactory, final ResourcePerspectives resourcePerspectives, final Project project,
    final JavaClasspath javaClasspath,
    final JavaTestClasspath javaTestClasspath, final CheckFactory checkFactory, @Nullable final CheckRegistrar[] checkRegistrars) {
    this.fileLinesContextFactory = fileLinesContextFactory;
    this.resourcePerspectives = resourcePerspectives;
    this.project = project;
    this.javaClasspath = javaClasspath;
    this.javaTestClasspath = javaTestClasspath;
    this.checkFactory = checkFactory;
    this.checks = Lists.newArrayList();

    if (checkRegistrars != null) {
      final CheckRegistrar.RegistrarContext registrarContext = new CheckRegistrar.RegistrarContext();
      for (final CheckRegistrar checkClassesRegister : checkRegistrars) {
        checkClassesRegister.register(registrarContext);
        switch (checkClassesRegister.type()) {
          case SOURCE_CHECKS:
            registerCheckClasses(registrarContext.repositoryKey(), Lists.newArrayList(registrarContext.checkClasses()));
            break;
          case TEST_CHECKS:
            registerTestCheckClasses(registrarContext.repositoryKey(), Lists.newArrayList(registrarContext.checkClasses()));
            break;

        }
      }
    }
  }

  public Resource resourceFromIOFile(final File file) {
    return org.sonar.api.resources.File.fromIOFile(file, this.project);
  }

  public FileLinesContext fileLinesContextFor(final File file) {
    return this.fileLinesContextFactory.createFor(resourceFromIOFile(file));
  }

  public Symbolizable symbolizableFor(final File file) {
    return this.resourcePerspectives.as(Symbolizable.class, resourceFromIOFile(file));
  }

  public Highlightable highlightableFor(final File file) {
    return this.resourcePerspectives.as(Highlightable.class, resourceFromIOFile(file));
  }

  public List<File> getJavaClasspath() {
    if (this.javaClasspath == null) {
      return Lists.newArrayList();
    }
    return this.javaClasspath.getElements();
  }

  public List<File> getJavaTestClasspath() {
    return this.javaTestClasspath.getElements();
  }

  public ResourcePerspectives getResourcePerspectives() {
    return this.resourcePerspectives;
  }

  public void registerCheckClasses(final String repositoryKey, final Collection<Class<? extends JavaCheck>> checkClasses) {
    this.checks.add(this.checkFactory.<JavaCheck>create(repositoryKey)
      .addAnnotatedChecks(checkClasses));
  }

  public CodeVisitor[] checkClasses() {
    final List<CodeVisitor> visitors = Lists.newArrayList();
    for (final Checks<JavaCheck> check : this.checks) {
      visitors.addAll(check.all());
    }
    return visitors.toArray(new CodeVisitor[visitors.size()]);
  }

  public Iterable<Checks<JavaCheck>> checks() {
    return Iterables.concat(this.checks, Lists.newArrayList(this.testChecks));
  }

  public void registerTestCheckClasses(final String repositoryKey, final List<Class<? extends JavaCheck>> javaTestChecks) {
    this.testChecks = this.checkFactory.<JavaCheck>create(repositoryKey)
      .addAnnotatedChecks(javaTestChecks);
  }

  public Collection<JavaCheck> testCheckClasses() {
    if (this.testChecks == null) {
      return Lists.newArrayList();
    }
    return this.testChecks.all();
  }

}
