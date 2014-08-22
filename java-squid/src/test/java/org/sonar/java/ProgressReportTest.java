/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ProgressReportTest {

  @Rule
  public final Timeout timeout = new Timeout(10000);

  @Test
  public void test() throws Exception {
    Logger logger = mock(Logger.class);

    ProgressReport report = new ProgressReport(ProgressReport.class.getName(), 100, logger);
    report.message("progress");
    report.start("foo start");

    // Wait for start message
    waitForMessage(logger);

    // Wait for at least one progress message
    waitForMessage(logger);

    report.stop("foo stop");

    // Waits for the thread to die
    // Note: We cannot simply wait for a message here, because it could either be a progress or a stop one
    report.join();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(logger, atLeast(3)).info(captor.capture());

    List<String> messages = captor.getAllValues();
    assertThat(messages.size()).isGreaterThanOrEqualTo(3);
    assertThat(messages.get(0)).isEqualTo("foo start");
    for (int i = 1; i < messages.size() - 1; i++) {
      assertThat(messages.get(i)).isEqualTo("progress");
    }
    assertThat(messages.get(messages.size() - 1)).isEqualTo("foo stop");
  }

  private static void waitForMessage(Logger logger) throws InterruptedException {
    synchronized (logger) {
      logger.wait();
    }
  }

}
