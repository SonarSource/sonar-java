/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
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
import org.sonar.api.config.Configuration;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.Version;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.classpath.ClasspathForMain;
import org.sonar.java.classpath.ClasspathForTest;
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
public class SonarComponents {

  private static final Logger LOG = Loggers.get(SonarComponents.class);
  private static final int LOGGED_MAX_NUMBER_UNDEFINED_TYPES = 50;

  public static final String FAIL_ON_EXCEPTION_KEY = "sonar.internal.analysis.failFast";
  public static final String SONAR_BATCH_MODE_KEY = "sonar.java.internal.batchMode";
  public static final String SONAR_AUTOSCAN = "sonar.internal.analysis.autoscan";
  public static final String SONAR_AUTOSCAN_CHECK_FILTERING = "sonar.internal.analysis.autoscan.filtering";
  public static final String SONAR_BATCH_SIZE_KEY = "sonar.java.experimental.batchModeSizeInKB";
  public static final String SONAR_FILE_BY_FILE = "sonar.java.fileByFile";

  private static final Version SONARLINT_6_3 = Version.parse("6.3");
  private static final Version SONARQUBE_9_2 = Version.parse("9.2");

  private final FileLinesContextFactory fileLinesContextFactory;

  private final ClasspathForMain javaClasspath;
  private final ClasspathForTest javaTestClasspath;
  private final Set<JProblem> undefinedTypes = new HashSet<>();

  private final CheckFactory checkFactory;
  @Nullable
  private final ProjectDefinition projectDefinition;
  private final FileSystem fs;
  private final List<JavaCheck> mainChecks;
  private final List<JavaCheck> testChecks;
  private final List<JavaCheck> jspChecks;
  private final List<Checks<JavaCheck>> allChecks;
  private SensorContext context;
  private UnaryOperator<List<JavaCheck>> checkFilter = UnaryOperator.identity();

  public SonarComponents(FileLinesContextFactory fileLinesContextFactory, FileSystem fs,
                         ClasspathForMain javaClasspath, ClasspathForTest javaTestClasspath,
                         CheckFactory checkFactory) {
    this(fileLinesContextFactory, fs, javaClasspath, javaTestClasspath, checkFactory, null, null);
  }

  /**
   * Will be called in SonarLint context when custom rules are present
   */
  public SonarComponents(FileLinesContextFactory fileLinesContextFactory, FileSystem fs,
                         ClasspathForMain javaClasspath, ClasspathForTest javaTestClasspath, CheckFactory checkFactory,
                         @Nullable CheckRegistrar[] checkRegistrars) {
    this(fileLinesContextFactory, fs, javaClasspath, javaTestClasspath, checkFactory, checkRegistrars, null);
  }

  /**
   * Will be called in SonarScanner context when no custom rules is present
   */
  public SonarComponents(FileLinesContextFactory fileLinesContextFactory, FileSystem fs,
                         ClasspathForMain javaClasspath, ClasspathForTest javaTestClasspath, CheckFactory checkFactory,
                         @Nullable ProjectDefinition projectDefinition) {
    this(fileLinesContextFactory, fs, javaClasspath, javaTestClasspath, checkFactory, null, projectDefinition);
  }

  /**
   * ProjectDefinition class is not available in SonarLint context, so this constructor will never be called when using SonarLint
   */
  public SonarComponents(FileLinesContextFactory fileLinesContextFactory, FileSystem fs,
                         ClasspathForMain javaClasspath, ClasspathForTest javaTestClasspath, CheckFactory checkFactory,
                         @Nullable CheckRegistrar[] checkRegistrars, @Nullable ProjectDefinition projectDefinition) {
    this.fileLinesContextFactory = fileLinesContextFactory;
    this.fs = fs;
    this.javaClasspath = javaClasspath;
    this.javaTestClasspath = javaTestClasspath;
    this.checkFactory = checkFactory;
    this.projectDefinition = projectDefinition;
    this.mainChecks = new ArrayList<>();
    this.testChecks = new ArrayList<>();
    this.jspChecks = new ArrayList<>();
    this.allChecks = new ArrayList<>();
    if (checkRegistrars != null) {
      CheckRegistrar.RegistrarContext registrarContext = new CheckRegistrar.RegistrarContext();
      for (CheckRegistrar checkClassesRegister : checkRegistrars) {
        checkClassesRegister.register(registrarContext);
        List<Class<? extends JavaCheck>> checkClasses = getChecks(registrarContext.checkClasses());
        List<Class<? extends JavaCheck>> testCheckClasses = getChecks(registrarContext.testCheckClasses());
        registerMainCheckClasses(registrarContext.repositoryKey(), checkClasses);
        registerTestCheckClasses(registrarContext.repositoryKey(), testCheckClasses);
      }
    }
  }

  private static List<Class<? extends JavaCheck>> getChecks(@Nullable Iterable<Class<? extends JavaCheck>> iterable) {
    return iterable != null ?
      StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toList()) :
      Collections.emptyList();
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

  public void registerMainCheckClasses(String repositoryKey, Iterable<Class<? extends JavaCheck>> checkClasses) {
    registerCheckClasses(mainChecks, repositoryKey, checkClasses);
  }

  public void registerTestCheckClasses(String repositoryKey, Iterable<Class<? extends JavaCheck>> checkClasses) {
    registerCheckClasses(testChecks, repositoryKey, checkClasses);
  }

  private void registerCheckClasses(List<JavaCheck> destinationList, String repositoryKey, Iterable<Class<? extends JavaCheck>> checkClasses) {
    Checks<JavaCheck> createdChecks = checkFactory.<JavaCheck>create(repositoryKey).addAnnotatedChecks(checkClasses);
    allChecks.add(createdChecks);
    Map<Class<? extends JavaCheck>, Integer> classIndexes = new HashMap<>();
    int i = 0;
    for (Class<? extends JavaCheck> checkClass : checkClasses) {
      classIndexes.put(checkClass, i);
      i++;
    }
    List<? extends JavaCheck> orderedChecks = createdChecks.all().stream()
      .sorted(Comparator.comparing(check -> classIndexes.getOrDefault(check.getClass(), Integer.MAX_VALUE)))
      .collect(Collectors.toList());
    destinationList.addAll(orderedChecks);
    jspChecks.addAll(orderedChecks.stream().filter(JspCodeVisitor.class::isInstance).collect(Collectors.toList()));
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
   * Returns the batch mode size as read from configuration. If not value can be found, compute dynamically an ideal value.
   * @return the batch mode size or a default value of -1L.
   */
  public long getBatchModeSizeInKB() {
    Configuration config = context.config();
    if (isAutoScan()) {
      return -1L;
    }
    return config.getLong(SONAR_BATCH_SIZE_KEY).orElse(computeIdealBatchSize());
  }

  private static long computeIdealBatchSize() {
    return (long) Math.ceil((Runtime.getRuntime().totalMemory() * 0.05) / 1000L);
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

  public InputComponent project() {
    return context.project();
  }

  public void collectUndefinedTypes(Set<JProblem> undefinedTypes) {
    this.undefinedTypes.addAll(undefinedTypes);
  }

  public void logUndefinedTypes() {
    if (!undefinedTypes.isEmpty()) {
      javaClasspath.logSuspiciousEmptyLibraries();
      javaTestClasspath.logSuspiciousEmptyLibraries();
      logUndefinedTypes(LOGGED_MAX_NUMBER_UNDEFINED_TYPES);

      // clear the set so only new undefined types will be logged
      undefinedTypes.clear();
    }
  }

  private void logUndefinedTypes(int maxLines) {
    logParserMessages(
      undefinedTypes.stream()
        .filter(m -> m.type() == JProblem.Type.UNDEFINED_TYPE),
      maxLines,
      "Unresolved imports/types have been detected during analysis. Enable DEBUG mode to see them.",
      "Unresolved imports/types:"
    );
    logParserMessages(
      undefinedTypes.stream()
        .filter(m -> m.type() == JProblem.Type.PREVIEW_FEATURE_USED),
      maxLines,
      "Use of preview features have been detected during analysis. Enable DEBUG mode to see them.",
      "Use of preview features:"
    );
  }

  private static void logParserMessages(Stream<JProblem> messages, int maxLines, String warningMessage, String debugMessage) {
    final List<String> messagesList = messages
      .map(Object::toString)
      .sorted()
      .collect(Collectors.toList());
    int messagesListSize = messagesList.size();
    if (messagesListSize == 0) {
      return;
    }
    final boolean moreThanMax = messagesListSize > maxLines;

    if (moreThanMax) {
      debugMessage += " (Limited to " + maxLines + ")";
    }

    final String delimiter = System.lineSeparator() + "- ";
    final String prefix = debugMessage + delimiter;
    final String suffix = moreThanMax ? (delimiter + "...") : "";

    LOG.warn(warningMessage);
    LOG.debug(messagesList
      .stream()
      .limit(maxLines)
      .collect(Collectors.joining(delimiter, prefix, suffix)));
  }

  public SensorContext context() {
    return context;
  }
}
