/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.plugins.java.api;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class CheckRegistrarTest {

  @Test
  void repository_key_is_mandatory() throws Exception {
    CheckRegistrar.RegistrarContext registrarContext = new CheckRegistrar.RegistrarContext();
    List<Class<? extends JavaCheck>> checkClasses = Collections.emptyList();
    List<Class<? extends JavaCheck>> testCheckClasses = Collections.emptyList();
    try {
      registrarContext.registerClassesForRepository("  ", checkClasses, testCheckClasses);
      fail("");
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Please specify a valid repository key to register your custom rules");
    } catch (Exception e) {
      fail("");
    }
  }
}
