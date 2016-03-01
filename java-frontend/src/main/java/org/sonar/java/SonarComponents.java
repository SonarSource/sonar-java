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
package org.sonar.java;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.sonar.api.BatchExtension;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.source.Highlightable;
import org.sonar.api.source.Symbolizable;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.plugins.java.api.CheckRegistrar;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.squidbridge.annotations.SqaleLinearRemediation;
import org.sonar.squidbridge.annotations.SqaleLinearWithOffsetRemediation;
import org.sonar.squidbridge.api.CodeVisitor;

import javax.annotation.Nullable;
import java.io.File;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SonarComponents implements BatchExtension {

  private static final boolean IS_SONARQUBE_52 = isSonarQube52();

  private final FileLinesContextFactory fileLinesContextFactory;
  private final ResourcePerspectives resourcePerspectives;
  private final JavaTestClasspath javaTestClasspath;
  private final CheckFactory checkFactory;
  private final SensorContext context;
  private final FileSystem fs;
  private final JavaClasspath javaClasspath;
  private final List<Checks<JavaCheck>> checks;
  private final List<Checks<JavaCheck>> testChecks;

  public SonarComponents(FileLinesContextFactory fileLinesContextFactory, ResourcePerspectives resourcePerspectives, FileSystem fs,
    JavaClasspath javaClasspath, JavaTestClasspath javaTestClasspath, SensorContext context,
    CheckFactory checkFactory) {
    this(fileLinesContextFactory, resourcePerspectives, fs, javaClasspath, javaTestClasspath, checkFactory, context, null);
  }

  public SonarComponents(FileLinesContextFactory fileLinesContextFactory, ResourcePerspectives resourcePerspectives, FileSystem fs,
    JavaClasspath javaClasspath, JavaTestClasspath javaTestClasspath, CheckFactory checkFactory, SensorContext context,
    @Nullable CheckRegistrar[] checkRegistrars) {
    this.fileLinesContextFactory = fileLinesContextFactory;
    this.resourcePerspectives = resourcePerspectives;
    this.fs = fs;
    this.javaClasspath = javaClasspath;
    this.javaTestClasspath = javaTestClasspath;
    this.checkFactory = checkFactory;
    this.context = context;
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

  public InputFile inputFromIOFile(File file) {
    return fs.inputFile(fs.predicates().is(file));
  }

  private InputPath inputPathFromIOFile(File file) {
    if (file.isDirectory()) {
      return fs.inputDir(file);
    } else {
      return inputFromIOFile(file);
    }
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

  public Issuable issuableFor(InputPath inputPath) {
    return resourcePerspectives.as(Issuable.class, inputPath);
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

  public FileSystem getFileSystem() {
    return fs;
  }

  public RuleKey getRuleKey(JavaCheck check) {
    for (Checks<JavaCheck> sonarChecks : checks()) {
      RuleKey ruleKey = sonarChecks.ruleKey(check);
      if (ruleKey != null) {
        return ruleKey;
      }
    }
    return null;
  }

  public void addIssue(File file, JavaCheck check, int line, String message, @Nullable Double cost) {
    reportIssue(new AnalyzerMessage(check, file, line, message, cost != null ? cost.intValue() : 0));
  }

  public void reportIssue(AnalyzerMessage analyzerMessage) {
    JavaCheck check = analyzerMessage.getCheck();
    Preconditions.checkNotNull(check);
    Preconditions.checkNotNull(analyzerMessage.getMessage());
    RuleKey key = getRuleKey(check);
    if (key == null) {
      return;
    }
    File file = analyzerMessage.getFile();
    InputPath inputPath = inputPathFromIOFile(file);
    if (inputPath == null) {
      return;
    }
    Double cost = analyzerMessage.getCost();
    assertEffortToFixIsNotRequired(check, cost);
    if (IS_SONARQUBE_52) {
      reportIssueAfterSQ52(analyzerMessage, key, inputPath, cost);
    } else {
      reportIssueBeforeSQ52(inputPath, key, cost, analyzerMessage.getMessage(), analyzerMessage.getLine());
    }
  }

  @VisibleForTesting
  void reportIssueAfterSQ52(AnalyzerMessage analyzerMessage, RuleKey key, InputPath inputPath, Double cost) {
    JavaIssue issue = JavaIssue.create(context, key, cost);
    AnalyzerMessage.TextSpan textSpan = analyzerMessage.primaryLocation();
    if (textSpan == null) {
      // either an issue at file or folder level
      issue.setPrimaryLocationOnFile(inputPath, analyzerMessage.getMessage());
    } else {
      issue.setPrimaryLocation((InputFile) inputPath, analyzerMessage.getMessage(), textSpan.startLine, textSpan.startCharacter, textSpan.endLine, textSpan.endCharacter);
    }
    for (AnalyzerMessage location : analyzerMessage.secondaryLocations) {
      AnalyzerMessage.TextSpan secondarySpan = location.primaryLocation();
      issue.addSecondaryLocation(
        inputFromIOFile(location.getFile()), secondarySpan.startLine, secondarySpan.startCharacter, secondarySpan.endLine, secondarySpan.endCharacter, location.getMessage());
    }
    issue.save();
  }

  private void reportIssueBeforeSQ52(InputPath inputFile, RuleKey key, @Nullable Double cost, String message, @Nullable Integer line) {
    Issuable issuable = issuableFor(inputFile);
    if (issuable != null) {
      Issuable.IssueBuilder newIssueBuilder = issuable.newIssueBuilder()
        .ruleKey(key)
        .message(message)
        .line(line)
        .effortToFix(cost);
      issuable.addIssue(newIssueBuilder.build());
    }
  }

  private static void assertEffortToFixIsNotRequired(JavaCheck check, @Nullable Double cost) {
    if (cost == null) {
      Annotation linear = AnnotationUtils.getAnnotation(check, SqaleLinearRemediation.class);
      Annotation linearWithOffset = AnnotationUtils.getAnnotation(check, SqaleLinearWithOffsetRemediation.class);
      if (linear != null || linearWithOffset != null) {
        throw new IllegalStateException("A check annotated with a linear sqale function should provide an effort to fix");
      }
    }
  }

  private static boolean isSonarQube52() {
    try {
      Issuable.IssueBuilder.class.getMethod("newLocation");
      return true;
    } catch (NoSuchMethodException e) {
      return false;
    }
  }
}
