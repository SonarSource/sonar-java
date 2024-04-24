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
package org.sonar.java.checks.verifier.internal;

import com.sonar.sslr.api.RecognitionException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.api.batch.sensor.cache.WriteCache;
import org.sonar.api.config.Configuration;
import org.sonar.java.SonarComponents;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.visitors.CommentLinesVisitor;
import org.sonar.java.caching.DummyCache;
import org.sonar.java.caching.JavaReadCacheImpl;
import org.sonar.java.caching.JavaWriteCacheImpl;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.java.checks.verifier.FilesUtils;
import org.sonar.java.classpath.ClasspathForMain;
import org.sonar.java.classpath.ClasspathForTest;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.test.classpath.TestClasspathUtils;
import org.sonar.java.testing.JavaFileScannerContextForTests;
import org.sonar.java.testing.VisitorsBridgeForTests;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.caching.CacheContext;
import org.sonarsource.analyzer.commons.checks.verifier.SingleFileVerifier;

import static java.nio.charset.StandardCharsets.UTF_8;

public class JavaCheckVerifier implements CheckVerifier {

  private static final JavaVersion DEFAULT_JAVA_VERSION = new JavaVersionImpl();
  private static final List<File> DEFAULT_CLASSPATH;
  private static final int COMMENT_PREFIX_LENGTH = 2;
  private static final int COMMENT_SUFFIX_LENGTH = 0;

  private JavaCheckVerifier() {
  }

  public static JavaCheckVerifier newInstance() {
    return new JavaCheckVerifier();
  }

  static {
    Path path = Paths.get(FilesUtils.DEFAULT_TEST_CLASSPATH_FILE.replace('/', File.separatorChar));
    // Because of 'java-custom-rules-example' module, we silently use an empty classpath if the file does not exist
    DEFAULT_CLASSPATH = Files.exists(path) ? TestClasspathUtils.loadFromFile(path.toString()) : new ArrayList<>();
    Optional.of(new File(FilesUtils.DEFAULT_TEST_CLASSES_DIRECTORY)).filter(File::exists).ifPresent(DEFAULT_CLASSPATH::add);
  }

  private List<JavaFileScanner> checks = null;
  private List<File> classpath = null;
  private JavaVersion javaVersion = null;
  private boolean inAndroidContext = false;
  private List<InputFile> files = null;
  private boolean withoutSemantic = false;
  private boolean isCacheEnabled = false;

  private CacheContext cacheContext = null;
  private ReadCache readCache;
  private WriteCache writeCache;

  private SingleFileVerifier createVerifier() {
    SingleFileVerifier singleFileVerifier = SingleFileVerifier.create(Path.of(files.get(0).relativePath()), UTF_8);

    JavaVersion actualVersion = javaVersion == null ? DEFAULT_JAVA_VERSION : javaVersion;
    List<File> actualClasspath = classpath == null ? DEFAULT_CLASSPATH : classpath;

    List<JavaFileScanner> visitors = new ArrayList<>(checks);
    CommentLinesVisitor commentLinesVisitor = new CommentLinesVisitor();
    visitors.add(commentLinesVisitor);
    SonarComponents sonarComponents = sonarComponents();
    VisitorsBridgeForTests visitorsBridge;
    if (withoutSemantic) {
      visitorsBridge = new VisitorsBridgeForTests(visitors, sonarComponents, actualVersion);
    } else {
      visitorsBridge = new VisitorsBridgeForTests(visitors, actualClasspath, sonarComponents, actualVersion);
    }

    JavaAstScanner astScanner = new JavaAstScanner(sonarComponents);
    visitorsBridge.setInAndroidContext(inAndroidContext);

    astScanner.setVisitorBridge(visitorsBridge);

    List<InputFile> filesToParse = files;
    if (isCacheEnabled) {
      visitorsBridge.setCacheContext(cacheContext);
      filesToParse = astScanner.scanWithoutParsing(files).get(false);
    }
    astScanner.scan(filesToParse);

    addComments(singleFileVerifier, commentLinesVisitor);

    JavaFileScannerContextForTests testJavaFileScannerContext = visitorsBridge.lastCreatedTestContext();
    JavaFileScannerContextForTests testModuleScannerContext = visitorsBridge.lastCreatedModuleContext();
    if (testJavaFileScannerContext != null) {
      addIssues(testJavaFileScannerContext, singleFileVerifier);
      addIssues(testModuleScannerContext, singleFileVerifier);
    }

    return singleFileVerifier;
  }

  private static void addIssues(JavaFileScannerContextForTests testJavaFileScannerContext, SingleFileVerifier singleFileVerifier) {
    testJavaFileScannerContext.getIssues().forEach(issue -> {
      String issueMessage = issue.getMessage();
      AnalyzerMessage.TextSpan textSpan = issue.primaryLocation();
      SingleFileVerifier.Issue verifierIssue = null;
      if (textSpan != null) {
        verifierIssue = singleFileVerifier.reportIssue(issueMessage).onRange(textSpan.startLine, textSpan.startCharacter, textSpan.endLine, textSpan.endCharacter);
      } else if (issue.getLine() != null) {
        verifierIssue = singleFileVerifier.reportIssue(issueMessage).onLine(issue.getLine());
      } else {
        verifierIssue = singleFileVerifier.reportIssue(issueMessage).onFile();
      }
      List<AnalyzerMessage> secondaries = issue.flows.stream().map(l -> l.isEmpty() ? null : l.get(0)).filter(Objects::nonNull).toList();
      SingleFileVerifier.Issue finalVerifierIssue = verifierIssue;
      secondaries.forEach(secondary -> addSecondary(finalVerifierIssue, secondary));
    });
  }

  private static void addSecondary(SingleFileVerifier.Issue issue, AnalyzerMessage secondary) {
    AnalyzerMessage.TextSpan textSpan = secondary.primaryLocation();
    issue.addSecondary(textSpan.startLine, textSpan.startCharacter, textSpan.endLine, textSpan.endCharacter, secondary.getMessage());
  }

  private static void addComments(SingleFileVerifier singleFileVerifier, CommentLinesVisitor commentLinesVisitor) {
    var syntaxTrivias = commentLinesVisitor.getSyntaxTrivia();
    syntaxTrivias.forEach(trivia -> singleFileVerifier.addComment(trivia.range().start().line(), trivia.range().start().columnOffset(), trivia.comment(), COMMENT_PREFIX_LENGTH,
      COMMENT_SUFFIX_LENGTH));
  }

  private SonarComponents sonarComponents() {
    SensorContext sensorContext;
    if (isCacheEnabled) {
      sensorContext = new CacheEnabledSensorContext(readCache, writeCache);
    } else {
      sensorContext = new InternalSensorContext();
    }
    FileSystem fileSystem = sensorContext.fileSystem();
    Configuration config = sensorContext.config();

    ClasspathForMain classpathForMain = new ClasspathForMain(config, fileSystem);
    ClasspathForTest classpathForTest = new ClasspathForTest(config, fileSystem);

    SonarComponents sonarComponents = new SonarComponents(null, fileSystem, classpathForMain, classpathForTest, null, null) {
      @Override
      public boolean reportAnalysisError(RecognitionException re, InputFile inputFile) {
        throw new AssertionError(String.format("Should not fail analysis (%s)", re.getMessage()));
      }

      @Override
      public boolean canSkipUnchangedFiles() {
        return isCacheEnabled;
      }
    };
    sonarComponents.setSensorContext(sensorContext);
    return sonarComponents;
  }

  @Override
  public CheckVerifier withCheck(JavaFileScanner check) {
    this.checks = Collections.singletonList(check);
    return this;
  }

  @Override
  public CheckVerifier withChecks(JavaFileScanner... checks) {
    this.checks = Arrays.asList(checks);
    return this;
  }

  @Override
  public CheckVerifier withClassPath(Collection<File> classpath) {
    this.classpath = new ArrayList<>(classpath);
    return this;
  }

  @Override
  public CheckVerifier withJavaVersion(int javaVersionAsInt) {
    return withJavaVersion(javaVersionAsInt, false);
  }

  @Override
  public CheckVerifier withJavaVersion(int javaVersionAsInt, boolean enablePreviewFeatures) {
    if (enablePreviewFeatures && javaVersionAsInt != JavaVersionImpl.MAX_SUPPORTED) {
      var message = String.format(
        "Preview features can only be enabled when the version == latest supported Java version (%d != %d)",
        javaVersionAsInt,
        JavaVersionImpl.MAX_SUPPORTED);
      throw new IllegalArgumentException(message);
    }
    this.javaVersion = new JavaVersionImpl(javaVersionAsInt, enablePreviewFeatures);
    return this;
  }

  @Override
  public CheckVerifier withinAndroidContext(boolean inAndroidContext) {
    this.inAndroidContext = inAndroidContext;
    return this;
  }

  @Override
  public CheckVerifier onFile(String filename) {
    return onFiles(Collections.singletonList(filename));
  }

  @Override
  public CheckVerifier onFiles(String... filenames) {
    return onFiles(Arrays.asList(filenames));
  }

  @Override
  public CheckVerifier onFiles(Collection<String> filenames) {
    this.files = new ArrayList<>();
    return addFiles(InputFile.Status.SAME, filenames);
  }

  @Override
  public CheckVerifier addFiles(InputFile.Status status, String... filenames) {
    return addFiles(status, Arrays.asList(filenames));
  }

  @Override
  public CheckVerifier addFiles(InputFile.Status status, Collection<String> filenames) {
    if (this.files == null) {
      this.files = new ArrayList<>(filenames.size());
    }

    var filesToAdd = filenames.stream()
      .map(name -> InternalInputFile.inputFile("", new File(name), status))
      .toList();

    var filesToAddStrings = filesToAdd.stream().map(Object::toString).toList();

    this.files.forEach(inputFile -> {
      if (filesToAddStrings.contains(inputFile.toString())) {
        throw new IllegalArgumentException(String.format("File %s was already added.", inputFile));
      }
    });

    this.files.addAll(filesToAdd);

    return this;
  }

  @Override
  public CheckVerifier withoutSemantic() {
    this.withoutSemantic = true;
    return this;
  }

  @Override
  public CheckVerifier withCache(@Nullable ReadCache readCache, @Nullable WriteCache writeCache) {
    this.isCacheEnabled = true;
    this.readCache = readCache;
    this.writeCache = writeCache;
    this.cacheContext = new InternalCacheContext(
      true,
      readCache == null ? new DummyCache() : new JavaReadCacheImpl(readCache),
      writeCache == null ? new DummyCache() : new JavaWriteCacheImpl(writeCache));
    return this;
  }

  @Override
  public void verifyIssues() {
    createVerifier().assertOneOrMoreIssues();
  }

  @Override
  public void verifyIssueOnFile(String expectedIssueMessage) {
    createVerifier().assertOneOrMoreIssues();
  }

  @Override
  public void verifyIssueOnProject(String expectedIssueMessage) {
    throw new UnsupportedOperationException("Not implemented!");
  }

  @Override
  public void verifyNoIssues() {
    createVerifier().assertNoIssuesRaised();
  }

}
