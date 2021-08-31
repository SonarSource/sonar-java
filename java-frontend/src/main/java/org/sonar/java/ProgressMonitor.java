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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import org.eclipse.core.runtime.IProgressMonitor;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.annotations.VisibleForTesting;

public class ProgressMonitor implements IProgressMonitor, Runnable {

  private final Logger logger;
  private final long period;
  private final Thread thread;

  private final BooleanSupplier isCanceled;

  private boolean success = false;
  private int totalWork = 0;
  private boolean unknownTotalWork = false;
  private int processedWork = 0;

  /**
   * The report loop can not rely only on Thread.interrupted() to end, according to
   * interrupted() javadoc, a thread interruption can be ignored because a thread was
   * not alive at the time of the interrupt. This could happen if done() is being called
   * before ProgressMonitor's thread becomes alive.
   * So this boolean flag ensures that ProgressMonitor never enter an infinite loop when
   * Thread.interrupted() failed to be set to true.
   */
  private final AtomicBoolean interrupted = new AtomicBoolean();

  @VisibleForTesting
  ProgressMonitor(BooleanSupplier isCanceled, Logger logger, long period) {
    interrupted.set(false);
    this.isCanceled = isCanceled;
    this.logger = logger;
    this.period = period;

    thread = new Thread(this);
    thread.setName("Report about progress of Java AST analyzer");
    thread.setDaemon(true);
  }

  public ProgressMonitor(BooleanSupplier isCanceled) {
    this(isCanceled, Loggers.get(ProgressMonitor.class), TimeUnit.SECONDS.toMillis(10));
  }

  @Override
  public void run() {
    while (!(interrupted.get() || Thread.currentThread().isInterrupted())) {
      try {
        Thread.sleep(period);
        if (unknownTotalWork) {
          log(String.format("%d/UNKNOWN unit(s) analyzed", processedWork));
        } else {
          double percentage = processedWork / (double) totalWork;
          log(String.format("%d%% analyzed", (int) (percentage * 100)));
        }
      } catch (InterruptedException e) {
        interrupted.set(true);
        thread.interrupt();
        break;
      }
    }
  }

  @Override
  public void beginTask(String name, int totalWork) {
    if (totalWork <= 0) {
      unknownTotalWork = true;
    }
    this.totalWork = totalWork;
    log("Starting batch processing.");
    thread.start();
  }

  @Override
  public void done() {
    if (success) {
      log("100% analyzed");
      log("Batch processing: Done!");
    }
    interrupted.set(true);
    thread.interrupt();
    join();
  }

  @Override
  public boolean isCanceled() {
    if (isCanceled.getAsBoolean()) {
      log("Batch processing: Cancelled!");
      return true;
    }
    return false;
  }

  @Override
  public void setCanceled(boolean value) {
    // do nothing
  }

  private void join() {
    try {
      thread.join();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public void worked(int work) {
    processedWork += work;
    if (processedWork == totalWork) {
      success = true;
    }
  }

  @Override
  public void internalWorked(double work) {
    // do nothing
  }

  @Override
  public void setTaskName(String name) {
    // do nothing
  }

  @Override
  public void subTask(String name) {
    // do nothing
  }

  private void log(String message) {
    synchronized (logger) {
      logger.info(message);
      logger.notifyAll();
    }
  }
}
