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
package org.sonar.java.model;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.sonar.sslr.api.RecognitionException;
import java.io.File;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
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
import org.sonar.java.EndOfAnalysisCheck;
import org.sonar.java.ExceptionHandler;
import org.sonar.java.IllegalRuleParameterException;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.java.SonarComponents;
import org.sonar.java.ast.visitors.SonarSymbolTableVisitor;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.bytecode.ClassLoaderBuilder;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.se.SymbolicExecutionMode;
import org.sonar.java.se.SymbolicExecutionVisitor;
import org.sonar.java.se.xproc.BehaviorCache;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

public class VisitorsBridge {

  private static final Logger LOG = Loggers.get(VisitorsBridge.class);

  private final BehaviorCache behaviorCache;
  private final List<JavaFileScanner> allScanners;
  private List<JavaFileScanner> executableScanners;
  private final SonarComponents sonarComponents;
  private final boolean symbolicExecutionEnabled;
  private SemanticModel semanticModel;
  protected InputFile currentFile;
  protected JavaVersion javaVersion;
  private Set<String> classesNotFound = new TreeSet<>();
  private final SquidClassLoader classLoader;
  private ScannerRunner scannerRunner;
  private static Predicate<JavaFileScanner> isIssuableSubscriptionVisitor = s -> s instanceof IssuableSubscriptionVisitor;

  @VisibleForTesting
  public VisitorsBridge(JavaFileScanner visitor) {
    this(Collections.singletonList(visitor), new ArrayList<>(), null);
  }

  @VisibleForTesting
  public VisitorsBridge(Iterable visitors, List<File> projectClasspath, @Nullable SonarComponents sonarComponents) {
    this(visitors, projectClasspath, sonarComponents, SymbolicExecutionMode.DISABLED);
  }

  public VisitorsBridge(Iterable visitors, List<File> projectClasspath, @Nullable SonarComponents sonarComponents, SymbolicExecutionMode symbolicExecutionMode) {
    this.allScanners = new ArrayList<>();
    for (Object visitor : visitors) {
      if (visitor instanceof JavaFileScanner) {
        allScanners.add((JavaFileScanner) visitor);
      }
    }
    this.executableScanners = allScanners.stream().filter(isIssuableSubscriptionVisitor.negate()).collect(Collectors.toList());
    this.scannerRunner = new ScannerRunner(allScanners);
    this.sonarComponents = sonarComponents;
    this.classLoader = ClassLoaderBuilder.create(projectClasspath);
    this.symbolicExecutionEnabled = symbolicExecutionMode.isEnabled();
    this.behaviorCache = new BehaviorCache(classLoader, symbolicExecutionMode.isCrossFileEnabled());
  }

  public JavaVersion getJavaVersion() {
    return javaVersion;
  }

  public void setJavaVersion(JavaVersion javaVersion) {
    this.javaVersion = javaVersion;
    List<JavaFileScanner> scannersForJavaVersion = executableScanners(allScanners, javaVersion);
    this.executableScanners = scannersForJavaVersion.stream().filter(isIssuableSubscriptionVisitor.negate()).collect(Collectors.toList());
    this.scannerRunner = new ScannerRunner(scannersForJavaVersion);
  }

  public void visitFile(@Nullable Tree parsedTree) {
    semanticModel = null;
    CompilationUnitTree tree = new JavaTree.CompilationUnitTreeImpl(null, new ArrayList<>(), new ArrayList<>(), null, null);
    boolean fileParsed = parsedTree != null;
    if (fileParsed && parsedTree.is(Tree.Kind.COMPILATION_UNIT)) {
      tree = (CompilationUnitTree) parsedTree;
      if (isNotJavaLangOrSerializable(PackageUtils.packageName(tree.packageDeclaration(), "/"))) {
        try {
          semanticModel = SemanticModel.createFor(tree, classLoader);
        } catch (Exception e) {
          LOG.error(String.format("Unable to create symbol table for : '%s'", currentFile), e);
          addAnalysisError(e, currentFile, AnalysisError.Kind.SEMANTIC_ERROR);
          sonarComponents.reportAnalysisError(currentFile, e.getMessage());
          return;
        }
        createSonarSymbolTable(tree);
      } else {
        SemanticModel.handleMissingTypes(tree);
      }
    }
    JavaFileScannerContext javaFileScannerContext = createScannerContext(tree, semanticModel, sonarComponents, fileParsed);
    // Symbolic execution checks
    if (symbolicExecutionEnabled && isNotJavaLangOrSerializable(PackageUtils.packageName(tree.packageDeclaration(), "/"))) {
      runScanner(javaFileScannerContext, new SymbolicExecutionVisitor(executableScanners, behaviorCache), AnalysisError.Kind.SE_ERROR);
      behaviorCache.cleanup();
    }
    executableScanners.forEach(scanner -> runScanner(javaFileScannerContext, scanner, AnalysisError.Kind.CHECK_ERROR));
    scannerRunner.run(javaFileScannerContext);
    if (semanticModel != null) {
      classesNotFound.addAll(semanticModel.classesNotFound());
    }
  }

  private void runScanner(JavaFileScannerContext javaFileScannerContext, JavaFileScanner scanner, AnalysisError.Kind kind) {
    try {
      scanner.scanFile(javaFileScannerContext);
    } catch (IllegalRuleParameterException e) {
      // bad configuration of a rule parameter, we want to fail analysis fast.
      throw e;
    } catch (Exception e) {
      if (sonarComponents != null && sonarComponents.shouldFailAnalysisOnException()) {
        throw e;
      }
      Throwable rootCause = Throwables.getRootCause(e);
      if (rootCause instanceof InterruptedIOException || rootCause instanceof InterruptedException) {
        throw e;
      }
      Rule annotation = AnnotationUtils.getAnnotation(scanner.getClass(), Rule.class);
      String key = "";
      if (annotation != null) {
        key = annotation.key();
      }
      LOG.error(
        String.format("Unable to run check %s - %s on file '%s', To help improve SonarJava, please report this problem to SonarSource : see https://www.sonarqube.org/community/",
          scanner.getClass(), key, currentFile),
        e);
      addAnalysisError(e, currentFile, kind);
    }
  }

  private void addAnalysisError(Exception e, InputFile inputFile, AnalysisError.Kind checkError) {
    if (sonarComponents != null) {
      sonarComponents.addAnalysisError(new AnalysisError(e, inputFile.toString(), checkError));
    }
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
    CompilationUnitTree tree, SemanticModel semanticModel, SonarComponents sonarComponents, boolean fileParsed) {
    return new DefaultJavaFileScannerContext(
      tree,
      currentFile,
      semanticModel,
      sonarComponents,
      javaVersion,
      fileParsed);
  }

  private boolean isNotJavaLangOrSerializable(String packageName) {
    String name = currentFile.filename();
    return !(inJavaLang(packageName) || isAnnotation(packageName, name) || isSerializable(packageName, name));
  }

  private static boolean isSerializable(String packageName, String name) {
    return "java/io".equals(packageName) && "Serializable.java".equals(name);
  }

  private static boolean isAnnotation(String packageName, String name) {
    return "java/lang/annotation".equals(packageName) && "Annotation.java".equals(name);
  }

  private static boolean inJavaLang(String packageName) {
    return "java/lang".equals(packageName);
  }

  private void createSonarSymbolTable(CompilationUnitTree tree) {
    if (sonarComponents != null && !sonarComponents.isSonarLintContext()) {
      SonarSymbolTableVisitor symVisitor = new SonarSymbolTableVisitor(sonarComponents.symbolizableFor(currentFile), semanticModel);
      symVisitor.visitCompilationUnit(tree);
    }
  }

  public void processRecognitionException(RecognitionException e, InputFile inputFile) {
    addAnalysisError(e, inputFile, AnalysisError.Kind.PARSE_ERROR);
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
    if(!classesNotFound.isEmpty()) {
      String message = "";
      if(classesNotFound.size() > 50) {
        message = ", ...";
      }
      LOG.warn("Classes not found during the analysis : [{}{}]", classesNotFound.stream().limit(50).collect(Collectors.joining(", ")), message);
    }
    allScanners.stream()
      .filter(s -> s instanceof EndOfAnalysisCheck)
      .map(EndOfAnalysisCheck.class::cast)
      .forEach(EndOfAnalysisCheck::endOfAnalysis);
    classLoader.close();
  }

  private static class ScannerRunner {
    private EnumMap<Tree.Kind, List<SubscriptionVisitor>> checks;
    private List<SubscriptionVisitor> subscriptionVisitors;

    ScannerRunner(List<JavaFileScanner> executableScanners) {
      checks = new EnumMap<>(Tree.Kind.class);
      subscriptionVisitors = executableScanners.stream()
        .filter(isIssuableSubscriptionVisitor)
        .map(s -> (SubscriptionVisitor) s)
        .collect(Collectors.toList());
      subscriptionVisitors.forEach(s -> s.nodesToVisit().forEach(k -> checks.computeIfAbsent(k, key -> new ArrayList<>()).add(s))
      );
    }

    public void run(JavaFileScannerContext javaFileScannerContext) {
      subscriptionVisitors.forEach(s -> s.setContext(javaFileScannerContext));
      visit(javaFileScannerContext.getTree());
      subscriptionVisitors.forEach(s -> s.leaveFile(javaFileScannerContext));
    }

    private void visitChildren(Tree tree) {
      JavaTree javaTree = (JavaTree) tree;
      if (!javaTree.isLeaf()) {
        for (Tree next : javaTree.getChildren()) {
          if (next != null) {
            visit(next);
          }
        }
      }
    }

    private void visit(Tree tree) {
      Consumer<SubscriptionVisitor> callback;
      boolean isToken = tree.kind() == Tree.Kind.TOKEN;
      if (isToken) {
        callback = s -> {
          SyntaxToken syntaxToken = (SyntaxToken) tree;
          s.visitToken(syntaxToken);
        };
      } else {
        callback = s -> s.visitNode(tree);
      }
      List<SubscriptionVisitor> subscribed = checks.getOrDefault(tree.kind(), Collections.emptyList());
      subscribed.forEach(callback);
      if (isToken) {
        checks.getOrDefault(Tree.Kind.TRIVIA, Collections.emptyList()).forEach(s -> ((SyntaxToken) tree).trivias().forEach(s::visitTrivia));
      } else {
        visitChildren(tree);
      }
      if(!isToken) {
        subscribed.forEach(s -> s.leaveNode(tree));
      }
    }
  }
}
