/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
import com.google.gson.Gson;
import com.sonar.sslr.api.RecognitionException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import javax.annotation.Nullable;
import org.sonar.api.SonarProduct;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.symbol.NewSymbolTable;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.measures.Metric;
import org.sonar.api.rule.RuleKey;
import org.sonar.plugins.java.api.CheckRegistrar;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonarsource.api.sonarlint.SonarLintSide;

@ScannerSide
@SonarLintSide
public class SonarComponents {

  /**
   * Metric to collect
   */
  public static final Metric<String> FEEDBACK_METRIC = new Metric.Builder("sonarjava_feedback", "SonarJava feedback", Metric.ValueType.DATA).setHidden(true).create();
  public static final String COLLECT_ANALYSIS_ERRORS_KEY = "sonar.java.collectAnalysisErrors";
  public static final String FAIL_ON_EXCEPTION_KEY = "sonar.java.failOnException";
  /**
   * Approximate limit of feedback of 200ko to roughly 100_000 characters of useful feedback.
   * This does not take into account eventual overhead of serialization.
   */
  private static final int ERROR_SERIALIZATION_LIMIT = 100_000;

  private final FileLinesContextFactory fileLinesContextFactory;
  private final JavaTestClasspath javaTestClasspath;
  private final CheckFactory checkFactory;
  @Nullable
  private final ProjectDefinition projectDefinition;
  private final FileSystem fs;
  private final JavaClasspath javaClasspath;
  private final List<Checks<JavaCheck>> checks;
  private final List<Checks<JavaCheck>> testChecks;
  private final List<Checks<JavaCheck>> allChecks;
  private SensorContext context;
  @VisibleForTesting
  public List<AnalysisError> analysisErrors;
  private int errorsSize = 0;

  public SonarComponents(FileLinesContextFactory fileLinesContextFactory, FileSystem fs,
                         JavaClasspath javaClasspath, JavaTestClasspath javaTestClasspath,
                         CheckFactory checkFactory) {
    this(fileLinesContextFactory, fs, javaClasspath, javaTestClasspath, checkFactory, null, null);
  }

  /**
   * Will be called in SonarLint context when custom rules are present
   */
  public SonarComponents(FileLinesContextFactory fileLinesContextFactory, FileSystem fs,
                         JavaClasspath javaClasspath, JavaTestClasspath javaTestClasspath, CheckFactory checkFactory,
                         @Nullable CheckRegistrar[] checkRegistrars) {
    this(fileLinesContextFactory, fs, javaClasspath, javaTestClasspath, checkFactory, checkRegistrars, null);
  }

  /**
   * Will be called in SonarScanner context when no custom rules is present
   */
  public SonarComponents(FileLinesContextFactory fileLinesContextFactory, FileSystem fs,
                         JavaClasspath javaClasspath, JavaTestClasspath javaTestClasspath, CheckFactory checkFactory,
                         @Nullable ProjectDefinition projectDefinition) {
    this(fileLinesContextFactory, fs, javaClasspath, javaTestClasspath, checkFactory, null, projectDefinition);
  }

  /**
   * ProjectDefinition class is not available in SonarLint context, so this constructor will never be called when using SonarLint
   */
  public SonarComponents(FileLinesContextFactory fileLinesContextFactory, FileSystem fs,
                         JavaClasspath javaClasspath, JavaTestClasspath javaTestClasspath, CheckFactory checkFactory,
                         @Nullable CheckRegistrar[] checkRegistrars, @Nullable ProjectDefinition projectDefinition) {
    this.fileLinesContextFactory = fileLinesContextFactory;
    this.fs = fs;
    this.javaClasspath = javaClasspath;
    this.javaTestClasspath = javaTestClasspath;
    this.checkFactory = checkFactory;
    this.projectDefinition = projectDefinition;
    this.checks = new ArrayList<>();
    this.testChecks = new ArrayList<>();
    this.allChecks = new ArrayList<>();
    this.analysisErrors = new ArrayList<>();
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

  /**
   * DEPRECATED: Use {@link #addIssue(InputComponent, JavaCheck, int, String, Integer)} instead.
   * As File-based API should not be used anymore, this is deprecated and will be dropped.
   *
   * @deprecated since SonarJava 5.12 - dropping usage of file to rely on InputComponent/InputFile
   */
  @Deprecated
  public void addIssue(File file, JavaCheck check, int line, String message, @Nullable Integer cost) {
    reportIssue(new AnalyzerMessage(check, inputFromIOFileOrDirectory(file), line, message, cost != null ? cost.intValue() : 0));
  }

  /**
   * DEPRECATED: Method to retrieve the corresponding IssueComponent from a file.
   * As File-based API should not be used anymore, this is deprecated and will be dropped.
   *
   * @deprecated since SonarJava 5.12 - dropping usage of file to rely on InputComponent/InputFile
   */
  @Deprecated
  @Nullable
  public InputComponent inputFromIOFileOrDirectory(File file) {
    if (file.isDirectory()) {
      // TODO context.module() is deprecated since SQ 7.6, but it can not yet be replaced by
      // context.project() until the oldest "SQ supported version" is >= 7.6
      return context != null && isInSubDirectory(fs.baseDir().getAbsoluteFile(), file.getAbsoluteFile()) ? project() : null;
    }
    return inputFromIOFile(file);
  }

  /**
   * DEPRECATED: As file should not be used anymore, this is deprecated and will be dropped.
   *
   * @deprecated since SonarJava 5.12 - dropping usage of file to rely on InputComponent/InputFile
   */
  @Deprecated
  private static boolean isInSubDirectory(File dir, @Nullable File file) {
    return file != null && (file.equals(dir) || isInSubDirectory(dir, file.getParentFile()));
  }

  /**
   * DEPRECATED: As file should not be used anymore, this is deprecated and will be dropped.
   *
   * InputFile/InputComponent should always be available in the context of a scan, and so no reason to still rely on File.
   *
   * @deprecated since SonarJava 5.12 - dropping usage of file to rely on InputComponent/InputFile
   */
  @Deprecated
  @Nullable
  public InputFile inputFromIOFile(File file) {
    return fs.inputFile(fs.predicates().is(file));
  }

  public FileLinesContext fileLinesContextFor(InputFile inputFile) {
    return fileLinesContextFactory.createFor(inputFile);
  }

  public NewSymbolTable symbolizableFor(InputFile inputFile) {
    return context.newSymbolTable().onFile(inputFile);
  }

  public NewHighlighting highlightableFor(InputFile inputFile) {
    Objects.requireNonNull(context);
    return context.newHighlighting().onFile(inputFile);
  }

  public List<File> getJavaClasspath() {
    if (javaClasspath == null) {
      return new ArrayList<>();
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

  public JavaCheck[] checkClasses() {
    return checks.stream().flatMap(ce -> ce.all().stream()).toArray(JavaCheck[]::new);
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
    List<JavaCheck> visitors = new ArrayList<>();
    for (Checks<JavaCheck> checksElement : testChecks) {
      Collection<JavaCheck> checksCollection = checksElement.all();
      if (!checksCollection.isEmpty()) {
        visitors.addAll(checksCollection);
      }
    }
    return visitors;
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

  public void addIssue(InputComponent inputComponent, JavaCheck check, int line, String message, @Nullable Integer cost) {
    reportIssue(new AnalyzerMessage(check, inputComponent, line, message, cost != null ? cost.intValue() : 0));
  }

  public void reportIssue(AnalyzerMessage analyzerMessage) {
    JavaCheck check = analyzerMessage.getCheck();
    Objects.requireNonNull(check);
    Objects.requireNonNull(analyzerMessage.getMessage());
    RuleKey key = getRuleKey(check);
    if (key == null) {
      return;
    }
    InputComponent inputComponent = analyzerMessage.getInputComponent();
    if (inputComponent == null) {
      return;
    }
    Double cost = analyzerMessage.getCost();
    reportIssue(analyzerMessage, key, inputComponent, cost);
  }

  @VisibleForTesting
  void reportIssue(AnalyzerMessage analyzerMessage, RuleKey key, InputComponent fileOrProject, @Nullable Double cost) {
    Objects.requireNonNull(context);
    JavaIssue issue = JavaIssue.create(context, key, cost);
    AnalyzerMessage.TextSpan textSpan = analyzerMessage.primaryLocation();
    if (textSpan == null) {
      // either an issue at file or project level
      issue.setPrimaryLocationOnComponent(fileOrProject, analyzerMessage.getMessage());
    } else {
      if (!textSpan.onLine()) {
        Preconditions.checkState(!textSpan.isEmpty(), "Issue location should not be empty");
      }
      issue.setPrimaryLocation((InputFile) fileOrProject, analyzerMessage.getMessage(), textSpan.startLine, textSpan.startCharacter, textSpan.endLine, textSpan.endCharacter);
    }
    if (!analyzerMessage.flows.isEmpty()) {
      issue.addFlow((InputFile) analyzerMessage.getInputComponent(), analyzerMessage.flows);
    }
    issue.save();
  }

  public boolean reportAnalysisError(RecognitionException re, InputFile inputFile) {
    reportAnalysisError(inputFile, re.getMessage());
    return isSonarLintContext();
  }

  public void reportAnalysisError(InputFile inputFile, String message) {
    context.newAnalysisError()
      .onFile(inputFile)
      .message(message)
      .save();
  }

  public boolean isSonarLintContext() {
    return context.runtime().getProduct() == SonarProduct.SONARLINT;
  }

  public List<String> fileLines(InputFile inputFile) {
    List<String> lines = new ArrayList<>();
    try (Scanner scanner = new Scanner(inputFile.inputStream(), inputFile.charset().name())) {
      while (scanner.hasNextLine()) {
        lines.add(scanner.nextLine());
      }
    } catch (IOException e) {
      throw new AnalysisException(String.format("Unable to read file '%s'", inputFile), e);
    }
    return lines;
  }

  public String inputFileContents(InputFile inputFile) {
    try {
      return inputFile.contents();
    } catch (IOException e) {
      throw new AnalysisException(String.format("Unable to read file '%s'", inputFile), e);
    }
  }

  public boolean analysisCancelled() {
    return context.isCancelled();
  }

  public void addAnalysisError(AnalysisError analysisError) {
    if (errorsSize < ERROR_SERIALIZATION_LIMIT) {
      errorsSize += analysisError.serializedSize();
      analysisErrors.add(analysisError);
    }
  }

  public void saveAnalysisErrors() {
    if (!isSonarLintContext() && !analysisErrors.isEmpty() && shouldCollectAnalysisErrors()) {
      Gson gson = new Gson();
      String metricValue = gson.toJson(analysisErrors);
      context.<String>newMeasure().forMetric(FEEDBACK_METRIC).on(context.module()).withValue(metricValue).save();
    }
  }

  public boolean shouldFailAnalysisOnException() {
    return context.config().getBoolean(FAIL_ON_EXCEPTION_KEY).orElse(false);
  }

  private boolean shouldCollectAnalysisErrors() {
    return context.config().getBoolean(COLLECT_ANALYSIS_ERRORS_KEY).orElse(false);
  }

  public File workDir() {
    ProjectDefinition current = projectDefinition;
    if(current == null) {
      return fs.workDir();
    }
    while (current.getParent() != null) {
      current = current.getParent();
    }
    return current.getWorkDir();
  }

  public File baseDir() {
    return fs.baseDir();
  }

  public InputComponent project() {
    // TODO to be changed to context.project() once LTS 7.x has been released
    return context.module();
  }
}
