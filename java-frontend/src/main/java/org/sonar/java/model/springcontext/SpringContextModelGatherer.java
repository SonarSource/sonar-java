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
package org.sonar.java.model.springcontext;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.LongSupplier;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.model.DefaultModuleScannerContext;
import org.sonar.java.telemetry.Telemetry;
import org.sonar.java.telemetry.TelemetryKey;
import org.sonar.plugins.java.api.DependencyVersionAware;
import org.sonar.plugins.java.api.InputFileScannerContext;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.ModuleScannerContext;
import org.sonar.plugins.java.api.Version;
import org.sonar.plugins.java.api.internal.EndOfAnalysis;
import org.sonar.plugins.java.api.tree.Tree;

/**
 * Base class for visitors that need to gather data in the SpringContextModel at the end of the analysis.
 * Extending classes gather relevant spring-related data by implementing {@link #visitSpringNode},
 * {@link #leaveSpringFile} and {@link #scanSpringFileWithoutParsing}, and store it in the SpringContextModel at the
 * end of a module analysis.
 *
 * <p>The corresponding scanner entry points are final: this class implements them to measure how long gathering takes
 * and report it under {@link TelemetryKey#JAVA_SPRING_CONTEXT_MODEL_GATHERING_TIME_MS}. Durations are accumulated in
 * nanoseconds and converted to milliseconds only once, in {@link #endOfAnalysis}, as individual calls are typically
 * shorter than a millisecond and would each be truncated to zero.
 *
 * <p>All gatherers are skipped when none of {@code spring-context}, {@code spring-beans},
 * {@code spring-boot-starter}, or {@code spring-boot-starter-web} is present in the module classpath.
 *
 * <p>Extends {@link IssuableSubscriptionVisitor} so that {@link #leaveFile} is invoked by
 * {@code IssuableSubscriptionVisitorsRunner} after each file. This is required for per-file cache
 * writes: without it, cache entries are never stored and {@link #scanWithoutParsing} always misses,
 * preventing unchanged files from being skipped in incremental analyses.
 */
public abstract class SpringContextModelGatherer extends IssuableSubscriptionVisitor implements EndOfAnalysis, DependencyVersionAware {

  private final Telemetry telemetry;
  private final LongSupplier nanoTime;
  private long gatheringTimeNanos;

  protected SpringContextModelGatherer(Telemetry telemetry) {
    this(telemetry, System::nanoTime);
  }

  @VisibleForTesting
  SpringContextModelGatherer(Telemetry telemetry, LongSupplier nanoTime) {
    this.telemetry = telemetry;
    this.nanoTime = nanoTime;
  }

  @Override
  public boolean isCompatibleWithDependencies(Function<String, Optional<Version>> dependencyFinder) {
    return dependencyFinder.apply("spring-context")
      .or(() -> dependencyFinder.apply("spring-beans"))
      .or(() -> dependencyFinder.apply("spring-boot-starter"))
      .or(() -> dependencyFinder.apply("spring-boot-starter-web"))
      .isPresent();
  }

  @Override
  public final void visitNode(Tree tree) {
    long startTime = nanoTime.getAsLong();
    try {
      visitSpringNode(tree);
    } finally {
      recordElapsedTime(startTime);
    }
  }

  /**
   * Gathers data from a node of one of the kinds returned by {@link #nodesToVisit()}.
   *
   * @param tree The node to visit.
   */
  protected void visitSpringNode(Tree tree) {
    // by default, gatherers collect nothing from the nodes they visit
  }

  @Override
  public final void leaveFile(JavaFileScannerContext context) {
    long startTime = nanoTime.getAsLong();
    try {
      leaveSpringFile(context);
    } finally {
      recordElapsedTime(startTime);
    }
  }

  /**
   * Called once the file being scanned has been fully visited, typically to write what was gathered from it to the cache.
   *
   * @param context The scanner context of the file that was just scanned.
   */
  protected void leaveSpringFile(JavaFileScannerContext context) {
    // by default, gatherers have nothing to do at the end of a file
  }

  /**
   * Gathers the data of an unchanged file from the cache, without parsing it.
   *
   * @param context The scanner context of the file to restore.
   * @return True if the data of the file could be restored from the cache, false if the file needs to be parsed.
   */
  protected boolean scanSpringFileWithoutParsing(InputFileScannerContext context) {
    return false;
  }

  @Override
  public final boolean scanWithoutParsing(InputFileScannerContext context) {
    long startTime = nanoTime.getAsLong();
    try {
      return scanSpringFileWithoutParsing(context);
    } finally {
      recordElapsedTime(startTime);
    }
  }

  @Override
  public final void endOfAnalysis(ModuleScannerContext context) {
    var defaultModuleContext = (DefaultModuleScannerContext) context;
    long startTime = nanoTime.getAsLong();
    try {
      gatherSpringContextData(context, defaultModuleContext.getSpringContextModel());
    } finally {
      recordElapsedTime(startTime);
      telemetry.aggregateAsCounter(
        TelemetryKey.JAVA_SPRING_CONTEXT_MODEL_GATHERING_TIME_MS,
        TimeUnit.NANOSECONDS.toMillis(gatheringTimeNanos));
      gatheringTimeNanos = 0;
    }
  }

  private void recordElapsedTime(long startTime) {
    gatheringTimeNanos += nanoTime.getAsLong() - startTime;
  }

  /**
   * Method called at the end of the analysis of a module, allowing to store gathered data in the SpringContextModel.
   */
  public abstract void gatherSpringContextData(ModuleScannerContext context, SpringContextModel springContextModel);

}
