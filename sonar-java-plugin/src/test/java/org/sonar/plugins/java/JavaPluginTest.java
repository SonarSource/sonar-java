/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.plugins.java;

import org.junit.jupiter.api.Test;
import org.sonar.api.Plugin;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.Version;
import org.sonar.java.jsp.Jasper;
import org.sonar.plugins.java.api.caching.SonarLintCache;

import static org.assertj.core.api.Assertions.assertThat;

class JavaPluginTest {

  private static final Version VERSION_9_9 = Version.create(9, 9);

  private final JavaPlugin javaPlugin = new JavaPlugin();

  @Test
  void sonarLint_9_9_extensions() {
    SonarRuntime runtime = SonarRuntimeImpl.forSonarLint(VERSION_9_9);
    Plugin.Context context = new Plugin.Context(runtime);
    javaPlugin.define(context);
    assertThat(context.getExtensions())
      .hasSize(19)
      .contains(SonarLintCache.class);
  }


  @Test
  void sonarqube_9_9_extensions() {
    SonarRuntime sqCommunity = SonarRuntimeImpl.forSonarQube(VERSION_9_9, SonarQubeSide.SCANNER, SonarEdition.COMMUNITY);
    Plugin.Context context = new Plugin.Context(sqCommunity);
    javaPlugin.define(context);
    assertThat(context.getExtensions())
      .hasSize(35)
      .doesNotContain(Jasper.class);
  }

  @Test
  void sonarqube_9_9_commercial_extensions() {
    SonarRuntime sqEnterprise = SonarRuntimeImpl.forSonarQube(VERSION_9_9, SonarQubeSide.SCANNER, SonarEdition.ENTERPRISE);
    Plugin.Context context = new Plugin.Context(sqEnterprise);
    javaPlugin.define(context);
    assertThat(context.getExtensions())
      .hasSize(36)
      .contains(Jasper.class);
  }

}
