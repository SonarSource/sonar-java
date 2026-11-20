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
package org.sonar.java.semsitter;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.sonar.sslr.api.RecognitionException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.AnalysisProgress;
import org.sonar.java.ExecutionTimeReport;
import org.sonar.java.model.JParseResult;
import org.sonar.java.model.JParser;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonarsource.analyzer.commons.ProgressReport;
import org.sonarsource.semsitter.wire.Document;

/**
 * The {@link JParser} backed by a SemSitter server. One server serves one call of {@link #parse}: it is started on the
 * common directory of the files, scans them all, and answers one document per file, which the {@link DocumentTranslator}
 * turns into the tree the checks run on. When the server cannot be started the files go to the {@code fallback} parser,
 * so a missing executable never costs an analysis.
 */
public final class SemSitterParser implements JParser {

  /** Starts a server on a project root. */
  @FunctionalInterface
  public interface Launcher {
    SemSitterApi start(Path projectRoot) throws IOException;
  }

  private static final Logger LOG = LoggerFactory.getLogger(SemSitterParser.class);

  static final int FILES_AHEAD = 64;

  private final Mode mode;
  private final JavaVersion javaVersion;
  private final List<File> classpath;
  private final Launcher launcher;
  private final Supplier<JParser> fallback;
  private final DocumentTranslator translator;
  private final Path javaHome;

  public SemSitterParser(Mode mode, JavaVersion javaVersion, List<File> classpath, Launcher launcher, Supplier<JParser> fallback, DocumentTranslator translator) {
    this(mode, javaVersion, classpath, launcher, fallback, translator, Path.of(System.getProperty("java.home")));
  }

  SemSitterParser(Mode mode, JavaVersion javaVersion, List<File> classpath, Launcher launcher, Supplier<JParser> fallback, DocumentTranslator translator,
    Path javaHome) {
    this.mode = mode;
    this.javaVersion = javaVersion;
    this.classpath = classpath;
    this.launcher = launcher;
    this.fallback = fallback;
    this.translator = translator;
    this.javaHome = javaHome;
  }

  @Override
  public void parse(Iterable<InputFile> inputFiles, BooleanSupplier isCanceled, AnalysisProgress analysisProgress, BiConsumer<InputFile, JParseResult> action) {
    List<InputFile> files = new ArrayList<>();
    inputFiles.forEach(files::add);
    if (files.isEmpty()) {
      return;
    }
    Path root = ProjectRoot.commonAncestor(files.stream().map(InputFile::path).toList());
    files.sort(Comparator.comparing(file -> ProjectRoot.relative(root, file.path())));
    Session session = open(root, files);
    if (session == null) {
      fallback.get().parse(files, isCanceled, analysisProgress, action);
      return;
    }
    scan(session, files, isCanceled, action);
  }

  private void scan(Session session, List<InputFile> files, BooleanSupplier isCanceled, BiConsumer<InputFile, JParseResult> action) {
    ExecutionTimeReport executionTimeReport = new ExecutionTimeReport();
    ProgressReport progressReport = new ProgressReport("Report about progress of Java AST analyzer", TimeUnit.SECONDS.toMillis(10));
    progressReport.start(files.stream().map(InputFile::toString).toList());
    boolean completed = false;
    try (session) {
      for (InputFile file : files) {
        if (isCanceled.getAsBoolean()) {
          if (mode == Mode.BATCH) {
            throw new CancellationException("Analysis cancelled");
          }
          break;
        }
        executionTimeReport.start(file);
        action.accept(file, session.parse(file));
        executionTimeReport.end();
        progressReport.nextFile();
      }
      completed = true;
    } finally {
      if (completed) {
        progressReport.stop();
      } else {
        progressReport.cancel();
      }
      if (mode == Mode.BATCH) {
        executionTimeReport.reportAsBatch();
      } else {
        executionTimeReport.report();
      }
    }
  }

  @Nullable
  private Session open(Path root, List<InputFile> files) {
    SemSitterApi api = null;
    try {
      api = launcher.start(root);
      api.setSettings(SemSitterSettings.of(javaVersion, classpath, javaHome));
      api.fullGen(files.stream().map(file -> ProjectRoot.relative(root, file.path())).toList());
      return new Session(api, root, javaVersion.isNotSet() ? Integer.MAX_VALUE : javaVersion.asInt(), classpath.isEmpty(), translator);
    } catch (IOException | RuntimeException e) {
      LOG.warn("SemSitter frontend could not start, falling back to the default one: {}", e.getMessage());
      if (api != null) {
        api.close();
      }
      return null;
    }
  }

  /** The open server and what parsing one file against it needs. */
  private static final class Session implements AutoCloseable {
    private final SemSitterApi api;
    private final Path root;
    private final int javaVersion;
    private final boolean noLibraries;
    private final DocumentTranslator translator;
    private final SessionTypes types;
    private volatile boolean open = true;

    private Session(SemSitterApi api, Path root, int javaVersion, boolean noLibraries, DocumentTranslator translator) {
      this.api = api;
      this.root = root;
      this.javaVersion = javaVersion;
      this.noLibraries = noLibraries;
      this.translator = translator;
      this.types = new SessionTypes(this::serverTypes);
    }

    private JParseResult parse(InputFile file) {
      if (!file.filename().endsWith(".java")) {
        return JParseResult.failure(new RecognitionException(1, "Parse error: unsupported file suffix for " + file.filename()));
      }
      try {
        Document document = api.getFile(ProjectRoot.relative(root, file.path()), FILES_AHEAD);
        return translator.translate(document, this::types, this::hasPackage, noLibraries, javaVersion);
      } catch (RecognitionException syntaxError) {
        return JParseResult.failure(syntaxError);
      } catch (IOException | RuntimeException failure) {
        LOG.error("SemSitter: unable to parse file {}", file, failure);
        return JParseResult.failure(new RecognitionException(-1, "SemSitter: unable to parse file.", failure));
      }
    }

    /** The declarations of the named types, or null once the server is closed: a type met after that stays as it was. */
    @Nullable
    private JsonObject types(Set<String> names) {
      if (!open) {
        return null;
      }
      return types.get(names);
    }

    private JsonObject serverTypes(Set<String> names) {
      try {
        JsonReader reader = new JsonReader(new InputStreamReader(new ByteArrayInputStream(api.getTypes(names)), StandardCharsets.UTF_8));
        reader.setNestingLimit(Integer.MAX_VALUE);
        return JsonParser.parseReader(reader).getAsJsonObject();
      } catch (IOException failure) {
        throw new UncheckedIOException(failure);
      }
    }

    @Nullable
    private Boolean hasPackage(String name) {
      if (!open) {
        return null;
      }
      try {
        return api.hasPackage(List.of(name)).get(name);
      } catch (IOException failure) {
        throw new UncheckedIOException(failure);
      }
    }

    @Override
    public void close() {
      open = false;
      api.close();
    }
  }
}
