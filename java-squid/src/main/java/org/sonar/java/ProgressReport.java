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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProgressReport implements Runnable {

  private final long period;
  private final Logger logger;
  private String message = "";
  private final Thread thread;
  private String stopMessage = "";

  public ProgressReport(String threadName, long period, Logger logger) {
    this.period = period;
    this.logger = logger;
    thread = new Thread(this);
    thread.setName(threadName);
  }

  public ProgressReport(String threadName, long period) {
    this(threadName, period, LoggerFactory.getLogger(ProgressReport.class));
  }

  @Override
  public void run() {
    while (!Thread.interrupted()) {
      try {
        Thread.sleep(period);
        logger.info(message);
      } catch (InterruptedException e) {
        thread.interrupt();
      }
    }
    logger.info(stopMessage);
  }

  public void start(String startMessage) {
    logger.info(startMessage);
    thread.start();
  }

  public void message(String message) {
    this.message = message;
  }

  public void stop(String stopMessage) {
    this.stopMessage = stopMessage;
    thread.interrupt();
  }

}
