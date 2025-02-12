/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.core.runtime.IProgressMonitor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

import static org.assertj.core.api.Assertions.assertThat;

class ProgressMonitorTest {

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  private static AtomicInteger threadSafeLoggerId = new AtomicInteger(0);
  private final Logger logger = LoggerFactory.getLogger("ProgressMonitorTest#" + threadSafeLoggerId.incrementAndGet());

  @Test
  void test_set_cancel_does_nothing() {
    AnalysisProgress analysisProgress = new AnalysisProgress(10);
    ProgressMonitor progressMonitor = new ProgressMonitor(() -> false, analysisProgress);

    progressMonitor.setCanceled(true);
    assertThat(progressMonitor.isCanceled()).isFalse();

    progressMonitor.done();
  }

  @Test
  void methods_do_nothing() {
    AnalysisProgress analysisProgress = new AnalysisProgress(10);
    ProgressMonitor report = new ProgressMonitor(() -> false, logger, TimeUnit.MILLISECONDS.toMillis(500), analysisProgress);

    report.setTaskName("task");
    report.subTask("task");
    report.internalWorked(2.0);

    report.done();

    assertThat(logTester.logs()).isEmpty();
  }

  @Timeout(3)
  @Test
  void test_simple_report_progress() throws Exception {
    AnalysisProgress analysisProgress = new AnalysisProgress(50);
    analysisProgress.startBatch(50);
    ProgressMonitor report = new ProgressMonitor(() -> false, logger, TimeUnit.MILLISECONDS.toMillis(250), analysisProgress);

    report.beginTask("taskName", 100);

    waitForMessage(logger);
    report.worked(100);
    report.done();

    assertThat(logTester.logs()).hasSizeGreaterThanOrEqualTo(4).contains(
      "Starting batch processing.",
      "0% analyzed",
      "100% analyzed",
      "Batch processing: Done."
    );
  }

  @Timeout(3)
  @Test
  void test_empty_batch() throws Exception {
    AnalysisProgress analysisProgress = new AnalysisProgress(0);
    analysisProgress.startBatch(0);
    ProgressMonitor report = new ProgressMonitor(() -> false, logger, TimeUnit.MILLISECONDS.toMillis(250), analysisProgress);

    report.beginTask("taskName", 2);

    waitForMessage(logger);
    report.worked(2);
    report.done();

    assertThat(logTester.logs()).hasSizeGreaterThanOrEqualTo(4).contains(
      "Starting batch processing.",
      "100% analyzed",
      "Batch processing: Done."
    );
  }

  @Timeout(3)
  @Test
  void test_report_progress_first_batch() throws Exception {
    AnalysisProgress analysisProgress = new AnalysisProgress(50);
    analysisProgress.startBatch(10);
    ProgressMonitor report = new ProgressMonitor(() -> false, logger, TimeUnit.MILLISECONDS.toMillis(250), analysisProgress);

    report.beginTask("taskName", 20);

    waitForMessage(logger);
    report.worked(10);
    waitForMessage(logger);
    report.worked(10);
    waitForMessage(logger);
    report.done();

    assertThat(logTester.logs()).contains(
      "Starting batch processing.",
      "0% analyzed",
      "10% analyzed",
      "20% analyzed"
    );
  }

  @Timeout(3)
  @Test
  void test_report_progress_second_batch() throws Exception {
    AnalysisProgress analysisProgress = new AnalysisProgress(50);
    analysisProgress.startBatch(10);
    analysisProgress.endBatch();
    analysisProgress.startBatch(10);
    ProgressMonitor report = new ProgressMonitor(() -> false, logger, TimeUnit.MILLISECONDS.toMillis(250), analysisProgress);

    report.beginTask("taskName", 20);

    waitForMessage(logger);
    report.worked(10);
    waitForMessage(logger);
    report.worked(10);
    waitForMessage(logger);
    report.done();

    assertThat(logTester.logs()).contains(
      "20% analyzed",
      "30% analyzed",
      "40% analyzed"
    );
  }

  @Timeout(3)
  @Test
  void test_report_progress_last_batch() throws Exception {
    AnalysisProgress analysisProgress = new AnalysisProgress(50);
    analysisProgress.startBatch(40);
    analysisProgress.endBatch();
    analysisProgress.startBatch(10);
    ProgressMonitor report = new ProgressMonitor(() -> false, logger, TimeUnit.MILLISECONDS.toMillis(250), analysisProgress);

    report.beginTask("taskName", 20);

    waitForMessage(logger);
    report.worked(10);
    waitForMessage(logger);
    report.worked(10);
    waitForMessage(logger);
    report.done();

    assertThat(logTester.logs()).contains(
      "80% analyzed",
      "90% analyzed",
      "100% analyzed",
      "Batch processing: Done."
    );
  }

  @Timeout(3)
  @Test
  void test_report_progress() throws Exception {
    AnalysisProgress analysisProgress = new AnalysisProgress(500);
    analysisProgress.startBatch(500);
    ProgressMonitor report = new ProgressMonitor(() -> false, logger, TimeUnit.MILLISECONDS.toMillis(250), analysisProgress);

    report.beginTask("taskName", 1000);
    // Wait for start message
    waitForMessage(logger);
    // Wait for at least one progress message
    waitForMessage(logger);
    report.worked(250);
    waitForMessage(logger);
    report.worked(500);
    waitForMessage(logger);
    report.worked(250);
    waitForMessage(logger);
    report.done();

    assertThat(logTester.logs()).hasSizeGreaterThanOrEqualTo(7).contains(
      "Starting batch processing.",
      "0% analyzed",
      "25% analyzed",
      "75% analyzed",
      "100% analyzed",
      // We waited an extra log before "done()", hence the two report of "100%"
      "100% analyzed",
      "Batch processing: Done."
    );
  }

  @Timeout(3)
  @Test
  void test_unknown_total_work() throws Exception {
    AnalysisProgress analysisProgress = new AnalysisProgress(125);
    analysisProgress.startBatch(125);
    ProgressMonitor report = new ProgressMonitor(() -> false, logger, TimeUnit.MILLISECONDS.toMillis(250), analysisProgress);

    report.beginTask("taskName", IProgressMonitor.UNKNOWN);

    waitForMessage(logger);
    report.worked(250);
    waitForMessage(logger);
    report.done();

    assertThat(logTester.logs()).hasSizeGreaterThanOrEqualTo(3).contains(
      "Starting batch processing.",
      "0/UNKNOWN unit(s) analyzed",
      "250/UNKNOWN unit(s) analyzed"
    );
  }

  @Timeout(3)
  @Test
  void test_is_cancelled() throws Exception {
    AnalysisProgress analysisProgress = new AnalysisProgress(50);
    analysisProgress.startBatch(50);
    ProgressMonitor report = new ProgressMonitor(() -> true, logger, TimeUnit.MILLISECONDS.toMillis(250), analysisProgress);

    report.beginTask("taskName", 100);

    waitForMessage(logger);
    report.worked(50);
    waitForMessage(logger);
    report.isCanceled();

    assertThat(logTester.logs()).hasSizeGreaterThanOrEqualTo(4).contains(
      "Starting batch processing.",
      "0% analyzed",
      "50% analyzed",
      "Batch processing: Cancelled!"
    );
  }

  @Timeout(3)
  @Test
  void test_done_without_success() throws Exception {
    AnalysisProgress analysisProgress = new AnalysisProgress(50);
    analysisProgress.startBatch(50);
    ProgressMonitor report = new ProgressMonitor(() -> true, logger, TimeUnit.MILLISECONDS.toMillis(250), analysisProgress);

    report.beginTask("taskName", 100);

    waitForMessage(logger);
    report.worked(50);
    report.done();

    assertThat(logTester.logs()).hasSizeGreaterThanOrEqualTo(2).contains(
      "Starting batch processing.",
      "0% analyzed"
    );
  }

  @Timeout(2)
  @Test
  void interrupting_the_thread_should_never_create_a_deadlock() {
    AnalysisProgress analysisProgress = new AnalysisProgress(50);
    ProgressMonitor report = new ProgressMonitor(() -> true, logger, TimeUnit.MILLISECONDS.toMillis(500), analysisProgress);

    long start = System.currentTimeMillis();
    report.beginTask("taskName", 100);
    report.done();
    long end = System.currentTimeMillis();

    // stopping the report too soon could fail to interrupt the thread that was not yet alive,
    // and fail to set the proper state for Thread.interrupted()
    // this test ensures that the report does not loop once or is interrupted when stop() is
    // called just after start()
    assertThat(end - start).isLessThan(300);
  }

  @Timeout(1)
  @Test
  void interrupted_thread_should_exit_immediately() throws InterruptedException {
    AnalysisProgress analysisProgress = new AnalysisProgress(50);
    ProgressMonitor report = new ProgressMonitor(() -> true, logger, TimeUnit.MILLISECONDS.toMillis(500), analysisProgress);
    AtomicLong time = new AtomicLong(10000);
    Thread selfInterruptedThread = new Thread(() -> {
      // set the thread as interrupted
      Thread.currentThread().interrupt();
      long start = System.currentTimeMillis();
      // execute run, while the thread is interrupted
      report.run();
      long end = System.currentTimeMillis();
      time.set(end - start);
    });
    selfInterruptedThread.start();
    selfInterruptedThread.join();
    assertThat(time.get()).isLessThan(300);
  }

  private static void waitForMessage(Logger logger) throws InterruptedException {
    synchronized (logger) {
      logger.wait();
    }
  }
}
