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

  private static final Logger LOG = LoggerFactory.getLogger(ProgressReport.class);

  private boolean shouldStop = false;
  private final long period;
  private String message = "";
  private final Thread thread;

  public ProgressReport(String threadName, long period) {
    this.period = period;
    thread = new Thread(this);
    thread.setName(threadName);
  }

  @Override
  public void run() {
    while (!shouldStop) {
      try {
        Thread.sleep(period);
        LOG.info(message);
      } catch (InterruptedException e) {
        // no operation
      }
    }
    System.out.println("done");
  }

  public synchronized void start(String startMessage) {
    LOG.info(startMessage);
    thread.start();
  }

  public void message(String message) {
    this.message = message;
  }

  public synchronized void stop(String stopMessage) {
    shouldStop = true;
    LOG.info(stopMessage);
    thread.interrupt();
  }

}
