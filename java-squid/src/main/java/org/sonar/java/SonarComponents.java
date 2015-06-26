/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.source.Highlightable;
import org.sonar.api.source.Symbolizable;
import org.sonar.plugins.java.api.CheckRegistrar;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.squidbridge.api.CodeVisitor;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SonarComponents implements BatchExtension {

  private final FileLinesContextFactory fileLinesContextFactory;
  private final ResourcePerspectives resourcePerspectives;
  private final JavaTestClasspath javaTestClasspath;
  private final CheckFactory checkFactory;
  private final FileSystem fs;
  private final JavaClasspath javaClasspath;
  private final List<Checks<JavaCheck>> checks;
  private final List<Checks<JavaCheck>> testChecks;

  public SonarComponents(FileLinesContextFactory fileLinesContextFactory, ResourcePerspectives resourcePerspectives, FileSystem fs,
    JavaClasspath javaClasspath, JavaTestClasspath javaTestClasspath,
    CheckFactory checkFactory) {
    this(fileLinesContextFactory, resourcePerspectives, fs, javaClasspath, javaTestClasspath, checkFactory, null);
  }

  public SonarComponents(FileLinesContextFactory fileLinesContextFactory, ResourcePerspectives resourcePerspectives, FileSystem fs,
    JavaClasspath javaClasspath, JavaTestClasspath javaTestClasspath, CheckFactory checkFactory,
    @Nullable CheckRegistrar[] checkRegistrars) {
    this.fileLinesContextFactory = fileLinesContextFactory;
    this.resourcePerspectives = resourcePerspectives;
    this.fs = fs;
    this.javaClasspath = javaClasspath;
    this.javaTestClasspath = javaTestClasspath;
    this.checkFactory = checkFactory;
    this.checks = Lists.newArrayList();
    this.testChecks = Lists.newArrayList();

    if (checkRegistrars != null) {
      CheckRegistrar.RegistrarContext registrarContext = new CheckRegistrar.RegistrarContext();
      for (CheckRegistrar checkClassesRegister : checkRegistrars) {
        checkClassesRegister.register(registrarContext);
        Iterable<Class<? extends JavaCheck>> checkClasses = registrarContext.checkClasses();
        Iterable<Class<? extends JavaCheck>> testCheckClasses = registrarContext.testCheckClasses();
        registerCheckClasses(registrarContext.repositoryKey(), Lists.newArrayList(checkClasses != null ? checkClasses : new ArrayList<Class<? extends JavaCheck>>()));
        registerTestCheckClasses(registrarContext.repositoryKey(), Lists.newArrayList(testCheckClasses != null ? testCheckClasses : new ArrayList<Class<? extends JavaCheck>>()));
      }
    }
  }

  private InputFile inputFromIOFile(File file) {
    return fs.inputFile(fs.predicates().is(file));
  }

  public FileLinesContext fileLinesContextFor(File file) {
    return fileLinesContextFactory.createFor(inputFromIOFile(file));
  }

  public Symbolizable symbolizableFor(File file) {
    return resourcePerspectives.as(Symbolizable.class, inputFromIOFile(file));
  }

  public Highlightable highlightableFor(File file) {
    return resourcePerspectives.as(Highlightable.class, inputFromIOFile(file));
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

  public void registerCheckClasses(String repositoryKey, List<Class<? extends JavaCheck>> checkClasses) {
    checks.add(checkFactory.<JavaCheck>create(repositoryKey).addAnnotatedChecks(checkClasses));
  }

  public CodeVisitor[] checkClasses() {
    List<CodeVisitor> visitors = Lists.newArrayList();
    for (Checks<JavaCheck> checksElement : checks) {
      Collection<JavaCheck> checksCollection = checksElement.all();
      if (!checksCollection.isEmpty()) {
        visitors.addAll(checksCollection);
      }
    }
    return visitors.toArray(new CodeVisitor[visitors.size()]);
  }

  public Iterable<Checks<JavaCheck>> checks() {
    return Iterables.concat(checks, Lists.newArrayList(testChecks));
  }

  public void registerTestCheckClasses(String repositoryKey, List<Class<? extends JavaCheck>> checkClasses) {
    testChecks.add(checkFactory.<JavaCheck>create(repositoryKey).addAnnotatedChecks(checkClasses));
  }

  public Collection<JavaCheck> testCheckClasses() {
    List<JavaCheck> visitors = Lists.newArrayList();
    for (Checks<JavaCheck> checksElement : testChecks) {
      Collection<JavaCheck> checksCollection = checksElement.all();
      if (!checksCollection.isEmpty()) {
        visitors.addAll(checksCollection);
      }
    }
    return visitors;
  }

}
