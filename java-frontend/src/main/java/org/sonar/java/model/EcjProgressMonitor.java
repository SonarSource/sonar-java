/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model;

import org.eclipse.core.runtime.IProgressMonitor;
import org.sonar.java.ProgressMonitor;

final class EcjProgressMonitor implements IProgressMonitor {

  private final ProgressMonitor delegate;

  EcjProgressMonitor(ProgressMonitor delegate) {
    this.delegate = delegate;
  }

  @Override
  public void beginTask(String name, int totalWork) {
    delegate.beginTask(name, totalWork);
  }

  @Override
  public void done() {
    delegate.done();
  }

  @Override
  public void worked(int work) {
    delegate.worked(work);
  }

  @Override
  public boolean isCanceled() {
    return delegate.isCanceled();
  }

  @Override
  public void setCanceled(boolean value) {
    delegate.setCanceled(value);
  }

  @Override
  public void internalWorked(double work) {
    delegate.internalWorked(work);
  }

  @Override
  public void setTaskName(String name) {
    delegate.setTaskName(name);
  }

  @Override
  public void subTask(String name) {
    delegate.subTask(name);
  }
}
