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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;
import org.slf4j.LoggerFactory;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProgressReportTest {

  private static final long PERIOD = 500;

  private Appender mockAppender;

  @Before
  public void setup() {
    mockAppender = mock(Appender.class);
    when(mockAppender.getName()).thenReturn("MOCK");
    Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    rootLogger.addAppender(mockAppender);
    rootLogger.setLevel(Level.ALL);
  }

  @Test
  public void should_log_start_messag() {
    new ProgressReport("").start("foo start");
    verifyLog("foo start", Level.INFO, Mockito.only());
  }

  @Test
  public void should_log_stop_message() {
    new ProgressReport("").stop("foo stop");
    verifyLog("foo stop", Level.INFO, Mockito.only());
  }

  @Test
  public void should_default_period_equal_10_seconds() {
    assertThat(new ProgressReport("").period).isEqualTo(10000L);
  }

  @Test
  public void should_not_log_progress_message_before_10_seconds() {
    ProgressReport report = new ProgressReport("");
    report.start("");
    report.message("foo progress");
    verifyLog("foo progress", Level.INFO, Mockito.never());
  }

  @Test
  public void should_log_progress_message_once() throws Exception {
    ProgressReport report = new ProgressReport("");
    report.period = PERIOD / 2;
    report.start("");

    report.message("foo progress");
    Thread.sleep(PERIOD);
    verifyLog("foo progress", Level.INFO, Mockito.atLeastOnce());

    report.stop("");
  }

  @Test
  public void should_log_progress_message_twice() throws Exception {
    ProgressReport report = new ProgressReport("");
    report.period = PERIOD / 2;
    report.start("");

    report.message("foo progress 1");
    Thread.sleep(PERIOD);
    verifyLog("foo progress 1", Level.INFO, Mockito.atLeastOnce());

    report.message("foo progress 2");
    Thread.sleep(PERIOD);
    verifyLog("foo progress 2", Level.INFO, Mockito.atLeastOnce());

    report.stop("");
  }

  @Test
  public void should_stop_upon_interruption_immediatly() throws Exception {
    ProgressReport report = new ProgressReport("");
    report.start("");
    report.thread.interrupt();
    while (report.thread.isAlive()) {
      // wait till thread dies
    }
  }

  @Test
  public void should_stop_upon_interruption_when_sleeping() throws Exception {
    ProgressReport report = new ProgressReport("");
    report.start("");
    Thread.sleep(PERIOD);
    report.thread.interrupt();
    while (report.thread.isAlive()) {
      // wait till thread dies
    }
  }

  protected void verifyLog(final String expectedFormattedMessage, final Level expectedLevel, VerificationMode mode) {
    verify(mockAppender, mode).doAppend(Matchers.argThat(new ArgumentMatcher() {

      @Override
      public boolean matches(final Object argument) {
        LoggingEvent event = (LoggingEvent) argument;
        return expectedFormattedMessage.equals(event.getFormattedMessage()) &&
          expectedLevel.equals(event.getLevel());
      }

    }));
  }

}
