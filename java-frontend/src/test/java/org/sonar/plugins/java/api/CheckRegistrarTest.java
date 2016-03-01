/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.plugins.java.api;

import com.google.common.collect.Lists;
import org.fest.assertions.Assertions;
import org.junit.Test;

import static org.fest.assertions.Fail.fail;

public class CheckRegistrarTest {

  @Test
  public void repository_key_is_mandatory() throws Exception {
    try {
      new CheckRegistrar.RegistrarContext().registerClassesForRepository("  ", Lists.<Class<? extends JavaCheck>>newArrayList(), Lists.<Class<? extends JavaCheck>>newArrayList());
      fail();
    } catch (IllegalArgumentException e) {
      Assertions.assertThat(e).hasMessage("Please specify a valid repository key to register your custom rules");
    } catch (Exception e) {
      fail();
    }
  }
}
