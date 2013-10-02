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
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.google.common.collect.ImmutableList;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ProgressReportTest {

  @Rule
  public final Timeout timeout = new Timeout(2000);

  @Test
  public void test() throws Exception {
    Appender mockAppender = mock(Appender.class);
    Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    rootLogger.addAppender(mockAppender);
    rootLogger.setLevel(Level.ALL);

    ProgressReport report = new ProgressReport(ProgressReport.class.getName(), 500);
    report.start("foo start");
    report.message("progress");
    Thread.sleep(700);
    report.stop("foo stop");

    ArgumentCaptor<ILoggingEvent> captor = ArgumentCaptor.forClass(ILoggingEvent.class);
    verify(mockAppender, atLeast(3)).doAppend(captor.capture());

    List<ILoggingEvent> events = captor.getAllValues();
    assertThat(events.size()).isGreaterThanOrEqualTo(3);
    ILoggingEvent event = events.get(0);
    assertThat(event.getFormattedMessage()).isEqualTo("foo start");
    assertThat(event.getLevel()).isEqualTo(Level.INFO);

    ImmutableList.Builder<String> messagesBuilder = ImmutableList.builder();
    for (int i = 1; i < events.size(); i++) {
      event = events.get(i);
      messagesBuilder.add(event.getFormattedMessage());
      assertThat(event.getLevel()).isEqualTo(Level.INFO);
    }
    List<String> messages = messagesBuilder.build();

    assertThat(messages).containsOnly("progress", "foo stop");
    assertThat(messages).contains("progress");
    assertThat(messages).contains("foo stop");
  }

}
