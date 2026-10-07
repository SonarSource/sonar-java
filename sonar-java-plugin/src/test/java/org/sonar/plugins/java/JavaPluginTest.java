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
package org.sonar.plugins.java;

import com.sonarsource.scanner.engine.sensor.test.fixtures.TestSonarRuntime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.api.Plugin;
import org.sonar.api.PropertyType;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.utils.Version;
import org.sonar.java.SonarComponents;
import org.sonar.java.jsp.Jasper;
import org.sonar.plugins.java.api.caching.SonarLintCache;

import static org.assertj.core.api.Assertions.assertThat;

class JavaPluginTest {

  private static final Version VERSION_9_9 = Version.create(9, 9);

  private final JavaPlugin javaPlugin = new JavaPlugin();

  @Test
  void sonarLint_9_9_extensions() {
    SonarRuntime runtime = TestSonarRuntime.forSonarLint(VERSION_9_9);
    Plugin.Context context = new Plugin.Context(runtime);
    javaPlugin.define(context);
    assertThat(context.getExtensions())
      .hasSize(23)
      .contains(SonarLintCache.class);
  }


  @Test
  void sonarqube_9_9_extensions() {
    SonarRuntime sqCommunity = TestSonarRuntime.forSonarQube(VERSION_9_9, SonarQubeSide.SCANNER, SonarEdition.COMMUNITY);
    Plugin.Context context = new Plugin.Context(sqCommunity);
    javaPlugin.define(context);
    assertThat(context.getExtensions())
      .hasSize(40)
      .doesNotContain(Jasper.class);
  }

  @Test
  void sonarqube_9_9_commercial_extensions() {
    SonarRuntime sqEnterprise = TestSonarRuntime.forSonarQube(VERSION_9_9, SonarQubeSide.SCANNER, SonarEdition.ENTERPRISE);
    Plugin.Context context = new Plugin.Context(sqEnterprise);
    javaPlugin.define(context);
    assertThat(context.getExtensions())
      .hasSize(41)
      .contains(Jasper.class);
  }

  @Test
  void compilation_property_is_boolean_and_disabled_by_default() {
    SonarRuntime runtime = TestSonarRuntime.forSonarQube(VERSION_9_9, SonarQubeSide.SCANNER, SonarEdition.COMMUNITY);
    Plugin.Context context = new Plugin.Context(runtime);
    javaPlugin.define(context);

    List<?> extensions = context.getExtensions();
    PropertyDefinition property = extensions.stream()
      .filter(PropertyDefinition.class::isInstance)
      .map(PropertyDefinition.class::cast)
      .filter(definition -> definition.key().equals(SonarComponents.SONAR_COMPILE_TO_BYTE_CODE))
      .findFirst().orElseThrow();
    assertThat(property.type()).isEqualTo(PropertyType.BOOLEAN);
    assertThat(property.defaultValue()).isEqualTo("false");
  }

}
