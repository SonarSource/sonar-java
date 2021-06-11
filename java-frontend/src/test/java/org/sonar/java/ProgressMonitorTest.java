/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.java;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.eclipse.core.runtime.IProgressMonitor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;
import org.sonar.api.utils.log.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ProgressMonitorTest {

  @Test
  void test_set_cancel_does_nothing() {
    ProgressMonitor progressMonitor = new ProgressMonitor(() -> false);

    progressMonitor.setCanceled(true);
    assertThat(progressMonitor.isCanceled()).isFalse();

    progressMonitor.done();
  }

  @Test
  void methods_do_nothing() {
    Logger logger = mock(Logger.class);

    ProgressMonitor report = new ProgressMonitor(() -> false, logger, TimeUnit.MILLISECONDS.toMillis(500));

    report.setTaskName("task");
    report.subTask("task");
    report.internalWorked(2.0);

    report.done();

    verifyNoInteractions(logger);
  }

  @Timeout(3)
  @Test
  void test_simple_report_progress() throws Exception {
    Logger logger = mock(Logger.class);

    ProgressMonitor report = new ProgressMonitor(() -> false, logger, TimeUnit.MILLISECONDS.toMillis(250));

    report.beginTask("taskName", 100);

    waitForMessage(logger);
    report.worked(100);
    report.done();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(logger, atLeast(4)).info(captor.capture());

    List<String> messages = captor.getAllValues();
    assertThat(messages).hasSizeGreaterThanOrEqualTo(4).contains(
      "Starting batch processing.",
      "0% analyzed",
      "100% analyzed",
      "Batch processing: Done!"
    );
  }

  @Timeout(3)
  @Test
  void test_report_progress() throws Exception {
    Logger logger = mock(Logger.class);

    ProgressMonitor report = new ProgressMonitor(() -> false, logger, TimeUnit.MILLISECONDS.toMillis(250));

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

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(logger, atLeast(7)).info(captor.capture());

    List<String> messages = captor.getAllValues();
    assertThat(messages).hasSizeGreaterThanOrEqualTo(7).contains(
      "Starting batch processing.",
      "0% analyzed",
      "25% analyzed",
      "75% analyzed",
      "100% analyzed",
      // We waited an extra log before "done()", hence the two report of "100%"
      "100% analyzed",
      "Batch processing: Done!"
    );
  }

  @Timeout(3)
  @Test
  void test_unknown_total_work() throws Exception {
    Logger logger = mock(Logger.class);

    ProgressMonitor report = new ProgressMonitor(() -> false, logger, TimeUnit.MILLISECONDS.toMillis(250));

    report.beginTask("taskName", IProgressMonitor.UNKNOWN);

    waitForMessage(logger);
    report.worked(250);
    waitForMessage(logger);
    report.done();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(logger, atLeast(3)).info(captor.capture());

    List<String> messages = captor.getAllValues();
    assertThat(messages).hasSizeGreaterThanOrEqualTo(3).contains(
      "Starting batch processing.",
      "0/UNKNOWN unit(s) analyzed",
      "250/UNKNOWN unit(s) analyzed"
    );
  }

  @Timeout(3)
  @Test
  void test_is_cancelled() throws Exception {
    Logger logger = mock(Logger.class);

    ProgressMonitor report = new ProgressMonitor(() -> true, logger, TimeUnit.MILLISECONDS.toMillis(250));

    report.beginTask("taskName", 100);

    waitForMessage(logger);
    report.worked(50);
    waitForMessage(logger);
    report.isCanceled();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(logger, atLeast(4)).info(captor.capture());

    List<String> messages = captor.getAllValues();
    assertThat(messages).hasSizeGreaterThanOrEqualTo(4).contains(
      "Starting batch processing.",
      "0% analyzed",
      "50% analyzed",
      "Batch processing: Cancelled!"
    );
  }

  @Timeout(3)
  @Test
  void test_done_without_success() throws Exception {
    Logger logger = mock(Logger.class);

    ProgressMonitor report = new ProgressMonitor(() -> true, logger, TimeUnit.MILLISECONDS.toMillis(250));

    report.beginTask("taskName", 100);

    waitForMessage(logger);
    report.worked(50);
    report.done();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(logger, atLeast(2)).info(captor.capture());

    List<String> messages = captor.getAllValues();
    assertThat(messages).hasSizeGreaterThanOrEqualTo(2).contains(
      "Starting batch processing.",
      "0% analyzed"
    );
  }

  private static void waitForMessage(Logger logger) throws InterruptedException {
    synchronized (logger) {
      logger.wait();
    }
  }
}
