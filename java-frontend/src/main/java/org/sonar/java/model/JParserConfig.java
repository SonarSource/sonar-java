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
package org.sonar.java.model;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.AnalysisException;
import org.sonar.java.AnalysisProgress;
import org.sonar.java.model.javac.JavacParser;
import org.sonar.java.model.javac.JavacBatchAnalysis;
import org.sonar.java.model.javac.JavacSourceAnalysis;
import org.sonar.plugins.java.api.JavaVersion;

public abstract class JParserConfig {

  public static final JavaVersion MAXIMUM_SUPPORTED_JAVA_VERSION = new JavaVersionImpl(JavaVersionImpl.MAX_SUPPORTED, true);

  protected final JavaVersion javaVersion;
  protected final List<File> classpath;

  protected JParserConfig(JavaVersion javaVersion, List<File> classpath) {
    this.javaVersion = javaVersion;
    this.classpath = classpath;
  }

  public abstract void parse(Iterable<? extends InputFile> inputFiles, BooleanSupplier isCanceled,
    AnalysisProgress analysisProgress, BiConsumer<InputFile, Result> action);

  public enum Mode {
    BATCH(Batch::new),
    FILE_BY_FILE(FileByFile::new);

    private final ParserConfigConstructor constructor;

    Mode(ParserConfigConstructor constructor) {
      this.constructor = constructor;
    }

    public JParserConfig create(JavaVersion javaVersion, List<File> classpath) {
      return create(javaVersion, classpath, false);
    }

    public JParserConfig create(JavaVersion javaVersion, List<File> classpath, boolean shouldIgnoreUnnamedModuleForSplitPackage) {
      return constructor.apply(javaVersion, classpath);
    }
  }

  public static class Result {
    private final Exception error;
    private final JavaTree.CompilationUnitTreeImpl tree;

    private Result(Exception error) {
      this.error = error;
      this.tree = null;
    }

    private Result(JavaTree.CompilationUnitTreeImpl tree) {
      this.error = null;
      this.tree = tree;
    }

    public JavaTree.CompilationUnitTreeImpl get() throws Exception {
      if (error != null) {
        throw error;
      }
      return tree;
    }
  }

  static boolean shouldEnablePreviewFlag(JavaVersion currentVersion) {
    return currentVersion.arePreviewFeaturesEnabled();
  }

  protected final JavacParser.Options options() {
    JavacParser.Options options = JavacParser.Options.defaults()
      .withClasspath(classpath.stream().map(File::toPath).toList())
      .withSourceVersion(javaVersion.asInt() > 0 ? Math.max(8, javaVersion.asInt()) : 27);
    return options.withPreview(shouldEnablePreviewFlag(javaVersion) || javaVersion.asInt() <= 0);
  }

  protected final void parseOne(InputFile inputFile, BiConsumer<InputFile, Result> action) {
    JavacSourceAnalysis analysis;
    try {
      analysis = JavacSourceAnalysis.analyze(inputFile.filename(), inputFile.contents(), options());
    } catch (Exception e) {
      action.accept(inputFile, new Result(e));
      return;
    }
    try (analysis) {
      action.accept(inputFile, new Result(analysis.tree()));
    } catch (java.io.IOException e) {
      throw new AnalysisException("Unable to close javac file manager", e);
    }
  }

  static class Batch extends JParserConfig {
    Batch(JavaVersion javaVersion, List<File> classpath) {
      super(javaVersion, classpath);
    }

    @Override
    public void parse(Iterable<? extends InputFile> inputFiles, BooleanSupplier isCanceled,
      AnalysisProgress analysisProgress, BiConsumer<InputFile, Result> action) {
      Map<String, String> sources = new LinkedHashMap<>();
      Map<String, InputFile> files = new LinkedHashMap<>();
      for (InputFile inputFile : inputFiles) {
        if (isCanceled.getAsBoolean()) {
          return;
        }
        files.put(inputFile.absolutePath(), inputFile);
        try {
          sources.put(inputFile.absolutePath(), inputFile.contents());
        } catch (java.io.IOException e) {
          action.accept(inputFile, new Result(e));
          files.remove(inputFile.absolutePath());
        }
      }
      if (sources.isEmpty()) {
        return;
      }
      try (JavacBatchAnalysis analysis = JavacBatchAnalysis.analyze(sources, options())) {
        for (Map.Entry<String, InputFile> entry : files.entrySet()) {
          if (isCanceled.getAsBoolean()) {
            return;
          }
          Exception error = analysis.error(entry.getKey());
          action.accept(entry.getValue(), error == null
            ? new Result(analysis.tree(entry.getKey()))
            : new Result(error));
        }
      } catch (java.io.IOException e) {
        throw new AnalysisException("Unable to analyze Java source files", e);
      }
    }
  }

  private static class FileByFile extends Batch {
    FileByFile(JavaVersion javaVersion, List<File> classpath) {
      super(javaVersion, classpath);
    }

    @Override
    public void parse(Iterable<? extends InputFile> inputFiles, BooleanSupplier isCanceled,
      AnalysisProgress analysisProgress, BiConsumer<InputFile, Result> action) {
      for (InputFile inputFile : inputFiles) {
        if (isCanceled.getAsBoolean()) {
          return;
        }
        parseOne(inputFile, action);
      }
    }
  }

  @FunctionalInterface
  public interface ParserConfigConstructor {
    JParserConfig apply(JavaVersion version, List<File> files);
  }
}
