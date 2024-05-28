/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import com.sonar.sslr.api.RecognitionException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.LongSupplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.SonarProduct;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.symbol.NewSymbolTable;
import org.sonar.api.config.Configuration;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.Version;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.caching.ContentHashCache;
import org.sonar.java.classpath.ClasspathForMain;
import org.sonar.java.classpath.ClasspathForTest;
import org.sonar.java.exceptions.ApiMismatchException;
import org.sonar.java.model.GeneratedFile;
import org.sonar.java.model.JProblem;
import org.sonar.java.model.LineUtils;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.JavaIssue;
import org.sonar.plugins.java.api.CheckRegistrar;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JspCodeVisitor;
import org.sonarsource.api.sonarlint.SonarLintSide;
import org.sonarsource.sonarlint.plugin.api.SonarLintRuntime;

@ScannerSide
@SonarLintSide
public class SonarComponents extends CheckRegistrar.RegistrarContext {

  private static final Logger LOG = LoggerFactory.getLogger(SonarComponents.class);
  private static final int LOGGED_MAX_NUMBER_UNDEFINED_TYPES = 50;

  public static final String FAIL_ON_EXCEPTION_KEY = "sonar.internal.analysis.failFast";
  public static final String SONAR_BATCH_MODE_KEY = "sonar.java.internal.batchMode";
  public static final String SONAR_AUTOSCAN = "sonar.internal.analysis.autoscan";
  public static final String SONAR_AUTOSCAN_CHECK_FILTERING = "sonar.internal.analysis.autoscan.filtering";
  public static final String SONAR_BATCH_SIZE_KEY = "sonar.java.experimental.batchModeSizeInKB";
  public static final String SONAR_FILE_BY_FILE = "sonar.java.fileByFile";
  /**
   * Describes if an optimized analysis of unchanged by skipping some rules is enabled.
   * By default, the property is not set (null), leaving SQ/SC to decide whether to enable this behavior.
   * Setting it to true or false, forces the behavior from the analyzer independently of the server.
   */
  public static final String SONAR_CAN_SKIP_UNCHANGED_FILES_KEY = "sonar.java.skipUnchanged";

  /**
   * Describes whether input files should be parsed while ignoring unnamed split modules.
   * In practice, enabling this parameter should help developers in the Android ecosystem and those
   * relying on (transitive) dependencies that do not respect modularization as defined by the JLS.
   */
  public static final String SONAR_IGNORE_UNNAMED_MODULE_FOR_SPLIT_PACKAGE = "sonar.java.ignoreUnnamedModuleForSplitPackage";
  private static final Version SONARLINT_6_3 = Version.parse("6.3");
  private static final Version SONARQUBE_9_2 = Version.parse("9.2");
  @VisibleForTesting
  static LongSupplier maxMemoryInBytesProvider = () -> Runtime.getRuntime().maxMemory();

  private final FileLinesContextFactory fileLinesContextFactory;

  private final ClasspathForMain javaClasspath;
  private final ClasspathForTest javaTestClasspath;
  private final Map<JProblem, List<String>> problemsToFilePaths = new HashMap<>();

  private final CheckFactory checkFactory;
  private final ActiveRules activeRules;
  @Nullable
  private final ProjectDefinition projectDefinition;
  private final FileSystem fs;
  private final List<JavaCheck> mainChecks;
  private final List<JavaCheck> testChecks;
  private final List<JavaCheck> jspChecks;
  private final List<Checks<JavaCheck>> allChecks;
  private SensorContext context;
  private UnaryOperator<List<JavaCheck>> checkFilter = UnaryOperator.identity();
  private final Set<RuleKey> additionalAutoScanCompatibleRuleKeys;

  private boolean alreadyLoggedSkipStatus = false;

  public SonarComponents(FileLinesContextFactory fileLinesContextFactory, FileSystem fs,
                         ClasspathForMain javaClasspath, ClasspathForTest javaTestClasspath,
                         CheckFactory checkFactory, ActiveRules activeRules) {
    this(fileLinesContextFactory, fs, javaClasspath, javaTestClasspath, checkFactory, activeRules, null, null);
  }

  /**
   * Will be called in SonarLint context when custom rules are present
   */
  public SonarComponents(FileLinesContextFactory fileLinesContextFactory, FileSystem fs,
                         ClasspathForMain javaClasspath, ClasspathForTest javaTestClasspath, CheckFactory checkFactory,
                         ActiveRules activeRules, @Nullable CheckRegistrar[] checkRegistrars) {
    this(fileLinesContextFactory, fs, javaClasspath, javaTestClasspath, checkFactory, activeRules, checkRegistrars, null);
  }

  /**
   * Will be called in SonarScanner context when no custom rules is present
   */
  public SonarComponents(FileLinesContextFactory fileLinesContextFactory, FileSystem fs,
                         ClasspathForMain javaClasspath, ClasspathForTest javaTestClasspath, CheckFactory checkFactory,
                         ActiveRules activeRules, @Nullable ProjectDefinition projectDefinition) {
    this(fileLinesContextFactory, fs, javaClasspath, javaTestClasspath, checkFactory, activeRules,null, projectDefinition);
  }

  /**
   * ProjectDefinition class is not available in SonarLint context, so this constructor will never be called when using SonarLint
   */
  public SonarComponents(FileLinesContextFactory fileLinesContextFactory, FileSystem fs,
                         ClasspathForMain javaClasspath, ClasspathForTest javaTestClasspath, CheckFactory checkFactory,
                         ActiveRules activeRules, @Nullable CheckRegistrar[] checkRegistrars,
                         @Nullable ProjectDefinition projectDefinition) {
    this.fileLinesContextFactory = fileLinesContextFactory;
    this.fs = fs;
    this.javaClasspath = javaClasspath;
    this.javaTestClasspath = javaTestClasspath;
    this.checkFactory = checkFactory;
    this.activeRules = activeRules;
    this.projectDefinition = projectDefinition;
    this.mainChecks = new ArrayList<>();
    this.testChecks = new ArrayList<>();
    this.jspChecks = new ArrayList<>();
    this.allChecks = new ArrayList<>();
    this.additionalAutoScanCompatibleRuleKeys = new TreeSet<>();
    if (checkRegistrars != null) {
      for (CheckRegistrar registrar : checkRegistrars) {
        registrar.register(this, checkFactory);
      }
    }
  }

  public void setSensorContext(SensorContext context) {
    this.context = context;
  }

  public void setCheckFilter(UnaryOperator<List<JavaCheck>> checkFilter) {
    this.checkFilter = checkFilter;
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

  public boolean inAndroidContext() {
    return javaClasspath.inAndroidContext();
  }

  public List<File> getJavaTestClasspath() {
    return javaTestClasspath.getElements();
  }

  public List<File> getJspClasspath() {
    List<File> jspClasspath = new ArrayList<>();
    // sonar-java jar is added to classpath in order to have semantic information on code generated from JSP files
    jspClasspath.add(findPluginJar());
    jspClasspath.addAll(getJavaClasspath());
    return jspClasspath;
  }

  /**
   * @return the jar of sonar-java plugin
   */
  private static File findPluginJar() {
    try {
      return new File(SonarComponents.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    } catch (URISyntaxException e) {
      // this should not happen under normal circumstances, and if it does we want to be aware of it
      throw new IllegalStateException("Failed to obtain plugin jar.", e);
    }
  }

  @Override
  public void registerMainChecks(String repositoryKey, Collection<?> javaCheckClassesAndInstances) {
    registerCheckClasses(mainChecks, repositoryKey, javaCheckClassesAndInstances);
  }

  @Override
  public void registerTestChecks(String repositoryKey, Collection<?> javaCheckClassesAndInstances) {
    registerCheckClasses(testChecks, repositoryKey, javaCheckClassesAndInstances);
  }

  @Override
  public void registerMainSharedCheck(JavaCheck check, Collection<RuleKey> ruleKeys) {
    if (hasAtLeastOneActiveRule(ruleKeys)) {
      mainChecks.add(check);
    }
  }

  @Override
  public void registerTestSharedCheck(JavaCheck check, Collection<RuleKey> ruleKeys) {
    if (hasAtLeastOneActiveRule(ruleKeys)) {
      testChecks.add(check);
    }
  }

  @Override
  public void registerAutoScanCompatibleRules(Collection<RuleKey> ruleKeys) {
    additionalAutoScanCompatibleRuleKeys.addAll(ruleKeys);
  }

  public Set<RuleKey> getAdditionalAutoScanCompatibleRuleKeys() {
    return additionalAutoScanCompatibleRuleKeys;
  }

  private boolean hasAtLeastOneActiveRule(Collection<RuleKey> ruleKeys) {
    return ruleKeys.stream().anyMatch(ruleKey -> activeRules.find(ruleKey) != null);
  }


  private void registerCheckClasses(List<JavaCheck> destinationList, String repositoryKey, Collection<?> javaCheckClassesAndInstances) {
    Checks<JavaCheck> createdChecks = checkFactory.<JavaCheck>create(repositoryKey).addAnnotatedChecks(javaCheckClassesAndInstances);
    allChecks.add(createdChecks);
    Map<Class<? extends JavaCheck>, Integer> classIndexes = new HashMap<>();
    int i = 0;
    for (Object javaCheckClassOrInstance : javaCheckClassesAndInstances) {
      if (javaCheckClassOrInstance instanceof Class) {
        classIndexes.put((Class<? extends JavaCheck>) javaCheckClassOrInstance, i);
      } else {
        classIndexes.put(((JavaCheck) javaCheckClassOrInstance).getClass(), i);
      }
      i++;
    }
    List<? extends JavaCheck> orderedChecks = createdChecks.all().stream()
      .sorted(Comparator.comparing(check -> classIndexes.getOrDefault(check.getClass(), Integer.MAX_VALUE)))
      .toList();
    destinationList.addAll(orderedChecks);
    jspChecks.addAll(orderedChecks.stream().filter(JspCodeVisitor.class::isInstance).toList());
  }

  public List<JavaCheck> mainChecks() {
    return checkFilter.apply(mainChecks);
  }

  public List<JavaCheck> testChecks() {
    return checkFilter.apply(testChecks);
  }

  public List<JavaCheck> jspChecks() {
    return checkFilter.apply(jspChecks);
  }

  public Optional<RuleKey> getRuleKey(JavaCheck check) {
    return allChecks.stream()
      .map(sonarChecks -> sonarChecks.ruleKey(check))
      .filter(Objects::nonNull)
      .findFirst();
  }

  public void addIssue(InputComponent inputComponent, JavaCheck check, int line, String message, @Nullable Integer cost) {
    reportIssue(new AnalyzerMessage(check, inputComponent, line, message, cost != null ? cost.intValue() : 0));
  }

  public void reportIssue(AnalyzerMessage analyzerMessage) {
    JavaCheck check = analyzerMessage.getCheck();
    Objects.requireNonNull(check);
    Objects.requireNonNull(analyzerMessage.getMessage());
    getRuleKey(check).ifPresent(key -> {
      InputComponent inputComponent = analyzerMessage.getInputComponent();
      if (inputComponent == null) {
        return;
      }
      Double cost = analyzerMessage.getCost();
      reportIssue(analyzerMessage, key, inputComponent, cost);
    });
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

  private void reportAnalysisError(InputFile inputFile, String message) {
    context.newAnalysisError()
      .onFile(inputFile)
      .message(message)
      .save();
  }

  public boolean isSonarLintContext() {
    return context.runtime().getProduct() == SonarProduct.SONARLINT;
  }

  public boolean isQuickFixCompatible() {
    return isSonarLintContext() && ((SonarLintRuntime) context.runtime()).getSonarLintPluginApiVersion().isGreaterThanOrEqual(SONARLINT_6_3);
  }

  public boolean isSetQuickFixAvailableCompatible() {
    return context.runtime().getProduct() == SonarProduct.SONARQUBE && context.runtime().getApiVersion().isGreaterThanOrEqual(SONARQUBE_9_2);
  }

  public List<String> fileLines(InputFile inputFile) {
    return LineUtils.splitLines(inputFileContents(inputFile));
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

  public boolean shouldFailAnalysisOnException() {
    return context.config().getBoolean(FAIL_ON_EXCEPTION_KEY).orElse(false);
  }

  public boolean isFileByFileEnabled() {
    return context.config().getBoolean(SONAR_FILE_BY_FILE).orElse(false);
  }

  public boolean isAutoScan() {
    return (context.config().getBoolean(SONAR_BATCH_MODE_KEY).orElse(false) ||
      context.config().getBoolean(SONAR_AUTOSCAN).orElse(false)) &&
      !context.config().hasKey(SONAR_BATCH_SIZE_KEY);
  }

  public boolean isAutoScanCheckFiltering() {
    return isAutoScan() && context.config().getBoolean(SONAR_AUTOSCAN_CHECK_FILTERING).orElse(true);
  }

  /**
   * Returns the batch mode size as read from configuration, in Kilo Bytes. If not value can be found, compute dynamically an ideal value.
   *
   * @return the batch mode size or a default value of -1L.
   */
  public long getBatchModeSizeInKB() {
    Configuration config = context.config();
    if (isAutoScan()) {
      return -1L;
    }
    return config.getLong(SONAR_BATCH_SIZE_KEY).orElse(computeIdealBatchSize());
  }

  public boolean shouldIgnoreUnnamedModuleForSplitPackage() {
    return context.config().getBoolean(SONAR_IGNORE_UNNAMED_MODULE_FOR_SPLIT_PACKAGE).orElse(false);
  }

  private static long computeIdealBatchSize() {
    // We take a fraction of the total memory available though -Xmx.
    // If we assume that the average size of a file is 5KB and the average CI should have 1GB of memory,
    // it will be able to analyze 10 files in batch.
    // We max the value to 500KB (100 files) because there is only little advantages to go further.
    return Math.min(500L, ((long) (maxMemoryInBytesProvider.getAsLong() * 0.00005)) / 1000L);
  }

  public File projectLevelWorkDir() {
    var root = getRootProject();
    if (root != null) {
      return root.getWorkDir();
    } else {
      return fs.workDir();
    }
  }

  /**
   * Returns an OS-independent key that should identify the module within the project
   *
   * @return A key representing the module
   */
  public String getModuleKey() {
    var root = getRootProject();
    if (root != null && projectDefinition != null) {
      var rootBase = root.getBaseDir().toPath();
      var moduleBase = projectDefinition.getBaseDir().toPath();
      return rootBase.relativize(moduleBase).toString().replace('\\', '/');
    }
    return "";
  }

  @CheckForNull
  private ProjectDefinition getRootProject() {
    ProjectDefinition current = projectDefinition;
    if (current == null) {
      return null;
    }
    while (current.getParent() != null) {
      current = current.getParent();
    }
    return current;
  }

  public boolean canSkipUnchangedFiles() throws ApiMismatchException {
    if (context == null) {
      return false;
    } else {
      var overrideSkipFlag = context.config() == null ? null : context.config().getBoolean(SONAR_CAN_SKIP_UNCHANGED_FILES_KEY).orElse(null);
      try {
        if (overrideSkipFlag != null) {
          return overrideSkipFlag;
        }
        Method canSkipUnchangedFiles = context.getClass().getMethod("canSkipUnchangedFiles");
        return (Boolean) canSkipUnchangedFiles.invoke(context);
      } catch (NoSuchMethodError | NoSuchMethodException error) {
        throw new ApiMismatchException(error);
      } catch (InvocationTargetException | IllegalAccessException error) {
        Throwable cause = error.getCause();
        if (cause instanceof NoSuchMethodError) {
          throw new ApiMismatchException(cause);
        }
        throw new ApiMismatchException(error);
      }
    }
  }


  public boolean fileCanBeSkipped(InputFile inputFile) {
    var contentHashCache = new ContentHashCache(context);
    if (inputFile instanceof GeneratedFile) {
      // Generated files should not be skipped as we cannot assess the change status of the source file
      return false;
    }
    boolean canSkipInContext;
    try {
      canSkipInContext = canSkipUnchangedFiles();
      if (!alreadyLoggedSkipStatus) {
        if (canSkipInContext) {
          LOG.info("The Java analyzer is running in a context where unchanged files can be skipped. Full analysis is performed " +
            "for changed files, optimized analysis for unchanged files.");
        } else {
          LOG.info("The Java analyzer cannot skip unchanged files in this context. A full analysis is performed for all files.");
        }
        alreadyLoggedSkipStatus = true;
      }
    } catch (ApiMismatchException e) {
      if (!alreadyLoggedSkipStatus) {
        LOG.info(
          "Cannot determine whether the context allows skipping unchanged files: canSkipUnchangedFiles not part of sonar-plugin-api. Not skipping. {}",
          e.getCause().getMessage()
        );
        alreadyLoggedSkipStatus = true;
      }
      contentHashCache.writeToCache(inputFile);
      return false;
    }
    if (!canSkipInContext) {
      contentHashCache.writeToCache(inputFile);
      return false;
    }
    return contentHashCache.hasSameHashCached(inputFile);
  }

  public InputComponent project() {
    return context.project();
  }

  public void collectUndefinedTypes(String pathToFile, Set<JProblem> undefinedTypes) {
    undefinedTypes.stream().forEach(problem -> {
      List<String> filesAffectedByProblem = problemsToFilePaths.computeIfAbsent(problem, key -> new ArrayList<>());
      filesAffectedByProblem.add(pathToFile);
    });
  }

  public void logUndefinedTypes() {
    if (problemsToFilePaths.isEmpty()) {
      return;
    }
    javaClasspath.logSuspiciousEmptyLibraries();
    if (!isAutoScan()) {
      // In autoscan, test + main code are analyzed in the same batch, and we do not make the distinction between
      // test and main libraries, everything is inside "sonar.java.libraries", it is expected to let the test property empty.
      javaTestClasspath.logSuspiciousEmptyLibraries();
    }
    logUndefinedTypes(LOGGED_MAX_NUMBER_UNDEFINED_TYPES);

    // clear the set so only new undefined types will be logged
    problemsToFilePaths.clear();
  }

  private void logUndefinedTypes(int maxLines) {
    logParserMessages(
      problemsToFilePaths.entrySet().stream()
        .filter(entry -> entry.getKey().type() == JProblem.Type.UNDEFINED_TYPE),
      maxLines,
      "Unresolved imports/types have been detected during analysis. Enable DEBUG mode to see them.",
      "Unresolved imports/types:"
    );
    logParserMessages(
      problemsToFilePaths.entrySet().stream()
        .filter(entry -> entry.getKey().type() == JProblem.Type.PREVIEW_FEATURE_USED),
      maxLines,
      "Use of preview features have been detected during analysis. Enable DEBUG mode to see them.",
      "Use of preview features:"
    );
  }

  private static void logParserMessages(Stream<Map.Entry<JProblem, List<String>>> messages, int maxProblems, String warningMessage, String debugMessage) {
    String problemDelimiter = System.lineSeparator() + "- ";
    List<List<String>> messagesList = messages
      .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
      // We only consider the first `maxProblems` elements. We keep an extra one to know if we passed the threshold in later tests.
      .limit(maxProblems + 1L)
      .map(entry -> {
        List<String> paths = entry.getValue();
        List<String> problemAndPaths = new ArrayList<>(paths.size() + 1);
        problemAndPaths.add(problemDelimiter + entry.getKey().toString());
        paths.forEach(path -> problemAndPaths.add("  * " + path));
        return problemAndPaths;
      })
      .toList();

    if (messagesList.isEmpty()) {
      return;
    }

    LOG.warn(warningMessage);
    if (LOG.isDebugEnabled()) {
      boolean moreThanMax = messagesList.size() > maxProblems;
      String firstLine = moreThanMax ? (debugMessage + " (Limited to " + maxProblems + ")") : debugMessage;
      String lastLine = moreThanMax ? (System.lineSeparator() + problemDelimiter + "...") : "";
      LOG.debug(messagesList
        .stream()
        .limit(maxProblems)
        .flatMap(List::stream)
        .collect(Collectors.joining(System.lineSeparator(), firstLine, lastLine))
      );
    }
  }

  public SensorContext context() {
    return context;
  }
}
