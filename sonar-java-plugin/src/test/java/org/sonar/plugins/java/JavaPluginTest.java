/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.Version;
import org.sonar.java.jsp.Jasper;

import static org.assertj.core.api.Assertions.assertThat;

public class JavaPluginTest {

  private static final Version VERSION_7_9 = Version.create(7, 9);
  private JavaPlugin javaPlugin = new JavaPlugin();

  @Test
  public void sonarLint_7_9_extensions() {
    SonarRuntime runtime = SonarRuntimeImpl.forSonarLint(VERSION_7_9);
    Plugin.Context context = new Plugin.Context(runtime);
    javaPlugin.define(context);
    assertThat(context.getExtensions()).hasSize(15);
  }


  @Test
  public void sonarqube_7_9_extensions() {
    Plugin.Context context = new Plugin.Context(SonarVersion.SQ_79_RUNTIME);
    javaPlugin.define(context);
    assertThat(context.getExtensions()).hasSize(32);
    assertThat(context.getExtensions()).doesNotContain(Jasper.class);
  }

  @Test
  public void sonarqube_commercial_extensions() {
    SonarRuntime sqEnterprise = SonarRuntimeImpl.forSonarQube(VERSION_7_9, SonarQubeSide.SCANNER, SonarEdition.ENTERPRISE);
    Plugin.Context context = new Plugin.Context(sqEnterprise);
    javaPlugin.define(context);
    assertThat(context.getExtensions()).hasSize(33);
    assertThat(context.getExtensions()).contains(Jasper.class);
  }

}
