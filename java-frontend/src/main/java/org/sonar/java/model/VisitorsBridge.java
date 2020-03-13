/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.sonar.sslr.api.RecognitionException;
import java.io.File;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.check.Rule;
import org.sonar.java.AnalysisError;
import org.sonar.java.AnalysisException;
import org.sonar.java.CheckFailureException;
import org.sonar.java.EndOfAnalysisCheck;
import org.sonar.java.ExceptionHandler;
import org.sonar.java.IllegalRuleParameterException;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.java.SonarComponents;
import org.sonar.java.ast.visitors.SonarSymbolTableVisitor;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.bytecode.ClassLoaderBuilder;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.se.SymbolicExecutionMode;
import org.sonar.java.se.SymbolicExecutionVisitor;
import org.sonar.java.se.xproc.BehaviorCache;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

public class VisitorsBridge {

  private static final Logger LOG = Loggers.get(VisitorsBridge.class);

  private final BehaviorCache behaviorCache;
  private final List<JavaFileScanner> allScanners;
  private List<JavaFileScanner> executableScanners;
  private final SonarComponents sonarComponents;
  private final boolean symbolicExecutionEnabled;
  protected InputFile currentFile;
  protected JavaVersion javaVersion;
  private final List<File> classpath;
  private final SquidClassLoader classLoader;
  private IssuableSubsciptionVisitorsRunner issuableSubscriptionVisitorsRunner;
  private static final Predicate<JavaFileScanner> IS_ISSUABLE_SUBSCRIPTION_VISITOR = IssuableSubscriptionVisitor.class::isInstance;

  @VisibleForTesting
  public VisitorsBridge(JavaFileScanner visitor) {
    this(Collections.singletonList(visitor), new ArrayList<>(), null);
  }

  @VisibleForTesting
  public VisitorsBridge(Iterable<? extends JavaCheck> visitors, List<File> projectClasspath,
                        @Nullable SonarComponents sonarComponents) {
    this(visitors, projectClasspath, sonarComponents, SymbolicExecutionMode.DISABLED);
  }

  public VisitorsBridge(Iterable<? extends JavaCheck> visitors, List<File> projectClasspath,
                        @Nullable SonarComponents sonarComponents, SymbolicExecutionMode symbolicExecutionMode) {
    this.allScanners = new ArrayList<>();
    for (Object visitor : visitors) {
      if (visitor instanceof JavaFileScanner) {
        allScanners.add((JavaFileScanner) visitor);
      }
    }
    this.classpath = projectClasspath;
    this.executableScanners = allScanners.stream().filter(IS_ISSUABLE_SUBSCRIPTION_VISITOR.negate()).collect(Collectors.toList());
    this.issuableSubscriptionVisitorsRunner = new IssuableSubsciptionVisitorsRunner(allScanners);
    this.sonarComponents = sonarComponents;
    this.classLoader = ClassLoaderBuilder.create(projectClasspath);
    this.symbolicExecutionEnabled = symbolicExecutionMode.isEnabled();
    this.behaviorCache = new BehaviorCache(classLoader, symbolicExecutionMode.isCrossFileEnabled());
  }

  public JavaVersion getJavaVersion() {
    return javaVersion;
  }

  public List<File> getClasspath() {
    return classpath;
  }

  public void setJavaVersion(JavaVersion javaVersion) {
    this.javaVersion = javaVersion;
    List<JavaFileScanner> scannersForJavaVersion = executableScanners(allScanners, javaVersion);
    this.executableScanners = scannersForJavaVersion.stream().filter(IS_ISSUABLE_SUBSCRIPTION_VISITOR.negate()).collect(Collectors.toList());
    this.issuableSubscriptionVisitorsRunner = new IssuableSubsciptionVisitorsRunner(scannersForJavaVersion);
  }

  public void visitFile(@Nullable Tree parsedTree) {
    JavaTree.CompilationUnitTreeImpl tree = new JavaTree.CompilationUnitTreeImpl(null, new ArrayList<>(), new ArrayList<>(), null, null);
    boolean fileParsed = parsedTree != null;
    if (fileParsed && parsedTree.is(Tree.Kind.COMPILATION_UNIT)) {
      tree = (JavaTree.CompilationUnitTreeImpl) parsedTree;
      createSonarSymbolTable(tree);
    }

    JavaFileScannerContext javaFileScannerContext = createScannerContext(tree, tree.sema, sonarComponents, fileParsed);

    // Symbolic execution checks
    if (symbolicExecutionEnabled) {
      try {
        runScanner(javaFileScannerContext, new SymbolicExecutionVisitor(executableScanners, behaviorCache), AnalysisError.Kind.SE_ERROR);
        behaviorCache.cleanup();
      } catch (CheckFailureException e) {
        interruptIfFailFast(e);
      }
    }

    for (JavaFileScanner scanner : executableScanners) {
      try {
        runScanner(javaFileScannerContext, scanner, AnalysisError.Kind.CHECK_ERROR);
      } catch (CheckFailureException e) {
        interruptIfFailFast(e);
      }
    }

    try {
      issuableSubscriptionVisitorsRunner.run(javaFileScannerContext);
    } catch (CheckFailureException e) {
      interruptIfFailFast(e);
    }
  }

  private void interruptIfFailFast(CheckFailureException e) {
    if (sonarComponents != null && sonarComponents.shouldFailAnalysisOnException()) {
      throw new AnalysisException("Failing check", e);
    }
  }

  private void runScanner(JavaFileScannerContext javaFileScannerContext, JavaFileScanner scanner, AnalysisError.Kind kind) throws CheckFailureException {
    runScanner(() -> scanner.scanFile(javaFileScannerContext), scanner, kind);
  }

  private void runScanner(Runnable action, JavaFileScanner scanner, AnalysisError.Kind kind) throws CheckFailureException {
    try {
      action.run();
    } catch (IllegalRuleParameterException e) {
      // bad configuration of a rule parameter, we want to fail analysis fast.
      throw new AnalysisException("Bad configuration of rule parameter", e);
    } catch (Exception e) {
      Throwable rootCause = Throwables.getRootCause(e);
      if (rootCause instanceof InterruptedIOException || rootCause instanceof InterruptedException) {
        throw e;
      }

      String message = String.format(
        "Unable to run check %s - %s on file '%s', To help improve SonarJava, please report this problem to SonarSource : see https://www.sonarqube.org/community/",
        scanner.getClass(), ruleKey(scanner), currentFile);

      LOG.error(message, e);

      throw new CheckFailureException(message, e);
    }
  }

  private static String ruleKey(JavaFileScanner scanner) {
    Rule annotation = AnnotationUtils.getAnnotation(scanner.getClass(), Rule.class);
    if (annotation != null) {
      return annotation.key();
    }
    return "";
  }

  private static List<JavaFileScanner> executableScanners(List<JavaFileScanner> scanners, JavaVersion javaVersion) {
    ImmutableList.Builder<JavaFileScanner> results = ImmutableList.builder();
    for (JavaFileScanner scanner : scanners) {
      if (!(scanner instanceof JavaVersionAwareVisitor) || ((JavaVersionAwareVisitor) scanner).isCompatibleWithJavaVersion(javaVersion)) {
        results.add(scanner);
      }
    }
    return results.build();
  }

  protected JavaFileScannerContext createScannerContext(
    CompilationUnitTree tree, @Nullable Sema semanticModel, SonarComponents sonarComponents, boolean fileParsed) {
    return new DefaultJavaFileScannerContext(
      tree,
      currentFile,
      semanticModel,
      sonarComponents,
      javaVersion,
      fileParsed);
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

  public void processRecognitionException(RecognitionException e, InputFile inputFile) {
    if(sonarComponents == null || !sonarComponents.reportAnalysisError(e, inputFile)) {
      this.visitFile(null);
      executableScanners.stream()
        .filter(scanner -> scanner instanceof ExceptionHandler)
        .forEach(scanner -> ((ExceptionHandler) scanner).processRecognitionException(e));
    }

  }

  public void setCurrentFile(InputFile inputFile) {
    this.currentFile = inputFile;
  }

  public void endOfAnalysis() {
    allScanners.stream()
      .filter(s -> s instanceof EndOfAnalysisCheck)
      .map(EndOfAnalysisCheck.class::cast)
      .forEach(EndOfAnalysisCheck::endOfAnalysis);
    classLoader.close();
  }

  private class IssuableSubsciptionVisitorsRunner {
    private EnumMap<Tree.Kind, List<SubscriptionVisitor>> checks;
    private List<SubscriptionVisitor> subscriptionVisitors;

    IssuableSubsciptionVisitorsRunner(List<JavaFileScanner> executableScanners) {
      checks = new EnumMap<>(Tree.Kind.class);
      subscriptionVisitors = executableScanners.stream()
        .filter(IS_ISSUABLE_SUBSCRIPTION_VISITOR)
        .map(SubscriptionVisitor.class::cast)
        .collect(Collectors.toList());

      subscriptionVisitors
        .forEach(s -> s.nodesToVisit()
          .forEach(k -> checks.computeIfAbsent(k, key -> new ArrayList<>()).add(s)));
    }

    public void run(JavaFileScannerContext javaFileScannerContext) throws CheckFailureException {
      forEach(subscriptionVisitors, s -> s.setContext(javaFileScannerContext));
      visit(javaFileScannerContext.getTree());
      forEach(subscriptionVisitors, s -> s.leaveFile(javaFileScannerContext));
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
      if(!isToken) {
        forEach(subscribed, s -> s.leaveNode(tree));
      }
    }

    private final void forEach(Collection<SubscriptionVisitor> visitors, Consumer<SubscriptionVisitor> callback) throws CheckFailureException {
      for (SubscriptionVisitor visitor : visitors) {
        runScanner(() -> callback.accept(visitor), visitor, AnalysisError.Kind.CHECK_ERROR);
      }
    }
  }
}
