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

import java.util.Collections;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class JProgressMonitorTest {

  @Test
  void cancelation() {
    JProgressMonitor progressMonitor = new JProgressMonitor(Collections.singletonList("file"));

    try {
      progressMonitor.setCanceled(false);
    } catch (Exception e) {
      fail("Should be possible to not cancel even while not started");
    }
    assertThat(progressMonitor.isCanceled()).isFalse();

    try {
      progressMonitor.cancel();
    } catch (Exception e) {
      fail("Should be possible to cancel() even while not started");
    }
    assertThat(progressMonitor.isCanceled()).isTrue();

    try {
      progressMonitor.cancel();
    } catch (Exception e) {
      fail("Should be possible to cancel() even after a first cancelation");
    }
    assertThat(progressMonitor.isCanceled()).isTrue();
  }

  @Test
  void some_method_does_nothing() {
    JProgressMonitor progressMonitor = new JProgressMonitor(Collections.singletonList("file"));
    try {
      progressMonitor.beginTask("???", Integer.MAX_VALUE);
      progressMonitor.setTaskName("???");
      progressMonitor.subTask("???");
      progressMonitor.worked(1000);
      progressMonitor.internalWorked(0.5);
    } catch (Exception e) {
      fail("should not have failed");
    }
  }

  @Test
  void start_stop() {
    JProgressMonitor progressMonitor = new JProgressMonitor(Collections.singletonList("file"));
    try {
      progressMonitor.start();
      progressMonitor.nextFile();
      progressMonitor.done();
    } catch (Exception e) {
      fail("should not have failed");
    }
  }
}
