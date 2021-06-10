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
import java.util.function.BooleanSupplier;
import org.eclipse.core.runtime.IProgressMonitor;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public class JProgressMonitor implements IProgressMonitor, Runnable {

  private static final Logger LOG = Loggers.get(JProgressMonitor.class);
  private static final long PERIOD = TimeUnit.SECONDS.toMillis(10);
  private final Thread thread;

  private final BooleanSupplier isCanceled;

  private boolean success = false;
  private int totalWork = 0;
  private int processedWork = 0;

  public JProgressMonitor(BooleanSupplier isCanceled) {
    this.isCanceled = isCanceled;

    thread = new Thread(this);
    thread.setName("Report about progress of Java AST analyzer");
    thread.setDaemon(true);
  }

  @Override
  public void run() {
    while (!Thread.interrupted()) {
      try {
        Thread.sleep(PERIOD);
        double percentage = processedWork / (double) totalWork;
        log(String.format("%d%% analyzed", (int) (percentage * 100)));
      } catch (InterruptedException e) {
        thread.interrupt();
        break;
      }
    }
  }

  @Override
  public void beginTask(String name, int totalWork) {
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

  private static void log(String message) {
    synchronized (LOG) {
      LOG.info(message);
      LOG.notifyAll();
    }
  }
}
