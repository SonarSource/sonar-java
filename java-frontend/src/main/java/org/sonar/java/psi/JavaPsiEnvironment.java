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
package org.sonar.java.psi;

import com.intellij.codeInsight.BaseExternalAnnotationsManager;
import com.intellij.codeInsight.ExternalAnnotationsManager;
import com.intellij.core.JavaCoreProjectEnvironment;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.EmptyProgressIndicatorBase;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LanguageLevelProjectExtension;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.PsiElementFinder;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.PsiElementFinderImpl;
import com.intellij.psi.impl.PsiJavaModuleModificationTracker;
import com.intellij.psi.impl.file.impl.JavaFileManager;
import com.intellij.testFramework.LightVirtualFile;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;

/**
 * Owns a standalone PSI project for explicitly supplied Java files.
 * Source contents are captured before opening the project and remain fixed for the session.
 * PSI objects obtained in {@link #read} are valid only while this environment remains open.
 */
public final class JavaPsiEnvironment implements AutoCloseable {

  private final Disposable disposable;
  private JavaCoreProjectEnvironment environment;
  private List<PsiJavaFile> sourceFiles;
  private BooleanSupplier isCanceled;

  private JavaPsiEnvironment(Disposable disposable, JavaCoreProjectEnvironment environment, List<PsiJavaFile> sourceFiles) {
    this.disposable = disposable;
    this.environment = environment;
    this.sourceFiles = sourceFiles;
  }

  public static JavaPsiEnvironment open(Path jdkHome, List<Path> sourceFiles, List<Path> classpath, int javaVersion) {
    return open(jdkHome, sourceFiles, classpath, javaVersion, false);
  }

  public static JavaPsiEnvironment open(Path jdkHome, List<Path> sourceFiles, List<Path> classpath, int javaVersion, boolean preview) {
    return open(jdkHome, sourceFiles, classpath, javaVersion, preview, false);
  }

  public static JavaPsiEnvironment open(Path jdkHome, List<Path> sourceFiles, List<Path> classpath,
    int javaVersion, boolean preview, boolean ignoreUnnamedModuleForSplitPackage) {
    var snapshots = sourceFiles.stream().map(JavaPsiEnvironment::javaSource).distinct().map(path -> {
      try {
        return new SourceSnapshot(path, Files.readString(path), StandardCharsets.UTF_8);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }).toList();
    return openSnapshots(jdkHome, snapshots, classpath, javaVersion, preview, ignoreUnnamedModuleForSplitPackage);
  }

  public record SourceSnapshot(Path path, String contents, Charset charset) {
    public SourceSnapshot {
      path = path.toAbsolutePath().normalize();
      java.util.Objects.requireNonNull(contents);
      java.util.Objects.requireNonNull(charset);
    }
  }

  public static JavaPsiEnvironment openSnapshots(Path jdkHome, List<SourceSnapshot> snapshots,
    List<Path> classpath, int javaVersion, boolean preview) {
    return openSnapshots(jdkHome, snapshots, classpath, javaVersion, preview, false);
  }

  public static synchronized JavaPsiEnvironment openSnapshots(Path jdkHome, List<SourceSnapshot> snapshots,
    List<Path> classpath, int javaVersion, boolean preview, boolean ignoreUnnamedModuleForSplitPackage) {
    return open(new JavaPsiConfiguration(jdkHome, classpath, javaVersion, preview, ignoreUnnamedModuleForSplitPackage), snapshots);
  }

  /** Opens a project under cancellation and retains the supplier for subsequent lazy reads. */
  public static synchronized JavaPsiEnvironment open(JavaPsiConfiguration configuration, List<SourceSnapshot> snapshots, BooleanSupplier isCanceled) {
    JavaPsiApplication.acquire();
    try {
      var indicator = indicator(isCanceled);
      return ProgressManager.getInstance().runProcess(() -> {
        indicator.checkCanceled();
        var result = open(configuration, snapshots);
        result.isCanceled = isCanceled;
        return result;
      }, indicator);
    } finally {
      JavaPsiApplication.release();
    }
  }

  public static synchronized JavaPsiEnvironment open(JavaPsiConfiguration configuration, List<SourceSnapshot> snapshots) {
    int javaVersion = configuration.sourceVersion();
    boolean preview = configuration.previewEnabled();
    boolean ignoreUnnamedModuleForSplitPackage = configuration.ignoreUnnamedModuleForSplitPackage();
    var languageLevel = Arrays.stream(LanguageLevel.values())
      .filter(level -> level.feature() == javaVersion && level.isPreview() == preview && !level.isUnsupported())
      .findFirst().orElseThrow(() -> new IllegalArgumentException("Unsupported Java source version: " + javaVersion + ", preview=" + preview));
    var runtime = configuration.runtime();
    var inputs = snapshots.stream()
      .filter(input -> javaVersion >= 9 || !input.path().getFileName().toString().equals("module-info.java"))
      .toList();
    if (inputs.stream().map(SourceSnapshot::path).distinct().count() != inputs.size()) {
      throw new IllegalArgumentException("Duplicate source snapshot paths");
    }
    var libraries = new java.util.ArrayList<>(configuration.classpath().stream()
      .filter(path -> Files.isDirectory(path) || Files.isRegularFile(path)).map(Path::toFile).toList());
    var explicitRuntimes = configuration.runtimeArchives().stream().map(Path::toFile).toList();
    var images = new java.util.ArrayList<Path>();
    if (explicitRuntimes.isEmpty()) {
      if (Files.isRegularFile(runtime.resolve("lib/modules"))) {
        images.add(runtime);
      } else {
        var archive = List.of(runtime.resolve("jre/lib/rt.jar"), runtime.resolve("lib/rt.jar")).stream()
          .filter(Files::isRegularFile).findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Expected a JDK image or runtime archive: " + runtime));
        libraries.add(archive.toFile());
      }
    } else {
      explicitRuntimes.stream().filter(file -> file.getName().equals("jrt-fs.jar"))
        .forEach(file -> {
          if (!file.isFile()) {
            throw new IllegalStateException("Failed to init packages for " + file);
          }
          images.add(file.toPath().getParent().getParent());
        });
    }
    ProgressManager.checkCanceled();
    var application = JavaPsiApplication.acquire();
    var owner = Disposer.newDisposable("SonarJava PSI project");
    try {
      var core = new JavaCoreProjectEnvironment(owner, application) {
        @Override
        protected JavaFileManager createCoreFileManager() {
          return new ExplicitJavaFileManager(myPsiManager, javaVersion, ignoreUnnamedModuleForSplitPackage);
        }

        @Override
        public void addSourcesToClasspath(VirtualFile root) {
          super.addSourcesToClasspath(root);
          ((ExplicitJavaFileManager) getProject().getService(JavaFileManager.class)).indexClasspathModule(root);
        }
      };
      JavaPsiApplication.registerExtensionPoints(core.getProject().getExtensionArea(), true);
      core.getProject().registerService(com.intellij.pom.tree.TreeAspect.class, new com.intellij.pom.tree.TreeAspect());
      core.getProject().registerService(com.intellij.pom.PomModel.class, new com.intellij.pom.core.impl.PomModelImpl(core.getProject()));
      core.getProject().registerService(PsiJavaModuleModificationTracker.class, new PsiJavaModuleModificationTracker(core.getProject()));
      PsiElementFinder.EP.getPoint(core.getProject()).registerExtension(new PsiElementFinderImpl(core.getProject()), owner);
      core.getProject().registerService(ExternalAnnotationsManager.class, new BaseExternalAnnotationsManager(PsiManager.getInstance(core.getProject())) {
        @Override
        protected boolean hasAnyAnnotationsRoots() {
          return false;
        }

        @Override
        public boolean hasConfiguredAnnotationRoot(com.intellij.psi.PsiModifierListOwner owner) {
          return false;
        }

        @Override
        protected List<VirtualFile> getExternalAnnotationsRoots(VirtualFile file) {
          return List.of();
        }
      });
      ApplicationManager.getApplication().runWriteAction(() ->
        LanguageLevelProjectExtension.getInstance(core.getProject()).setLanguageLevel(languageLevel));
      for (var library : libraries) {
        var root = library.isDirectory()
          ? application.getLocalFileSystem().findFileByPath(library.getPath())
          : application.getJarFileSystem().findFileByPath(library.getPath() + "!/");
        if (root != null) {
          core.addSourcesToClasspath(root);
        }
      }
      for (var image : images) {
        var jrt = new JrtVirtualFileSystem(image);
        Disposer.register(owner, () -> {
          try {
            jrt.close();
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        });
        for (var module : jrt.moduleRoots()) {
          ProgressManager.checkCanceled();
          core.addSourcesToClasspath(module);
        }
      }
      var files = ReadAction.compute(() -> inputs.stream().map(file -> findJavaFile(core, file)).toList());
      ReadAction.run(() -> ((ExplicitJavaFileManager) core.getProject().getService(JavaFileManager.class)).index(files));
      return new JavaPsiEnvironment(owner, core, files);
    } catch (IOException failure) {
      cleanup(owner, failure);
      throw new UncheckedIOException(failure);
    } catch (RuntimeException | Error failure) {
      cleanup(owner, failure);
      throw failure;
    }
  }

  public synchronized <T> T read(BiFunction<Project, List<PsiJavaFile>, T> action) {
    if (environment == null) {
      throw new IllegalStateException("PSI environment is closed");
    }
    Computable<T> read = () -> ReadAction.compute(() -> {
      if (Thread.currentThread().isInterrupted() || (isCanceled != null && isCanceled.getAsBoolean())) {
        throw new ProcessCanceledException();
      }
      ProgressManager.checkCanceled();
      return action.apply(environment.getProject(), sourceFiles);
    });
    var progress = ProgressManager.getInstance();
    if (isCanceled == null || progress.getProgressIndicator() != null) {
      return read.compute();
    }
    return progress.runProcess(read, indicator(isCanceled));
  }

  private static CancellationIndicator indicator(BooleanSupplier isCanceled) {
    return new CancellationIndicator(isCanceled, ProgressManager.getInstance().getProgressIndicator());
  }

  private static final class CancellationIndicator extends EmptyProgressIndicatorBase {
    private final BooleanSupplier isCanceled;
    private final Thread thread = Thread.currentThread();
    private final ProgressIndicator parent;
    private volatile boolean canceled;

    private CancellationIndicator(BooleanSupplier isCanceled, ProgressIndicator parent) {
      this.isCanceled = isCanceled;
      this.parent = parent;
    }

    @Override
    public void cancel() {
      canceled = true;
    }

    @Override
    public boolean isCanceled() {
      return thread.isInterrupted() || isCanceled.getAsBoolean() || canceled || (parent != null && parent.isCanceled());
    }
  }

  @Override
  public synchronized void close() {
    if (environment != null) {
      environment = null;
      sourceFiles = List.of();
      synchronized (JavaPsiEnvironment.class) {
        try {
          Disposer.dispose(disposable);
        } finally {
          JavaPsiApplication.release();
        }
      }
    }
  }

  private static void cleanup(Disposable owner, Throwable failure) {
    try {
      Disposer.dispose(owner);
    } catch (RuntimeException | Error cleanupFailure) {
      failure.addSuppressed(cleanupFailure);
    } finally {
      try {
        JavaPsiApplication.release();
      } catch (RuntimeException | Error cleanupFailure) {
        failure.addSuppressed(cleanupFailure);
      }
    }
  }

  private static Path javaSource(Path path) {
    var normalized = path.toAbsolutePath().normalize();
    if (!Files.isRegularFile(normalized) || !normalized.toString().endsWith(".java")) {
      throw new IllegalArgumentException("Expected a Java source file: " + normalized);
    }
    return normalized;
  }

  private static PsiJavaFile findJavaFile(JavaCoreProjectEnvironment environment, SourceSnapshot input) {
    var virtualFile = new LightVirtualFile(input.path().getFileName().toString(), JavaFileType.INSTANCE,
      input.contents(), input.charset(), 0) {
      @Override
      public String getPath() {
        return input.path().toString();
      }
    };
    virtualFile.setWritable(false);
    if (PsiManager.getInstance(environment.getProject()).findFile(virtualFile) instanceof PsiJavaFile file) {
      return file;
    }
    throw new IllegalStateException("Unable to load Java PSI: " + input);
  }
}
