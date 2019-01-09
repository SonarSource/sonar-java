/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.plugins.surefire.data;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class UnitTestResultTest {

  @Test
  public void shouldBeError() {
    UnitTestResult result = new UnitTestResult().setStatus(UnitTestResult.STATUS_ERROR);
    assertThat(result.getStatus(), is(UnitTestResult.STATUS_ERROR));
    assertThat(result.isError(), is(true));
    assertThat(result.isErrorOrFailure(), is(true));
  }

  @Test
  public void shouldBeFailure() {
    UnitTestResult result = new UnitTestResult().setStatus(UnitTestResult.STATUS_FAILURE);
    assertThat(result.getStatus(), is(UnitTestResult.STATUS_FAILURE));
    assertThat(result.isError(), is(false));
    assertThat(result.isErrorOrFailure(), is(true));
  }

  @Test
  public void shouldBeSuccess() {
    UnitTestResult result = new UnitTestResult().setStatus(UnitTestResult.STATUS_OK);
    assertThat(result.getStatus(), is(UnitTestResult.STATUS_OK));
    assertThat(result.isError(), is(false));
    assertThat(result.isErrorOrFailure(), is(false));
  }
}
