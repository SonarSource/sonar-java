/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.testing;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

/**
 * Only collect logs written on the same thead as the one used for the constructor of ThreadLocalLogTester
 */
public class ThreadLocalLogTester implements AfterEachCallback, BeforeEachCallback {

  private final ThreadLocalAppender appender = new ThreadLocalAppender(Thread.currentThread());
  private Level slf4jLevel = Level.DEBUG;

  @Override
  public void beforeEach(ExtensionContext context) {
    setLevel(slf4jLevel);
    appender.start();
    getRootLogger().addAppender(appender);
  }

  @Override
  public void afterEach(ExtensionContext context) {
    getRootLogger().detachAppender(appender);
    appender.list.clear();
    appender.stop();
  }

  public Level getLevel() {
    return slf4jLevel;
  }

  public ThreadLocalLogTester setLevel(Level level) {
    this.slf4jLevel = level;
    this.appender.setLevel(level);
    return this;
  }

  public List<String> logs() {
    return appender.list.stream()
      .map(ILoggingEvent::getFormattedMessage)
      .toList();
  }

  public List<String> logs(Level level) {
    ch.qos.logback.classic.Level logBackLevel = ch.qos.logback.classic.Level.toLevel(level.toString());
    return appender.list.stream()
      .filter(event -> event.getLevel().toInt() == logBackLevel.toInt())
      .map(ILoggingEvent::getFormattedMessage)
      .toList();
  }

  public ThreadLocalLogTester clear() {
    appender.list.clear();
    return this;
  }

  private static ch.qos.logback.classic.Logger getRootLogger() {
    return (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
  }

  public static class ThreadLocalAppender extends ListAppender<ILoggingEvent> {
    private final Thread filteringThread;
    private ch.qos.logback.classic.Level logBackLevel;
    public ThreadLocalAppender(Thread filteringThread) {
      this.filteringThread = filteringThread;
    }

    public void setLevel(Level level) {
      this.logBackLevel = ch.qos.logback.classic.Level.toLevel(level.toString());
      if (!logBackLevel.isGreaterOrEqual(getRootLogger().getLevel())) {
        getRootLogger().setLevel(logBackLevel);
      }
    }

    @Override
    protected void append(ILoggingEvent e) {
      if (Thread.currentThread() == filteringThread && e.getLevel().isGreaterOrEqual(logBackLevel)) {
        super.append(e);
      }
    }
  }
}
