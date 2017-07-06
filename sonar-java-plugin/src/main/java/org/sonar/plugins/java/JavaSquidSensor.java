/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.plugins.java;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.batch.Phase;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.DefaultJavaResourceLocator;
import org.sonar.java.JavaSquid;
import org.sonar.java.Measurer;
import org.sonar.java.SonarComponents;
import org.sonar.java.checks.CheckList;
import org.sonar.java.filters.PostAnalysisIssueFilter;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.plugins.java.api.JavaVersion;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Phase(name = Phase.Name.PRE)
@DependsUpon("BEFORE_SQUID")
@DependedUpon("squid")
public class JavaSquidSensor implements Sensor {

  private static final Logger LOG = Loggers.get(JavaSquidSensor.class);

  private final SonarComponents sonarComponents;
  private final FileSystem fs;
  private final DefaultJavaResourceLocator javaResourceLocator;
  private final Settings settings;
  private final NoSonarFilter noSonarFilter;

  public JavaSquidSensor(SonarComponents sonarComponents, FileSystem fs,
    DefaultJavaResourceLocator javaResourceLocator, Settings settings, NoSonarFilter noSonarFilter, PostAnalysisIssueFilter postAnalysisIssueFilter) {
    this.noSonarFilter = noSonarFilter;
    this.sonarComponents = sonarComponents;
    this.fs = fs;
    this.javaResourceLocator = javaResourceLocator;
    this.settings = settings;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.onlyOnLanguage(Java.KEY).name("JavaSquidSensor");
  }

  @Override
  public void execute(SensorContext context) {
    javaResourceLocator.setSensorContext(context);
    sonarComponents.setSensorContext(context);
    sonarComponents.registerCheckClasses(CheckList.REPOSITORY_KEY, CheckList.getJavaChecks());
    sonarComponents.registerTestCheckClasses(CheckList.REPOSITORY_KEY, CheckList.getJavaTestChecks());
    try {
      ExecutorService executorService = createExecutor();
      CompletionService cs = new ExecutorCompletionService(executorService);
      ThreadContext threadContext = new ThreadContext(context);
      int fileSize = 0;
      for (File sourceFile : getSourceFiles()) {
        cs.submit(() -> scan(threadContext, sourceFile, false), null);
        fileSize++;
      }
      for (File testFile : getTestFiles()) {
        cs.submit(() -> scan(threadContext, testFile, true), null);
        fileSize++;
      }
      executorService.shutdown();
      for (int i = 0; i < fileSize; i++) {
        try {
          cs.take().get();
          if (i % 10 == 0) {
            LOG.info("analyzed " + i + "/" + fileSize + " files");
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        } catch (ExecutionException e) {
          Throwable cause = e.getCause();
          if (cause instanceof RuntimeException) {
            throw (RuntimeException) cause;
          } else if (cause instanceof Error) {
            throw ((Error) cause);
          }
        }
      }
    } finally {
      sonarComponents.closeClassLoaders();
    }

  }

  private class ThreadContext {
    final ThreadLocal<JavaSquid> javaSquidThreadLocal;

    ThreadContext(SensorContext context) {
      javaSquidThreadLocal = ThreadLocal.withInitial(() -> {
        Measurer measurer = new Measurer(fs, context, noSonarFilter);
        return new JavaSquid(getJavaVersion(), sonarComponents, measurer, javaResourceLocator, new PostAnalysisIssueFilter(fs), sonarComponents.checksForParallel());
      });
    }

    JavaSquid getJavaSquid() {
      return javaSquidThreadLocal.get();
    }
  }

  private void scan(ThreadContext context, File file, boolean scanTests) {
    JavaSquid squid = context.getJavaSquid();
    Iterable<File> files = Collections.singleton(file);
    if(scanTests) {
      squid.scan(Collections.emptyList(), files);
    } else {
      squid.scan(files, Collections.emptyList());
    }
  }

  private Iterable<File> getSourceFiles() {
    return toFile(fs.inputFiles(fs.predicates().and(fs.predicates().hasLanguage(Java.KEY), fs.predicates().hasType(InputFile.Type.MAIN))));
  }

  private Iterable<File> getTestFiles() {
    return toFile(fs.inputFiles(fs.predicates().and(fs.predicates().hasLanguage(Java.KEY), fs.predicates().hasType(InputFile.Type.TEST))));
  }

  private static Iterable<File> toFile(Iterable<InputFile> inputFiles) {
    return StreamSupport.stream(inputFiles.spliterator(), false).map(InputFile::file).collect(Collectors.toList());
  }

  private ExecutorService createExecutor() {
//    int numThreads = Runtime.getRuntime().availableProcessors() + 1;
    int numThreads = 2;
    return Executors.newFixedThreadPool(numThreads, new ThreadFactoryBuilder().setNameFormat("SonarJava-parallel-analysis-%d").build());
  }

  private JavaVersion getJavaVersion() {
    return JavaVersionImpl.fromString(settings.getString(Java.SOURCE_VERSION));
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
