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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.java.AnalysisProgress;

final class PsiBatchProgress implements AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(PsiBatchProgress.class);
  private final AnalysisProgress progress;
  private final int total;
  private final AtomicInteger processed = new AtomicInteger();
  private final ScheduledExecutorService reporter;

  PsiBatchProgress(AnalysisProgress progress, int total) {
    this.progress = progress;
    this.total = total;
    if (progress.isFirstBatch()) {
      LOG.info("Starting batch processing.");
    }
    reporter = Executors.newSingleThreadScheduledExecutor(action -> {
      var thread = new Thread(action, "Report about progress of Java AST analyzer");
      thread.setDaemon(true);
      return thread;
    });
    reporter.scheduleAtFixedRate(() -> {
      double fraction = total == 0 ? 0 : processed.get() / (double) total;
      LOG.info("{}% analyzed", (int) (progress.toGlobalPercentage(fraction) * 100));
    }, 10, 10, TimeUnit.SECONDS);
  }

  void nextFile() {
    processed.incrementAndGet();
  }

  @Override
  public void close() {
    reporter.shutdownNow();
    if (processed.get() == total && progress.isLastBatch()) {
      LOG.info("100% analyzed");
      LOG.info("Batch processing: Done.");
    }
  }
}
