/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
package org.sonar.plugins.java;

import org.junit.Test;
import org.sonar.api.Plugin;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.Version;

import static org.assertj.core.api.Assertions.assertThat;

public class JavaPluginTest {

  private static final Version VERSION_6_7 = Version.create(6, 7);
  private static final Version VERSION_7_2 = Version.create(7, 2);
  private JavaPlugin javaPlugin = new JavaPlugin();

  @Test
  public void sonarLint_6_7_extensions() {
    SonarRuntime runtime = SonarRuntimeImpl.forSonarLint(VERSION_6_7);
    Plugin.Context context = new Plugin.Context(runtime);
    javaPlugin.define(context);
    assertThat(context.getExtensions()).hasSize(14);
  }

  @Test
  public void sonarLint_7_2_extensions() {
    SonarRuntime runtime = SonarRuntimeImpl.forSonarLint(VERSION_7_2);
    Plugin.Context context = new Plugin.Context(runtime);
    javaPlugin.define(context);
    assertThat(context.getExtensions()).hasSize(14);
  }

  @Test
  public void sonarqube_6_7_extensions() {
    SonarRuntime runtime = SonarRuntimeImpl.forSonarQube(VERSION_6_7, SonarQubeSide.SERVER);
    Plugin.Context context = new Plugin.Context(runtime);
    javaPlugin.define(context);
    assertThat(context.getExtensions()).hasSize(27);
  }

  @Test
  public void sonarqube_7_2_extensions() {
    SonarRuntime runtime = SonarRuntimeImpl.forSonarQube(VERSION_7_2, SonarQubeSide.SERVER);
    Plugin.Context context = new Plugin.Context(runtime);
    javaPlugin.define(context);
    assertThat(context.getExtensions()).hasSize(34);
  }

}
