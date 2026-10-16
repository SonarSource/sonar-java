/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model.javac;

import org.sonar.java.javac.source.tree.CompilationUnitTree;
import org.sonar.java.javac.source.util.JavacTask;
import org.sonar.java.javac.source.util.SourcePositions;
import org.sonar.java.javac.source.util.Trees;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import org.sonar.java.javac.api.JavacTool;

/** Parses Java source with the compiler in the running JDK. */
public final class JavacParser {

  private JavacParser() {
  }

  public static Result parse(Path path) throws IOException {
    return parse(path, Options.defaults());
  }

  public static Result parse(Path path, Options options) throws IOException {
    return parse(path.toUri(), Files.readString(path, StandardCharsets.UTF_8), options);
  }

  public static Result parse(String fileName, String source) throws IOException {
    return parse(fileName, source, Options.defaults());
  }

  public static Result parse(String fileName, String source, Options options) throws IOException {
    Path path = Path.of(fileName);
    if (!path.getFileName().toString().endsWith(".java")) {
      path = path.resolveSibling(path.getFileName() + ".java");
    }
    return parse(path.toAbsolutePath().toUri(), source, options);
  }

  public static Result parse(List<Path> paths, Options options) throws IOException {
    Objects.requireNonNull(paths);
    Objects.requireNonNull(options);
    JavaCompiler compiler = systemCompiler();
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8);
    try {
      return parseFiles(compiler, diagnostics, fileManager, fileManager.getJavaFileObjectsFromPaths(paths), options);
    } catch (IOException | RuntimeException e) {
      fileManager.close();
      throw e;
    }
  }

  public static Result parse(Map<String, String> sources, Options options) throws IOException {
    Objects.requireNonNull(sources);
    Objects.requireNonNull(options);
    JavaCompiler compiler = systemCompiler();
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8);
    try {
      List<JavaFileObject> sourceFiles = sources.entrySet().stream()
        .map(entry -> new SourceFile(Path.of(entry.getKey()).toAbsolutePath().toUri(), entry.getValue()))
        .map(JavaFileObject.class::cast)
        .toList();
      return parseFiles(compiler, diagnostics, fileManager, sourceFiles, options);
    } catch (IOException | RuntimeException e) {
      fileManager.close();
      throw e;
    }
  }

  private static Result parse(URI uri, String source, Options options) throws IOException {
    Objects.requireNonNull(uri);
    Objects.requireNonNull(source);
    Objects.requireNonNull(options);
    JavaCompiler compiler = systemCompiler();
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8);
    try {
      JavaFileObject sourceFile = new SourceFile(uri, source);
      return parseFiles(compiler, diagnostics, fileManager, List.of(sourceFile), options);
    } catch (IOException | RuntimeException e) {
      fileManager.close();
      throw e;
    }
  }

  private static JavaCompiler systemCompiler() {
    return JavacTool.create();
  }

  public static boolean isFatalSyntaxError(Diagnostic<? extends JavaFileObject> diagnostic) {
    if (diagnostic.getKind() != Diagnostic.Kind.ERROR) {
      return false;
    }
    String code = diagnostic.getCode();
    return code.equals("compiler.err.premature.eof")
      || code.equals("compiler.err.class.method.or.field.expected")
      || code.equals("compiler.err.extraneous.semicolon")
      || code.startsWith("compiler.err.feature.")
      || code.startsWith("compiler.err.preview.feature.");
  }

  private static Result parseFiles(JavaCompiler compiler, DiagnosticCollector<JavaFileObject> diagnostics,
    StandardJavaFileManager fileManager, Iterable<? extends JavaFileObject> sourceFiles, Options options) throws IOException {
    fileManager.setLocationFromPaths(StandardLocation.CLASS_PATH, options.classpath);
    List<String> compilerOptions = new ArrayList<>(List.of("-proc:none", "-Xlint:cast,try"));
    if (options.sourceVersion != null) {
      compilerOptions.add("-source");
      compilerOptions.add(Integer.toString(options.sourceVersion));
    }
    if (options.enablePreview && options.sourceVersion() == 27) {
      compilerOptions.add("--enable-preview");
    }
    if (options.system != null) {
      compilerOptions.add("--system");
      compilerOptions.add(options.system.toString());
    }
    JavacTask task = (JavacTask) compiler.getTask(null, fileManager, diagnostics, compilerOptions, null, sourceFiles);
    List<CompilationUnitTree> units = new ArrayList<>();
    task.parse().forEach(units::add);
    if (options.analyze) {
      task.analyze();
    }
    Trees trees = Trees.instance(task);
    return new Result(units, task, trees, diagnostics, fileManager);
  }

  public static final class Options {
    private final List<Path> classpath;
    private final Integer sourceVersion;
    private final boolean enablePreview;
    private final boolean analyze;
    private final Path system;

    private Options(List<Path> classpath, Integer sourceVersion, boolean enablePreview, boolean analyze, Path system) {
      this.classpath = classpath;
      this.sourceVersion = sourceVersion;
      this.enablePreview = enablePreview;
      this.analyze = analyze;
      this.system = system;
    }

    public static Options defaults() {
      String system = System.getProperty("sonar.java.javac.system");
      return new Options(List.of(), null, false, false, system == null || system.isBlank() ? null : Path.of(system));
    }

    public int sourceVersion() {
      return sourceVersion == null ? 27 : sourceVersion;
    }

    public Options withClasspath(List<Path> classpath) {
      return new Options(List.copyOf(classpath), sourceVersion, enablePreview, analyze, system);
    }

    public Options withSourceVersion(int sourceVersion) {
      return new Options(classpath, sourceVersion, enablePreview, analyze, system);
    }

    public Options withPreview(boolean enablePreview) {
      return new Options(classpath, sourceVersion, enablePreview, analyze, system);
    }

    public Options withAnalysis(boolean analyze) {
      return new Options(classpath, sourceVersion, enablePreview, analyze, system);
    }

    public Options withSystem(Path system) {
      return new Options(classpath, sourceVersion, enablePreview, analyze, system);
    }
  }

  public static final class Result implements AutoCloseable {
    private final List<CompilationUnitTree> units;
    private final JavacTask task;
    private final Trees trees;
    private final DiagnosticCollector<JavaFileObject> diagnostics;
    private final StandardJavaFileManager fileManager;

    private Result(List<CompilationUnitTree> units, JavacTask task, Trees trees,
      DiagnosticCollector<JavaFileObject> diagnostics, StandardJavaFileManager fileManager) {
      this.units = List.copyOf(units);
      this.task = task;
      this.trees = trees;
      this.diagnostics = diagnostics;
      this.fileManager = fileManager;
    }

    public CompilationUnitTree unit() {
      return units.get(0);
    }

    public List<CompilationUnitTree> units() {
      return units;
    }

    public JavacTask task() {
      return task;
    }

    public Trees trees() {
      return trees;
    }

    public SourcePositions sourcePositions() {
      return trees.getSourcePositions();
    }

    public List<Diagnostic<? extends JavaFileObject>> diagnostics() {
      return new ArrayList<>(diagnostics.getDiagnostics());
    }

    @Override
    public void close() throws IOException {
      fileManager.close();
    }
  }

  private static final class SourceFile extends SimpleJavaFileObject {
    private final String source;

    private SourceFile(URI uri, String source) {
      super(uri, Kind.SOURCE);
      this.source = source;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
      return source;
    }
  }
}
