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
package org.sonar.java;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.sonar.sslr.api.RecognitionException;
import org.sonar.api.SonarProduct;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.symbol.NewSymbolTable;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.Version;
import org.sonar.plugins.java.api.CheckRegistrar;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.squidbridge.api.CodeVisitor;
import org.sonarsource.api.sonarlint.SonarLintSide;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@BatchSide
@SonarLintSide
public class SonarComponents {

  private static final Version SQ_6_0 = Version.create(6, 0);
  private static final Version SQ_6_2 = Version.create(6, 2);
  private final FileLinesContextFactory fileLinesContextFactory;
  private final JavaTestClasspath javaTestClasspath;
  private final CheckFactory checkFactory;
  private final FileSystem fs;
  private final JavaClasspath javaClasspath;
  private final List<Checks<JavaCheck>> checks;
  private final List<Checks<JavaCheck>> testChecks;
  private final List<Checks<JavaCheck>> allChecks;
  private SensorContext context;

  public SonarComponents(FileLinesContextFactory fileLinesContextFactory, FileSystem fs,
    JavaClasspath javaClasspath, JavaTestClasspath javaTestClasspath,
    CheckFactory checkFactory) {
    this(fileLinesContextFactory, fs, javaClasspath, javaTestClasspath, checkFactory, null);
  }

  public SonarComponents(FileLinesContextFactory fileLinesContextFactory, FileSystem fs,
    JavaClasspath javaClasspath, JavaTestClasspath javaTestClasspath, CheckFactory checkFactory,
    @Nullable CheckRegistrar[] checkRegistrars) {
    this.fileLinesContextFactory = fileLinesContextFactory;
    this.fs = fs;
    this.javaClasspath = javaClasspath;
    this.javaTestClasspath = javaTestClasspath;
    this.checkFactory = checkFactory;
    this.checks = new ArrayList<>();
    this.testChecks = new ArrayList<>();
    this.allChecks = new ArrayList<>();
    if (checkRegistrars != null) {
      CheckRegistrar.RegistrarContext registrarContext = new CheckRegistrar.RegistrarContext();
      for (CheckRegistrar checkClassesRegister : checkRegistrars) {
        checkClassesRegister.register(registrarContext);
        Iterable<Class<? extends JavaCheck>> checkClasses = registrarContext.checkClasses();
        Iterable<Class<? extends JavaCheck>> testCheckClasses = registrarContext.testCheckClasses();
        registerCheckClasses(registrarContext.repositoryKey(), Lists.newArrayList(checkClasses != null ? checkClasses : new ArrayList<>()));
        registerTestCheckClasses(registrarContext.repositoryKey(), Lists.newArrayList(testCheckClasses != null ? testCheckClasses : new ArrayList<>()));
      }
    }
  }

  public void setSensorContext(SensorContext context) {
    this.context = context;
  }

  public InputFile inputFromIOFile(File file) {
    return fs.inputFile(fs.predicates().is(file));
  }

  public int fileLength(File file) {
    return inputFromIOFile(file).lines();
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

  public NewSymbolTable symbolizableFor(File file) {
    return context.newSymbolTable().onFile(inputFromIOFile(file));
  }

  public NewHighlighting highlightableFor(File file) {
    Preconditions.checkNotNull(context);
    return context.newHighlighting().onFile(inputFromIOFile(file));
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

  public void registerCheckClasses(String repositoryKey, Iterable<Class<? extends JavaCheck>> checkClasses) {
    Checks<JavaCheck> createdChecks = checkFactory.<JavaCheck>create(repositoryKey).addAnnotatedChecks(checkClasses);
    checks.add(createdChecks);
    allChecks.add(createdChecks);
  }

  public CodeVisitor[] checkClasses() {
    return checks.stream().flatMap(ce -> ce.all().stream()).toArray(CodeVisitor[]::new);
  }

  public Iterable<Checks<JavaCheck>> checks() {
    return allChecks;
  }

  public void registerTestCheckClasses(String repositoryKey, Iterable<Class<? extends JavaCheck>> checkClasses) {
    Checks<JavaCheck> createdChecks = checkFactory.<JavaCheck>create(repositoryKey).addAnnotatedChecks(checkClasses);
    testChecks.add(createdChecks);
    allChecks.add(createdChecks);
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

  public void addIssue(File file, JavaCheck check, int line, String message, @Nullable Integer cost) {
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
    reportIssue(analyzerMessage, key, inputPath, cost);
  }

  @VisibleForTesting
  void reportIssue(AnalyzerMessage analyzerMessage, RuleKey key, InputPath inputPath, Double cost) {
    Preconditions.checkNotNull(context);
    JavaIssue issue = JavaIssue.create(context, key, cost);
    AnalyzerMessage.TextSpan textSpan = analyzerMessage.primaryLocation();
    if (textSpan == null) {
      // either an issue at file or folder level
      issue.setPrimaryLocationOnFile(inputPath, analyzerMessage.getMessage());
    } else {
      if (!textSpan.onLine()) {
        Preconditions.checkState(!textSpan.isEmpty(), "Issue location should not be empty");
      }
      issue.setPrimaryLocation((InputFile) inputPath, analyzerMessage.getMessage(), textSpan.startLine, textSpan.startCharacter, textSpan.endLine, textSpan.endCharacter);
    }
    issue.addFlow(inputFromIOFile(analyzerMessage.getFile()), analyzerMessage.flows).save();
  }

  public boolean reportAnalysisError(RecognitionException re, File file) {
    if (context.getSonarQubeVersion().isGreaterThanOrEqual(SQ_6_0)) {
      context.newAnalysisError()
        .onFile(inputFromIOFile(file))
        .message(re.getMessage())
        .save();
      return context.runtime().getProduct() == SonarProduct.SONARLINT;
    }
    return false;
  }

  public boolean isSQGreaterThan62() {
    return context.getSonarQubeVersion().isGreaterThanOrEqual(SQ_6_2);
  }
}
