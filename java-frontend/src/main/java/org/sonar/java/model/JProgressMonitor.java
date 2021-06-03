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
package org.sonar.java.model;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.eclipse.core.runtime.IProgressMonitor;
import org.sonarsource.analyzer.commons.ProgressReport;

public class JProgressMonitor implements IProgressMonitor {

  private final List<String> files;
  private final ProgressReport progressReport;

  private boolean canceled = false;

  public JProgressMonitor(List<String> files) {
    this.files = files;
    progressReport = new ProgressReport("Report about progress of Java AST analyzer", TimeUnit.SECONDS.toMillis(10));
  }

  public void start() {
    progressReport.start(files);
  }

  @Override
  public void done() {
    progressReport.stop();
  }

  @Override
  public boolean isCanceled() {
    return canceled;
  }

  public void cancel() {
    setCanceled(true);
  }

  @Override
  public void setCanceled(boolean value) {
    if (!canceled) {
      canceled = value;
      if (canceled) {
        progressReport.cancel();
      }
    }
  }

  public void nextFile() {
    progressReport.nextFile();
  }

  @Override
  public void worked(int work) {
    // do nothing
  }

  @Override
  public void beginTask(String name, int totalWork) {
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

  @Override
  public void internalWorked(double work) {
    // do nothing
  }

}
