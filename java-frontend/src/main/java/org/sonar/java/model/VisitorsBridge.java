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
package org.sonar.java.model;

import com.sonar.sslr.api.RecognitionException;
import java.io.File;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.check.Rule;
import org.sonar.java.AnalysisException;
import org.sonar.java.CheckFailureException;
import org.sonar.java.ExceptionHandler;
import org.sonar.java.IllegalRuleParameterException;
import org.sonar.java.SonarComponents;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.ast.visitors.SonarSymbolTableVisitor;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.caching.CacheContextImpl;
import org.sonar.java.exceptions.ApiMismatchException;
import org.sonar.java.exceptions.ThrowableUtils;
import org.sonar.plugins.java.api.InputFileScannerContext;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.ModuleScannerContext;
import org.sonar.plugins.java.api.caching.CacheContext;
import org.sonar.plugins.java.api.internal.EndOfAnalysis;
import org.sonar.plugins.java.api.semantic.Sema;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonarsource.performance.measure.PerformanceMeasure;

public class VisitorsBridge {

  private static final Logger LOG = LoggerFactory.getLogger(VisitorsBridge.class);

  private final Iterable<? extends JavaCheck> visitors;
  private final List<JavaFileScanner> allScanners;
  private final List<JavaFileScanner> scannersThatCannotBeSkipped;
  private final SonarComponents sonarComponents;
  protected InputFile currentFile;
  protected final JavaVersion javaVersion;
  private final List<File> classpath;
  protected boolean inAndroidContext = false;
  private int fullyScannedFileCount = 0;
  private int skippedFileCount = 0;
  @VisibleForTesting
  CacheContext cacheContext;

  @VisibleForTesting
  public VisitorsBridge(JavaFileScanner visitor) {
    this(Collections.singletonList(visitor), new ArrayList<>(), null, new JavaVersionImpl());
  }

  @VisibleForTesting
  public VisitorsBridge(Iterable<? extends JavaCheck> visitors, List<File> projectClasspath, @Nullable SonarComponents sonarComponents) {
    this(visitors, projectClasspath, sonarComponents, new JavaVersionImpl());
  }

  public VisitorsBridge(Iterable<? extends JavaCheck> visitors, List<File> projectClasspath, @Nullable SonarComponents sonarComponents, JavaVersion javaVersion) {
    this.visitors = visitors;
    this.allScanners = new ArrayList<>();
    this.scannersThatCannotBeSkipped = new ArrayList<>();
    this.classpath = projectClasspath;
    this.sonarComponents = sonarComponents;
    this.cacheContext = CacheContextImpl.of(sonarComponents);

    this.javaVersion = javaVersion;
    updateScanners();
  }

  private void updateScanners() {
    allScanners.clear();
    scannersThatCannotBeSkipped.clear();

    allScanners.addAll(filterVisitors(visitors, this::isVisitorJavaVersionCompatible));
    if (canSkipScanningOfUnchangedFiles()) {
      scannersThatCannotBeSkipped.addAll(filterVisitors(visitors, this::isUnskippableVisitor));
    }
  }

  private List<JavaFileScanner> filterVisitors(Iterable<? extends JavaCheck> visitors, Predicate<Object> predicate) {
    List<JavaFileScanner> scanners = new ArrayList<>();
    final IssuableSubscriptionVisitorsRunner runner = new IssuableSubscriptionVisitorsRunner();

    StreamSupport.stream(visitors.spliterator(), false)
      .filter(predicate)
      .forEach(visitor -> {
        if (visitor instanceof IssuableSubscriptionVisitor issuableSubscriptionVisitor) {
          runner.add(issuableSubscriptionVisitor);
        } else if (visitor instanceof JavaFileScanner javaFileScanner) {
          scanners.add(javaFileScanner);
        }
      });

    if (!runner.subscriptionVisitors.isEmpty()) {
      scanners.add(runner);
    }
    return scanners;
  }

  boolean canSkipScanningOfUnchangedFiles() {
    try {
      return sonarComponents != null && sonarComponents.canSkipUnchangedFiles();
    } catch (ApiMismatchException ignored) {
      return false;
    }
  }

  boolean isUnskippableVisitor(Object visitor) {
    return isVisitorJavaVersionCompatible(visitor) && !canVisitorBeSkippedOnUnchangedFiles(visitor);
  }

  boolean isVisitorJavaVersionCompatible(Object visitor) {
    return !(visitor instanceof JavaVersionAwareVisitor javaVersionAwareVisitor) ||
      javaVersionAwareVisitor.isCompatibleWithJavaVersion(javaVersion);
  }

  static boolean canVisitorBeSkippedOnUnchangedFiles(Object visitor) {
    return !(visitor instanceof EndOfAnalysis) && visitor.getClass().getCanonicalName().startsWith("org.sonar.java.checks.");
  }

  public JavaVersion getJavaVersion() {
    return javaVersion;
  }

  public List<File> getClasspath() {
    return classpath;
  }

  public void setInAndroidContext(boolean inAndroidContext) {
    this.inAndroidContext = inAndroidContext;
  }

  public void setCacheContext(CacheContext cacheContext) {
    this.cacheContext = cacheContext;
  }

  /**
   * In cases where incremental analysis is enabled, try to scan a raw file without parsing its content.
   *
   * @param inputFile    The file to scan
   * @return True if all scanners successfully scan the file without contents. False otherwise.
   */
  public boolean scanWithoutParsing(InputFile inputFile) {
    if (sonarComponents != null && sonarComponents.fileCanBeSkipped(inputFile)) {
      PerformanceMeasure.Duration duration = PerformanceMeasure.start("ScanWithoutParsing");
      boolean allScansSucceeded = true;
      var fileScannerContext = createScannerContext(sonarComponents, inputFile, javaVersion, inAndroidContext, cacheContext);
      for (var scanner: scannersThatCannotBeSkipped) {
        boolean exceptionIsBlownUp = false;
        PerformanceMeasure.Duration scannerDuration = PerformanceMeasure.start(scanner);
        try {
          allScansSucceeded &= scanner.scanWithoutParsing(fileScannerContext);
        } catch (AnalysisException e) {
          // In the case where the IssuableSubscriptionVisitorsRunner throws an exception, the problem has already been
          // logged and the exception formatted.
          throw e;
        } catch (Exception e) {
          exceptionIsBlownUp = true;
          allScansSucceeded = false;
          String failureMessage = String.format(
            "Scan without parsing of file %s failed for scanner %s.",
            inputFile,
            scanner.getClass().getCanonicalName()
          );
          LOG.warn(failureMessage);
          interruptIfFailFast(new CheckFailureException(failureMessage, e));
          exceptionIsBlownUp = false;
        } finally {
          scannerDuration.stop();
          if (exceptionIsBlownUp) {
            duration.stop();
          }
        }
      }
      duration.stop();
      return allScansSucceeded;
    } else {
      return false;
    }
  }

  public void visitFile(@Nullable Tree parsedTree, boolean fileCanBeSkipped) {
    if (fileCanBeSkipped) {
      skippedFileCount++;
    } else {
      fullyScannedFileCount++;
    }

    PerformanceMeasure.Duration compilationUnitDuration = PerformanceMeasure.start("CompilationUnit");
    JavaTree.CompilationUnitTreeImpl tree = new JavaTree.CompilationUnitTreeImpl(null, new ArrayList<>(), new ArrayList<>(), null, null);
    compilationUnitDuration.stop();

    PerformanceMeasure.Duration symbolTableDuration = PerformanceMeasure.start("SymbolTable");
    boolean fileParsed = parsedTree != null;
    if (fileParsed && parsedTree.is(Tree.Kind.COMPILATION_UNIT)) {
      tree = (JavaTree.CompilationUnitTreeImpl) parsedTree;
      createSonarSymbolTable(tree);
    }
    symbolTableDuration.stop();

    JavaFileScannerContext javaFileScannerContext = createScannerContext(tree, tree.sema, sonarComponents, fileParsed);
    var scanners = getScanners(fileCanBeSkipped);

    PerformanceMeasure.Duration scannersDuration = PerformanceMeasure.start("Scanners");
    for (JavaFileScanner scanner : scanners) {
      PerformanceMeasure.Duration scannerDuration = PerformanceMeasure.start(scanner);
      try {
        runScanner(javaFileScannerContext, scanner);
      } catch (CheckFailureException e) {
        interruptIfFailFast(e);
      } finally {
        scannerDuration.stop();
      }
    }
    scannersDuration.stop();
  }

  private void interruptIfFailFast(CheckFailureException e) {
    if (sonarComponents != null && sonarComponents.shouldFailAnalysisOnException()) {
      throw new AnalysisException("Failing check", e);
    }
  }

  private void runScanner(JavaFileScannerContext javaFileScannerContext, JavaFileScanner scanner) throws CheckFailureException {
    runScanner(() -> scanner.scanFile(javaFileScannerContext), scanner);
  }

  private void runScanner(Runnable action, JavaFileScanner scanner) throws CheckFailureException {
    try {
      action.run();
    } catch (IllegalRuleParameterException e) {
      // bad configuration of a rule parameter, we want to fail analysis fast.
      throw new AnalysisException("Bad configuration of rule parameter", e);
    } catch (Exception e) {
      Throwable rootCause = ThrowableUtils.getRootCause(e);
      if (rootCause instanceof InterruptedIOException
        || rootCause instanceof InterruptedException
        || rootCause instanceof CancellationException
        || analysisCancelled()) {
        throw e;
      }

      String message = String.format(
        "Unable to run check %s - %s on file '%s', To help improve the SonarSource Java Analyzer, please report this problem to SonarSource: see https://community.sonarsource.com/",
        scanner.getClass(), ruleKey(scanner), currentFile);

      LOG.error(message, e);

      throw new CheckFailureException(message, e);
    }
  }

  private boolean analysisCancelled() {
    return sonarComponents != null && sonarComponents.analysisCancelled();
  }

  private static String ruleKey(JavaFileScanner scanner) {
    Rule annotation = AnnotationUtils.getAnnotation(scanner.getClass(), Rule.class);
    if (annotation != null) {
      return annotation.key();
    }
    return "";
  }

  protected InputFileScannerContext createScannerContext(
    SonarComponents sonarComponents, InputFile inputFile, JavaVersion javaVersion, boolean inAndroidContext, CacheContext cacheContext) {
    return new DefaultInputFileScannerContext(
      sonarComponents,
      inputFile,
      javaVersion,
      inAndroidContext,
      cacheContext
    );
  }

  protected JavaFileScannerContext createScannerContext(
    CompilationUnitTree tree, @Nullable Sema semanticModel, SonarComponents sonarComponents, boolean fileParsed) {
    return new DefaultJavaFileScannerContext(
      tree,
      currentFile,
      semanticModel,
      sonarComponents,
      javaVersion,
      fileParsed,
      inAndroidContext,
      cacheContext
    );
  }

  protected ModuleScannerContext createScannerContext(
    @Nullable SonarComponents sonarComponents, JavaVersion javaVersion, boolean inAndroidContext, @Nullable CacheContext cacheContext
  ) {
    return new DefaultModuleScannerContext(sonarComponents, javaVersion, inAndroidContext, cacheContext);
  }

  private void createSonarSymbolTable(CompilationUnitTree tree) {
    if (sonarComponents != null
      && !sonarComponents.isSonarLintContext()
      // don't provide semantic data (symbol highlighting) to SQ for generated files (jsp)
      && !(currentFile instanceof GeneratedFile)) {
      SonarSymbolTableVisitor symVisitor = new SonarSymbolTableVisitor(sonarComponents.symbolizableFor(currentFile));
      symVisitor.visitCompilationUnit(tree);
    }
  }

  private List<JavaFileScanner> getScanners(boolean supportedScannersCanBeSkippedForThisFile) {
    return supportedScannersCanBeSkippedForThisFile ? scannersThatCannotBeSkipped : allScanners;
  }

  public void processRecognitionException(RecognitionException e, InputFile inputFile) {
    if (sonarComponents == null || !sonarComponents.reportAnalysisError(e, inputFile)) {
      this.visitFile(null, false);
      getScanners(false).stream()
        .filter(ExceptionHandler.class::isInstance)
        .forEach(scanner -> ((ExceptionHandler) scanner).processRecognitionException(e));
    }
  }

  public void setCurrentFile(InputFile inputFile) {
    this.currentFile = inputFile;
  }

  public void endOfAnalysis() {
    if (skippedFileCount > 0) {
      LOG.info("Optimized analysis for {} of {} files.", skippedFileCount, skippedFileCount + fullyScannedFileCount);
    } else if (fullyScannedFileCount > 0) {
      LOG.info("Did not optimize analysis for any files, performed a full analysis for all {} files.", fullyScannedFileCount);
    }

    var moduleContext = createScannerContext(sonarComponents, javaVersion, inAndroidContext, cacheContext);

    allScanners.stream()
      .filter(EndOfAnalysis.class::isInstance)
      .map(EndOfAnalysis.class::cast)
      .forEach(check -> check.endOfAnalysis(moduleContext));
  }

  private class IssuableSubscriptionVisitorsRunner implements JavaFileScanner, EndOfAnalysis {
    private EnumMap<Tree.Kind, List<SubscriptionVisitor>> checks;
    private List<SubscriptionVisitor> subscriptionVisitors;

    IssuableSubscriptionVisitorsRunner() {
      checks = new EnumMap<>(Tree.Kind.class);
      this.subscriptionVisitors = new ArrayList<>();
    }

    private void add(SubscriptionVisitor subscriptionVisitor) {
      this.subscriptionVisitors.add(subscriptionVisitor);
      subscriptionVisitor.nodesToVisit()
        .forEach(k -> checks.computeIfAbsent(k, key -> new ArrayList<>()).add(subscriptionVisitor));
    }

    @Override
    public boolean scanWithoutParsing(InputFileScannerContext fileScannerContext) throws AnalysisException {
      boolean allScansSucceeded = true;
      for (SubscriptionVisitor visitor : subscriptionVisitors) {
        PerformanceMeasure.Duration duration = PerformanceMeasure.start(visitor);
        try {
          allScansSucceeded &= visitor.scanWithoutParsing(fileScannerContext);
        } catch (Exception e) {
          allScansSucceeded = false;
          String failureMessage = String.format(
            "Scan without parsing of file %s failed for scanner %s.",
            fileScannerContext.getInputFile(),
            visitor.getClass().getCanonicalName()
          );
          LOG.warn(failureMessage);
          interruptIfFailFast(new CheckFailureException(failureMessage, e));
        } finally {
          duration.stop();
        }
      }
      return allScansSucceeded;
    }

    @Override
    public void scanFile(JavaFileScannerContext javaFileScannerContext) {
      PerformanceMeasure.Duration issuableSubscriptionVisitorsDuration = PerformanceMeasure.start("IssuableSubscriptionVisitors");
      try {
        forEach(subscriptionVisitors, s -> s.setContext(javaFileScannerContext));
        visit(javaFileScannerContext.getTree());
        forEach(subscriptionVisitors, s -> s.leaveFile(javaFileScannerContext));
      } catch (CheckFailureException e) {
        interruptIfFailFast(e);
      } finally {
        issuableSubscriptionVisitorsDuration.stop();
      }
    }

    @Override
    public void endOfAnalysis(ModuleScannerContext cachedContext) {
      subscriptionVisitors.stream()
        .filter(EndOfAnalysis.class::isInstance)
        .map(EndOfAnalysis.class::cast)
        .forEach(check -> check.endOfAnalysis(cachedContext));
    }

    private void visitChildren(Tree tree) throws CheckFailureException {
      JavaTree javaTree = (JavaTree) tree;
      if (!javaTree.isLeaf()) {
        for (Tree next : javaTree.getChildren()) {
          if (next != null) {
            visit(next);
          }
        }
      }
    }

    private void visit(Tree tree) throws CheckFailureException {
      Kind kind = tree.kind();
      List<SubscriptionVisitor> subscribed = checks.getOrDefault(kind, Collections.emptyList());
      Consumer<SubscriptionVisitor> callback;
      boolean isToken = (kind == Tree.Kind.TOKEN);
      if (isToken) {
        callback = s -> s.visitToken((SyntaxToken) tree);
      } else {
        callback = s -> s.visitNode(tree);
      }
      forEach(subscribed, callback);
      if (isToken) {
        forEach(checks.getOrDefault(Tree.Kind.TRIVIA, Collections.emptyList()), s -> ((SyntaxToken) tree).trivias().forEach(s::visitTrivia));
      } else {
        visitChildren(tree);
      }
      if (!isToken) {
        forEach(subscribed, s -> s.leaveNode(tree));
      }
    }

    private final void forEach(Collection<SubscriptionVisitor> visitors, Consumer<SubscriptionVisitor> callback) throws CheckFailureException {
      for (SubscriptionVisitor visitor : visitors) {
        PerformanceMeasure.Duration visitorDuration = PerformanceMeasure.start(visitor);
        runScanner(() -> callback.accept(visitor), visitor);
        visitorDuration.stop();
      }
    }
  }
}
